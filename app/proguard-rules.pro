# Proguard rules for Expense Tracker

# SQLCipher
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**

# Android SQLite & Room
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Domain Models & Entities (persisted or JSON serialized)
-keep class com.hrshd1eux.expensetracker.domain.model.** { *; }
-keep class com.hrshd1eux.expensetracker.data.local.entity.** { *; }
-keep class com.hrshd1eux.expensetracker.data.local.dao.** { *; }
-keep class com.hrshd1eux.expensetracker.util.UpdateCheckResult** { *; }

# Security & Crypto
-keep class com.hrshd1eux.expensetracker.security.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
