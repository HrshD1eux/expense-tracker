package com.hrshd1eux.expensetracker

import com.hrshd1eux.expensetracker.domain.model.Category
import com.hrshd1eux.expensetracker.domain.repository.CategoryRepository
import com.hrshd1eux.expensetracker.domain.usecase.AddCategoryUseCase
import com.hrshd1eux.expensetracker.domain.usecase.ArchiveCategoryUseCase
import com.hrshd1eux.expensetracker.domain.usecase.DeleteCategoryUseCase
import com.hrshd1eux.expensetracker.domain.usecase.GetCategoriesUseCase
import com.hrshd1eux.expensetracker.domain.usecase.UpdateCategoryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategoryUseCasesTest {

    private lateinit var fakeCategoryRepo: FakeCategoryRepository
    private lateinit var getCategoriesUseCase: GetCategoriesUseCase
    private lateinit var addCategoryUseCase: AddCategoryUseCase
    private lateinit var updateCategoryUseCase: UpdateCategoryUseCase
    private lateinit var archiveCategoryUseCase: ArchiveCategoryUseCase
    private lateinit var deleteCategoryUseCase: DeleteCategoryUseCase

    @Before
    fun setup() {
        fakeCategoryRepo = FakeCategoryRepository()
        getCategoriesUseCase = GetCategoriesUseCase(fakeCategoryRepo)
        addCategoryUseCase = AddCategoryUseCase(fakeCategoryRepo)
        updateCategoryUseCase = UpdateCategoryUseCase(fakeCategoryRepo)
        archiveCategoryUseCase = ArchiveCategoryUseCase(fakeCategoryRepo)
        deleteCategoryUseCase = DeleteCategoryUseCase(fakeCategoryRepo)

        fakeCategoryRepo.categories.add(
            Category(id = "cat_food", name = "Food", icon = "restaurant", color = 0xFFFB8C00, isDefault = true)
        )
        fakeCategoryRepo.categories.add(
            Category(id = "cat_transport", name = "Transport", icon = "directions_transit", color = 0xFF1E88E5, isDefault = true)
        )
    }

    @Test
    fun testAddCategory_validName_succeeds() = runBlocking {
        val result = addCategoryUseCase("Books & Reading", "menu_book", 0xFF8E24AA)
        assertTrue(result.isSuccess)
        val created = result.getOrThrow()
        assertEquals("Books & Reading", created.name)
        assertEquals("menu_book", created.icon)
        assertEquals(0xFF8E24AA, created.color)
        assertFalse(created.isDefault)
        assertFalse(created.isArchived)
        assertTrue(created.id.startsWith("cat_"))

        val all = getCategoriesUseCase(includeArchived = false).first()
        assertEquals(3, all.size)
    }

    @Test
    fun testAddCategory_emptyOrBlankName_fails() = runBlocking {
        val result1 = addCategoryUseCase("", "category", 0xFF000000)
        assertTrue(result1.isFailure)

        val result2 = addCategoryUseCase("   ", "category", 0xFF000000)
        assertTrue(result2.isFailure)
    }

    @Test
    fun testRenameAndUpdateCategory() = runBlocking {
        val cat = fakeCategoryRepo.categories.first { it.id == "cat_food" }
        val updated = cat.copy(name = "Dining & Drinks", icon = "local_cafe")

        val result = updateCategoryUseCase(updated)
        assertTrue(result.isSuccess)

        val retrieved = fakeCategoryRepo.getCategoryById("cat_food")
        assertNotNull(retrieved)
        assertEquals("Dining & Drinks", retrieved?.name)
        assertEquals("local_cafe", retrieved?.icon)
    }

    @Test
    fun testArchiveAndRestoreCategory() = runBlocking {
        // Archive cat_transport
        archiveCategoryUseCase("cat_transport", archive = true)

        val activeList = getCategoriesUseCase(includeArchived = false).first()
        assertEquals(1, activeList.size)
        assertEquals("cat_food", activeList.first().id)

        val allList = getCategoriesUseCase(includeArchived = true).first()
        assertEquals(2, allList.size)
        val archived = allList.first { it.id == "cat_transport" }
        assertTrue(archived.isArchived)

        // Restore cat_transport
        archiveCategoryUseCase("cat_transport", archive = false)
        val restoredActiveList = getCategoriesUseCase(includeArchived = false).first()
        assertEquals(2, restoredActiveList.size)
    }

    @Test
    fun testDeleteCategory_withHistoricalExpenses_archivesInsteadOfPermanentDelete() = runBlocking {
        fakeCategoryRepo.categoriesWithExpenses.add("cat_food")

        val deleted = deleteCategoryUseCase("cat_food")
        assertFalse("Should not hard delete when historical expenses reference it", deleted)

        // Verify it was archived instead
        val cat = fakeCategoryRepo.getCategoryById("cat_food")
        assertNotNull(cat)
        assertTrue("Category should be archived", cat?.isArchived == true)
    }

    @Test
    fun testDeleteCategory_withZeroExpenses_hardDeletes() = runBlocking {
        // Add a custom category with 0 expenses
        val newCat = addCategoryUseCase("Temporary", "category", 0xFF123456).getOrThrow()
        assertEquals(3, fakeCategoryRepo.categories.size)

        val deleted = deleteCategoryUseCase(newCat.id)
        assertTrue("Should permanently delete when 0 expenses reference it", deleted)
        assertEquals(2, fakeCategoryRepo.categories.size)
    }

    private class FakeCategoryRepository : CategoryRepository {
        val categories = mutableListOf<Category>()
        val categoriesWithExpenses = mutableSetOf<String>()

        override fun getAllCategories(): Flow<List<Category>> = flowOf(categories.toList())
        override fun getActiveCategories(): Flow<List<Category>> = flowOf(categories.filter { !it.isArchived })
        override suspend fun getCategoryById(id: String): Category? = categories.find { it.id == id }
        override fun getCategoryByIdFlow(id: String): Flow<Category?> = flowOf(categories.find { it.id == id })

        override suspend fun addCategory(category: Category) {
            val idx = categories.indexOfFirst { it.id == category.id }
            if (idx != -1) categories[idx] = category else categories.add(category)
        }

        override suspend fun updateCategory(category: Category) { addCategory(category) }

        override suspend fun archiveCategory(id: String, isArchived: Boolean) {
            val cat = categories.find { it.id == id }
            if (cat != null) addCategory(cat.copy(isArchived = isArchived))
        }

        override suspend fun deleteCategory(id: String): Boolean {
            return if (categoriesWithExpenses.contains(id)) {
                archiveCategory(id, true)
                false
            } else {
                categories.removeAll { it.id == id }
                true
            }
        }

        override suspend fun prepopulateDefaultCategories() {}
    }
}
