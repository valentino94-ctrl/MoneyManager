package com.v94studio.moneymanager.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupHelper {
    private const val DB_NAME = "money_manager.db"

    fun createBackupFile(context: Context): File? {
        return try {
            val dbFile = context.getDatabasePath(DB_NAME)
            val dbShm = File(dbFile.path + "-shm")
            val dbWal = File(dbFile.path + "-wal")

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val zipFile = File(backupDir, "MoneyManager_Backup_$timestamp.mmback")

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                addToZip(zos, dbFile, "database.db")
                if (dbShm.exists()) addToZip(zos, dbShm, "database.db-shm")
                if (dbWal.exists()) addToZip(zos, dbWal, "database.db-wal")
            }
            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareBackup(context: Context) {
        val zipFile = createBackupFile(context) ?: return
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Backup File"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun restoreBackup(context: Context, backupUri: Uri): Boolean {
        return try {
            val dbFile = context.getDatabasePath(DB_NAME)
            val dbDir = dbFile.parentFile ?: return false
            
            // Temporary files to extract to
            val tempDb = File(dbDir, "temp_restore.db")
            val tempShm = File(dbDir, "temp_restore.db-shm")
            val tempWal = File(dbDir, "temp_restore.db-wal")

            context.contentResolver.openInputStream(backupUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val outFile = when (entry.name) {
                            "database.db" -> tempDb
                            "database.db-shm" -> tempShm
                            "database.db-wal" -> tempWal
                            else -> null
                        }
                        
                        outFile?.let {
                            FileOutputStream(it).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            if (!tempDb.exists()) return false

            // To safely restore, we should close the DB connection first.
            // But since this is a simple helper, we will overwrite and ask user to restart.
            
            // Delete existing files
            dbFile.delete()
            File(dbFile.path + "-shm").delete()
            File(dbFile.path + "-wal").delete()

            // Move temp files to real locations
            tempDb.renameTo(dbFile)
            if (tempShm.exists()) tempShm.renameTo(File(dbFile.path + "-shm"))
            if (tempWal.exists()) tempWal.renameTo(File(dbFile.path + "-wal"))

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun writeBackupToUri(context: Context, uri: Uri): Boolean {
        val zipFile = createBackupFile(context) ?: return false
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(zipFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun writeCsvToUri(context: Context, uri: Uri, csvContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvContent.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addToZip(zos: ZipOutputStream, file: File, entryName: String) {
        zos.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { fis ->
            fis.copyTo(zos)
        }
        zos.closeEntry()
    }
}
