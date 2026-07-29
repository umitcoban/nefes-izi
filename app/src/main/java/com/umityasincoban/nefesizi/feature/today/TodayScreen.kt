package com.umityasincoban.nefesizi.feature.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import com.umityasincoban.nefesizi.core.domain.ExposureTotal
import com.umityasincoban.nefesizi.feature.onboarding.OnboardingViewModel
import com.umityasincoban.nefesizi.feature.products.ProductFormSheet
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Currency
import kotlinx.coroutines.delay

@Composable
fun TodayScreen(
    snackbarHostState: SnackbarHostState,
    onAllRecords: () -> Unit,
    onAddDetails: (String) -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
    productViewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val productForm by productViewModel.form.collectAsState()
    var showProductSheet by remember { mutableStateOf(false) }
    var lastCreatedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.defaultProduct) {
        if (state.defaultProduct != null) showProductSheet = false
    }
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TodayEffect.ProductRequired -> showProductSheet = true
                TodayEffect.SaveFailed -> snackbarHostState.showSnackbar("Kayıt oluşturulamadı.")
                is TodayEffect.RecordCreated -> {
                    lastCreatedId = effect.id
                    snackbarHostState.showSnackbar(
                        message = "1 sigara kaydedildi",
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.background(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.46f),
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.background,
                ),
            ),
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            top = 20.dp,
            end = 18.dp,
            bottom = 112.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { TodayHeader(state) }
        item {
            QuickLogCard(
                state = state,
                onLog = viewModel::logCigarette,
                onSelectProduct = viewModel::selectQuickProduct,
            )
        }
        lastCreatedId?.let { id ->
            item(key = "created-$id") {
                QuickLogSuccess(
                    onAddDetails = {
                        lastCreatedId = null
                        onAddDetails(id)
                    },
                    onUndo = {
                        lastCreatedId = null
                        viewModel.undo(id)
                    },
                    onDismiss = { lastCreatedId = null },
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactMetric(
                    label = "BUGÜN",
                    value = state.totalCount.toString(),
                    unit = "adet",
                    modifier = Modifier.weight(1f),
                )
                CompactMetric(
                    label = "ORTALAMA ARALIK",
                    value = state.summary.averageIntervalMillis.formatDuration(),
                    unit = "",
                    modifier = Modifier.weight(1.5f),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                CompactMetric(
                    label = "İLK KAYIT",
                    value = state.summary.firstRecordAtEpochMillis.formatTime(),
                    unit = "",
                    modifier = Modifier.weight(1f),
                )
                if (state.showCost) {
                    CompactMetric(
                        label = "TAHMİNİ MALİYET",
                        value = state.summary.cost.formatCurrency(),
                        unit = state.summary.cost.coverageLabel(),
                        modifier = Modifier.weight(1.5f),
                    )
                } else {
                    CompactMetric(
                        label = "SON KAYITTAN BERİ",
                        value = elapsedText(state.records.firstOrNull()),
                        unit = "",
                        modifier = Modifier.weight(1.5f),
                    )
                }
            }
        }
        if (state.showExposure) {
            item {
                SectionTitle(
                    title = "Tahmini emisyonlar",
                    supporting = "Kayıtlı ürün değerlerine göre",
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ExposureCard("Nikotin", state.nicotine, Color(0xFFE8795F), Modifier.weight(1f))
                    ExposureCard("Katran", state.tar, Color(0xFF2F6972), Modifier.weight(1f))
                    ExposureCard("CO", state.carbonMonoxide, Color(0xFFF0AD55), Modifier.weight(1f))
                }
            }
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Bu değerler duman emisyonlarının matematiksel toplamıdır; kişisel emilimi veya kandaki düzeyi göstermez.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            SectionTitle(
                title = "Bugünün izi",
                supporting = if (state.records.isEmpty()) "İlk kayıt burada görünecek" else "En son kayıtların",
                actionLabel = if (state.records.isEmpty()) null else "Tümünü gör",
                onAction = onAllRecords,
            )
        }
        if (state.records.isEmpty()) {
            item { EmptyTimeline() }
        } else {
            items(state.records.take(5), key = { it.id }) { record ->
                RecordRow(record)
            }
        }
    }

    if (showProductSheet) {
        ProductFormSheet(
            state = productForm,
            onDismiss = { showProductSheet = false },
            onNameChange = productViewModel::updateName,
            onNicotineChange = productViewModel::updateNicotine,
            onTarChange = productViewModel::updateTar,
            onCarbonMonoxideChange = productViewModel::updateCarbonMonoxide,
            onSave = productViewModel::saveProductAndContinue,
            saveLabel = "Varsayılan ürün yap",
        )
    }
}

