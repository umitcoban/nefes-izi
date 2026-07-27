package com.umityasincoban.nefesizi.feature.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.umityasincoban.nefesizi.feature.onboarding.ProductFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormSheet(
    state: ProductFormState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onNicotineChange: (String) -> Unit,
    onTarChange: (String) -> Unit,
    onCarbonMonoxideChange: (String) -> Unit,
    onSave: () -> Unit,
    saveLabel: String,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Hızlı kayıt ürünün", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Sadece ad gerekli. Paketindeki emisyon değerlerini biliyorsan ekleyebilirsin.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text("Ürün adı") },
                placeholder = { Text("Örn. Günlük sigaram") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = state.error != null && state.name.isBlank(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DecimalField(
                    value = state.nicotineMg,
                    onValueChange = onNicotineChange,
                    label = "Nikotin",
                    modifier = Modifier.weight(1f),
                )
                DecimalField(
                    value = state.tarMg,
                    onValueChange = onTarChange,
                    label = "Katran",
                    modifier = Modifier.weight(1f),
                )
                DecimalField(
                    value = state.carbonMonoxideMg,
                    onValueChange = onCarbonMonoxideChange,
                    label = "CO",
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                "Bu değerler kişisel emilim değil, sigara başı tahmini duman emisyonudur.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(2.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(saveLabel)
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Şimdi değil")
            }
        }
    }
}

@Composable
private fun DecimalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text("mg") },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}
