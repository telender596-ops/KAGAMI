package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.KagamiSettings
import com.example.ui.components.KagamiCrestEmblem
import com.example.ui.components.KagamiGlassCard
import com.example.ui.components.KagamiPillButton
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.KagamiCardBorder
import com.example.ui.theme.KagamiMutedText
import com.example.ui.theme.KagamiSurface
import com.example.ui.theme.KagamiWhite
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.SubtleMagenta
import kotlin.math.roundToInt

@Composable
fun KagamiSettingsContent(
    innerPadding: PaddingValues,
    settings: KagamiSettings,
    onPureOledChanged: (Boolean) -> Unit,
    onShowParticlesChanged: (Boolean) -> Unit,
    onDefaultBrushSizeChanged: (Float) -> Unit,
    onBrushSmoothingChanged: (Float) -> Unit,
    onShowCanvasGridChanged: (Boolean) -> Unit,
    onDefaultPaperToneChanged: (String) -> Unit,
    onExportFormatChanged: (String) -> Unit,
    onJpgQualityChanged: (Int) -> Unit,
    onResetPreferences: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .testTag("settings_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KagamiCrestEmblem(size = 24.dp)
                    Column {
                        Text(
                            text = "ATELIER CONFIGURATION",
                            style = MaterialTheme.typography.titleLarge,
                            color = KagamiWhite
                        )
                        Text(
                            text = "Customize theme, brush physics, canvas & export",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                            color = KagamiMutedText
                        )
                    }
                }
            }

            // 1. Theme & Visual Atmosphere
            item {
                KagamiGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = LuminousViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "THEME & ATMOSPHERE",
                                style = MaterialTheme.typography.titleSmall,
                                color = LuminousViolet
                            )
                        }

                        SettingsSwitchRow(
                            title = "Pure AMOLED Pitch-Black (#000000)",
                            subtitle = "Maximizes contrast and battery efficiency on OLED phones",
                            checked = settings.pureOledTheme,
                            onCheckedChange = onPureOledChanged,
                            testTag = "switch_pure_oled"
                        )

                        SettingsSwitchRow(
                            title = "Ambient Violet Aura Particles",
                            subtitle = "Subtle floating motes on dashboard and import screens",
                            checked = settings.showParticles,
                            onCheckedChange = onShowParticlesChanged,
                            testTag = "switch_particles"
                        )
                    }
                }
            }

            // 2. Brush Physics Preferences
            item {
                KagamiGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = null,
                                tint = SubtleMagenta,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "BRUSH & STYLUS PREFERENCES",
                                style = MaterialTheme.typography.titleSmall,
                                color = LuminousViolet
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Default Brush Size",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = KagamiWhite
                                )
                                Text(
                                    text = "${settings.defaultBrushSize.roundToInt()} px",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = LuminousViolet
                                )
                            }
                            Slider(
                                value = settings.defaultBrushSize,
                                onValueChange = onDefaultBrushSizeChanged,
                                valueRange = 2f..40f,
                                colors = SliderDefaults.colors(
                                    thumbColor = KagamiWhite,
                                    activeTrackColor = ElectricViolet
                                )
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Stroke Stabilization (Smoothing)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = KagamiWhite
                                )
                                Text(
                                    text = "${(settings.brushSmoothing * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SubtleMagenta
                                )
                            }
                            Slider(
                                value = settings.brushSmoothing,
                                onValueChange = onBrushSmoothingChanged,
                                valueRange = 0f..0.85f,
                                colors = SliderDefaults.colors(
                                    thumbColor = KagamiWhite,
                                    activeTrackColor = SubtleMagenta
                                )
                            )
                        }
                    }
                }
            }

            // 3. Canvas Preferences
            item {
                KagamiGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = null,
                                tint = LuminousViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "CANVAS PREFERENCES",
                                style = MaterialTheme.typography.titleSmall,
                                color = LuminousViolet
                            )
                        }

                        SettingsSwitchRow(
                            title = "Rule-of-Thirds & Perspective Grid",
                            subtitle = "Display subtle non-printing proportion guides on canvas",
                            checked = settings.showCanvasGrid,
                            onCheckedChange = onShowCanvasGridChanged,
                            testTag = "switch_canvas_grid"
                        )

                        Text(
                            text = "Default Canvas Paper Tone",
                            style = MaterialTheme.typography.bodyLarge,
                            color = KagamiWhite
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val tones = listOf(
                                "PAPER_WHITE" to "Pure White",
                                "DARK_SLATE" to "Violet Slate",
                                "OLED_VOID" to "OLED Void"
                            )
                            tones.forEach { (key, label) ->
                                val selected = settings.defaultPaperTone == key
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) DeepPurple else KagamiSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) LuminousViolet else KagamiCardBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onDefaultPaperToneChanged(key) }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = KagamiWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Export Quality & Format
            item {
                KagamiGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = LuminousViolet,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "EXPORT QUALITY",
                                style = MaterialTheme.typography.titleSmall,
                                color = LuminousViolet
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf("PNG", "JPG").forEach { fmt ->
                                val selected = settings.exportFormat == fmt
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) DeepPurple else KagamiSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) LuminousViolet else KagamiCardBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onExportFormatChanged(fmt) }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (fmt == "PNG") "PNG (Lossless)" else "JPG (Compressed)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = KagamiWhite
                                        )
                                    }
                                }
                            }
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "JPG Compression Quality",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = KagamiWhite
                                )
                                Text(
                                    text = "${settings.jpgQuality}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = LuminousViolet
                                )
                            }
                            Slider(
                                value = settings.jpgQuality.toFloat(),
                                onValueChange = { onJpgQualityChanged(it.roundToInt()) },
                                valueRange = 60f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = KagamiWhite,
                                    activeTrackColor = ElectricViolet
                                )
                            )
                        }
                    }
                }
            }

            // 5. About KAGAMI & Original Mascot Showcase + Privacy Notice
            item {
                KagamiGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    glowAccent = true,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_kagami_mascot_bust),
                                contentDescription = "KAGAMI Mascot",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .border(
                                        BorderStroke(
                                            1.2.dp,
                                            Brush.linearGradient(listOf(LuminousViolet, SubtleMagenta))
                                        ),
                                        CircleShape
                                    )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "KAGAMI • 鏡 v2.4 Studio",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = KagamiWhite
                                )
                                Text(
                                    text = "Original Anime Digital Atelier & Auto-Draw Engine",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                    color = LuminousViolet
                                )
                                Text(
                                    text = "Featuring original studio creator mascot Ren Kagami.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.5.sp),
                                    color = KagamiMutedText
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(KagamiSurface)
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Privacy",
                                tint = SubtleMagenta,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "100% On-Device Privacy: All edge extraction, manga sketch rendering, vector auto-tracing, and project storage run locally on your phone. No images or drawings ever leave your device.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp),
                                color = KagamiMutedText
                            )
                        }

                        KagamiPillButton(
                            text = "Reset All Preferences",
                            icon = Icons.Default.RestartAlt,
                            isPrimary = false,
                            onClick = onResetPreferences,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_preferences_button")
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = KagamiWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = KagamiMutedText
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = KagamiWhite,
                checkedTrackColor = ElectricViolet,
                uncheckedThumbColor = KagamiMutedText,
                uncheckedTrackColor = KagamiSurface
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
