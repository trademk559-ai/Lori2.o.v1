package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.modules.youtube.LoriYouTubeModule
import com.example.ui.theme.LoriYouTubeRed
import com.example.viewmodel.LoriMainViewModel

data class MoodShortcut(
    val title: String,
    val hindiTitle: String,
    val query: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun YouTubeAssistantScreen(
    viewModel: LoriMainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var songQuery by remember { mutableStateOf("") }

    val moodShortcuts = listOf(
        MoodShortcut("Workout / Gym", "Tagda Workout Songs", "energetic gym workout hindi songs", Icons.Filled.FitnessCenter, Color(0xFFEF4444)),
        MoodShortcut("Romantic", "Bollywood Love Songs", "romantic bollywood hindi songs top hits", Icons.Filled.Favorite, Color(0xFFEC4899)),
        MoodShortcut("Party / Dance", "Dhamaka Dance Hits", "bollywood dance party songs latest", Icons.Filled.Celebration, Color(0xFFF59E0B)),
        MoodShortcut("Relax / Lo-Fi", "Calm Acoustic & Chill", "calm relaxing lo-fi hindi songs", Icons.Filled.NightsStay, Color(0xFF8B5CF6)),
        MoodShortcut("Trending", "Superhit Songs", "top trending superhit hindi songs", Icons.Filled.Whatshot, Color(0xFF10B981)),
        MoodShortcut("Devotional", "Bhajan & Aarti", "popular peaceful devotional bhajan hindi", Icons.Filled.SelfImprovement, Color(0xFF06B6D4))
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("youtube_assistant_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = LoriYouTubeRed,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "YouTube Music Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Lori understands your mood and plays the perfect songs directly on YouTube.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Custom Search Input
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Search Artist, Song or Mood",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = songQuery,
                            onValueChange = { songQuery = it },
                            placeholder = { Text("e.g. Arijit Singh latest song") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("youtube_search_input"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LoriYouTubeRed
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (songQuery.isNotBlank()) {
                                    LoriYouTubeModule.openYouTube(context, songQuery)
                                    viewModel.speakMessageAloud("YouTube par $songQuery play kar rahi hoon!")
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LoriYouTubeRed),
                            modifier = Modifier.testTag("youtube_play_button")
                        ) {
                            Text("Play", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Mood & Activity Shortcuts Title
        item {
            Text(
                text = "Instant Mood Playlists / Gaane",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // 2x3 Grid Rows
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (chunk in moodShortcuts.chunked(2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (shortcut in chunk) {
                            ElevatedCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(115.dp)
                                    .clickable {
                                        LoriYouTubeModule.openYouTube(context, shortcut.query)
                                        viewModel.speakMessageAloud("YouTube par ${shortcut.hindiTitle} chala rahi hoon!")
                                    }
                                    .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                                    .testTag("youtube_mood_${shortcut.title.lowercase().replace(" ", "_")}"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        imageVector = shortcut.icon,
                                        contentDescription = shortcut.title,
                                        tint = shortcut.color,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = shortcut.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = shortcut.hindiTitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                        if (chunk.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Voice Command Examples
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Try Saying / Lori se bolein:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• “Lori, YouTube kholo aur Arijit Singh ka gana chalao”", fontSize = 13.sp)
                    Text("• “Lori, koi tagda workout song chalao”", fontSize = 13.sp)
                    Text("• “Lori, apne hisaab se koi achha gana chalao”", fontSize = 13.sp)
                }
            }
        }
    }
}
