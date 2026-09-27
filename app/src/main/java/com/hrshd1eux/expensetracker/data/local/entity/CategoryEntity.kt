package com.hrshd1eux.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hrshd1eux.expensetracker.domain.model.Category

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val icon: String,
    val color: Long,
    val isDefault: Boolean,
    val isArchived: Boolean,
    val createdAt: Long
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    color = color,
    isDefault = isDefault,
    isArchived = isArchived,
    createdAt = createdAt
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    color = color,
    isDefault = isDefault,
    isArchived = isArchived,
    createdAt = createdAt
)
