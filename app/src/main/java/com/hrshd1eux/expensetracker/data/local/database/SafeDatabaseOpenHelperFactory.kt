package com.hrshd1eux.expensetracker.data.local.database

import android.content.Context
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * A resilient [SupportSQLiteOpenHelper.Factory] wrapping SQLCipher's [SupportOpenHelperFactory].
 *
 * If a database file on disk cannot be decrypted (e.g. because of an OS-level Keystore reset,
 * migration from an unencrypted legacy database, or file corruption causing SQLCipher code 26
 * "file is not a database" / "hmac check failed"), this factory automatically catches the fatal
 * error, purges the un-decryptable database files, and creates a pristine, encrypted database
 * using the valid passphrase. This guarantees the application never enters an unrecoverable crash loop.
 */
class SafeDatabaseOpenHelperFactory(
    private val passphrase: ByteArray
) : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val delegateFactory = SupportOpenHelperFactory(passphrase)
        val delegateHelper = delegateFactory.create(configuration)
        return SafeSupportSQLiteOpenHelper(
            context = configuration.context,
            databaseName = configuration.name,
            initialDelegate = delegateHelper,
            factory = delegateFactory,
            configuration = configuration
        )
    }

    private class SafeSupportSQLiteOpenHelper(
        private val context: Context,
        override val databaseName: String?,
        initialDelegate: SupportSQLiteOpenHelper,
        private val factory: SupportOpenHelperFactory,
        private val configuration: SupportSQLiteOpenHelper.Configuration
    ) : SupportSQLiteOpenHelper {

        @Volatile
        private var delegate: SupportSQLiteOpenHelper = initialDelegate

        override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
            delegate.setWriteAheadLoggingEnabled(enabled)
        }

        override val writableDatabase: SupportSQLiteDatabase
            get() = synchronized(this) {
                try {
                    delegate.writableDatabase
                } catch (e: Throwable) {
                    if (isDecryptionOrCorruptionError(e)) {
                        Log.e("SafeDatabaseHelper", "Database decryption or integrity check failed (code 26 / HMAC error). Self-healing database...", e)
                        recoverDatabase()
                        delegate.writableDatabase
                    } else {
                        throw e
                    }
                }
            }

        override val readableDatabase: SupportSQLiteDatabase
            get() = synchronized(this) {
                try {
                    delegate.readableDatabase
                } catch (e: Throwable) {
                    if (isDecryptionOrCorruptionError(e)) {
                        Log.e("SafeDatabaseHelper", "Database decryption or integrity check failed (code 26 / HMAC error). Self-healing database...", e)
                        recoverDatabase()
                        delegate.readableDatabase
                    } else {
                        throw e
                    }
                }
            }

        override fun close() {
            synchronized(this) {
                delegate.close()
            }
        }

        private fun isDecryptionOrCorruptionError(e: Throwable): Boolean {
            var curr: Throwable? = e
            while (curr != null) {
                val msg = curr.message?.lowercase() ?: ""
                if (msg.contains("file is not a database") ||
                    msg.contains("code 26") ||
                    msg.contains("hmac check failed") ||
                    msg.contains("error decrypting page") ||
                    msg.contains("corrupt") ||
                    (curr is android.database.sqlite.SQLiteException && msg.contains("sqlite_schema"))
                ) {
                    return true
                }
                curr = curr.cause
            }
            return false
        }

        private fun recoverDatabase() {
            try {
                delegate.close()
            } catch (_: Exception) {}

            if (!databaseName.isNullOrBlank()) {
                try {
                    val dbFile = context.getDatabasePath(databaseName)
                    if (dbFile != null && dbFile.exists()) {
                        val backupFile = java.io.File(context.filesDir, "${databaseName}_corrupted_${System.currentTimeMillis()}.bak")
                        dbFile.copyTo(backupFile, overwrite = true)
                        Log.w("SafeDatabaseHelper", "Preserved un-decryptable database copy to: ${backupFile.absolutePath}")
                    }
                    context.deleteDatabase(databaseName)
                    Log.i("SafeDatabaseHelper", "Purged un-decryptable database: $databaseName")
                } catch (delEx: Exception) {
                    Log.e("SafeDatabaseHelper", "Failed during database backup/deletion", delEx)
                }
            }

            // Create a fresh delegate bound to the now-clean database path
            delegate = factory.create(configuration)
        }
    }
}
