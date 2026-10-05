package com.example.external

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ExternalJobStatus {
    IDLE,
    READY,
    CALIBRATING,
    PREVIEWING,
    DRAWING,
    PAUSED,
    COMPLETED,
    UNSUPPORTED_CANVAS_ERROR
}

enum class CalibrationUiMode {
    AUTO_VISUAL,      // Primary beginner-friendly visual rectangle with 1-tap Confirm
    FINE_TUNE_TOUCH,  // Simple visual Move, Resize, Rotate, Zoom controls
    MANUAL_CORNERS    // Advanced 4-corner (TL, TR, BL, BR) adjustment
}

data class ExternalDrawSessionState(
    val sourceBitmap: Bitmap? = null,
    val sourceWidth: Int = 1080,
    val sourceHeight: Int = 1080,
    val smartProfile: SmartImageProfile? = null,
    val pathConfig: ExternalPathConfig = ExternalPathConfig(),
    val extractedPaths: List<NormalizedPath> = emptyList(),
    val totalPointsCount: Int = 0,
    val isExtractingPaths: Boolean = false,
    val isWePlayModeActive: Boolean = false,
    val activePreset: TargetCanvasPreset = TargetCanvasPreset.UNIVERSAL_AUTO,
    val activeSpeedPreset: SpeedPreset = SpeedPreset.NORMAL,
    val calibrationUiMode: CalibrationUiMode = CalibrationUiMode.AUTO_VISUAL,
    val screenWidthPx: Int = 1080,
    val screenHeightPx: Int = 2400,
    val calibrationQuad: CalibrationQuad = CalibrationQuad.defaultForScreen(1080, 2400),
    val hasUserCalibrated: Boolean = false,
    val isOverlayServiceRunning: Boolean = false,
    val isGestureServiceConnected: Boolean = false,
    val jobStatus: ExternalJobStatus = ExternalJobStatus.IDLE,
    val currentStrokeIndex: Int = 0,
    val isOverlayCollapsed: Boolean = false,
    val showOverlaySettingsPanel: Boolean = false,
    val statusMessage: String = "Select an image for Smart Auto analysis."
) {
    val totalStrokes: Int get() = extractedPaths.size
    val progressFraction: Float
        get() = if (extractedPaths.isEmpty()) 0f else (currentStrokeIndex.toFloat() / extractedPaths.size.toFloat()).coerceIn(0f, 1f)
}

object KagamiExternalDrawingManager {

    private val _state = MutableStateFlow(ExternalDrawSessionState())
    val state: StateFlow<ExternalDrawSessionState> = _state.asStateFlow()

    fun updateScreenDimensions(widthPx: Int, heightPx: Int) {
        if (widthPx <= 100 || heightPx <= 100) return
        _state.update { current ->
            val aspect = if (current.sourceHeight > 0) {
                current.sourceWidth.toFloat() / current.sourceHeight.toFloat()
            } else {
                1f
            }
            val updatedQuad = if (!current.hasUserCalibrated) {
                CalibrationQuad.fromPreset(current.activePreset, widthPx, heightPx, aspect)
            } else {
                current.calibrationQuad
            }
            current.copy(
                screenWidthPx = widthPx,
                screenHeightPx = heightPx,
                calibrationQuad = updatedQuad
            )
        }
    }

    fun updateScreenDimensionsIfUncalibrated(widthPx: Int, heightPx: Int) {
        updateScreenDimensions(widthPx, heightPx)
    }

    fun setOverlayServiceRunning(running: Boolean) {
        _state.update {
            it.copy(
                isOverlayServiceRunning = running,
                jobStatus = if (!running && it.jobStatus == ExternalJobStatus.DRAWING) {
                    ExternalJobStatus.IDLE
                } else {
                    it.jobStatus
                }
            )
        }
    }

    fun setGestureServiceConnected(connected: Boolean) {
        _state.update { it.copy(isGestureServiceConnected = connected) }
    }

