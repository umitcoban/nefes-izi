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
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text("Son 7 gün", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${state.recordedDaysLastWeek}/7 gün kayıtlı",
                            style = MaterialTheme.typography.headlineMedium,
                        )
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
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
            ) {
                Text(if (state.isSaving) "Kaydediliyor…" else "Bugünün notunu kaydet")
            }
        }
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
