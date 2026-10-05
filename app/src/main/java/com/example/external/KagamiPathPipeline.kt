package com.example.external

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import com.example.engine.AutoDrawMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Target canvas presets for Smart Auto Calibration & Dedicated WePlay Mode.
 */
enum class TargetCanvasPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val isWePlayPreset: Boolean = false
) {
    WEPLAY_PARTY(
        id = "WEPLAY_PARTY",
        title = "WePlay Draw & Guess",
        subtitle = "Auto-fits upper-center WePlay game board",
        isWePlayPreset = true
    ),
    WEPLAY_SQUARE(
        id = "WEPLAY_SQUARE",
        title = "WePlay 1:1 Board",
        subtitle = "Square centered WePlay quick-draw area",
        isWePlayPreset = true
    ),
    UNIVERSAL_AUTO(
        id = "UNIVERSAL_AUTO",
        title = "Smart Auto Canvas",
        subtitle = "Balanced center area for universal art apps",
        isWePlayPreset = false
    ),
    UNIVERSAL_FULL(
        id = "UNIVERSAL_FULL",
        title = "Wide Sketchpad",
        subtitle = "Expanded portrait/landscape drawing area",
        isWePlayPreset = false
    )
}

/**
 * Drawing speed presets for 1-tap selection in WePlay Mode & Floating Overlay.
 */
enum class SpeedPreset(val label: String, val multiplier: Float, val pauseMs: Long) {
    SLOW("Slow", 0.65f, 75L),
    NORMAL("Normal", 1.25f, 42L),
    FAST("Fast", 2.25f, 22L)
}

/**
 * Result of Smart Image Auto-Setup diagnostic analysis.
 */
data class SmartImageProfile(
    val meanBrightness: Float,       // 0..255
    val stdDevContrast: Float,       // 0..128
    val edgeDensity: Float,          // 0.0..1.0
    val backgroundComplexity: Float, // 0.0..1.0
    val isLineArtAlready: Boolean,
    val recommendedInternalMode: AutoDrawMode,
    val recommendedInternalIntensity: Float,
    val recommendedExternalConfig: ExternalPathConfig,
    val summaryLabel: String
)

/**
 * Configuration parameters for the natural path extraction pipeline.
 */
data class ExternalPathConfig(
    val isSmartAutoEnabled: Boolean = true,
    val detailLevel: Float = 0.68f,      // 0.1f..1.0f
    val smoothness: Float = 0.72f,       // 0.0f..1.0f
    val lineThickness: Float = 3.0f,     // 1.0f..10.0f
    val noiseReduction: Float = 0.55f,   // 0.0f..1.0f
    val simplification: Float = 0.42f,   // 0.0f..1.0f
    val drawingSpeed: Float = 1.25f,     // 0.25f..3.0f
    val strokePauseMs: Long = 42L,
    val naturalVelocityVariation: Boolean = true,
    val preserveAspectRatio: Boolean = true
)

/**
 * Normalized stroke path where each point is in [0.0f .. 1.0f] relative to the source image aspect,
 * with an adaptive stroke weight factor (`weightFactor` 0.6f..1.5f) based on contour prominence.
 */
data class NormalizedPath(
    val points: List<Offset>,
    val lengthNormalized: Float,
    val weightFactor: Float = 1.0f,
    val isPrimaryFeature: Boolean = false
)

/**
 * Visual Calibration Area supporting beginner-friendly Move, Resize/Zoom, Rotate,
 * and optional Advanced 4-corner adjustment without exposing technical X/Y numbers.
 */
