package com.hrshd1eux.expensetracker.domain.model

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val color: Long,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
