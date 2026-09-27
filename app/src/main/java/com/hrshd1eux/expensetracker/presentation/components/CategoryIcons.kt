package com.hrshd1eux.expensetracker.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIcons {

    private val iconMap = mapOf(
        "restaurant" to Icons.Default.Restaurant,
        "shopping_cart" to Icons.Default.ShoppingCart,
        "local_cafe" to Icons.Default.Coffee,
        "coffee" to Icons.Default.Coffee,
        "fastfood" to Icons.Default.Fastfood,
        "directions_transit" to Icons.Default.Train,
        "local_gas_station" to Icons.Default.LocalGasStation,
        "electric_rickshaw" to Icons.Default.LocalTaxi,
        "local_taxi" to Icons.Default.LocalTaxi,
        "directions_bus" to Icons.Default.DirectionsBus,
        "shopping_bag" to Icons.Default.ShoppingCart,
        "checkroom" to Icons.Default.Checkroom,
        "devices" to Icons.Default.Devices,
        "home" to Icons.Default.Home,
        "spa" to Icons.Default.Spa,
        "receipt_long" to Icons.AutoMirrored.Filled.ReceiptLong,
        "bolt" to Icons.Default.Bolt,
        "smartphone" to Icons.Default.Smartphone,
        "wifi" to Icons.Default.Wifi,
        "apartment" to Icons.Default.Apartment,
        "subscriptions" to Icons.Default.Subscriptions,
        "movie" to Icons.Default.Movie,
        "flight" to Icons.Default.Flight,
        "fitness_center" to Icons.Default.FitnessCenter,
        "local_hospital" to Icons.Default.LocalHospital,
        "school" to Icons.Default.School,
        "card_giftcard" to Icons.Default.CardGiftcard,
        "category" to Icons.Default.Category
    )

    fun getIcon(key: String): ImageVector {
        return iconMap[key.lowercase()] ?: Icons.Default.Category
    }

    val availableIconKeys: List<String> = listOf(
        "restaurant", "shopping_cart", "coffee", "fastfood",
        "directions_transit", "local_gas_station", "local_taxi", "directions_bus",
        "checkroom", "devices", "home", "spa",
        "receipt_long", "bolt", "smartphone", "wifi", "apartment", "subscriptions",
        "movie", "flight", "fitness_center", "local_hospital", "school", "card_giftcard",
        "category"
    )
}
