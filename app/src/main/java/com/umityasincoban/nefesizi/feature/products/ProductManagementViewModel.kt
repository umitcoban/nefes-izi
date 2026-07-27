package com.umityasincoban.nefesizi.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umityasincoban.nefesizi.core.data.NefesIziRepository
import com.umityasincoban.nefesizi.core.data.ProductIdentityDraft
import com.umityasincoban.nefesizi.core.data.ProductRevisionDraft
import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.core.database.CigaretteProductRevisionEntity
import com.umityasincoban.nefesizi.core.domain.calculatePricePerCigaretteMicros
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductItemUi(
    val product: CigaretteProductEntity,
    val revisions: List<CigaretteProductRevisionEntity>,
    val currentRevision: CigaretteProductRevisionEntity?,
)

enum class ProductEditorMode {
    CREATE,
    EDIT,
}

data class ProductEditorState(
    val isVisible: Boolean = false,
    val mode: ProductEditorMode = ProductEditorMode.CREATE,
    val productId: String? = null,
    val name: String = "",
    val brand: String = "",
    val variant: String = "",
    val nicotineMg: String = "",
    val tarMg: String = "",
    val carbonMonoxideMg: String = "",
    val packPrice: String = "",
    val cigarettesPerPack: String = "20",
    val currencyCode: String = "TRY",
    val effectiveDate: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
)

