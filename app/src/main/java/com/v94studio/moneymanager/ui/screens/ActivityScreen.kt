package com.v94studio.moneymanager.ui.screens

import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v94studio.moneymanager.LocalRepository
import com.v94studio.moneymanager.LocalSettingsRepository
import com.v94studio.moneymanager.LocalUserSettings
import com.v94studio.moneymanager.R
import com.v94studio.moneymanager.data.RecurringWithCategory
import com.v94studio.moneymanager.data.TransactionType
import com.v94studio.moneymanager.data.TransactionWithCategory
import com.v94studio.moneymanager.data.isRecurringOnDate
import com.v94studio.moneymanager.data.toCategoryType
import com.v94studio.moneymanager.ui.components.*
import com.v94studio.moneymanager.ui.settings.TransactionFilterSetting
import com.v94studio.moneymanager.ui.theme.*
import com.v94studio.moneymanager.ui.util.*
import com.v94studio.moneymanager.ui.components.featurediscovery.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActivityScreen(
    onEditTransaction: (Long) -> Unit,
    onViewReport: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    transactions: List<TransactionWithCategory> = emptyList()
) {
    val repository = LocalRepository.current
    val settings = LocalUserSettings.current
    val recurringItems by repository.observeRecurring().collectAsState()
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    val scope = rememberCoroutineScope()
    val featureDiscoveryViewModel = com.v94studio.moneymanager.LocalFeatureDiscovery.current
    val featureDiscoveryState by featureDiscoveryViewModel.uiState.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(TransactionFilterSetting.ALL) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    
    var openTransactionId by remember { mutableStateOf<Long?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionWithCategory?>(null) }
    var knownTransactionIds by remember { mutableStateOf(transactions.map { it.transaction.id }.toSet()) }
    var newlyAppearedTransactionIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val bottomPadding = com.v94studio.moneymanager.LocalBottomPadding.current

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) openTransactionId = null
            }
    }

    LaunchedEffect(transactions, featureDiscoveryState.isLoaded) {
        if (featureDiscoveryState.isLoaded && transactions.size == 1) {
            featureDiscoveryViewModel.showSwipeTutorial()
        }
    }

    LaunchedEffect(transactions.map { it.transaction.id }) {
        val currentIds = transactions.asSequence().map { it.transaction.id }.filter { it > 0 }.toSet()
        newlyAppearedTransactionIds = currentIds - knownTransactionIds
        knownTransactionIds = knownTransactionIds + currentIds
    }
    
    val displayData = transactions

    val filteredTransactions = remember(displayData, recurringItems, selectedFilter, selectedDate, searchQuery) {
        val now = System.currentTimeMillis()

        val activeSelectedDate = selectedDate
        val dateBounds = if (activeSelectedDate != null) {
            val cal = Calendar.getInstance().apply { time = activeSelectedDate }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            start until cal.timeInMillis
        } else null

        val list = displayData.filter { item ->
            val matchesFilter = when (selectedFilter) {
                TransactionFilterSetting.ALL -> true
                TransactionFilterSetting.INCOME -> item.transaction.type == TransactionType.INCOME
                TransactionFilterSetting.EXPENSE -> item.transaction.type == TransactionType.EXPENSE
            }
            val matchesDate = dateBounds == null || item.transaction.timestamp in dateBounds
            val matchesQuery = searchQuery.isEmpty() || 
                (item.categoryName ?: "").contains(searchQuery, ignoreCase = true) || 
                item.transaction.note.contains(searchQuery, ignoreCase = true)
            
            matchesFilter && matchesDate && matchesQuery
        }.toMutableList()

        if (selectedDate != null && selectedDate!!.time > now) {
            recurringItems.forEach { recurring ->
                if (isRecurringOnDate(recurring.recurring, selectedDate!!)) {
                    val matchesFilter = when (selectedFilter) {
                        TransactionFilterSetting.ALL -> true
                        TransactionFilterSetting.INCOME -> recurring.recurring.type == TransactionType.INCOME
                        TransactionFilterSetting.EXPENSE -> recurring.recurring.type == TransactionType.EXPENSE
                    }
                    if (matchesFilter) {
                        list.add(
                            TransactionWithCategory(
                                transaction = com.v94studio.moneymanager.data.TransactionEntity(
                                    id = - (recurring.recurring.id + 1000000L),
                                    amountMinor = com.v94studio.moneymanager.data.amountToMinor(recurring.recurring.amount),
                                    type = recurring.recurring.type,
                                    categoryId = recurring.recurring.categoryId,
                                    accountId = recurring.recurring.accountId,
                                    note = "[Upcoming] ${recurring.recurring.note}",
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
        }
        list.sortedByDescending { it.transaction.timestamp }
    }

    val groupedTransactions by remember(filteredTransactions) {
        derivedStateOf {
            val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            filteredTransactions.groupBy {
                dateFormat.format(Date(it.transaction.timestamp))
            }.toList()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MoneyTopAppBar(
                title = "Activity",
                subtitle = "Track your transactions and trends",
                scrollBehavior = scrollBehavior,
                bottomContent = {
                    FilterChips(selectedFilter) { selectedFilter = it }
                }
            )
        }
    ) { padding ->
        val topPadding = padding.calculateTopPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    if (openTransactionId != null) {
                        openTransactionId = null
                    }
                }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = (topPadding - 60.dp).coerceAtLeast(0.dp) + 28.dp,
                    bottom = bottomPadding + 100.dp
                )
            ) {
                item(key = "overview", contentType = "overview") {
                    MonthlyOverviewCard(filteredTransactions, recurringItems, settings.currencyCode, onViewReport)
                }

                item(key = "date_selector", contentType = "date_selector") {
                    WeeklyDateSelector(
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = if (selectedDate == it) null else it },
                        transactions = transactions
                    )
                }

                item(key = "date_spacer") {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                groupedTransactions.forEachIndexed { groupIndex, (date, dayTransactions) ->
                    item(key = "header_$date", contentType = "header") {
                        TimelineDateHeader(date, isFirst = groupIndex == 0)
                    }
                    
                    itemsIndexed(
                        items = dayTransactions,
                        key = { _, it -> "tx_${it.transaction.id}" },
                        contentType = { _, _ -> "transaction" }
                    ) { itemIndex, tx ->
                        val isFirstItem = groupIndex == 0 && itemIndex == 0
                        val isUpcoming = tx.transaction.id < 0
                        AnimatedActivityTransactionEntry(
                            transactionId = tx.transaction.id,
                            animateEntrance = tx.transaction.id in newlyAppearedTransactionIds
                        ) {
                        SwipeableTransactionItem(
                            item = tx,
                            currencyCode = settings.currencyCode,
                            is24Hour = is24Hour,
                            isOpen = openTransactionId == tx.transaction.id,
                            onOpen = { openTransactionId = tx.transaction.id },
                            onClose = { openTransactionId = null },
                            onEdit = { onEditTransaction(tx.transaction.id) },
                            onDelete = { transactionToDelete = tx },
                            onTargetPositioned = if (isFirstItem) {
                                { featureDiscoveryViewModel.onTargetPositioned(TutorialType.SWIPE, it) }
                            } else null,
                            isSwipeEnabled = !isUpcoming
                        )
                        }
                    }
                }

                if (groupedTransactions.isEmpty()) {
                    item(key = "empty", contentType = "empty") {
                        EmptyState()
                    }
                }
            }
            
            val tooltipData = when (featureDiscoveryState.currentTutorial) {
                TutorialType.FAB -> "Create Transaction" to "Tap here to create your first income or expense transaction."
                TutorialType.SWIPE -> "Swipe to Manage" to "Swipe left to edit or delete any transaction."
                else -> "" to ""
            }

            FeatureDiscoveryOverlay(
                isVisible = featureDiscoveryState.isVisible,
                targetRect = featureDiscoveryState.targetRect,
                title = tooltipData.first,
                description = tooltipData.second,
                onDismiss = { featureDiscoveryViewModel.dismissTutorial() },
                shape = if (featureDiscoveryState.currentTutorial == TutorialType.SWIPE) DiscoveryShape.Rectangle else DiscoveryShape.Circle
            )
        }
    }

    if (transactionToDelete != null) {
        val toDelete = transactionToDelete!!
        com.v94studio.moneymanager.ui.components.PremiumAlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction?") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                repository.deleteTransaction(toDelete.transaction)
                                openTransactionId = null
                            } catch (e: Exception) {
                                // Log or handle
                            } finally {
                                transactionToDelete = null
                            }
                        }
                    },
                    modifier = Modifier.width(104.dp).height(36.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FintechRed),
                    border = BorderStroke(1.dp, FintechRed.copy(alpha = 0.7f)),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { transactionToDelete = null },
                    modifier = Modifier.width(104.dp).height(36.dp),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AnimatedActivityTransactionEntry(
    transactionId: Long,
    animateEntrance: Boolean,
    content: @Composable () -> Unit
) {
    var visible by remember(transactionId) { mutableStateOf(!animateEntrance) }

    LaunchedEffect(transactionId, animateEntrance) {
        if (animateEntrance) visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(420, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(500, easing = FastOutSlowInEasing)) { it / 3 } +
            scaleIn(tween(420, easing = FastOutSlowInEasing), initialScale = 0.96f)
    ) {
        content()
    }
}

