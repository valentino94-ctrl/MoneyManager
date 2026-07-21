package com.v94studio.moneymanager.ui.screens

import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v94studio.moneymanager.LocalFabVisible
import com.v94studio.moneymanager.LocalRepository
import com.v94studio.moneymanager.LocalSettingsRepository
import com.v94studio.moneymanager.LocalUserSettings
import com.v94studio.moneymanager.R
import com.v94studio.moneymanager.data.TransactionType
import com.v94studio.moneymanager.data.TransactionWithCategory
import com.v94studio.moneymanager.ui.components.AppBottomSheet
import com.v94studio.moneymanager.ui.components.*
import com.v94studio.moneymanager.ui.settings.TransactionFilterSetting
import com.v94studio.moneymanager.ui.settings.TransactionSortSetting
import com.v94studio.moneymanager.ui.settings.UserSettings
import com.v94studio.moneymanager.ui.theme.FintechGreen
import com.v94studio.moneymanager.ui.theme.FintechRed
import com.v94studio.moneymanager.ui.theme.FintechSecondary
import com.v94studio.moneymanager.ui.util.formatDateTime
import com.v94studio.moneymanager.ui.util.getLocalizedCategory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onEditTransaction: (Long) -> Unit
) {
    val repository = LocalRepository.current
    val settingsRepository = LocalSettingsRepository.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val settings: UserSettings = LocalUserSettings.current
    val transactions by repository.observeTransactions().collectAsState(initial = emptyList())
    val recurringItems by repository.observeRecurring().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }

    var searchQuery by rememberSaveable { mutableStateOf(settings.transactionsQuery) }
    var transactionFilter by rememberSaveable { mutableStateOf(settings.transactionsFilter) }
    var dateRangeDays by rememberSaveable { mutableIntStateOf(settings.transactionsRangeDays) }
    var sortSetting by rememberSaveable { mutableStateOf(settings.transactionsSort) }

    var showFilterSheet by remember { mutableStateOf(false) }
    var isCalendarVisible by rememberSaveable { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var currentDisplayMonth by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.time) }

    val activityRange = remember(currentDisplayMonth) {
        val cal = Calendar.getInstance().apply { time = currentDisplayMonth }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val end = cal.timeInMillis
        start to end
    }
    val activityDays by repository.observeActivityDays(activityRange.first, activityRange.second).collectAsState(initial = emptyList())

    val filteredTransactions = remember(transactions, recurringItems, searchQuery, transactionFilter, dateRangeDays, sortSetting, selectedDate) {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val selectedDayStr = selectedDate?.let { sdf.format(it) }
        
        val nowMillis = System.currentTimeMillis()
        val since = nowMillis - dateRangeDays * 24L * 60L * 60L * 1000L

        // Combined list of real transactions and projected recurring items
        val list = transactions.filter { item ->
            val inRange = if (selectedDayStr != null) {
                sdf.format(Date(item.transaction.timestamp)) == selectedDayStr
            } else {
                item.transaction.timestamp >= since
            }
            
            val matchesType = when (transactionFilter) {
                TransactionFilterSetting.ALL -> true
                TransactionFilterSetting.INCOME -> item.transaction.type == TransactionType.INCOME
                TransactionFilterSetting.EXPENSE -> item.transaction.type == TransactionType.EXPENSE
            }
            val query = searchQuery.trim()
            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                val category = item.categoryName ?: ""
                val note = item.transaction.note
                val localizedCategory = getLocalizedCategory(category, context)
                localizedCategory.contains(query, ignoreCase = true) || note.contains(query, ignoreCase = true)
            }
            inRange && matchesType && matchesQuery
        }.toMutableList()

        // If a future date is selected, add projected recurring items
        if (selectedDate != null && selectedDate!!.time > nowMillis) {
            recurringItems.forEach { recurring ->
                if (isRecurringOnDate(recurring.recurring, selectedDate!!)) {
                    // Create a "virtual" transaction for display
                    list.add(
                        TransactionWithCategory(
                            transaction = com.v94studio.moneymanager.data.TransactionEntity(
                                id = -1L, // Temporary ID
                                amountMinor = com.v94studio.moneymanager.data.amountToMinor(recurring.recurring.amount),
                                type = recurring.recurring.type,
                                categoryId = recurring.recurring.categoryId,
                                accountId = recurring.recurring.accountId,
                                note = "[Scheduled] ${recurring.recurring.note}",
                                timestamp = selectedDate!!.time
                            ),
                            categoryName = recurring.categoryName,
                            categoryType = recurring.recurring.type.toCategoryType(),
                            accountName = recurring.accountName,
                            accountEmoji = recurring.accountEmoji
                        )
                    )
                }
            }
        }

        list.let { sortedList ->
            when (sortSetting) {
                TransactionSortSetting.NEWEST -> sortedList.sortedByDescending { it.transaction.timestamp }
                TransactionSortSetting.OLDEST -> sortedList.sortedBy { it.transaction.timestamp }
                TransactionSortSetting.AMOUNT_DESC -> sortedList.sortedByDescending { kotlin.math.abs(it.transaction.amount) }
                TransactionSortSetting.AMOUNT_ASC -> sortedList.sortedBy { kotlin.math.abs(it.transaction.amount) }
            }
        }
    }

    val listState = rememberLazyListState()
    val fabVisibleState = LocalFabVisible.current

    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousScrollOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (currentIndex, currentOffset) ->
                if (currentIndex > previousIndex || (currentIndex == previousIndex && currentOffset > previousScrollOffset)) {
                    fabVisibleState.value = false
                } else if (currentIndex < previousIndex || currentOffset < previousScrollOffset) {
                    fabVisibleState.value = true
                }
                previousIndex = currentIndex
                previousScrollOffset = currentOffset
            }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.widthIn(max = 800.dp).fillMaxSize()) {
            // Premium Search & Filter Bar
            SearchBarRow(
                query = searchQuery,
                onQueryChange = { 
                    searchQuery = it
                    scope.launch { settingsRepository.setTransactionsQuery(it) }
                },
                onFilterClick = { showFilterSheet = true },
                onCalendarToggle = { isCalendarVisible = !isCalendarVisible },
                isCalendarActive = isCalendarVisible || selectedDate != null,
                activeFilterCount = getActiveFilterCount(transactionFilter, dateRangeDays)
            )

            AnimatedVisibility(
                visible = isCalendarVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                CalendarSection(
                    currentMonthDate = currentDisplayMonth,
                    selectedDate = selectedDate,
                    onDateSelected = { 
                        selectedDate = if (selectedDate == it) null else it 
                    },
                    onMonthChange = { currentDisplayMonth = it },
                    transactions = transactions,
                    recurringItems = recurringItems,
                    activityDays = activityDays
                )
            }

            if (selectedDate != null) {
                val dayFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { selectedDate = null },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(dayFormat.format(selectedDate!!), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 150.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredTransactions.isEmpty()) {
                    item {
                        EmptyStateCard()
                    }
                } else {
                    items(filteredTransactions, key = { it.transaction.id }) { item ->
                        TransactionCard(
                            item = item,
                            currencyCode = settings.currencyCode,
                            is24Hour = is24Hour,
                            onEdit = { onEditTransaction(item.transaction.id) },
                            onDelete = {
                                scope.launch { 
                                    repository.deleteTransaction(item.transaction)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        )
                    }
                }
            }
        }

        FadingLazyColumnScrollbar(
            listState = listState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .padding(end = 6.dp, top = 6.dp, bottom = 6.dp)
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            currentFilter = transactionFilter,
            onFilterChange = {
                transactionFilter = it
                scope.launch { settingsRepository.setTransactionsFilter(it) }
            },
            currentRange = dateRangeDays,
            onRangeChange = {
                dateRangeDays = it
                scope.launch { settingsRepository.setTransactionsRangeDays(it) }
            },
            currentSort = sortSetting,
            onSortChange = {
                sortSetting = it
                scope.launch { settingsRepository.setTransactionsSort(it) }
            },
            onDismiss = { showFilterSheet = false },
            onReset = {
                transactionFilter = TransactionFilterSetting.ALL
                dateRangeDays = 30
                sortSetting = TransactionSortSetting.NEWEST
                scope.launch {
                    settingsRepository.setTransactionsFilter(TransactionFilterSetting.ALL)
                    settingsRepository.setTransactionsRangeDays(30)
                    settingsRepository.setTransactionsSort(TransactionSortSetting.NEWEST)
                }
            }
        )
    }
}

