package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.LiquidBackdrop
import com.example.ui.theme.LocalGlassPalette

@Composable
fun AppLockOverlay(
    onRequestBiometric: (onError: (String) -> Unit) -> Unit,
    onFallbackUnlock: () -> Unit,
) {
    val P = LocalGlassPalette.current
    var errorText by remember { mutableStateOf<String?>(null) }

    LiquidBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlassCard(
                tint = P.heroYellow,
                cornerRadius = 30.dp,
                padding = PaddingValues(24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(P.obsidian),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = P.heroYellow, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("TakaFlow Locked", style = MaterialTheme.typography.headlineSmall, color = P.obsidian)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Authenticate with biometrics or device PIN to view your finances.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = P.obsidian.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center,
                    )
                    if (errorText != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(errorText!!, style = MaterialTheme.typography.bodyMedium, color = P.coralRed, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(20.dp))
                    GlassPrimaryButton(
                        text = "UNLOCK WITH BIOMETRICS",
                        icon = Icons.Filled.Fingerprint,
                        containerColor = P.obsidian,
                        onClick = {
                            onRequestBiometric { err ->
                                errorText = err
                                if (err.contains("No biometric", ignoreCase = true) || err.contains("unavailable", ignoreCase = true)) {
                                    onFallbackUnlock()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("app_lock_unlock_button"),
                    )
                }
            }
        }
    }
}
