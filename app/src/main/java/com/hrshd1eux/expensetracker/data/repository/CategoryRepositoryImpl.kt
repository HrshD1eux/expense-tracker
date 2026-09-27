package com.hrshd1eux.expensetracker.data.repository

import com.hrshd1eux.expensetracker.data.local.dao.CategoryDao
import com.hrshd1eux.expensetracker.data.local.dao.ExpenseDao
import com.hrshd1eux.expensetracker.data.local.entity.toDomain
import com.hrshd1eux.expensetracker.data.local.entity.toEntity
import com.hrshd1eux.expensetracker.domain.model.Category
import com.hrshd1eux.expensetracker.domain.model.DefaultCategories
import com.hrshd1eux.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getActiveCategories(): Flow<List<Category>> {
        return categoryDao.getActiveCategories().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getCategoryById(id: String): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override fun getCategoryByIdFlow(id: String): Flow<Category?> {
        return categoryDao.getCategoryByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun addCategory(category: Category) {
        categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun archiveCategory(id: String, isArchived: Boolean) {
        categoryDao.setArchived(id, isArchived)
    }

    override suspend fun deleteCategory(id: String): Boolean {
        // Safe check: do not permanently delete if historical expenses reference it
        val expenseCount = expenseDao.countExpensesForCategory(id)
        return if (expenseCount > 0) {
            // Archive instead of hard delete
            categoryDao.setArchived(id, true)
            false
        } else {
            categoryDao.deleteCategory(id)
            true
        }
    }

    override suspend fun prepopulateDefaultCategories() {
        val count = categoryDao.getCategoryCount()
        if (count == 0) {
            val defaults = DefaultCategories.getDefaultCategories().map { it.toEntity() }
            categoryDao.insertCategories(defaults)
        }
    }
}
