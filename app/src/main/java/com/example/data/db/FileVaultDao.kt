package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.EncryptedFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileVaultDao {
    @Query("SELECT * FROM encrypted_files ORDER BY createdAt DESC")
    fun getAllFiles(): Flow<List<EncryptedFileEntity>>

    @Query("SELECT * FROM encrypted_files WHERE id = :id")
    suspend fun getFileById(id: String): EncryptedFileEntity?

    @Query("SELECT * FROM encrypted_files WHERE shareToken = :shareToken")
    suspend fun getFileByShareToken(shareToken: String): EncryptedFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: EncryptedFileEntity)

    @Update
    suspend fun updateFile(file: EncryptedFileEntity)

    @Query("DELETE FROM encrypted_files WHERE id = :id")
    suspend fun deleteFileById(id: String)

    @Query("UPDATE encrypted_files SET status = :status WHERE id = :id")
    suspend fun updateFileStatus(id: String, status: String)

    @Query("UPDATE encrypted_files SET downloadCount = downloadCount + 1 WHERE id = :id")
    suspend fun incrementDownloadCount(id: String)
}
