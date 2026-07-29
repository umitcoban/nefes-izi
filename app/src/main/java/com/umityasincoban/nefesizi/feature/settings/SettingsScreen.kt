package com.umityasincoban.nefesizi.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.umityasincoban.nefesizi.core.backup.ImportMode
import android.Manifest
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.umityasincoban.nefesizi.core.data.ThemeMode
import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.BuildConfig
import com.umityasincoban.nefesizi.MainActivity

@Composable
fun SettingsScreen(
    onOpenProducts: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val backupState by backupViewModel.state.collectAsState()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.setEveningNotification(granted) }
    val weeklyPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.setWeeklyNotification(granted) }
    val inactivityPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.setInactivityNotification(granted) }
    val biometricPrompt = remember(context) {
        val activity = context as MainActivity
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    activity.markBiometricAuthenticated()
                    viewModel.setBiometricLock(true)
                }
            },
        )
    }
    val biometricPromptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Nefes İzi uygulama kilidi")
            .setSubtitle("Bu özellik yalnızca uygulamaya erişimi sınırlar; veritabanını şifrelemez.")
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()
    }
    val jsonExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(backupViewModel::exportJson) }
    val csvExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let(backupViewModel::exportCsv) }
    val importFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(backupViewModel::previewImport) }

    LaunchedEffect(Unit) {
        backupViewModel.effects.collect { snackbarHostState.showSnackbar(it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Ayarlar", style = MaterialTheme.typography.headlineLarge)
            Text("Görünüm, ürünler ve gizlilik", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SettingsCard(Icons.Outlined.Palette, "Görünüm") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> "Sistem"
                                        ThemeMode.LIGHT -> "Açık"
                                        ThemeMode.DARK -> "Koyu"
                                    },
                                )
                            },
                        )
                    }
                }
                PreferenceSwitch(
                    title = "Dinamik renk",
                    supporting = "Desteklenen cihazlarda sistem renklerini kullan",
                    checked = state.personalization.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor,
                )
            }
        }
        item {
            SettingsCard(Icons.Outlined.Palette, "Bugün ekranı") {
                PreferenceSwitch(
                    title = "Tahmini maliyet",
                    supporting = "Günlük maliyet kartını göster",
                    checked = state.todayDisplay.showCost,
                    onCheckedChange = viewModel::setShowTodayCost,
                )
                PreferenceSwitch(
                    title = "Tahmini emisyonlar",
                    supporting = "Nikotin, katran ve CO kartlarını göster",
                    checked = state.todayDisplay.showExposure,
                    onCheckedChange = viewModel::setShowTodayExposure,
                )
                OutlinedTextField(
                    value = state.wakeTime,
                    onValueChange = viewModel::setWakeTime,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Uyanma saati (isteğe bağlı)") },
                    placeholder = { Text("07:30") },
                    supportingText = {
                        Text("Analizde ilk kayda kadar süreyi hesaplamak için kullanılır.")
                    },
                    singleLine = true,
                )
                PreferenceSwitch(
                    title = "Sağlık sekmesi",
                    supporting = "Alt navigasyonda sağlık günlüğünü göster",
                    checked = state.personalization.showHealthTab,
                    onCheckedChange = viewModel::setShowHealthTab,
                )
                Text("Gün başlangıcı", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(0, 4, 6).forEach { hour ->
                        FilterChip(
                            selected = state.personalization.dayStartHour == hour,
                            onClick = { viewModel.setDayStartHour(hour) },
                            label = { Text("%02d:00".format(hour)) },
                        )
                    }
                }
                Text("Haftanın ilk günü", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("MONDAY" to "Pazartesi", "SUNDAY" to "Pazar").forEach { (value, label) ->
                        FilterChip(
                            selected = state.personalization.firstDayOfWeek == value,
                            onClick = { viewModel.setFirstDayOfWeek(value) },
                            label = { Text(label) },
                        )
                    }
                }
                Text("Tercih edilen para birimi", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("TRY", "EUR", "USD").forEach { code ->
                        FilterChip(
                            selected = state.personalization.preferredCurrency == code,
                            onClick = { viewModel.setCurrency(code) },
                            label = { Text(code) },
                        )
                    }
                }
                Text(
                    "Bu tercih geçmiş kayıtların para birimi snapshot’larını dönüştürmez.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SettingsCard(Icons.Outlined.Storage, "Sigara ürünleri") {
                if (state.products.isEmpty()) {
                    Text(
                        "Hızlı kayıt için ilk ürününü ekleyebilirsin.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.products.firstOrNull { it.isDefault }?.let { ProductRow(it) }
                    Text(
                        "${state.products.size} aktif ürün · fiyat ve değer geçmişi korunur",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onOpenProducts, modifier = Modifier.fillMaxWidth()) {
                    Text("Ürünleri yönet")
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
        item {
            SettingsCard(Icons.Outlined.Lock, "Gizlilik") {
                Text("Hesap yok · İnternet izni yok · Analitik yok")
                Text(
                    "Kayıtların yalnızca bu cihazdaki Room veritabanında tutulur. Android bulut yedeklemesi kapalıdır.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PreferenceSwitch(
                    title = "Uygulama kilidi",
                    supporting = if (state.biometricSupported) {
                        "Arka plandan dönüşte biyometri veya cihaz kilidi iste"
                    } else {
                        "Bu cihazda uygun biyometri/cihaz kilidi bulunmuyor"
                    },
                    checked = state.personalization.biometricLock,
                    onCheckedChange = { enabled ->
                        if (!enabled) viewModel.setBiometricLock(false)
                        else if (state.biometricSupported) biometricPrompt.authenticate(biometricPromptInfo)
                    },
                )
                Text(
                    "Uygulama kilidi bir erişim kapısıdır; cihazdaki Room verisini ayrıca şifrelediği anlamına gelmez.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SettingsCard(Icons.Outlined.FavoriteBorder, "Bildirimler") {
                PreferenceSwitch(
                    title = "Akşam özeti",
                    supporting = "Günün kayıtlarına bakmak için tarafsız hatırlatma",
                    checked = state.notifications.eveningEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= 33) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setEveningNotification(enabled)
                        }
                    },
                )
                if (state.notifications.eveningEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("20:00", "21:00", "22:00").forEach { value ->
                            FilterChip(
                                selected = state.notifications.eveningTime == value,
                                onClick = { viewModel.setEveningTime(value) },
                                label = { Text(value) },
                            )
                        }
                    }
                }
                PreferenceSwitch(
                    title = "Haftalık özet",
                    supporting = "Pazar akşamı haftalık kayıt özeti",
                    checked = state.notifications.weeklyEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= 33) {
                            weeklyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setWeeklyNotification(enabled)
                        }
                    },
                )
                PreferenceSwitch(
                    title = "Kayıt arası hatırlatma",
                    supporting = "${state.notifications.inactivityDays} gün kayıt olmazsa hatırlat",
                    checked = state.notifications.inactivityEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= 33) {
                            inactivityPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setInactivityNotification(enabled)
                        }
                    },
                )
                Text(
                    "Bildirimler varsayılan olarak kapalıdır ve yargılayıcı hedef mesajları içermez.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SettingsCard(Icons.Outlined.Storage, "Yerel yedekleme") {
                Text(
                    "Dosyalar yalnızca seçtiğin konuma yazılır. Geniş depolama izni kullanılmaz.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { jsonExport.launch("nefes-izi-yedek.json") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !backupState.isWorking,
                ) { Text("JSON yedeği dışa aktar") }
                Button(
                    onClick = { csvExport.launch("nefes-izi-csv.zip") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !backupState.isWorking,
                ) { Text("CSV arşivi dışa aktar") }
                Button(
                    onClick = { importFile.launch(arrayOf("application/json", "text/plain")) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !backupState.isWorking,
                ) { Text("JSON yedeğini içe aktar") }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Nefes İzi", style = MaterialTheme.typography.headlineMedium)
                    Text("Sürüm ${BuildConfig.VERSION_NAME} · local-first kişisel günlük")
                    Text(
                        "Tıbbi teşhis veya tedavi amacı taşımaz.",
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }

    backupState.preview?.let { preview ->
        AlertDialog(
            onDismissRequest = backupViewModel::dismissPreview,
            title = { Text(if (preview.canReplace) "Yedek hazır" else "Yedek içe aktarılamıyor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    preview.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    preview.validation?.let {
                        Text("${it.productCount} ürün · ${it.recordCount} kayıt · ${it.healthEntryCount} sağlık günü")
                        it.errors.forEach { error ->
                            Text("• $error", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Text("${preview.duplicateCount} aynı kayıt atlanacak · ${preview.conflictCount} çakışma")
                    if (preview.conflictCount > 0) {
                        Text("Aynı kimlikte farklı içerik otomatik olarak ezilmez.")
                    }
                }
            },
            confirmButton = {
                if (preview.canMerge) {
                    TextButton(onClick = { backupViewModel.applyImport(ImportMode.MERGE) }) {
                        Text("Birleştir")
                    }
                }
            },
            dismissButton = {
                Row {
                    if (preview.canReplace) {
                        TextButton(onClick = { backupViewModel.applyImport(ImportMode.REPLACE) }) {
                            Text("Yerine koy")
                        }
                    }
                    TextButton(onClick = backupViewModel::dismissPreview) { Text("Vazgeç") }
                }
            },
        )
    }
}

@Composable
private fun PreferenceSwitch(
    title: String,
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp))
                }
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 10.dp))
            }
            content()
        }
    }
}

@Composable
private fun ProductRow(product: CigaretteProductEntity) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Varsayılan hızlı kayıt ürünü",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Outlined.Check, contentDescription = "Varsayılan ürün")
        }
    }
}
