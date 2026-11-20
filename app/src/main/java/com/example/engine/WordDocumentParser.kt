package com.example.engine

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.zip.ZipFile
import kotlin.math.min

enum class WordAlign {
    LEFT, CENTER, RIGHT, JUSTIFY
}

sealed interface WordBlock {
    data class ParagraphBlock(
        val text: String,
        val isHeading: Boolean = false,
        val headingLevel: Int = 0,
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val alignment: WordAlign = WordAlign.LEFT
    ) : WordBlock

    data class TableBlock(
        val rows: List<List<String>>
    ) : WordBlock

    data class ImageBlock(
        val imageBytes: ByteArray,
        val name: String = ""
    ) : WordBlock

    data object PageBreakBlock : WordBlock
}

data class ParsedWordDocument(
    val title: String,
    val blocks: List<WordBlock>,
    val sourceFormat: String,
    val warningsBn: List<String> = emptyList()
)

object WordDocumentParser {

    fun parseWordFile(file: File, displayTitle: String): Result<ParsedWordDocument> = runCatching {
        if (!file.exists() || file.length() == 0L) {
            error("ওয়ার্ড ফাইলটি খালি অথবা পাওয়া যায়নি।")
        }

        val header = ByteArray(8)
        file.inputStream().use { it.read(header) }

        // Detect ZIP / DOCX magic signature: PK\x03\x04
        val isZip = header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
        // Detect OLE2 CFB / Legacy .DOC magic signature: D0 CF 11 E0 A1 B1 1A E1
        val isOle2 = header[0] == 0xD0.toByte() && header[1] == 0xCF.toByte() &&
            header[2] == 0x11.toByte() && header[3] == 0xE0.toByte()

        when {
            isZip -> parseDocx(file, displayTitle)
            isOle2 -> parseLegacyDoc(file, displayTitle)
            file.extension.equals("docx", ignoreCase = true) -> parseDocx(file, displayTitle)
            file.extension.equals("doc", ignoreCase = true) -> parseLegacyDoc(file, displayTitle)
            else -> error("অসমর্থিত ফাইল ফরম্যাট। অনুগ্রহ করে .doc বা .docx ফাইল নির্বাচন করুন।")
        }
    }

