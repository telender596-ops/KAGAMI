package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import com.example.engine.AutoDrawMode
import com.example.engine.KagamiAutoDrawEngine
import com.example.external.CalibrationQuad
import com.example.external.CalibrationUiMode
import com.example.external.ExternalJobStatus
import com.example.external.ExternalPathConfig
import com.example.external.KagamiExternalDrawingManager
import com.example.external.KagamiGestureService
import com.example.external.KagamiPathPipeline
import com.example.external.SpeedPreset
import com.example.external.TargetCanvasPreset
import com.example.viewmodel.DrawingTool
import com.example.viewmodel.KagamiScreen
import com.example.viewmodel.KagamiViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read KAGAMI app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("KAGAMI", appName)
    }

    @Test
    fun `test AutoDrawEngine local image processing modes and stroke serialization`() = runBlocking {
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        for (y in 16..48) {
            for (x in 16..48) {
                bmp.setPixel(x, y, Color.rgb(20, 10, 40))
            }
        }

        for (mode in AutoDrawMode.entries) {
            val out = KagamiAutoDrawEngine.processImage(
                source = bmp,
                mode = mode,
                intensity = 0.65f,
                invertOutput = false
            )
            assertNotNull("Output bitmap should not be null for ${mode.id}", out)
            assertEquals(64, out.width)
            assertEquals(64, out.height)
        }

        val autoStrokes = KagamiAutoDrawEngine.extractVectorAutoStrokes(
            source = bmp,
            intensity = 0.7f,
            strokeColorArgb = Color.BLACK,
            strokeWidth = 4f
        )
        assertTrue("Expected extracted vector auto-strokes from high-contrast square", autoStrokes.isNotEmpty())

        val json = KagamiAutoDrawEngine.serializeStrokesToJson(autoStrokes)
        val restored = KagamiAutoDrawEngine.deserializeStrokesFromJson(json)
        assertEquals(autoStrokes.size, restored.size)
    }

    @Test
    fun `test External 11-stage path extraction, 4-point calibration mapping, and emergency stop`() = runBlocking {
        val bmp = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        for (y in 20..60) {
            for (x in 25..95) {
                bmp.setPixel(x, y, Color.BLACK)
            }
        }

        val cfg = ExternalPathConfig(
            detailLevel = 0.7f,
            smoothness = 0.75f,
            noiseReduction = 0.5f,
            simplification = 0.4f,
            preserveAspectRatio = true
        )
        val result = KagamiPathPipeline.generateNaturalPaths(bmp, cfg)
        assertTrue("Expected non-empty natural paths from 11-stage pipeline", result.paths.isNotEmpty())

        // Verify 4-point calibration mapping preserves bounds inside portrait & landscape quads
        val portraitQuad = CalibrationQuad(
            topLeft = Offset(100f, 200f),
            topRight = Offset(900f, 200f),
            bottomLeft = Offset(100f, 1400f),
            bottomRight = Offset(900f, 1400f)
        )
        val mappedCenter = KagamiPathPipeline.mapNormalizedToScreen(
            pt = Offset(0.5f, 0.5f),
            sourceWidth = 120,
            sourceHeight = 80,
            quad = portraitQuad,
            preserveAspectRatio = true
        )
        assertEquals(500f, mappedCenter.x, 1f)
        assertEquals(800f, mappedCenter.y, 1f)

        // Verify ExternalDrawingManager state transitions & unsupported canvas safety when service disconnected
        KagamiExternalDrawingManager.setSourceAndPaths(bmp, result, cfg)
        assertEquals(ExternalJobStatus.READY, KagamiExternalDrawingManager.state.value.jobStatus)

        KagamiExternalDrawingManager.enterCalibrationMode()
        assertEquals(ExternalJobStatus.CALIBRATING, KagamiExternalDrawingManager.state.value.jobStatus)

        KagamiExternalDrawingManager.enterPreviewMode()
        assertEquals(ExternalJobStatus.PREVIEWING, KagamiExternalDrawingManager.state.value.jobStatus)

        // Attempting external drawing without connected AccessibilityService must report unsupported canvas cleanly
        KagamiGestureService.startExternalDrawingFromBeginning()
        assertEquals(
            ExternalJobStatus.UNSUPPORTED_CANVAS_ERROR,
            KagamiExternalDrawingManager.state.value.jobStatus
        )
        assertEquals(
            "External drawing is not supported on this canvas.",
            KagamiExternalDrawingManager.state.value.statusMessage
        )

        // Emergency stop resets cleanly
        KagamiExternalDrawingManager.emergencyStop()
        assertEquals(ExternalJobStatus.READY, KagamiExternalDrawingManager.state.value.jobStatus)

        // Verify Smart Image Auto-Setup diagnostic analysis
        val smartProfile = KagamiPathPipeline.analyzeImageSmartProfile(bmp)
        assertNotNull(smartProfile)
        assertTrue(smartProfile.summaryLabel.startsWith("Smart Auto:"))

        // Verify WePlay Mode & Smart Auto Calibration presets + Fine Tune transforms
        KagamiExternalDrawingManager.setWePlayMode(true)
        assertTrue(KagamiExternalDrawingManager.state.value.isWePlayModeActive)
        assertEquals(TargetCanvasPreset.WEPLAY_PARTY, KagamiExternalDrawingManager.state.value.activePreset)

        KagamiExternalDrawingManager.selectSpeedPreset(SpeedPreset.FAST)
        assertEquals(SpeedPreset.FAST, KagamiExternalDrawingManager.state.value.activeSpeedPreset)

        KagamiExternalDrawingManager.setCalibrationUiMode(CalibrationUiMode.FINE_TUNE_TOUCH)
        KagamiExternalDrawingManager.applyFineTuneTransform(
            panDelta = Offset(25f, -15f),
            scaleMultiplier = 1.15f,
            rotationDeltaDeg = 15f
        )
        assertEquals(1.15f, KagamiExternalDrawingManager.state.value.calibrationQuad.fineTuneScale, 0.01f)
    }

    @Test
    fun `test KagamiViewModel drawing, brush size, eraser, undo and redo history`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = KagamiViewModel(app)

        vm.finishSplash()
        assertEquals(KagamiScreen.HOME, vm.currentScreen.value)

        vm.openImageSelectionScreen()
        assertEquals(KagamiScreen.IMAGE_SELECT, vm.currentScreen.value)
        vm.onGalleryImagePicked(null)
        assertNotNull(vm.workspaceState.value.imageSelectionError)

        vm.startBlankCanvasWorkspace()
        assertEquals(KagamiScreen.WORKSPACE, vm.currentScreen.value)
        assertFalse(vm.workspaceState.value.canUndo)
        assertFalse(vm.workspaceState.value.canRedo)

        vm.setBrushSize(14f)
        assertEquals(14f, vm.workspaceState.value.brushSize, 0.01f)
        vm.onStrokeStart(Offset(10f, 10f))
        vm.onStrokeMove(Offset(50f, 50f))
        vm.onStrokeMove(Offset(90f, 100f))
        vm.onStrokeEnd()

        assertEquals(1, vm.workspaceState.value.strokes.size)
        assertTrue(vm.workspaceState.value.canUndo)
        assertFalse(vm.workspaceState.value.canRedo)

        vm.selectTool(DrawingTool.ERASER)
        vm.onStrokeStart(Offset(20f, 20f))
        vm.onStrokeMove(Offset(60f, 60f))
        vm.onStrokeEnd()

        assertEquals(2, vm.workspaceState.value.strokes.size)
        assertTrue(vm.workspaceState.value.strokes.last().isEraser)

        vm.undo()
        assertEquals(1, vm.workspaceState.value.strokes.size)
        assertTrue(vm.workspaceState.value.canUndo)
        assertTrue(vm.workspaceState.value.canRedo)

        vm.undo()
        assertEquals(0, vm.workspaceState.value.strokes.size)
        assertFalse(vm.workspaceState.value.canUndo)
        assertTrue(vm.workspaceState.value.canRedo)

        vm.redo()
        assertEquals(1, vm.workspaceState.value.strokes.size)
        assertFalse(vm.workspaceState.value.strokes.first().isEraser)
    }
}
