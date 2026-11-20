package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class PdfToWordResult(
    val docxFile: File,
    val displayTitle: String,
    val totalPages: Int,
    val scannedPagesCount: Int,
    val ocrUsed: Boolean,
    val usedBengaliTesseract: Boolean,
    val extractedImagesCount: Int,
    val extractedBlocksCount: Int,
    val previewSnippet: String,
    val hasLayoutLimitationWarning: Boolean,
    val warningsBn: List<String>
)

object PdfToWordEngine {

    @Volatile
    private var pdfBoxInitialized = false

    private fun ensurePdfBox(context: Context) {
        if (!pdfBoxInitialized) {
            synchronized(this) {
                if (!pdfBoxInitialized) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    pdfBoxInitialized = true
                }
            }
        }
    }

    fun tessdataDir(context: Context): File =
        File(context.filesDir, "tessdata").apply { mkdirs() }

    fun hasBengaliOcrPack(context: Context): Boolean {
        val ben = File(tessdataDir(context), "ben.traineddata")
        return ben.exists() && ben.length() > 100_000L
    }

    fun hasEnglishOcrPack(context: Context): Boolean {
        val eng = File(tessdataDir(context), "eng.traineddata")
        return eng.exists() && eng.length() > 100_000L
    }

    suspend fun downloadBengaliAndEnglishOcrPack(
        context: Context,
        onStatus: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = tessdataDir(context)
            val targets = listOf(
                "ben.traineddata" to "https://github.com/tesseract-ocr/tessdata_fast/raw/main/ben.traineddata",
                "eng.traineddata" to "https://github.com/tesseract-ocr/tessdata_fast/raw/main/eng.traineddata"
            )
            targets.forEachIndexed { idx, (fileName, urlStr) ->
                coroutineContext.ensureActive()
                val finalFile = File(dir, fileName)
                if (finalFile.exists() && finalFile.length() > 100_000L) return@forEachIndexed

                onStatus(
                    if (idx == 0) "বাংলা ওসিআর ডেটা ডাউনলোড হচ্ছে..."
                    else "ইংরেজি ওসিআর ডেটা ডাউনলোড হচ্ছে..."
                )
                val tmpFile = File(dir, "$fileName.tmp")
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 40_000
                    instanceFollowRedirects = true
                }
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) {
                        error("সার্ভার থেকে ওসিআর ডেটা নামানো যায়নি (কোড: $code)।")
                    }
                    conn.inputStream.use { input ->
                        FileOutputStream(tmpFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } finally {
                    conn.disconnect()
                }
                if (!tmpFile.exists() || tmpFile.length() < 100_000L) {
                    tmpFile.delete()
                    error("ডাউনলোড সম্পূর্ণ হয়নি। ইন্টারনেট সংযোগ পরীক্ষা করুন।")
                }
                if (finalFile.exists()) finalFile.delete()
                if (!tmpFile.renameTo(finalFile)) {
                    tmpFile.copyTo(finalFile, overwrite = true)
                    tmpFile.delete()
                }
            }
        }
    }

    suspend fun convertPdfToWord(
        context: Context,
        sourcePdfFile: File,
        displayTitle: String,
        outputDocxFile: File,
        onProgress: (currentPage: Int, totalPages: Int, statusBn: String) -> Unit
    ): Result<PdfToWordResult> = withContext(Dispatchers.IO) {
        runCatching {
            if (!sourcePdfFile.exists() || sourcePdfFile.length() == 0L) {
                error("পিডিএফ ফাইলটি পাওয়া যায়নি।")
            }
            ensurePdfBox(context)

            val blocks = mutableListOf<WordBlock>()
            val warnings = mutableListOf<String>()
            var totalPages = 0
            var scannedPages = 0
            var ocrUsed = false
            var usedBengaliTesseract = false
            var extractedImagesCount = 0
            var hasComplexTablesOrLayout = false

            val hasBen = hasBengaliOcrPack(context)
            val hasEng = hasEnglishOcrPack(context)

            val doc = try {
                PDDocument.load(sourcePdfFile)
            } catch (e: Exception) {
                error("পিডিএফ ফাইলটি খোলা যায়নি (পাসওয়ার্ড-সুরক্ষিত বা ক্ষতিগ্রস্ত হতে পারে)।")
            }

            try {
                if (doc.isEncrypted) {
                    error("পাসওয়ার্ড-সুরক্ষিত পিডিএফ থেকে ওয়ার্ড তৈরি করা যায় না।")
                }
                totalPages = doc.numberOfPages
                if (totalPages <= 0) error("পিডিএফটিতে কোনো পৃষ্ঠা নেই।")

                val pfd = ParcelFileDescriptor.open(sourcePdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)

                try {
                    for (pageIdx in 0 until totalPages) {
                        coroutineContext.ensureActive()
                        val pageNum = pageIdx + 1
                        onProgress(pageNum, totalPages, "পৃষ্ঠা বিশ্লেষণ চলছে...")

                        if (pageIdx > 0) {
                            blocks.add(WordBlock.PageBreakBlock)
                        }

                        // 1. Extract digital structured lines using coordinate-aware PDFTextStripper
                        val stripper = StructuredPdfTextStripper().apply {
                            sortByPosition = true
                            startPage = pageNum
                            endPage = pageNum
                        }
                        stripper.getText(doc)
                        val lineEntries = stripper.extractedLines

                        // 2. Extract embedded raster images on this page
                        val pageImages = extractImagesFromPdPage(doc.getPage(pageIdx))
                        if (pageImages.isNotEmpty()) {
                            extractedImagesCount += pageImages.size
                            hasComplexTablesOrLayout = true
                        }

                        val totalChars = lineEntries.sumOf { it.text.trim().length }
                        if (totalChars >= 18) {
                            val pageBlocks = buildBlocksFromLines(lineEntries)
                            if (pageBlocks.any { it is WordBlock.TableBlock }) {
                                hasComplexTablesOrLayout = true
                            }
                            blocks.addAll(pageBlocks)
                            pageImages.forEach { imgBytes ->
                                blocks.add(WordBlock.ImageBlock(imgBytes, "pdf_p${pageNum}"))
                            }
                        } else {
                            // Scanned / image-only PDF page -> OCR + preserve page image if needed
                            scannedPages++
                            onProgress(pageNum, totalPages, "স্ক্যান করা পৃষ্ঠায় ওসিআর চালানো হচ্ছে...")
                            val renderedBitmap = renderPageToBitmap(renderer, pageIdx, 2.1f)
                            var ocrText = ""
                            if (renderedBitmap != null) {
                                if (hasBen || hasEng) {
                                    val langs = when {
                                        hasBen && hasEng -> "ben+eng"
                                        hasBen -> "ben"
                                        else -> "eng"
                                    }
                                    ocrText = runTesseractOcr(context, renderedBitmap, langs)
                                    if (ocrText.isNotBlank()) {
                                        ocrUsed = true
                                        if (hasBen) usedBengaliTesseract = true
                                    }
                                }
                                if (ocrText.isBlank()) {
                                    ocrText = runMlKitLatinOcr(renderedBitmap)
                                    if (ocrText.isNotBlank()) ocrUsed = true
                                }

                                if (ocrText.isNotBlank()) {
                                    ocrText.split('\n')
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                        .forEach { para ->
                                            blocks.add(WordBlock.ParagraphBlock(text = para))
                                        }
                                }

                                // Also embed original scanned page image if OCR yielded sparse text
                                if (ocrText.length < 40) {
                                    val pngBytes = bitmapToPngBytes(renderedBitmap, maxDim = 1200)
                                    if (pngBytes.isNotEmpty()) {
                                        blocks.add(WordBlock.ImageBlock(pngBytes, "scan_p$pageNum.png"))
                                        extractedImagesCount++
                                    }
                                }
                                renderedBitmap.recycle()
                            }
                            pageImages.forEach { imgBytes ->
                                if (ocrText.length >= 40) {
                                    blocks.add(WordBlock.ImageBlock(imgBytes, "pdf_p${pageNum}"))
                                }
                            }
                        }
                    }
                } finally {
                    renderer.close()
                    pfd.close()
                }
            } finally {
                doc.close()
            }

            if (blocks.isEmpty()) {
                error("পিডিএফ ফাইলটি থেকে কোনো লেখা বা ছবি উদ্ধার করা যায়নি।")
            }

            if (scannedPages > 0 && !hasBen) {
                warnings.add("স্ক্যান করা বাংলা পৃষ্ঠার সম্পূর্ণ টেক্সট উদ্ধার করতে বাংলা ওসিআর প্যাক ডাউনলোড করুন (আপাতত ইংরেজি ওসিআর ও পৃষ্ঠার ছবি যুক্ত করা হয়েছে)।")
            } else if (scannedPages > 0) {
                warnings.add("স্ক্যান করা পৃষ্ঠাগুলো ওসিআর-এর মাধ্যমে রূপান্তরিত হয়েছে; বানান বা ছকের বিন্যাস সামান্য পরিবর্তিত হতে পারে।")
            }
            if (hasComplexTablesOrLayout) {
                warnings.add("জটিল ছক, কলাম বা গ্রাফিক্সযুক্ত পিডিএফের মূল লেআউট ১০০% একইরকম নাও থাকতে পারে।")
            }

            onProgress(totalPages, totalPages, "ওয়ার্ড (.docx) ফাইল তৈরি হচ্ছে...")
            writeDocxPackage(outputDocxFile, blocks)

            val snippet = blocks.filterIsInstance<WordBlock.ParagraphBlock>()
                .take(12)
                .joinToString("\n\n") { it.text }
                .take(900)

            PdfToWordResult(
                docxFile = outputDocxFile,
                displayTitle = displayTitle,
                totalPages = totalPages,
                scannedPagesCount = scannedPages,
                ocrUsed = ocrUsed,
                usedBengaliTesseract = usedBengaliTesseract,
                extractedImagesCount = extractedImagesCount,
                extractedBlocksCount = blocks.size,
                previewSnippet = snippet,
                hasLayoutLimitationWarning = warnings.isNotEmpty(),
                warningsBn = warnings
            )
        }
    }

    private fun buildBlocksFromLines(lines: List<ExtractedLine>): List<WordBlock> {
        val result = mutableListOf<WordBlock>()
        val medianFont = lines.map { it.maxFontSize }.sorted().let {
            if (it.isEmpty()) 11f else it[it.size / 2]
        }

        val tableBuffer = mutableListOf<List<String>>()
        val paraBuffer = StringBuilder()
        var paraIsHeading = false
        var paraHeadingLevel = 0
        var lastBottomY = -1f

        fun flushPara() {
            val txt = paraBuffer.toString().trim()
            if (txt.isNotEmpty()) {
                result.add(
                    WordBlock.ParagraphBlock(
                        text = txt,
                        isHeading = paraIsHeading,
                        headingLevel = paraHeadingLevel,
                        isBold = paraIsHeading
                    )
                )
            }
            paraBuffer.clear()
            paraIsHeading = false
            paraHeadingLevel = 0
        }

        fun flushTable() {
            if (tableBuffer.isNotEmpty()) {
                val maxCols = tableBuffer.maxOf { it.size }
                val normalized = tableBuffer.map { row ->
                    if (row.size == maxCols) row else row + List(maxCols - row.size) { "" }
                }
                result.add(WordBlock.TableBlock(normalized))
                tableBuffer.clear()
            }
        }

        for (line in lines) {
            val clean = line.text.trim()
            if (clean.isEmpty()) continue

            if (line.columns.size >= 2) {
                flushPara()
                tableBuffer.add(line.columns)
                lastBottomY = line.y
                continue
            } else {
                flushTable()
            }

            val isHeadingLine = line.maxFontSize >= medianFont * 1.25f && clean.length <= 120
            val largeVerticalGap = lastBottomY > 0f && abs(line.y - lastBottomY) > line.maxFontSize * 1.85f

            if (paraBuffer.isNotEmpty() && (largeVerticalGap || isHeadingLine != paraIsHeading)) {
                flushPara()
            }

            if (paraBuffer.isEmpty()) {
                paraIsHeading = isHeadingLine
                paraHeadingLevel = if (line.maxFontSize >= medianFont * 1.55f) 1 else if (isHeadingLine) 2 else 0
            } else {
                paraBuffer.append(' ')
            }
            paraBuffer.append(clean)
            lastBottomY = line.y
        }
        flushPara()
        flushTable()
        return result
    }

    private fun extractImagesFromPdPage(page: com.tom_roush.pdfbox.pdmodel.PDPage): List<ByteArray> {
        val list = mutableListOf<ByteArray>()
        return try {
            val resources = page.resources ?: return emptyList()
            for (name in resources.xObjectNames) {
                val xobj = resources.getXObject(name)
                if (xobj is PDImageXObject) {
                    val bmp = xobj.image
                    if (bmp != null) {
                        val bytes = bitmapToPngBytes(bmp, 1100)
                        if (bytes.isNotEmpty()) list.add(bytes)
                        bmp.recycle()
                    }
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun renderPageToBitmap(renderer: PdfRenderer, pageIdx: Int, scale: Float): Bitmap? {
        return try {
            renderer.openPage(pageIdx).use { page ->
                val w = (page.width * scale).toInt().coerceIn(480, 1800)
                val h = (page.height * scale).toInt().coerceIn(480, 2400)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bmp
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun bitmapToPngBytes(bitmap: Bitmap, maxDim: Int): ByteArray {
        val scaled = if (max(bitmap.width, bitmap.height) > maxDim) {
            val ratio = maxDim.toFloat() / max(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                max(1, (bitmap.width * ratio).toInt()),
                max(1, (bitmap.height * ratio).toInt()),
                true
            )
        } else bitmap
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 92, baos)
        if (scaled !== bitmap) scaled.recycle()
        return baos.toByteArray()
    }

    private fun runTesseractOcr(context: Context, bitmap: Bitmap, langCode: String): String {
        val api = TessBaseAPI()
        return try {
            val ok = api.init(context.filesDir.absolutePath, langCode)
            if (!ok) return ""
            api.setImage(bitmap)
            api.utF8Text?.trim() ?: ""
        } catch (_: Exception) {
            ""
        } finally {
            try {
                api.recycle()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun runMlKitLatinOcr(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val input = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(input)
                .addOnSuccessListener { result ->
                    recognizer.close()
                    if (cont.isActive) cont.resume(result.text.trim())
                }
                .addOnFailureListener {
                    recognizer.close()
                    if (cont.isActive) cont.resume("")
                }
        } catch (_: Exception) {
            if (cont.isActive) cont.resume("")
        }
    }

    /**
     * Generates a genuine OOXML .docx ZIP package readable by Microsoft Word, Google Docs, and LibreOffice.
     */
    fun writeDocxPackage(outputFile: File, blocks: List<WordBlock>) {
        outputFile.parentFile?.mkdirs()
        val tmp = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}.tmp.docx")
        val mediaImages = mutableListOf<Pair<String, ByteArray>>() // rId -> png bytes

        val bodyXml = StringBuilder()
        var docPrId = 1

        for (block in blocks) {
            when (block) {
                is WordBlock.PageBreakBlock -> {
                    bodyXml.append("""<w:p><w:r><w:br w:type="page"/></w:r></w:p>""")
                }

                is WordBlock.ParagraphBlock -> {
                    val escaped = escapeXml(stripInvalidXmlChars(block.text))
                    bodyXml.append("<w:p>")
                    val hasPpr = block.isHeading || block.alignment != WordAlign.LEFT
                    if (hasPpr) {
                        bodyXml.append("<w:pPr>")
                        if (block.isHeading) {
                            val styleId = if (block.headingLevel <= 1) "Heading1" else "Heading2"
                            bodyXml.append("""<w:pStyle w:val="$styleId"/>""")
                        }
                        if (block.alignment != WordAlign.LEFT) {
                            val jc = when (block.alignment) {
                                WordAlign.CENTER -> "center"
                                WordAlign.RIGHT -> "right"
                                WordAlign.JUSTIFY -> "both"
                                WordAlign.LEFT -> "left"
                            }
                            bodyXml.append("""<w:jc w:val="$jc"/>""")
                        }
                        bodyXml.append("</w:pPr>")
                    }
                    bodyXml.append("<w:r>")
                    if (block.isBold || block.isItalic || block.isHeading) {
                        bodyXml.append("<w:rPr>")
                        if (block.isBold || block.isHeading) bodyXml.append("<w:b/>")
                        if (block.isItalic) bodyXml.append("<w:i/>")
                        bodyXml.append("</w:rPr>")
                    }
                    val lines = escaped.split('\n')
                    lines.forEachIndexed { idx, l ->
                        if (idx > 0) bodyXml.append("<w:br/>")
                        bodyXml.append("""<w:t xml:space="preserve">$l</w:t>""")
                    }
                    bodyXml.append("</w:r></w:p>")
                }

                is WordBlock.TableBlock -> {
                    bodyXml.append(
                        """<w:tbl><w:tblPr><w:tblStyle w:val="TableGrid"/><w:tblW w:w="5000" w:type="pct"/><w:tblBorders>""" +
                            """<w:top w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>""" +
                            """<w:left w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>""" +
                            """<w:bottom w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>""" +
                            """<w:right w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>""" +
                            """<w:insideH w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>""" +
                            """<w:insideV w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>""" +
                            """</w:tblBorders></w:tblPr>"""
                    )
                    block.rows.forEach { row ->
                        bodyXml.append("<w:tr>")
                        row.forEach { cell ->
                            val cellEsc = escapeXml(stripInvalidXmlChars(cell))
                            bodyXml.append("""<w:tc><w:p><w:r><w:t xml:space="preserve">$cellEsc</w:t></w:r></w:p></w:tc>""")
                        }
                        bodyXml.append("</w:tr>")
                    }
                    bodyXml.append("</w:tbl>")
                }

                is WordBlock.ImageBlock -> {
                    val imgNum = mediaImages.size + 1
                    val rId = "rIdImg$imgNum"
                    mediaImages.add(rId to block.imageBytes)
                    val cx = 5000000L // ~5.5 inches
                    val cy = 3500000L
                    val currentDocPr = docPrId++
                    bodyXml.append(
                        """<w:p><w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0">""" +
                            """<wp:extent cx="$cx" cy="$cy"/>""" +
                            """<wp:docPr id="$currentDocPr" name="Image$imgNum"/>""" +
                            """<a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">""" +
                            """<a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">""" +
                            """<pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">""" +
                            """<pic:nvPicPr><pic:cNvPr id="$currentDocPr" name="image$imgNum.png"/><pic:cNvPicPr/></pic:nvPicPr>""" +
                            """<pic:blipFill><a:blip r:embed="$rId"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>""" +
                            """<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="$cx" cy="$cy"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr>""" +
                            """</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>"""
                    )
                }
            }
        }

        val documentXml =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
 xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
 xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing">
<w:body>$bodyXml<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1080" w:right="1080" w:bottom="1080" w:left="1080"/></w:sectPr></w:body>
</w:document>"""

        val contentTypesXml =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Default Extension="png" ContentType="image/png"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""

        val rootRelsXml =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        val docRelsEntries = StringBuilder()
        docRelsEntries.append(
            """<Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>"""
        )
        mediaImages.forEachIndexed { idx, (rId, _) ->
            val num = idx + 1
            docRelsEntries.append(
                """<Relationship Id="$rId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image$num.png"/>"""
            )
        }
        val docRelsXml =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$docRelsEntries
</Relationships>"""

        val stylesXml =
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:style w:type="paragraph" w:default="1" w:styleId="Normal">
<w:name w:val="Normal"/><w:rPr><w:sz w:val="24"/><w:szCs w:val="24"/></w:rPr>
</w:style>
<w:style w:type="paragraph" w:styleId="Heading1">
<w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:rPr><w:b/><w:sz w:val="34"/><w:szCs w:val="34"/></w:rPr>
</w:style>
<w:style w:type="paragraph" w:styleId="Heading2">
<w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:rPr><w:b/><w:sz w:val="28"/><w:szCs w:val="28"/></w:rPr>
</w:style>
<w:style w:type="table" w:styleId="TableGrid"><w:name w:val="Table Grid"/></w:style>
</w:styles>"""

        ZipOutputStream(FileOutputStream(tmp)).use { zos ->
            fun putText(name: String, xml: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(xml.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            putText("[Content_Types].xml", contentTypesXml)
            putText("_rels/.rels", rootRelsXml)
            putText("word/document.xml", documentXml)
            putText("word/styles.xml", stylesXml)
            putText("word/_rels/document.xml.rels", docRelsXml)

            mediaImages.forEachIndexed { idx, (_, imgBytes) ->
                val num = idx + 1
                zos.putNextEntry(ZipEntry("word/media/image$num.png"))
                zos.write(imgBytes)
                zos.closeEntry()
            }
        }

        if (outputFile.exists()) outputFile.delete()
        if (!tmp.renameTo(outputFile)) {
            tmp.copyTo(outputFile, overwrite = true)
            tmp.delete()
        }
    }

    private fun escapeXml(input: String): String {
        val sb = StringBuilder(input.length + 16)
        for (ch in input) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun stripInvalidXmlChars(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val code = ch.code
            if (code == 0x9 || code == 0xA || code == 0xD || (code in 0x20..0xD7FF) || (code in 0xE000..0xFFFD)) {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}

private data class ExtractedLine(
    val text: String,
    val y: Float,
    val maxFontSize: Float,
    val columns: List<String>
)

private class StructuredPdfTextStripper : PDFTextStripper() {
    val extractedLines = mutableListOf<ExtractedLine>()

    override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
        if (textPositions.isEmpty()) return
        val y = textPositions.first().yDirAdj
        val maxSize = textPositions.maxOfOrNull { it.fontSizeInPt } ?: 11f

        // Detect wide horizontal gaps between runs to identify tabular rows
        val cols = mutableListOf<StringBuilder>()
        var currentCol = StringBuilder()
        var prevEndX = textPositions.first().xDirAdj
        val avgSpace = max(6f, textPositions.first().widthOfSpace)

        for (pos in textPositions) {
            val gap = pos.xDirAdj - prevEndX
            if (gap > avgSpace * 4.5f && currentCol.isNotBlank()) {
                cols.add(currentCol)
                currentCol = StringBuilder()
            }
            currentCol.append(pos.unicode)
            prevEndX = pos.xDirAdj + pos.widthDirAdj
        }
        if (currentCol.isNotBlank()) cols.add(currentCol)

        val cleanedCols = if (cols.size >= 2) {
            cols.map { it.toString().trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        extractedLines.add(
            ExtractedLine(
                text = text,
                y = y,
                maxFontSize = maxSize,
                columns = cleanedCols
            )
        )
    }
}