data class CalibrationQuad(
    val topLeft: Offset,
    val topRight: Offset,
    val bottomLeft: Offset,
    val bottomRight: Offset,
    val fineTuneOffsetX: Float = 0f,
    val fineTuneOffsetY: Float = 0f,
    val fineTuneScale: Float = 1.0f,
    val fineTuneRotationDeg: Float = 0f
) {
    fun boundingRect(): RectF {
        val tQuad = withAppliedFineTune()
        val minX = min(min(tQuad.topLeft.x, tQuad.topRight.x), min(tQuad.bottomLeft.x, tQuad.bottomRight.x))
        val maxX = max(max(tQuad.topLeft.x, tQuad.topRight.x), max(tQuad.bottomLeft.x, tQuad.bottomRight.x))
        val minY = min(min(tQuad.topLeft.y, tQuad.topRight.y), min(tQuad.bottomLeft.y, tQuad.bottomRight.y))
        val maxY = max(max(tQuad.topLeft.y, tQuad.topRight.y), max(tQuad.bottomLeft.y, tQuad.bottomRight.y))
        return RectF(minX, minY, max(minX + 40f, maxX), max(minY + 40f, maxY))
    }

    fun centerPoint(): Offset {
        val cx = (topLeft.x + topRight.x + bottomLeft.x + bottomRight.x) * 0.25f + fineTuneOffsetX
        val cy = (topLeft.y + topRight.y + bottomLeft.y + bottomRight.y) * 0.25f + fineTuneOffsetY
        return Offset(cx, cy)
    }

    private fun transformCorner(pt: Offset, rawCenter: Offset): Offset {
        val dx = (pt.x - rawCenter.x) * fineTuneScale
        val dy = (pt.y - rawCenter.y) * fineTuneScale
        val rad = Math.toRadians(fineTuneRotationDeg.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val rx = dx * cosA - dy * sinA
        val ry = dx * sinA + dy * cosA
        return Offset(
            x = rawCenter.x + fineTuneOffsetX + rx,
            y = rawCenter.y + fineTuneOffsetY + ry
        )
    }

    fun withAppliedFineTune(): CalibrationQuad {
        if (fineTuneOffsetX == 0f && fineTuneOffsetY == 0f && fineTuneScale == 1f && fineTuneRotationDeg == 0f) {
            return this
        }
        val rawCenter = Offset(
            x = (topLeft.x + topRight.x + bottomLeft.x + bottomRight.x) * 0.25f,
            y = (topLeft.y + topRight.y + bottomLeft.y + bottomRight.y) * 0.25f
        )
        return CalibrationQuad(
            topLeft = transformCorner(topLeft, rawCenter),
            topRight = transformCorner(topRight, rawCenter),
            bottomLeft = transformCorner(bottomLeft, rawCenter),
            bottomRight = transformCorner(bottomRight, rawCenter)
        )
    }

    fun mapNormalizedPoint(u: Float, v: Float): Offset {
        val effective = withAppliedFineTune()
        val uc = u.coerceIn(0f, 1f)
        val vc = v.coerceIn(0f, 1f)
        val topX = effective.topLeft.x + (effective.topRight.x - effective.topLeft.x) * uc
        val topY = effective.topLeft.y + (effective.topRight.y - effective.topLeft.y) * uc
        val botX = effective.bottomLeft.x + (effective.bottomRight.x - effective.bottomLeft.x) * uc
        val botY = effective.bottomLeft.y + (effective.bottomRight.y - effective.bottomLeft.y) * uc
        return Offset(
            x = topX + (botX - topX) * vc,
            y = topY + (botY - topY) * vc
        )
    }

    companion object {
        fun defaultForScreen(screenWidth: Int, screenHeight: Int): CalibrationQuad {
            return fromPreset(TargetCanvasPreset.UNIVERSAL_AUTO, screenWidth, screenHeight, 1f)
        }

        /**
         * Smart Auto Calibration generator that calculates optimal canvas boundaries,
         * scale, and aspect ratio for WePlay or Universal drawing apps without X/Y numbers.
         */
        fun fromPreset(
            preset: TargetCanvasPreset,
            screenWidth: Int,
            screenHeight: Int,
            sourceAspect: Float = 1f
        ): CalibrationQuad {
            val w = screenWidth.toFloat().coerceAtLeast(360f)
            val h = screenHeight.toFloat().coerceAtLeast(640f)
            val isLandscape = w > h

            val (left, top, right, bottom) = when (preset) {
                TargetCanvasPreset.WEPLAY_PARTY -> {
                    // WePlay's drawing board sits in the upper-middle area above chat & color tools
                    val boardW = if (isLandscape) h * 0.68f else w * 0.86f
                    val boardH = boardW * 0.82f
                    val cx = w * 0.5f
                    val cy = if (isLandscape) h * 0.46f else h * 0.36f
                    listOf(cx - boardW / 2f, cy - boardH / 2f, cx + boardW / 2f, cy + boardH / 2f)
                }

                TargetCanvasPreset.WEPLAY_SQUARE -> {
                    val side = min(w, h) * 0.80f
                    val cx = w * 0.5f
                    val cy = if (isLandscape) h * 0.48f else h * 0.38f
                    listOf(cx - side / 2f, cy - side / 2f, cx + side / 2f, cy + side / 2f)
                }

                TargetCanvasPreset.UNIVERSAL_AUTO -> {
                    val maxW = w * 0.82f
                    val maxH = h * 0.54f
                    val safeAspect = sourceAspect.coerceIn(0.45f, 2.2f)
                    val boxW: Float
                    val boxH: Float
                    if (maxW / maxH > safeAspect) {
                        boxH = maxH
                        boxW = boxH * safeAspect
                    } else {
                        boxW = maxW
                        boxH = boxW / safeAspect
                    }
                    val cx = w * 0.5f
                    val cy = h * 0.46f
                    listOf(cx - boxW / 2f, cy - boxH / 2f, cx + boxW / 2f, cy + boxH / 2f)
                }

                TargetCanvasPreset.UNIVERSAL_FULL -> {
                    listOf(w * 0.08f, h * 0.16f, w * 0.92f, h * 0.78f)
                }
            }

            return CalibrationQuad(
                topLeft = Offset(left, top),
                topRight = Offset(right, top),
                bottomLeft = Offset(left, bottom),
                bottomRight = Offset(right, bottom)
            )
        }
    }
}

data class PathExtractionResult(
    val paths: List<NormalizedPath>,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val totalPoints: Int,
    val smartProfile: SmartImageProfile? = null
)

object KagamiPathPipeline {

    /**
     * Automatically analyzes a selected image's brightness, contrast, resolution, edge density,
     * and background complexity to choose the optimal internal & external Auto-Draw settings.
     */
    suspend fun analyzeImageSmartProfile(source: Bitmap): SmartImageProfile = withContext(Dispatchers.Default) {
        val sampleDim = 160
        val scale = min(1f, sampleDim.toFloat() / max(source.width, source.height).coerceAtLeast(1))
        val sw = max(24, (source.width * scale).roundToInt())
        val sh = max(24, (source.height * scale).roundToInt())
        val thumb = Bitmap.createScaledBitmap(source, sw, sh, true)

        val pixels = IntArray(sw * sh)
        thumb.getPixels(pixels, 0, sw, 0, 0, sw, sh)

        val luma = IntArray(sw * sh)
        var sumLuma = 0L
        var nearWhiteCount = 0
        var nearBlackCount = 0

        for (i in pixels.indices) {
            val px = pixels[i]
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            val v = (0.299f * r + 0.587f * g + 0.114f * b).roundToInt().coerceIn(0, 255)
            luma[i] = v
            sumLuma += v
            if (v > 230) nearWhiteCount++
            if (v < 35) nearBlackCount++
        }

        val totalPx = (sw * sh).coerceAtLeast(1)
        val meanBrightness = sumLuma.toFloat() / totalPx

        var varianceSum = 0.0
        for (v in luma) {
            val diff = v - meanBrightness
            varianceSum += diff * diff
        }
        val stdDevContrast = sqrt(varianceSum / totalPx).toFloat()

        // Measure edge density & outer-border background complexity
        var strongEdges = 0
        var borderEdges = 0
        var borderTotal = 0
        val borderMarginX = (sw * 0.15f).roundToInt().coerceAtLeast(2)
        val borderMarginY = (sh * 0.15f).roundToInt().coerceAtLeast(2)

        for (y in 1 until sh - 1) {
            val row = y * sw
            val isBorderY = y < borderMarginY || y > (sh - borderMarginY)
            for (x in 1 until sw - 1) {
                val idx = row + x
                val gx = abs(luma[idx + 1] - luma[idx - 1])
                val gy = abs(luma[idx + sw] - luma[idx - sw])
                val gMag = gx + gy
                if (gMag > 48) {
                    strongEdges++
                }
                if (isBorderY || x < borderMarginX || x > (sw - borderMarginX)) {
                    borderTotal++
                    if (gMag > 42) borderEdges++
                }
            }
        }

        val edgeDensity = (strongEdges.toFloat() / totalPx).coerceIn(0f, 1f)
        val backgroundComplexity = if (borderTotal > 0) {
            (borderEdges.toFloat() / borderTotal).coerceIn(0f, 1f)
        } else {
            0.2f
        }

        val extremeRatio = (nearWhiteCount + nearBlackCount).toFloat() / totalPx
        val isLineArtAlready = extremeRatio > 0.72f && stdDevContrast > 48f

        // Automatically choose optimal parameters based on image diagnostics
        val autoDetail = when {
            isLineArtAlready -> 0.76f
            backgroundComplexity > 0.35f -> 0.58f // Suppress busy background clutter
            edgeDensity < 0.08f -> 0.80f          // Boost subtle portrait/soft art features
            else -> 0.68f
        }

        val autoNoiseReduction = when {
            isLineArtAlready -> 0.25f
            backgroundComplexity > 0.32f || edgeDensity > 0.24f -> 0.68f
            else -> 0.48f
        }

        val autoSmoothness = if (isLineArtAlready) 0.62f else 0.75f
        val autoSimplification = if (edgeDensity > 0.22f) 0.50f else 0.38f
        val autoThickness = if (isLineArtAlready) 2.6f else 3.2f

        val internalMode = when {
            isLineArtAlready -> AutoDrawMode.THRESHOLD
            stdDevContrast < 38f -> AutoDrawMode.SKETCH
            else -> AutoDrawMode.EDGE_OUTLINE
        }
        val internalIntensity = when {
            stdDevContrast < 38f -> 0.74f
            isLineArtAlready -> 0.56f
            else -> 0.64f
        }

        val profileTag = when {
            isLineArtAlready -> "Smart Auto: Crisp Line-Art Detected"
            backgroundComplexity > 0.32f -> "Smart Auto: Complex BG Filtered • Focus Locked"
            stdDevContrast < 40f -> "Smart Auto: Soft Tones Boosted"
            else -> "Smart Auto: Balanced Anime Contour Profile"
        }

        SmartImageProfile(
            meanBrightness = meanBrightness,
            stdDevContrast = stdDevContrast,
            edgeDensity = edgeDensity,
            backgroundComplexity = backgroundComplexity,
            isLineArtAlready = isLineArtAlready,
            recommendedInternalMode = internalMode,
            recommendedInternalIntensity = internalIntensity,
            recommendedExternalConfig = ExternalPathConfig(
                isSmartAutoEnabled = true,
                detailLevel = autoDetail,
                smoothness = autoSmoothness,
                lineThickness = autoThickness,
                noiseReduction = autoNoiseReduction,
                simplification = autoSimplification,
                drawingSpeed = 1.25f,
                strokePauseMs = 42L,
                naturalVelocityVariation = true,
                preserveAspectRatio = true
            ),
            summaryLabel = profileTag
        )
    }

    /**
     * High-precision 11-stage natural drawing path pipeline with:
     * - Facial/central feature preservation weighting (higher sensitivity in the portrait focal zone)
     * - Hysteresis double-thresholding (connects weak facial/contour ridges to strong anchor edges)
     * - Adaptive line thickness per contour
     * - Meaningless micro-detail pruning & collinear contour stitching
     */
    suspend fun generateNaturalPaths(
        source: Bitmap,
        config: ExternalPathConfig
    ): PathExtractionResult = withContext(Dispatchers.Default) {
        val srcW = source.width.coerceAtLeast(1)
        val srcH = source.height.coerceAtLeast(1)

        // Higher working resolution (220..380px) for clean facial & eye contours
        val workDim = (220 + (config.detailLevel.coerceIn(0.1f, 1f) * 160)).roundToInt()
        val scale = min(1f, workDim.toFloat() / max(srcW, srcH))
        val w = max(40, (srcW * scale).roundToInt())
        val h = max(40, (srcH * scale).roundToInt())

        val scaledBmp = if (w == srcW && h == srcH) {
            source
        } else {
            Bitmap.createScaledBitmap(source, w, h, true)
        }

        val pixels = IntArray(w * h)
        scaledBmp.getPixels(pixels, 0, w, 0, 0, w, h)

        // Stage 1: Grayscale Luminance
        val luma = IntArray(w * h)
        var minL = 255
        var maxL = 0
        for (i in pixels.indices) {
            val px = pixels[i]
            val a = (px ushr 24) and 0xFF
            if (a < 32) {
                luma[i] = 255
            } else {
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                val v = (0.299f * r + 0.587f * g + 0.114f * b).roundToInt().coerceIn(0, 255)
                luma[i] = v
                if (v < minL) minL = v
                if (v > maxL) maxL = v
            }
        }

        // Stage 2: Contrast Normalization with gentle S-curve for clear feature separation
        val range = (maxL - minL).coerceAtLeast(1)
        for (i in luma.indices) {
            val norm = ((luma[i] - minL).toFloat() / range).coerceIn(0f, 1f)
            // Subtle contrast S-curve
            val curved = norm * norm * (3f - 2f * norm)
            luma[i] = (curved * 255f).roundToInt().coerceIn(0, 255)
        }

        // Stage 3: Edge-preserving smoothing (3x3 Gaussian passes based on noiseReduction)
        val blurPasses = if (config.noiseReduction > 0.68f) 2 else if (config.noiseReduction > 0.22f) 1 else 0
        var smoothed = luma
        repeat(blurPasses) {
            smoothed = gaussian3x3(smoothed, w, h)
        }

        // Stage 4: Sobel Gradient Magnitude & Orientation + Central Focal Feature Weighting
        val mag = FloatArray(w * h)
        val dir = ByteArray(w * h)
        var sumMag = 0f
        var edgePixelsCount = 0

        val centerX = w * 0.5f
        val centerY = h * 0.42f // Typical face/subject focal center in portrait & character art
        val maxRad = hypot(centerX.toDouble(), centerY.toDouble()).toFloat().coerceAtLeast(1f)

        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val idx = row + x
                val tl = smoothed[idx - w - 1]
                val tc = smoothed[idx - w]
                val tr = smoothed[idx - w + 1]
                val ml = smoothed[idx - 1]
                val mr = smoothed[idx + 1]
                val bl = smoothed[idx + w - 1]
                val bc = smoothed[idx + w]
                val br = smoothed[idx + w + 1]

                val gx = (-tl + tr - 2 * ml + 2 * mr - bl + br).toFloat()
                val gy = (-tl - 2 * tc - tr + bl + 2 * bc + br).toFloat()
                val rawMag = sqrt(gx * gx + gy * gy) / 4f

                // Boost central subject/facial contours by up to 22%, gently attenuate outer background noise
                val distFromFocus = hypot((x - centerX).toDouble(), (y - centerY).toDouble()).toFloat() / maxRad
                val focusWeight = (1.18f - distFromFocus * 0.28f).coerceIn(0.82f, 1.18f)
                val weightedMag = rawMag * focusWeight

                mag[idx] = weightedMag
                if (weightedMag > 7f) {
                    sumMag += weightedMag
                    edgePixelsCount++
                }

                val absGx = abs(gx)
                val absGy = abs(gy)
                dir[idx] = when {
                    absGy <= absGx * 0.4142f -> 0
                    absGy >= absGx * 2.4142f -> 2
                    gx * gy > 0 -> 1
                    else -> 3
                }
            }
        }

        // Stage 5: Non-Maximum Suppression + Canny Hysteresis Double Thresholding
        val meanMag = if (edgePixelsCount > 0) sumMag / edgePixelsCount else 26f
        val detailFactor = 1.40f - (config.detailLevel.coerceIn(0.1f, 1f) * 0.92f)
        val highThreshold = (meanMag * detailFactor).coerceIn(12f, 88f)
        val lowThreshold = (highThreshold * 0.48f).coerceAtLeast(6f)

        val strongEdge = BooleanArray(w * h)
        val weakEdge = BooleanArray(w * h)

        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val idx = row + x
                val m = mag[idx]
                if (m < lowThreshold) continue

                val n1: Float
                val n2: Float
                when (dir[idx].toInt()) {
                    0 -> {
                        n1 = mag[idx - 1]
                        n2 = mag[idx + 1]
                    }
                    1 -> {
                        n1 = mag[idx - w - 1]
                        n2 = mag[idx + w + 1]
                    }
                    2 -> {
                        n1 = mag[idx - w]
                        n2 = mag[idx + w]
                    }
                    else -> {
                        n1 = mag[idx - w + 1]
                        n2 = mag[idx + w - 1]
                    }
                }
                if (m >= n1 && m >= n2) {
                    if (m >= highThreshold) {
                        strongEdge[idx] = true
                    } else {
                        weakEdge[idx] = true
                    }
                }
            }
        }

        // Hysteresis promotion: promote weak edges connected to strong edges (preserves delicate eyes/mouth lines)
        val edgeMap = strongEdge.copyOf()
        val dx = intArrayOf(1, 1, 0, -1, -1, -1, 0, 1)
        val dy = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
        var promoted = true
        var passes = 0
        while (promoted && passes < 4) {
            promoted = false
            passes++
            for (y in 1 until h - 1) {
                val row = y * w
                for (x in 1 until w - 1) {
                    val idx = row + x
                    if (weakEdge[idx] && !edgeMap[idx]) {
                        for (d in 0 until 8) {
                            if (edgeMap[(y + dy[d]) * w + (x + dx[d])]) {
                                edgeMap[idx] = true
                                promoted = true
                                break
                            }
                        }
                    }
                }
            }
        }

        // Stage 6: Connected Contour Tracing with Momentum Priority
        val visited = BooleanArray(w * h)
        val rawContours = mutableListOf<MutableList<Offset>>()

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val startIdx = y * w + x
                if (edgeMap[startIdx] && !visited[startIdx]) {
                    val chain = mutableListOf<Offset>()
                    var cx = x
                    var cy = y
                    var prevDir = 0
                    visited[startIdx] = true
                    chain.add(Offset(cx.toFloat(), cy.toFloat()))

                    var tracing = true
                    while (tracing && chain.size < 260) {
                        tracing = false
                        for (step in 0 until 8) {
                            val d = (prevDir + step) % 8
                            val nx = cx + dx[d]
                            val ny = cy + dy[d]
                            if (nx in 1 until (w - 1) && ny in 1 until (h - 1)) {
                                val nIdx = ny * w + nx
                                if (edgeMap[nIdx] && !visited[nIdx]) {
                                    visited[nIdx] = true
                                    cx = nx
                                    cy = ny
                                    prevDir = (d + 7) % 8
                                    chain.add(Offset(cx.toFloat(), cy.toFloat()))
                                    tracing = true
                                    break
                                }
                            }
                        }
                    }
                    rawContours.add(chain)
                }
            }
        }

        // Stage 7: Meaningless Micro-Detail Removal (allow shorter chains inside the central facial zone for eyes/nose)
        val baseMinLen = (4 + (config.noiseReduction.coerceIn(0f, 1f) * 7)).roundToInt().coerceAtLeast(3)
        val filteredContours = rawContours.filter { chain ->
            if (chain.size >= baseMinLen) {
                true
            } else if (chain.size >= 3) {
                val mid = chain[chain.size / 2]
                val inFocalZone = abs(mid.x - centerX) < w * 0.26f && abs(mid.y - centerY) < h * 0.26f
                inFocalZone && config.detailLevel >= 0.55f
            } else {
                false
            }
        }.toMutableList()

        // Stage 8: Collinear & Endpoint Contour Joining
        val joinRadius = 4.0f + config.smoothness * 3.5f
        val joinedContours = joinBrokenContours(filteredContours, joinRadius)

        // Stage 9: Duplicate-Line Removal
        val dedupedContours = removeDuplicateOutlines(joinedContours, w, h)

        // Stage 10 & 11: Path Simplification (RDP) + Chaikin Bezier Smoothing + Adaptive Line Thickness
        val rdpEpsilon = 0.40f + config.simplification.coerceIn(0f, 1f) * 1.9f
        val smoothIterations = when {
            config.smoothness >= 0.65f -> 2
            config.smoothness >= 0.25f -> 1
            else -> 0
        }

        val normalizedCandidates = mutableListOf<NormalizedPath>()
        val invW = 1f / w.toFloat()
        val invH = 1f / h.toFloat()

        for (contour in dedupedContours) {
            val simplified = rdpSimplify(contour, rdpEpsilon)
            var smoothedPts = simplified
            repeat(smoothIterations) {
                smoothedPts = chaikinSmooth(smoothedPts)
            }
            if (smoothedPts.size >= 2) {
                var avgGradient = 0f
                for (pt in contour) {
                    val ix = pt.x.roundToInt().coerceIn(0, w - 1)
                    val iy = pt.y.roundToInt().coerceIn(0, h - 1)
                    avgGradient += mag[iy * w + ix]
                }
                avgGradient /= contour.size.coerceAtLeast(1)

                val normPts = smoothedPts.map { pt ->
                    Offset(
                        x = (pt.x * invW).coerceIn(0f, 1f),
                        y = (pt.y * invH).coerceIn(0f, 1f)
                    )
                }
                var pathLen = 0f
                for (i in 1 until normPts.size) {
                    val p0 = normPts[i - 1]
                    val p1 = normPts[i]
                    pathLen += hypot((p1.x - p0.x).toDouble(), (p1.y - p0.y).toDouble()).toFloat()
                }
                if (pathLen >= 0.010f) {
                    val isPrimary = pathLen > 0.14f || avgGradient > highThreshold * 1.45f
                    val weight = (0.72f + (avgGradient / (highThreshold * 2.2f)).coerceIn(0f, 0.68f))
                    normalizedCandidates.add(
                        NormalizedPath(
                            points = normPts,
                            lengthNormalized = pathLen,
                            weightFactor = weight,
                            isPrimaryFeature = isPrimary
                        )
                    )
                }
            }
        }

        val maxAllowedPaths = (85 + (config.detailLevel.coerceIn(0.1f, 1f) * 340)).roundToInt()
        val topPaths = normalizedCandidates
            .sortedByDescending { it.lengthNormalized * (if (it.isPrimaryFeature) 1.35f else 1.0f) }
            .take(maxAllowedPaths)

        val orderedPaths = orderStrokesNaturally(topPaths)
        val totalPts = orderedPaths.sumOf { it.points.size }

        PathExtractionResult(
            paths = orderedPaths,
            sourceWidth = srcW,
            sourceHeight = srcH,
            totalPoints = totalPts
        )
    }

    private fun gaussian3x3(input: IntArray, w: Int, h: Int): IntArray {
        val out = input.copyOf()
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val idx = row + x
                val sum =
                    input[idx - w - 1] + (input[idx - w] shl 1) + input[idx - w + 1] +
                    (input[idx - 1] shl 1) + (input[idx] shl 2) + (input[idx + 1] shl 1) +
                    input[idx + w - 1] + (input[idx + w] shl 1) + input[idx + w + 1]
                out[idx] = sum shr 4
            }
        }
        return out
    }

    private fun joinBrokenContours(
        contours: List<MutableList<Offset>>,
        maxGap: Float
    ): List<List<Offset>> {
        if (contours.size <= 1) return contours
        val pool = contours.map { it.toMutableList() }.toMutableList()
        val used = BooleanArray(pool.size)
        val result = mutableListOf<List<Offset>>()
        val maxGapSq = maxGap * maxGap

        for (i in pool.indices) {
            if (used[i]) continue
            used[i] = true
            val current = pool[i]

            var mergedAny = true
            while (mergedAny && current.size < 300) {
                mergedAny = false
                val tail = current.last()
                var bestIdx = -1
                var reverseCandidate = false
                var bestDistSq = maxGapSq

                for (j in pool.indices) {
                    if (used[j]) continue
                    val cand = pool[j]
                    val headDistSq = distSq(tail, cand.first())
                    if (headDistSq < bestDistSq) {
                        bestDistSq = headDistSq
                        bestIdx = j
                        reverseCandidate = false
                    }
                    val tailDistSq = distSq(tail, cand.last())
                    if (tailDistSq < bestDistSq) {
                        bestDistSq = tailDistSq
                        bestIdx = j
                        reverseCandidate = true
                    }
                }

                if (bestIdx != -1) {
                    used[bestIdx] = true
                    val nextPts = if (reverseCandidate) pool[bestIdx].asReversed() else pool[bestIdx]
                    current.addAll(nextPts.drop(1))
                    mergedAny = true
                }
            }
            result.add(current)
        }
        return result
    }

    private fun removeDuplicateOutlines(
        contours: List<List<Offset>>,
        w: Int,
        h: Int
    ): List<List<Offset>> {
        val occupancy = BooleanArray(w * h)
        val kept = mutableListOf<List<Offset>>()
        val sorted = contours.sortedByDescending { it.size }

        for (chain in sorted) {
            var overlapCount = 0
            for (pt in chain) {
                val ix = pt.x.roundToInt().coerceIn(0, w - 1)
                val iy = pt.y.roundToInt().coerceIn(0, h - 1)
                if (occupancy[iy * w + ix]) {
                    overlapCount++
                }
            }
            val overlapRatio = overlapCount.toFloat() / chain.size.coerceAtLeast(1)
            if (overlapRatio < 0.58f) {
                kept.add(chain)
                for (pt in chain) {
                    val cx = pt.x.roundToInt()
                    val cy = pt.y.roundToInt()
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val nx = cx + dx
                            val ny = cy + dy
                            if (nx in 0 until w && ny in 0 until h) {
                                occupancy[ny * w + nx] = true
                            }
                        }
                    }
                }
            }
        }
        return kept
    }

    private fun rdpSimplify(points: List<Offset>, epsilon: Float): List<Offset> {
        if (points.size < 3) return points
        var dmax = 0f
        var index = 0
        val end = points.lastIndex
        val startPt = points[0]
        val endPt = points[end]

        for (i in 1 until end) {
            val d = perpendicularDistance(points[i], startPt, endPt)
            if (d > dmax) {
                index = i
                dmax = d
            }
        }

        return if (dmax > epsilon) {
            val left = rdpSimplify(points.subList(0, index + 1), epsilon)
            val right = rdpSimplify(points.subList(index, end + 1), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(startPt, endPt)
        }
    }

    private fun perpendicularDistance(pt: Offset, lineStart: Offset, lineEnd: Offset): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val mag = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (mag < 0.0001f) return hypot((pt.x - lineStart.x).toDouble(), (pt.y - lineStart.y).toDouble()).toFloat()
        val num = abs(dy * pt.x - dx * pt.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x)
        return num / mag
    }

    private fun chaikinSmooth(points: List<Offset>): List<Offset> {
        if (points.size <= 2) return points
        val smoothed = ArrayList<Offset>(points.size * 2)
        smoothed.add(points.first())
        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val q = Offset(0.75f * p0.x + 0.25f * p1.x, 0.75f * p0.y + 0.25f * p1.y)
            val r = Offset(0.25f * p0.x + 0.75f * p1.x, 0.25f * p0.y + 0.75f * p1.y)
            smoothed.add(q)
            smoothed.add(r)
        }
        smoothed.add(points.last())
        return smoothed
    }

    private fun orderStrokesNaturally(paths: List<NormalizedPath>): List<NormalizedPath> {
        if (paths.size <= 1) return paths
        // Group into primary structural outlines first, then fine internal details
        val primaryPool = paths.filter { it.isPrimaryFeature }.toMutableList()
        val detailPool = paths.filter { !it.isPrimaryFeature }.toMutableList()

        val ordered = ArrayList<NormalizedPath>(paths.size)

        fun drainPoolNaturally(pool: MutableList<NormalizedPath>, initialAnchor: Offset?): Offset? {
            if (pool.isEmpty()) return initialAnchor
            var current = if (initialAnchor == null) {
                pool.removeAt(0)
            } else {
                var firstIdx = 0
                var minD = Float.MAX_VALUE
                for (i in pool.indices) {
                    val d = distSq(initialAnchor, pool[i].points.first())
                    if (d < minD) {
                        minD = d
                        firstIdx = i
                    }
                }
                pool.removeAt(firstIdx)
            }
            ordered.add(current)

            while (pool.isNotEmpty()) {
                val lastEnd = current.points.last()
                var bestIdx = 0
                var shouldReverse = false
                var bestScore = Float.MAX_VALUE

                for (i in pool.indices) {
                    val cand = pool[i]
                    val dStart = distSq(lastEnd, cand.points.first())
                    val dEnd = distSq(lastEnd, cand.points.last())
                    if (dStart < bestScore) {
                        bestScore = dStart
                        bestIdx = i
                        shouldReverse = false
                    }
                    if (dEnd < bestScore) {
                        bestScore = dEnd
                        bestIdx = i
                        shouldReverse = true
                    }
                }

                val next = pool.removeAt(bestIdx)
                current = if (shouldReverse) {
                    next.copy(points = next.points.asReversed())
                } else {
                    next
                }
                ordered.add(current)
            }
            return current.points.lastOrNull()
        }

        val lastPrimaryPt = drainPoolNaturally(primaryPool, null)
        drainPoolNaturally(detailPool, lastPrimaryPt)
        return ordered
    }

    private fun distSq(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy
    }

    fun mapNormalizedToScreen(
        pt: Offset,
        sourceWidth: Int,
        sourceHeight: Int,
        quad: CalibrationQuad,
        preserveAspectRatio: Boolean
    ): Offset {
        if (!preserveAspectRatio || sourceWidth <= 0 || sourceHeight <= 0) {
            return quad.mapNormalizedPoint(pt.x, pt.y)
        }

        val bounds = quad.boundingRect()
        val quadW = bounds.width().coerceAtLeast(1f)
        val quadH = bounds.height().coerceAtLeast(1f)
        val srcAspect = sourceWidth.toFloat() / sourceHeight.toFloat()
        val quadAspect = quadW / quadH

        val u: Float
        val v: Float
        if (srcAspect > quadAspect) {
            val activeH = quadAspect / srcAspect
            val marginV = (1f - activeH) / 2f
            u = pt.x
            v = marginV + pt.y * activeH
        } else {
            val activeW = srcAspect / quadAspect
            val marginU = (1f - activeW) / 2f
            u = marginU + pt.x * activeW
            v = pt.y
        }
        return quad.mapNormalizedPoint(u, v)
    }
}
