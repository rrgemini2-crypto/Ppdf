package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.example.util.ImageExportQuality
import com.example.util.ImageOutputFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToInt

data class StitchedLongImageResult(
    val outputFile: File,
    val widthPx: Int,
    val heightPx: Int,
    val sizeBytes: Long,
    val pagesStitched: Int
)

object LongImageStitcher {

    suspend fun stitchPagesToLongImage(
        pageImageFiles: List<File>,
        outputFile: File,
        format: ImageOutputFormat,
        quality: ImageExportQuality,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<StitchedLongImageResult> = withContext(Dispatchers.IO) {
        runCatching {
            val validFiles = pageImageFiles.filter { it.exists() }
            if (validFiles.isEmpty()) error("লম্বা ছবি তৈরির জন্য কমপক্ষে একটি পৃষ্ঠা বাছুন।")

            // Determine desired width per quality while capping total stitched height to avoid OOM/Canvas limits
            val desiredWidth = when (quality) {
                ImageExportQuality.HIGH -> 1320
                ImageExportQuality.MEDIUM -> 1040
                ImageExportQuality.LOW -> 780
            }
            val dividerPx = 4

            val pageSizes = validFiles.map { file ->
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, opts)
                val w = max(1, opts.outWidth)
                val h = max(1, opts.outHeight)
                w to h
            }

            // Compute height at desiredWidth
            val initialTotalH = pageSizes.sumOf { (w, h) ->
                (desiredWidth.toFloat() * h / w).roundToInt()
            } + max(0, validFiles.size - 1) * dividerPx

            val maxAllowedHeight = 16_000
            val targetWidth = if (initialTotalH > maxAllowedHeight) {
                val shrink = maxAllowedHeight.toFloat() / initialTotalH
                max(420, (desiredWidth * shrink).roundToInt())
            } else {
                desiredWidth
            }

            val scaledHeights = pageSizes.map { (w, h) ->
                max(100, (targetWidth.toFloat() * h / w).roundToInt())
            }
            val totalHeight = scaledHeights.sum() + max(0, validFiles.size - 1) * dividerPx

            val stitched = Bitmap.createBitmap(targetWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(stitched)
            canvas.drawColor(Color.WHITE)

            val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val sepPaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                style = Paint.Style.FILL
            }

            var cursorY = 0
            try {
                validFiles.forEachIndexed { idx, file ->
                    coroutineContext.ensureActive()
                    onProgress(idx + 1, validFiles.size)

                    val pageH = scaledHeights[idx]
                    val bmp = decodeSampledBitmap(file, targetWidth, pageH)
                        ?: error("${idx + 1} নম্বর পৃষ্ঠাটি পড়া যায়নি।")

                    canvas.drawBitmap(
                        bmp,
                        null,
                        Rect(0, cursorY, targetWidth, cursorY + pageH),
                        drawPaint
                    )
                    bmp.recycle()
                    cursorY += pageH

                    if (idx < validFiles.lastIndex) {
                        canvas.drawRect(
                            0f,
                            cursorY.toFloat(),
                            targetWidth.toFloat(),
                            (cursorY + dividerPx).toFloat(),
                            sepPaint
                        )
                        cursorY += dividerPx
                    }
                }

                outputFile.parentFile?.mkdirs()
                val tmp = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}.tmp")
                FileOutputStream(tmp).use { out ->
                    val q = if (format == ImageOutputFormat.PNG) 100 else quality.jpegQuality
                    if (!stitched.compress(format.compressFormat, q, out)) {
                        error("লম্বা ছবিটি সংরক্ষণ করতে সমস্যা হয়েছে।")
                    }
                }
                if (outputFile.exists()) outputFile.delete()
                if (!tmp.renameTo(outputFile)) {
                    tmp.copyTo(outputFile, overwrite = true)
                    tmp.delete()
                }

                StitchedLongImageResult(
                    outputFile = outputFile,
                    widthPx = targetWidth,
                    heightPx = totalHeight,
                    sizeBytes = outputFile.length(),
                    pagesStitched = validFiles.size
                )
            } finally {
                stitched.recycle()
            }
        }
    }

    private fun decodeSampledBitmap(file: File, reqW: Int, reqH: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        var inSampleSize = 1
        val halfH = opts.outHeight / 2
        val halfW = opts.outWidth / 2
        while (halfH / inSampleSize >= reqH && halfW / inSampleSize >= reqW) {
            inSampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        return BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
    }
}
