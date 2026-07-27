package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.EncryptedFileEntity
import com.example.data.model.UserKeyEntity
import com.example.data.model.UserPermissionEntity

@Database(
    entities = [
        EncryptedFileEntity::class,
        UserPermissionEntity::class,
        AuditLogEntity::class,
        UserKeyEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileVaultDao(): FileVaultDao
    abstract fun userPermissionDao(): UserPermissionDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun userKeyDao(): UserKeyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "secureshare_vault.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
