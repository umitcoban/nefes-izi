package com.umityasincoban.nefesizi.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umityasincoban.nefesizi.core.data.AppPreferences
import com.umityasincoban.nefesizi.core.data.NefesIziRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductFormState(
    val name: String = "",
    val nicotineMg: String = "",
    val tarMg: String = "",
    val carbonMonoxideMg: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: NefesIziRepository,
    private val preferences: AppPreferences,
) : ViewModel() {
    private val _form = MutableStateFlow(ProductFormState())
    val form: StateFlow<ProductFormState> = _form.asStateFlow()

    fun updateName(value: String) = _form.update { it.copy(name = value, error = null) }
    fun updateNicotine(value: String) = _form.update { it.copy(nicotineMg = value, error = null) }
    fun updateTar(value: String) = _form.update { it.copy(tarMg = value, error = null) }
    fun updateCarbonMonoxide(value: String) =
        _form.update { it.copy(carbonMonoxideMg = value, error = null) }

    fun saveProductAndContinue() {
        val current = _form.value
        if (current.name.isBlank()) {
            _form.update { it.copy(error = "Ürün adı gerekli.") }
            return
        }
        val nicotine = current.nicotineMg.toMicrogramsOrNull()
        val tar = current.tarMg.toMicrogramsOrNull()
        val co = current.carbonMonoxideMg.toMicrogramsOrNull()
        if (
            (current.nicotineMg.isNotBlank() && nicotine == null) ||
            (current.tarMg.isNotBlank() && tar == null) ||
            (current.carbonMonoxideMg.isNotBlank() && co == null)
        ) {
            _form.update { it.copy(error = "Değerleri 0 veya daha büyük ondalık sayı olarak gir.") }
            return
        }
        viewModelScope.launch {
            _form.update { it.copy(isSaving = true, error = null) }
            runCatching {
                repository.createDefaultProduct(current.name, nicotine, tar, co)
                preferences.completeOnboarding()
            }.onFailure {
                _form.update { state ->
                    state.copy(isSaving = false, error = "Ürün kaydedilemedi. Tekrar deneyebilirsin.")
                }
            }
        }
    }

    fun skip() {
        viewModelScope.launch { preferences.completeOnboarding() }
    }
}

private fun String.toMicrogramsOrNull(): Long? {
    if (isBlank()) return null
    return runCatching {
        replace(',', '.')
            .toBigDecimal()
            .takeIf { it >= BigDecimal.ZERO }
            ?.multiply(BigDecimal(1_000))
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.longValueExact()
    }.getOrNull()
}
