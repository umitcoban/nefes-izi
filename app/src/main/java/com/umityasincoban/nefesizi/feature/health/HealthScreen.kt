package com.umityasincoban.nefesizi.feature.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun HealthScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: HealthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.saved.collect { success ->
            snackbarHostState.showSnackbar(
                if (success) "Bugünün sağlık notu kaydedildi" else "Sağlık notu kaydedilemedi",
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Sağlık günlüğü", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Kendini nasıl hissettiğini, yorum katmadan kaydet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = viewModel::previousDay) {
                        Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Önceki gün")
                    }
                    OutlinedTextField(
                        value = state.selectedDate,
                        onValueChange = viewModel::updateDateText,
                        modifier = Modifier.weight(1f),
                        label = { Text("Gün") },
                        singleLine = true,
                    )
                    TextButton(onClick = { viewModel.selectDate(state.selectedDate) }) {
                        Text("Git")
                    }
                    TextButton(onClick = viewModel::nextDay) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = "Sonraki gün")
                    }
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    ) {
                        Icon(
                            Icons.Outlined.SelfImprovement,
                            contentDescription = null,
                            modifier = Modifier.padding(13.dp),
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f).padding(start = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        SummaryCount("7 gün", state.recordedDays7)
                        SummaryCount("14 gün", state.recordedDays14)
                        SummaryCount("30 gün", state.recordedDays30)
                    }
                }
            }
        }
        item {
            JournalCard("Bugün nasıl hissediyorsun?") {
                LevelSelector("Enerji", state.energy, viewModel::setEnergy)
                LevelSelector("Stres", state.stress, viewModel::setStress)
                LevelSelector("Uyku kalitesi", state.sleep, viewModel::setSleep)
            }
        }
        item {
            JournalCard("Belirti notları") {
                SymptomSelector("Sabah öksürüğü", state.morningCough, viewModel::setMorningCough)
                SymptomSelector("Baş ağrısı", state.headache, viewModel::setHeadache)
                SymptomSelector(
                    "Nefes darlığı hissi",
                    state.shortnessOfBreath,
                    viewModel::setShortnessOfBreath,
                )
                SymptomSelector(
                    "Göğüs rahatsızlığı",
                    state.chestDiscomfort,
                    viewModel::setChestDiscomfort,
                )
            }
        }
        if (state.shortnessOfBreath == true || state.chestDiscomfort == true) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Info, contentDescription = null)
                        Text(
                            "Nefes darlığı veya göğüs rahatsızlığı önemli olabilir. " +
                                "Belirti yeni, şiddetli ya da endişe vericiyse uygun sağlık desteğine başvur.",
                            modifier = Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
        }
        item {
            JournalCard("İstersen biraz daha") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(
                        state.restingHeartRate,
                        viewModel::setHeartRate,
                        "Dinlenme nabzı",
                        "bpm",
                        Modifier.weight(1f),
                    )
                    NumberField(
                        state.exerciseMinutes,
                        viewModel::setExercise,
                        "Egzersiz",
                        "dk",
                        Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(
                        state.systolicBloodPressure,
                        viewModel::setSystolic,
                        "Büyük tansiyon",
                        "mmHg",
                        Modifier.weight(1f),
                    )
                    NumberField(
                        state.diastolicBloodPressure,
                        viewModel::setDiastolic,
                        "Küçük tansiyon",
                        "mmHg",
                        Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = state.weightKg,
                    onValueChange = viewModel::setWeight,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Kilo") },
                    suffix = { Text("kg") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::setNote,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bugünün notu") },
                    minLines = 3,
                    shape = RoundedCornerShape(18.dp),
                )
            }
        }
        if (state.errors.isNotEmpty() || state.warnings.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (state.errors.isNotEmpty()) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    },
                ) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        (state.errors + state.warnings).forEach { Text("• $it") }
                        if (state.warnings.isNotEmpty()) {
                            Text(
                                "Bu bir teşhis değildir; yalnızca olası giriş hatasını tekrar kontrol etmen içindir.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
        if (state.associations.isNotEmpty()) {
            item {
                JournalCard("Kayıtlar birlikte nasıl değişiyor?") {
                    state.associations.forEach { association ->
                        Text(association.text, style = MaterialTheme.typography.titleMedium)
                        Text(
                            association.caveat,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider()
                    }
                }
            }
        } else {
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Info, contentDescription = null)
                        Text(
                            "İlişki özeti için en az 14 ortak kayıtlı gün ve karşılaştırılan her grupta en az 5 gün gerekli.",
                            modifier = Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (state.noteHistory.isNotEmpty()) {
            item {
                JournalCard("Not geçmişi") {
                    state.noteHistory.take(5).forEach { entry ->
                        Text(entry.entryDate, style = MaterialTheme.typography.labelLarge)
                        Text(entry.note.orEmpty())
                        HorizontalDivider()
                    }
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(15.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Bu günlük tıbbi değerlendirme yapmaz. Kayıtlar arasındaki benzerlikler neden-sonuç göstermez.",
                        modifier = Modifier.padding(start = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Button(
                onClick = {
                    viewModel.save(confirmWarnings = state.warningConfirmationRequired)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
            ) {
                Text(
                    when {
                        state.isSaving -> "Kaydediliyor…"
                        state.warningConfirmationRequired -> "Kontrol ettim, yine de kaydet"
                        else -> "Sağlık notunu kaydet"
                    },
                )
            }
        }
    }
}

@Composable
private fun SummaryCount(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun JournalCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun LevelSelector(label: String, value: Int?, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(value?.let { "$it / 5" } ?: "Seçilmedi", color = MaterialTheme.colorScheme.primary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (1..5).forEach { level ->
                FilterChip(
                    selected = value == level,
                    onClick = { onChange(level) },
                    label = { Text(level.toString()) },
                    shape = CircleShape,
                )
            }
        }
    }
}

@Composable
private fun SymptomSelector(
    label: String,
    value: Boolean?,
    onChange: (Boolean?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            FilterChip(selected = value == null, onClick = { onChange(null) }, label = { Text("—") })
            FilterChip(selected = value == false, onClick = { onChange(false) }, label = { Text("Yok") })
            FilterChip(selected = value == true, onClick = { onChange(true) }, label = { Text("Var") })
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    suffix: String,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