@Composable
private fun TimelineDateHeader(date: String, isFirst: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.5f)
                        .width(2.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        .align(Alignment.TopCenter)
                )
            }
            
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .shadow(4.dp, CircleShape, spotColor = BrandPurple)
                    .background(Brush.linearGradient(listOf(Color(0xFF7C4DFF), Color(0xFFA855F7))), CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = date,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SwipeableTransactionItem(
    item: TransactionWithCategory,
    currencyCode: String?,
    is24Hour: Boolean,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTargetPositioned: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    isSwipeEnabled: Boolean = true
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { 160.dp.toPx() }
    
    val offsetX = remember { Animatable(if (isOpen) -actionWidthPx else 0f) }
    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(isOpen) {
        if (isOpen) offsetX.animateTo(-actionWidthPx)
        else offsetX.animateTo(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }

        if (isSwipeEnabled) {
            Row(
                modifier = Modifier
                    .padding(start = 44.dp, top = 8.dp, bottom = 8.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp)),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(80.dp)
                        .background(BrandPurple)
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, "Edit", tint = Color.White)
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(80.dp)
                        .background(FintechRed.copy(alpha = 0.8f))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.White)
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(isSwipeEnabled) {
                    if (!isSwipeEnabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < -actionWidthPx * 0.4f) {
                                    offsetX.animateTo(-actionWidthPx)
                                    onOpen()
                                } else {
                                    offsetX.animateTo(0f)
                                    onClose()
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (offsetX.value + dragAmount).coerceIn(-actionWidthPx, 0f)
                            if (newOffset < -actionWidthPx * 0.8f && offsetX.value >= -actionWidthPx * 0.8f) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            }
                            scope.launch { offsetX.snapTo(newOffset) }
                        }
                    )
                }
                .fillMaxWidth()
                .padding(start = 44.dp, top = 8.dp, bottom = 8.dp)
                .shadow(2.dp, RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .then(
                    if (onTargetPositioned != null) {
                        Modifier.discoverable(onTargetPositioned)
                    } else Modifier
                )
                .clickable {
                    if (isOpen) {
                        scope.launch { 
                            offsetX.animateTo(0f)
                            onClose()
                        }
                    } else {
                        onClose() // Signal to close other items if needed
                    }
                }
        ) {
            TimelineTransactionItem(item, currencyCode, is24Hour)
        }
    }
}

