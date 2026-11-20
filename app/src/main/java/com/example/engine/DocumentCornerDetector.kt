package com.example.engine

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.example.data.model.NormPoint
import com.example.data.model.PageEditParams
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class DocumentDetectionResult(
    val corners: List<NormPoint>,
    val confidence: Float,
    val isDetected: Boolean
)

object DocumentCornerDetector {

    private const val GRID_W = 120
    private const val GRID_H = 160

    fun detectFromImageProxy(image: ImageProxy): DocumentDetectionResult {
        val plane = image.planes.firstOrNull() ?: return fallbackResult()
        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val buffer = plane.buffer
        val rotation = image.imageInfo.rotationDegrees

        val luma = IntArray(GRID_W * GRID_H)
        for (gy in 0 until GRID_H) {
            val ny = gy.toFloat() / (GRID_H - 1)
            for (gx in 0 until GRID_W) {
                val nx = gx.toFloat() / (GRID_W - 1)
                // Map normalized portrait display coords back to sensor coords
                val (sxNorm, syNorm) = when (rotation) {
                    90 -> syToSensor90(nx, ny)
                    270 -> syToSensor270(nx, ny)
                    180 -> (1f - nx) to (1f - ny)
                    else -> nx to ny
                }
                val sx = (sxNorm * (width - 1)).toInt().coerceIn(0, width - 1)
                val sy = (syNorm * (height - 1)).toInt().coerceIn(0, height - 1)
                val idx = sy * rowStride + sx * pixelStride
                luma[gy * GRID_W + gx] = if (idx in 0 until buffer.limit()) {
                    buffer.get(idx).toInt() and 0xFF
                } else {
                    128
                }
            }
        }
        return detectFromLumaGrid(luma, GRID_W, GRID_H)
    }

    private fun syToSensor90(nx: Float, ny: Float): Pair<Float, Float> = ny to (1f - nx)
    private fun syToSensor270(nx: Float, ny: Float): Pair<Float, Float> = (1f - ny) to nx

