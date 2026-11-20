package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.data.model.ImagePlacement
import com.example.data.model.PdfBuildConfig
import com.example.data.model.WorkspacePage
import com.example.util.FileActionsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class GeneratedPdfInfo(
    val pdfFile: File,
    val thumbnailFile: File?,
    val sizeBytes: Long,
    val pageCount: Int
)

object PdfGenerator {

    fun estimateSizeBytes(pageCount: Int, config: PdfBuildConfig): Long {
        if (pageCount <= 0) return 0L
        val basePerPage = when (config.quality) {
            com.example.data.model.ImageQuality.HIGH -> 380_000L
            com.example.data.model.ImageQuality.MEDIUM -> 210_000L
            com.example.data.model.ImageQuality.LOW -> 115_000L
        }
        val compressMultiplier = if (config.compressOutput) 0.62f else 1.0f
        return (pageCount * basePerPage * compressMultiplier).toLong() + 12_000L
    }

    suspend fun createPdfFromPages(
        context: Context,
        pages: List<WorkspacePage>,
        config: PdfBuildConfig,
        outputPdfFile: File,
        outputThumbFile: File,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<GeneratedPdfInfo> = withContext(Dispatchers.IO) {
        runCatching {
            if (pages.isEmpty()) error("পিডিএফ তৈরির জন্য কমপক্ষে একটি ছবি নির্বাচন করুন।")

            val estBytes = estimateSizeBytes(pages.size, config)
            if (FileActionsHelper.availableInternalBytes(context) < estBytes + 5_000_000L) {
                error("মোবাইলে পর্যাপ্ত ফাঁকা জায়গা নেই। অনুগ্রহ করে কিছু জায়গা খালি করে আবার চেষ্টা করুন।")
            }

            outputPdfFile.parentFile?.mkdirs()
            outputThumbFile.parentFile?.mkdirs()

            val tempPdf = File(outputPdfFile.parentFile, "${outputPdfFile.nameWithoutExtension}.tmp")
            val (pageW, pageH) = config.resolvePageDimensionsPt()
            val marginPt = config.margin.marginPt.coerceAtMost(min(pageW, pageH) / 4)

            val effectiveJpegQuality = if (config.compressOutput) {
                (config.quality.jpegQuality - 18).coerceAtLeast(35)
            } else {
                config.quality.jpegQuality
            }
            val maxDim = if (config.compressOutput) {
                (config.quality.targetMaxDimension * 0.78f).toInt().coerceAtLeast(900)
            } else {
                config.quality.targetMaxDimension
            }

            val pdfDoc = PdfDocument()
            try {
                pages.forEachIndexed { index, page ->
                    coroutineContext.ensureActive()
                    onProgress(index + 1, pages.size)

                    val processed = ImageFilterProcessor.processPageBitmap(
                        originalPath = page.originalPath,
                        params = page.editParams,
                        maxDimension = maxDim
                    ) ?: error("${index + 1} নম্বর ছবিটি পড়া যায়নি।")

                    // Compress bitmap through JPEG pass when compression or lower quality is requested
                    val drawBitmap = reencodeBitmapForPdf(processed, effectiveJpegQuality)
                    if (drawBitmap !== processed) processed.recycle()

                    val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                    val pdfPage = pdfDoc.startPage(pageInfo)
                    val canvas = pdfPage.canvas
                    canvas.drawColor(Color.WHITE)

                    drawBitmapIntoPage(
                        canvas = canvas,
                        bitmap = drawBitmap,
                        pageWidth = pageW,
                        pageHeight = pageH,
                        margin = marginPt,
                        placement = config.placement
                    )

                    pdfDoc.finishPage(pdfPage)
                    drawBitmap.recycle()
                }

                FileOutputStream(tempPdf).use { out ->
                    pdfDoc.writeTo(out)
                }
            } finally {
                pdfDoc.close()
            }

            if (outputPdfFile.exists()) outputPdfFile.delete()
            if (!tempPdf.renameTo(outputPdfFile)) {
                tempPdf.copyTo(outputPdfFile, overwrite = true)
                tempPdf.delete()
            }

            val thumbCreated = generatePdfThumbnail(outputPdfFile, outputThumbFile)
            GeneratedPdfInfo(
                pdfFile = outputPdfFile,
                thumbnailFile = if (thumbCreated) outputThumbFile else null,
                sizeBytes = outputPdfFile.length(),
                pageCount = pages.size
            )
        }
    }

    private fun reencodeBitmapForPdf(bitmap: Bitmap, jpegQuality: Int): Bitmap {
        if (jpegQuality >= 95) return bitmap
        return try {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
            val bytes = baos.toByteArray()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: bitmap
        } catch (_: OutOfMemoryError) {
            bitmap
        }
    }

    private fun drawBitmapIntoPage(
        canvas: Canvas,
        bitmap: Bitmap,
        pageWidth: Int,
        pageHeight: Int,
        margin: Int,
        placement: ImagePlacement
    ) {
        val availW = max(10, pageWidth - margin * 2)
        val availH = max(10, pageHeight - margin * 2)
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (placement) {
            ImagePlacement.FIT_ENTIRE -> {
                val scale = min(availW / bw, availH / bh)
                val dw = (bw * scale).roundToInt()
                val dh = (bh * scale).roundToInt()
                val left = margin + (availW - dw) / 2
                val top = margin + (availH - dh) / 2
                canvas.drawBitmap(
                    bitmap,
                    null,
                    Rect(left, top, left + dw, top + dh),
                    paint
                )
            }
            ImagePlacement.FILL_PAGE -> {
                val scale = max(availW / bw, availH / bh)
                val srcCropW = (availW / scale).roundToInt().coerceIn(1, bitmap.width)
                val srcCropH = (availH / scale).roundToInt().coerceIn(1, bitmap.height)
                val srcLeft = (bitmap.width - srcCropW) / 2
                val srcTop = (bitmap.height - srcCropH) / 2
                val srcRect = Rect(srcLeft, srcTop, srcLeft + srcCropW, srcTop + srcCropH)
                val dstRect = Rect(margin, margin, margin + availW, margin + availH)
                canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
            }
            ImagePlacement.CENTER_MEDIUM -> {
                val scale = min(availW / bw, availH / bh) * 0.82f
                val dw = (bw * scale).roundToInt()
                val dh = (bh * scale).roundToInt()
                val left = margin + (availW - dw) / 2
                val top = margin + (availH - dh) / 2
                canvas.drawBitmap(
                    bitmap,
                    null,
                    Rect(left, top, left + dw, top + dh),
                    paint
                )
            }
        }
    }

    fun generatePdfThumbnail(pdfFile: File, outputThumbFile: File): Boolean {
        return try {
            outputThumbFile.parentFile?.mkdirs()
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount <= 0) return false
                    renderer.openPage(0).use { page ->
                        val targetW = 320
                        val targetH = max(120, (targetW * page.height.toFloat() / max(1, page.width)).roundToInt())
                        val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        FileOutputStream(outputThumbFile).use { out ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
                        }
                        bmp.recycle()
                        true
                    }
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    fun inspectPdfPageCount(pdfFile: File): Int {
        return try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (_: Exception) {
            -1
        }
    }
}
