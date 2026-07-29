package com.umityasincoban.nefesizi.feature.records

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.core.database.SmokingRecordEntity
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: RecordsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var deleteCandidate by remember { mutableStateOf<SmokingRecordEntity?>(null) }
    LaunchedEffect(Unit) {
        viewModel.deletions.collect { record ->
            val result = snackbarHostState.showSnackbar("Kayıt silindi", "Geri al")
            if (result == SnackbarResult.ActionPerformed) viewModel.restore(record)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.notifications.collect { snackbarHostState.showSnackbar(it) }
    }
    val grouped = state.records.groupBy { it.localDate() }

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
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kayıtlar", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "${state.totalCount} adet · yerel ve sana ait",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    onClick = viewModel::openCreate,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Geçmiş kayıt ekle",
                        modifier = Modifier.padding(13.dp),
                    )
                }
            }
        }
        if (state.filters.activeCount > 0) {
            item {
                ActiveFilters(
                    filters = state.filters,
                    products = state.products,
                    onClear = viewModel::clearFilters,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::updateQuery,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    placeholder = { Text("Ürün, not, tetikleyici ara") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                )
                Surface(
                    onClick = viewModel::showFilters,
                    shape = RoundedCornerShape(20.dp),
                    color = if (state.filters.activeCount > 0) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Outlined.FilterAlt, contentDescription = "Filtreler")
                        Text(
                            if (state.filters.activeCount > 0) "${state.filters.activeCount}" else "Filtre",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
        if (state.records.isEmpty()) {
            item {
                EmptyRecords(
                    filtered = state.query.isNotBlank() || state.filters.activeCount > 0,
                    onAdd = viewModel::openCreate,
                )
            }
        } else {
            grouped.forEach { (date, records) ->
                item(key = "header-$date") { DayHeader(date, records) }
                items(records, key = { it.id }) { record ->
                    RecordCard(
                        record = record,
                        onClick = { viewModel.select(record) },
                        onDelete = { deleteCandidate = record },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }

    state.selectedRecord?.let { record ->
        RecordDetailSheet(
            record = record,
            onDismiss = viewModel::dismissDetail,
            onEdit = { viewModel.openEdit(record) },
            onDuplicate = { viewModel.openDuplicate(record) },
            onDelete = { deleteCandidate = record },
        )
    }
    state.editor?.let { editor ->
        RecordEditorSheet(
            editor = editor,
            products = state.products,
            onUpdate = viewModel::updateEditor,
            onSave = viewModel::saveEditor,
            onDismiss = viewModel::dismissEditor,
        )
    }
    if (state.filtersVisible) {
        RecordFiltersSheet(
            state = state,
            onDismiss = viewModel::hideFilters,
            onPeriod = viewModel::updatePeriod,
            onProduct = viewModel::updateProductFilter,
            onTrigger = viewModel::updateTriggerFilter,
            onMood = viewModel::updateMoodFilter,
            onUnknown = viewModel::updateUnknownFilter,
            onNotes = viewModel::updateNotesFilter,
            onStartDate = viewModel::updateStartDate,
            onEndDate = viewModel::updateEndDate,
            onClear = viewModel::clearFilters,
        )
    }
    deleteCandidate?.let { record ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Bu kaydı silelim mi?") },
            text = {
                Text(
                    "${record.productNameSnapshot} · ${record.localDate()} ${record.formattedTime()}\n\nSilme işleminden sonra kısa süre içinde geri alabilirsin.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    deleteCandidate = null
                    viewModel.delete(record)
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Vazgeç") }
            },
        )
    }
}

@Composable
private fun DayHeader(date: LocalDate, records: List<SmokingRecordEntity>) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                date.format(DateTimeFormatter.ofPattern("d MMMM, EEEE", TURKISH)),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "${records.sumOf { it.quantity }} adet",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Text(
            "${records.totalCostLabel()} · emisyon verisi ${records.emissionCoverageCount()}/${records.size} kayıtta",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecordCard(
    record: SmokingRecordEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    record.formattedTime(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(record.productNameSnapshot, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        append("${record.quantity} adet · ${record.consumedQuarter * 25}%")
                        record.trigger?.let { append(" · $it") }
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                record.note?.takeIf(String::isNotBlank)?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Text(
                    if (record.hasEmissionSnapshot()) {
                        "Nikotin ${record.nicotineMicrogramsPerCigaretteSnapshot?.toMilligrams()} mg · ${record.costLabel()}"
                    } else {
                        "Ürün değerleri bilinmiyor · ${record.costLabel()}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, "Kaydı sil", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordDetailSheet(
    record: SmokingRecordEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.padding(15.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(record.productNameSnapshot, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${record.localDate().format(DateTimeFormatter.ofPattern("d MMMM yyyy", TURKISH))} · ${record.formattedTime()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Bu kaydın izi", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Metric("${record.quantity}", "adet", Modifier.weight(1f))
                        Metric("${record.consumedQuarter * 25}%", "içilen", Modifier.weight(1f))
                        Metric(record.costLabel(), "maliyet", Modifier.weight(1f))
                    }
                    record.nicotineMicrogramsPerCigaretteSnapshot?.let {
                        Text(
                            "Nikotin ${it.toMilligrams()} mg · Tar ${record.tarMicrogramsPerCigaretteSnapshot?.toMilligrams() ?: "—"} mg · CO ${record.carbonMonoxideMicrogramsPerCigaretteSnapshot?.toMilligrams() ?: "—"} mg",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            DetailLine("İstek seviyesi", record.cravingLevel?.let { "$it / 5" })
            DetailLine("Tetikleyici", record.trigger)
            DetailLine("Ruh hali", record.mood)
            DetailLine("Konum", record.locationType)
            DetailLine("Not", record.note)
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Text(" Düzenle")
                }
                FilledTonalButton(onClick = onDuplicate, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    Text(" Çoğalt")
                }
            }
            OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                Text(" Kaydı sil")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(12.dp)) {
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecordEditorSheet(
    editor: RecordEditorState,
    products: List<CigaretteProductEntity>,
    onUpdate: ((RecordEditorState) -> RecordEditorState) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        when (editor.mode) {
                            RecordEditorMode.CREATE -> "Geçmiş kayıt ekle"
                            RecordEditorMode.EDIT -> "Kaydı düzenle"
                            RecordEditorMode.DUPLICATE -> "Kaydı çoğalt"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        "O anı birkaç dokunuşla ayrıntılandır",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Kapat") }
            }
            SectionTitle("Ne zaman?")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editor.date,
                    onValueChange = { value -> onUpdate { it.copy(date = value) } },
                    modifier = Modifier.weight(1.4f),
                    label = { Text("YYYY-AA-GG") },
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = editor.time,
                    onValueChange = { value -> onUpdate { it.copy(time = value) } },
                    modifier = Modifier.weight(1f),
                    label = { Text("SS:DD") },
                    leadingIcon = { Icon(Icons.Outlined.Schedule, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
            SectionTitle("Ürün")
            val selectableProducts = products.filter { !it.isArchived || it.id == editor.productId }
            if (selectableProducts.isEmpty()) {
                Text("Önce Ayarlar’dan bir ürün oluşturmalısın.", color = MaterialTheme.colorScheme.error)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectableProducts.forEach { product ->
                        FilterChip(
                            selected = editor.productId == product.id,
                            onClick = { onUpdate { it.copy(productId = product.id) } },
                            label = {
                                Text(
                                    product.name + if (product.isArchived) " · arşivde" else "",
                                )
                            },
                        )
                    }
                }
            }
            SectionTitle("Miktar")
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = {
                        onUpdate { it.copy(quantity = (it.quantity - 1).coerceAtLeast(1)) }
                    }) { Icon(Icons.Outlined.Remove, "Azalt") }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${editor.quantity}", style = MaterialTheme.typography.headlineSmall)
                        Text("adet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        onUpdate { it.copy(quantity = (it.quantity + 1).coerceAtMost(99)) }
                    }) { Icon(Icons.Outlined.Add, "Artır") }
                }
            }
            ChoiceRow(
                title = "İçilen oran",
                options = (1..4).map { it to "${it * 25}%" },
                selected = editor.consumedQuarter,
                onSelect = { value -> onUpdate { it.copy(consumedQuarter = value) } },
            )
            ChoiceRow(
                title = "İstek seviyesi",
                options = listOf(null to "Yok") + (1..5).map { it to "$it" },
                selected = editor.cravingLevel,
                onSelect = { value -> onUpdate { it.copy(cravingLevel = value) } },
            )
            StringChoices("Tetikleyici", RecordsViewModel.TRIGGERS, editor.trigger) { value ->
                onUpdate { it.copy(trigger = value) }
            }
            StringChoices("Ruh hali", RecordsViewModel.MOODS, editor.mood) { value ->
                onUpdate { it.copy(mood = value) }
            }
            StringChoices("Konum", RecordsViewModel.LOCATIONS, editor.locationType) { value ->
                onUpdate { it.copy(locationType = value) }
            }
            OutlinedTextField(
                value = editor.note,
                onValueChange = { value -> onUpdate { it.copy(note = value.take(500)) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Not (isteğe bağlı)") },
                supportingText = { Text("${editor.note.length}/500") },
                minLines = 3,
                shape = RoundedCornerShape(20.dp),
            )
            editor.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onSave,
                enabled = !editor.isSaving && selectableProducts.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                if (editor.isSaving) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (editor.mode == RecordEditorMode.EDIT) "Değişiklikleri kaydet" else "Kaydı ekle")
                }
            }
            Text(
                "Ürün ve tarih değişirse o tarihte geçerli fiyat/değer snapshot’ı kullanılır. Geçmiş kayıtlar sonraki fiyat değişikliklerinden etkilenmez.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceRow(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    SectionTitle(title)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StringChoices(
    title: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    SectionTitle(title)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("Belirtilmedi") },
        )
        options.forEach { value ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(value) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecordFiltersSheet(
    state: RecordsUiState,
    onDismiss: () -> Unit,
    onPeriod: (RecordPeriod) -> Unit,
    onProduct: (String?) -> Unit,
    onTrigger: (String?) -> Unit,
    onMood: (String?) -> Unit,
    onUnknown: (Boolean) -> Unit,
    onNotes: (Boolean) -> Unit,
    onStartDate: (String) -> Unit,
    onEndDate: (String) -> Unit,
    onClear: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kayıtları süz", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${state.filters.activeCount} aktif filtre",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onClear) { Text("Temizle") }
            }
            SectionTitle("Tarih aralığı")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = state.filters.period == period,
                        onClick = { onPeriod(period) },
                        label = { Text(period.label) },
                    )
                }
            }
            if (state.filters.period == RecordPeriod.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.filters.startDate,
                        onValueChange = onStartDate,
                        modifier = Modifier.weight(1f),
                        label = { Text("Başlangıç") },
                        placeholder = { Text("YYYY-AA-GG") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.filters.endDate,
                        onValueChange = onEndDate,
                        modifier = Modifier.weight(1f),
                        label = { Text("Bitiş") },
                        placeholder = { Text("YYYY-AA-GG") },
                        singleLine = true,
                    )
                }
            }
            SectionTitle("Ürün")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.filters.productId == null,
                    onClick = { onProduct(null) },
                    label = { Text("Tümü") },
                )
                state.products.forEach { product ->
                    FilterChip(
                        selected = state.filters.productId == product.id,
                        onClick = { onProduct(product.id) },
                        label = { Text(product.name) },
                    )
                }
            }
            FilterStringSection("Tetikleyici", RecordsViewModel.TRIGGERS, state.filters.trigger, onTrigger)
            FilterStringSection("Ruh hali", RecordsViewModel.MOODS, state.filters.mood, onMood)
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Eksik / bilinmeyen veriler")
                        Text(
                            "Ürünü veya kaynak snapshot’ı eksik kayıtlar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.filters.unknownOnly, onCheckedChange = onUnknown)
                }
            }
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Yalnızca not içerenler")
                        Text(
                            "Kişisel not eklenmiş kayıtları göster",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.filters.notesOnly, onCheckedChange = onNotes)
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("${state.totalCount} adetlik sonucu göster")
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilters(
    filters: RecordFilters,
    products: List<CigaretteProductEntity>,
    onClear: () -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (filters.period != RecordPeriod.ALL) {
            FilterChip(true, {}, label = {
                Text(
                    if (filters.period == RecordPeriod.CUSTOM) {
                        "${filters.startDate.ifBlank { "…" }} → ${filters.endDate.ifBlank { "…" }}"
                    } else {
                        filters.period.label
                    },
                )
            })
        }
        filters.productId?.let { id ->
            FilterChip(true, {}, label = { Text(products.firstOrNull { it.id == id }?.name ?: "Ürün") })
        }
        filters.trigger?.let { FilterChip(true, {}, label = { Text(it) }) }
        filters.mood?.let { FilterChip(true, {}, label = { Text(it) }) }
        if (filters.unknownOnly) FilterChip(true, {}, label = { Text("Eksik değer") })
        if (filters.notesOnly) FilterChip(true, {}, label = { Text("Notlu") })
        TextButton(onClick = onClear) { Text("Tümünü temizle") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterStringSection(
    title: String,
    values: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    SectionTitle(title)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Tümü") })
        values.forEach { value ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(value) })
        }
    }
}

@Composable
private fun SectionTitle(value: String) {
    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptyRecords(filtered: Boolean, onAdd: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (filtered) "Eşleşen kayıt yok" else "Henüz kayıt oluşturmadın",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                if (filtered) "Arama veya filtreleri değiştirerek tekrar deneyebilirsin."
                else "Şimdi ya da geçmiş bir ana ait ilk izini ekleyebilirsin.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!filtered) {
                FilledTonalButton(onClick = onAdd) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text(" İlk kaydı ekle")
                }
            }
        }
    }
}

