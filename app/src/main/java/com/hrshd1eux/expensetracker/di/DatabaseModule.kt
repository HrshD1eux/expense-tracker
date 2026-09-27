package com.hrshd1eux.expensetracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hrshd1eux.expensetracker.data.local.dao.CategoryDao
import com.hrshd1eux.expensetracker.data.local.dao.ExpenseDao
import com.hrshd1eux.expensetracker.data.local.dao.RecurringExpenseDao
import com.hrshd1eux.expensetracker.data.local.database.AppDatabase
import com.hrshd1eux.expensetracker.data.repository.CategoryRepositoryImpl
import com.hrshd1eux.expensetracker.data.repository.ExpenseRepositoryImpl
import com.hrshd1eux.expensetracker.data.local.entity.toEntity
import com.hrshd1eux.expensetracker.domain.model.DefaultCategories
import com.hrshd1eux.expensetracker.domain.repository.CategoryRepository
import com.hrshd1eux.expensetracker.domain.repository.ExpenseRepository
import com.hrshd1eux.expensetracker.security.KeystoreManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferences(impl: com.hrshd1eux.expensetracker.data.preferences.PreferenceDataStore): com.hrshd1eux.expensetracker.data.preferences.UserPreferences
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN isReimbursable INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recurring_expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                amountPaise INTEGER NOT NULL,
                categoryId TEXT NOT NULL,
                paymentMethod TEXT NOT NULL,
                frequency TEXT NOT NULL,
                nextDueDateEpochMillis INTEGER NOT NULL,
                isActive INTEGER NOT NULL,
                autoAdd INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        keystoreManager: KeystoreManager,
        categoryDaoProvider: Provider<CategoryDao>
    ): AppDatabase {
        // Retrieve (or generate) the database passphrase from Keystore-encrypted prefs.
        // The passphrase is a random 32-byte value whose encrypted form lives in private
        // SharedPreferences. The raw bytes are used only to open the database, then wiped.
        val prefs = context.getSharedPreferences("db_sec", Context.MODE_PRIVATE)
        val passphrase: ByteArray = keystoreManager.getOrCreateDatabasePassphrase(prefs)

        // SafeDatabaseOpenHelperFactory uses modern SQLCipher with self-healing recovery against code 26 errors
        val factory = com.hrshd1eux.expensetracker.data.local.database.SafeDatabaseOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigrationOnDowngrade()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Pre-populate default categories on first database creation
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val defaults = DefaultCategories.getDefaultCategories()
                                .map { cat -> cat.toEntity() }
                            categoryDaoProvider.get().insertCategories(defaults)
                        } catch (e: Exception) {
                            // Safe fallback — categories will be empty on launch
                        }
                    }
                }
            })
            .build()
    }

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao {
        return database.expenseDao()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideRecurringExpenseDao(database: AppDatabase): RecurringExpenseDao {
        return database.recurringExpenseDao()
    }
}
