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
import com.example.data.model.CompressionLevel
import com.example.data.model.CompressionResult
import com.example.util.FileActionsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToInt

object PdfCompressor {

    suspend fun compressPdf(
        context: Context,
        sourceFile: File,
        outputFile: File,
        level: CompressionLevel,
        onProgress: (current: Int, total: Int) -> Unit
    ): CompressionResult = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() == 0L) {
            return@withContext CompressionResult.Error("পিডিএফ ফাইলটি পাওয়া যায়নি বা খালি।")
        }

        val originalSize = sourceFile.length()
        if (FileActionsHelper.availableInternalBytes(context) < originalSize + 5_000_000L) {
            return@withContext CompressionResult.Error("মোবাইলে পর্যাপ্ত ফাঁকা জায়গা নেই।")
        }

        outputFile.parentFile?.mkdirs()
        val tempFile = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}.tmp")

        try {
            ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val totalPages = renderer.pageCount
                    if (totalPages <= 0) {
                        return@withContext CompressionResult.Error("এই পিডিএফ ফাইলটিতে কোনো পৃষ্ঠা পাওয়া যায়নি।")
                    }

                    val outDoc = PdfDocument()
                    try {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                        for (i in 0 until totalPages) {
                            coroutineContext.ensureActive()
                            onProgress(i + 1, totalPages)

                            renderer.openPage(i).use { page ->
                                val pageW = max(72, page.width)
                                val pageH = max(72, page.height)

                                val renderW = (pageW * level.scaleFactor).roundToInt().coerceIn(240, 2200)
                                val renderH = (pageH * level.scaleFactor).roundToInt().coerceIn(240, 2800)

                                val rendered = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                                rendered.eraseColor(Color.WHITE)
                                page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val baos = ByteArrayOutputStream()
                                rendered.compress(Bitmap.CompressFormat.JPEG, level.jpegQuality, baos)
                                rendered.recycle()

                                val jpegBytes = baos.toByteArray()
                                val compressedBmp = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
                                    ?: error("পৃষ্ঠা সংকোচন করতে সমস্যা হয়েছে।")

                                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, i + 1).create()
                                val outPage = outDoc.startPage(pageInfo)
                                outPage.canvas.drawColor(Color.WHITE)
                                outPage.canvas.drawBitmap(
                                    compressedBmp,
                                    null,
                                    Rect(0, 0, pageW, pageH),
                                    paint
                                )
                                outDoc.finishPage(outPage)
                                compressedBmp.recycle()
                            }
                        }

                        FileOutputStream(tempFile).use { fos ->
                            outDoc.writeTo(fos)
                        }
                    } finally {
                        outDoc.close()
                    }

                    val compressedSize = tempFile.length()
                    if (compressedSize >= (originalSize * 0.97).toLong() || compressedSize <= 0L) {
                        tempFile.delete()
                        return@withContext CompressionResult.AlreadyCompressed(
                            originalSizeBytes = originalSize,
                            pageCount = totalPages
                        )
                    }

                    if (outputFile.exists()) outputFile.delete()
                    if (!tempFile.renameTo(outputFile)) {
                        tempFile.copyTo(outputFile, overwrite = true)
                        tempFile.delete()
                    }

                    val savedBytes = originalSize - compressedSize
                    val savedPercent = ((savedBytes.toDouble() / originalSize.toDouble()) * 100.0)
                        .roundToInt()
                        .coerceIn(1, 99)

                    CompressionResult.Success(
                        outputFilePath = outputFile.absolutePath,
                        outputFileName = outputFile.nameWithoutExtension,
                        originalSizeBytes = originalSize,
                        compressedSizeBytes = compressedSize,
                        savedBytes = savedBytes,
                        savedPercent = savedPercent,
                        pageCount = totalPages
                    )
                }
            }
        } catch (e: SecurityException) {
            tempFile.delete()
            CompressionResult.Error("পাসওয়ার্ড সুরক্ষিত পিডিএফ সংকুচিত করা যায়নি।")
        } catch (e: OutOfMemoryError) {
            tempFile.delete()
            CompressionResult.Error("ফাইলটি খুব বড় হওয়ায় মেমোরির সীমা অতিক্রম করেছে।")
        } catch (e: Exception) {
            tempFile.delete()
            CompressionResult.Error("পিডিএফ ফাইলটি ক্ষতিগ্রস্ত অথবা সংকুচিত করতে সমস্যা হয়েছে।")
        }
    }
}
