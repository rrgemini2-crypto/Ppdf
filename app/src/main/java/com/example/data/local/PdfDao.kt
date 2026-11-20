package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdf_documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents ORDER BY CASE WHEN lastOpenedAt > createdAt THEN lastOpenedAt ELSE createdAt END DESC LIMIT 40")
    fun getRecentDocuments(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteDocuments(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): PdfDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: PdfDocumentEntity): Long

    @Update
    suspend fun updateDocument(document: PdfDocumentEntity)

    @Query("UPDATE pdf_documents SET name = :newName WHERE id = :id")
    suspend fun renameDocument(id: Long, newName: String)

    @Query("UPDATE pdf_documents SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE pdf_documents SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun updateLastOpened(id: Long, timestamp: Long)

    @Query("UPDATE pdf_documents SET folderId = :folderId WHERE id = :id")
    suspend fun moveToFolder(id: Long, folderId: Long?)

    @Query("UPDATE pdf_documents SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolderReference(folderId: Long)

    @Query("DELETE FROM pdf_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)
}
