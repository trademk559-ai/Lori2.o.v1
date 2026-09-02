package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.modules.notifications.LoriNotificationManager
import com.example.modules.settings.PermissionHelper
import com.example.ui.theme.LoriCyanSecondary
import com.example.ui.theme.LoriIndigoPrimary
import com.example.viewmodel.LoriMainViewModel

@Composable
fun SettingsScreen(
    viewModel: LoriMainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val permissions by viewModel.permissionStatus.collectAsState()

    var showClearHistoryDialog by remember { mutableStateOf(false) }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear All Assistant Data?") },
            text = { Text("This will permanently delete all chat messages and notification logs.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChat()
                        viewModel.clearNotifications()
                        showClearHistoryDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_all_data")
                ) {
                    Text("Delete Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = LoriIndigoPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lori Settings & Preferences",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Manage Lori's voice triggers, background service, and module permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 1: Voice & Wake Word
        item {
            SettingsCategoryCard(title = "Voice & Wake Word / Aawaz Aur Pehchan") {
                SettingToggleRow(
                    title = "Voice Assistant",
                    subtitle = "Lori can speak answers and notifications aloud",
                    checked = settings.isVoiceAssistantEnabled,
                    icon = Icons.Filled.VolumeUp,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(isVoiceAssistantEnabled = checked) }
                    }
                )

                SettingToggleRow(
                    title = "Wake Word Detection",
                    subtitle = "Responds to “${settings.wakePhrase}”, “Hey ${settings.wakePhrase}”",
                    checked = settings.isWakeWordEnabled,
                    icon = Icons.Filled.Mic,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(isWakeWordEnabled = checked) }
                    }
                )

                // Voice Language Selection (Hindi, Hinglish, English)
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(
                        text = "Voice Recognition Language / Bhasha",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Supports Hindi, mixed Hinglish, and Indian English speech",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val languages = listOf(
                            "hi-IN" to "हिन्दी / Hinglish",
                            "en-IN" to "English (India)",
                            "en-US" to "English (US)"
                        )
                        languages.forEach { (code, label) ->
                            val isSelected = settings.preferredVoiceLang == code
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.updateSettings { it.copy(preferredVoiceLang = code) }
                                },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LoriIndigoPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                SettingToggleRow(
                    title = "Continuous Conversation Mode",
                    subtitle = "Keep listening automatically after Lori finishes speaking",
                    checked = settings.isContinuousVoiceMode,
                    icon = Icons.Filled.GraphicEq,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(isContinuousVoiceMode = checked) }
                    }
                )

                SettingToggleRow(
                    title = "Quiet Mode (Do Not Disturb)",
                    subtitle = "Mute all voice announcements & spoken alerts",
                    checked = settings.isQuietMode,
                    icon = Icons.Filled.NotificationsOff,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(isQuietMode = checked) }
                    }
                )
            }
        }

        // Section 2: Background Service & Battery
        item {
            SettingsCategoryCard(title = "Background Operations / Background Service") {
                SettingToggleRow(
                    title = "Background Mode",
                    subtitle = "Keep Lori active in the background for call & notification alerts",
                    checked = settings.isBackgroundModeEnabled,
                    icon = Icons.Filled.Security,
                    onCheckedChange = { checked ->
                        viewModel.toggleBackgroundService(checked)
                    }
                )

                if (permissions.isBatteryOptimized) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.BatteryAlert, contentDescription = null, tint = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Battery Optimization Active",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To prevent Android from killing Lori in the background, disable battery optimization for Lori.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { PermissionHelper.requestBatteryOptimizationExemption(context) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Disable Battery Optimization")
                        }
                    }
                }
            }
        }

        // Section 3: Feature Modules
        item {
            SettingsCategoryCard(title = "Assistant Modules / Features") {
                SettingToggleRow(
                    title = "Internet Search Grounding",
                    subtitle = "Gemini Google Search for verified facts & news",
                    checked = settings.isInternetSearchEnabled,
                    icon = Icons.Filled.Search,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(isInternetSearchEnabled = checked) }
                    }
                )

                SettingToggleRow(
                    title = "WhatsApp Reply Assistant",
                    subtitle = "Draft polite replies with voice confirmation",
                    checked = settings.isWhatsAppAssistantEnabled,
                    icon = Icons.Filled.Send,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(isWhatsAppAssistantEnabled = checked) }
                    }
                )

                SettingToggleRow(
                    title = "Incoming Call Assistant",
                    subtitle = "Announce caller names and voice accept/reject",
                    checked = settings.isCallAssistantEnabled,
                    icon = Icons.Filled.Call,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(isCallAssistantEnabled = checked) }
                    }
                )

                SettingToggleRow(
                    title = "YouTube Music Assistant",
                    subtitle = "Play songs and playlists based on mood",
                    checked = settings.isYouTubeAssistantEnabled,
                    icon = Icons.Filled.PlayArrow,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(isYouTubeAssistantEnabled = checked) }
                    }
                )
            }
        }

        // Section 4: Notification Voice Alerts
        item {
            SettingsCategoryCard(title = "Notification Voice Settings") {
                SettingToggleRow(
                    title = "Voice Notification Alerts",
                    subtitle = "Speak incoming notifications aloud",
                    checked = settings.isVoiceNotificationAlertsEnabled,
                    icon = Icons.Filled.Notifications,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(isVoiceNotificationAlertsEnabled = checked) }
                    }
                )

                SettingToggleRow(
                    title = "Read Full Message Content",
                    subtitle = "Speak full text of incoming messages",
                    checked = settings.isReadFullNotificationContent,
                    icon = Icons.Filled.RecordVoiceOver,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(isReadFullNotificationContent = checked) }
                    }
                )

                SettingToggleRow(
                    title = "Read App Name",
                    subtitle = "e.g. “WhatsApp par...”, “Instagram par...”",
                    checked = settings.isReadAppName,
                    icon = Icons.Filled.Notifications,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(isReadAppName = checked) }
                    }
                )

                SettingToggleRow(
                    title = "Read Sender Name",
                    subtitle = "e.g. “Rahul ne likha...”",
                    checked = settings.isReadSenderName,
                    icon = Icons.Filled.Notifications,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(isReadSenderName = checked) }
                    }
                )
            }
        }

        // Section 5: Voice Speech Tuning Sliders
        item {
            SettingsCategoryCard(title = "Voice Speech Tuning (TTS)") {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speech Rate (Speed): ${String.format("%.2f", settings.ttsSpeechRate)}x", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = settings.ttsSpeechRate,
                        onValueChange = { rate ->
                            viewModel.updateSettings { it.copy(ttsSpeechRate = rate) }
                        },
                        valueRange = 0.6f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = LoriIndigoPrimary, activeTrackColor = LoriIndigoPrimary)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speech Pitch: ${String.format("%.2f", settings.ttsSpeechPitch)}x", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(
                        value = settings.ttsSpeechPitch,
                        onValueChange = { pitch ->
                            viewModel.updateSettings { it.copy(ttsSpeechPitch = pitch) }
                        },
                        valueRange = 0.7f..1.4f,
                        colors = SliderDefaults.colors(thumbColor = LoriCyanSecondary, activeTrackColor = LoriCyanSecondary)
                    )

                    Button(
                        onClick = {
                            viewModel.speakMessageAloud("Namaste bhai! Main Lori hoon, aapki AI assistant.")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LoriIndigoPrimary),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Lori Voice", color = Color.White)
                    }
                }
            }
        }

        // Section 6: Permissions Checklist
        item {
            SettingsCategoryCard(title = "System Permissions Status") {
                PermissionStatusRow("Microphone (Audio)", permissions.hasAudioPermission)
                PermissionStatusRow("Notifications", permissions.hasNotificationPermission)
                PermissionStatusRow("Notification Listener", permissions.hasNotificationListenerPermission)
                PermissionStatusRow("Phone State & Calls", permissions.hasPhoneStatePermission)
                PermissionStatusRow("Contacts Lookup", permissions.hasContactsPermission)

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { PermissionHelper.openAppSettings(context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Manage Permissions in Android Settings")
                }
            }
        }

        // Section 7: Data & Privacy
        item {
            SettingsCategoryCard(title = "Privacy & Security Telemetry") {
                Text(
                    text = "Lori values your privacy. Sensor controls, background indicators, and the emergency kill switch can be inspected at any time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showClearHistoryDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_all_data_button")
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Chats & Logs", color = Color.White)
                }
            }
        }

        // Section 8: Single User Account & Session
        item {
            val authState by viewModel.authState.collectAsState()
            SettingsCategoryCard(title = "Single User Account & Session") {
                Text(
                    text = "Authorized Phone: ${if (authState.authorizedPhone.isNotBlank()) authState.authorizedPhone else "Enrolled User"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Session security powered by AES-256 encrypted credential storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingToggleRow(
                    title = "Biometric Quick Unlock",
                    subtitle = "Use fingerprint / face unlock for swift authentication",
                    checked = authState.isBiometricEnabled,
                    icon = Icons.Filled.Security,
                    onCheckedChange = { checked ->
                        viewModel.authManager.setBiometricEnabled(checked)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.authManager.logout()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_account_logout")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure Logout")
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: ImageVector,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LoriIndigoPrimary
            )
        )
    }
}

@Composable
private fun PermissionStatusRow(
    permissionName: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = permissionName, style = MaterialTheme.typography.bodyMedium)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isGranted) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isGranted) "Granted" else "Missing",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isGranted) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
