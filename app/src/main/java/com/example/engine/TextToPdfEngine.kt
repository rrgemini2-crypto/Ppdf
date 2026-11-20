package com.example.engine

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.PdfPageSize
import com.example.util.BengaliFormatters
import com.example.util.FileActionsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.max

enum class TextAlignOption(
    val titleBn: String,
    val layoutAlignment: Layout.Alignment
) {
    LEFT("বাম", Layout.Alignment.ALIGN_NORMAL),
    CENTER("মাঝখান", Layout.Alignment.ALIGN_CENTER),
    RIGHT("ডান", Layout.Alignment.ALIGN_OPPOSITE)
}

data class TextPdfConfig(
    val fileName: String,
    val title: String = "",
    val bodyText: String,
    val fontSizePt: Float = 13f,
    val alignment: TextAlignOption = TextAlignOption.LEFT,
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val folderId: Long? = null
)

data class PaginatedTextPage(
    val pageNumber: Int,
    val showTitle: Boolean,
    val lines: List<String>
)

object TextToPdfEngine {

    private fun resolveBengaliTypeface(context: Context): Typeface {
        return try {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali) ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    /**
     * Pure JVM-safe page estimator and line wrapper for live Compose preview & unit testing.
     */
    fun paginateTextForPreview(
        title: String,
        bodyText: String,
        fontSizePt: Float,
        pageSize: PdfPageSize
    ): List<PaginatedTextPage> {
        val pageW = pageSize.widthPt
        val pageH = pageSize.heightPt
        val margin = 44
        val contentW = max(120, pageW - margin * 2)
        val approxCharW = fontSizePt * 0.56f
        val charsPerLine = max(14, (contentW / approxCharW).toInt())
        val lineHeight = fontSizePt * 1.48f
        val footerReserve = 28f
        val usableH = (pageH - margin * 2 - footerReserve).coerceAtLeast(100f)

        val wrappedLines = mutableListOf<String>()
        val rawParagraphs = bodyText.replace("\r\n", "\n").split('\n')
        for (para in rawParagraphs) {
            if (para.isBlank()) {
                wrappedLines.add("")
                continue
            }
            val words = para.split(' ')
            val lineBuilder = StringBuilder()
            for (word in words) {
                if (word.length > charsPerLine) {
                    if (lineBuilder.isNotEmpty()) {
                        wrappedLines.add(lineBuilder.toString())
                        lineBuilder.clear()
                    }
                    var idx = 0
                    while (idx < word.length) {
                        val end = (idx + charsPerLine).coerceAtMost(word.length)
                        wrappedLines.add(word.substring(idx, end))
                        idx = end
                    }
                } else {
                    val candidateLen = if (lineBuilder.isEmpty()) word.length else lineBuilder.length + 1 + word.length
                    if (candidateLen <= charsPerLine) {
                        if (lineBuilder.isNotEmpty()) lineBuilder.append(' ')
                        lineBuilder.append(word)
                    } else {
                        wrappedLines.add(lineBuilder.toString())
                        lineBuilder.clear()
                        lineBuilder.append(word)
                    }
                }
            }
            if (lineBuilder.isNotEmpty()) {
                wrappedLines.add(lineBuilder.toString())
            }
        }
        if (wrappedLines.isEmpty()) wrappedLines.add("")

        val pages = mutableListOf<PaginatedTextPage>()
        val titleReserve = if (title.isNotBlank()) (fontSizePt * 1.5f + 28f) else 0f
        var cursor = 0
        var pageNum = 1

        while (cursor < wrappedLines.size || (pageNum == 1 && title.isNotBlank())) {
            val isFirst = pageNum == 1
            val avail = (usableH - if (isFirst) titleReserve else 0f).coerceAtLeast(lineHeight)
            val maxLinesOnPage = max(1, (avail / lineHeight).toInt())
            val end = (cursor + maxLinesOnPage).coerceAtMost(wrappedLines.size)
            val slice = if (cursor < end) wrappedLines.subList(cursor, end).toList() else emptyList()
            pages.add(
                PaginatedTextPage(
                    pageNumber = pageNum,
                    showTitle = isFirst && title.isNotBlank(),
                    lines = slice
                )
            )
            cursor = end
            pageNum++
            if (cursor >= wrappedLines.size) break
        }
        return pages
    }

    suspend fun createPdfFromText(
        context: Context,
        config: TextPdfConfig,
        outputPdfFile: File,
        outputThumbFile: File,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<GeneratedPdfInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanBody = config.bodyText.trimEnd()
            if (cleanBody.isBlank() && config.title.isBlank()) {
                error("পিডিএফ তৈরির জন্য অনুগ্রহ করে কিছু লেখা লিখুন।")
            }
            if (FileActionsHelper.availableInternalBytes(context) < 4_000_000L) {
                error("মোবাইলে পর্যাপ্ত ফাঁকা জায়গা নেই।")
            }

            outputPdfFile.parentFile?.mkdirs()
            outputThumbFile.parentFile?.mkdirs()
            val tempPdf = File(outputPdfFile.parentFile, "${outputPdfFile.nameWithoutExtension}.tmp")

            val typeface = resolveBengaliTypeface(context)
            val pageW = config.pageSize.widthPt
            val pageH = config.pageSize.heightPt
            val margin = 44
            val contentW = max(120, pageW - margin * 2)
            val footerReserve = 28f

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = (config.fontSizePt + 5f).coerceIn(15f, 28f)
                this.typeface = Typeface.create(typeface, Typeface.BOLD)
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(25, 32, 48)
                textSize = config.fontSizePt.coerceIn(9f, 28f)
                this.typeface = typeface
            }
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 9.5f
                this.typeface = typeface
                textAlign = Paint.Align.CENTER
            }
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 1f
            }

            val titleLayout = if (config.title.isNotBlank()) {
                StaticLayout.Builder
                    .obtain(config.title.trim(), 0, config.title.trim().length, titlePaint, contentW)
                    .setAlignment(config.alignment.layoutAlignment)
                    .setLineSpacing(2f, 1.15f)
                    .build()
            } else {
                null
            }

            val fullBody = cleanBody.ifBlank { " " }
            val bodyLayout = StaticLayout.Builder
                .obtain(fullBody, 0, fullBody.length, bodyPaint, contentW)
                .setAlignment(config.alignment.layoutAlignment)
                .setLineSpacing(3f, 1.35f)
                .setIncludePad(true)
                .build()

            // Slice lines accurately into page ranges using StaticLayout coordinates
            val pageLineRanges = mutableListOf<IntRange>()
            var lineIdx = 0
            val totalLines = bodyLayout.lineCount
            var pageIndex = 0
            while (lineIdx < totalLines || (pageIndex == 0 && titleLayout != null)) {
                val topMargin = margin.toFloat() + if (pageIndex == 0 && titleLayout != null) {
                    titleLayout.height + 18f
                } else {
                    0f
                }
                val maxBottomOnPage = pageH - margin - footerReserve
                val availBodyHeight = (maxBottomOnPage - topMargin).coerceAtLeast(30f)

                val startLine = lineIdx
                if (startLine < totalLines) {
                    val startY = bodyLayout.getLineTop(startLine)
                    var endLine = startLine
                    while (endLine < totalLines) {
                        val lineBottomRelative = bodyLayout.getLineBottom(endLine) - startY
                        if (lineBottomRelative > availBodyHeight && endLine > startLine) {
                            break
                        }
                        endLine++
                    }
                    pageLineRanges.add(startLine until endLine)
                    lineIdx = endLine
                } else {
                    pageLineRanges.add(0 until 0)
                }
                pageIndex++
                if (lineIdx >= totalLines) break
            }

            val totalPages = max(1, pageLineRanges.size)
            val pdfDoc = PdfDocument()
            try {
                pageLineRanges.forEachIndexed { idx, lineRange ->
                    coroutineContext.ensureActive()
                    onProgress(idx + 1, totalPages)

                    val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, idx + 1).create()
                    val page = pdfDoc.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)

                    var cursorY = margin.toFloat()
                    if (idx == 0 && titleLayout != null) {
                        canvas.save()
                        canvas.translate(margin.toFloat(), cursorY)
                        titleLayout.draw(canvas)
                        canvas.restore()
                        cursorY += titleLayout.height + 8f
                        canvas.drawLine(margin.toFloat(), cursorY, (pageW - margin).toFloat(), cursorY, dividerPaint)
                        cursorY += 12f
                    }

                    if (!lineRange.isEmpty()) {
                        val startLine = lineRange.first
                        val endLine = lineRange.last
                        val clipTop = bodyLayout.getLineTop(startLine).toFloat()
                        val clipBottom = bodyLayout.getLineBottom(endLine).toFloat()

                        canvas.save()
                        canvas.translate(margin.toFloat(), cursorY - clipTop)
                        canvas.clipRect(0f, clipTop, contentW.toFloat(), clipBottom)
                        bodyLayout.draw(canvas)
                        canvas.restore()
                    }

                    val footerText = "পৃষ্ঠা ${BengaliFormatters.toBengaliDigits(idx + 1)} / ${BengaliFormatters.toBengaliDigits(totalPages)}"
                    canvas.drawText(footerText, pageW / 2f, pageH - 18f, footerPaint)
                    pdfDoc.finishPage(page)
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

            val thumbOk = PdfGenerator.generatePdfThumbnail(outputPdfFile, outputThumbFile)
            GeneratedPdfInfo(
                pdfFile = outputPdfFile,
                thumbnailFile = if (thumbOk) outputThumbFile else null,
                sizeBytes = outputPdfFile.length(),
                pageCount = totalPages
            )
        }
    }
}
