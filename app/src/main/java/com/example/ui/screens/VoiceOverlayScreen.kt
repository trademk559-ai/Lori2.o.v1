package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.modules.voice.VoiceState
import com.example.ui.components.LoriOrb
import com.example.ui.components.LoriVoiceWaveform
import com.example.ui.components.WhatsAppConfirmationCard
import com.example.ui.theme.LoriCyanSecondary
import com.example.ui.theme.LoriIndigoPrimary
import com.example.viewmodel.LoriMainViewModel

@Composable
fun VoiceOverlayScreen(
    viewModel: LoriMainViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val voiceState by viewModel.voiceState.collectAsState()
    val rmsDb by viewModel.rmsDb.collectAsState()
    val liveText by viewModel.liveSpokenText.collectAsState()
    val statusMsg by viewModel.voiceStatusMessage.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val pendingWhatsAppDraft by viewModel.pendingWhatsAppDraft.collectAsState()
    val allMessages by viewModel.allMessages.collectAsState()

    val lastAssistantMessage = allMessages.lastOrNull { it.role == "assistant" }?.text

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141218),
                        Color(0xFF1D192B),
                        Color(0xFF2B213A)
                    )
                )
            )
            .testTag("voice_overlay_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Close button & Continuous Mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Continuous Mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = settings.isContinuousVoiceMode,
                        onCheckedChange = { isEnabled ->
                            viewModel.updateSettings { it.copy(isContinuousVoiceMode = isEnabled) }
                        }
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .testTag("voice_overlay_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Voice Overlay",
                        tint = Color.White
                    )
                }
            }

            // Central Dialogue Bubbles & Animated Lori Orb
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live Spoken User Transcript Bubble
                AnimatedVisibility(
                    visible = liveText.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF6750A4).copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .fillMaxWidth(0.9f)
                    ) {
                        Text(
                            text = "“$liveText”",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Central Glowing Orb
                LoriOrb(
                    voiceState = voiceState,
                    rmsDb = rmsDb,
                    onClick = {
                        when (voiceState) {
                            VoiceState.SPEAKING -> viewModel.stopSpeaking()
                            VoiceState.LISTENING -> viewModel.stopVoiceInteraction()
                            else -> viewModel.startVoiceInteraction()
                        }
                    },
                    size = 170.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Voice State Title
                Text(
                    text = when (voiceState) {
                        VoiceState.LISTENING -> "Lori sun rahi hai..."
                        VoiceState.SPEAKING -> "Lori bol rahi hai..."
                        VoiceState.PROCESSING -> "Lori samajh rahi hai..."
                        else -> "Tap Orb or speak “Hey ${settings.wakePhrase}”"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD0BCFF)
                )

                Text(
                    text = statusMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCAC4D0)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Lottie-based Live Animated Voice Waveform Visualizer
                LoriVoiceWaveform(
                    voiceState = voiceState,
                    rmsDb = rmsDb,
                    height = 76.dp,
                    showLabel = true,
                    modifier = Modifier.fillMaxWidth(0.92f)
                )

                // Assistant Response Preview if spoken
                if (!lastAssistantMessage.isNullOrBlank() && voiceState == VoiceState.SPEAKING) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        Text(
                            text = lastAssistantMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(14.dp),
                            maxLines = 4
                        )
                    }
                }

                // WhatsApp Confirmation in Voice Mode
                if (pendingWhatsAppDraft != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    WhatsAppConfirmationCard(
                        draft = pendingWhatsAppDraft!!,
                        onConfirm = { viewModel.confirmSendWhatsApp() },
                        onCancel = { viewModel.cancelWhatsAppDraft() }
                    )
                }
            }

            // Bottom Voice Action Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (voiceState == VoiceState.SPEAKING) {
                    Button(
                        onClick = { viewModel.stopSpeaking() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.testTag("voice_stop_speaking_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Stop, contentDescription = "Stop", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lori ko roko (Stop Speaking)", color = Color.White)
                    }
                } else if (voiceState == VoiceState.LISTENING) {
                    OutlinedButton(
                        onClick = { viewModel.stopVoiceInteraction() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("voice_cancel_listening_button")
                    ) {
                        Text("Listening Band Karein", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = { viewModel.startVoiceInteraction() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        modifier = Modifier.testTag("voice_start_listening_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Mic, contentDescription = "Speak", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Boliye (Tap to Talk)", color = Color.White)
                    }
                }
            }
        }
    }
}
