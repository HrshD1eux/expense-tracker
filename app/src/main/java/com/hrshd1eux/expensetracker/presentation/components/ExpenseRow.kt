package com.hrshd1eux.expensetracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hrshd1eux.expensetracker.domain.model.ExpenseWithCategory
import com.hrshd1eux.expensetracker.domain.model.PaymentMethod
import com.hrshd1eux.expensetracker.presentation.theme.PaymentCashColor
import com.hrshd1eux.expensetracker.presentation.theme.PaymentUpiColor
import com.hrshd1eux.expensetracker.util.DateTimeUtils

/**
 * A single expense row for lists.
 *
 * Subtitle line format (always one line, never reserved space for empty note):
 *   With note:    "Lunch · UPI · 1:24 PM"
 *   Without note: "UPI · 1:24 PM"
 */
@Composable
fun ExpenseRow(
    item: ExpenseWithCategory,
    currencySymbol: String,
    currencyCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expense = item.expense
    val category = item.category
    val catColorLong = category?.color ?: 0xFF00897B
    val catIconKey = category?.icon ?: "category"
    val catName = category?.name ?: "Expense"

    val isUpi = expense.paymentMethod == PaymentMethod.UPI
    val badgeBg = if (isUpi) PaymentUpiColor.copy(alpha = 0.12f) else PaymentCashColor.copy(alpha = 0.12f)
    val badgeText = if (isUpi) PaymentUpiColor else PaymentCashColor
    val timeStr = DateTimeUtils.formatTime(expense.timestamp)

    // Build the one-line subtitle: "[note · ]METHOD · time"
    val subtitlePrefix = if (expense.note.isNotBlank()) "${expense.note} · " else ""
    val subtitleSuffix = "· $timeStr"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Category Icon
        CategoryIconView(
            iconKey = catIconKey,
            colorLong = catColorLong,
            size = 44.dp,
            iconSize = 24.dp
        )

        // Middle column: category name + one-line subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Category name — primary line
            Text(
                text = catName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle — note (if any) · PAYMENT · time, all on one line
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Optional note prefix text
                if (subtitlePrefix.isNotEmpty()) {
                    Text(
                        text = subtitlePrefix,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                // Payment method colored badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = expense.paymentMethod.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeText
                    )
                }

                if (expense.isReimbursable) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Lent",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                // " · time"
                Text(
                    text = subtitleSuffix,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
        }

        // Amount — right-aligned
        AmountDisplay(
            amountPaise = expense.amountPaise,
            currencySymbol = currencySymbol,
            currencyCode = currencyCode,
            textStyle = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
