package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val KagamiOledColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = KagamiWhite,
    primaryContainer = DeepPurple,
    onPrimaryContainer = KagamiWhite,
    secondary = SubtleMagenta,
    onSecondary = OledBlack,
    secondaryContainer = Color(0xFF3B0764),
    onSecondaryContainer = SoftMagentaGlow,
    tertiary = LuminousViolet,
    onTertiary = OledBlack,
    background = OledBlack,
    onBackground = KagamiWhite,
    surface = KagamiSurface,
    onSurface = KagamiWhite,
    surfaceVariant = KagamiSurfaceElevated,
    onSurfaceVariant = KagamiMutedText,
    outline = KagamiCardBorder,
    outlineVariant = KagamiSubtleBorder,
    error = StatusDanger,
    onError = KagamiWhite
)

private val KagamiMidnightColorScheme = KagamiOledColorScheme.copy(
    background = DeepObsidian,
    surface = Color(0xFF130B24),
    surfaceVariant = Color(0xFF1E1336)
)

val KagamiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun KagamiTheme(
    pureOledBlack: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (pureOledBlack) KagamiOledColorScheme else KagamiMidnightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = KagamiShapes,
        content = content
    )
}
