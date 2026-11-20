package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import com.example.data.model.ImageFilterType
import com.example.data.model.NormPoint
import com.example.data.model.NormRect
import com.example.data.model.PageEditParams
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

object ImageFilterProcessor {

    fun decodeOrientedBitmap(filePath: String, maxDimension: Int): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = BitmapFactory.decodeFile(filePath, decodeOpts) ?: return null

        val exifRotation = readExifRotation(filePath)
        val oriented = if (exifRotation != 0) {
            val matrix = Matrix().apply { postRotate(exifRotation.toFloat()) }
            val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
            if (rotated !== raw) raw.recycle()
            rotated
        } else {
            raw
        }

        // Scale down precisely if still larger than maxDimension
        val maxEdge = max(oriented.width, oriented.height)
        return if (maxEdge > maxDimension && maxDimension > 0) {
            val scale = maxDimension.toFloat() / maxEdge
            val targetW = max(1, (oriented.width * scale).roundToInt())
            val targetH = max(1, (oriented.height * scale).roundToInt())
            val scaled = Bitmap.createScaledBitmap(oriented, targetW, targetH, true)
            if (scaled !== oriented) oriented.recycle()
            scaled
        } else {
            oriented
        }
    }

    fun processPageBitmap(
        originalPath: String,
        params: PageEditParams,
        maxDimension: Int
    ): Bitmap? {
        var current = decodeOrientedBitmap(originalPath, maxDimension) ?: return null

        // 1. Perspective warp OR rectangular crop
        if (params.usePerspectiveWarp && params.corners.size == 4) {
            val warped = warpPerspective(current, params.corners)
            if (warped !== current) {
                current.recycle()
                current = warped
            }
        } else if (!params.cropRect.isFull) {
            val cropped = applyRectCrop(current, params.cropRect)
            if (cropped !== current) {
                current.recycle()
                current = cropped
            }
        }

        // 2. Rotation
        val normalizedRot = ((params.rotationDegrees % 360) + 360) % 360
        if (normalizedRot != 0) {
            val rotMatrix = Matrix().apply { postRotate(normalizedRot.toFloat()) }
            val rotated = Bitmap.createBitmap(current, 0, 0, current.width, current.height, rotMatrix, true)
            if (rotated !== current) {
                current.recycle()
                current = rotated
            }
        }

        // 3. Filters, brightness & contrast
        val needsColorPass = params.filterType != ImageFilterType.ORIGINAL ||
            params.brightness != 0f ||
            params.contrast != 1f

        if (needsColorPass) {
            val filtered = applyFilterAndAdjustments(
                source = current,
                filterType = params.filterType,
                brightness = params.brightness,
                contrast = params.contrast
            )
            if (filtered !== current) {
                current.recycle()
                current = filtered
            }
        }

        return current
    }

    fun renderThumbnailForFilter(
        sourceBitmap: Bitmap,
        filterType: ImageFilterType
    ): Bitmap {
        return applyFilterAndAdjustments(
            source = sourceBitmap,
            filterType = filterType,
            brightness = 0f,
            contrast = 1f
        )
    }

    fun saveWorkspacePreview(
        context: Context,
        pageId: String,
        originalPath: String,
        params: PageEditParams,
        maxDimension: Int = 1080
    ): String? {
        val processed = processPageBitmap(originalPath, params, maxDimension) ?: return null
        return try {
            val dir = File(context.filesDir, "workspace").apply { mkdirs() }
            val outFile = File(dir, "preview_${pageId}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { out ->
                processed.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            outFile.absolutePath
        } catch (_: Exception) {
            null
        } finally {
            processed.recycle()
        }
    }

    private fun warpPerspective(source: Bitmap, corners: List<NormPoint>): Bitmap {
        val w = source.width.toFloat()
        val h = source.height.toFloat()
        val tl = corners[0]
        val tr = corners[1]
        val br = corners[2]
        val bl = corners[3]

        val src = floatArrayOf(
            tl.x * w, tl.y * h,
            tr.x * w, tr.y * h,
            br.x * w, br.y * h,
            bl.x * w, bl.y * h
        )

        val widthTop = hypot((tr.x - tl.x) * w, (tr.y - tl.y) * h)
        val widthBot = hypot((br.x - bl.x) * w, (br.y - bl.y) * h)
        val heightLeft = hypot((bl.x - tl.x) * w, (bl.y - tl.y) * h)
        val heightRight = hypot((br.x - tr.x) * w, (br.y - tr.y) * h)

        val outW = max(64, max(widthTop, widthBot).roundToInt())
        val outH = max(64, max(heightLeft, heightRight).roundToInt())

        val dst = floatArrayOf(
            0f, 0f,
            outW.toFloat(), 0f,
            outW.toFloat(), outH.toFloat(),
            0f, outH.toFloat()
        )

        val matrix = Matrix()
        val ok = matrix.setPolyToPoly(src, 0, dst, 0, 4)
        if (!ok) return source

        val output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    private fun applyRectCrop(source: Bitmap, rect: NormRect): Bitmap {
        val l = (rect.left.coerceIn(0f, 0.95f) * source.width).roundToInt()
        val t = (rect.top.coerceIn(0f, 0.95f) * source.height).roundToInt()
        val r = (rect.right.coerceIn(rect.left + 0.05f, 1f) * source.width).roundToInt()
        val b = (rect.bottom.coerceIn(rect.top + 0.05f, 1f) * source.height).roundToInt()
        val cw = max(16, r - l)
        val ch = max(16, b - t)
        return Bitmap.createBitmap(source, l, t, cw.coerceAtMost(source.width - l), ch.coerceAtMost(source.height - t))
    }

    private fun applyFilterAndAdjustments(
        source: Bitmap,
        filterType: ImageFilterType,
        brightness: Float,
        contrast: Float
    ): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val cm = ColorMatrix()

        when (filterType) {
            ImageFilterType.ORIGINAL -> Unit
            ImageFilterType.GRAYSCALE -> cm.setSaturation(0f)
            ImageFilterType.BLACK_AND_WHITE -> {
                cm.setSaturation(0f)
                val bw = contrastTranslateMatrix(contrast = 1.95f, brightness = -32f)
                cm.postConcat(bw)
            }
            ImageFilterType.CRISP_INK -> {
                cm.setSaturation(0f)
                val ink = contrastTranslateMatrix(contrast = 2.4f, brightness = -48f)
                cm.postConcat(ink)
            }
            ImageFilterType.DOCUMENT_SCAN -> {
                val sat = ColorMatrix().apply { setSaturation(1.15f) }
                val doc = contrastTranslateMatrix(contrast = 1.48f, brightness = 18f)
                cm.postConcat(sat)
                cm.postConcat(doc)
            }
            ImageFilterType.CLEAN_SCAN -> {
                val sat = ColorMatrix().apply { setSaturation(0.35f) }
                val clean = contrastTranslateMatrix(contrast = 1.65f, brightness = 24f)
                cm.postConcat(sat)
                cm.postConcat(clean)
            }
            ImageFilterType.HIGH_CONTRAST -> {
                cm.postConcat(contrastTranslateMatrix(contrast = 1.65f, brightness = -10f))
            }
            ImageFilterType.COLOR_PHOTO -> {
                cm.setSaturation(1.42f)
                cm.postConcat(contrastTranslateMatrix(contrast = 1.15f, brightness = 6f))
            }
            ImageFilterType.BRIGHTNESS_BOOST -> {
                cm.postConcat(contrastTranslateMatrix(contrast = 1.06f, brightness = 36f))
            }
            ImageFilterType.VINTAGE -> {
                val sepia = ColorMatrix(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(sepia)
            }
        }

        if (brightness != 0f || contrast != 1f) {
            cm.postConcat(contrastTranslateMatrix(contrast = contrast, brightness = brightness))
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }

    private fun contrastTranslateMatrix(contrast: Float, brightness: Float): ColorMatrix {
        val translate = (-0.5f * contrast + 0.5f) * 255f + brightness
        return ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    private fun readExifRotation(filePath: String): Int {
        return try {
            val exif = ExifInterface(filePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Exception) {
            0
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        if (maxDim <= 0) return 1
        while (width / (inSampleSize * 2) >= maxDim && height / (inSampleSize * 2) >= maxDim) {
            inSampleSize *= 2
        }
        return inSampleSize
    }
}
