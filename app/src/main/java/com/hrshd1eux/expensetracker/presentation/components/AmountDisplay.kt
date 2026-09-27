package com.hrshd1eux.expensetracker.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrshd1eux.expensetracker.util.CurrencyFormatter

@Composable
fun AmountDisplay(
    amountPaise: Long,
    modifier: Modifier = Modifier,
    currencySymbol: String = "₹",
    currencyCode: String = "INR",
    textStyle: TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    includeDecimalsWhenZero: Boolean = false
) {
    val formatted = CurrencyFormatter.formatPaise(
        paise = amountPaise,
        symbol = currencySymbol,
        currencyCode = currencyCode,
        includeDecimalsWhenZero = includeDecimalsWhenZero
    )

    Text(
        text = formatted,
        style = textStyle,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}
