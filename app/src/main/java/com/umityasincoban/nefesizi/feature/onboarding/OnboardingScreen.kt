package com.umityasincoban.nefesizi.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.umityasincoban.nefesizi.R
import com.umityasincoban.nefesizi.feature.products.ProductFormSheet

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsState()
    var showProductSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Image(
            painter = painterResource(R.drawable.onboarding_trace_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.58f to MaterialTheme.colorScheme.background.copy(alpha = 0.08f),
                        1f to MaterialTheme.colorScheme.background,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(top = 18.dp, bottom = 22.dp),
        ) {
            Surface(
                color = Color(0xFF09232B).copy(alpha = 0.78f),
                contentColor = Color.White,
                shape = RoundedCornerShape(99.dp),
            ) {
                Text(
                    "NEFES İZİ  •  SADECE SENDE",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Alışkanlığını\nyargılamadan gör.",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Bir dokunuşla kaydet; zamanını, maliyetini ve kendi örüntülerini sakin bir günlükte takip et.",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FeaturePill(Icons.Outlined.Lock, "Cihazında", Modifier.weight(1f))
                FeaturePill(Icons.Outlined.QueryStats, "Örüntüler", Modifier.weight(1f))
                FeaturePill(Icons.Outlined.Tune, "Senin hızın", Modifier.weight(1f))
            }
            Button(
                onClick = { showProductSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("İlk ürünümü ekle")
            }
            TextButton(onClick = viewModel::skip, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Şimdilik atla")
            }
            Text(
                "Tıbbi teşhis veya tedavi amacı taşımaz. Emisyon değerleri tahminidir; gerçek kişisel emilimi göstermez.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showProductSheet) {
        ProductFormSheet(
            state = form,
            onDismiss = { showProductSheet = false },
            onNameChange = viewModel::updateName,
            onNicotineChange = viewModel::updateNicotine,
            onTarChange = viewModel::updateTar,
            onCarbonMonoxideChange = viewModel::updateCarbonMonoxide,
            onSave = viewModel::saveProductAndContinue,
            saveLabel = "Kaydet ve başla",
        )
    }
}

@Composable
private fun FeaturePill(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(18.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
