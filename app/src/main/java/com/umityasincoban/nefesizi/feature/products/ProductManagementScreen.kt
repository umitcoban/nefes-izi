package com.umityasincoban.nefesizi.feature.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.umityasincoban.nefesizi.core.database.CigaretteProductRevisionEntity
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

@Composable
fun ProductManagementScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: ProductManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showArchived by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { snackbarHostState.showSnackbar(it) }
    }

    val visibleItems = state.items.filter { it.product.isArchived == showArchived }
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Geri",
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Ürünlerin", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Fiyat ve ürün değerlerinin zaman çizgisi",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = viewModel::openCreate) {
                    Icon(Icons.Outlined.Add, contentDescription = "Yeni ürün")
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    ) {
                        Icon(
                            Icons.Outlined.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.padding(14.dp).size(30.dp),
                        )
                    }
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            "${state.items.count { !it.product.isArchived }} aktif ürün",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            "${state.items.sumOf { it.revisions.size }} değer revizyonu · geçmiş kayıtlar sabit",
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.76f),
                        )
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showArchived,
                    onClick = { showArchived = false },
                    label = { Text("Aktif") },
                )
                FilterChip(
                    selected = showArchived,
                    onClick = { showArchived = true },
                    label = { Text("Arşiv") },
                )
            }
        }
        if (state.isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (visibleItems.isEmpty()) {
            item {
                EmptyProducts(
                    archived = showArchived,
                    onAdd = viewModel::openCreate,
                )
            }
        } else {
            items(visibleItems.size, key = { visibleItems[it].product.id }) { index ->
                ProductCard(
                    item = visibleItems[index],
                    onClick = { viewModel.selectProduct(visibleItems[index].product.id) },
                )
            }
        }
        if (!showArchived) {
            item {
                Button(
                    onClick = viewModel::openCreate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text("Yeni ürün ve değerleri", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    state.selectedItem?.let { item ->
        ProductDetailSheet(
            item = item,
            onDismiss = { viewModel.selectProduct(null) },
            onEdit = { viewModel.openEdit(item) },
            onDefault = { viewModel.setDefault(item) },
            onDuplicate = { viewModel.duplicate(item) },
            onArchive = { viewModel.setArchived(item, !item.product.isArchived) },
        )
    }

    if (state.editor.isVisible) {
        ProductEditorSheet(
            state = state.editor,
            onDismiss = viewModel::closeEditor,
            onNameChange = viewModel::updateName,
            onBrandChange = viewModel::updateBrand,
            onVariantChange = viewModel::updateVariant,
            onNicotineChange = viewModel::updateNicotine,
            onTarChange = viewModel::updateTar,
            onCarbonMonoxideChange = viewModel::updateCarbonMonoxide,
            onPackPriceChange = viewModel::updatePackPrice,
            onPackSizeChange = viewModel::updateCigarettesPerPack,
            onCurrencyChange = viewModel::updateCurrency,
            onEffectiveDateChange = viewModel::updateEffectiveDate,
            onSave = viewModel::save,
        )
    }
}

@Composable
private fun ProductCard(item: ProductItemUi, onClick: () -> Unit) {
    val revision = item.currentRevision
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.product.name, style = MaterialTheme.typography.titleLarge)
                        if (item.product.isDefault) {
                            Surface(
                                modifier = Modifier.padding(start = 8.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    "Varsayılan",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                    val identity = listOfNotNull(item.product.brand, item.product.variant)
                        .joinToString(" · ")
                    if (identity.isNotBlank()) {
                        Text(identity, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    revision?.priceMicrosPerCigarette.asMoney(revision?.currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SnapshotMetric("Nikotin", revision?.nicotineMicrogramsPerCigarette.asMg())
                SnapshotMetric("Katran", revision?.tarMicrogramsPerCigarette.asMg())
                SnapshotMetric("CO", revision?.carbonMonoxideMicrogramsPerCigarette.asMg())
                SnapshotMetric("Revizyon", item.revisions.size.toString())
            }
        }
    }
}

@Composable
private fun SnapshotMetric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDetailSheet(
    item: ProductItemUi,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDefault: () -> Unit,
    onDuplicate: () -> Unit,
    onArchive: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(item.product.name, style = MaterialTheme.typography.headlineMedium)
            Text(
                if (item.product.isArchived) {
                    "Arşivde · geçmiş kayıtlar bu ürün snapshot’ını kullanmaya devam eder."
                } else {
                    "Her revizyon yalnızca yürürlüğe girdikten sonraki yeni kayıtları etkiler."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onEdit, enabled = !item.product.isArchived) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Text("Düzenle", modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(onClick = onDuplicate) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    Text("Kopyala", modifier = Modifier.padding(start = 6.dp))
                }
            }
            if (!item.product.isDefault && !item.product.isArchived) {
                OutlinedButton(onClick = onDefault, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.StarOutline, contentDescription = null)
                    Text("Varsayılan ürün yap", modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text("Değer geçmişi", style = MaterialTheme.typography.titleLarge)
            item.revisions.forEachIndexed { index, revision ->
                RevisionRow(revision = revision, current = revision.id == item.currentRevision?.id)
                if (index != item.revisions.lastIndex) HorizontalDivider()
            }
            OutlinedButton(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    if (item.product.isArchived) Icons.Outlined.Restore else Icons.Outlined.Archive,
                    contentDescription = null,
                )
                Text(
                    if (item.product.isArchived) "Arşivden çıkar" else "Ürünü arşivle",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun RevisionRow(revision: CigaretteProductRevisionEntity, current: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = if (current) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Icon(
                if (current) Icons.Outlined.Check else Icons.Outlined.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                Instant.ofEpochMilli(revision.effectiveFromEpochMillis)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "N ${revision.nicotineMicrogramsPerCigarette.asMg()} · " +
                    "K ${revision.tarMicrogramsPerCigarette.asMg()} · " +
                    "CO ${revision.carbonMonoxideMicrogramsPerCigarette.asMg()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                revision.priceMicrosPerCigarette.asMoney(revision.currencyCode),
                style = MaterialTheme.typography.titleMedium,
            )
            revision.packPriceMicros?.let {
                Text(
                    "Paket ${it.asMoney(revision.currencyCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditorSheet(
    state: ProductEditorState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onBrandChange: (String) -> Unit,
    onVariantChange: (String) -> Unit,
    onNicotineChange: (String) -> Unit,
    onTarChange: (String) -> Unit,
    onCarbonMonoxideChange: (String) -> Unit,
    onPackPriceChange: (String) -> Unit,
    onPackSizeChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onEffectiveDateChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Text(
                if (state.mode == ProductEditorMode.CREATE) "Yeni ürün" else "Ürünü güncelle",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "Yeni değerler geçmiş kayıtları değiştirmez. Kayıtlar oluşturulurken yürürlükteki değerlerin kopyasını alır.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ürün adı") },
                singleLine = true,
                isError = state.error != null && state.name.isBlank(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.brand,
                    onValueChange = onBrandChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Marka") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.variant,
                    onValueChange = onVariantChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Varyant") },
                    singleLine = true,
                )
            }
            Text("Sigara başı tahmini emisyon", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorDecimalField(state.nicotineMg, onNicotineChange, "Nikotin", Modifier.weight(1f))
                EditorDecimalField(state.tarMg, onTarChange, "Katran", Modifier.weight(1f))
                EditorDecimalField(state.carbonMonoxideMg, onCarbonMonoxideChange, "CO", Modifier.weight(1f))
            }
            Text("Paket ve maliyet", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.packPrice,
                    onValueChange = onPackPriceChange,
                    modifier = Modifier.weight(1.4f),
                    label = { Text("Paket fiyatı") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.cigarettesPerPack,
                    onValueChange = onPackSizeChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Paket adedi") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.currencyCode,
                    onValueChange = onCurrencyChange,
                    modifier = Modifier.weight(0.8f),
                    label = { Text("Birim") },
                    singleLine = true,
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text("Şu tarihten itibaren geçerli", style = MaterialTheme.typography.labelMedium)
                        Text(state.effectiveDate, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            ) {
                Text(
                    "Geçmiş bir tarih seçsen bile mevcut sigara kayıtları değişmez. Bu revizyon yalnızca bundan sonra oluşturulan ve olay zamanı bu aralığa düşen kayıtlarda kullanılır.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        if (state.mode == ProductEditorMode.CREATE) {
                            "Ürünü oluştur"
                        } else {
                            "Değişiklikleri kaydet"
                        },
                    )
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Vazgeç")
            }
        }
    }

    if (showDatePicker) {
        val initial = runCatching {
            LocalDate.parse(state.effectiveDate)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onEffectiveDateChange(
                                Instant.ofEpochMilli(it)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                    .toString(),
                            )
                        }
                        showDatePicker = false
                    },
                ) { Text("Seç") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("İptal") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun EditorDecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        suffix = { Text("mg") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

@Composable
private fun EmptyProducts(archived: Boolean, onAdd: () -> Unit) {
    Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                if (archived) "Arşiv boş" else "Henüz ürün yok",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 10.dp),
            )
            if (!archived) {
                TextButton(onClick = onAdd) { Text("İlk ürünü ekle") }
            }
        }
    }
}

private fun Long?.asMg(): String = this?.let {
    val mg = BigDecimal.valueOf(it).divide(BigDecimal.valueOf(1_000)).stripTrailingZeros()
    "${mg.toPlainString()} mg"
} ?: "—"

private fun Long?.asMoney(currencyCode: String?): String {
    if (this == null || currencyCode == null) return "Fiyat yok"
    return runCatching {
        NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance(currencyCode)
        }.format(BigDecimal.valueOf(this).divide(BigDecimal.valueOf(1_000_000)))
    }.getOrElse { "$this $currencyCode" }
}
