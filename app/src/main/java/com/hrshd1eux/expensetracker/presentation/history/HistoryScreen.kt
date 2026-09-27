package com.hrshd1eux.expensetracker.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hrshd1eux.expensetracker.domain.model.PaymentMethod
import com.hrshd1eux.expensetracker.presentation.components.CategoryChip
import com.hrshd1eux.expensetracker.presentation.components.DateHeader
import com.hrshd1eux.expensetracker.presentation.components.EmptyState
import com.hrshd1eux.expensetracker.presentation.components.ExpenseRow
import com.hrshd1eux.expensetracker.presentation.components.SwipeableExpenseRow
import com.hrshd1eux.expensetracker.util.DateTimeUtils
import com.hrshd1eux.expensetracker.util.MoneyUtils
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    onNavigateToEditExpense: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val hasActiveFilters = state.filter.paymentMethod != null ||
            state.filter.selectedCategoryIds.isNotEmpty() ||
            state.filter.startMillis != null ||
            state.filter.minAmountPaise != null ||
            state.filter.maxAmountPaise != null

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is HistoryUiEvent.ShowUndoSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "UNDO",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete(event.expense)
                    }
                }
            }
        }
    }

    if (state.isFilterSheetVisible) {
        var tempPaymentMethod by remember { mutableStateOf(state.filter.paymentMethod) }
        var tempSelectedCats by remember { mutableStateOf(state.filter.selectedCategoryIds) }
        // Reverse-map the active startMillis to the correct preset label so the chip
        // stays selected when the sheet is re-opened after a filter was applied.
        var tempPresetRange by remember {
            mutableStateOf(
                when (state.filter.startMillis) {
                    DateTimeUtils.getDayStartEpochMillis() -> "TODAY"
                    DateTimeUtils.getWeekStartEpochMillis() -> "WEEK"
                    DateTimeUtils.getMonthStartEpochMillis() -> "MONTH"
                    else -> "ALL"
                }
            )
        }
        var tempMinAmount by remember {
            mutableStateOf(if (state.filter.minAmountPaise != null) MoneyUtils.formatPaiseToEditable(state.filter.minAmountPaise!!) else "")
        }
        var tempMaxAmount by remember {
            mutableStateOf(if (state.filter.maxAmountPaise != null) MoneyUtils.formatPaiseToEditable(state.filter.maxAmountPaise!!) else "")
        }

        ModalBottomSheet(
            onDismissRequest = { viewModel.setFilterSheetVisible(false) },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Expenses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(onClick = { viewModel.resetFilters() }) {
                        Text("Reset")
                    }
                }

                // Date Presets
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Date Range",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ALL" to "All", "TODAY" to "Today", "WEEK" to "This Week", "MONTH" to "This Month").forEach { (key, label) ->
                            FilterChip(
                                selected = tempPresetRange == key,
                                onClick = { tempPresetRange = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                // Payment Method
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Payment Method",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = tempPaymentMethod == null,
                            onClick = { tempPaymentMethod = null },
                            label = { Text("All") }
                        )
                        FilterChip(
                            selected = tempPaymentMethod == PaymentMethod.UPI,
                            onClick = { tempPaymentMethod = PaymentMethod.UPI },
                            label = { Text("UPI") }
                        )
                        FilterChip(
                            selected = tempPaymentMethod == PaymentMethod.CASH,
                            onClick = { tempPaymentMethod = PaymentMethod.CASH },
                            label = { Text("Cash") }
                        )
                    }
                }

                // Amount Range
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Amount Range (${state.currencySymbol})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = tempMinAmount,
                            onValueChange = { tempMinAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Min") },
                            placeholder = { Text("0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tempMaxAmount,
                            onValueChange = { tempMaxAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Max") },
                            placeholder = { Text("No limit") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Categories
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.allCategories.forEach { cat ->
                            CategoryChip(
                                category = cat,
                                isSelected = tempSelectedCats.contains(cat.id),
                                onSelected = {
                                    tempSelectedCats = if (tempSelectedCats.contains(cat.id)) {
                                        tempSelectedCats - cat.id
                                    } else {
                                        tempSelectedCats + cat.id
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val (startM, endM) = when (tempPresetRange) {
                            "TODAY" -> Pair(DateTimeUtils.getDayStartEpochMillis(), DateTimeUtils.getDayEndEpochMillis())
                            "WEEK" -> Pair(DateTimeUtils.getWeekStartEpochMillis(), DateTimeUtils.getWeekEndEpochMillis())
                            "MONTH" -> Pair(DateTimeUtils.getMonthStartEpochMillis(), DateTimeUtils.getMonthEndEpochMillis())
                            else -> Pair(null, null)
                        }
                        val minPaise = if (tempMinAmount.isNotBlank()) MoneyUtils.parseAmountToPaise(tempMinAmount) else null
                        val maxPaise = if (tempMaxAmount.isNotBlank()) MoneyUtils.parseAmountToPaise(tempMaxAmount) else null

                        viewModel.applyFilters(
                            paymentMethod = tempPaymentMethod,
                            categoryIds = tempSelectedCats,
                            startMillis = startM,
                            endMillis = endM,
                            minAmountPaise = minPaise,
                            maxAmountPaise = maxPaise
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Apply Filters", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search & Filter Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.filter.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search note, category, amount...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (state.filter.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Filter Button with Badge
                IconButton(
                    onClick = { viewModel.setFilterSheetVisible(true) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (hasActiveFilters) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                ) {
                    if (hasActiveFilters) {
                        BadgedBox(badge = { Badge { Text("!") } }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filters",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filters",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Grouped Transaction List
            if (state.groupedExpenses.isEmpty() && !state.isLoading) {
                EmptyState(
                    icon = Icons.Default.History,
                    title = if (hasActiveFilters || state.filter.searchQuery.isNotEmpty()) "No matching expenses" else "No expenses yet",
                    description = if (hasActiveFilters || state.filter.searchQuery.isNotEmpty()) "Try adjusting your search or active filters." else "Your chronological spending history will appear here.",
                    actionLabel = if (hasActiveFilters) "Reset Filters" else null,
                    onActionClick = if (hasActiveFilters) { { viewModel.resetFilters() } } else null,
                    modifier = Modifier.padding(top = 40.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.groupedExpenses.forEach { group ->
                        item(key = "header_${group.dateEpochMillis}") {
                            DateHeader(
                                title = group.dateHeader,
                                totalPaise = group.totalPaise,
                                currencySymbol = state.currencySymbol,
                                currencyCode = state.currencyCode
                            )
                        }

                        items(
                            items = group.expenses,
                            key = { it.expense.id }
                        ) { item ->
                            SwipeableExpenseRow(
                                item = item,
                                currencySymbol = state.currencySymbol,
                                currencyCode = state.currencyCode,
                                onClick = { onNavigateToEditExpense(item.expense.id) },
                                onDelete = { viewModel.deleteExpense(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}
