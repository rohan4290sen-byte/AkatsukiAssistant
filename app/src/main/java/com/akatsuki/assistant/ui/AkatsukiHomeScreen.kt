package com.akatsuki.assistant.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akatsuki.assistant.ui.theme.AkatsukiBlack
import com.akatsuki.assistant.ui.theme.AkatsukiCard
import com.akatsuki.assistant.ui.theme.AkatsukiRed
import com.akatsuki.assistant.ui.theme.AkatsukiRedDim
import com.akatsuki.assistant.ui.theme.AkatsukiRedGlow
import com.akatsuki.assistant.ui.theme.AkatsukiSurface
import com.akatsuki.assistant.ui.theme.AkatsukiTextPrimary
import com.akatsuki.assistant.ui.theme.AkatsukiTextSecondary

@Composable
fun AkatsukiHomeScreen(
    isListening: Boolean,
    isSpeaking: Boolean,
    statusText: String,
    lastUserQuery: String,
    lastAssistantResponse: String,
    currentLanguage: String,
    isOverlayActive: Boolean,
    apiKey: String,
    onMicClick: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onQuickAction: (String) -> Unit
) {
    var showSettingsDialog by remember { mutableStateOf(false) }
    var tempApiKey by remember { mutableStateOf(apiKey) }

    // Pulsing animation for the Akatsuki Core
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening || isSpeaking) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AkatsukiBlack)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AKATSUKI",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    color = AkatsukiRedGlow,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "OnePlus Nord CE 3 Lite AI System",
                    fontSize = 12.sp,
                    color = AkatsukiTextSecondary
                )
            }

            IconButton(onClick = {
                tempApiKey = apiKey
                showSettingsDialog = true
            }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = AkatsukiTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Center Akatsuki Glowing Core HUD
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp)
        ) {
            // Outer Glow Ring
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AkatsukiRedGlow.copy(alpha = 0.5f),
                                AkatsukiRedDim.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Inner Ring Border
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .border(3.dp, AkatsukiRed, CircleShape)
                    .background(AkatsukiSurface),
                contentAlignment = Alignment.Center
            ) {
                // Interactive Mic Button inside the core
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier.size(110.dp)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Speak to Akatsuki",
                        tint = if (isListening) AkatsukiRedGlow else AkatsukiTextPrimary,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Status Text
        Text(
            text = statusText,
            color = if (isListening || isSpeaking) AkatsukiRedGlow else AkatsukiTextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Conversation Output Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AkatsukiCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Aapne Kaha:",
                    fontSize = 12.sp,
                    color = AkatsukiTextSecondary
                )
                Text(
                    text = if (lastUserQuery.isNotBlank()) "\"$lastUserQuery\"" else "— Tap mic button & speak",
                    fontSize = 16.sp,
                    color = AkatsukiTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Akatsuki Response (Indian Voice):",
                    fontSize = 12.sp,
                    color = AkatsukiRedGlow
                )
                Text(
                    text = if (lastAssistantResponse.isNotBlank()) lastAssistantResponse else "— Akatsuki will respond here.",
                    fontSize = 15.sp,
                    color = AkatsukiTextPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Voice Language Switcher (Hindi IN / English IN)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = AkatsukiSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Clear Indian Voice Accent", color = AkatsukiTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Select Hindi or English Indian pronunciation", color = AkatsukiTextSecondary, fontSize = 12.sp)
                }

                Row {
                    val isHindi = currentLanguage == "hi"
                    Button(
                        onClick = { onLanguageChange("hi") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHindi) AkatsukiRed else AkatsukiCard
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Hindi", color = AkatsukiTextPrimary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { onLanguageChange("en") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isHindi) AkatsukiRed else AkatsukiCard
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("English", color = AkatsukiTextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Floating Screen Overlay Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = AkatsukiSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Floating HUD Overlay", color = AkatsukiTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Display Akatsuki on top of other apps", color = AkatsukiTextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = isOverlayActive,
                    onCheckedChange = onToggleOverlay,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AkatsukiRedGlow,
                        checkedTrackColor = AkatsukiRedDim
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Test Commands
        Text(
            text = "Quick Voice Command Shortcuts:",
            color = AkatsukiTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickChip(label = "🔦 Torch Toggle") { onQuickAction("Torch on ya off karo") }
            QuickChip(label = "🔋 Battery Check") { onQuickAction("Phone ki battery kitni hai") }
            QuickChip(label = "💬 WhatsApp") { onQuickAction("WhatsApp open karo") }
        }
    }

    // Settings Dialog for Gemini API Key
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = AkatsukiCard,
            title = {
                Text("Gemini API Key Setup", color = AkatsukiTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Apna free Google Gemini API key enter karein (Get it from aistudio.google.com):",
                        color = AkatsukiTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        label = { Text("AIzaSy...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AkatsukiTextPrimary,
                            unfocusedTextColor = AkatsukiTextPrimary,
                            focusedBorderColor = AkatsukiRed,
                            unfocusedBorderColor = AkatsukiTextSecondary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveApiKey(tempApiKey)
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AkatsukiRed)
                ) {
                    Text("Save", color = AkatsukiTextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel", color = AkatsukiTextSecondary)
                }
            }
        )
    }
}

@Composable
fun QuickChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AkatsukiCard)
            .border(1.dp, AkatsukiRedDim, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(text = label, color = AkatsukiTextPrimary, fontSize = 12.sp)
    }
}
