package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY createdAt ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("UPDATE folders SET name = :newName WHERE id = :id")
    suspend fun renameFolder(id: Long, newName: String)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: Long)
}