    fun setSourceAndPaths(
        bitmap: Bitmap,
        result: PathExtractionResult,
        config: ExternalPathConfig,
        smartProfile: SmartImageProfile? = result.smartProfile
    ) {
        _state.update { current ->
            val srcAspect = result.sourceWidth.toFloat() / result.sourceHeight.coerceAtLeast(1).toFloat()
            val autoQuad = if (!current.hasUserCalibrated) {
                CalibrationQuad.fromPreset(
                    preset = current.activePreset,
                    screenWidth = current.screenWidthPx,
                    screenHeight = current.screenHeightPx,
                    sourceAspect = srcAspect
                )
            } else {
                current.calibrationQuad
            }
            current.copy(
                sourceBitmap = bitmap,
                sourceWidth = result.sourceWidth,
                sourceHeight = result.sourceHeight,
                smartProfile = smartProfile ?: current.smartProfile,
                pathConfig = config,
                extractedPaths = result.paths,
                totalPointsCount = result.totalPoints,
                isExtractingPaths = false,
                calibrationQuad = autoQuad,
                currentStrokeIndex = 0,
                jobStatus = if (result.paths.isNotEmpty()) ExternalJobStatus.READY else ExternalJobStatus.IDLE,
                statusMessage = if (result.paths.isNotEmpty()) {
                    "${smartProfile?.summaryLabel ?: "Ready"} • ${result.paths.size} clean strokes"
                } else {
                    "No paths detected. Try another image."
                }
            )
        }
    }

    fun setExtracting(extracting: Boolean) {
        _state.update { it.copy(isExtractingPaths = extracting) }
    }

    fun updatePathConfig(newConfig: ExternalPathConfig) {
        _state.update { it.copy(pathConfig = newConfig) }
    }

    /**
     * Activates Dedicated WePlay Mode or Universal Mode with 1-tap auto calibration & recommended settings.
     */
    fun setWePlayMode(enabled: Boolean) {
        _state.update { current ->
            val preset = if (enabled) TargetCanvasPreset.WEPLAY_PARTY else TargetCanvasPreset.UNIVERSAL_AUTO
            val srcAspect = current.sourceWidth.toFloat() / current.sourceHeight.coerceAtLeast(1).toFloat()
            val autoQuad = CalibrationQuad.fromPreset(
                preset = preset,
                screenWidth = current.screenWidthPx,
                screenHeight = current.screenHeightPx,
                sourceAspect = srcAspect
            )
            current.copy(
                isWePlayModeActive = enabled,
                activePreset = preset,
                calibrationQuad = autoQuad,
                hasUserCalibrated = false,
                statusMessage = if (enabled) {
                    "WePlay Mode Active • Auto-calibrated for WePlay canvas"
                } else {
                    "Universal Canvas Mode • Auto-calibrated to screen"
                }
            )
        }
    }

    /**
     * One-tap Smart Auto Calibration for the selected preset (WePlay Party, WePlay Square, Universal Auto, Wide).
     */
    fun applySmartAutoCalibration(preset: TargetCanvasPreset = _state.value.activePreset) {
        _state.update { current ->
            val srcAspect = current.sourceWidth.toFloat() / current.sourceHeight.coerceAtLeast(1).toFloat()
            val autoQuad = CalibrationQuad.fromPreset(
                preset = preset,
                screenWidth = current.screenWidthPx,
                screenHeight = current.screenHeightPx,
                sourceAspect = srcAspect
            )
            current.copy(
                activePreset = preset,
                isWePlayModeActive = preset.isWePlayPreset,
                calibrationQuad = autoQuad,
                calibrationUiMode = CalibrationUiMode.AUTO_VISUAL,
                hasUserCalibrated = true,
                statusMessage = "Auto-calibrated: ${preset.title}"
            )
        }
    }