private fun SmokingRecordEntity.localDate(): LocalDate =
    Instant.ofEpochMilli(smokedAtEpochMillis)
        .atZone(runCatching { ZoneId.of(zoneIdSnapshot) }.getOrDefault(ZoneId.systemDefault()))
        .toLocalDate()

private fun SmokingRecordEntity.formattedTime(): String =
    Instant.ofEpochMilli(smokedAtEpochMillis)
        .atZone(runCatching { ZoneId.of(zoneIdSnapshot) }.getOrDefault(ZoneId.systemDefault()))
        .format(DateTimeFormatter.ofPattern("HH:mm"))

private fun SmokingRecordEntity.costLabel(): String {
    val price = priceMicrosPerCigaretteSnapshot ?: return "—"
    val total = price.toDouble() * quantity * consumedQuarter / 4.0 / 1_000_000.0
    return runCatching {
        NumberFormat.getCurrencyInstance(TURKISH).apply {
            currency = Currency.getInstance(currencyCodeSnapshot)
            maximumFractionDigits = 2
        }.format(total)
    }.getOrElse { "%.2f".format(TURKISH, total) }
}

private fun List<SmokingRecordEntity>.totalCostLabel(): String {
    val known = filter { it.priceMicrosPerCigaretteSnapshot != null }
    if (known.isEmpty()) return "maliyet bilinmiyor"
    val currency = known.first().currencyCodeSnapshot
    val total = known.sumOf {
        checkNotNull(it.priceMicrosPerCigaretteSnapshot).toDouble() *
            it.quantity *
            it.consumedQuarter /
            4.0 /
            1_000_000.0
    }
    val label = runCatching {
        NumberFormat.getCurrencyInstance(TURKISH).apply {
            this.currency = Currency.getInstance(currency)
            maximumFractionDigits = 2
        }.format(total)
    }.getOrElse { "%.2f".format(TURKISH, total) }
    return if (known.size == size) label else "$label · ${known.size}/$size fiyatlı"
}

private fun SmokingRecordEntity.hasEmissionSnapshot(): Boolean =
    nicotineMicrogramsPerCigaretteSnapshot != null &&
        tarMicrogramsPerCigaretteSnapshot != null &&
        carbonMonoxideMicrogramsPerCigaretteSnapshot != null

private fun List<SmokingRecordEntity>.emissionCoverageCount(): Int = count { it.hasEmissionSnapshot() }

private fun Long.toMilligrams(): String =
    if (this % 1_000L == 0L) (this / 1_000L).toString() else "%.2f".format(TURKISH, this / 1_000.0)

private val TURKISH = Locale.forLanguageTag("tr-TR")
