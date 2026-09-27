package com.hrshd1eux.expensetracker.presentation.analytics

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hrshd1eux.expensetracker.domain.usecase.AnalyticsPeriod
import com.hrshd1eux.expensetracker.presentation.components.AmountDisplay
import com.hrshd1eux.expensetracker.presentation.components.CategoryIconView
import com.hrshd1eux.expensetracker.presentation.components.EmptyState
import com.hrshd1eux.expensetracker.presentation.theme.PaymentCashColor
import com.hrshd1eux.expensetracker.presentation.theme.PaymentUpiColor
import com.hrshd1eux.expensetracker.util.CurrencyFormatter
import com.hrshd1eux.expensetracker.util.DateTimeUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val data = state.analyticsData
    val context = LocalContext.current

    /**
     * Opens two sequential DatePickerDialogs (start, then end) and calls
     * [viewModel.onCustomRangeSelected] when both are confirmed.
     * Uses Android View DatePickerDialog — no Compose-only alternative without
     * a third-party library, and this is the same approach used in AddExpenseScreen.
     */
    fun showCustomRangePicker() {
        val today = LocalDate.now()
        val startCal = Calendar.getInstance().apply {
            timeInMillis = state.customStartMillis
        }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val startDate = LocalDate.of(y, m + 1, d)
                val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // After start is picked, open end picker
                val endCal = Calendar.getInstance().apply {
                    timeInMillis = state.customEndMillis
                }
                DatePickerDialog(
                    context,
                    { _, ey, em, ed ->
                        val endDate = LocalDate.of(ey, em + 1, ed)
                        val endMillis = endDate.atTime(23, 59, 59)
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        // Swap if user picked end before start
                        if (endMillis >= startMillis) {
                            viewModel.onCustomRangeSelected(startMillis, endMillis)
                        } else {
                            viewModel.onCustomRangeSelected(endMillis, startMillis)
                        }
                    },
                    endCal.get(Calendar.YEAR),
                    endCal.get(Calendar.MONTH),
                    endCal.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    datePicker.maxDate = System.currentTimeMillis()
                    setTitle("End date")
                }.show()
            },
            startCal.get(Calendar.YEAR),
            startCal.get(Calendar.MONTH),
            startCal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            setTitle("Start date")
        }.show()
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Screen Header & Period Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Analytics",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Scrollable row — all 4 period chips fit without clipping
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            AnalyticsPeriod.TODAY,
                            AnalyticsPeriod.THIS_WEEK,
                            AnalyticsPeriod.THIS_MONTH
                        ).forEach { period ->
                            FilterChip(
                                selected = state.selectedPeriod == period,
                                onClick = { viewModel.onPeriodSelected(period) },
                                label = { Text(period.displayName) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Custom range chip — shows date range label when active
                        val customLabel = if (state.selectedPeriod == AnalyticsPeriod.CUSTOM) {
                            val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
                            val start = Instant.ofEpochMilli(state.customStartMillis)
                                .atZone(ZoneId.systemDefault()).toLocalDate().format(fmt)
                            val end = Instant.ofEpochMilli(state.customEndMillis)
                                .atZone(ZoneId.systemDefault()).toLocalDate().format(fmt)
                            "$start – $end"
                        } else {
                            "Custom"
                        }
                        FilterChip(
                            selected = state.selectedPeriod == AnalyticsPeriod.CUSTOM,
                            onClick = { showCustomRangePicker() },
                            label = { Text(customLabel) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Show active date range sub-label for Custom
                    if (state.selectedPeriod == AnalyticsPeriod.CUSTOM) {
                        val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
                        val start = Instant.ofEpochMilli(state.customStartMillis)
                            .atZone(ZoneId.systemDefault()).toLocalDate().format(fmt)
                        val end = Instant.ofEpochMilli(state.customEndMillis)
                            .atZone(ZoneId.systemDefault()).toLocalDate().format(fmt)
                        Text(
                            text = "$start → $end",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            if (data == null || data.totalSpendingPaise == 0L) {
                item {
                    EmptyState(
                        icon = Icons.Default.Insights,
                        title = "Not enough data yet",
                        description = "Add a few expenses in this period to see your spending patterns and trends.",
                        modifier = Modifier.padding(top = 40.dp)
                    )
                }
            } else {
                // Total Spent Hero Card
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Total spent (${state.selectedPeriod.displayName.lowercase()})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AmountDisplay(
                                amountPaise = data.totalSpendingPaise,
                                currencySymbol = state.currencySymbol,
                                currencyCode = state.currencyCode,
                                textStyle = MaterialTheme.typography.displayLarge
                            )
                        }
                    }
                }

                // Stats Cards Row: Average per day & Highest spending day
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Average Daily
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Daily avg",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                AmountDisplay(
                                    amountPaise = data.averageDailySpendingPaise,
                                    currencySymbol = state.currencySymbol,
                                    currencyCode = state.currencyCode,
                                    textStyle = MaterialTheme.typography.titleLarge
                                )
                            }
                        }

                        // Highest Day
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Peak day",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (data.highestSpendingDay != null) {
                                    val (date, paise) = data.highestSpendingDay
                                    AmountDisplay(
                                        amountPaise = paise,
                                        currencySymbol = state.currencySymbol,
                                        currencyCode = state.currencyCode,
                                        textStyle = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = date.format(
                                            DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                } else {
                                    Text(
                                        text = "—",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                        }
                    }
                }

                // Largest Expense card — previously computed but never displayed
                if (data.largestExpense != null) {
                    item {
                        val le = data.largestExpense
                        val catName = le.category?.name ?: "Expense"
                        val catIcon = le.category?.icon ?: "category"
                        val catColor = le.category?.color ?: 0xFF00897B

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Icon with arrow-up badge overlay
                                Box(contentAlignment = Alignment.TopEnd) {
                                    CategoryIconView(
                                        iconKey = catIcon,
                                        colorLong = catColor,
                                        size = 44.dp,
                                        iconSize = 24.dp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onError,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Largest expense",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = catName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (le.expense.note.isNotBlank()) {
                                        Text(
                                            text = le.expense.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                AmountDisplay(
                                    amountPaise = le.expense.amountPaise,
                                    currencySymbol = state.currencySymbol,
                                    currencyCode = state.currencyCode,
                                    textStyle = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }

                // Daily / Hourly Spending Bar Chart
                item {
                    val isToday = state.selectedPeriod == AnalyticsPeriod.TODAY
                    val breakdown = if (isToday) {
                        data.hourlyBreakdown.map { it.label to it.totalPaise }
                    } else {
                        data.dailyBreakdown.map { it.label to it.totalPaise }
                    }

                    val maxVal = (breakdown.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1L)

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = if (isToday) "Hourly spending" else "Daily spending",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Bar chart — last 14 days for daily, or every-3rd-hour for today
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val displayedItems = if (isToday) {
                                    // Show every 3rd hour (0, 3, 6, … 21) to avoid label collision
                                    breakdown.filterIndexed { index, _ -> index % 3 == 0 }
                                } else {
                                    breakdown.takeLast(14)
                                }

                                displayedItems.forEach { (label, value) ->
                                    val fraction = (value.toFloat() / maxVal).coerceIn(0.04f, 1f)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.Bottom,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.7f)
                                                .fillMaxHeight(fraction)
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(
                                                    if (value > 0L) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                )
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Payment Method: UPI vs Cash
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Payment methods",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Visual split bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (data.upiPercentage > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(data.upiPercentage.coerceAtLeast(0.01f))
                                            .background(PaymentUpiColor)
                                    )
                                }
                                if (data.cashPercentage > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(data.cashPercentage.coerceAtLeast(0.01f))
                                            .background(PaymentCashColor)
                                    )
                                }
                            }

                            // Legend rows
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // UPI
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(PaymentUpiColor)
                                    )
                                    Column {
                                        Text(
                                            text = CurrencyFormatter.formatPaise(
                                                data.upiSpendingPaise,
                                                state.currencySymbol,
                                                state.currencyCode
                                            ),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "UPI · ${String.format(Locale.getDefault(), "%.0f%%", data.upiPercentage)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                // Cash
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(PaymentCashColor)
                                    )
                                    Column {
                                        Text(
                                            text = CurrencyFormatter.formatPaise(
                                                data.cashSpendingPaise,
                                                state.currencySymbol,
                                                state.currencyCode
                                            ),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Cash · ${String.format(Locale.getDefault(), "%.0f%%", data.cashPercentage)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Spending by Category — horizontal bars
                item {
                    Text(
                        text = "Spending by category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                items(data.categorySpends) { catSpend ->
                    val cat = catSpend.category
                    val catName = cat?.name ?: "Other"
                    val catIcon = cat?.icon ?: "category"
                    val catColor = cat?.color ?: 0xFF00897B

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CategoryIconView(
                                    iconKey = catIcon,
                                    colorLong = catColor,
                                    size = 36.dp,
                                    iconSize = 20.dp
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = catName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.getDefault(),
                                            "%.1f%% of total",
                                            catSpend.percentage
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                AmountDisplay(
                                    amountPaise = catSpend.amountPaise,
                                    currencySymbol = state.currencySymbol,
                                    currencyCode = state.currencyCode,
                                    textStyle = MaterialTheme.typography.titleMedium
                                )
                            }

                            // Horizontal progress bar (width = percentage of total)
                            LinearProgressIndicator(
                                progress = { (catSpend.percentage / 100f).coerceIn(0f, 1f) },
                                color = Color(catColor),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