    fun selectSpeedPreset(preset: SpeedPreset) {
        _state.update { current ->
            current.copy(
                activeSpeedPreset = preset,
                pathConfig = current.pathConfig.copy(
                    drawingSpeed = preset.multiplier,
                    strokePauseMs = preset.pauseMs
                ),
                statusMessage = "Speed set to ${preset.label} (${preset.multiplier}x)"
            )
        }
    }

    fun setCalibrationUiMode(mode: CalibrationUiMode) {
        _state.update { it.copy(calibrationUiMode = mode) }
    }

    fun applyFineTuneTransform(
        panDelta: Offset = Offset.Zero,
        scaleMultiplier: Float = 1.0f,
        rotationDeltaDeg: Float = 0f
    ) {
        _state.update { current ->
            val q = current.calibrationQuad
            val updated = q.copy(
                fineTuneOffsetX = (q.fineTuneOffsetX + panDelta.x).coerceIn(-800f, 800f),
                fineTuneOffsetY = (q.fineTuneOffsetY + panDelta.y).coerceIn(-1200f, 1200f),
                fineTuneScale = (q.fineTuneScale * scaleMultiplier).coerceIn(0.35f, 2.5f),
                fineTuneRotationDeg = (q.fineTuneRotationDeg + rotationDeltaDeg) % 360f
            )
            current.copy(
                calibrationQuad = updated,
                hasUserCalibrated = true
            )
        }
    }

    fun resetFineTuneTransform() {
        applySmartAutoCalibration(_state.value.activePreset)
    }

    fun updateCalibrationQuad(quad: CalibrationQuad) {
        _state.update {
            it.copy(
                calibrationQuad = quad,
                hasUserCalibrated = true
            )
        }
    }

    fun setOverlayCollapsed(collapsed: Boolean) {
        _state.update { it.copy(isOverlayCollapsed = collapsed) }
    }

    fun toggleOverlaySettingsPanel() {
        _state.update { it.copy(showOverlaySettingsPanel = !it.showOverlaySettingsPanel) }
    }

    fun enterCalibrationMode(uiMode: CalibrationUiMode = CalibrationUiMode.AUTO_VISUAL) {
        _state.update {
            it.copy(
                jobStatus = ExternalJobStatus.CALIBRATING,
                calibrationUiMode = uiMode,
                isOverlayCollapsed = false,
                statusMessage = "Visual Canvas Guide active. Tap 'Confirm Canvas' when ready."
            )
        }
    }

    fun enterPreviewMode() {
        _state.update {
            it.copy(
                jobStatus = ExternalJobStatus.PREVIEWING,
                isOverlayCollapsed = false,
                statusMessage = "Previewing ${it.extractedPaths.size} mapped strokes."
            )
        }
    }

    fun exitCalibrationOrPreviewMode() {
        _state.update {
            it.copy(
                jobStatus = if (it.extractedPaths.isNotEmpty()) ExternalJobStatus.READY else ExternalJobStatus.IDLE,
                hasUserCalibrated = true,
                statusMessage = "Canvas confirmed (${it.extractedPaths.size} strokes ready)."
            )
        }
    }

    fun updateProgress(strokeIdx: Int, status: ExternalJobStatus, message: String? = null) {
        _state.update {
            it.copy(
                currentStrokeIndex = strokeIdx.coerceIn(0, it.extractedPaths.size),
                jobStatus = status,
                statusMessage = message ?: it.statusMessage
            )
        }
    }

    fun reportUnsupportedCanvas() {
        _state.update {
            it.copy(
                jobStatus = ExternalJobStatus.UNSUPPORTED_CANVAS_ERROR,
                statusMessage = "External drawing is not supported on this canvas."
            )
        }
    }

    fun emergencyStop() {
        KagamiGestureService.stopCurrentDrawingImmediately()
        _state.update {
            it.copy(
                jobStatus = if (it.extractedPaths.isNotEmpty()) ExternalJobStatus.READY else ExternalJobStatus.IDLE,
                currentStrokeIndex = 0,
                statusMessage = "Stopped external drawing."
            )
        }
    }
}