@Composable
private fun TimelineTransactionItem(
    item: TransactionWithCategory,
    currencyCode: String?,
    is24Hour: Boolean
) {
    val isIncome = item.transaction.type == TransactionType.INCOME
    val categoryName = item.categoryName ?: "General"
    val emoji = getCategoryEmoji(categoryName)
    val categoryColor = getCategoryColor(categoryName)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(categoryColor.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 24.sp)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.accountEmoji ?: "🏦",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = item.accountName ?: "Unknown Account",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.transaction.note.isNotEmpty()) {
                Text(
                    text = item.transaction.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Text(
            text = (if (isIncome) "+" else "-") + formatCurrency(item.transaction.amount, currencyCode),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isIncome) FintechGreen else FintechRed
        )
    }
}

@Composable
private fun FilterChips(
    selected: TransactionFilterSetting,
    onFilterSelected: (TransactionFilterSetting) -> Unit
) {
    val filters = listOf(
        TransactionFilterSetting.ALL to "All",
        TransactionFilterSetting.INCOME to "Income",
        TransactionFilterSetting.EXPENSE to "Expense"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (filter, label) ->
            val isSelected = selected == filter
            Surface(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onFilterSelected(filter) },
                color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.surface,
                shape = CircleShape,
                shadowElevation = if (isSelected) 0.dp else 1.dp
            ) {
                Box(
                    modifier = if (isSelected) {
                        Modifier.background(Brush.horizontalGradient(BrandPurpleGradient))
                    } else {
                        Modifier
                    }.padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyOverviewCard(
    transactions: List<TransactionWithCategory>,
    recurringItems: List<RecurringWithCategory>,
    currencyCode: String?,
    onViewReport: () -> Unit
) {
    val now = remember { Calendar.getInstance() }
    val monthName = remember(now) { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now.time) }
    
    val totals = remember(transactions, recurringItems, now) {
        val monthStart = (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val monthEnd = (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val currentMonthTransactions = transactions.filter {
            it.transaction.timestamp in monthStart.timeInMillis..monthEnd.timeInMillis
        }

        var income = currentMonthTransactions.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount }
        var expense = currentMonthTransactions.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }

        val todayStart = (Calendar.getInstance()).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val tempCal = todayStart.clone() as Calendar
        tempCal.add(Calendar.DAY_OF_YEAR, 1)
        
        while (!tempCal.after(monthEnd)) {
            val date = tempCal.time
            recurringItems.forEach { recurring ->
                if (isRecurringOnDate(recurring.recurring, date)) {
                    if (recurring.recurring.type == TransactionType.INCOME) {
                        income += recurring.recurring.amount
                    } else {
                        expense += recurring.recurring.amount
                    }
                }
            }
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        Triple(income, expense, income - expense)
    }

    val (income, expense, net) = totals

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(text = monthName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Financial summary for this month", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(
                    onClick = onViewReport,
                    modifier = Modifier.offset(x = 10.dp)
                ) {
                    Text("View Report", color = BrandPurple)
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(12.dp), tint = BrandPurple)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp) // Added horizontal padding to prevent overlap
            ) {
                MetricColumn("Income", income, FintechGreen, currencyCode, Modifier.weight(1f))
                VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 8.dp), thickness = 0.5.dp)
                MetricColumn("Expenses", -expense, FintechRed, currencyCode, Modifier.weight(1f))
                VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 8.dp), thickness = 0.5.dp)
                MetricColumn("Net", net, BrandPurple, currencyCode, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = FintechGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You're saving more than last month",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, amount: Double, color: Color, currencyCode: String?, modifier: Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BounceMarqueeText(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BounceMarqueeText(
            text = formatCurrency(abs(amount), currencyCode),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeeklyDateSelector(
    selectedDate: Date?,
    onDateSelected: (Date) -> Unit,
    transactions: List<TransactionWithCategory>
) {
    val dateRange = remember {
        val start = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }
        val end = Calendar.getInstance().apply { add(Calendar.MONTH, 6) }
        val dates = mutableListOf<Date>()
        while (start.before(end)) {
            dates.add(start.time)
            start.add(Calendar.DAY_OF_YEAR, 1)
        }
        dates
    }

    val today = remember { Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time }
    
    val todayIndex = remember(dateRange) {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayStr = sdf.format(today)
        dateRange.indexOfFirst { sdf.format(it) == todayStr }.coerceAtLeast(0)
    }

    val listState = com.v94studio.moneymanager.LocalActivityCalendarListState.current
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    var lastActiveDir by remember { mutableIntStateOf(0) }

    val todayOffscreenDir by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) lastActiveDir
            else {
                val currentDir = when {
                    todayIndex < visibleItems.first().index -> -1
                    todayIndex > visibleItems.last().index -> 1
                    else -> 0
                }
                if (currentDir != 0) {
                    lastActiveDir = currentDir
                }
                currentDir
            }
        }
    }

    LaunchedEffect(todayOffscreenDir) {
        if (todayOffscreenDir != 0) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    scope.launch {
                        val target = (listState.firstVisibleItemIndex - 7).coerceAtLeast(0)
                        listState.animateScrollToItem(target)
                    }
                },
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = BrandPurple)
            }

            LazyRow(
                state = listState,
                flingBehavior = snapFlingBehavior,
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(dateRange) { date ->
                    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    val isSelected = selectedDate != null && sdf.format(date) == sdf.format(selectedDate)
                    val isToday = sdf.format(date) == sdf.format(today)

                    val hasActivity = transactions.any {
                        sdf.format(Date(it.transaction.timestamp)) == sdf.format(date)
                    }

                    DateItem(date, isSelected, isToday, hasActivity) { onDateSelected(date) }
                }
            }

            IconButton(
                onClick = {
                    scope.launch {
                        val target = (listState.firstVisibleItemIndex + 7).coerceAtMost(dateRange.size - 1)
                        listState.animateScrollToItem(target)
                    }
                },
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BrandPurple)
            }
        }

        AnimatedVisibility(
            visible = todayOffscreenDir != 0,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Surface(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem((todayIndex - 3).coerceAtLeast(0))
                        }
                    },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(if (lastActiveDir < 0) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(horizontal = 56.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (lastActiveDir < 0) Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, modifier = Modifier.size(14.dp))
                        Text("Today", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        if (lastActiveDir > 0) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DateItem(date: Date, isSelected: Boolean, isToday: Boolean, hasActivity: Boolean, onClick: () -> Unit) {
    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
    val dayNumber = SimpleDateFormat("d", Locale.getDefault()).format(date)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(55.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isSelected) Modifier.background(Brush.horizontalGradient(BrandPurpleGradient))
                else if (isToday) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                else Modifier.background(MaterialTheme.colorScheme.surface)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = dayName, 
            style = MaterialTheme.typography.labelSmall, 
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dayNumber, 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(modifier = Modifier.height(4.dp)) {
            if (hasActivity) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else BrandPurple)
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Inbox, 
            contentDescription = null, 
            modifier = Modifier.size(64.dp), 
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No activity found", 
            color = MaterialTheme.colorScheme.onSurfaceVariant, 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Add your first transaction to see a preview of your financial activity here.", 
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
