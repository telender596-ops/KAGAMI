package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.KagamiDatabase
import com.example.data.KagamiPreferencesRepository
import com.example.data.KagamiProject
import com.example.data.KagamiSettings
import com.example.engine.AutoDrawMode
import com.example.engine.KagamiAutoDrawEngine
import com.example.engine.StrokeData
import com.example.external.ExternalPathConfig
import com.example.external.KagamiExternalDrawingManager
import com.example.external.KagamiPathPipeline
import com.example.external.SmartImageProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class KagamiScreen {
    SPLASH,
    HOME,
    IMAGE_SELECT,
    WORKSPACE,
    SETTINGS
}

enum class HomeTab {
    STUDIO,
    EXTERNAL_OVERLAY,
    ARCHIVE,
    CONFIG
}

enum class DrawingTool {
    BRUSH,
    ERASER,
    PAN_ZOOM
}

sealed interface CanvasHistoryAction {
    data class AddStrokes(val added: List<StrokeData>) : CanvasHistoryAction
    data class ClearCanvas(
        val previousStrokes: List<StrokeData>,
        val previousMode: AutoDrawMode,
        val previousIntensity: Float
    ) : CanvasHistoryAction
    data class ChangeAutoDrawEffect(
        val fromMode: AutoDrawMode,
        val fromIntensity: Float,
        val toMode: AutoDrawMode,
        val toIntensity: Float,
        val fromInvert: Boolean,
        val toInvert: Boolean
    ) : CanvasHistoryAction
}

data class WorkspaceUiState(
    val currentProjectId: Long? = null,
    val projectTitle: String = "KAGAMI Study #01",
    val canvasWidth: Int = 1080,
    val canvasHeight: Int = 1350,
    val canvasBackgroundArgb: Int = Color.WHITE,
    // Pending image in Image Selection screen
    val pendingPreviewBitmap: Bitmap? = null,
    val pendingSourceLabel: String = "",
    val pendingSmartProfile: SmartImageProfile? = null,
    val imageSelectionError: String? = null,
    val isLoadingImage: Boolean = false,
    // Active Workspace reference & processed bitmaps
    val originalReferenceBitmap: Bitmap? = null,
    val processedReferenceBitmap: Bitmap? = null,
    val appliedMode: AutoDrawMode = AutoDrawMode.EDGE_OUTLINE,
    val appliedIntensity: Float = 0.62f,
    val appliedInvert: Boolean = false,
    val referenceAlpha: Float = 0.85f,
    val showReferenceLayer: Boolean = true,
    // Auto Draw Engine Modal Preview State
    val showAutoDrawSheet: Boolean = false,
    val previewMode: AutoDrawMode = AutoDrawMode.EDGE_OUTLINE,
    val previewIntensity: Float = 0.62f,
    val previewInvert: Boolean = false,
    val previewBitmap: Bitmap? = null,
    val isProcessingPreview: Boolean = false,
    val isAutoTracingLive: Boolean = false,
    val autoTraceProgress: Float = 0f,
    // Drawing & Brush State
    val activeTool: DrawingTool = DrawingTool.BRUSH,
    val brushColorArgb: Int = 0xFF090514.toInt(),
    val brushSize: Float = 8f,
    val brushOpacity: Float = 1f,
    val strokes: List<StrokeData> = emptyList(),
    val activeStrokePoints: List<Offset> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    // Viewport Transform & UI Chrome
    val zoomScale: Float = 1f,
    val panOffset: Offset = Offset.Zero,
    val isFullscreenMode: Boolean = false,
    val showExportSheet: Boolean = false,
    val isSavingOrExporting: Boolean = false,
    val statusBannerMessage: String? = null,
    val isStatusError: Boolean = false
)

class KagamiViewModel(application: Application) : AndroidViewModel(application) {

    private val database = KagamiDatabase.getDatabase(application)
    private val projectDao = database.projectDao()
    private val prefsRepo = KagamiPreferencesRepository(application)

    private val _currentScreen = MutableStateFlow(KagamiScreen.HOME)
    val currentScreen: StateFlow<KagamiScreen> = _currentScreen.asStateFlow()

    private val _homeTab = MutableStateFlow(HomeTab.STUDIO)
    val homeTab: StateFlow<HomeTab> = _homeTab.asStateFlow()