    fun detectFromBitmap(bitmap: Bitmap): DocumentDetectionResult {
        val scaled = Bitmap.createScaledBitmap(bitmap, GRID_W, GRID_H, true)
        val pixels = IntArray(GRID_W * GRID_H)
        scaled.getPixels(pixels, 0, GRID_W, 0, 0, GRID_W, GRID_H)
        if (scaled !== bitmap) scaled.recycle()

        val luma = IntArray(GRID_W * GRID_H)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            luma[i] = (r * 30 + g * 59 + b * 11) / 100
        }
        return detectFromLumaGrid(luma, GRID_W, GRID_H)
    }

    private fun detectFromLumaGrid(luma: IntArray, w: Int, h: Int): DocumentDetectionResult {
        // 1. 3x3 box blur to reduce sensor noise
        val blurred = IntArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                var sum = 0
                for (dy in -1..1) {
                    val row = (y + dy) * w
                    for (dx in -1..1) {
                        sum += luma[row + x + dx]
                    }
                }
                blurred[y * w + x] = sum / 9
            }
        }

        // 2. Sobel gradient magnitude
        val grad = IntArray(w * h)
        var maxGrad = 1
        var sumGrad = 0L
        for (y in 2 until h - 2) {
            for (x in 2 until w - 2) {
                val p00 = blurred[(y - 1) * w + (x - 1)]
                val p01 = blurred[(y - 1) * w + x]
                val p02 = blurred[(y - 1) * w + (x + 1)]
                val p10 = blurred[y * w + (x - 1)]
                val p12 = blurred[y * w + (x + 1)]
                val p20 = blurred[(y + 1) * w + (x - 1)]
                val p21 = blurred[(y + 1) * w + x]
                val p22 = blurred[(y + 1) * w + (x + 1)]

                val gx = -p00 + p02 - 2 * p10 + 2 * p12 - p20 + p22
                val gy = -p00 - 2 * p01 - p02 + p20 + 2 * p21 + p22
                val mag = abs(gx) + abs(gy)
                grad[y * w + x] = mag
                if (mag > maxGrad) maxGrad = mag
                sumGrad += mag
            }
        }

        val meanGrad = (sumGrad / max(1, (w - 4) * (h - 4))).toInt()
        val edgeThreshold = max(65, (meanGrad * 2.4f).toInt())

        // 3. Find strongest corner candidates in each quadrant relative to center
        val cx = w / 2f
        val cy = h / 2f
        var tlBest: Pair<Float, Float>? = null
        var tlScore = 0f
        var trBest: Pair<Float, Float>? = null
        var trScore = 0f
        var brBest: Pair<Float, Float>? = null
        var brScore = 0f
        var blBest: Pair<Float, Float>? = null
        var blScore = 0f

        var strongEdgeCount = 0
        val marginX = (w * 0.04f).toInt()
        val marginY = (h * 0.04f).toInt()

        for (y in marginY until h - marginY) {
            for (x in marginX until w - marginX) {
                val g = grad[y * w + x]
                if (g < edgeThreshold) continue
                strongEdgeCount++

                // Local 3x3 edge density boosts true paper corners over isolated speckles
                var localSupport = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (grad[(y + dy) * w + (x + dx)] >= edgeThreshold) localSupport++
                    }
                }
                if (localSupport < 3) continue

                val dist = hypot((x - cx).toDouble(), (y - cy).toDouble()).toFloat()
                // Prefer corners that are clearly away from center, but not glued to border
                val score = g * (0.4f + dist / max(w, h)) * localSupport

                when {
                    x < cx * 0.9f && y < cy * 0.9f -> {
                        if (score > tlScore) {
                            tlScore = score
                            tlBest = (x.toFloat() / w) to (y.toFloat() / h)
                        }
                    }
                    x > cx * 1.1f && y < cy * 0.9f -> {
                        if (score > trScore) {
                            trScore = score
                            trBest = (x.toFloat() / w) to (y.toFloat() / h)
                        }
                    }
                    x > cx * 1.1f && y > cy * 1.1f -> {
                        if (score > brScore) {
                            brScore = score
                            brBest = (x.toFloat() / w) to (y.toFloat() / h)
                        }
                    }
                    x < cx * 0.9f && y > cy * 1.1f -> {
                        if (score > blScore) {
                            blScore = score
                            blBest = (x.toFloat() / w) to (y.toFloat() / h)
                        }
                    }
                }
            }
        }

        if (tlBest == null || trBest == null || brBest == null || blBest == null || strongEdgeCount < 35) {
            return fallbackResult()
        }

        val tl = NormPoint(tlBest.first.coerceIn(0.03f, 0.45f), tlBest.second.coerceIn(0.03f, 0.45f))
        val tr = NormPoint(trBest.first.coerceIn(0.55f, 0.97f), trBest.second.coerceIn(0.03f, 0.45f))
        val br = NormPoint(brBest.first.coerceIn(0.55f, 0.97f), brBest.second.coerceIn(0.55f, 0.97f))
        val bl = NormPoint(blBest.first.coerceIn(0.03f, 0.45f), blBest.second.coerceIn(0.55f, 0.97f))

        val area = polygonArea(listOf(tl, tr, br, bl))
        val widthTop = abs(tr.x - tl.x)
        val widthBot = abs(br.x - bl.x)
        val heightLeft = abs(bl.y - tl.y)
        val heightRight = abs(br.y - tr.y)

        val validShape = area in 0.16f..0.92f &&
            min(widthTop, widthBot) > 0.25f &&
            min(heightLeft, heightRight) > 0.25f

        if (!validShape) return fallbackResult()

        val confidence = ((strongEdgeCount / 220f).coerceIn(0.35f, 1f) * 0.5f +
            (area / 0.65f).coerceIn(0.3f, 1f) * 0.5f).coerceIn(0f, 1f)

        return DocumentDetectionResult(
            corners = listOf(tl, tr, br, bl),
            confidence = confidence,
            isDetected = confidence >= 0.45f
        )
    }

    private fun polygonArea(pts: List<NormPoint>): Float {
        if (pts.size < 4) return 0f
        var sum = 0f
        for (i in pts.indices) {
            val j = (i + 1) % pts.size
            sum += pts[i].x * pts[j].y - pts[j].x * pts[i].y
        }
        return abs(sum) * 0.5f
    }

    private fun fallbackResult(): DocumentDetectionResult {
        return DocumentDetectionResult(
            corners = PageEditParams.defaultCorners().let {
                listOf(
                    NormPoint(0.08f, 0.10f),
                    NormPoint(0.92f, 0.10f),
                    NormPoint(0.92f, 0.90f),
                    NormPoint(0.08f, 0.90f)
                )
            },
            confidence = 0f,
            isDetected = false
        )
    }
}
