package com.hrshd1eux.expensetracker.domain.repository

import com.hrshd1eux.expensetracker.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getActiveCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: String): Category?
    fun getCategoryByIdFlow(id: String): Flow<Category?>
    suspend fun addCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun archiveCategory(id: String, isArchived: Boolean)
    suspend fun deleteCategory(id: String): Boolean
    suspend fun prepopulateDefaultCategories()
}
