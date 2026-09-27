package com.hrshd1eux.expensetracker.domain.model

object DefaultCategories {

    fun getDefaultCategories(): List<Category> {
        val now = 1727424000000L // Consistent initial creation timestamp
        return listOf(
            // Food
            Category(id = "cat_food", name = "Food", icon = "restaurant", color = 0xFFFB8C00, isDefault = true, createdAt = now),
            Category(id = "cat_groceries", name = "Groceries", icon = "shopping_cart", color = 0xFF43A047, isDefault = true, createdAt = now),
            Category(id = "cat_coffee", name = "Coffee", icon = "local_cafe", color = 0xFF6D4C41, isDefault = true, createdAt = now),
            Category(id = "cat_snacks", name = "Snacks", icon = "fastfood", color = 0xFFFFA000, isDefault = true, createdAt = now),

            // Transport
            Category(id = "cat_transport", name = "Transport", icon = "directions_transit", color = 0xFF1E88E5, isDefault = true, createdAt = now),
            Category(id = "cat_fuel", name = "Fuel", icon = "local_gas_station", color = 0xFFE53935, isDefault = true, createdAt = now),
            Category(id = "cat_auto", name = "Auto", icon = "electric_rickshaw", color = 0xFFFDD835, isDefault = true, createdAt = now),
            Category(id = "cat_cab", name = "Cab", icon = "local_taxi", color = 0xFFFFB300, isDefault = true, createdAt = now),
            Category(id = "cat_bus_train", name = "Bus/Train", icon = "directions_bus", color = 0xFF039BE5, isDefault = true, createdAt = now),

            // Shopping
            Category(id = "cat_shopping", name = "Shopping", icon = "shopping_bag", color = 0xFFD81B60, isDefault = true, createdAt = now),
            Category(id = "cat_clothing", name = "Clothing", icon = "checkroom", color = 0xFF8E24AA, isDefault = true, createdAt = now),
            Category(id = "cat_electronics", name = "Electronics", icon = "devices", color = 0xFF3949AB, isDefault = true, createdAt = now),
            Category(id = "cat_household", name = "Household", icon = "home", color = 0xFF00897B, isDefault = true, createdAt = now),
            Category(id = "cat_personal_care", name = "Personal Care", icon = "spa", color = 0xFF00ACC1, isDefault = true, createdAt = now),

            // Bills
            Category(id = "cat_bills", name = "Bills", icon = "receipt_long", color = 0xFF546E7A, isDefault = true, createdAt = now),
            Category(id = "cat_electricity", name = "Electricity", icon = "bolt", color = 0xFFFFB300, isDefault = true, createdAt = now),
            Category(id = "cat_mobile", name = "Mobile", icon = "smartphone", color = 0xFF1E88E5, isDefault = true, createdAt = now),
            Category(id = "cat_internet", name = "Internet", icon = "wifi", color = 0xFF00897B, isDefault = true, createdAt = now),
            Category(id = "cat_rent", name = "Rent", icon = "apartment", color = 0xFF5E35B1, isDefault = true, createdAt = now),
            Category(id = "cat_subscription", name = "Subscription", icon = "subscriptions", color = 0xFFE53935, isDefault = true, createdAt = now),

            // Lifestyle / Entertainment
            Category(id = "cat_entertainment", name = "Entertainment", icon = "movie", color = 0xFF8E24AA, isDefault = true, createdAt = now),
            Category(id = "cat_travel", name = "Travel", icon = "flight", color = 0xFF00ACC1, isDefault = true, createdAt = now),
            Category(id = "cat_fitness", name = "Fitness", icon = "fitness_center", color = 0xFF43A047, isDefault = true, createdAt = now),

            // Personal
            Category(id = "cat_medical", name = "Medical", icon = "local_hospital", color = 0xFFE53935, isDefault = true, createdAt = now),
            Category(id = "cat_education", name = "Education", icon = "school", color = 0xFF3949AB, isDefault = true, createdAt = now),
            Category(id = "cat_gifts", name = "Gifts", icon = "card_giftcard", color = 0xFFD81B60, isDefault = true, createdAt = now),

            // Other
            Category(id = "cat_other", name = "Other", icon = "category", color = 0xFF78909C, isDefault = true, createdAt = now)
        )
    }
}
