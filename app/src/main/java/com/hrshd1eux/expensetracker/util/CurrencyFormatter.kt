package com.hrshd1eux.expensetracker.util

object CurrencyFormatter {

    /**
     * Formats integer paise into a localized currency string.
     * For INR, it formats in the Indian numbering system (e.g., ₹1,25,000 or ₹1,250.50).
     * For other currencies, standard comma thousand separators are used.
     * All calculations are pure integer and string manipulation without Float/Double.
     */
    fun formatPaise(
        paise: Long,
        symbol: String = "₹",
        currencyCode: String = "INR",
        includeDecimalsWhenZero: Boolean = false
    ): String {
        val isNegative = paise < 0L
        val absPaise = if (isNegative) -paise else paise

        val rupees = absPaise / 100L
        val remainder = absPaise % 100L

        val formattedRupees = if (currencyCode.equals("INR", ignoreCase = true)) {
            formatIndianNumbering(rupees)
        } else {
            formatStandardNumbering(rupees)
        }

        val decimalPart = if (remainder != 0L || includeDecimalsWhenZero) {
            "." + remainder.toString().padStart(2, '0')
        } else {
            ""
        }

        val prefix = if (isNegative) "-$symbol" else symbol
        return "$prefix$formattedRupees$decimalPart"
    }

    /**
     * Indian numbering system:
     * Last 3 digits grouped, then previous digits grouped in pairs of 2.
     * Example: 125000 -> "1,25,000"
     * Example: 12345678 -> "1,23,45,678"
     */
    private fun formatIndianNumbering(number: Long): String {
        val str = number.toString()
        if (str.length <= 3) return str

        val lastThree = str.substring(str.length - 3)
        var remaining = str.substring(0, str.length - 3)

        val parts = mutableListOf<String>()
        parts.add(lastThree)

        while (remaining.length > 2) {
            val part = remaining.substring(remaining.length - 2)
            parts.add(part)
            remaining = remaining.substring(0, remaining.length - 2)
        }

        if (remaining.isNotEmpty()) {
            parts.add(remaining)
        }

        return parts.reversed().joinToString(",")
    }

    /**
     * Standard 3-digit thousand separator numbering system.
     * Example: 125000 -> "125,000"
     */
    private fun formatStandardNumbering(number: Long): String {
        val str = number.toString()
        if (str.length <= 3) return str

        val parts = mutableListOf<String>()
        var remaining = str

        while (remaining.length > 3) {
            val part = remaining.substring(remaining.length - 3)
            parts.add(part)
            remaining = remaining.substring(0, remaining.length - 3)
        }

        if (remaining.isNotEmpty()) {
            parts.add(remaining)
        }

        return parts.reversed().joinToString(",")
    }
}
