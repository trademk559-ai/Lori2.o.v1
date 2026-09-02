package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.modules.voice.VoiceState
import com.example.ui.theme.LoriCallBlue
import com.example.ui.theme.LoriCyanSecondary
import com.example.ui.theme.LoriIndigoPrimary
import com.example.ui.theme.LoriPinkAccent
import com.example.ui.theme.LoriVoiceGlowCyan
import com.example.viewmodel.LoriMainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardScreen(
    viewModel: LoriMainViewModel,
    onBack: () -> Unit
) {
    val voiceState by viewModel.voiceState.collectAsState()
    val isBgRunning by viewModel.isForegroundServiceRunning.collectAsState()
    val permissions by viewModel.permissionsState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val isMicActive = voiceState == VoiceState.LISTENING

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Privacy & Security Control", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Main Emergency Killswitch Header Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2A1520)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LoriPinkAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(LoriPinkAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PowerSettingsNew,
                                contentDescription = "Kill Switch",
                                tint = LoriPinkAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "EMERGENCY PRIVACY CONTROL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LoriPinkAccent
                        )

                        Text(
                            text = "Immediately stop microphone, background service, voice activation, and all active Lori tasks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE6E1E5),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.stopAllActivity()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LoriPinkAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_stop_all_activity")
                        ) {
                            Icon(Icons.Filled.Block, contentDescription = "Stop All")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("STOP ALL ACTIVITY", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Real-Time Telemetry & Sensors",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Real-Time Sensor Telemetry Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Microphone Telemetry
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMicActive) Color(0xFF1E2B2A) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.Mic,
                                    contentDescription = "Mic",
                                    tint = if (isMicActive) LoriVoiceGlowCyan else Color(0xFFCAC4D0)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isMicActive) LoriVoiceGlowCyan else Color.Gray)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Microphone",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isMicActive) "ACTIVE (Listening)" else "STANDBY / OFF",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMicActive) LoriVoiceGlowCyan else Color(0xFF938F99),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Background Service Telemetry
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isBgRunning) Color(0xFF222036) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.Notifications,
                                    contentDescription = "Background",
                                    tint = if (isBgRunning) LoriIndigoPrimary else Color(0xFFCAC4D0)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isBgRunning) Color(0xFF81C784) else Color.Gray)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Foreground Service",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isBgRunning) "RUNNING" else "STOPPED",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isBgRunning) Color(0xFF81C784) else Color(0xFF938F99),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Permission Status & Access Control",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Permissions Status List
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PermissionStatusRow("Audio Recording (Microphone)", permissions.hasAudioPermission)
                        PermissionStatusRow("Notifications (Alerts)", permissions.hasNotificationPermission)
                        PermissionStatusRow("Notification Listener (WhatsApp Auto-read)", permissions.hasNotificationListenerPermission)
                        PermissionStatusRow("Contacts Access (Call & WhatsApp Name Lookup)", permissions.hasContactsPermission)
                        PermissionStatusRow("Phone Calls & State", permissions.hasPhoneStatePermission)
                    }
                }
            }

            item {
                Text(
                    text = "Security & Telemetry Audit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Last Assistant Activity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date())
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Continuous Voice Activation: ${if (settings.isWakeWordEnabled) "ENABLED (${settings.wakePhrase})" else "OFF"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (settings.isWakeWordEnabled) LoriCyanSecondary else Color(0xFF938F99)
                        )
                        Text(
                            text = "No private credentials or conversation tokens are shared externally.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCAC4D0)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(label: String, isGranted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = if (isGranted) "Granted" else "Not Granted",
                tint = if (isGranted) Color(0xFF81C784) else LoriPinkAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isGranted) "Granted" else "Disabled",
                style = MaterialTheme.typography.labelSmall,
                color = if (isGranted) Color(0xFF81C784) else LoriPinkAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
