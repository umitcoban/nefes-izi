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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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

@Composable
fun SettingsScreen(
    onOpenProducts: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

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
                    Text("Sürüm 1.0 · local-first kişisel günlük")
                    Text(
                        "Tıbbi teşhis veya tedavi amacı taşımaz.",
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.75f),
                    )
                }
            }
        }
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
