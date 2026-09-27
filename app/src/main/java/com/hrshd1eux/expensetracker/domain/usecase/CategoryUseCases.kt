package com.hrshd1eux.expensetracker.domain.usecase

import com.hrshd1eux.expensetracker.domain.model.Category
import com.hrshd1eux.expensetracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(includeArchived: Boolean = false): Flow<List<Category>> {
        return if (includeArchived) {
            categoryRepository.getAllCategories()
        } else {
            categoryRepository.getActiveCategories()
        }
    }

    suspend fun ensureDefaultCategories() {
        categoryRepository.prepopulateDefaultCategories()
    }
}

class AddCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(name: String, icon: String, color: Long): Result<Category> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Category name cannot be empty"))
        }

        val category = Category(
            id = "cat_" + UUID.randomUUID().toString().replace("-", "").take(12),
            name = trimmed,
            icon = icon,
            color = color,
            isDefault = false,
            isArchived = false,
            createdAt = System.currentTimeMillis()
        )
        categoryRepository.addCategory(category)
        return Result.success(category)
    }
}

class UpdateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): Result<Unit> {
        if (category.name.trim().isEmpty()) {
            return Result.failure(IllegalArgumentException("Category name cannot be empty"))
        }
        categoryRepository.updateCategory(category)
        return Result.success(Unit)
    }
}

class ArchiveCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(id: String, archive: Boolean) {
        categoryRepository.archiveCategory(id, archive)
    }
}

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(id: String): Boolean {
        return categoryRepository.deleteCategory(id)
    }
}
