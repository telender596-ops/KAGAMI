package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.external.CalibrationUiMode
import com.example.external.ExternalJobStatus
import com.example.external.ExternalPathConfig
import com.example.external.KagamiExternalDrawingManager
import com.example.external.KagamiGestureService
import com.example.external.KagamiOverlayService
import com.example.external.SpeedPreset
import com.example.external.TargetCanvasPreset
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
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SubtleMagenta
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun KagamiExternalAutoDrawScreen(
    innerPadding: PaddingValues,
    onPickImageForExternal: (Uri?) -> Unit,
    onLoadShrineSampleForExternal: () -> Unit,
    onReapplySmartAutoSetup: () -> Unit,
    onUpdateExternalPathConfig: (ExternalPathConfig) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val externalState by KagamiExternalDrawingManager.state.collectAsState()

    var hasOverlayPerm by remember { mutableStateOf(KagamiOverlayService.canDrawOverlays(context)) }
    var isAccessibilityReady by remember { mutableStateOf(KagamiGestureService.isConnected()) }
    var showSourceUnderlay by remember { mutableStateOf(false) }
    var showAdvancedControls by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPerm = KagamiOverlayService.canDrawOverlays(context)
                isAccessibilityReady = KagamiGestureService.isConnected()
                KagamiExternalDrawingManager.setGestureServiceConnected(isAccessibilityReady)
                if (!hasOverlayPerm && externalState.isOverlayServiceRunning) {
                    KagamiOverlayService.stopOverlayService(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        onPickImageForExternal(uri)
    }

    val legacyDocPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onPickImageForExternal(uri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .testTag("external_autodraw_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Compact Top Title Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KagamiCrestEmblem(size = 24.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "EXTERNAL AUTO-DRAW & WEPLAY",
                                style = MaterialTheme.typography.titleLarge,
                                color = KagamiWhite
                            )
                            Text(
                                text = "Select Image → Smart Auto → Auto Calibrate → Start",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                color = KagamiMutedText
                            )
                        }
                    }
                }
            }

            // 1. Target Mode Switcher: Dedicated WePlay Mode vs Universal Canvas Mode
            item {
                KagamiGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    glowAccent = externalState.isWePlayModeActive,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // WePlay Mode Pill
                            val wePlaySelected = externalState.isWePlayModeActive
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (wePlaySelected) DeepPurple else KagamiSurface,
                                border = BorderStroke(
                                    1.2.dp,
                                    if (wePlaySelected) SubtleMagenta else KagamiCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { KagamiExternalDrawingManager.setWePlayMode(true) }
                                    .testTag("mode_weplay_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SportsEsports,
                                        contentDescription = "WePlay Mode",
                                        tint = if (wePlaySelected) SubtleMagenta else LuminousViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "WePlay Mode",
                                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                                            color = KagamiWhite
                                        )
                                        Text(
                                            text = "1-Tap Game Board Fit",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                            color = KagamiMutedText
                                        )
                                    }
                                }
                            }

                            // Universal Mode Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (!wePlaySelected) DeepPurple else KagamiSurface,
                                border = BorderStroke(
                                    1.2.dp,
                                    if (!wePlaySelected) LuminousViolet else KagamiCardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { KagamiExternalDrawingManager.setWePlayMode(false) }
                                    .testTag("mode_universal_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = "Universal Mode",
                                        tint = LuminousViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Universal Mode",
                                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                                            color = KagamiWhite
                                        )
                                        Text(
                                            text = "Any Art / Sketch App",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                            color = KagamiMutedText
                                        )
                                    }
                                }
                            }
                        }

                        // Preset & Speed Selector Row (Slow / Normal / Fast)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Drawing Speed:",
                                style = MaterialTheme.typography.labelMedium,
                                color = KagamiMutedText
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SpeedPreset.entries.forEach { preset ->
                                    val selected = externalState.activeSpeedPreset == preset
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) ElectricViolet else KagamiSurface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) LuminousViolet else KagamiCardBorder.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { KagamiExternalDrawingManager.selectSpeedPreset(preset) }
                                            .testTag("speed_preset_${preset.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = KagamiWhite,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Select Image & Smart Auto Preview Card
            item {
                KagamiGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "SMART AUTO LINE PREVIEW",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = LuminousViolet
                                    )
                                    if (externalState.smartProfile != null) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = ElectricViolet.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "AUTO-TUNED",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                                color = LuminousViolet,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = externalState.smartProfile?.summaryLabel
                                        ?: "${externalState.totalStrokes} continuous strokes ready",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                    color = KagamiMutedText
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Ref",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KagamiMutedText
                                )
                                Switch(
                                    checked = showSourceUnderlay,
                                    onCheckedChange = { showSourceUnderlay = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = KagamiWhite,
                                        checkedTrackColor = ElectricViolet
                                    ),
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }

                        // Clean Line Path Preview Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(195.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF07040E))
                                .border(1.dp, KagamiCardBorder, RoundedCornerShape(14.dp))
                                .testTag("external_path_preview_box"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (externalState.isExtractingPaths) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = ElectricViolet,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = "Smart Auto analyzing image & building strokes...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LuminousViolet
                                    )
                                }
                            } else if (externalState.extractedPaths.isEmpty()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    KagamiCrestEmblem(size = 32.dp)
                                    Text(
                                        text = "Select an Image to Auto-Generate Clean Paths",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = KagamiWhite
                                    )
                                    Text(
                                        text = "Smart Auto automatically configures contrast, edge strength, noise filtering, and stroke order.",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = KagamiMutedText
                                    )
                                }
                            } else {
                                val srcBmp = externalState.sourceBitmap
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                ) {
                                    val sw = externalState.sourceWidth.toFloat().coerceAtLeast(1f)
                                    val sh = externalState.sourceHeight.toFloat().coerceAtLeast(1f)
                                    val fit = min(size.width / sw, size.height / sh)
                                    val drawW = sw * fit
                                    val drawH = sh * fit
                                    val left = (size.width - drawW) / 2f
                                    val top = (size.height - drawH) / 2f

                                    drawRect(
                                        color = Color(0xFF100A1E),
                                        topLeft = Offset(left, top),
                                        size = androidx.compose.ui.geometry.Size(drawW, drawH)
                                    )

                                    if (showSourceUnderlay && srcBmp != null) {
                                        drawImage(
                                            image = srcBmp.asImageBitmap(),
                                            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                                            dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt()),
                                            alpha = 0.25f
                                        )
                                    }

                                    val baseWidthPx = externalState.pathConfig.lineThickness.coerceIn(1.2f, 7f)
                                    val paths = externalState.extractedPaths
                                    for (idx in paths.indices) {
                                        val normPath = paths[idx]
                                        val pts = normPath.points
                                        if (pts.size < 2) continue
                                        val path = Path()
                                        val p0 = pts.first()
                                        path.moveTo(left + p0.x * drawW, top + p0.y * drawH)
                                        for (i in 1 until pts.size) {
                                            val p = pts[i]
                                            path.lineTo(left + p.x * drawW, top + p.y * drawH)
                                        }
                                        val strokeColor = if (normPath.isPrimaryFeature) {
                                            KagamiWhite
                                        } else {
                                            LuminousViolet
                                        }
                                        drawPath(
                                            path = path,
                                            color = strokeColor,
                                            style = Stroke(
                                                width = baseWidthPx * normPath.weightFactor,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Select Image + Shrine Sample + Smart Auto Reset Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KagamiPillButton(
                                text = "Select Image",
                                icon = Icons.Default.AddPhotoAlternate,
                                isPrimary = externalState.extractedPaths.isEmpty(),
                                onClick = {
                                    try {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    } catch (e: Exception) {
                                        legacyDocPicker.launch("image/*")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("external_select_image_button")
                            )

                            KagamiPillButton(
                                text = "Sample Art",
                                icon = Icons.Default.AutoAwesome,
                                isPrimary = false,
                                onClick = onLoadShrineSampleForExternal,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("external_sample_button")
                            )

                            if (externalState.sourceBitmap != null) {
                                KagamiPillButton(
                                    text = "Smart Auto",
                                    icon = Icons.Default.AutoFixHigh,
                                    isPrimary = false,
                                    onClick = onReapplySmartAutoSetup,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reapply_smart_auto_button")
                                )
                            }
                        }

                        // Collapsible "Advanced" Manual Tuning Section (Hidden by default so normal users never need it)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = KagamiSurface,
                            border = BorderStroke(0.8.dp, KagamiCardBorder.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showAdvancedControls = !showAdvancedControls }
                                .testTag("toggle_advanced_settings_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = LuminousViolet,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (externalState.pathConfig.isSmartAutoEnabled) {
                                            "Advanced Path Controls (Smart Auto Active)"
                                        } else {
                                            "Advanced Path Controls (Custom)"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = KagamiWhite
                                    )
                                }
                                Icon(
                                    imageVector = if (showAdvancedControls) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle Advanced",
                                    tint = KagamiMutedText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        AnimatedVisibility(visible = showAdvancedControls) {
                            val cfg = externalState.pathConfig
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PipelineSliderRow(
                                    label = "Detail Level",
                                    valueText = "${(cfg.detailLevel * 100).roundToInt()}%",
                                    value = cfg.detailLevel,
                                    range = 0.15f..1.0f,
                                    onValueChange = {
                                        onUpdateExternalPathConfig(
                                            cfg.copy(isSmartAutoEnabled = false, detailLevel = it)
                                        )
                                    },
                                    testTag = "slider_external_detail"
                                )

                                PipelineSliderRow(
                                    label = "Bezier Smoothness",
                                    valueText = "${(cfg.smoothness * 100).roundToInt()}%",
                                    value = cfg.smoothness,
                                    range = 0.0f..1.0f,
                                    onValueChange = {
                                        onUpdateExternalPathConfig(
                                            cfg.copy(isSmartAutoEnabled = false, smoothness = it)
                                        )
                                    },
                                    testTag = "slider_external_smoothness"
                                )

                                PipelineSliderRow(
                                    label = "Noise Reduction",
                                    valueText = "${(cfg.noiseReduction * 100).roundToInt()}%",
                                    value = cfg.noiseReduction,
                                    range = 0.0f..1.0f,
                                    onValueChange = {
                                        onUpdateExternalPathConfig(
                                            cfg.copy(isSmartAutoEnabled = false, noiseReduction = it)
                                        )
                                    },
                                    testTag = "slider_external_noise"
                                )

                                PipelineSliderRow(
                                    label = "Path Simplification",
                                    valueText = "${(cfg.simplification * 100).roundToInt()}%",
                                    value = cfg.simplification,
                                    range = 0.1f..1.0f,
                                    onValueChange = {
                                        onUpdateExternalPathConfig(
                                            cfg.copy(isSmartAutoEnabled = false, simplification = it)
                                        )
                                    },
                                    testTag = "slider_external_simplification"
                                )

                                PipelineSliderRow(
                                    label = "Line Thickness",
                                    valueText = "${String.format("%.1f", cfg.lineThickness)} px",
                                    value = cfg.lineThickness,
                                    range = 1.5f..7.0f,
                                    onValueChange = {
                                        onUpdateExternalPathConfig(cfg.copy(lineThickness = it))
                                    },
                                    testTag = "slider_external_thickness"
                                )
                            }
                        }
                    }
                }
            }

            // 3. Smart Auto Calibration & Floating Overlay Controller Card
            item {
                KagamiGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    glowAccent = externalState.isOverlayServiceRunning,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SMART AUTO CALIBRATION & OVERLAY",
                            style = MaterialTheme.typography.titleSmall,
                            color = LuminousViolet
                        )

                        // Canvas Preset Chips (No X/Y Numbers!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TargetCanvasPreset.entries.forEach { preset ->
                                val selected = externalState.activePreset == preset
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) DeepPurple else KagamiSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) LuminousViolet else KagamiCardBorder.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            KagamiExternalDrawingManager.applySmartAutoCalibration(preset)
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = when (preset) {
                                                TargetCanvasPreset.WEPLAY_PARTY -> "WePlay"
                                                TargetCanvasPreset.WEPLAY_SQUARE -> "WePlay 1:1"
                                                TargetCanvasPreset.UNIVERSAL_AUTO -> "Auto Fit"
                                                TargetCanvasPreset.UNIVERSAL_FULL -> "Wide"
                                            },
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                                            color = KagamiWhite,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        if (externalState.jobStatus == ExternalJobStatus.UNSUPPORTED_CANVAS_ERROR) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StatusDanger.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, StatusDanger)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = StatusDanger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "External drawing is not supported on this canvas.",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = KagamiWhite
                                    )
                                }
                            }
                        }

                        // Primary Launch / Auto Calibrate / Start / Emergency STOP Controls
                        if (!externalState.isOverlayServiceRunning) {
                            KagamiPillButton(
                                text = if (hasOverlayPerm) "Open Floating Overlay & Auto Calibrate" else "Grant Overlay Permission to Start",
                                icon = Icons.Default.Layers,
                                isPrimary = true,
                                onClick = {
                                    if (KagamiOverlayService.canDrawOverlays(context)) {
                                        hasOverlayPerm = true
                                        KagamiExternalDrawingManager.applySmartAutoCalibration()
                                        KagamiOverlayService.startOverlayService(context)
                                    } else {
                                        try {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            ).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("launch_floating_overlay_button")
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                KagamiPillButton(
                                    text = "Auto Calibrate",
                                    icon = Icons.Default.CropFree,
                                    isPrimary = true,
                                    onClick = {
                                        KagamiExternalDrawingManager.applySmartAutoCalibration()
                                        KagamiExternalDrawingManager.enterCalibrationMode(CalibrationUiMode.AUTO_VISUAL)
                                    },
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .testTag("calibrate_overlay_button")
                                )

                                KagamiPillButton(
                                    text = "Preview",
                                    icon = Icons.Default.Visibility,
                                    isPrimary = false,
                                    onClick = { KagamiExternalDrawingManager.enterPreviewMode() },
                                    modifier = Modifier.weight(0.9f)
                                )

                                KagamiPillButton(
                                    text = "Close Overlay",
                                    icon = Icons.Default.Stop,
                                    isPrimary = false,
                                    onClick = { KagamiOverlayService.stopOverlayService(context) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stop_floating_overlay_button")
                                )
                            }

                            // In-App Start / Pause / Resume / Large Emergency STOP Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isDrawing = externalState.jobStatus == ExternalJobStatus.DRAWING
                                val isPaused = externalState.jobStatus == ExternalJobStatus.PAUSED

                                KagamiPillButton(
                                    text = when {
                                        isDrawing -> "Pause"
                                        isPaused -> "Resume"
                                        else -> "Start Auto-Draw"
                                    },
                                    icon = if (isDrawing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    isPrimary = true,
                                    enabled = externalState.extractedPaths.isNotEmpty(),
                                    onClick = {
                                        when {
                                            isDrawing -> KagamiGestureService.pauseExternalDrawing()
                                            isPaused -> KagamiGestureService.resumeExternalDrawing()
                                            else -> {
                                                KagamiExternalDrawingManager.setOverlayCollapsed(true)
                                                KagamiGestureService.startExternalDrawingFromBeginning()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1.1f)
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = StatusDanger,
                                    border = BorderStroke(1.2.dp, KagamiWhite),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(38.dp)
                                        .clip(CircleShape)
                                        .clickable { KagamiExternalDrawingManager.emergencyStop() }
                                        .testTag("emergency_stop_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = "Emergency Stop",
                                            tint = KagamiWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "STOP",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = KagamiWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Compact System Permissions Verification Card
            item {
                KagamiGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "ANDROID PERMISSIONS STATUS",
                            style = MaterialTheme.typography.titleSmall,
                            color = LuminousViolet
                        )

                        PermissionStatusRow(
                            title = "Display Over Other Apps",
                            subtitle = if (hasOverlayPerm) "Granted • Floating overlay enabled" else "Needed to show floating KAGAMI controls",
                            isGranted = hasOverlayPerm,
                            buttonText = if (hasOverlayPerm) "Active" else "Enable",
                            onClick = {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    ).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(fallback)
                                }
                            },
                            testTag = "grant_overlay_permission_button"
                        )

                        PermissionStatusRow(
                            title = "External Gesture Engine",
                            subtitle = if (isAccessibilityReady) "Connected • Ready to draw" else "Enable KAGAMI in Accessibility for external strokes",
                            isGranted = isAccessibilityReady,
                            buttonText = if (isAccessibilityReady) "Ready" else "Setup",
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            },
                            testTag = "enable_gesture_service_button"
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    buttonText: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KagamiSurface)
            .border(
                width = 1.dp,
                color = if (isGranted) StatusSuccess.copy(alpha = 0.5f) else KagamiCardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.AccessibilityNew,
                contentDescription = null,
                tint = if (isGranted) StatusSuccess else SubtleMagenta,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.padding(end = 6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 12.5.sp),
                    color = KagamiWhite
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.5.sp),
                    color = KagamiMutedText
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = if (isGranted) DeepPurple.copy(alpha = 0.4f) else ElectricViolet,
            border = BorderStroke(1.dp, if (isGranted) StatusSuccess else LuminousViolet),
            modifier = Modifier
                .clickable(onClick = onClick)
                .testTag(testTag)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = KagamiWhite
                )
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = KagamiWhite,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun PipelineSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = KagamiWhite
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = LuminousViolet
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = KagamiWhite,
                activeTrackColor = ElectricViolet,
                inactiveTrackColor = DeepPurple.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .height(26.dp)
                .testTag(testTag)
        )
    }
}
