package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.FolderEntity
import com.example.data.local.PdfDocumentEntity
import com.example.data.model.ImageFilterType
import com.example.data.model.NormPoint
import com.example.data.model.PageEditParams
import com.example.data.model.PageOrientation
import com.example.data.model.PdfBuildConfig
import com.example.data.model.PdfPageSize
import com.example.engine.DocumentCornerDetector
import com.example.engine.ImageFilterProcessor
import com.example.engine.PdfGenerator
import com.example.util.BengaliFormatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Bengali identity`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("সহজ পিডিএফ মেকার", appName)
    }

    @Test
    fun `bengali formatters convert digits and sizes accurately`() {
        assertEquals("১২৩৪৫", BengaliFormatters.toBengaliDigits(12345))
        assertEquals("৫১২ বাইট", BengaliFormatters.formatFileSize(512))
        assertEquals("১.০ কেবি", BengaliFormatters.formatFileSize(1024))
        assertEquals("১.০০ এমবি", BengaliFormatters.formatFileSize(1024L * 1024L))
        assertEquals("১ম পৃষ্ঠা", BengaliFormatters.formatOrdinalPage(1))
    }

    @Test
    fun `document corner detector identifies paper quad on dark background`() {
        val bmp = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(25, 25, 30))
        val paperPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(36f, 48f, 204f, 272f, paperPaint)

        val result = DocumentCornerDetector.detectFromBitmap(bmp)
        bmp.recycle()

        assertTrue("Expected document to be detected", result.isDetected)
        assertEquals(4, result.corners.size)
    }

    @Test
    fun `image filter processor applies all 10 filters and perspective warp without altering original`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imgFile = File(context.cacheDir, "test_doc_page.jpg")
        val bmp = Bitmap.createBitmap(400, 560, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        val p = Paint().apply { color = Color.BLUE; strokeWidth = 10f }
        canvas.drawLine(40f, 80f, 360f, 480f, p)
        FileOutputStream(imgFile).use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bmp.recycle()
        val origBytesBefore = imgFile.length()

        for (filter in ImageFilterType.entries) {
            val params = PageEditParams(
                filterType = filter,
                rotationDegrees = 90,
                brightness = 10f,
                contrast = 1.2f,
                usePerspectiveWarp = true,
                corners = listOf(
                    NormPoint(0.1f, 0.1f),
                    NormPoint(0.9f, 0.1f),
                    NormPoint(0.9f, 0.9f),
                    NormPoint(0.1f, 0.9f)
                )
            )
            val processed = ImageFilterProcessor.processPageBitmap(imgFile.absolutePath, params, 600)
            assertNotNull("Filter ${filter.name} returned null", processed)
            processed?.recycle()
        }

        assertEquals(origBytesBefore, imgFile.length())
    }

    @Test
    fun `pdf build config resolves dimensions and estimated size accurately`() {
        val portraitA4 = PdfBuildConfig(fileName = "টেস্ট", pageSize = PdfPageSize.A4)
        assertEquals(595 to 842, portraitA4.resolvePageDimensionsPt())

        val landscapeA4 = portraitA4.copy(orientation = PageOrientation.LANDSCAPE)
        assertEquals(842 to 595, landscapeA4.resolvePageDimensionsPt())

        val uncompressed = PdfGenerator.estimateSizeBytes(4, portraitA4)
        val compressed = PdfGenerator.estimateSizeBytes(4, portraitA4.copy(compressOutput = true))
        assertTrue(compressed < uncompressed)
    }

    @Test
    fun `room database persists documents folders and favorites`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val pdfDao = db.pdfDao()
        val folderDao = db.folderDao()

        val folderId = folderDao.insertFolder(FolderEntity(name = "অফিস"))
        val docId = pdfDao.insertDocument(
            PdfDocumentEntity(
                name = "চুক্তিপত্র",
                filePath = "/tmp/contract.pdf",
                thumbnailPath = null,
                folderId = folderId,
                sizeBytes = 204800L,
                pageCount = 3
            )
        )

        pdfDao.setFavorite(docId, true)
        pdfDao.renameDocument(docId, "নতুন_চুক্তিপত্র")
        folderDao.renameFolder(folderId, "জরুরি অফিস")

        val favs = pdfDao.getFavoriteDocuments().first()
        val folders = folderDao.getAllFolders().first()
        assertEquals(1, favs.size)
        assertEquals("নতুন_চুক্তিপত্র", favs.first().name)
        assertEquals("PDF", favs.first().fileType)
        assertEquals(folderId, favs.first().folderId)
        assertEquals("জরুরি অফিস", folders.first().name)

        db.close()
    }

    @Test
    fun `docx writer and word document parser round-trip Bengali text headings and tables`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val docxFile = File(context.cacheDir, "roundtrip_test.docx")
        val blocks = listOf(
            com.example.engine.WordBlock.ParagraphBlock(
                text = "সহজ পিডিএফ প্রতিবেদন",
                isHeading = true,
                headingLevel = 1,
                isBold = true,
                alignment = com.example.engine.WordAlign.CENTER
            ),
            com.example.engine.WordBlock.ParagraphBlock(
                text = "এটি একটি বাংলা এবং English পরীক্ষামূলক অনুচ্ছেদ।",
                isBold = false
            ),
            com.example.engine.WordBlock.TableBlock(
                rows = listOf(
                    listOf("নাম", "সংখ্যা"),
                    listOf("কলম", "১০")
                )
            )
        )

        com.example.engine.PdfToWordEngine.writeDocxPackage(docxFile, blocks)
        assertTrue(docxFile.exists() && docxFile.length() > 200L)

        val parsedResult = com.example.engine.WordDocumentParser.parseWordFile(docxFile, "প্রতিবেদন")
        assertTrue(parsedResult.isSuccess)
        val parsed = parsedResult.getOrThrow()
        assertEquals("DOCX", parsed.sourceFormat)
        assertEquals(3, parsed.blocks.size)

        val h = parsed.blocks[0] as com.example.engine.WordBlock.ParagraphBlock
        assertEquals("সহজ পিডিএফ প্রতিবেদন", h.text)
        assertTrue(h.isHeading)
        assertEquals(com.example.engine.WordAlign.CENTER, h.alignment)

        val p = parsed.blocks[1] as com.example.engine.WordBlock.ParagraphBlock
        assertTrue(p.text.contains("বাংলা"))
        assertTrue(p.text.contains("English"))

        val tbl = parsed.blocks[2] as com.example.engine.WordBlock.TableBlock
        assertEquals(2, tbl.rows.size)
        assertEquals("কলম", tbl.rows[1][0])
        assertEquals("১০", tbl.rows[1][1])
    }

    @Test
    fun `text to pdf preview paginates long Bengali content into multiple pages`() {
        val longBody = (1..80).joinToString("\n") {
            "লাইন ${BengaliFormatters.toBengaliDigits(it)}: সহজ পিডিএফ মেকার দিয়ে দ্রুত বাংলা ও ইংরেজি নথি তৈরি করা যায়।"
        }
        val pages = com.example.engine.TextToPdfEngine.paginateTextForPreview(
            title = "পরীক্ষামূলক শিরোনাম",
            bodyText = longBody,
            fontSizePt = 13f,
            pageSize = PdfPageSize.A4
        )
        assertTrue("Expected multiple pages", pages.size >= 2)
        assertTrue(pages.first().showTitle)
        assertEquals(false, pages[1].showTitle)
    }

    @Test
    fun `long image stitcher merges pages vertically into valid image`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val page1 = File(context.cacheDir, "stitch_p1.png")
        val page2 = File(context.cacheDir, "stitch_p2.png")
        listOf(page1 to Color.RED, page2 to Color.BLUE).forEach { (f, color) ->
            val bmp = Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(color)
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
        }

        val outFile = File(context.cacheDir, "stitched_out.jpg")
        val res = com.example.engine.LongImageStitcher.stitchPagesToLongImage(
            pageImageFiles = listOf(page1, page2),
            outputFile = outFile,
            format = com.example.util.ImageOutputFormat.JPG,
            quality = com.example.util.ImageExportQuality.HIGH
        )
        assertTrue(res.isSuccess)
        val info = res.getOrThrow()
        assertEquals(2, info.pagesStitched)
        assertTrue(info.heightPx > info.widthPx)
        assertTrue(outFile.exists() && outFile.length() > 0L)
    }
}
