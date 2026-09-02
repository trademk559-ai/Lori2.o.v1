package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.modules.voice.VoiceState
import com.example.ui.components.LoriOrb
import com.example.ui.components.LoriVoiceWaveform
import com.example.ui.components.WhatsAppConfirmationCard
import com.example.ui.theme.LoriCallBlue
import com.example.ui.theme.LoriCyanSecondary
import com.example.ui.theme.LoriIndigoPrimary
import com.example.ui.theme.LoriWhatsAppGreen
import com.example.ui.theme.LoriYouTubeRed
import com.example.viewmodel.LoriMainViewModel

@Composable
fun HomeScreen(
    viewModel: LoriMainViewModel,
    onNavigateToTab: (Int) -> Unit,
    onOpenVoiceOverlay: () -> Unit,
    onOpenSpecificTool: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val voiceState by viewModel.voiceState.collectAsState()
    val rmsDb by viewModel.rmsDb.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isBgRunning by viewModel.isBackgroundServiceRunning.collectAsState()
    val pendingWhatsAppDraft by viewModel.pendingWhatsAppDraft.collectAsState()

    val quickCommands = listOf(
        "Lori, YouTube kholo aur Arijit Singh ka gana chalao 🎵",
        "Lori, internet par dekh ke bata aaj ka mausam 🌦️",
        "Lori, Rahul ko reply kar do 'Haan shaam ko milte hain' 💬",
        "Lori, last notification padh ke sunao 🔔",
        "Lori, koi tagda workout song chalao ⚡",
        "Lori, kya haal hai? 😄"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_lazy_column"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Hero Header Section
        item {
            HeroHeader(
                isWakeWordActive = settings.isWakeWordEnabled,
                wakeWord = settings.wakePhrase,
                isBgActive = isBgRunning
            )
        }

        // WhatsApp Pending Confirmation Card if active
        if (pendingWhatsAppDraft != null) {
            item {
                WhatsAppConfirmationCard(
                    draft = pendingWhatsAppDraft!!,
                    onConfirm = { viewModel.confirmSendWhatsApp() },
                    onCancel = { viewModel.cancelWhatsAppDraft() }
                )
            }
        }

        // Central Voice Orb Interactive Area
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (voiceState) {
                        VoiceState.LISTENING -> "Lori sun rahi hai... Boliye!"
                        VoiceState.SPEAKING -> "Lori bol rahi hai..."
                        VoiceState.PROCESSING -> "Lori samajh rahi hai..."
                        else -> "Tap to speak with Lori"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Ya bolein: “Hey ${settings.wakePhrase}”",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                LoriOrb(
                    voiceState = voiceState,
                    rmsDb = rmsDb,
                    onClick = {
                        if (voiceState == VoiceState.SPEAKING) {
                            viewModel.stopSpeaking()
                        } else if (voiceState == VoiceState.LISTENING) {
                            viewModel.stopVoiceInteraction()
                        } else {
                            viewModel.startVoiceInteraction()
                        }
                    },
                    size = 150.dp
                )

                if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LoriVoiceWaveform(
                        voiceState = voiceState,
                        rmsDb = rmsDb,
                        height = 68.dp,
                        showLabel = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                }
            }
        }

        // Quick Conversational Suggestions
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Try Saying / Koshish Karein",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickCommands) { cmd ->
                        SuggestionChip(
                            onClick = {
                                 viewModel.sendTextMessage(cmd)
                                onNavigateToTab(1) // Go to Chat
                            },
                            label = { Text(cmd, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = Color.White.copy(alpha = 0.5f),
                                borderWidth = 1.dp
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }
        }

        // Main Features Grid (Large touch targets for one-handed use)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Lori Capabilities / Suvidhayein",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2-Column Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        title = "AI Chat",
                        subtitle = "Hindi & Hinglish",
                        icon = Icons.Filled.Chat,
                        color = LoriIndigoPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(1) }
                    )
                    FeatureCard(
                        title = "Voice Mode",
                        subtitle = "Continuous Talk",
                        icon = Icons.Filled.GraphicEq,
                        color = LoriCyanSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenVoiceOverlay
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        title = "Internet Search",
                        subtitle = "Live Grounding",
                        icon = Icons.Filled.Search,
                        color = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(2) }
                    )
                    FeatureCard(
                        title = "WhatsApp Reply",
                        subtitle = "Voice Assistant",
                        icon = Icons.Filled.Send,
                        color = LoriWhatsAppGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSpecificTool("whatsapp") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        title = "Calls Assistant",
                        subtitle = "Announce & Control",
                        icon = Icons.Filled.Call,
                        color = LoriCallBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSpecificTool("call") }
                    )
                    FeatureCard(
                        title = "YouTube Music",
                        subtitle = "Songs & Playlists",
                        icon = Icons.Filled.PlayArrow,
                        color = LoriYouTubeRed,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSpecificTool("youtube") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        title = "Privacy Control",
                        subtitle = "Telemetry & Kill Switch",
                        icon = Icons.Filled.Security,
                        color = Color(0xFFF43F5E),
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenSpecificTool("privacy") }
                    )
                    FeatureCard(
                        title = "Settings",
                        subtitle = "Preferences & Voice",
                        icon = Icons.Filled.Settings,
                        color = Color(0xFF64748B),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(4) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroHeader(
    isWakeWordActive: Boolean,
    wakeWord: String,
    isBgActive: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF21005D),
                        Color(0xFF381E72),
                        Color(0xFF4F378B)
                    )
                )
            )
    ) {
        // Hero Background Illustration
        Image(
            painter = painterResource(id = R.drawable.lori_hero_banner),
            contentDescription = "Lori Visual Banner",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.28f
        )

        // Overlay Header Details
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_lori_avatar),
                        contentDescription = "Lori Avatar",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFFD0BCFF), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Lori",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "“Tum bolo, Lori samjhe.”",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFEADDFF)
                        )
                    }
                }

                // Active Status Badges with Frosted Glass look
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isWakeWordActive) Color(0xFFD0BCFF).copy(alpha = 0.25f) else Color.DarkGray.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "“$wakeWord” active",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Subtitle Guidance Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Aapki personal Hindi/Hinglish AI Voice Assistant",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF3EDF7)
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick)
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .testTag("feature_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f))
                    .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
