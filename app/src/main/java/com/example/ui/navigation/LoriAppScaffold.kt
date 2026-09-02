package com.example.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.screens.CallAssistantScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NotificationsFeedScreen
import com.example.ui.screens.PrivacyDashboardScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceOverlayScreen
import com.example.ui.screens.WhatsAppAssistantScreen
import com.example.ui.screens.YouTubeAssistantScreen
import com.example.ui.theme.LoriIndigoPrimary
import com.example.ui.theme.LoriVoiceGlowCyan
import com.example.viewmodel.LoriMainViewModel

sealed class NavigationTab(val index: Int, val title: String, val icon: ImageVector) {
    object Home : NavigationTab(0, "Home", Icons.Filled.Home)
    object Chat : NavigationTab(1, "Chat", Icons.Filled.Chat)
    object Search : NavigationTab(2, "Search", Icons.Filled.Search)
    object Notifications : NavigationTab(3, "Alerts", Icons.Filled.Notifications)
    object Settings : NavigationTab(4, "Settings", Icons.Filled.Settings)
}

@Composable
fun LoriAppScaffold(
    viewModel: LoriMainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isVoiceOverlayActive by viewModel.isVoiceOverlayActive.collectAsState()

    var activeSubTool by remember { mutableStateOf<String?>(null) } // "whatsapp", "call", "youtube", "privacy"

    // If not authenticated, show Single User Login Screen
    if (!authState.isAuthenticated) {
        LoginScreen(
            authManager = viewModel.authManager,
            onLoginSuccess = { /* Automatically updates state */ }
        )
        return
    }

    // Permission Launchers
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissions()
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val ungranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    shadowElevation = 12.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("lori_bottom_navigation_bar")
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        val tabs = listOf(
                            NavigationTab.Home,
                            NavigationTab.Chat,
                            NavigationTab.Search,
                            NavigationTab.Notifications,
                            NavigationTab.Settings
                        )

                        tabs.forEach { tab ->
                            val isSelected = selectedTab == tab.index && activeSubTool == null
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    activeSubTool = null
                                    viewModel.setSelectedTab(tab.index)
                                },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_item_${tab.title.lowercase()}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.startVoiceInteraction() },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 12.dp),
                    modifier = Modifier.testTag("fab_voice_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Voice Assistant",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (activeSubTool != null) {
                    when (activeSubTool) {
                        "whatsapp" -> WhatsAppAssistantScreen(
                            viewModel = viewModel,
                            onBack = { activeSubTool = null }
                        )
                        "call" -> CallAssistantScreen(
                            viewModel = viewModel
                        )
                        "youtube" -> YouTubeAssistantScreen(
                            viewModel = viewModel
                        )
                        "privacy" -> PrivacyDashboardScreen(
                            viewModel = viewModel,
                            onBack = { activeSubTool = null }
                        )
                    }
                } else {
                    when (selectedTab) {
                        0 -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToTab = { tab -> viewModel.setSelectedTab(tab) },
                            onOpenVoiceOverlay = { viewModel.startVoiceInteraction() },
                            onOpenSpecificTool = { tool -> activeSubTool = tool }
                        )
                        1 -> ChatScreen(
                            viewModel = viewModel,
                            onOpenVoiceOverlay = { viewModel.startVoiceInteraction() }
                        )
                        2 -> SearchScreen(
                            viewModel = viewModel
                        )
                        3 -> NotificationsFeedScreen(
                            viewModel = viewModel
                        )
                        4 -> SettingsScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // Fullscreen Immersive Voice Mode Overlay
        AnimatedVisibility(
            visible = isVoiceOverlayActive,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            VoiceOverlayScreen(
                viewModel = viewModel,
                onDismiss = { viewModel.stopVoiceInteraction() }
            )
        }
    }
}
