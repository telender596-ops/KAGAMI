package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.KagamiCardBorder
import com.example.ui.theme.KagamiGlassSurface
import com.example.ui.theme.KagamiMutedText
import com.example.ui.theme.KagamiSubtleBorder
import com.example.ui.theme.KagamiWhite
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SubtleMagenta
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun KagamiAtmosphericBackground(
    pureOled: Boolean = true,
    showParticles: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "kagami_aura")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_phase"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    val baseBg = if (pureOled) OledBlack else DeepObsidian

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricViolet.copy(alpha = 0.12f * pulse),
                        DeepPurple.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.85f, h * 0.10f),
                    radius = w * 0.7f
                ),
                center = Offset(w * 0.85f, h * 0.10f),
                radius = w * 0.7f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SubtleMagenta.copy(alpha = 0.07f * pulse),
                        DeepPurple.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.15f, h * 0.88f),
                    radius = w * 0.6f
                ),
                center = Offset(w * 0.15f, h * 0.88f),
                radius = w * 0.6f
            )

            if (showParticles) {
                val seeds = listOf(
                    Triple(0.18f, 0.22f, 1.8f),
                    Triple(0.76f, 0.18f, 2.2f),
                    Triple(0.42f, 0.35f, 1.5f),
                    Triple(0.88f, 0.48f, 2.0f),
                    Triple(0.12f, 0.64f, 1.6f),
                    Triple(0.62f, 0.72f, 2.1f),
                    Triple(0.31f, 0.84f, 1.7f),
                    Triple(0.81f, 0.82f, 1.5f)
                )
                seeds.forEachIndexed { idx, (bx, by, rad) ->
                    val driftX = cos(phase + idx * 0.9f) * 10f
                    val driftY = sin(phase * 1.2f + idx * 0.7f) * 14f
                    val center = Offset(w * bx + driftX, h * by + driftY)
                    val color = if (idx % 3 == 0) SubtleMagenta else LuminousViolet
                    drawCircle(
                        color = color.copy(alpha = 0.18f * pulse),
                        radius = rad * density,
                        center = center
                    )
                }
            }
        }

        content()
    }
}

/**
 * Compact glassmorphism card with restrained padding and crisp violet border.
 */
@Composable
fun KagamiGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderColor: Color = KagamiCardBorder,
    containerColor: Color = KagamiGlassSurface,
    glowAccent: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .then(
                if (glowAccent) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = shape,
                        ambientColor = ElectricViolet.copy(alpha = 0.4f),
                        spotColor = SubtleMagenta.copy(alpha = 0.4f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        containerColor,
                        Color(0xD90A0514)
                    )
                )
            )
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = if (glowAccent) {
                            listOf(ElectricViolet.copy(alpha = 0.7f), SubtleMagenta.copy(alpha = 0.45f))
                        } else {
                            listOf(borderColor, KagamiSubtleBorder)
                        }
                    )
                ),
                shape = shape
            )
            .padding(contentPadding),
        content = content
    )
}

/**
 * Compact, touch-friendly pill button for mobile screens.
 */
@Composable
fun KagamiPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    subtitleTag: String? = null
) {
    val shape = CircleShape
    val bgBrush = when {
        !enabled -> Brush.horizontalGradient(listOf(Color(0xFF181224), Color(0xFF181224)))
        isPrimary -> Brush.horizontalGradient(
            colors = listOf(
                DeepPurple,
                ElectricViolet,
                Color(0xFFA855F7)
            )
        )
        else -> Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF160E2A),
                Color(0xFF100920)
            )
        )
    }

    val borderStroke = BorderStroke(
        width = 1.dp,
        brush = Brush.horizontalGradient(
            colors = if (!enabled) {
                listOf(Color(0x33756896), Color(0x22756896))
            } else if (isPrimary) {
                listOf(LuminousViolet, SubtleMagenta.copy(alpha = 0.75f))
            } else {
                listOf(ElectricViolet.copy(alpha = 0.5f), KagamiSubtleBorder)
            }
        )
    )

    Surface(
        modifier = modifier
            .then(
                if (isPrimary && enabled) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = shape,
                        ambientColor = ElectricViolet,
                        spotColor = SubtleMagenta
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        color = Color.Transparent,
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .background(bgBrush)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = if (enabled) KagamiWhite else KagamiMutedText.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (enabled) KagamiWhite else KagamiMutedText.copy(alpha = 0.45f)
                )
                if (subtitleTag != null) {
                    Text(
                        text = subtitleTag,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        color = KagamiWhite.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}

@Composable
fun KagamiCrestEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        val outerPath = Path().apply {
            moveTo(cx, h * 0.06f)
            lineTo(w * 0.92f, cy)
            lineTo(cx, h * 0.94f)
            lineTo(w * 0.08f, cy)
            close()
        }
        drawPath(
            path = outerPath,
            brush = Brush.linearGradient(
                colors = listOf(ElectricViolet, SubtleMagenta)
            ),
            style = Stroke(width = w * 0.06f, cap = StrokeCap.Round)
        )

        val nibPath = Path().apply {
            moveTo(cx, h * 0.22f)
            lineTo(w * 0.72f, cy * 1.05f)
            lineTo(cx, h * 0.80f)
            lineTo(w * 0.28f, cy * 1.05f)
            close()
        }
        drawPath(
            path = nibPath,
            brush = Brush.verticalGradient(
                colors = listOf(LuminousViolet.copy(alpha = 0.85f), DeepPurple.copy(alpha = 0.9f))
            )
        )

        drawLine(
            color = KagamiWhite,
            start = Offset(cx, h * 0.22f),
            end = Offset(cx, h * 0.54f),
            strokeWidth = w * 0.05f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = KagamiWhite,
            radius = w * 0.065f,
            center = Offset(cx, h * 0.56f)
        )
    }
}
