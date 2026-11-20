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

        val favs = pdfDao.getFavoriteDocuments().first()
        assertEquals(1, favs.size)
        assertEquals("নতুন_চুক্তিপত্র", favs.first().name)
        assertEquals(folderId, favs.first().folderId)

        db.close()
    }
}
