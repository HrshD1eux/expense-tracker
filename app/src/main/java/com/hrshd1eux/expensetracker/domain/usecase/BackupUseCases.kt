package com.hrshd1eux.expensetracker.domain.usecase

import com.hrshd1eux.expensetracker.data.preferences.UserPreferences
import com.hrshd1eux.expensetracker.domain.model.Category
import com.hrshd1eux.expensetracker.domain.model.DefaultCategories
import com.hrshd1eux.expensetracker.domain.model.Expense
import com.hrshd1eux.expensetracker.domain.model.PaymentMethod
import com.hrshd1eux.expensetracker.domain.repository.CategoryRepository
import com.hrshd1eux.expensetracker.domain.repository.ExpenseRepository
import com.hrshd1eux.expensetracker.util.CurrencyFormatter
import com.hrshd1eux.expensetracker.util.DateTimeUtils
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class ExportBackupUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferences: UserPreferences
) {
    suspend operator fun invoke(password: String? = null): Result<String> {
        return try {
            val currency = userPreferences.currencyCodeFlow.first()
            val categories = categoryRepository.getAllCategories().first()
            val expenses = expenseRepository.getAllExpensesSync()

            val rootJson = JSONObject().apply {
                put("backupVersion", 1)
                put("createdAt", DateTimeUtils.toIsoString(System.currentTimeMillis()))
                put("currency", currency)

                val categoriesArray = JSONArray()
                categories.forEach { cat ->
                    val catObj = JSONObject().apply {
                        put("id", cat.id)
                        put("name", cat.name)
                        put("icon", cat.icon)
                        put("color", cat.color)
                        put("isDefault", cat.isDefault)
                        put("isArchived", cat.isArchived)
                        put("createdAt", cat.createdAt)
                    }
                    categoriesArray.put(catObj)
                }
                put("categories", categoriesArray)

                val expensesArray = JSONArray()
                expenses.forEach { exp ->
                    val expObj = JSONObject().apply {
                        put("id", exp.id)
                        put("amountPaise", exp.amountPaise)
                        put("categoryId", exp.categoryId)
                        put("paymentMethod", exp.paymentMethod.name)
                        put("note", exp.note)
                        put("timestamp", exp.timestamp)
                        put("createdAt", exp.createdAt)
                        put("updatedAt", exp.updatedAt)
                    }
                    expensesArray.put(expObj)
                }
                put("expenses", expensesArray)
            }

            val plainJson = rootJson.toString(2)

            if (!password.isNullOrBlank()) {
                val encrypted = encryptWithPassword(plainJson, password)
                Result.success(encrypted)
            } else {
                Result.success(plainJson)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun encryptWithPassword(content: String, password: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(content.toByteArray(Charsets.UTF_8))

        val envelope = JSONObject().apply {
            put("type", "encrypted_backup")
            put("backupVersion", 1)
            put("salt", Base64.getEncoder().encodeToString(salt))
            put("iv", Base64.getEncoder().encodeToString(iv))
            put("ciphertext", Base64.getEncoder().encodeToString(ciphertext))
        }
        return envelope.toString(2)
    }
}

class ImportBackupUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferences: UserPreferences
) {
    sealed class ImportResult {
        data class Success(val categoryCount: Int, val expenseCount: Int) : ImportResult()
        data class Error(val message: String) : ImportResult()
        object PasswordRequired : ImportResult()
    }

    companion object {
        const val SUPPORTED_VERSION = 1
        private const val MAX_PLAUSIBLE_AMOUNT_PAISE = 100_000_000_000L // 100 crore rupees / 1 billion units
    }

    suspend operator fun invoke(content: String, password: String? = null): ImportResult {
        return try {
            val trimmed = content.trim()
            if (trimmed.isEmpty()) {
                return ImportResult.Error("The backup file is empty.")
            }

            val rootJson = try {
                JSONObject(trimmed)
            } catch (e: Exception) {
                return ImportResult.Error("The selected file is not a valid JSON backup.")
            }

            // Check if encrypted
            val effectiveJson = if (rootJson.optString("type") == "encrypted_backup") {
                if (password.isNullOrBlank()) {
                    return ImportResult.PasswordRequired
                }
                try {
                    val decrypted = decryptWithPassword(rootJson, password)
                    JSONObject(decrypted)
                } catch (e: Exception) {
                    return ImportResult.Error("Incorrect password or corrupted backup file.")
                }
            } else {
                rootJson
            }

            // 1. Validate version
            if (!effectiveJson.has("backupVersion")) {
                return ImportResult.Error("Invalid backup file: missing 'backupVersion'.")
            }
            val version = effectiveJson.optInt("backupVersion", -1)
            if (version < 1) {
                return ImportResult.Error("Invalid backup version: $version.")
            }
            if (version > SUPPORTED_VERSION) {
                return ImportResult.Error("Unsupported backup version ($version). Please update the app to restore this backup.")
            }

            // 2. Validate required root fields
            if (!effectiveJson.has("categories")) {
                return ImportResult.Error("Invalid backup file: missing 'categories' section.")
            }
            if (!effectiveJson.has("expenses")) {
                return ImportResult.Error("Invalid backup file: missing 'expenses' section.")
            }

            val categoriesJson = effectiveJson.optJSONArray("categories")
                ?: return ImportResult.Error("Malformed categories data: expected a JSON array.")
            val expensesJson = effectiveJson.optJSONArray("expenses")
                ?: return ImportResult.Error("Malformed expenses data: expected a JSON array.")

            // 3. Optional currency restore
            val currencyCode = effectiveJson.optString("currency", "").trim()
            if (currencyCode.isNotEmpty()) {
                val symbol = when (currencyCode.uppercase()) {
                    "INR" -> "₹"
                    "USD" -> "$"
                    "EUR" -> "€"
                    "GBP" -> "£"
                    "JPY" -> "¥"
                    "CAD" -> "$"
                    "AUD" -> "$"
                    else -> "₹"
                }
                try {
                    userPreferences.setCurrency(currencyCode.uppercase(), symbol)
                } catch (e: Exception) {
                    // Non-critical, continue restore
                }
            }

            // 4. Parse & validate categories
            val categoriesList = mutableListOf<Category>()
            val restoredCategoryIds = mutableSetOf<String>()

            for (i in 0 until categoriesJson.length()) {
                val catObj = categoriesJson.optJSONObject(i) ?: continue
                val id = catObj.optString("id", "").trim()
                val name = catObj.optString("name", "").trim()
                if (id.isEmpty() || name.isEmpty()) {
                    continue // Skip corrupted category safely
                }

                val icon = catObj.optString("icon", "category").trim().ifEmpty { "category" }
                val color = catObj.optLong("color", 0xFF00897B)
                val isDefault = catObj.optBoolean("isDefault", false)
                val isArchived = catObj.optBoolean("isArchived", false)
                val createdAt = catObj.optLong("createdAt", System.currentTimeMillis())
                val validCreatedAt = if (createdAt <= 0L) System.currentTimeMillis() else createdAt

                categoriesList.add(
                    Category(
                        id = id,
                        name = name,
                        icon = icon,
                        color = color,
                        isDefault = isDefault,
                        isArchived = isArchived,
                        createdAt = validCreatedAt
                    )
                )
                restoredCategoryIds.add(id)
            }

            // Collect existing categories to ensure category data integrity for expenses
            val existingCategoryIds = try {
                categoryRepository.getAllCategories().first().map { it.id }.toSet()
            } catch (e: Exception) {
                emptySet()
            }

            val allKnownCategoryIds = (existingCategoryIds + restoredCategoryIds).toMutableSet()
            // Ensure default "cat_other" is always available as fallback
            allKnownCategoryIds.add("cat_other")

            // 5. Parse & validate expenses
            val expensesList = mutableListOf<Expense>()
            val now = System.currentTimeMillis()

            for (i in 0 until expensesJson.length()) {
                val expObj = expensesJson.optJSONObject(i) ?: continue

                // ID validation
                val id = expObj.optString("id", "").trim()
                val validId = if (id.isNotEmpty()) id else UUID.randomUUID().toString()

                // Amount validation: strictly > 0 and <= MAX_PLAUSIBLE_AMOUNT_PAISE
                val amount = expObj.optLong("amountPaise", 0L)
                if (amount <= 0L || amount > MAX_PLAUSIBLE_AMOUNT_PAISE) {
                    continue // Skip corrupted or invalid amount
                }

                // Category ID integrity
                var catId = expObj.optString("categoryId", "").trim()
                if (catId.isEmpty() || !allKnownCategoryIds.contains(catId)) {
                    // Fallback to "cat_other" or first available category to avoid losing the transaction
                    catId = if (allKnownCategoryIds.contains("cat_other")) {
                        "cat_other"
                    } else {
                        allKnownCategoryIds.firstOrNull() ?: "cat_other"
                    }
                }

                // Payment method validation
                val paymentStr = expObj.optString("paymentMethod", "UPI").trim()
                val payment = try {
                    PaymentMethod.valueOf(paymentStr.uppercase())
                } catch (e: Exception) {
                    PaymentMethod.UPI // Fallback safely to UPI
                }

                // Dates validation
                val timestamp = expObj.optLong("timestamp", now)
                val validTimestamp = if (timestamp <= 0L) now else timestamp

                val createdAt = expObj.optLong("createdAt", validTimestamp)
                val validCreatedAt = if (createdAt <= 0L) validTimestamp else createdAt

                val updatedAt = expObj.optLong("updatedAt", validCreatedAt)
                val validUpdatedAt = if (updatedAt <= 0L) validCreatedAt else updatedAt

                val note = expObj.optString("note", "")

                expensesList.add(
                    Expense(
                        id = validId,
                        amountPaise = amount,
                        categoryId = catId,
                        paymentMethod = payment,
                        note = note,
                        timestamp = validTimestamp,
                        createdAt = validCreatedAt,
                        updatedAt = validUpdatedAt
                    )
                )
            }

            // 6. Insert validated data
            categoriesList.forEach { category ->
                categoryRepository.addCategory(category)
            }

            if (expensesList.isNotEmpty()) {
                expenseRepository.restoreBackupExpenses(expensesList)
            }

            ImportResult.Success(
                categoryCount = categoriesList.size,
                expenseCount = expensesList.size
            )
        } catch (e: Exception) {
            ImportResult.Error("Failed to restore backup: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun decryptWithPassword(envelope: JSONObject, password: String): String {
        val saltBase64 = envelope.getString("salt")
        val ivBase64 = envelope.getString("iv")
        val ciphertextBase64 = envelope.getString("ciphertext")

        val salt = Base64.getDecoder().decode(saltBase64)
        val iv = Base64.getDecoder().decode(ivBase64)
        val ciphertext = Base64.getDecoder().decode(ciphertextBase64)

        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val plaintextBytes = cipher.doFinal(ciphertext)
        return String(plaintextBytes, Charsets.UTF_8)
    }
}
