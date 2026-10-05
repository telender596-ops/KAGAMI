package com.example.external

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Android AccessibilityService dedicated strictly to dispatching user-authorized drawing strokes
 * onto the user's calibrated target canvas.
 * Never reads or inspects window content, text, passwords, or personal data.
 */
class KagamiGestureService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var drawingJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        KagamiExternalDrawingManager.setGestureServiceConnected(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally no-op: KAGAMI never inspects or collects screen content or window hierarchy.
    }

    override fun onInterrupt() {
        stopDrawingInternal()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        stopDrawingInternal()
        if (instance === this) {
            instance = null
        }
        KagamiExternalDrawingManager.setGestureServiceConnected(false)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stopDrawingInternal()
        if (instance === this) {
            instance = null
        }
        KagamiExternalDrawingManager.setGestureServiceConnected(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun stopDrawingInternal() {
        drawingJob?.cancel()
        drawingJob = null
    }

    private fun startOrResumeDrawing(startIndex: Int) {
        drawingJob?.cancel()
        drawingJob = serviceScope.launch {
            try {
                val initialSnap = KagamiExternalDrawingManager.state.value
                val paths = initialSnap.extractedPaths
                if (paths.isEmpty()) {
                    KagamiExternalDrawingManager.updateProgress(
                        strokeIdx = 0,
                        status = ExternalJobStatus.IDLE,
                        message = "No paths loaded to draw."
                    )
                    return@launch
                }

                var idx = startIndex.coerceIn(0, paths.lastIndex)
                KagamiExternalDrawingManager.updateProgress(
                    strokeIdx = idx,
                    status = ExternalJobStatus.DRAWING,
                    message = "Drawing stroke ${idx + 1} / ${paths.size}..."
                )

                // Brief 350ms settle delay so the user's finger tap on the overlay Start button finishes cleanly
                delay(350L)

                var consecutiveFailures = 0

                while (isActive && idx < paths.size) {
                    val liveState = KagamiExternalDrawingManager.state.value
                    if (liveState.jobStatus == ExternalJobStatus.PAUSED) {
                        delay(120L)
                        continue
                    }
                    if (liveState.jobStatus != ExternalJobStatus.DRAWING) {
                        break
                    }

                    val normPath = paths[idx]
                    val quad = liveState.calibrationQuad
                    val cfg = liveState.pathConfig

                    val screenPts = normPath.points.map { pt ->
                        KagamiPathPipeline.mapNormalizedToScreen(
                            pt = pt,
                            sourceWidth = liveState.sourceWidth,
                            sourceHeight = liveState.sourceHeight,
                            quad = quad,
                            preserveAspectRatio = cfg.preserveAspectRatio
                        )
                    }

                    if (screenPts.isNotEmpty()) {
                        val androidPath = Path()
                        val first = screenPts.first()
                        androidPath.moveTo(first.x, first.y)
                        if (screenPts.size == 1) {
                            androidPath.lineTo(first.x + 1.5f, first.y + 1.5f)
                        } else {
                            for (i in 1 until screenPts.size) {
                                val p = screenPts[i]
                                androidPath.lineTo(p.x, p.y)
                            }
                        }

                        // Compute natural human-like stroke duration based on length & speed
                        val speedFactor = cfg.drawingSpeed.coerceIn(0.25f, 3.0f)
                        val baseDurationMs = ((normPath.lengthNormalized * 520f) / speedFactor)
                            .roundToLong()
                            .coerceIn(28L, 900L)

                        val jitterMultiplier = if (cfg.naturalVelocityVariation) {
                            0.88f + Random.nextFloat() * 0.24f
                        } else {
                            1.0f
                        }
                        val finalDurationMs = (baseDurationMs * jitterMultiplier).roundToLong().coerceIn(24L, 1200L)

                        val success = dispatchSingleStrokeGesture(androidPath, finalDurationMs)
                        if (!success) {
                            consecutiveFailures++
                            if (consecutiveFailures >= 3) {
                                KagamiExternalDrawingManager.reportUnsupportedCanvas()
                                return@launch
                            }
                        } else {
                            consecutiveFailures = 0
                        }
                    }

                    idx++
                    if (idx < paths.size) {
                        KagamiExternalDrawingManager.updateProgress(
                            strokeIdx = idx,
                            status = ExternalJobStatus.DRAWING,
                            message = "Drawing stroke ${idx + 1} / ${paths.size}"
                        )
                        val pauseMs = (cfg.strokePauseMs / cfg.drawingSpeed.coerceAtLeast(0.5f))
                            .roundToLong()
                            .coerceIn(18L, 260L)
                        delay(pauseMs)
                    }
                }

                if (isActive && idx >= paths.size) {
                    KagamiExternalDrawingManager.updateProgress(
                        strokeIdx = paths.size,
                        status = ExternalJobStatus.COMPLETED,
                        message = "Completed all ${paths.size} strokes!"
                    )
                }
            } catch (_: CancellationException) {
                // Clean cancellation when paused or stopped
            } catch (e: Exception) {
                KagamiExternalDrawingManager.reportUnsupportedCanvas()
            }
        }
    }

    private suspend fun dispatchSingleStrokeGesture(
        path: Path,
        durationMs: Long
    ): Boolean = suspendCancellableCoroutine { cont ->
        try {
            val strokeDesc = GestureDescription.StrokeDescription(path, 0L, durationMs)
            val gesture = GestureDescription.Builder()
                .addStroke(strokeDesc)
                .build()

            val dispatched = dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (cont.isActive) cont.resume(false)
                    }
                },
                null
            )
            if (!dispatched && cont.isActive) {
                cont.resume(false)
            }
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(false)
        }
    }

    companion object {
        @Volatile
        private var instance: KagamiGestureService? = null

        fun isConnected(): Boolean = instance != null

        fun startExternalDrawingFromBeginning() {
            val service = instance
            if (service == null) {
                KagamiExternalDrawingManager.reportUnsupportedCanvas()
                return
            }
            service.startOrResumeDrawing(0)
        }

        fun pauseExternalDrawing() {
            val current = KagamiExternalDrawingManager.state.value
            if (current.jobStatus == ExternalJobStatus.DRAWING) {
                KagamiExternalDrawingManager.updateProgress(
                    strokeIdx = current.currentStrokeIndex,
                    status = ExternalJobStatus.PAUSED,
                    message = "Paused at stroke ${current.currentStrokeIndex} / ${current.totalStrokes}"
                )
            }
        }

        fun resumeExternalDrawing() {
            val service = instance
            if (service == null) {
                KagamiExternalDrawingManager.reportUnsupportedCanvas()
                return
            }
            val current = KagamiExternalDrawingManager.state.value
            service.startOrResumeDrawing(current.currentStrokeIndex)
        }

        fun stopCurrentDrawingImmediately() {
            instance?.stopDrawingInternal()
        }
    }
}
