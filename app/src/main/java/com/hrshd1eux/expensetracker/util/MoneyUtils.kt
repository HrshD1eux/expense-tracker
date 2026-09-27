package com.hrshd1eux.expensetracker.util

/**
 * Pure integer-based monetary calculation utilities.
 * All operations prevent floating point rounding inaccuracies.
 */
object MoneyUtils {

    /**
     * Converts a raw user input string (e.g., "250", "125.50", "0.75") to integer paise.
     * Returns 0L for invalid or blank inputs.
     * NO Float or Double is used in this conversion.
     */
    fun parseAmountToPaise(input: String): Long {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return 0L

        // Remove any existing commas or currency symbols
        val sanitized = trimmed.replace(",", "").replace("₹", "").replace("$", "").trim()
        if (sanitized.isEmpty()) return 0L

        val parts = sanitized.split(".")
        if (parts.size > 2) return 0L // Invalid format (multiple decimal points)

        val rupeesStr = parts[0]
        val rupees = if (rupeesStr.isEmpty()) 0L else rupeesStr.toLongOrNull() ?: return 0L

        val paise = if (parts.size == 2) {
            val decimalsStr = parts[1]
            when (decimalsStr.length) {
                0 -> 0L
                1 -> (decimalsStr.substring(0, 1).toLongOrNull() ?: 0L) * 10L
                else -> decimalsStr.substring(0, 2).toLongOrNull() ?: 0L
            }
        } else {
            0L
        }

        if (rupees < 0L || paise < 0L) return 0L

        // Check for overflow
        return try {
            Math.addExact(Math.multiplyExact(rupees, 100L), paise)
        } catch (e: ArithmeticException) {
            Long.MAX_VALUE
        }
    }

    /**
     * Converts paise into plain decimal string for editing (e.g. 25000L -> "250", 12550L -> "125.50").
     */
    fun formatPaiseToEditable(paise: Long): String {
        if (paise <= 0L) return ""
        val rupees = paise / 100L
        val remainder = paise % 100L
        return if (remainder == 0L) {
            rupees.toString()
        } else {
            val remStr = remainder.toString().padStart(2, '0')
            "$rupees.$remStr"
        }
    }

    /**
     * Divides total paise by days, handling division by zero safely and rounding properly.
     */
    fun calculateDailyAverage(totalPaise: Long, days: Int): Long {
        if (days <= 0 || totalPaise <= 0L) return 0L
        return (totalPaise + (days / 2)) / days
    }

    /**
     * Checks if the given string contains arithmetic operators.
     */
    fun hasMathOperators(input: String): Boolean {
        return input.any { it == '+' || it == '-' || it == '*' || it == '/' || it == '×' || it == '÷' }
    }

    /**
     * Evaluates an arithmetic expression (e.g. "1450 / 3", "45 + 120 + 85", "350 + 40").
     * Respects standard operator precedence (* and / before + and -).
     * Returns the evaluated amount in integer paise, or null if invalid or incomplete.
     */
    fun evaluateExpressionToPaise(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        if (!hasMathOperators(trimmed)) {
            val p = parseAmountToPaise(trimmed)
            return if (p > 0L) p else null
        }

        return try {
            val sanitized = trimmed
                .replace("×", "*")
                .replace("÷", "/")
                .replace(",", "")
                .replace("₹", "")
                .replace("$", "")
                .trim()

            val tokens = tokenize(sanitized)
            if (tokens.isEmpty()) return null

            val result = parseExpr(tokens) ?: return null
            if (result < java.math.BigDecimal.ZERO) return null

            val paise = result.multiply(java.math.BigDecimal(100))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact()
            paise
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Evaluates an expression and returns a user-friendly decimal string (e.g. "250" or "483.33").
     */
    fun evaluateExpressionToString(input: String): String? {
        val paise = evaluateExpressionToPaise(input) ?: return null
        return formatPaiseToEditable(paise)
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        val n = expr.length
        while (i < n) {
            val c = expr[i]
            if (c.isWhitespace()) {
                i++
                continue
            }
            if (c == '+' || c == '-' || c == '*' || c == '/') {
                tokens.add(c.toString())
                i++
            } else if (c.isDigit() || c == '.') {
                val sb = StringBuilder()
                while (i < n && (expr[i].isDigit() || expr[i] == '.')) {
                    sb.append(expr[i])
                    i++
                }
                tokens.add(sb.toString())
            } else {
                return emptyList()
            }
        }
        return tokens
    }

    private fun parseExpr(tokens: List<String>): java.math.BigDecimal? {
        if (tokens.isEmpty()) return null
        // First pass: handle * and /
        val intermediateTokens = mutableListOf<String>()
        var idx = 0
        while (idx < tokens.size) {
            val token = tokens[idx]
            if (token == "*" || token == "/") {
                if (intermediateTokens.isEmpty() || idx + 1 >= tokens.size) return null
                val prevVal = intermediateTokens.removeAt(intermediateTokens.size - 1).toBigDecimalOrNull() ?: return null
                val nextVal = tokens[idx + 1].toBigDecimalOrNull() ?: return null
                val computed = if (token == "*") {
                    prevVal.multiply(nextVal)
                } else {
                    if (nextVal.compareTo(java.math.BigDecimal.ZERO) == 0) return null
                    prevVal.divide(nextVal, 6, java.math.RoundingMode.HALF_UP)
                }
                intermediateTokens.add(computed.toPlainString())
                idx += 2
            } else {
                intermediateTokens.add(token)
                idx++
            }
        }

        // Second pass: handle + and -
        if (intermediateTokens.isEmpty()) return null
        var total = intermediateTokens[0].toBigDecimalOrNull() ?: return null
        var opIdx = 1
        while (opIdx < intermediateTokens.size) {
            val op = intermediateTokens[opIdx]
            if (opIdx + 1 >= intermediateTokens.size) return null
            val nextVal = intermediateTokens[opIdx + 1].toBigDecimalOrNull() ?: return null
            total = when (op) {
                "+" -> total.add(nextVal)
                "-" -> total.subtract(nextVal)
                else -> return null
            }
            opIdx += 2
        }
        return total
    }
}

