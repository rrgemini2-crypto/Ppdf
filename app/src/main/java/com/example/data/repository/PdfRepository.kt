package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.FolderEntity
import com.example.data.local.PdfDocumentEntity
import com.example.engine.GeneratedPdfInfo
import com.example.engine.PdfGenerator
import com.example.util.BengaliFormatters
import com.example.util.FileActionsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class PdfRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val pdfDao = db.pdfDao()
    private val folderDao = db.folderDao()

    val allDocuments: Flow<List<PdfDocumentEntity>> = pdfDao.getAllDocuments()
    val recentDocuments: Flow<List<PdfDocumentEntity>> = pdfDao.getRecentDocuments()
    val favoriteDocuments: Flow<List<PdfDocumentEntity>> = pdfDao.getFavoriteDocuments()
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()

    fun pdfsDir(): File = File(context.filesDir, "pdfs").apply { mkdirs() }
    fun thumbnailsDir(): File = File(context.filesDir, "thumbnails").apply { mkdirs() }
    fun workspaceDir(): File = File(context.filesDir, "workspace").apply { mkdirs() }

    fun createNewPdfTargetPair(): Pair<File, File> {
        val token = UUID.randomUUID().toString().take(12)
        val pdfFile = File(pdfsDir(), "pdf_$token.pdf")
        val thumbFile = File(thumbnailsDir(), "thumb_$token.png")
        return pdfFile to thumbFile
    }

    suspend fun getDocumentById(id: Long): PdfDocumentEntity? = pdfDao.getDocumentById(id)

    suspend fun registerPdfDocument(
        name: String,
        info: GeneratedPdfInfo,
        folderId: Long? = null,
        isCompressed: Boolean = false
    ): PdfDocumentEntity {
        val now = System.currentTimeMillis()
        val cleanName = BengaliFormatters.sanitizeFileName(name)
        val entity = PdfDocumentEntity(
            name = cleanName,
            filePath = info.pdfFile.absolutePath,
            thumbnailPath = info.thumbnailFile?.absolutePath,
            folderId = folderId,
            sizeBytes = info.sizeBytes,
            pageCount = info.pageCount,
            createdAt = now,
            lastOpenedAt = now,
            isFavorite = false,
            isCompressed = isCompressed
        )
        val id = pdfDao.insertDocument(entity)
        return entity.copy(id = id)
    }

    suspend fun registerCompressedPdf(
        name: String,
        outputPdfFile: File,
        pageCount: Int,
        folderId: Long? = null
    ): PdfDocumentEntity = withContext(Dispatchers.IO) {
        val token = UUID.randomUUID().toString().take(10)
        val thumbFile = File(thumbnailsDir(), "thumb_$token.png")
        val thumbOk = PdfGenerator.generatePdfThumbnail(outputPdfFile, thumbFile)
        val now = System.currentTimeMillis()
        val entity = PdfDocumentEntity(
            name = BengaliFormatters.sanitizeFileName(name),
            filePath = outputPdfFile.absolutePath,
            thumbnailPath = if (thumbOk) thumbFile.absolutePath else null,
            folderId = folderId,
            sizeBytes = outputPdfFile.length(),
            pageCount = pageCount,
            createdAt = now,
            lastOpenedAt = now,
            isFavorite = false,
            isCompressed = true
        )
        val id = pdfDao.insertDocument(entity)
        entity.copy(id = id)
    }

    suspend fun updateExistingPdfMetadata(
        docId: Long,
        newInfo: GeneratedPdfInfo
    ) {
        val existing = pdfDao.getDocumentById(docId) ?: return
        pdfDao.updateDocument(
            existing.copy(
                filePath = newInfo.pdfFile.absolutePath,
                thumbnailPath = newInfo.thumbnailFile?.absolutePath ?: existing.thumbnailPath,
                sizeBytes = newInfo.sizeBytes,
                pageCount = newInfo.pageCount,
                lastOpenedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun copyExternalPdfToTemp(uri: Uri): Result<Pair<File, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val displayName = FileActionsHelper.queryDisplayName(context, uri)
            val tempFile = File(workspaceDir(), "ext_${System.currentTimeMillis()}.pdf")
            val copied = FileActionsHelper.copyUriToFile(context, uri, tempFile)
            if (!copied) error("নির্বাচিত পিডিএফ ফাইলটি পড়া যায়নি।")
            val pageCount = PdfGenerator.inspectPdfPageCount(tempFile)
            if (pageCount <= 0) {
                tempFile.delete()
                error("ফাইলটি বৈধ পিডিএফ নয় অথবা ক্ষতিগ্রস্ত হয়েছে।")
            }
            tempFile to displayName
        }
    }

    suspend fun copyPickedImageToWorkspace(uri: Uri): File? = withContext(Dispatchers.IO) {
        val dest = File(workspaceDir(), "img_${UUID.randomUUID().toString().take(10)}.jpg")
        if (FileActionsHelper.copyUriToFile(context, uri, dest)) dest else null
    }

    suspend fun markDocumentOpened(id: Long) {
        pdfDao.updateLastOpened(id, System.currentTimeMillis())
    }

    suspend fun renameDocument(id: Long, newName: String) {
        val clean = BengaliFormatters.sanitizeFileName(newName)
        pdfDao.renameDocument(id, clean)
    }

    suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        pdfDao.setFavorite(id, isFavorite)
    }

    suspend fun moveToFolder(id: Long, folderId: Long?) {
        pdfDao.moveToFolder(id, folderId)
    }

    suspend fun deleteDocument(doc: PdfDocumentEntity) = withContext(Dispatchers.IO) {
        pdfDao.deleteDocumentById(doc.id)
        try {
            File(doc.filePath).delete()
            doc.thumbnailPath?.let { File(it).delete() }
        } catch (_: Exception) {
        }
    }

    suspend fun createFolder(name: String): Long {
        val clean = name.trim().ifEmpty { "নতুন ফোল্ডার" }
        return folderDao.insertFolder(FolderEntity(name = clean))
    }

    suspend fun deleteFolder(folderId: Long) {
        pdfDao.clearFolderReference(folderId)
        folderDao.deleteFolderById(folderId)
    }

    suspend fun clearWorkspaceTempFiles() = withContext(Dispatchers.IO) {
        try {
            workspaceDir().listFiles()?.forEach { it.deleteRecursively() }
        } catch (_: Exception) {
        }
    }
}