data class ProductManagementUiState(
    val isLoading: Boolean = true,
    val items: List<ProductItemUi> = emptyList(),
    val selectedProductId: String? = null,
    val editor: ProductEditorState = ProductEditorState(),
) {
    val selectedItem: ProductItemUi?
        get() = items.firstOrNull { it.product.id == selectedProductId }
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ProductManagementViewModel @Inject constructor(
    private val repository: NefesIziRepository,
    private val clock: Clock,
) : ViewModel() {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val _editor = MutableStateFlow(ProductEditorState())
    private val _selectedProductId = MutableStateFlow<String?>(null)
    private val effectChannel = Channel<String>(Channel.BUFFERED)
    val effects: Flow<String> = effectChannel.receiveAsFlow()

    private val items = repository.observeAllProducts()
        .flatMapLatest { products ->
            if (products.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    products.map { product ->
                        repository.observeProductRevisions(product.id).map { revisions ->
                            ProductItemUi(
                                product = product,
                                revisions = revisions,
                                currentRevision = revisions
                                    .filter { it.effectiveFromEpochMillis <= clock.millis() }
                                    .maxByOrNull { it.effectiveFromEpochMillis },
                            )
                        }
                    },
                ) { it.toList() }
            }
        }

    val state: StateFlow<ProductManagementUiState> = combine(
        items,
        _selectedProductId,
        _editor,
    ) { productItems, selectedId, editor ->
        ProductManagementUiState(
            isLoading = false,
            items = productItems,
            selectedProductId = selectedId,
            editor = editor,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProductManagementUiState(),
    )

    fun selectProduct(productId: String?) {
        _selectedProductId.value = productId
    }

    fun openCreate() {
        _selectedProductId.value = null
        _editor.value = ProductEditorState(
            isVisible = true,
            effectiveDate = LocalDate.now(clock).format(formatter),
        )
    }

    fun openEdit(item: ProductItemUi) {
        val revision = item.currentRevision
        _selectedProductId.value = null
        _editor.value = ProductEditorState(
            isVisible = true,
            mode = ProductEditorMode.EDIT,
            productId = item.product.id,
            name = item.product.name,
            brand = item.product.brand.orEmpty(),
            variant = item.product.variant.orEmpty(),
            nicotineMg = revision?.nicotineMicrogramsPerCigarette.toMgInput(),
            tarMg = revision?.tarMicrogramsPerCigarette.toMgInput(),
            carbonMonoxideMg = revision?.carbonMonoxideMicrogramsPerCigarette.toMgInput(),
            packPrice = revision?.packPriceMicros.toMoneyInput(),
            cigarettesPerPack = revision?.cigarettesPerPack?.toString() ?: "20",
            currencyCode = revision?.currencyCode ?: item.product.currencyCode,
            effectiveDate = LocalDate.now(clock).format(formatter),
        )
    }

    fun closeEditor() {
        if (!_editor.value.isSaving) _editor.value = ProductEditorState()
    }

    fun updateName(value: String) = updateEditor { copy(name = value, error = null) }
    fun updateBrand(value: String) = updateEditor { copy(brand = value, error = null) }
    fun updateVariant(value: String) = updateEditor { copy(variant = value, error = null) }
    fun updateNicotine(value: String) = updateEditor { copy(nicotineMg = value, error = null) }
    fun updateTar(value: String) = updateEditor { copy(tarMg = value, error = null) }
    fun updateCarbonMonoxide(value: String) =
        updateEditor { copy(carbonMonoxideMg = value, error = null) }

    fun updatePackPrice(value: String) = updateEditor { copy(packPrice = value, error = null) }
    fun updateCigarettesPerPack(value: String) =
        updateEditor { copy(cigarettesPerPack = value, error = null) }

    fun updateCurrency(value: String) =
        updateEditor { copy(currencyCode = value.uppercase().take(3), error = null) }

    fun updateEffectiveDate(value: String) =
        updateEditor { copy(effectiveDate = value, error = null) }

    fun save() {
        val editor = _editor.value
        val validation = editor.toValidatedDraft() ?: return
        viewModelScope.launch {
            _editor.update { it.copy(isSaving = true, error = null) }
            runCatching {
                when (editor.mode) {
                    ProductEditorMode.CREATE -> repository.createProduct(
                        validation.identity,
                        validation.revision,
                    )
                    ProductEditorMode.EDIT -> {
                        val item = state.value.items.firstOrNull {
                            it.product.id == editor.productId
                        } ?: error("Ürün bulunamadı.")
                        repository.updateProduct(
                            product = item.product,
                            identity = validation.identity,
                            revisionDraft = validation.revision.takeIf {
                                item.currentRevision.hasDifferentValues(it)
                            },
                        )
                    }
                }
            }.onSuccess {
                _editor.value = ProductEditorState()
                effectChannel.send(
                    if (editor.mode == ProductEditorMode.CREATE) {
                        "Ürün ve ilk değer revizyonu oluşturuldu"
                    } else {
                        "Ürün güncellendi · geçmiş kayıtlar değişmedi"
                    },
                )
            }.onFailure { error ->
                _editor.update {
                    it.copy(
                        isSaving = false,
                        error = if (error.message?.contains("UNIQUE") == true) {
                            "Bu ürün için aynı yürürlük anında bir revizyon zaten var."
                        } else {
                            "Ürün kaydedilemedi. Alanları kontrol edip tekrar dene."
                        },
                    )
                }
            }
        }
    }

    fun setDefault(item: ProductItemUi) {
        viewModelScope.launch {
            repository.setDefaultProduct(item.product)
            _selectedProductId.value = null
            effectChannel.send("${item.product.name} varsayılan ürün oldu")
        }
    }

    fun duplicate(item: ProductItemUi) {
        viewModelScope.launch {
            runCatching {
                repository.duplicateProduct(item.product, item.currentRevision)
            }.onSuccess {
                _selectedProductId.value = null
                effectChannel.send("Ürün güncel değerleriyle kopyalandı")
            }.onFailure {
                effectChannel.send("Ürün kopyalanamadı")
            }
        }
    }

    fun setArchived(item: ProductItemUi, archived: Boolean) {
        viewModelScope.launch {
            val changed = repository.setProductArchived(item.product, archived)
            _selectedProductId.value = null
            effectChannel.send(
                when {
                    !changed -> "Son aktif ürün arşivlenemez"
                    archived -> "Ürün arşivlendi · geçmiş kayıtlar korundu"
                    else -> "Ürün yeniden etkinleştirildi"
                },
            )
        }
    }

    private inline fun updateEditor(block: ProductEditorState.() -> ProductEditorState) {
        _editor.update(block)
    }

    private fun ProductEditorState.toValidatedDraft(): ValidatedProductDraft? {
        fun fail(message: String): ValidatedProductDraft? {
            _editor.update { it.copy(error = message) }
            return null
        }

        if (name.isBlank()) return fail("Ürün adı gerekli.")
        val nicotine = nicotineMg.toScaledLongOrNull(1_000)
        val tar = tarMg.toScaledLongOrNull(1_000)
        val co = carbonMonoxideMg.toScaledLongOrNull(1_000)
        if (
            (nicotineMg.isNotBlank() && nicotine == null) ||
            (tarMg.isNotBlank() && tar == null) ||
            (carbonMonoxideMg.isNotBlank() && co == null)
        ) {
            return fail("Emisyon değerleri 0 veya daha büyük ondalık sayı olmalı.")
        }
        val packPriceMicros = packPrice.toScaledLongOrNull(1_000_000)
        if (packPrice.isNotBlank() && packPriceMicros == null) {
            return fail("Paket fiyatı 0 veya daha büyük bir sayı olmalı.")
        }
        val packSize = cigarettesPerPack.toIntOrNull()
        if (packPriceMicros != null && (packSize == null || packSize <= 0)) {
            return fail("Paket fiyatı için paket adedi pozitif bir tam sayı olmalı.")
        }
        if (currencyCode.length != 3) return fail("Para birimi üç harfli ISO kodu olmalı.")
        val date = try {
            LocalDate.parse(effectiveDate, formatter)
        } catch (_: DateTimeParseException) {
            return fail("Yürürlük tarihi YYYY-AA-GG biçiminde olmalı.")
        }
        if (date.isAfter(LocalDate.now(clock))) {
            return fail("İlk sürümde geleceğe planlı fiyat revizyonu oluşturulamıyor.")
        }
        val effectiveFrom = if (date == LocalDate.now(clock)) {
            clock.millis()
        } else {
            date.atStartOfDay(clock.zone).toInstant().toEpochMilli()
        }
        val pricePerCigarette = runCatching {
            calculatePricePerCigaretteMicros(packPriceMicros, packSize)
        }.getOrElse {
            return fail("Paket fiyatı ve adedi hesaplanamadı.")
        }
        return ValidatedProductDraft(
            identity = ProductIdentityDraft(
                name = name,
                brand = brand,
                variant = variant,
            ),
            revision = ProductRevisionDraft(
                effectiveFromEpochMillis = effectiveFrom,
                nicotineMicrogramsPerCigarette = nicotine,
                tarMicrogramsPerCigarette = tar,
                carbonMonoxideMicrogramsPerCigarette = co,
                packPriceMicros = packPriceMicros,
                cigarettesPerPack = packSize?.takeIf { packPriceMicros != null },
                priceMicrosPerCigarette = pricePerCigarette,
                currencyCode = currencyCode,
            ),
        )
    }
}

private data class ValidatedProductDraft(
    val identity: ProductIdentityDraft,
    val revision: ProductRevisionDraft,
)

private fun CigaretteProductRevisionEntity?.hasDifferentValues(
    draft: ProductRevisionDraft,
): Boolean = this == null ||
    nicotineMicrogramsPerCigarette != draft.nicotineMicrogramsPerCigarette ||
    tarMicrogramsPerCigarette != draft.tarMicrogramsPerCigarette ||
    carbonMonoxideMicrogramsPerCigarette != draft.carbonMonoxideMicrogramsPerCigarette ||
    packPriceMicros != draft.packPriceMicros ||
    cigarettesPerPack != draft.cigarettesPerPack ||
    priceMicrosPerCigarette != draft.priceMicrosPerCigarette ||
    currencyCode != draft.currencyCode

private fun String.toScaledLongOrNull(multiplier: Long): Long? {
    if (isBlank()) return null
    return runCatching {
        replace(',', '.')
            .toBigDecimal()
            .takeIf { it >= BigDecimal.ZERO }
            ?.multiply(BigDecimal.valueOf(multiplier))
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.longValueExact()
    }.getOrNull()
}

private fun Long?.toMgInput(): String = this?.let {
    BigDecimal.valueOf(it)
        .divide(BigDecimal.valueOf(1_000))
        .stripTrailingZeros()
        .toPlainString()
}.orEmpty()

private fun Long?.toMoneyInput(): String = this?.let {
    BigDecimal.valueOf(it)
        .divide(BigDecimal.valueOf(1_000_000))
        .stripTrailingZeros()
        .toPlainString()
}.orEmpty()
