package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.KagamiSettings
import com.example.engine.AutoDrawMode
import com.example.engine.StrokeData
import com.example.ui.components.KagamiGlassCard
import com.example.ui.components.KagamiPillButton
import com.example.ui.theme.DeepObsidian
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.KagamiBrushPalette
import com.example.ui.theme.KagamiCardBorder
import com.example.ui.theme.KagamiDimText
import com.example.ui.theme.KagamiGlassSurface
import com.example.ui.theme.KagamiMutedText
import com.example.ui.theme.KagamiSurface
import com.example.ui.theme.KagamiWhite
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.OledBlack
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SubtleMagenta
import com.example.viewmodel.DrawingTool
import com.example.viewmodel.WorkspaceUiState
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KagamiDrawingWorkspaceScreen(
    state: WorkspaceUiState,
    settings: KagamiSettings,
    onBack: () -> Unit,
    onSelectNewImage: () -> Unit,
    onSelectTool: (DrawingTool) -> Unit,
    onBrushColorSelected: (Int) -> Unit,
    onBrushSizeChanged: (Float) -> Unit,
    onBrushOpacityChanged: (Float) -> Unit,
    onReferenceAlphaChanged: (Float) -> Unit,
    onToggleReferenceVisibility: () -> Unit,
    onPaperColorChanged: (Int) -> Unit,
    onStrokeStart: (Offset) -> Unit,
    onStrokeMove: (Offset) -> Unit,
    onStrokeEnd: () -> Unit,
    onStrokeCancel: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClearCanvas: () -> Unit,
    onZoomAndPan: (Float, Offset) -> Unit,
    onStepZoom: (Float) -> Unit,
    onResetFit: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenAutoDrawEngine: () -> Unit,
    onCloseAutoDrawEngine: () -> Unit,
    onUpdateAutoDrawPreview: (AutoDrawMode, Float, Boolean) -> Unit,
    onCommitAutoDrawToCanvas: () -> Unit,
    onTriggerLiveVectorAutoTrace: () -> Unit,
    onSetExportSheetVisible: (Boolean) -> Unit,
    onUpdateProjectTitle: (String) -> Unit,
    onSaveProjectLocal: () -> Unit,
    onExportArtwork: (String) -> Unit,
    onDismissBanner: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    var showBrushTunerPanel by remember { mutableStateOf(false) }
    var showLayerTunerPanel by remember { mutableStateOf(false) }
    var pendingLegacyExportFormat by remember { mutableStateOf<String?>(null) }

    // Legacy WRITE_EXTERNAL_STORAGE runtime permission launcher only for API <= 28
    val legacyStoragePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val fmt = pendingLegacyExportFormat ?: "PNG"
        pendingLegacyExportFormat = null
        if (granted) {
            onExportArtwork(fmt)
        } else {
            // Even if legacy storage permission is denied on API <= 28, save locally to app archive and show graceful message
            onSaveProjectLocal()
        }
    }

    fun requestExportWithPermissionCheck(format: String) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                pendingLegacyExportFormat = format
                legacyStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return
            }
        }
        onExportArtwork(format)
    }

    // Automatically dismiss status toast after 3.2 seconds
    LaunchedEffect(state.statusBannerMessage) {
        if (state.statusBannerMessage != null) {
            delay(3200)
            onDismissBanner()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (settings.pureOledTheme) OledBlack else DeepObsidian)
            .testTag("drawing_workspace_screen")
    ) {
        // 1. Interactive Center Drawing Canvas Viewport
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .testTag("interactive_drawing_canvas_container"),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val viewportW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val viewportH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            val cw = state.canvasWidth.toFloat().coerceAtLeast(1f)
            val ch = state.canvasHeight.toFloat().coerceAtLeast(1f)

            // Fit canvas inside viewport with comfortable margin
            val fitScale = remember(viewportW, viewportH, cw, ch) {
                min((viewportW * 0.90f) / cw, (viewportH * 0.72f) / ch)
            }
            val displayW = cw * fitScale
            val displayH = ch * fitScale

            val currentScale = state.zoomScale
            val currentPan = state.panOffset
            val activeTool = state.activeTool

            // Helper to map touch screen coordinates inside the Box into logical canvas (0..cw, 0..ch) coordinates
            fun screenToCanvas(screenPt: Offset): Offset {
                val centerX = viewportW / 2f
                val centerY = viewportH / 2f
                // Undo graphicsLayer translation & scale around center
                val unpannedX = (screenPt.x - centerX - currentPan.x) / currentScale + centerX
                val unpannedY = (screenPt.y - centerY - currentPan.y) / currentScale + centerY
                val left = (viewportW - displayW) / 2f
                val top = (viewportH - displayH) / 2f
                val canvasX = ((unpannedX - left) / fitScale).coerceIn(0f, cw)
                val canvasY = ((unpannedY - top) / fitScale).coerceIn(0f, ch)
                return Offset(canvasX, canvasY)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(activeTool, fitScale, cw, ch) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var isMultiTouchZoom = false
                            var strokeStarted = false

                            if (activeTool != DrawingTool.PAN_ZOOM) {
                                strokeStarted = true
                                onStrokeStart(screenToCanvas(down.position))
                            }

                            do {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }

                                if (pressedCount >= 2 || activeTool == DrawingTool.PAN_ZOOM) {
                                    if (strokeStarted) {
                                        onStrokeCancel()
                                        strokeStarted = false
                                    }
                                    isMultiTouchZoom = true
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    if (zoomChange != 1f || panChange != Offset.Zero) {
                                        onZoomAndPan(zoomChange, panChange)
                                    }
                                    event.changes.forEach { it.consume() }
                                } else if (pressedCount == 1 && !isMultiTouchZoom && strokeStarted) {
                                    val change = event.changes.first()
                                    onStrokeMove(screenToCanvas(change.position))
                                    change.consume()
                                }
                            } while (event.changes.any { it.pressed })

                            if (strokeStarted && !isMultiTouchZoom) {
                                onStrokeEnd()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val widthDp = with(density) { displayW.toDp() }
                val heightDp = with(density) { displayH.toDp() }

                // The actual Paper & Layer Stack
                Box(
                    modifier = Modifier
                        .size(widthDp, heightDp)
                        .graphicsLayer {
                            scaleX = state.zoomScale
                            scaleY = state.zoomScale
                            translationX = state.panOffset.x
                            translationY = state.panOffset.y
                        }
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(8.dp),
                            ambientColor = ElectricViolet.copy(alpha = 0.45f),
                            spotColor = SubtleMagenta.copy(alpha = 0.45f)
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(state.canvasBackgroundArgb))
                        .border(1.dp, ElectricViolet.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .testTag("drawing_canvas_surface")
                ) {
                    // Layer 1: Processed Reference / Auto-Draw Bitmap Layer
                    val refBitmap = state.processedReferenceBitmap ?: state.originalReferenceBitmap
                    if (refBitmap != null && state.showReferenceLayer && state.referenceAlpha > 0.01f) {
                        val imageBitmap = remember(refBitmap) { refBitmap.asImageBitmap() }
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawImage(
                                image = imageBitmap,
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                alpha = state.referenceAlpha.coerceIn(0f, 1f)
                            )
                        }
                    }

                    // Layer 2: Subtle Alignment Grid (if enabled in settings)
                    if (settings.showCanvasGrid) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val gridCols = 8
                            val gridRows = 10
                            val cellW = size.width / gridCols
                            val cellH = size.height / gridRows
                            val lineColor = ElectricViolet.copy(alpha = 0.10f)
                            for (c in 1 until gridCols) {
                                drawLine(
                                    color = lineColor,
                                    start = Offset(c * cellW, 0f),
                                    end = Offset(c * cellW, size.height),
                                    strokeWidth = 1f
                                )
                            }
                            for (r in 1 until gridRows) {
                                drawLine(
                                    color = lineColor,
                                    start = Offset(0f, r * cellH),
                                    end = Offset(size.width, r * cellH),
                                    strokeWidth = 1f
                                )
                            }
                        }
                    }

                    // Layer 3: Dedicated Offscreen Stroke Layer so Eraser (BlendMode.Clear) erases ONLY strokes!
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    ) {
                        val scaleX = size.width / cw
                        val scaleY = size.height / ch

                        fun drawSingleStroke(stroke: StrokeData) {
                            val pts = stroke.points
                            if (pts.isEmpty()) return
                            val scaledWidth = (stroke.strokeWidth * scaleX).coerceAtLeast(1.5f)
                            val blend = if (stroke.isEraser) BlendMode.Clear else BlendMode.SrcOver
                            val color = if (stroke.isEraser) {
                                Color.Transparent
                            } else {
                                Color(stroke.colorArgb).copy(alpha = stroke.alpha)
                            }

                            if (pts.size == 1) {
                                val p = pts.first()
                                drawCircle(
                                    color = color,
                                    radius = scaledWidth / 2f,
                                    center = Offset(p.x * scaleX, p.y * scaleY),
                                    blendMode = blend
                                )
                            } else {
                                val path = Path().apply {
                                    val first = pts.first()
                                    moveTo(first.x * scaleX, first.y * scaleY)
                                    for (i in 1 until pts.size) {
                                        val prev = pts[i - 1]
                                        val curr = pts[i]
                                        val midX = ((prev.x + curr.x) / 2f) * scaleX
                                        val midY = ((prev.y + curr.y) / 2f) * scaleY
                                        quadraticTo(prev.x * scaleX, prev.y * scaleY, midX, midY)
                                    }
                                    val last = pts.last()
                                    lineTo(last.x * scaleX, last.y * scaleY)
                                }
                                drawPath(
                                    path = path,
                                    color = color,
                                    style = Stroke(
                                        width = scaledWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    ),
                                    blendMode = blend
                                )
                            }
                        }

                        state.strokes.forEach { drawSingleStroke(it) }

                        // Active in-progress user stroke
                        if (state.activeStrokePoints.isNotEmpty()) {
                            drawSingleStroke(
                                StrokeData(
                                    points = state.activeStrokePoints,
                                    colorArgb = state.brushColorArgb,
                                    strokeWidth = state.brushSize,
                                    alpha = if (state.activeTool == DrawingTool.ERASER) 1f else state.brushOpacity,
                                    isEraser = state.activeTool == DrawingTool.ERASER
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. Top Floating Glass Command Bar (Hidden in Fullscreen mode)
        AnimatedVisibility(
            visible = !state.isFullscreenMode,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            KagamiGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("workspace_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = KagamiWhite
                            )
                        }

                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = state.projectTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                color = KagamiWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${(state.zoomScale * 100).roundToInt()}% • ${state.appliedMode.title}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = LuminousViolet
                            )
                        }
                    }

                    // Undo / Redo / Clear / Fullscreen / Save Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onUndo,
                            enabled = state.canUndo,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("undo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (state.canUndo) KagamiWhite else KagamiDimText.copy(alpha = 0.4f)
                            )
                        }

                        IconButton(
                            onClick = onRedo,
                            enabled = state.canRedo,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("redo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo",
                                tint = if (state.canRedo) KagamiWhite else KagamiDimText.copy(alpha = 0.4f)
                            )
                        }

                        IconButton(
                            onClick = onClearCanvas,
                            enabled = state.strokes.isNotEmpty(),
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("clear_canvas_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Strokes",
                                tint = if (state.strokes.isNotEmpty()) SubtleMagenta else KagamiDimText.copy(alpha = 0.4f)
                            )
                        }

                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("fullscreen_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen Canvas",
                                tint = LuminousViolet
                            )
                        }

                        IconButton(
                            onClick = { onSetExportSheetVisible(true) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ElectricViolet.copy(alpha = 0.25f))
                                .border(1.dp, ElectricViolet, CircleShape)
                                .testTag("save_export_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Save or Export",
                                tint = KagamiWhite
                            )
                        }
                    }
                }
            }
        }

        // Fullscreen exit pill when in distraction-free mode
        AnimatedVisibility(
            visible = state.isFullscreenMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xD90E081C),
                border = BorderStroke(1.dp, ElectricViolet),
                modifier = Modifier
                    .clickable(onClick = onToggleFullscreen)
                    .testTag("exit_fullscreen_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        tint = LuminousViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Exit Fullscreen",
                        style = MaterialTheme.typography.labelMedium,
                        color = KagamiWhite
                    )
                }
            }
        }

        // 3. Floating Zoom & Fit Side Pill (Right Edge)
        AnimatedVisibility(
            visible = !state.isFullscreenMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
        ) {
            KagamiGlassCard(
                cornerRadius = 50.dp,
                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onStepZoom(1.25f) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("zoom_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom In",
                            tint = KagamiWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onResetFit,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("fit_canvas_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Fit Canvas",
                            tint = LuminousViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onStepZoom(0.8f) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("zoom_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOut,
                            contentDescription = "Zoom Out",
                            tint = KagamiWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 4. Live Auto-Trace Progress Pill or Status Banner
        AnimatedVisibility(
            visible = state.isAutoTracingLive || state.statusBannerMessage != null,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = if (state.isFullscreenMode) 16.dp else 74.dp, start = 24.dp, end = 24.dp)
        ) {
            KagamiGlassCard(
                cornerRadius = 50.dp,
                borderColor = if (state.isStatusError) StatusDanger else ElectricViolet,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (state.isStatusError) StatusDanger else SubtleMagenta,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = state.statusBannerMessage ?: "Auto-Drawing vector strokes...",
                            style = MaterialTheme.typography.labelMedium,
                            color = KagamiWhite
                        )
                    }
                    if (state.isAutoTracingLive) {
                        LinearProgressIndicator(
                            progress = { state.autoTraceProgress },
                            modifier = Modifier
                                .width(180.dp)
                                .height(4.dp)
                                .clip(CircleShape),
                            color = ElectricViolet,
                            trackColor = DeepPurple.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        // 5. Bottom Dock & Expandable Brush / Layer Control Panels
        AnimatedVisibility(
            visible = !state.isFullscreenMode,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Expandable Brush Size, Opacity & Color Palette Panel
                AnimatedVisibility(visible = showBrushTunerPanel) {
                    KagamiGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("brush_tuner_panel"),
                        cornerRadius = 22.dp,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (state.activeTool == DrawingTool.ERASER) {
                                        "ERASER SIZE: ${state.brushSize.roundToInt()} px"
                                    } else {
                                        "BRUSH SIZE: ${state.brushSize.roundToInt()} px • OPACITY ${(state.brushOpacity * 100).roundToInt()}%"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LuminousViolet
                                )
                                IconButton(
                                    onClick = { showBrushTunerPanel = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Brush Panel",
                                        tint = KagamiMutedText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Brush Size Slider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Size",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KagamiMutedText,
                                    modifier = Modifier.width(46.dp)
                                )
                                Slider(
                                    value = state.brushSize,
                                    onValueChange = onBrushSizeChanged,
                                    valueRange = 2f..54f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = KagamiWhite,
                                        activeTrackColor = ElectricViolet,
                                        inactiveTrackColor = DeepPurple.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("brush_size_slider")
                                )
                            }

                            // Brush Opacity Slider (when in Brush mode)
                            if (state.activeTool != DrawingTool.ERASER) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Alpha",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KagamiMutedText,
                                        modifier = Modifier.width(46.dp)
                                    )
                                    Slider(
                                        value = state.brushOpacity,
                                        onValueChange = onBrushOpacityChanged,
                                        valueRange = 0.15f..1f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = KagamiWhite,
                                            activeTrackColor = SubtleMagenta,
                                            inactiveTrackColor = DeepPurple.copy(alpha = 0.35f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("brush_opacity_slider")
                                    )
                                }

                                // Color Swatches Row
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    items(KagamiBrushPalette) { swatchColor ->
                                        val argb = swatchColor.toArgb()
                                        val isSelected = state.brushColorArgb == argb
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) SubtleMagenta else KagamiCardBorder,
                                                    shape = CircleShape
                                                )
                                                .clickable { onBrushColorSelected(argb) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected Color",
                                                    tint = if (swatchColor == Color.White) OledBlack else KagamiWhite,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Expandable Reference Trace Opacity & Paper Tone Panel
                AnimatedVisibility(visible = showLayerTunerPanel) {
                    KagamiGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("layer_tuner_panel"),
                        cornerRadius = 22.dp,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TRACE LAYER & CANVAS PAPER",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LuminousViolet
                                )
                                IconButton(
                                    onClick = { showLayerTunerPanel = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Layer Panel",
                                        tint = KagamiMutedText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Reference Trace Opacity Slider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconButton(
                                    onClick = onToggleReferenceVisibility,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (state.showReferenceLayer) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Reference Visibility",
                                        tint = if (state.showReferenceLayer) LuminousViolet else KagamiDimText
                                    )
                                }
                                Text(
                                    text = "Ref ${(state.referenceAlpha * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KagamiWhite,
                                    modifier = Modifier.width(64.dp)
                                )
                                Slider(
                                    value = if (state.showReferenceLayer) state.referenceAlpha else 0f,
                                    onValueChange = onReferenceAlphaChanged,
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = KagamiWhite,
                                        activeTrackColor = ElectricViolet
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Paper Background Tone Selector + Import New Image button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val paperOptions = listOf(
                                        "White" to android.graphics.Color.WHITE,
                                        "Slate" to 0xFF140D26.toInt(),
                                        "OLED" to 0xFF05030A.toInt()
                                    )
                                    paperOptions.forEach { (label, argb) ->
                                        val selected = state.canvasBackgroundArgb == argb
                                        Surface(
                                            shape = CircleShape,
                                            color = if (selected) DeepPurple else KagamiSurface,
                                            border = BorderStroke(
                                                1.dp,
                                                if (selected) LuminousViolet else KagamiCardBorder
                                            ),
                                            modifier = Modifier.clickable { onPaperColorChanged(argb) }
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = KagamiWhite,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = ElectricViolet.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, ElectricViolet),
                                    modifier = Modifier.clickable(onClick = onSelectNewImage)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Change Image",
                                            tint = LuminousViolet,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Change Image",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = KagamiWhite
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Main Primary Floating Tool Dock
                KagamiGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 26.dp,
                    glowAccent = true,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WorkspaceDockToolButton(
                            label = "Brush",
                            icon = Icons.Default.Brush,
                            selected = state.activeTool == DrawingTool.BRUSH,
                            accentDotColor = Color(state.brushColorArgb),
                            onClick = {
                                if (state.activeTool == DrawingTool.BRUSH) {
                                    showBrushTunerPanel = !showBrushTunerPanel
                                    showLayerTunerPanel = false
                                } else {
                                    onSelectTool(DrawingTool.BRUSH)
                                    showBrushTunerPanel = true
                                    showLayerTunerPanel = false
                                }
                            },
                            modifier = Modifier.testTag("tool_brush_button")
                        )

                        WorkspaceDockToolButton(
                            label = "Eraser",
                            icon = Icons.Default.CleaningServices,
                            selected = state.activeTool == DrawingTool.ERASER,
                            onClick = {
                                if (state.activeTool == DrawingTool.ERASER) {
                                    showBrushTunerPanel = !showBrushTunerPanel
                                    showLayerTunerPanel = false
                                } else {
                                    onSelectTool(DrawingTool.ERASER)
                                    showBrushTunerPanel = true
                                    showLayerTunerPanel = false
                                }
                            },
                            modifier = Modifier.testTag("tool_eraser_button")
                        )

                        // Center Highlighted Auto-Draw Engine Button
                        WorkspaceDockToolButton(
                            label = "Auto Draw",
                            icon = Icons.Default.AutoFixHigh,
                            selected = state.showAutoDrawSheet,
                            highlightGlow = true,
                            onClick = {
                                showBrushTunerPanel = false
                                showLayerTunerPanel = false
                                onOpenAutoDrawEngine()
                            },
                            modifier = Modifier.testTag("tool_autodraw_button")
                        )

                        WorkspaceDockToolButton(
                            label = "Layers",
                            icon = Icons.Default.Layers,
                            selected = showLayerTunerPanel,
                            onClick = {
                                showLayerTunerPanel = !showLayerTunerPanel
                                showBrushTunerPanel = false
                            },
                            modifier = Modifier.testTag("tool_layers_button")
                        )

                        WorkspaceDockToolButton(
                            label = "Pan/Zoom",
                            icon = Icons.Default.PanTool,
                            selected = state.activeTool == DrawingTool.PAN_ZOOM,
                            onClick = {
                                showBrushTunerPanel = false
                                showLayerTunerPanel = false
                                onSelectTool(
                                    if (state.activeTool == DrawingTool.PAN_ZOOM) DrawingTool.BRUSH else DrawingTool.PAN_ZOOM
                                )
                            },
                            modifier = Modifier.testTag("tool_pan_button")
                        )
                    }
                }
            }
        }

        // 6. Auto-Draw Engine Modal Bottom Sheet (Live Preview + Intensity Slider + Vector Auto-Trace)
        if (state.showAutoDrawSheet) {
            ModalBottomSheet(
                onDismissRequest = onCloseAutoDrawEngine,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF0C0618),
                scrimColor = Color(0xB3000000),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("autodraw_engine_sheet"),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "KAGAMI AUTO-DRAW ENGINE",
                                style = MaterialTheme.typography.titleLarge,
                                color = KagamiWhite
                            )
                            Text(
                                text = "${state.previewMode.title} (${state.previewMode.japaneseTag}) • Live Preview",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = LuminousViolet
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = ElectricViolet.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, ElectricViolet),
                            modifier = Modifier.clickable(onClick = onSelectNewImage)
                        ) {
                            Text(
                                text = "Switch Image",
                                style = MaterialTheme.typography.labelSmall,
                                color = KagamiWhite,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Live Processed Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(OledBlack)
                            .border(1.dp, KagamiCardBorder, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = state.previewBitmap ?: state.processedReferenceBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Live Filter Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }
                        if (state.isProcessingPreview) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color(0x66000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = ElectricViolet,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // Filter Mode Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(AutoDrawMode.entries, key = { it.id }) { mode ->
                            val selected = state.previewMode == mode
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) DeepPurple else KagamiSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) LuminousViolet else KagamiCardBorder.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .clickable {
                                        onUpdateAutoDrawPreview(
                                            mode,
                                            state.previewIntensity,
                                            state.previewInvert
                                        )
                                    }
                                    .testTag("autodraw_mode_${mode.id.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                                        color = KagamiWhite
                                    )
                                    Text(
                                        text = mode.japaneseTag,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = if (selected) SubtleMagenta else KagamiMutedText
                                    )
                                }
                            }
                        }
                    }

                    // Adjustable Intensity Slider + Invert Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Effect Intensity",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = KagamiWhite
                                )
                                Text(
                                    text = "${(state.previewIntensity * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LuminousViolet
                                )
                            }
                            Slider(
                                value = state.previewIntensity,
                                onValueChange = { newIntensity ->
                                    onUpdateAutoDrawPreview(
                                        state.previewMode,
                                        newIntensity,
                                        state.previewInvert
                                    )
                                },
                                valueRange = 0.1f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = KagamiWhite,
                                    activeTrackColor = ElectricViolet
                                ),
                                modifier = Modifier.testTag("autodraw_intensity_slider")
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Dark Ink",
                                style = MaterialTheme.typography.labelSmall,
                                color = KagamiMutedText
                            )
                            Switch(
                                checked = state.previewInvert,
                                onCheckedChange = { inv ->
                                    onUpdateAutoDrawPreview(
                                        state.previewMode,
                                        state.previewIntensity,
                                        inv
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = KagamiWhite,
                                    checkedTrackColor = ElectricViolet
                                )
                            )
                        }
                    }

                    // Action Buttons: Commit Reference Layer OR Live Auto-Trace Vector Strokes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KagamiPillButton(
                            text = "Apply Layer",
                            icon = Icons.Default.Check,
                            isPrimary = false,
                            onClick = onCommitAutoDrawToCanvas,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("apply_autodraw_button")
                        )

                        KagamiPillButton(
                            text = "Auto-Draw Strokes",
                            icon = Icons.Default.PlayArrow,
                            isPrimary = true,
                            onClick = onTriggerLiveVectorAutoTrace,
                            modifier = Modifier
                                .weight(1.25f)
                                .testTag("live_autotrace_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // 7. Save & Export Modal Bottom Sheet
        if (state.showExportSheet) {
            ModalBottomSheet(
                onDismissRequest = { onSetExportSheetVisible(false) },
                containerColor = Color(0xFF0C0618),
                scrimColor = Color(0xB3000000),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("save_export_sheet"),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "SAVE & EXPORT ARTWORK",
                        style = MaterialTheme.typography.titleLarge,
                        color = KagamiWhite
                    )
                    Text(
                        text = "Save your editable project to the local KAGAMI Archive or export a high-resolution image directly to your Android Gallery.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                        color = KagamiMutedText
                    )

                    OutlinedTextField(
                        value = state.projectTitle,
                        onValueChange = onUpdateProjectTitle,
                        label = { Text("Artwork Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = KagamiCardBorder,
                            focusedTextColor = KagamiWhite,
                            unfocusedTextColor = KagamiWhite,
                            focusedLabelColor = LuminousViolet,
                            unfocusedLabelColor = KagamiMutedText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_title_input")
                    )

                    KagamiPillButton(
                        text = "Save Project to Local Archive",
                        icon = Icons.Default.Save,
                        isPrimary = false,
                        enabled = !state.isSavingOrExporting,
                        onClick = onSaveProjectLocal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_project_archive_button")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KagamiPillButton(
                            text = "Export PNG",
                            subtitleTag = "Lossless Studio Quality",
                            icon = Icons.Default.Download,
                            isPrimary = true,
                            enabled = !state.isSavingOrExporting,
                            onClick = { requestExportWithPermissionCheck("PNG") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_png_button")
                        )

                        KagamiPillButton(
                            text = "Export JPG",
                            subtitleTag = "Compact (${settings.jpgQuality}%)",
                            icon = Icons.Default.Download,
                            isPrimary = false,
                            enabled = !state.isSavingOrExporting,
                            onClick = { requestExportWithPermissionCheck("JPG") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_jpg_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun WorkspaceDockToolButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlightGlow: Boolean = false,
    accentDotColor: Color? = null
) {
    val bgBrush = when {
        selected -> Brush.verticalGradient(listOf(ElectricViolet, DeepPurple))
        highlightGlow -> Brush.verticalGradient(
            listOf(
                SubtleMagenta.copy(alpha = 0.28f),
                DeepPurple.copy(alpha = 0.35f)
            )
        )
        else -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bgBrush)
            .border(
                width = 1.dp,
                color = when {
                    selected -> LuminousViolet
                    highlightGlow -> SubtleMagenta.copy(alpha = 0.6f)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected || highlightGlow) KagamiWhite else KagamiMutedText,
                modifier = Modifier.size(22.dp)
            )
            if (accentDotColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentDotColor)
                        .border(1.dp, KagamiWhite, CircleShape)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected || highlightGlow) KagamiWhite else KagamiMutedText
        )
    }
}