@Composable
private fun CalendarSection(
    currentMonthDate: Date,
    selectedDate: Date?,
    onDateSelected: (Date) -> Unit,
    onMonthChange: (Date) -> Unit,
    transactions: List<TransactionWithCategory>,
    recurringItems: List<com.v94studio.moneymanager.data.RecurringWithCategory> = emptyList(),
    activityDays: List<com.v94studio.moneymanager.data.ActivityDay> = emptyList()
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    val cal = Calendar.getInstance().apply { 
                        time = currentMonthDate
                        add(Calendar.MONTH, -1)
                    }
                    onMonthChange(cal.time)
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    Text(
                        text = monthFormat.format(currentMonthDate),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { 
                            val today = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.time
                            onMonthChange(today)
                            onDateSelected(Date())
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(stringResource(R.string.label_today), style = MaterialTheme.typography.labelMedium)
                    }
                }
                
                IconButton(onClick = { 
                    val cal = Calendar.getInstance().apply { 
                        time = currentMonthDate
                        add(Calendar.MONTH, 1)
                    }
                    onMonthChange(cal.time)
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                }
            }
            
            CalendarGrid(
                currentMonthDate = currentMonthDate,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                transactions = transactions,
                recurringItems = recurringItems,
                activityDays = activityDays
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonthDate: Date,
    selectedDate: Date?,
    onDateSelected: (Date) -> Unit,
    transactions: List<TransactionWithCategory>,
    recurringItems: List<com.v94studio.moneymanager.data.RecurringWithCategory>,
    activityDays: List<com.v94studio.moneymanager.data.ActivityDay>
) {
    val cal = Calendar.getInstance().apply { time = currentMonthDate }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val offset = cal.get(Calendar.DAY_OF_WEEK) - 1

    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val isoSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = sdf.format(Date())
    val selectedStr = selectedDate?.let { sdf.format(it) }

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = FintechSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        for (row in 0..5) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (col in 0..6) {
                    val dayNumber = row * 7 + col - offset + 1
                    if (dayNumber in 1..daysInMonth) {
                        val dayDate = Calendar.getInstance().apply {
                            time = currentMonthDate
                            set(Calendar.DAY_OF_MONTH, dayNumber)
                        }.time
                        val dayStr = sdf.format(dayDate)
                        val isoDayStr = isoSdf.format(dayDate)
                        
                        val activity = activityDays.find { it.day == isoDayStr }
                        
                        CalendarDay(
                            dayNumber = dayNumber,
                            isSelected = dayStr == selectedStr,
                            isToday = dayStr == todayStr,
                            hasIncome = activity?.hasIncome ?: false,
                            hasExpense = activity?.hasExpense ?: false,
                            hasFutureRecurring = recurringItems.any {
                                isRecurringOnDate(it.recurring, dayDate)
                            },
                            onClick = { onDateSelected(dayDate) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
            if ((row * 7 + 7 - offset) >= daysInMonth) break
            else Spacer(modifier = Modifier.height(4.dp)) // Added spacing between rows
        }
    }
}

@Composable
private fun CalendarDay(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasIncome: Boolean,
    hasExpense: Boolean,
    hasFutureRecurring: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .aspectRatio(1f) // Changed to perfect square
            .padding(1.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
        
        // Dot indicators
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 2.dp)
                .height(4.dp)
                .fillMaxWidth()
        ) {
            if (hasIncome && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(FintechGreen)
                )
            }
            if (hasExpense && !isSelected) {
                if (hasIncome) Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(FintechRed)
                )
            }
            if (hasFutureRecurring && !isSelected) {
                if (hasIncome || hasExpense) Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}

@Composable
private fun SearchBarRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onCalendarToggle: () -> Unit,
    isCalendarActive: Boolean,
    activeFilterCount: Int
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            placeholder = { Text(stringResource(R.string.hint_search_transactions), style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = FintechSecondary) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    }
                }
            } else null,
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )

        FilledTonalIconButton(
            onClick = onCalendarToggle,
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isCalendarActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                contentColor = if (isCalendarActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(Icons.Default.CalendarMonth, null)
        }

        Box {
            FilledTonalIconButton(
                onClick = onFilterClick,
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Icon(Icons.Default.FilterList, null)
            }
            if (activeFilterCount > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Text(
                        text = activeFilterCount.toString(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    currentFilter: TransactionFilterSetting,
    onFilterChange: (TransactionFilterSetting) -> Unit,
    currentRange: Int,
    onRangeChange: (Int) -> Unit,
    currentSort: TransactionSortSetting,
    onSortChange: (TransactionSortSetting) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    AppBottomSheet(
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.title_filters), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = onReset,
                    colors = ButtonDefaults.textButtonColors(contentColor = FintechRed)
                ) {
                    Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.action_reset_all))
                }
            }

            FilterSection(title = stringResource(R.string.label_transaction_type)) {
                val types = listOf(
                    TransactionFilterSetting.ALL to stringResource(R.string.label_all),
                    TransactionFilterSetting.INCOME to stringResource(R.string.label_income),
                    TransactionFilterSetting.EXPENSE to stringResource(R.string.label_expenses)
                )
                FintechSegmentedControl(
                    options = types,
                    selectedOption = currentFilter,
                    onOptionSelect = onFilterChange
                )
            }

            FilterSection(title = stringResource(R.string.label_time_period)) {
                val ranges = listOf(7 to 7, 30 to 30, 90 to 90)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ranges.forEach { (days, _) ->
                        val selected = currentRange == days
                        FilterChip(
                            selected = selected,
                            onClick = { onRangeChange(days) },
                            label = { Text(stringResource(R.string.label_x_days_filter, days)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = null
                        )
                    }
                }
            }

            FilterSection(title = stringResource(R.string.label_sort_order)) {
                val sortByDate = currentSort == TransactionSortSetting.NEWEST || currentSort == TransactionSortSetting.OLDEST
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortPill(label = stringResource(R.string.label_date_sort), selected = sortByDate) {
                        val newSort = if (currentSort == TransactionSortSetting.AMOUNT_DESC || currentSort == TransactionSortSetting.AMOUNT_ASC) {
                             TransactionSortSetting.NEWEST 
                        } else currentSort
                        onSortChange(newSort)
                    }
                    SortPill(label = stringResource(R.string.label_amount_sort), selected = !sortByDate) {
                        val newSort = if (currentSort == TransactionSortSetting.NEWEST || currentSort == TransactionSortSetting.OLDEST) {
                            TransactionSortSetting.AMOUNT_DESC
                        } else currentSort
                        onSortChange(newSort)
                    }
                }
                
                Spacer(Modifier.height(4.dp))
                
                val isDescending = currentSort == TransactionSortSetting.NEWEST || currentSort == TransactionSortSetting.AMOUNT_DESC
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortPill(label = stringResource(R.string.label_descending), selected = isDescending) {
                        val newSort = if (sortByDate) TransactionSortSetting.NEWEST else TransactionSortSetting.AMOUNT_DESC
                        onSortChange(newSort)
                    }
                    SortPill(label = stringResource(R.string.label_ascending), selected = !isDescending) {
                        val newSort = if (sortByDate) TransactionSortSetting.OLDEST else TransactionSortSetting.AMOUNT_ASC
                        onSortChange(newSort)
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.action_show_results), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SortPill(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            containerColor = Color.Transparent,
            labelColor = FintechSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            selectedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = FintechSecondary, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun TransactionCard(
    item: com.v94studio.moneymanager.data.TransactionWithCategory,
    currencyCode: String?,
    is24Hour: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = item.transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) FintechGreen else FintechRed
    val isProjected = item.transaction.id == -1L

    Card(
        onClick = if (isProjected) ({}) else onEdit,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isProjected) 0.7f else 1f },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isProjected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) 
                            else MaterialTheme.colorScheme.surface
        ),
        elevation = if (isProjected) CardDefaults.cardElevation(defaultElevation = 0.dp) 
                    else CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = getLocalizedCategory(item.categoryName ?: "Uncategorized", LocalContext.current),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        if (isProjected) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.title_upcoming).uppercase(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDateTime(item.transaction.timestamp, is24Hour),
                        style = MaterialTheme.typography.bodySmall,
                        color = FintechSecondary
                    )
                }
                
                AnimatedAmountText(
                    amount = if (isIncome) item.transaction.amount else -item.transaction.amount,
                    currencyCode = currencyCode,
                    color = if (isProjected) amountColor.copy(alpha = 0.6f) else amountColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    useSignedFormat = true
                )
            }

            if (item.transaction.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.transaction.note.replace("[Scheduled] ", ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
            }

            if (!isProjected) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FintechActionButton(
                        icon = Icons.Default.Edit,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = stringResource(R.string.cd_edit),
                        onClick = onEdit
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FintechActionButton(
                        icon = Icons.Default.Delete,
                        tint = FintechRed,
                        contentDescription = stringResource(R.string.cd_delete),
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.FilterList,
                null,
                modifier = Modifier.size(48.dp),
                tint = FintechSecondary.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.msg_no_transactions),
                style = MaterialTheme.typography.titleMedium,
                color = FintechSecondary
            )
            Text(
                stringResource(R.string.msg_adjust_filters),
                style = MaterialTheme.typography.bodySmall,
                color = FintechSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

private fun getActiveFilterCount(filter: TransactionFilterSetting, range: Int): Int {
    var count = 0
    if (filter != TransactionFilterSetting.ALL) count++
    if (range != 30) count++
    return count
}

private fun isRecurringOnDate(recurring: com.v94studio.moneymanager.data.RecurringEntity, date: Date): Boolean {
    // Normalize target date to midnight
    val targetCal = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    // Normalize recurring start date to midnight
    val startCal = Calendar.getInstance().apply {
        timeInMillis = recurring.nextRunAt
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (targetCal.timeInMillis == startCal.timeInMillis) return true
    
    if (targetCal.timeInMillis > startCal.timeInMillis) {
        val diffMillis = targetCal.timeInMillis - startCal.timeInMillis
        val diffDays = diffMillis / (24 * 60 * 60 * 1000)
        return diffDays % recurring.intervalDays == 0L
    }
    
    return false
}

private fun com.v94studio.moneymanager.data.TransactionType.toCategoryType(): com.v94studio.moneymanager.data.CategoryType {
    return if (this == com.v94studio.moneymanager.data.TransactionType.INCOME) 
        com.v94studio.moneymanager.data.CategoryType.INCOME 
    else 
        com.v94studio.moneymanager.data.CategoryType.EXPENSE
}
