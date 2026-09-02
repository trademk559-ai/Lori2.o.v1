package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.R
import com.example.modules.voice.VoiceState
import kotlin.math.sin

/**
 * Lottie-based animated voice waveform component with real-time decibel (RMS) responsiveness.
 * Provides vivid visual feedback during microphone usage when Lori is listening.
 */
@Composable
fun LoriVoiceWaveform(
    voiceState: VoiceState,
    rmsDb: Float,
    modifier: Modifier = Modifier,
    height: Dp = 80.dp,
    showLabel: Boolean = true
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.voice_waveform))
    val isPlaying = voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = LottieConstants.IterateForever,
        speed = if (voiceState == VoiceState.LISTENING) (1.0f + (rmsDb / 4f)).coerceIn(1.0f, 2.5f) else 1.0f
    )

    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnim")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    val liveRms = remember { Animatable(0f) }
    LaunchedEffect(rmsDb, voiceState) {
        if (voiceState == VoiceState.LISTENING) {
            liveRms.animateTo(rmsDb.coerceIn(0f, 10f), tween(80))
        } else {
            liveRms.animateTo(0f, tween(200))
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1E192B).copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFD0BCFF).copy(alpha = 0.4f),
                    Color(0xFF4DD0E1).copy(alpha = 0.6f),
                    Color(0xFFF48FB1).copy(alpha = 0.4f)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("lori_voice_waveform_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showLabel) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (voiceState == VoiceState.LISTENING) Color(0xFF4DD0E1)
                                    else if (voiceState == VoiceState.SPEAKING) Color(0xFFD0BCFF)
                                    else Color.Gray
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (voiceState) {
                                VoiceState.LISTENING -> "LIVE MICROPHONE AUDIO"
                                VoiceState.SPEAKING -> "LORI VOICE OUTPUT"
                                VoiceState.PROCESSING -> "PROCESSING SPEECH..."
                                else -> "MIC STANDBY"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = when (voiceState) {
                                VoiceState.LISTENING -> Color(0xFF4DD0E1)
                                VoiceState.SPEAKING -> Color(0xFFD0BCFF)
                                else -> Color(0xFFCAC4D0)
                            }
                        )
                    }

                    if (voiceState == VoiceState.LISTENING) {
                        Text(
                            text = "${(liveRms.value * 10).toInt()} dB",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFEADDFF),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                contentAlignment = Alignment.Center
            ) {
                // Background dynamic continuous audio sine wave
                Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val centerY = canvasHeight / 2
                    val amp = if (voiceState == VoiceState.LISTENING) {
                        (canvasHeight * 0.15f) + (liveRms.value / 10f) * (canvasHeight * 0.32f)
                    } else if (voiceState == VoiceState.SPEAKING) {
                        canvasHeight * 0.22f
                    } else {
                        canvasHeight * 0.05f
                    }

                    // Sine path 1
                    val path1 = Path()
                    val path2 = Path()

                    val step = 4
                    for (x in 0..canvasWidth.toInt() step step) {
                        val progressX = x / canvasWidth
                        val angle1 = (progressX * 4 * Math.PI) + wavePhase
                        val y1 = centerY + sin(angle1).toFloat() * amp

                        val angle2 = (progressX * 3 * Math.PI) - wavePhase
                        val y2 = centerY + sin(angle2).toFloat() * (amp * 0.75f)

                        if (x == 0) {
                            path1.moveTo(0f, y1)
                            path2.moveTo(0f, y2)
                        } else {
                            path1.lineTo(x.toFloat(), y1)
                            path2.lineTo(x.toFloat(), y2)
                        }
                    }

                    drawPath(
                        path = path1,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4DD0E1).copy(alpha = 0.5f),
                                Color(0xFFD0BCFF).copy(alpha = 0.8f),
                                Color(0xFFF48FB1).copy(alpha = 0.5f)
                            )
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    drawPath(
                        path = path2,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFF48FB1).copy(alpha = 0.4f),
                                Color(0xFF6750A4).copy(alpha = 0.7f),
                                Color(0xFF4DD0E1).copy(alpha = 0.4f)
                            )
                        ),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Lottie Animated Voice Visualizer Layer
                if (composition != null) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(height)
                            .testTag("lottie_waveform_animation")
                    )
                } else {
                    // Fallback procedural visualizer bars if composition is compiling
                    ProceduralWaveformBars(
                        rmsDb = liveRms.value,
                        isListening = voiceState == VoiceState.LISTENING,
                        height = height
                    )
                }
            }
        }
    }
}

/**
 * Procedural responsive bars that modulate symmetrically with speech dB.
 */
@Composable
private fun ProceduralWaveformBars(
    rmsDb: Float,
    isListening: Boolean,
    height: Dp
) {
    val barCount = 13
    val infiniteTransition = rememberInfiniteTransition(label = "ProceduralBars")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(height)
    ) {
        val totalWidth = size.width
        val barWidth = 6.dp.toPx()
        val spacing = (totalWidth - (barCount * barWidth)) / (barCount - 1)
        val centerY = size.height / 2

        for (i in 0 until barCount) {
            val distanceFromCenter = kotlin.math.abs(i - (barCount / 2)).toFloat() / (barCount / 2)
            val baseScale = 1.0f - (distanceFromCenter * 0.5f)
            
            val barHeightFactor = if (isListening) {
                ((rmsDb / 10f) * 0.75f + (pulse * 0.25f) + 0.15f) * baseScale
            } else {
                (0.12f + pulse * 0.1f) * baseScale
            }

            val finalBarHeight = (size.height * barHeightFactor.coerceIn(0.1f, 0.95f))
            val x = i * (barWidth + spacing)
            val y = centerY - (finalBarHeight / 2)

            val color = when {
                i % 3 == 0 -> Color(0xFF4DD0E1)
                i % 3 == 1 -> Color(0xFFD0BCFF)
                else -> Color(0xFFF48FB1)
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, finalBarHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
