package com.umityasincoban.nefesizi.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.umityasincoban.nefesizi.core.domain.AdvancedAnalytics
import com.umityasincoban.nefesizi.core.domain.AnalyticsDay
import com.umityasincoban.nefesizi.core.domain.AnalyticsInsight
import com.umityasincoban.nefesizi.core.domain.CurrencyAmount
import com.umityasincoban.nefesizi.core.domain.ExposureTotal
import com.umityasincoban.nefesizi.core.domain.RankedMetric
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val analytics = state.analytics
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            top = 18.dp,
            end = 18.dp,
            bottom = 110.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Analiz", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Kendi kayıt ritmini, maliyetini ve coverage durumunu gör",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = state.selection.period == period,
                        onClick = { viewModel.selectPeriod(period) },
                        label = { Text(period.label) },
                    )
                }
            }
        }
        if (state.selection.period == AnalyticsPeriod.CUSTOM) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.selection.customStart,
                        onValueChange = viewModel::updateCustomStart,
                        label = { Text("Başlangıç") },
                        placeholder = { Text("YYYY-AA-GG") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.selection.customEnd,
                        onValueChange = viewModel::updateCustomEnd,
                        label = { Text("Bitiş") },
                        placeholder = { Text("YYYY-AA-GG") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
            }
            state.dateError?.let { error ->
                item { Text(error, color = MaterialTheme.colorScheme.error) }
            }
        }
        if (analytics != null) {
            item { AnalyticsHero(analytics) }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2,
                ) {
                    MetricTile(
                        "EN YÜKSEK GÜN",
                        analytics.highestDay.dayLabel(),
                        Modifier.weight(1f),
                    )
                    MetricTile(
                        "EN DÜŞÜK GÜN",
                        analytics.lowestDay.dayLabel(),
                        Modifier.weight(1f),
                    )
                    MetricTile(
                        "ORTALAMA ARALIK",
                        analytics.averageIntervalMinutes.formatGap(),
                        Modifier.weight(1f),
                    )
                    MetricTile(
                        "EN UZUN ARA",
                        analytics.longestGapMinutes.formatGap(),
                        Modifier.weight(1f),
                    )
                    if (analytics.averageFirstRecordAfterWakeMinutes != null) {
                        MetricTile(
                            "UYANIŞTAN İLK KAYDA",
                            analytics.averageFirstRecordAfterWakeMinutes.formatGap(),
                            Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                SectionCard("Günlük ritim", "${analytics.recordedDayCount}/${analytics.dayCount} kayıtlı gün") {
                    DailyBars(analytics.daily.takeLast(14))
                    Text(
                        if (analytics.daily.size > 14) "Seçilen dönemin son 14 günü gösteriliyor."
                        else "Seçilen dönemin günlük adetleri.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { CostSection(state, analytics) }
            item {
                SectionCard("Sık karşılaşılan bağlam", "Girilen alanlar üzerinden") {
                    RankedRow("Ürün", analytics.mostCommonProduct)
                    RankedRow("Tetikleyici", analytics.mostCommonTrigger)
                    RankedRow("Ruh hâli", analytics.mostCommonMood)
                }
            }
            item {
                SectionCard("Saat dağılımı", "Olay anındaki yerel saate göre") {
                    HourBands(analytics.hourlyCounts)
                }
            }
            item {
                SectionCard("Haftanın ritmi", "Adet dağılımı") {
                    WeekdayBars(analytics.weekdayCounts)
                }
            }
            item {
                Text("Tahmini duman emisyonu", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Kayıtlı ürün değerlerinin matematiksel toplamı",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ExposureCard("Nikotin", analytics.nicotine, Color(0xFFE8795F), Modifier.weight(1f))
                    ExposureCard("Katran", analytics.tar, Color(0xFF2F6972), Modifier.weight(1f))
                    ExposureCard("CO", analytics.carbonMonoxide, Color(0xFFF0AD55), Modifier.weight(1f))
                }
            }
            if (analytics.insights.isNotEmpty()) {
                item {
                    Text("Kayıtlarından notlar", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Deterministik ve açıklanabilir özetler",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                analytics.insights.forEach { insight ->
                    item(key = insight.type) { InsightCard(insight) }
                }
            }
            item { AnalyticsDisclaimer(analytics) }
        }
    }
}

@Composable
private fun AnalyticsHero(analytics: AdvancedAnalytics) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SEÇİLEN DÖNEM", style = MaterialTheme.typography.labelLarge)
                    Text(
                        analytics.totalCount.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Günlük ortalama ${DecimalFormat("0.0").format(analytics.dailyAverage)} adet")
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                ) {
                    Icon(
                        Icons.Outlined.AutoGraph,
                        contentDescription = null,
                        modifier = Modifier.padding(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            analytics.comparison?.let { comparison ->
                val label = comparison.changePercent?.let {
                    val direction = if (it >= 0) "fazla" else "az"
                    "Önceki eş döneme göre %${abs(it).toInt()} $direction"
                } ?: "Önceki eş dönemde kayıt yok"
                Text(label, color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.78f))
            }
            Text(
                "${analytics.startDate.format(SHORT_DATE)} – ${analytics.endDate.format(SHORT_DATE)}",
                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.72f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CostSection(state: AnalyticsUiState, analytics: AdvancedAnalytics) {
    SectionCard("Maliyet görünümü", "Snapshot fiyatlar üzerinden") {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            CostTile("Bugün", state.todayCosts, Modifier.weight(1f))
            CostTile("Bu hafta", state.weekCosts, Modifier.weight(1f))
            CostTile("Bu ay", state.monthCosts, Modifier.weight(1f))
            CostTile("Seçilen dönem", analytics.costs, Modifier.weight(1f))
        }
        if (analytics.unknownCostCount > 0) {
            Text(
                "${analytics.unknownCostCount} adet kayıtta fiyat bilinmiyor; maliyete sıfır olarak eklenmedi.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (analytics.annualProjectionEligible) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Payments, contentDescription = null)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text("Mevcut kayıt hızına göre yıllık tahmin")
                        Text(
                            analytics.annualCostProjection.amountLabel(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Son 30 gün ve en az 7 kayıtlı gün üzerinden.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        } else {
            Text(
                "Yıllık tahmin için son 30 günde en az 7 farklı kayıtlı gün gerekli.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    supporting: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DailyBars(days: List<AnalyticsDay>) {
    val max = days.maxOfOrNull(AnalyticsDay::count)?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(day.count.toString(), style = MaterialTheme.typography.labelSmall)
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .width(if (days.size > 10) 12.dp else 22.dp)
                        .height((12 + 75 * day.count / max).dp)
                        .background(
                            if (day == days.lastOrNull()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(10.dp),
                        ),
                )
                Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CostTile(label: String, values: List<CurrencyAmount>, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(values.amountLabel(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RankedRow(label: String, metric: RankedMetric?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            metric?.let { "${it.label} · ${it.count}/${it.knownCount}" } ?: "Yeterli veri yok",
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HourBands(hourly: List<Int>) {
    val bands = listOf(0..5, 6..11, 12..17, 18..23).map { range ->
        "${"%02d".format(range.first)}–${"%02d".format(range.last + 1)}" to
            range.sumOf { hourly.getOrElse(it) { 0 } }
    }
    HorizontalDistribution(bands)
}

@Composable
private fun WeekdayBars(counts: Map<DayOfWeek, Int>) {
    val labels = DayOfWeek.entries.map {
        it.getDisplayName(java.time.format.TextStyle.SHORT, TURKISH) to (counts[it] ?: 0)
    }
    HorizontalDistribution(labels)
}

@Composable
private fun HorizontalDistribution(values: List<Pair<String, Int>>) {
    val max = values.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { (label, count) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, modifier = Modifier.width(54.dp), style = MaterialTheme.typography.labelMedium)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(count.toFloat() / max)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                    )
                }
                Text("$count", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelMedium)
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
    Surface(modifier = modifier, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.width(12.dp).height(12.dp).background(accent, CircleShape))
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(exposure.micrograms.formatMg(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "${exposure.knownCount} bilinen" +
                    if (exposure.unknownCount > 0) " · ${exposure.unknownCount} eksik" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InsightCard(insight: AnalyticsInsight) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(insight.text, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${insight.numerator}/${insight.denominator} · ${insight.caveat}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AnalyticsDisclaimer(analytics: AdvancedAnalytics) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                if (analytics.recordedDayCount >= 7) {
                    "Grafikler kişisel geçmiş karşılaştırmasıdır. Sağlık açısından güvenli veya tehlikeli bir sınır ve neden-sonuç ilişkisi göstermez."
                } else {
                    "Daha dengeli dönem karşılaştırmaları için en az 7 farklı günde kayıt gerekli."
                },
                modifier = Modifier.padding(start = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun List<CurrencyAmount>.amountLabel(): String =
    if (isEmpty()) {
        "—"
    } else {
        joinToString(" · ") { amount ->
            runCatching {
                NumberFormat.getCurrencyInstance(TURKISH).apply {
                    currency = Currency.getInstance(amount.currencyCode)
                    maximumFractionDigits = 2
                }.format(amount.micros / 1_000_000.0)
            }.getOrElse { "${amount.micros / 1_000_000.0} ${amount.currencyCode}" }
        }
    }

private fun AnalyticsDay?.dayLabel(): String =
    this?.let { "${date.format(DAY_DATE)} · $count" } ?: "—"

private fun Long?.formatGap(): String = when {
    this == null -> "—"
    this < 60 -> "$this dk"
    else -> "${this / 60} sa ${this % 60} dk"
}

private fun Long?.formatMg(): String =
    this?.let { "${DecimalFormat("0.##").format(it / 1_000.0)} mg" } ?: "—"

private val TURKISH = Locale.forLanguageTag("tr-TR")
private val SHORT_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", TURKISH)
private val DAY_DATE = DateTimeFormatter.ofPattern("d MMM", TURKISH)
