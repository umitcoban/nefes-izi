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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.umityasincoban.nefesizi.core.data.ThemeMode
import com.umityasincoban.nefesizi.core.database.CigaretteProductEntity
import com.umityasincoban.nefesizi.feature.onboarding.OnboardingViewModel
import com.umityasincoban.nefesizi.feature.products.ProductFormSheet

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    productViewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val productForm by productViewModel.form.collectAsState()
    var showProductSheet by remember { mutableStateOf(false) }
    val productCount = state.products.size
    LaunchedEffect(productCount) {
        if (productCount > 0) showProductSheet = false
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
                    state.products.forEach { product ->
                        ProductRow(product, onMakeDefault = { viewModel.setDefaultProduct(product) })
                    }
                }
                Button(onClick = { showProductSheet = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text("Yeni ürün ekle", modifier = Modifier.padding(start = 8.dp))
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

    if (showProductSheet) {
        ProductFormSheet(
            state = productForm,
            onDismiss = { showProductSheet = false },
            onNameChange = productViewModel::updateName,
            onNicotineChange = productViewModel::updateNicotine,
            onTarChange = productViewModel::updateTar,
            onCarbonMonoxideChange = productViewModel::updateCarbonMonoxide,
            onSave = productViewModel::saveProductAndContinue,
            saveLabel = "Ürünü kaydet",
        )
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
private fun ProductRow(product: CigaretteProductEntity, onMakeDefault: () -> Unit) {
    Surface(
        onClick = onMakeDefault,
        shape = RoundedCornerShape(18.dp),
        color = if (product.isDefault) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (product.isDefault) "Varsayılan hızlı kayıt ürünü" else "Varsayılan yapmak için dokun",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (product.isDefault) {
                Icon(Icons.Outlined.Check, contentDescription = "Varsayılan ürün")
            }
        }
    }
}