@Composable
private fun TodayHeader(state: TodayUiState) {
    val date = remember {
        DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale.forLanguageTag("tr-TR"))
            .format(java.time.LocalDate.now())
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Bugün", style = MaterialTheme.typography.headlineLarge)
            Text(
                date.replaceFirstChar { it.uppercase() },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            tonalElevation = 2.dp,
        ) {
            Text(
                state.totalCount.toString().padStart(2, '0'),
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickLogCard(
    state: TodayUiState,
    onLog: () -> Unit,
    onSelectProduct: (String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        shape = RoundedCornerShape(32.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.secondary, Color(0xFF08262E)),
                    ),
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.align(Alignment.CenterStart).padding(end = 126.dp)) {
                Text(
                    "Tek dokunuş,\nnet bir iz.",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    state.selectedQuickProduct?.let { "Seçili · ${it.name}" } ?: "Önce bir ürün seç",
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onLog,
                enabled = !state.isLogging,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(112.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = BorderStroke(7.dp, Color.White.copy(alpha = 0.12f)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (state.isLogging) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(30.dp))
                        Text(
                            "Sigara\nİçtim",
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
            }
            if (state.products.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.products.forEach { product ->
                        FilterChip(
                            selected = state.selectedQuickProduct?.id == product.id,
                            onClick = { onSelectProduct(product.id) },
                            label = { Text(product.name, maxLines = 1) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickLogSuccess(
    onAddDetails: () -> Unit,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Kayıt hazır", style = MaterialTheme.typography.titleMedium)
                Text(
                    "İstersen bağlam ve not ekleyebilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onUndo) { Text("Geri al") }
            TextButton(onClick = onAddDetails) { Text("Detay ekle") }
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    }
}

@Composable
private fun CompactMetric(label: String, value: String, unit: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium)
                if (unit.isNotBlank()) {
                    Text(
                        "  $unit",
                        modifier = Modifier.padding(bottom = 3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExposureCard(
    title: String,
    exposure: ExposureTotal,
    accent: Color,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(
                exposure.micrograms.formatMg(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (exposure.unknownCount > 0) "${exposure.unknownCount} bilinmeyen" else "${exposure.knownCount} kayıtlı",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    supporting: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (actionLabel == null) {
            Text(
                supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun EmptyTimeline() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.padding(13.dp),
                )
            }
            Column {
                Text("Henüz bugün için kayıt yok", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Hazır olduğunda tek dokunuş yeterli.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecordRow(record: SmokingRecordEntity) {
    val time = remember(record.smokedAtEpochMillis) {
        Instant.ofEpochMilli(record.smokedAtEpochMillis)
            .atZone(ZoneId.of(record.zoneIdSnapshot))
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    time,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(record.productNameSnapshot, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${record.quantity} adet · tam içildi",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun elapsedText(lastRecord: SmokingRecordEntity?): String {
    if (lastRecord == null) return "—"
    val now by produceState(initialValue = System.currentTimeMillis(), lastRecord.id) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000)
        }
    }
    val minutes = Duration.ofMillis((now - lastRecord.smokedAtEpochMillis).coerceAtLeast(0)).toMinutes()
    return when {
        minutes < 1 -> "şimdi"
        minutes < 60 -> "$minutes dk"
        else -> "${minutes / 60} sa ${minutes % 60} dk"
    }
}

private fun Long?.formatMg(): String {
    if (this == null) return "—"
    val format = DecimalFormat("0.##", DecimalFormatSymbols.getInstance())
    return "${format.format(this / 1_000.0)} mg"
}

private fun Long?.formatDuration(): String {
    if (this == null) return "—"
    val minutes = Duration.ofMillis(this).toMinutes()
    return when {
        minutes < 1 -> "<1 dk"
        minutes < 60 -> "$minutes dk"
        else -> "${minutes / 60} sa ${minutes % 60} dk"
    }
}

private fun Long?.formatTime(): String {
    if (this == null) return "—"
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun com.umityasincoban.nefesizi.core.domain.CostTotal.formatCurrency(): String {
    val value = micros ?: return "—"
    val currency = currencyCode ?: return "Karma"
    return runCatching {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("tr-TR")).apply {
            this.currency = Currency.getInstance(currency)
            maximumFractionDigits = 2
        }.format(value / 1_000_000.0)
    }.getOrElse { "—" }
}

private fun com.umityasincoban.nefesizi.core.domain.CostTotal.coverageLabel(): String =
    when {
        knownCount == 0 -> "fiyat bilinmiyor"
        unknownCount > 0 -> "$knownCount bilinen · $unknownCount eksik"
        else -> "$knownCount adet kapsanıyor"
    }