    private fun parseDocx(file: File, displayTitle: String): ParsedWordDocument {
        ZipFile(file).use { zip ->
            val docEntry = zip.getEntry("word/document.xml")
                ?: error("এই DOCX ফাইলটি বৈধ Word ডকুমেন্ট নয় (word/document.xml অনুপস্থিত)।")

            // Map relationship IDs (rId) -> embedded media zip paths
            val relsMap = mutableMapOf<String, String>()
            val relsEntry = zip.getEntry("word/_rels/document.xml.rels")
            if (relsEntry != null) {
                zip.getInputStream(relsEntry).use { stream ->
                    val parser = Xml.newPullParser()
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    parser.setInput(stream, "UTF-8")
                    var evt = parser.eventType
                    while (evt != XmlPullParser.END_DOCUMENT) {
                        if (evt == XmlPullParser.START_TAG && localName(parser.name) == "Relationship") {
                            val id = parser.getAttributeValue(null, "Id")
                            val target = parser.getAttributeValue(null, "Target")
                            if (!id.isNullOrBlank() && !target.isNullOrBlank()) {
                                val resolved = when {
                                    target.startsWith("/word/") -> target.removePrefix("/")
                                    target.startsWith("word/") -> target
                                    else -> "word/${target.removePrefix("./")}"
                                }
                                relsMap[id] = resolved
                            }
                        }
                        evt = parser.next()
                    }
                }
            }

            val blocks = mutableListOf<WordBlock>()
            val warnings = mutableListOf<String>()

            zip.getInputStream(docEntry).use { stream ->
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(stream, "UTF-8")

                var inBody = false
                var inParagraph = false
                var inRun = false
                var inRunProps = false
                var inParaProps = false
                var inTable = false
                var inTableRow = false
                var inTableCell = false

                val paraText = StringBuilder()
                var paraBold = false
                var paraItalic = false
                var paraHeading = false
                var paraHeadingLevel = 0
                var paraAlign = WordAlign.LEFT
                val paraImages = mutableListOf<WordBlock.ImageBlock>()

                var runBold = false
                var runItalic = false
                var hasAnyBoldRun = false
                var hasAnyItalicRun = false

                val currentTableRows = mutableListOf<MutableList<String>>()
                var currentRow = mutableListOf<String>()
                val currentCellText = StringBuilder()

                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    val tag = localName(parser.name)
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            when (tag) {
                                "body" -> inBody = true
                                "tbl" -> if (inBody && !inTable) {
                                    inTable = true
                                    currentTableRows.clear()
                                }
                                "tr" -> if (inTable) {
                                    inTableRow = true
                                    currentRow = mutableListOf()
                                }
                                "tc" -> if (inTableRow) {
                                    inTableCell = true
                                    currentCellText.clear()
                                }
                                "p" -> if (inBody && !inParagraph) {
                                    inParagraph = true
                                    paraText.clear()
                                    paraBold = false
                                    paraItalic = false
                                    paraHeading = false
                                    paraHeadingLevel = 0
                                    paraAlign = WordAlign.LEFT
                                    paraImages.clear()
                                    hasAnyBoldRun = false
                                    hasAnyItalicRun = false
                                }
                                "pPr" -> if (inParagraph) inParaProps = true
                                "rPr" -> if (inRun || inParaProps) inRunProps = true
                                "pStyle" -> if (inParaProps) {
                                    val styleVal = attr(parser, "val")?.lowercase() ?: ""
                                    if (styleVal.startsWith("heading") || styleVal.contains("title") || styleVal == "subtitle") {
                                        paraHeading = true
                                        paraBold = true
                                        paraHeadingLevel = when {
                                            styleVal.contains("1") || styleVal == "title" -> 1
                                            styleVal.contains("2") || styleVal == "subtitle" -> 2
                                            else -> 3
                                        }
                                    }
                                }
                                "jc" -> if (inParaProps) {
                                    paraAlign = when (attr(parser, "val")?.lowercase()) {
                                        "center" -> WordAlign.CENTER
                                        "right", "end" -> WordAlign.RIGHT
                                        "both", "distribute" -> WordAlign.JUSTIFY
                                        else -> WordAlign.LEFT
                                    }
                                }
                                "r" -> if (inParagraph) {
                                    inRun = true
                                    runBold = false
                                    runItalic = false
                                }
                                "b" -> if (inRunProps) {
                                    val v = attr(parser, "val")?.lowercase()
                                    if (v != "false" && v != "0") {
                                        runBold = true
                                        hasAnyBoldRun = true
                                    }
                                }
                                "i" -> if (inRunProps) {
                                    val v = attr(parser, "val")?.lowercase()
                                    if (v != "false" && v != "0") {
                                        runItalic = true
                                        hasAnyItalicRun = true
                                    }
                                }
                                "tab" -> if (inParagraph) {
                                    if (inTableCell) currentCellText.append("  ")
                                    else paraText.append("    ")
                                }
                                "br" -> if (inParagraph) {
                                    val brType = attr(parser, "type")?.lowercase()
                                    if (brType == "page" && !inTableCell) {
                                        if (paraText.isNotBlank()) {
                                            blocks.add(
                                                WordBlock.ParagraphBlock(
                                                    text = paraText.toString().trimEnd(),
                                                    isHeading = paraHeading,
                                                    headingLevel = paraHeadingLevel,
                                                    isBold = paraBold || hasAnyBoldRun,
                                                    isItalic = paraItalic || hasAnyItalicRun,
                                                    alignment = paraAlign
                                                )
                                            )
                                            paraText.clear()
                                        }
                                        blocks.add(WordBlock.PageBreakBlock)
                                    } else {
                                        if (inTableCell) currentCellText.append('\n')
                                        else paraText.append('\n')
                                    }
                                }
                                "t" -> if (inParagraph) {
                                    val textContent = safeNextText(parser)
                                    if (inTableCell) {
                                        currentCellText.append(textContent)
                                    } else {
                                        paraText.append(textContent)
                                    }
                                }
                                "blip", "imagedata" -> if (inParagraph) {
                                    val rId = attr(parser, "embed") ?: attr(parser, "id")
                                    val mediaPath = rId?.let { relsMap[it] }
                                    if (mediaPath != null) {
                                        val entry = zip.getEntry(mediaPath)
                                        if (entry != null) {
                                            runCatching {
                                                zip.getInputStream(entry).use { imgStream ->
                                                    val bytes = imgStream.readBytes()
                                                    if (bytes.isNotEmpty()) {
                                                        paraImages.add(WordBlock.ImageBlock(bytes, mediaPath.substringAfterLast('/')))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        XmlPullParser.END_TAG -> {
                            when (tag) {
                                "body" -> inBody = false
                                "rPr" -> inRunProps = false
                                "pPr" -> inParaProps = false
                                "r" -> inRun = false
                                "p" -> if (inParagraph) {
                                    if (inTableCell) {
                                        if (currentCellText.isNotEmpty() && !currentCellText.endsWith("\n")) {
                                            currentCellText.append("\n")
                                        }
                                    } else {
                                        val txt = paraText.toString().trimEnd()
                                        if (txt.isNotEmpty()) {
                                            blocks.add(
                                                WordBlock.ParagraphBlock(
                                                    text = txt,
                                                    isHeading = paraHeading,
                                                    headingLevel = paraHeadingLevel,
                                                    isBold = paraBold || hasAnyBoldRun,
                                                    isItalic = paraItalic || hasAnyItalicRun,
                                                    alignment = paraAlign
                                                )
                                            )
                                        }
                                        paraImages.forEach { blocks.add(it) }
                                    }
                                    inParagraph = false
                                }
                                "tc" -> if (inTableCell) {
                                    currentRow.add(currentCellText.toString().trim())
                                    inTableCell = false
                                }
                                "tr" -> if (inTableRow) {
                                    if (currentRow.isNotEmpty()) currentTableRows.add(currentRow)
                                    inTableRow = false
                                }
                                "tbl" -> if (inTable) {
                                    if (currentTableRows.isNotEmpty()) {
                                        blocks.add(WordBlock.TableBlock(currentTableRows.map { it.toList() }))
                                    }
                                    inTable = false
                                }
                            }
                        }
                    }
                    event = parser.next()
                }
            }

            if (blocks.isEmpty()) {
                warnings.add("ডকুমেন্টে কোনো দৃশ্যমান লেখা বা ছবি পাওয়া যায়নি।")
            }

            return ParsedWordDocument(
                title = displayTitle,
                blocks = blocks,
                sourceFormat = "DOCX",
                warningsBn = warnings
            )
        }
    }

    /**
     * Pure-Kotlin OLE2 Compound File Binary + Word Binary (.doc) parser.
     * Supports reading WordDocument & Table streams, FIB, and CLX Piece Table (PlcPcd).
     */
    private fun parseLegacyDoc(file: File, displayTitle: String): ParsedWordDocument {
        val rawBytes = file.readBytes()
        if (rawBytes.size < 512) {
            error("ফাইলটি বৈধ .doc ডকুমেন্ট নয়।")
        }

        val cfb = Ole2Reader(rawBytes)
        val wordDocBytes = cfb.readStream("WordDocument")
            ?: error("এই .doc ফাইলের ভেতরে WordDocument ডেটা পাওয়া যায়নি।")

        val fibBuf = ByteBuffer.wrap(wordDocBytes).order(ByteOrder.LITTLE_ENDIAN)
        val wIdent = fibBuf.getShort(0).toInt() and 0xFFFF
        if (wIdent != 0xA5EC) {
            error("ফাইলটি সমর্থিত Word (.doc) ফরম্যাটে নেই।")
        }

        val flags = fibBuf.getShort(0x000A).toInt() and 0xFFFF
        val isEncrypted = (flags and 0x0100) != 0
        if (isEncrypted) {
            error("পাসওয়ার্ড-সুরক্ষিত .doc ফাইল প্রক্রিয়া করা যায় না।")
        }
        val whichTbl = (flags and 0x0200) != 0
        val tableStreamName = if (whichTbl) "1Table" else "0Table"
        val tableBytes = cfb.readStream(tableStreamName)

        // Attempt CLX PieceTable extraction for clean Unicode/CP1252 text
        var extractedText = if (tableBytes != null) {
            extractDocPieceTableText(wordDocBytes, tableBytes)
        } else {
            null
        }

        val warnings = mutableListOf(
            "পুরোনো .doc ফরম্যাটে জটিল ফরম্যাটিং বা ছবি আংশিকভাবে সরলীকৃত হতে পারে; সর্বাধিক নিখুঁত ফলাফলের জন্য .docx ব্যবহার করুন।"
        )

        if (extractedText.isNullOrBlank()) {
            // Best-effort text recovery from WordDocument stream if CLX is non-standard
            val ccpText = if (wordDocBytes.size > 0x0050) {
                ByteBuffer.wrap(wordDocBytes).order(ByteOrder.LITTLE_ENDIAN).getInt(0x004C)
            } else 0
            val fcMin = if (wordDocBytes.size > 0x001C) {
                ByteBuffer.wrap(wordDocBytes).order(ByteOrder.LITTLE_ENDIAN).getInt(0x0018)
            } else 0
            if (ccpText > 0 && fcMin > 0 && fcMin < wordDocBytes.size) {
                val byteCount = min(ccpText * 2, wordDocBytes.size - fcMin)
                val utf16Candidate = String(wordDocBytes, fcMin, byteCount, Charsets.UTF_16LE)
                val printableRatio = utf16Candidate.count { it.code >= 0x20 || it == '\r' || it == '\n' }.toFloat() /
                    utf16Candidate.length.coerceAtLeast(1)
                extractedText = if (printableRatio > 0.72f) {
                    utf16Candidate
                } else {
                    val latinLen = min(ccpText, wordDocBytes.size - fcMin)
                    String(wordDocBytes, fcMin, latinLen, Charset.forName("windows-1252"))
                }
            }
        }

        val cleanedText = (extractedText ?: "")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace("\u000B", "\n")
            .replace("\u000C", "\n<<PAGE_BREAK>>\n")

        val blocks = mutableListOf<WordBlock>()
        val rawLines = cleanedText.split('\n')
        for (line in rawLines) {
            if (line == "<<PAGE_BREAK>>") {
                blocks.add(WordBlock.PageBreakBlock)
                continue
            }
            if (line.contains('\u0007')) {
                val cells = line.split('\u0007')
                    .map { sanitizeControlChars(it).trim() }
                    .filter { it.isNotEmpty() }
                if (cells.isNotEmpty()) {
                    val last = blocks.lastOrNull()
                    if (last is WordBlock.TableBlock) {
                        blocks[blocks.lastIndex] = WordBlock.TableBlock(last.rows + listOf(cells))
                    } else {
                        blocks.add(WordBlock.TableBlock(listOf(cells)))
                    }
                }
            } else {
                val cleanLine = sanitizeControlChars(line).trim()
                if (cleanLine.isNotEmpty()) {
                    blocks.add(WordBlock.ParagraphBlock(text = cleanLine))
                }
            }
        }

        if (blocks.isEmpty()) {
            error("এই .doc ফাইল থেকে পাঠযোগ্য লেখা বের করা যায়নি।")
        }

        return ParsedWordDocument(
            title = displayTitle,
            blocks = blocks,
            sourceFormat = "DOC",
            warningsBn = warnings
        )
    }

    private fun extractDocPieceTableText(wordDocBytes: ByteArray, tableBytes: ByteArray): String? {
        return try {
            val fib = ByteBuffer.wrap(wordDocBytes).order(ByteOrder.LITTLE_ENDIAN)
            // In Word97+ FIB, FibRgFcLcb starts after FibBase(32) + csw(2) + FibRgW(28) + cslw(2) + FibRgLw(88) + cbRgFcLcb(2) = offset 154
            // fcClx is at index 33 in FibRgFcLcb97 => offset 154 + 33 * 8 = 418 (0x01A2)
            if (wordDocBytes.size < 426) return null
            val fcClx = fib.getInt(0x01A2)
            val lcbClx = fib.getInt(0x01A6)
            if (fcClx < 0 || lcbClx <= 0 || fcClx + lcbClx > tableBytes.size) return null

            var pos = fcClx
            val clxEnd = fcClx + lcbClx
            while (pos < clxEnd) {
                val clxt = tableBytes[pos].toInt() and 0xFF
                if (clxt == 1) {
                    // Grpprl: 1 byte type + 2 byte size
                    if (pos + 3 > clxEnd) break
                    val cbGrpprl = ByteBuffer.wrap(tableBytes, pos + 1, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
                    pos += 3 + cbGrpprl
                } else if (clxt == 2) {
                    // Pcdt: 1 byte type + 4 byte lcbPlcPcd
                    if (pos + 5 > clxEnd) break
                    val lcbPlcPcd = ByteBuffer.wrap(tableBytes, pos + 1, 4).order(ByteOrder.LITTLE_ENDIAN).int
                    val plcStart = pos + 5
                    if (lcbPlcPcd < 12 || plcStart + lcbPlcPcd > tableBytes.size) break

                    // PlcPcd has (n + 1) CPs (4 bytes each) followed by n PCDs (8 bytes each)
                    // 4*(n+1) + 8*n = 12*n + 4 = lcbPlcPcd => n = (lcbPlcPcd - 4) / 12
                    val n = (lcbPlcPcd - 4) / 12
                    if (n <= 0) break

                    val ccpText = fib.getInt(0x004C)
                    val plcBuf = ByteBuffer.wrap(tableBytes, plcStart, lcbPlcPcd).order(ByteOrder.LITTLE_ENDIAN)
                    val cps = IntArray(n + 1) { idx -> plcBuf.getInt(idx * 4) }

                    val sb = StringBuilder()
                    val cpLimit = if (ccpText > 0) ccpText else cps[n]
                    for (i in 0 until n) {
                        val cpStart = cps[i]
                        val cpEnd = cps[i + 1]
                        if (cpStart >= cpLimit) break
                        val effectiveEnd = min(cpEnd, cpLimit)
                        val charCount = effectiveEnd - cpStart
                        if (charCount <= 0) continue

                        val pcdOffset = (n + 1) * 4 + i * 8
                        val fcCompressed = plcBuf.getInt(pcdOffset + 2)
                        val isAnsiCompressed = (fcCompressed and 0x40000000) != 0
                        val rawFc = fcCompressed and 0x3FFFFFFF

                        if (isAnsiCompressed) {
                            val byteStart = rawFc / 2
                            if (byteStart in wordDocBytes.indices) {
                                val available = min(charCount, wordDocBytes.size - byteStart)
                                sb.append(String(wordDocBytes, byteStart, available, Charset.forName("windows-1252")))
                            }
                        } else {
                            if (rawFc in wordDocBytes.indices) {
                                val byteLen = min(charCount * 2, wordDocBytes.size - rawFc)
                                sb.append(String(wordDocBytes, rawFc, byteLen, Charsets.UTF_16LE))
                            }
                        }
                    }
                    return sb.toString()
                } else {
                    break
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun sanitizeControlChars(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when {
                ch == '\t' -> sb.append("    ")
                ch.code < 0x20 -> { /* strip OLE field markers like 0x01, 0x13, 0x14, 0x15 */ }
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun localName(raw: String?): String {
        if (raw == null) return ""
        val colon = raw.indexOf(':')
        return if (colon >= 0) raw.substring(colon + 1) else raw
    }

    private fun attr(parser: XmlPullParser, localAttr: String): String? {
        for (i in 0 until parser.attributeCount) {
            if (localName(parser.getAttributeName(i)) == localAttr) {
                return parser.getAttributeValue(i)
            }
        }
        return null
    }

    private fun safeNextText(parser: XmlPullParser): String {
        return try {
            parser.nextText()
        } catch (_: Exception) {
            ""
        }
    }
}

/**
 * Minimal pure-Kotlin OLE2 Compound File Binary (MS-CFB) reader for reading WordDocument & Table streams.
 */
private class Ole2Reader(private val data: ByteArray) {
    private val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    private val sectorSize: Int
    private val miniSectorSize: Int
    private val firstDirSector: Int
    private val miniStreamCutoff: Int
    private val firstMiniFatSector: Int
    private val fat: IntArray
    private val miniFat: IntArray
    private var rootMiniStream: ByteArray = ByteArray(0)
    private val entries = mutableListOf<DirEntry>()

    private data class DirEntry(
        val name: String,
        val type: Int,
        val startSector: Int,
        val size: Long
    )

    init {
        val sectorShift = buf.getShort(30).toInt() and 0xFFFF
        val miniShift = buf.getShort(32).toInt() and 0xFFFF
        sectorSize = 1 shl sectorShift
        miniSectorSize = 1 shl miniShift
        firstDirSector = buf.getInt(48)
        miniStreamCutoff = buf.getInt(56)
        firstMiniFatSector = buf.getInt(60)
        val difatCount = buf.getInt(72)
        val firstDifatSector = buf.getInt(68)

        // Collect FAT sector IDs (first 109 in header, then DIFAT chain)
        val fatSectors = mutableListOf<Int>()
        for (i in 0 until 109) {
            val s = buf.getInt(76 + i * 4)
            if (s >= 0) fatSectors.add(s)
        }
        var difatSec = firstDifatSector
        var difatSteps = 0
        while (difatSec >= 0 && difatSteps < difatCount + 2) {
            val off = sectorOffset(difatSec)
            if (off < 0 || off + sectorSize > data.size) break
            val entriesPerSec = (sectorSize / 4) - 1
            for (i in 0 until entriesPerSec) {
                val s = buf.getInt(off + i * 4)
                if (s >= 0) fatSectors.add(s)
            }
            difatSec = buf.getInt(off + entriesPerSec * 4)
            difatSteps++
        }

        val intsPerSector = sectorSize / 4
        val fatList = IntArray(fatSectors.size * intsPerSector) { -1 }
        fatSectors.forEachIndexed { secIdx, secId ->
            val off = sectorOffset(secId)
            if (off >= 0 && off + sectorSize <= data.size) {
                for (i in 0 until intsPerSector) {
                    fatList[secIdx * intsPerSector + i] = buf.getInt(off + i * 4)
                }
            }
        }
        fat = fatList

        // Read Directory Stream
        val dirBytes = readChain(firstDirSector, -1L)
        val dirCount = dirBytes.size / 128
        val dirBuf = ByteBuffer.wrap(dirBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until dirCount) {
            val base = i * 128
            val nameLen = (dirBuf.getShort(base + 64).toInt() and 0xFFFF).coerceIn(0, 64)
            val name = if (nameLen >= 2) {
                String(dirBytes, base, nameLen - 2, Charsets.UTF_16LE)
            } else ""
            val type = dirBytes[base + 66].toInt() and 0xFF
            val startSec = dirBuf.getInt(base + 116)
            val sizeLow = dirBuf.getInt(base + 120).toLong() and 0xFFFFFFFFL
            entries.add(DirEntry(name, type, startSec, sizeLow))
        }

        // Read MiniFAT & Root MiniStream
        val miniFatRaw = if (firstMiniFatSector >= 0) readChain(firstMiniFatSector, -1L) else ByteArray(0)
        val mBuf = ByteBuffer.wrap(miniFatRaw).order(ByteOrder.LITTLE_ENDIAN)
        miniFat = IntArray(miniFatRaw.size / 4) { idx -> mBuf.getInt(idx * 4) }

        val root = entries.firstOrNull { it.type == 5 }
        if (root != null && root.startSector >= 0 && root.size > 0) {
            rootMiniStream = readChain(root.startSector, root.size)
        }
    }

    fun readStream(name: String): ByteArray? {
        val entry = entries.firstOrNull { it.name == name && it.type == 2 } ?: return null
        if (entry.size <= 0 || entry.startSector < 0) return null
        return if (entry.size < miniStreamCutoff && rootMiniStream.isNotEmpty()) {
            readMiniChain(entry.startSector, entry.size)
        } else {
            readChain(entry.startSector, entry.size)
        }
    }

    private fun sectorOffset(sectorId: Int): Int {
        if (sectorId < 0) return -1
        return (sectorId + 1) * sectorSize
    }

    private fun readChain(startSector: Int, expectedSize: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        var cur = startSector
        var guard = 0
        while (cur >= 0 && cur < fat.size && guard < 100_000) {
            val off = sectorOffset(cur)
            if (off < 0 || off + sectorSize > data.size) break
            out.write(data, off, sectorSize)
            if (expectedSize > 0 && out.size() >= expectedSize) break
            cur = fat[cur]
            guard++
        }
        val bytes = out.toByteArray()
        return if (expectedSize > 0 && bytes.size > expectedSize) {
            bytes.copyOf(expectedSize.toInt())
        } else bytes
    }

    private fun readMiniChain(startMiniSector: Int, expectedSize: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        var cur = startMiniSector
        var guard = 0
        while (cur >= 0 && cur < miniFat.size && guard < 100_000) {
            val off = cur * miniSectorSize
            if (off < 0 || off + miniSectorSize > rootMiniStream.size) break
            out.write(rootMiniStream, off, miniSectorSize)
            if (expectedSize > 0 && out.size() >= expectedSize) break
            cur = miniFat[cur]
            guard++
        }
        val bytes = out.toByteArray()
        return if (expectedSize > 0 && bytes.size > expectedSize) {
            bytes.copyOf(expectedSize.toInt())
        } else bytes
    }
}
