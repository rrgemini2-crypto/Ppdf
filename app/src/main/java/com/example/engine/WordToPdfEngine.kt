package com.example.engine

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.util.BengaliFormatters
import com.example.util.FileActionsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object WordToPdfEngine {

    private fun resolveBengaliTypeface(context: Context): Typeface {
        return try {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali) ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    suspend fun convertParsedWordToPdf(
        context: Context,
        parsed: ParsedWordDocument,
        outputPdfFile: File,
        outputThumbFile: File,
        onProgress: (currentBlock: Int, totalBlocks: Int) -> Unit = { _, _ -> }
    ): Result<GeneratedPdfInfo> = withContext(Dispatchers.IO) {
        runCatching {
            if (parsed.blocks.isEmpty()) {
                error("ওয়ার্ড ফাইলটিতে রূপান্তরের মতো কোনো বিষয়বস্তু পাওয়া যায়নি।")
            }
            if (FileActionsHelper.availableInternalBytes(context) < 4_000_000L) {
                error("মোবাইলে পর্যাপ্ত ফাঁকা জায়গা নেই।")
            }

            outputPdfFile.parentFile?.mkdirs()
            outputThumbFile.parentFile?.mkdirs()
            val tempPdf = File(outputPdfFile.parentFile, "${outputPdfFile.nameWithoutExtension}.tmp")

            val pageW = 595
            val pageH = 842
            val margin = 44f
            val contentW = (pageW - margin * 2).toInt()
            val bottomLimit = pageH - margin - 24f

            val baseTypeface = resolveBengaliTypeface(context)
            val boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD)
            val italicTypeface = Typeface.create(baseTypeface, Typeface.ITALIC)
            val boldItalicTypeface = Typeface.create(baseTypeface, Typeface.BOLD_ITALIC)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(180, 194, 214)
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            val headerCellBg = Paint().apply {
                color = Color.rgb(239, 246, 255)
                style = Paint.Style.FILL
            }
            val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 9.5f
                typeface = baseTypeface
                textAlign = Paint.Align.CENTER
            }

            val pdfDoc = PdfDocument()
            var pageNumber = 0
            var currentPage: PdfDocument.Page? = null
            var canvas: Canvas? = null
            var cursorY = margin

            fun finishCurrentPage() {
                val p = currentPage ?: return
                val c = canvas ?: return
                c.drawText(
                    "পৃষ্ঠা ${BengaliFormatters.toBengaliDigits(pageNumber)}",
                    pageW / 2f,
                    pageH - 18f,
                    footerPaint
                )
                pdfDoc.finishPage(p)
                currentPage = null
                canvas = null
            }

            fun startNextPage() {
                finishCurrentPage()
                pageNumber++
                val info = PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create()
                val p = pdfDoc.startPage(info)
                p.canvas.drawColor(Color.WHITE)
                currentPage = p
                canvas = p.canvas
                cursorY = margin
            }

            try {
                startNextPage()

                val total = parsed.blocks.size
                parsed.blocks.forEachIndexed { idx, block ->
                    coroutineContext.ensureActive()
                    onProgress(idx + 1, total)

                    when (block) {
                        is WordBlock.PageBreakBlock -> {
                            if (cursorY > margin + 8f) {
                                startNextPage()
                            }
                        }

                        is WordBlock.ParagraphBlock -> {
                            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = if (block.isHeading) Color.rgb(15, 23, 42) else Color.rgb(30, 41, 59)
                                textSize = when {
                                    block.isHeading && block.headingLevel == 1 -> 18f
                                    block.isHeading && block.headingLevel == 2 -> 15.5f
                                    block.isHeading -> 14f
                                    else -> 12f
                                }
                                typeface = when {
                                    (block.isBold || block.isHeading) && block.isItalic -> boldItalicTypeface
                                    block.isBold || block.isHeading -> boldTypeface
                                    block.isItalic -> italicTypeface
                                    else -> baseTypeface
                                }
                            }
                            val align = when (block.alignment) {
                                WordAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
                                WordAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
                                else -> Layout.Alignment.ALIGN_NORMAL
                            }
                            val layout = StaticLayout.Builder
                                .obtain(block.text, 0, block.text.length, paint, contentW)
                                .setAlignment(align)
                                .setLineSpacing(2.5f, 1.28f)
                                .setIncludePad(true)
                                .build()

                            // Render line-by-line across pages cleanly if paragraph exceeds remaining page space
                            var lineIdx = 0
                            while (lineIdx < layout.lineCount) {
                                val avail = bottomLimit - cursorY
                                if (avail < 22f) {
                                    startNextPage()
                                    continue
                                }
                                val sliceStartTop = layout.getLineTop(lineIdx)
                                var endLine = lineIdx
                                while (endLine < layout.lineCount) {
                                    val h = layout.getLineBottom(endLine) - sliceStartTop
                                    if (h > avail && endLine > lineIdx) break
                                    endLine++
                                }
                                if (endLine == lineIdx) endLine = lineIdx + 1
                                val sliceBottom = layout.getLineBottom(endLine - 1)
                                val sliceHeight = (sliceBottom - sliceStartTop).toFloat()

                                val c = canvas!!
                                c.save()
                                c.translate(margin, cursorY - sliceStartTop)
                                c.clipRect(0f, sliceStartTop.toFloat(), contentW.toFloat(), sliceBottom.toFloat())
                                layout.draw(c)
                                c.restore()

                                cursorY += sliceHeight
                                lineIdx = endLine
                                if (lineIdx < layout.lineCount) {
                                    startNextPage()
                                }
                            }
                            cursorY += if (block.isHeading) 10f else 7f
                        }

                        is WordBlock.TableBlock -> {
                            val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.rgb(30, 41, 59)
                                textSize = 10.5f
                                typeface = baseTypeface
                            }
                            val headerCellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.rgb(15, 23, 42)
                                textSize = 10.5f
                                typeface = boldTypeface
                            }

                            block.rows.forEachIndexed { rIdx, row ->
                                val colCount = max(1, row.size)
                                val colW = contentW.toFloat() / colCount
                                val cellLayouts = row.map { cellText ->
                                    val textW = max(24, (colW - 10f).toInt())
                                    val safe = cellText.ifBlank { " " }
                                    StaticLayout.Builder
                                        .obtain(safe, 0, safe.length, if (rIdx == 0) headerCellPaint else cellPaint, textW)
                                        .setLineSpacing(1.5f, 1.15f)
                                        .build()
                                }
                                val rowH = max(22f, (cellLayouts.maxOfOrNull { it.height } ?: 14) + 10f)
                                if (cursorY + rowH > bottomLimit) {
                                    startNextPage()
                                }

                                val c = canvas!!
                                row.indices.forEach { cIdx ->
                                    val left = margin + cIdx * colW
                                    val right = left + colW
                                    if (rIdx == 0) {
                                        c.drawRect(left, cursorY, right, cursorY + rowH, headerCellBg)
                                    }
                                    c.drawRect(left, cursorY, right, cursorY + rowH, borderPaint)
                                    c.save()
                                    c.translate(left + 5f, cursorY + 5f)
                                    cellLayouts[cIdx].draw(c)
                                    c.restore()
                                }
                                cursorY += rowH
                            }
                            cursorY += 10f
                        }

                        is WordBlock.ImageBlock -> {
                            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(block.imageBytes, 0, block.imageBytes.size, opts)
                            if (opts.outWidth > 0 && opts.outHeight > 0) {
                                val sampleOpts = BitmapFactory.Options().apply {
                                    inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, 1200, 1200)
                                }
                                val bmp = BitmapFactory.decodeByteArray(block.imageBytes, 0, block.imageBytes.size, sampleOpts)
                                if (bmp != null) {
                                    val maxDrawH = (bottomLimit - margin) * 0.72f
                                    val scale = min(contentW.toFloat() / bmp.width, maxDrawH / bmp.height).coerceAtMost(1.2f)
                                    val drawW = (bmp.width * scale).roundToInt().coerceAtLeast(24)
                                    val drawH = (bmp.height * scale).roundToInt().coerceAtLeast(24)

                                    if (cursorY + drawH > bottomLimit) {
                                        startNextPage()
                                    }
                                    val left = margin + (contentW - drawW) / 2f
                                    canvas!!.drawBitmap(
                                        bmp,
                                        null,
                                        Rect(left.roundToInt(), cursorY.roundToInt(), (left + drawW).roundToInt(), (cursorY + drawH).roundToInt()),
                                        imgPaint
                                    )
                                    bmp.recycle()
                                    cursorY += drawH + 10f
                                }
                            }
                        }
                    }
                }

                finishCurrentPage()
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

            val thumbOk = PdfGenerator.generatePdfThumbnail(outputPdfFile, outputThumbFile)
            GeneratedPdfInfo(
                pdfFile = outputPdfFile,
                thumbnailFile = if (thumbOk) outputThumbFile else null,
                sizeBytes = outputPdfFile.length(),
                pageCount = max(1, pageNumber)
            )
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqW: Int, reqH: Int): Int {
        var inSampleSize = 1
        if (height > reqH || width > reqW) {
            val halfH = height / 2
            val halfW = width / 2
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