    val settingsState: StateFlow<KagamiSettings> = prefsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KagamiSettings()
    )

    val recentProjects: StateFlow<List<KagamiProject>> = projectDao.observeAllProjects().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _workspaceState = MutableStateFlow(WorkspaceUiState())
    val workspaceState: StateFlow<WorkspaceUiState> = _workspaceState.asStateFlow()

    // Undo / Redo Stacks
    private val undoStack = ArrayDeque<CanvasHistoryAction>()
    private val redoStack = ArrayDeque<CanvasHistoryAction>()

    private var previewJob: Job? = null
    private var autoTraceJob: Job? = null

    init {
        viewModelScope.launch {
            val initialSettings = prefsRepo.settingsFlow.first()
            _workspaceState.update {
                it.copy(
                    brushSize = initialSettings.defaultBrushSize,
                    canvasBackgroundArgb = paperToneToArgb(initialSettings.defaultPaperTone)
                )
            }
        }
    }

    private fun paperToneToArgb(tone: String): Int = when (tone) {
        "DARK_SLATE" -> 0xFF140D26.toInt()
        "OLED_VOID" -> 0xFF05030A.toInt()
        else -> Color.WHITE
    }

    fun finishSplash() {
        if (_currentScreen.value == KagamiScreen.SPLASH) {
            _currentScreen.value = KagamiScreen.HOME
        }
    }

    fun selectHomeTab(tab: HomeTab) {
        _homeTab.value = tab
    }

    fun navigateTo(screen: KagamiScreen) {
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        return when (_currentScreen.value) {
            KagamiScreen.WORKSPACE -> {
                if (_workspaceState.value.isFullscreenMode) {
                    toggleFullscreenMode()
                    true
                } else {
                    autoSaveCurrentProjectQuietly()
                    _currentScreen.value = KagamiScreen.HOME
                    true
                }
            }
            KagamiScreen.IMAGE_SELECT -> {
                _currentScreen.value = KagamiScreen.HOME
                true
            }
            KagamiScreen.SETTINGS -> {
                _currentScreen.value = KagamiScreen.HOME
                true
            }
            KagamiScreen.HOME -> {
                if (_homeTab.value != HomeTab.STUDIO) {
                    _homeTab.value = HomeTab.STUDIO
                    true
                } else {
                    false
                }
            }
            KagamiScreen.SPLASH -> false
        }
    }

    // --- IMAGE SELECTION & WORKSPACE INITIALIZATION ---

    fun openImageSelectionScreen() {
        _workspaceState.update {
            it.copy(
                imageSelectionError = null,
                isLoadingImage = false
            )
        }
        _currentScreen.value = KagamiScreen.IMAGE_SELECT
    }

    fun onGalleryImagePicked(uri: Uri?) {
        if (uri == null) {
            // User canceled picker gracefully - do not crash, show clean feedback if no image was loaded yet
            if (_workspaceState.value.pendingPreviewBitmap == null) {
                _workspaceState.update {
                    it.copy(
                        imageSelectionError = "Image selection canceled. Pick a gallery image or load the KAGAMI Shrine sample below.",
                        isLoadingImage = false
                    )
                }
            }
            return
        }

        viewModelScope.launch {
            _workspaceState.update { it.copy(isLoadingImage = true, imageSelectionError = null) }
            val bmp = KagamiAutoDrawEngine.loadOptimizedBitmap(
                context = getApplication(),
                uri = uri,
                maxDimension = 1080
            )
            if (bmp != null) {
                val smartProfile = KagamiPathPipeline.analyzeImageSmartProfile(bmp)
                _workspaceState.update {
                    it.copy(
                        pendingPreviewBitmap = bmp,
                        pendingSourceLabel = "Device Gallery Image (${bmp.width}×${bmp.height})",
                        pendingSmartProfile = smartProfile,
                        isLoadingImage = false,
                        imageSelectionError = null
                    )
                }
            } else {
                _workspaceState.update {
                    it.copy(
                        isLoadingImage = false,
                        imageSelectionError = "Could not decode the selected image file. Please try another PNG or JPG photo."
                    )
                }
            }
        }
    }

    fun loadSampleReferenceForSelection(andOpenWorkspaceImmediately: Boolean = false, presetMode: AutoDrawMode? = null) {
        viewModelScope.launch {
            _workspaceState.update { it.copy(isLoadingImage = true, imageSelectionError = null) }
            val bmp = KagamiAutoDrawEngine.loadOptimizedBitmap(
                context = getApplication(),
                uri = null,
                drawableResId = R.drawable.img_sample_reference,
                maxDimension = 1080
            )
            if (bmp != null) {
                val smartProfile = KagamiPathPipeline.analyzeImageSmartProfile(bmp)
                _workspaceState.update {
                    it.copy(
                        pendingPreviewBitmap = bmp,
                        pendingSourceLabel = "KAGAMI Cyber-Shrine Reference (${bmp.width}×${bmp.height})",
                        pendingSmartProfile = smartProfile,
                        isLoadingImage = false,
                        imageSelectionError = null
                    )
                }
                if (andOpenWorkspaceImmediately) {
                    confirmSelectedImageAndStartWorkspace(initialMode = presetMode ?: smartProfile.recommendedInternalMode)
                }
            } else {
                _workspaceState.update {
                    it.copy(
                        isLoadingImage = false,
                        imageSelectionError = "Unable to load built-in reference asset."
                    )
                }
            }
        }
    }

    fun startBlankCanvasWorkspace() {
        autoTraceJob?.cancel()
        undoStack.clear()
        redoStack.clear()
        val settings = settingsState.value
        val bgArgb = paperToneToArgb(settings.defaultPaperTone)
        val defaultInk = if (bgArgb == Color.WHITE) 0xFF090514.toInt() else Color.WHITE
        val projectCount = recentProjects.value.size + 1

        _workspaceState.update {
            WorkspaceUiState(
                currentProjectId = null,
                projectTitle = "KAGAMI Sketch #0$projectCount",
                canvasWidth = 1080,
                canvasHeight = 1350,
                canvasBackgroundArgb = bgArgb,
                originalReferenceBitmap = null,
                processedReferenceBitmap = null,
                showReferenceLayer = false,
                brushColorArgb = defaultInk,
                brushSize = settings.defaultBrushSize,
                activeTool = DrawingTool.BRUSH
            )
        }
        _currentScreen.value = KagamiScreen.WORKSPACE
    }

    fun confirmSelectedImageAndStartWorkspace(initialMode: AutoDrawMode = AutoDrawMode.EDGE_OUTLINE) {
        val pendingBmp = _workspaceState.value.pendingPreviewBitmap ?: return
        autoTraceJob?.cancel()
        undoStack.clear()
        redoStack.clear()

        viewModelScope.launch {
            _workspaceState.update { it.copy(isLoadingImage = true) }
            val settings = settingsState.value
            val bgArgb = paperToneToArgb(settings.defaultPaperTone)
            val isDarkPaper = bgArgb != Color.WHITE
            val processed = KagamiAutoDrawEngine.processImage(
                source = pendingBmp,
                mode = initialMode,
                intensity = initialMode.defaultIntensity,
                invertOutput = isDarkPaper
            )
            val projectCount = recentProjects.value.size + 1
            val defaultInk = if (isDarkPaper) 0xFFA78BFA.toInt() else 0xFF090514.toInt()

            _workspaceState.update {
                WorkspaceUiState(
                    currentProjectId = null,
                    projectTitle = "KAGAMI AutoStudy #0$projectCount",
                    canvasWidth = pendingBmp.width,
                    canvasHeight = pendingBmp.height,
                    canvasBackgroundArgb = bgArgb,
                    pendingPreviewBitmap = pendingBmp,
                    originalReferenceBitmap = pendingBmp,
                    processedReferenceBitmap = processed,
                    appliedMode = initialMode,
                    appliedIntensity = initialMode.defaultIntensity,
                    appliedInvert = isDarkPaper,
                    referenceAlpha = 0.85f,
                    showReferenceLayer = true,
                    previewMode = initialMode,
                    previewIntensity = initialMode.defaultIntensity,
                    previewInvert = isDarkPaper,
                    previewBitmap = processed,
                    brushColorArgb = defaultInk,
                    brushSize = settings.defaultBrushSize,
                    activeTool = DrawingTool.BRUSH,
                    statusBannerMessage = "Applied ${initialMode.title} (${(initialMode.defaultIntensity * 100).toInt()}%)"
                )
            }
            _currentScreen.value = KagamiScreen.WORKSPACE
        }
    }

    // --- DRAWING & HISTORY MANAGEMENT ---

    fun selectTool(tool: DrawingTool) {
        _workspaceState.update { it.copy(activeTool = tool) }
    }

    fun setBrushColor(colorArgb: Int) {
        _workspaceState.update {
            it.copy(
                brushColorArgb = colorArgb,
                activeTool = if (it.activeTool == DrawingTool.ERASER) DrawingTool.BRUSH else it.activeTool
            )
        }
    }

    fun setBrushSize(size: Float) {
        _workspaceState.update { it.copy(brushSize = size.coerceIn(1.5f, 64f)) }
    }

    fun setBrushOpacity(opacity: Float) {
        _workspaceState.update { it.copy(brushOpacity = opacity.coerceIn(0.1f, 1f)) }
    }

    fun setReferenceAlpha(alpha: Float) {
        _workspaceState.update { it.copy(referenceAlpha = alpha.coerceIn(0f, 1f), showReferenceLayer = alpha > 0.02f) }
    }

    fun toggleReferenceVisibility() {
        _workspaceState.update { state ->
            val nextVisible = !state.showReferenceLayer
            state.copy(
                showReferenceLayer = nextVisible,
                referenceAlpha = if (nextVisible && state.referenceAlpha < 0.1f) 0.75f else state.referenceAlpha
            )
        }
    }

    fun setCanvasPaperColor(colorArgb: Int) {
        _workspaceState.update { it.copy(canvasBackgroundArgb = colorArgb) }
    }

    fun onStrokeStart(canvasPoint: Offset) {
        if (_workspaceState.value.activeTool == DrawingTool.PAN_ZOOM) return
        _workspaceState.update {
            it.copy(activeStrokePoints = listOf(canvasPoint))
        }
    }

    fun onStrokeMove(canvasPoint: Offset) {
        val state = _workspaceState.value
        if (state.activeTool == DrawingTool.PAN_ZOOM || state.activeStrokePoints.isEmpty()) return
        val lastPt = state.activeStrokePoints.last()
        val dx = canvasPoint.x - lastPt.x
        val dy = canvasPoint.y - lastPt.y
        if (dx * dx + dy * dy < 1.8f) return

        // Apply subtle smoothing based on user preferences
        val smoothing = settingsState.value.brushSmoothing.coerceIn(0f, 0.85f)
        val smoothedPt = Offset(
            x = lastPt.x * smoothing + canvasPoint.x * (1f - smoothing),
            y = lastPt.y * smoothing + canvasPoint.y * (1f - smoothing)
        )
        _workspaceState.update {
            it.copy(activeStrokePoints = it.activeStrokePoints + smoothedPt)
        }
    }

    fun onStrokeEnd() {
        val state = _workspaceState.value
        if (state.activeStrokePoints.isEmpty()) return
        val newStroke = StrokeData(
            points = state.activeStrokePoints,
            colorArgb = state.brushColorArgb,
            strokeWidth = state.brushSize,
            alpha = if (state.activeTool == DrawingTool.ERASER) 1f else state.brushOpacity,
            isEraser = state.activeTool == DrawingTool.ERASER,
            isAutoGenerated = false
        )
        pushHistoryAction(CanvasHistoryAction.AddStrokes(listOf(newStroke)))
        _workspaceState.update {
            it.copy(
                strokes = it.strokes + newStroke,
                activeStrokePoints = emptyList(),
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun onStrokeCancel() {
        _workspaceState.update { it.copy(activeStrokePoints = emptyList()) }
    }

    private fun pushHistoryAction(action: CanvasHistoryAction) {
        undoStack.addLast(action)
        if (undoStack.size > 50) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        autoTraceJob?.cancel()
        val action = undoStack.removeLast()
        redoStack.addLast(action)

        when (action) {
            is CanvasHistoryAction.AddStrokes -> {
                val removeCount = action.added.size
                _workspaceState.update { state ->
                    val updated = state.strokes.dropLast(removeCount.coerceAtMost(state.strokes.size))
                    state.copy(
                        strokes = updated,
                        isAutoTracingLive = false,
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty(),
                        statusBannerMessage = "Undid stroke"
                    )
                }
            }

            is CanvasHistoryAction.ClearCanvas -> {
                _workspaceState.update { state ->
                    state.copy(
                        strokes = action.previousStrokes,
                        appliedMode = action.previousMode,
                        appliedIntensity = action.previousIntensity,
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty(),
                        statusBannerMessage = "Restored cleared canvas"
                    )
                }
                recomputeAppliedReferenceBitmap()
            }

            is CanvasHistoryAction.ChangeAutoDrawEffect -> {
                _workspaceState.update { state ->
                    state.copy(
                        appliedMode = action.fromMode,
                        appliedIntensity = action.fromIntensity,
                        appliedInvert = action.fromInvert,
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty(),
                        statusBannerMessage = "Reverted filter to ${action.fromMode.title}"
                    )
                }
                recomputeAppliedReferenceBitmap()
            }
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        autoTraceJob?.cancel()
        val action = redoStack.removeLast()
        undoStack.addLast(action)

        when (action) {
            is CanvasHistoryAction.AddStrokes -> {
                _workspaceState.update { state ->
                    state.copy(
                        strokes = state.strokes + action.added,
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty(),
                        statusBannerMessage = "Redid stroke"
                    )
                }
            }

            is CanvasHistoryAction.ClearCanvas -> {
                _workspaceState.update { state ->
                    state.copy(
                        strokes = emptyList(),
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty(),
                        statusBannerMessage = "Cleared strokes"
                    )
                }
            }

            is CanvasHistoryAction.ChangeAutoDrawEffect -> {
                _workspaceState.update { state ->
                    state.copy(
                        appliedMode = action.toMode,
                        appliedIntensity = action.toIntensity,
                        appliedInvert = action.toInvert,
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty(),
                        statusBannerMessage = "Reapplied ${action.toMode.title}"
                    )
                }
                recomputeAppliedReferenceBitmap()
            }
        }
    }

    fun clearCanvasStrokes() {
        autoTraceJob?.cancel()
        val state = _workspaceState.value
        if (state.strokes.isEmpty()) return
        pushHistoryAction(
            CanvasHistoryAction.ClearCanvas(
                previousStrokes = state.strokes,
                previousMode = state.appliedMode,
                previousIntensity = state.appliedIntensity
            )
        )
        _workspaceState.update {
            it.copy(
                strokes = emptyList(),
                isAutoTracingLive = false,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                statusBannerMessage = "Canvas strokes cleared"
            )
        }
    }

    // --- VIEWPORT ZOOM, PAN & FULLSCREEN ---

    fun updateZoomAndPan(scaleChange: Float, panChange: Offset) {
        _workspaceState.update { state ->
            val newScale = (state.zoomScale * scaleChange).coerceIn(0.5f, 5.5f)
            val maxPan = 1400f * newScale
            val newPan = Offset(
                x = (state.panOffset.x + panChange.x).coerceIn(-maxPan, maxPan),
                y = (state.panOffset.y + panChange.y).coerceIn(-maxPan, maxPan)
            )
            state.copy(zoomScale = newScale, panOffset = newPan)
        }
    }

    fun stepZoom(multiplier: Float) {
        _workspaceState.update { state ->
            val newScale = (state.zoomScale * multiplier).coerceIn(0.5f, 5.5f)
            state.copy(zoomScale = newScale)
        }
    }

    fun resetViewportFit() {
        _workspaceState.update {
            it.copy(
                zoomScale = 1f,
                panOffset = Offset.Zero,
                statusBannerMessage = "Viewport fitted to 100%"
            )
        }
    }

    fun toggleFullscreenMode() {
        _workspaceState.update { it.copy(isFullscreenMode = !it.isFullscreenMode) }
    }

    // --- AUTO DRAW / IMAGE PROCESSING ENGINE ---

    fun openAutoDrawEngineSheet() {
        val state = _workspaceState.value
        // If user opened a blank canvas without a reference image yet, load the sample reference so Auto Draw works immediately!
        if (state.originalReferenceBitmap == null) {
            viewModelScope.launch {
                val sampleBmp = KagamiAutoDrawEngine.loadOptimizedBitmap(
                    context = getApplication(),
                    uri = null,
                    drawableResId = R.drawable.img_sample_reference,
                    maxDimension = 1080
                )
                if (sampleBmp != null) {
                    val processed = KagamiAutoDrawEngine.processImage(
                        source = sampleBmp,
                        mode = state.appliedMode,
                        intensity = state.appliedIntensity,
                        invertOutput = state.appliedInvert
                    )
                    _workspaceState.update {
                        it.copy(
                            originalReferenceBitmap = sampleBmp,
                            processedReferenceBitmap = processed,
                            previewBitmap = processed,
                            previewMode = it.appliedMode,
                            previewIntensity = it.appliedIntensity,
                            previewInvert = it.appliedInvert,
                            showReferenceLayer = true,
                            showAutoDrawSheet = true
                        )
                    }
                }
            }
        } else {
            _workspaceState.update {
                it.copy(
                    showAutoDrawSheet = true,
                    previewMode = it.appliedMode,
                    previewIntensity = it.appliedIntensity,
                    previewInvert = it.appliedInvert,
                    previewBitmap = it.processedReferenceBitmap ?: it.originalReferenceBitmap
                )
            }
        }
    }

    fun closeAutoDrawEngineSheet() {
        _workspaceState.update { it.copy(showAutoDrawSheet = false) }
    }

    fun updateAutoDrawPreview(
        mode: AutoDrawMode = _workspaceState.value.previewMode,
        intensity: Float = _workspaceState.value.previewIntensity,
        invert: Boolean = _workspaceState.value.previewInvert
    ) {
        val source = _workspaceState.value.originalReferenceBitmap ?: return
        _workspaceState.update {
            it.copy(
                previewMode = mode,
                previewIntensity = intensity,
                previewInvert = invert,
                isProcessingPreview = true
            )
        }
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(35) // Debounce slider drags for silky 60fps UI
            val result = KagamiAutoDrawEngine.processImage(
                source = source,
                mode = mode,
                intensity = intensity,
                invertOutput = invert
            )
            _workspaceState.update {
                it.copy(
                    previewBitmap = result,
                    isProcessingPreview = false
                )
            }
        }
    }

    fun commitAutoDrawPreviewToCanvas() {
        val state = _workspaceState.value
        val previewBmp = state.previewBitmap ?: return
        if (state.appliedMode != state.previewMode ||
            state.appliedIntensity != state.previewIntensity ||
            state.appliedInvert != state.previewInvert
        ) {
            pushHistoryAction(
                CanvasHistoryAction.ChangeAutoDrawEffect(
                    fromMode = state.appliedMode,
                    fromIntensity = state.appliedIntensity,
                    toMode = state.previewMode,
                    toIntensity = state.previewIntensity,
                    fromInvert = state.appliedInvert,
                    toInvert = state.previewInvert
                )
            )
        }
        _workspaceState.update {
            it.copy(
                processedReferenceBitmap = previewBmp,
                appliedMode = state.previewMode,
                appliedIntensity = state.previewIntensity,
                appliedInvert = state.previewInvert,
                showReferenceLayer = true,
                referenceAlpha = if (state.referenceAlpha < 0.15f) 0.85f else state.referenceAlpha,
                showAutoDrawSheet = false,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                statusBannerMessage = "Committed ${state.previewMode.title} (${(state.previewIntensity * 100).toInt()}%)"
            )
        }
    }

    /**
     * Runs the signature Auto-Draw animated vector stroke plotting directly onto the canvas!
     */
    fun triggerLiveVectorAutoTrace() {
        val state = _workspaceState.value
        val source = state.originalReferenceBitmap ?: return
        autoTraceJob?.cancel()
        autoTraceJob = viewModelScope.launch {
            _workspaceState.update {
                it.copy(
                    showAutoDrawSheet = false,
                    isAutoTracingLive = true,
                    autoTraceProgress = 0.05f,
                    statusBannerMessage = "Extracting KAGAMI vector line contours..."
                )
            }

            val autoStrokes = KagamiAutoDrawEngine.extractVectorAutoStrokes(
                source = source,
                intensity = state.previewIntensity,
                strokeColorArgb = state.brushColorArgb,
                strokeWidth = state.brushSize.coerceIn(2.5f, 6.5f)
            )

            if (autoStrokes.isEmpty()) {
                _workspaceState.update {
                    it.copy(
                        isAutoTracingLive = false,
                        autoTraceProgress = 0f,
                        statusBannerMessage = "No strong contours found at current intensity. Try increasing intensity."
                    )
                }
                return@launch
            }

            // Record entire batch as a single undoable operation
            pushHistoryAction(CanvasHistoryAction.AddStrokes(autoStrokes))

            // Animate strokes appearing progressively on the canvas
            val total = autoStrokes.size
            val batchSize = (total / 25).coerceAtLeast(2)
            var index = 0
            while (index < total) {
                val end = (index + batchSize).coerceAtMost(total)
                val slice = autoStrokes.subList(index, end)
                val progress = end.toFloat() / total.toFloat()
                _workspaceState.update { current ->
                    current.copy(
                        strokes = current.strokes + slice,
                        autoTraceProgress = progress,
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty()
                    )
                }
                index = end
                delay(24)
            }

            _workspaceState.update {
                it.copy(
                    isAutoTracingLive = false,
                    autoTraceProgress = 1f,
                    statusBannerMessage = "Auto-Draw plotted $total vector strokes (Undoable)"
                )
            }
        }
    }

    private fun recomputeAppliedReferenceBitmap() {
        val state = _workspaceState.value
        val src = state.originalReferenceBitmap ?: return
        viewModelScope.launch {
            val processed = KagamiAutoDrawEngine.processImage(
                source = src,
                mode = state.appliedMode,
                intensity = state.appliedIntensity,
                invertOutput = state.appliedInvert
            )
            _workspaceState.update { it.copy(processedReferenceBitmap = processed) }
        }
    }

    // --- LOCAL PROJECT PERSISTENCE & GALLERY EXPORT ---

    fun setExportSheetVisible(visible: Boolean) {
        _workspaceState.update { it.copy(showExportSheet = visible) }
    }

    fun updateProjectTitle(newTitle: String) {
        _workspaceState.update { it.copy(projectTitle = newTitle.take(40)) }
    }

    fun saveProjectToLocalArchive(showConfirmation: Boolean = true) {
        val state = _workspaceState.value
        viewModelScope.launch {
            if (showConfirmation) {
                _workspaceState.update { it.copy(isSavingOrExporting = true) }
            }
            try {
                val context = getApplication<Application>()
                val composite = KagamiAutoDrawEngine.renderCompositeBitmap(
                    width = state.canvasWidth,
                    height = state.canvasHeight,
                    canvasBackgroundColorArgb = state.canvasBackgroundArgb,
                    processedReference = if (state.showReferenceLayer) state.processedReferenceBitmap else null,
                    referenceAlpha = state.referenceAlpha,
                    strokes = state.strokes
                )
                val stamp = System.currentTimeMillis()
                val thumbPath = KagamiAutoDrawEngine.saveInternalBitmap(
                    context = context,
                    bitmap = composite,
                    filename = "thumb_$stamp.png"
                )
                val basePath = state.originalReferenceBitmap?.let { orig ->
                    KagamiAutoDrawEngine.saveInternalBitmap(
                        context = context,
                        bitmap = orig,
                        filename = "base_$stamp.png"
                    )
                }
                val strokesJson = KagamiAutoDrawEngine.serializeStrokesToJson(state.strokes)

                val project = KagamiProject(
                    id = state.currentProjectId ?: 0L,
                    title = state.projectTitle.ifBlank { "KAGAMI Study" },
                    thumbnailPath = thumbPath,
                    baseLayerPath = basePath,
                    drawingLayerPath = thumbPath,
                    strokesJson = strokesJson,
                    filterMode = state.appliedMode.id,
                    filterIntensity = state.appliedIntensity,
                    referenceAlpha = if (state.showReferenceLayer) state.referenceAlpha else 0f,
                    canvasBackgroundHex = state.canvasBackgroundArgb.toLong(),
                    width = state.canvasWidth,
                    height = state.canvasHeight,
                    updatedAt = stamp
                )

                val savedId = projectDao.insertProject(project)
                _workspaceState.update {
                    it.copy(
                        currentProjectId = savedId,
                        isSavingOrExporting = false,
                        showExportSheet = false,
                        statusBannerMessage = if (showConfirmation) "Project saved to KAGAMI Archive" else it.statusBannerMessage,
                        isStatusError = false
                    )
                }
            } catch (e: Exception) {
                _workspaceState.update {
                    it.copy(
                        isSavingOrExporting = false,
                        statusBannerMessage = "Could not save project locally: ${e.localizedMessage ?: "Error"}",
                        isStatusError = true
                    )
                }
            }
        }
    }

    private fun autoSaveCurrentProjectQuietly() {
        val state = _workspaceState.value
        if (state.strokes.isNotEmpty() || state.originalReferenceBitmap != null) {
            saveProjectToLocalArchive(showConfirmation = false)
        }
    }

    fun openSavedProject(project: KagamiProject) {
        autoTraceJob?.cancel()
        undoStack.clear()
        redoStack.clear()
        viewModelScope.launch {
            val baseBitmap = project.baseLayerPath?.let { path ->
                val f = File(path)
                if (f.exists()) BitmapFactory.decodeFile(path) else null
            }
            val mode = AutoDrawMode.fromId(project.filterMode)
            val isDarkBg = project.canvasBackgroundHex.toInt() != Color.WHITE
            val processed = baseBitmap?.let {
                KagamiAutoDrawEngine.processImage(
                    source = it,
                    mode = mode,
                    intensity = project.filterIntensity,
                    invertOutput = isDarkBg
                )
            }
            val restoredStrokes = KagamiAutoDrawEngine.deserializeStrokesFromJson(project.strokesJson)

            _workspaceState.update {
                WorkspaceUiState(
                    currentProjectId = project.id,
                    projectTitle = project.title,
                    canvasWidth = project.width,
                    canvasHeight = project.height,
                    canvasBackgroundArgb = project.canvasBackgroundHex.toInt(),
                    originalReferenceBitmap = baseBitmap,
                    processedReferenceBitmap = processed,
                    appliedMode = mode,
                    appliedIntensity = project.filterIntensity,
                    appliedInvert = isDarkBg,
                    referenceAlpha = project.referenceAlpha,
                    showReferenceLayer = project.referenceAlpha > 0.02f && baseBitmap != null,
                    previewMode = mode,
                    previewIntensity = project.filterIntensity,
                    previewInvert = isDarkBg,
                    previewBitmap = processed,
                    strokes = restoredStrokes,
                    brushSize = settingsState.value.defaultBrushSize,
                    statusBannerMessage = "Reopened \"${project.title}\""
                )
            }
            _currentScreen.value = KagamiScreen.WORKSPACE
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            projectDao.deleteProjectById(projectId)
        }
    }

    fun exportFinishedArtwork(format: String) {
        val state = _workspaceState.value
        viewModelScope.launch {
            _workspaceState.update { it.copy(isSavingOrExporting = true) }
            val composite = KagamiAutoDrawEngine.renderCompositeBitmap(
                width = state.canvasWidth,
                height = state.canvasHeight,
                canvasBackgroundColorArgb = state.canvasBackgroundArgb,
                processedReference = if (state.showReferenceLayer) state.processedReferenceBitmap else null,
                referenceAlpha = state.referenceAlpha,
                strokes = state.strokes
            )
            val result = KagamiAutoDrawEngine.exportToGallery(
                context = getApplication(),
                bitmap = composite,
                format = format,
                jpgQuality = settingsState.value.jpgQuality,
                projectTitle = state.projectTitle
            )
            // Also persist to local archive so work isn't lost
            saveProjectToLocalArchive(showConfirmation = false)

            result.fold(
                onSuccess = { savedPath ->
                    _workspaceState.update {
                        it.copy(
                            isSavingOrExporting = false,
                            showExportSheet = false,
                            statusBannerMessage = "Exported $format to $savedPath",
                            isStatusError = false
                        )
                    }
                },
                onFailure = { err ->
                    _workspaceState.update {
                        it.copy(
                            isSavingOrExporting = false,
                            statusBannerMessage = "Export failed: ${err.localizedMessage ?: "Storage error"}",
                            isStatusError = true
                        )
                    }
                }
            )
        }
    }

    fun clearStatusBanner() {
        _workspaceState.update { it.copy(statusBannerMessage = null, isStatusError = false) }
    }

    // --- SETTINGS ACTIONS ---

    fun updatePureOledTheme(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setPureOledTheme(enabled) }
    }

    fun updateShowParticles(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setShowParticles(enabled) }
    }

    fun updateDefaultBrushSize(size: Float) {
        viewModelScope.launch { prefsRepo.setDefaultBrushSize(size) }
    }

    fun updateBrushSmoothing(smoothing: Float) {
        viewModelScope.launch { prefsRepo.setBrushSmoothing(smoothing) }
    }

    fun updateShowCanvasGrid(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setShowCanvasGrid(enabled) }
    }

    fun updateDefaultPaperTone(tone: String) {
        viewModelScope.launch { prefsRepo.setDefaultPaperTone(tone) }
    }

    fun updateExportFormat(format: String) {
        viewModelScope.launch { prefsRepo.setExportFormat(format) }
    }

    fun updateJpgQuality(quality: Int) {
        viewModelScope.launch { prefsRepo.setJpgQuality(quality) }
    }

    fun resetAllPreferences() {
        viewModelScope.launch { prefsRepo.resetAllPreferences() }
    }

    // --- EXTERNAL AUTO-DRAW OVERLAY PIPELINE ACTIONS ---

    private var externalPipelineJob: Job? = null

    fun sendPendingImageToExternalAutoDraw() {
        val pendingBmp = _workspaceState.value.pendingPreviewBitmap ?: return
        viewModelScope.launch {
            _homeTab.value = HomeTab.EXTERNAL_OVERLAY
            _currentScreen.value = KagamiScreen.HOME
            KagamiExternalDrawingManager.setExtracting(true)
            val profile = _workspaceState.value.pendingSmartProfile
                ?: KagamiPathPipeline.analyzeImageSmartProfile(pendingBmp)
            val autoCfg = profile.recommendedExternalConfig.copy(
                drawingSpeed = KagamiExternalDrawingManager.state.value.activeSpeedPreset.multiplier,
                strokePauseMs = KagamiExternalDrawingManager.state.value.activeSpeedPreset.pauseMs
            )
            val res = KagamiPathPipeline.generateNaturalPaths(pendingBmp, autoCfg)
            KagamiExternalDrawingManager.setSourceAndPaths(
                bitmap = pendingBmp,
                result = res.copy(smartProfile = profile),
                config = autoCfg,
                smartProfile = profile
            )
        }
    }

    fun pickImageForExternalAutoDraw(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            KagamiExternalDrawingManager.setExtracting(true)
            val bmp = KagamiAutoDrawEngine.loadOptimizedBitmap(
                context = getApplication(),
                uri = uri,
                maxDimension = 1080
            )
            if (bmp != null) {
                val profile = KagamiPathPipeline.analyzeImageSmartProfile(bmp)
                val autoCfg = profile.recommendedExternalConfig.copy(
                    drawingSpeed = KagamiExternalDrawingManager.state.value.activeSpeedPreset.multiplier,
                    strokePauseMs = KagamiExternalDrawingManager.state.value.activeSpeedPreset.pauseMs
                )
                val res = KagamiPathPipeline.generateNaturalPaths(bmp, autoCfg)
                KagamiExternalDrawingManager.setSourceAndPaths(
                    bitmap = bmp,
                    result = res.copy(smartProfile = profile),
                    config = autoCfg,
                    smartProfile = profile
                )
            } else {
                KagamiExternalDrawingManager.setExtracting(false)
            }
        }
    }

    fun loadSampleForExternalAutoDraw() {
        viewModelScope.launch {
            KagamiExternalDrawingManager.setExtracting(true)
            val bmp = KagamiAutoDrawEngine.loadOptimizedBitmap(
                context = getApplication(),
                uri = null,
                drawableResId = R.drawable.img_sample_reference,
                maxDimension = 1080
            )
            if (bmp != null) {
                val profile = KagamiPathPipeline.analyzeImageSmartProfile(bmp)
                val autoCfg = profile.recommendedExternalConfig.copy(
                    drawingSpeed = KagamiExternalDrawingManager.state.value.activeSpeedPreset.multiplier,
                    strokePauseMs = KagamiExternalDrawingManager.state.value.activeSpeedPreset.pauseMs
                )
                val res = KagamiPathPipeline.generateNaturalPaths(bmp, autoCfg)
                KagamiExternalDrawingManager.setSourceAndPaths(
                    bitmap = bmp,
                    result = res.copy(smartProfile = profile),
                    config = autoCfg,
                    smartProfile = profile
                )
            } else {
                KagamiExternalDrawingManager.setExtracting(false)
            }
        }
    }

    fun reapplySmartAutoSetupForExternal() {
        val currentState = KagamiExternalDrawingManager.state.value
        val sourceBmp = currentState.sourceBitmap ?: return
        viewModelScope.launch {
            KagamiExternalDrawingManager.setExtracting(true)
            val profile = currentState.smartProfile
                ?: KagamiPathPipeline.analyzeImageSmartProfile(sourceBmp)
            val autoCfg = profile.recommendedExternalConfig.copy(
                drawingSpeed = currentState.activeSpeedPreset.multiplier,
                strokePauseMs = currentState.activeSpeedPreset.pauseMs
            )
            val res = KagamiPathPipeline.generateNaturalPaths(sourceBmp, autoCfg)
            KagamiExternalDrawingManager.setSourceAndPaths(
                bitmap = sourceBmp,
                result = res.copy(smartProfile = profile),
                config = autoCfg,
                smartProfile = profile
            )
        }
    }

    fun updateExternalPathConfigAndReprocess(newConfig: ExternalPathConfig) {
        val currentState = KagamiExternalDrawingManager.state.value
        val oldConfig = currentState.pathConfig
        KagamiExternalDrawingManager.updatePathConfig(newConfig)

        // Only re-run the pixel pipeline if extraction parameters changed (speed/thickness/aspect are instant)
        val needsReExtraction =
            oldConfig.detailLevel != newConfig.detailLevel ||
            oldConfig.smoothness != newConfig.smoothness ||
            oldConfig.noiseReduction != newConfig.noiseReduction ||
            oldConfig.simplification != newConfig.simplification

        val sourceBmp = currentState.sourceBitmap
        if (needsReExtraction && sourceBmp != null) {
            externalPipelineJob?.cancel()
            externalPipelineJob = viewModelScope.launch {
                delay(80) // Debounce slider drags
                KagamiExternalDrawingManager.setExtracting(true)
                val res = KagamiPathPipeline.generateNaturalPaths(sourceBmp, newConfig)
                KagamiExternalDrawingManager.setSourceAndPaths(sourceBmp, res, newConfig)
            }
        }
    }
}
