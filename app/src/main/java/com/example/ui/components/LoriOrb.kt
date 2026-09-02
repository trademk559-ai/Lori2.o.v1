package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.modules.voice.VoiceState
import com.example.ui.theme.LoriCyanSecondary
import com.example.ui.theme.LoriIndigoPrimary
import com.example.ui.theme.LoriVoiceGlowCyan
import com.example.ui.theme.LoriVoiceGlowIndigo
import com.example.ui.theme.LoriVoiceGlowPink
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LoriOrb(
    voiceState: VoiceState,
    rmsDb: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")

    // Idle breathing pulse
    val idleScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdleScale"
    )

    // Speaking glow rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    // Animated dynamic scale based on speech loudness (RMS dB)
    val rmsScale = remember { Animatable(1f) }
    LaunchedEffect(rmsDb, voiceState) {
        if (voiceState == VoiceState.LISTENING) {
            val target = 1.0f + (rmsDb / 10f).coerceIn(0f, 0.4f)
            rmsScale.animateTo(target, tween(100))
        } else {
            rmsScale.snapTo(1f)
        }
    }

    val effectiveScale = when (voiceState) {
        VoiceState.LISTENING -> rmsScale.value
        VoiceState.SPEAKING -> idleScale * 1.06f
        VoiceState.PROCESSING -> idleScale * 0.98f
        else -> idleScale
    }

    val orbGradient = when (voiceState) {
        VoiceState.LISTENING -> Brush.radialGradient(
            colors = listOf(
                Color(0xFFEADDFF),
                Color(0xFFD0BCFF),
                Color(0xFF6750A4),
                Color(0xFF381E72)
            )
        )
        VoiceState.SPEAKING -> Brush.sweepGradient(
            colors = listOf(
                Color(0xFFD0BCFF),
                Color(0xFFF48FB1),
                Color(0xFF9A82DB),
                Color(0xFF6750A4),
                Color(0xFFD0BCFF)
            )
        )
        VoiceState.PROCESSING -> Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD8E4),
                Color(0xFF7D5260),
                Color(0xFF31111D)
            )
        )
        else -> Brush.radialGradient(
            colors = listOf(
                Color(0xFFD0BCFF),
                Color(0xFF6750A4),
                Color(0xFF4F378B),
                Color(0xFF21005D)
            )
        )
    }

    Box(
        modifier = modifier
            .size(size * 1.55f)
            .testTag("lori_orb_container"),
        contentAlignment = Alignment.Center
    ) {
        // Frosted concentric glowing rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val baseRadius = (size.toPx() / 2) * effectiveScale

            // Outer soft frosted glow
            drawCircle(
                color = Color(0xFFD0BCFF).copy(alpha = if (voiceState == VoiceState.LISTENING) 0.35f else 0.18f),
                radius = baseRadius * 1.45f,
                center = center
            )
            // Middle frosted glass ring border
            drawCircle(
                color = Color.White.copy(alpha = if (voiceState == VoiceState.SPEAKING) 0.5f else 0.3f),
                radius = baseRadius * 1.25f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            // Outer frosted ring border
            drawCircle(
                color = Color(0xFFEADDFF).copy(alpha = 0.25f),
                radius = baseRadius * 1.45f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )
        }

        // Core Orb Button with Frosted Glass Surface
        Box(
            modifier = Modifier
                .size(size)
                .scale(effectiveScale)
                .shadow(elevation = 20.dp, shape = CircleShape, spotColor = Color(0xFFD0BCFF))
                .clip(CircleShape)
                .background(orbGradient)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .testTag("lori_orb_button"),
            contentAlignment = Alignment.Center
        ) {
            // Inner Frosted Glass highlight sheen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.15f)
                            )
                        )
                    )
            )

            // Icon Indicator inside Orb
            when (voiceState) {
                VoiceState.SPEAKING -> {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop Lori Speaking",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.35f)
                    )
                }
                VoiceState.LISTENING -> {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Listening...",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.38f)
                    )
                }
                VoiceState.PROCESSING -> {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = "Processing...",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.35f)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Tap to talk to Lori",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.38f)
                    )
                }
            }
        }
    }
}
