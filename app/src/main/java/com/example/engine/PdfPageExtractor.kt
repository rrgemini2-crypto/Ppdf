package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToInt

data class ExtractedPdfPage(
    val originalPageNumber: Int,
    val imagePath: String,
    val rotationDegrees: Int = 0
)

object PdfPageExtractor {

    suspend fun extractPagesToTempFiles(
        context: Context,
        pdfFile: File,
        scale: Float = 2.0f,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<List<ExtractedPdfPage>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!pdfFile.exists()) error("পিডিএফ ফাইলটি পাওয়া যায়নি।")
            val outDir = File(context.filesDir, "workspace/pdf_extract_${System.currentTimeMillis()}").apply {
                mkdirs()
            }
            val result = mutableListOf<ExtractedPdfPage>()
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val total = renderer.pageCount
                    if (total <= 0) error("পিডিএফ ফাইলটিতে কোনো পৃষ্ঠা নেই।")

                    for (i in 0 until total) {
                        coroutineContext.ensureActive()
                        onProgress(i + 1, total)

                        renderer.openPage(i).use { page ->
                            val w = (page.width * scale).roundToInt().coerceIn(300, 2000)
                            val h = (page.height * scale).roundToInt().coerceIn(300, 2600)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            val imgFile = File(outDir, "page_${i + 1}.png")
                            FileOutputStream(imgFile).use { fos ->
                                bmp.compress(Bitmap.CompressFormat.PNG, 95, fos)
                            }
                            bmp.recycle()

                            result.add(
                                ExtractedPdfPage(
                                    originalPageNumber = i + 1,
                                    imagePath = imgFile.absolutePath,
                                    rotationDegrees = 0
                                )
                            )
                        }
                    }
                }
            }
            result
        }
    }

    suspend fun rebuildPdfFromExtractedPages(
        pages: List<ExtractedPdfPage>,
        outputPdfFile: File,
        outputThumbFile: File,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<GeneratedPdfInfo> = withContext(Dispatchers.IO) {
        runCatching {
            if (pages.isEmpty()) error("কমপক্ষে একটি পৃষ্ঠা রাখতে হবে।")
            outputPdfFile.parentFile?.mkdirs()
            val tempPdf = File(outputPdfFile.parentFile, "${outputPdfFile.nameWithoutExtension}.tmp")

            val doc = PdfDocument()
            try {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                pages.forEachIndexed { idx, item ->
                    coroutineContext.ensureActive()
                    onProgress(idx + 1, pages.size)

                    var bmp = BitmapFactory.decodeFile(item.imagePath)
                        ?: error("${idx + 1} নম্বর পৃষ্ঠাটি লোড করা যায়নি।")

                    val rot = ((item.rotationDegrees % 360) + 360) % 360
                    if (rot != 0) {
                        val m = Matrix().apply { postRotate(rot.toFloat()) }
                        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                        if (rotated !== bmp) bmp.recycle()
                        bmp = rotated
                    }

                    // Keep standard proportional page points (approx 72dpi from 2x image or A4 scale)
                    val pageW = max(300, (bmp.width / 2f).roundToInt())
                    val pageH = max(300, (bmp.height / 2f).roundToInt())
                    val info = PdfDocument.PageInfo.Builder(pageW, pageH, idx + 1).create()
                    val page = doc.startPage(info)
                    page.canvas.drawColor(Color.WHITE)
                    page.canvas.drawBitmap(bmp, null, Rect(0, 0, pageW, pageH), paint)
                    doc.finishPage(page)
                    bmp.recycle()
                }

                FileOutputStream(tempPdf).use { fos ->
                    doc.writeTo(fos)
                }
            } finally {
                doc.close()
            }

            if (outputPdfFile.exists()) outputPdfFile.delete()
            if (!tempPdf.renameTo(outputPdfFile)) {
                tempPdf.copyTo(outputPdfFile, overwrite = true)
                tempPdf.delete()
            }

            val thumbOk = PdfGenerator.generatePdfThumbnail(outputPdfFile, outputThumbFile)
            GeneratedPdfInfo(
                pdfFile = outputPdfFile,
                thumbnailFile = if (thumbOk) outputThumbFile else null,
                sizeBytes = outputPdfFile.length(),
                pageCount = pages.size
            )
        }
    }
}
