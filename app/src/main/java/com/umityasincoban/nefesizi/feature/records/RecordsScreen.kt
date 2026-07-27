package com.umityasincoban.nefesizi.feature.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RecordsScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: RecordsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.deletions.collect { record ->
            val result = snackbarHostState.showSnackbar(
                message = "Kayıt silindi",
                actionLabel = "Geri al",
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restore(record)
        }
    }
    val grouped = state.records.groupBy { record ->
        Instant.ofEpochMilli(record.smokedAtEpochMillis)
            .atZone(ZoneId.of(record.zoneIdSnapshot))
            .toLocalDate()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Kayıtlar", style = MaterialTheme.typography.headlineLarge)
            Text(
                "${state.totalCount} adetlik kişisel arşivin",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                placeholder = { Text("Ürün veya notlarda ara") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
            )
        }
        if (state.records.isEmpty()) {
            item { EmptyRecords(hasQuery = state.query.isNotBlank()) }
        } else {
            grouped.forEach { (date, records) ->
                item(key = "header-$date") { DayHeader(date, records) }
                items(records, key = { it.id }) { record ->
                    RecordCard(record = record, onDelete = { viewModel.delete(record) })
                }
            }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, records: List<SmokingRecordEntity>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            date.format(DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale.forLanguageTag("tr-TR"))),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "${records.sumOf { it.quantity }} adet",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun RecordCard(record: SmokingRecordEntity, onDelete: () -> Unit) {
    val time = Instant.ofEpochMilli(record.smokedAtEpochMillis)
        .atZone(ZoneId.of(record.zoneIdSnapshot))
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    time,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(record.productNameSnapshot, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${record.quantity} adet · ${record.consumedQuarter * 25}% içildi",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                record.note?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Kaydı sil",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun EmptyRecords(hasQuery: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (hasQuery) "Eşleşen kayıt yok" else "Henüz kayıt oluşturmadın",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                if (hasQuery) "Arama ifadesini değiştirerek tekrar deneyebilirsin."
                else "Bugün ekranındaki büyük butonla ilk izini bırakabilirsin.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
