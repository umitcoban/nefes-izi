package com.umityasincoban.nefesizi.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Analiz", style = MaterialTheme.typography.headlineLarge)
            Text("Son 30 gün · yalnızca cihazındaki kayıtlar", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("TOPLAM KAYIT", style = MaterialTheme.typography.labelLarge)
                        Text(
                            state.total.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("Günlük ortalama ${DecimalFormat("0.0").format(state.dailyAverage)}")
                    }
                    Icon(
                        Icons.Outlined.AutoGraph,
                        contentDescription = null,
                        modifier = Modifier.width(58.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(
                    "EN UZUN ARA",
                    state.longestGapMinutes.formatGap(),
                    Modifier.weight(1f),
                )
                MetricTile(
                    "YOĞUN SAAT",
                    state.peakHour?.let { "%02d:00".format(it) } ?: "—",
                    Modifier.weight(1f),
                )
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Son 7 günün ritmi", style = MaterialTheme.typography.titleLarge)
                    WeeklyBars(state.lastSevenDays)
                    Text(
                        state.lastSevenDays.joinToString(" · ") {
                            "${it.date.format(DateTimeFormatter.ofPattern("E", Locale.forLanguageTag("tr-TR")))} ${it.count}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Tahmini nikotin emisyonu", style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.nicotineMicrograms?.let { "${DecimalFormat("0.##").format(it / 1_000.0)} mg" } ?: "—",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        "${state.nicotineKnown} sigara üzerinden hesaplandı" +
                            if (state.nicotineUnknown > 0) " · ${state.nicotineUnknown} bilinmiyor" else "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        if (state.hasEnoughData) {
                            "Grafikler kişisel geçmiş karşılaştırmasıdır; sağlık açısından güvenli bir sınır göstermez."
                        } else {
                            "Daha anlamlı örüntüler için en az 7 farklı günde kayıt gerekli."
                        },
                        modifier = Modifier.padding(start = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun WeeklyBars(days: List<DailyCount>) {
    val max = days.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(day.count.toString(), style = MaterialTheme.typography.bodyMedium)
                Box(
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .width(22.dp)
                        .height((18 + 75 * day.count / max).dp)
                        .background(
                            if (day == days.lastOrNull()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(12.dp),
                        ),
                )
                Text(
                    day.date.format(DateTimeFormatter.ofPattern("EE", Locale.forLanguageTag("tr-TR"))),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private fun Long?.formatGap(): String = when {
    this == null -> "—"
    this < 60 -> "$this dk"
    else -> "${this / 60} sa ${this % 60} dk"
}
