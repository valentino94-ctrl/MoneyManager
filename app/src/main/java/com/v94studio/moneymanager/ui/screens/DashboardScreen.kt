package com.v94studio.moneymanager.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import com.v94studio.moneymanager.LocalRepository
import com.v94studio.moneymanager.LocalUserSettings
import com.v94studio.moneymanager.LocalFabVisible
import com.v94studio.moneymanager.LocalFeatureDiscovery
import com.v94studio.moneymanager.R
import com.v94studio.moneymanager.data.*
import com.v94studio.moneymanager.ui.components.*
import com.v94studio.moneymanager.ui.settings.ThemeMode
import com.v94studio.moneymanager.ui.theme.*
import com.v94studio.moneymanager.ui.util.*
import com.v94studio.moneymanager.ui.components.featurediscovery.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    onViewAllTransactions: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val repository = LocalRepository.current
    val settings = LocalUserSettings.current
    
    val rollingDays = settings.rollingDays
    val nowMillis = remember { System.currentTimeMillis() }
    val monthStart = remember(nowMillis) { startOfMonthMillis(nowMillis) }

    val overviewFlow = remember(repository, monthStart) { repository.observeDashboardOverview(monthStart) }
    val overviewData by overviewFlow.collectAsState(initial = null)
    
    val monthIncomeValue by remember { derivedStateOf { overviewData?.monthIncome ?: 0.0 } }
    val monthExpenseValue by remember { derivedStateOf { overviewData?.monthExpense ?: 0.0 } }
    val hasTransactions by remember {
        derivedStateOf {
            overviewData?.let { it.totalIncome != 0.0 || it.totalExpense != 0.0 } == true
        }
    }
    
    var savedTotalBalance by rememberSaveable { mutableDoubleStateOf(0.0) }
    val accountsWithBalances by repository.observeAccountsWithBalances().collectAsState(initial = emptyList())
    
    val totalBalance by remember {
        derivedStateOf {
            if (accountsWithBalances.isEmpty()) savedTotalBalance else accountsWithBalances.sumOf { it.balance }
        }
    }

    LaunchedEffect(accountsWithBalances) {
        if (accountsWithBalances.isNotEmpty()) {
            savedTotalBalance = accountsWithBalances.sumOf { it.balance }
        }
    }

    val savingsRate by remember {
        derivedStateOf {
            if (monthIncomeValue <= 0.0) 0.0 else (monthIncomeValue - monthExpenseValue) / monthIncomeValue
        }
    }

    var trendDays by rememberSaveable { mutableIntStateOf(7) }
    val trendSince = remember(trendDays) { nowMillis - daysToMillis(trendDays) }
    
    val expenseTrendFlow = remember(repository, trendSince) {
        repository.observeDailyTotalsSince(TransactionType.EXPENSE, trendSince)
    }
    val incomeTrendFlow = remember(repository, trendSince) {
        repository.observeDailyTotalsSince(TransactionType.INCOME, trendSince)
    }
    val expenseTrend by expenseTrendFlow.collectAsState(initial = emptyList())
    val incomeTrend by incomeTrendFlow.collectAsState(initial = emptyList())
    
    val dayBuckets = remember(trendDays) { buildDayBuckets(trendDays, nowMillis) }
    
    val expenseSeries by remember {
        derivedStateOf {
            dayBuckets.map { bucket -> expenseTrend.firstOrNull { it.day == bucket.key }?.total ?: 0.0 }
        }
    }
    val incomeSeries by remember {
        derivedStateOf {
            dayBuckets.map { bucket -> incomeTrend.firstOrNull { it.day == bucket.key }?.total ?: 0.0 }
        }
    }
    val trendLabels = remember(dayBuckets) { dayBuckets.map { it.label } }

    val categorySpendFlow = remember(repository, trendSince) {
        repository.observeCategorySpendNamedSince(TransactionType.EXPENSE, trendSince)
    }
    val incomeCategorySpendFlow = remember(repository, trendSince) {
        repository.observeCategorySpendNamedSince(TransactionType.INCOME, trendSince)
    }
    val categorySpend by categorySpendFlow.collectAsState(initial = emptyList())
    val incomeCategorySpend by incomeCategorySpendFlow.collectAsState(initial = emptyList())

    val monthBuckets = remember(nowMillis) { buildMonthBuckets(3, nowMillis) }
    val monthSince = remember(monthBuckets) { monthBuckets.firstOrNull()?.startMillis ?: nowMillis }
    
    val monthlyTotalsFlow = remember(repository, monthSince) {
        repository.observeMonthlyTotalsSince(TransactionType.EXPENSE, monthSince)
    }
    val incomeMonthlyTotalsFlow = remember(repository, monthSince) {
        repository.observeMonthlyTotalsSince(TransactionType.INCOME, monthSince)
    }
    val monthlyTotals by monthlyTotalsFlow.collectAsState(initial = emptyList())
    val incomeMonthlyTotals by incomeMonthlyTotalsFlow.collectAsState(initial = emptyList())
    
    val monthlySeries by remember {
        derivedStateOf {
            monthBuckets.map { bucket -> monthlyTotals.firstOrNull { it.month == bucket.key }?.total ?: 0.0 }
        }
    }
    val incomeMonthlySeries by remember {
        derivedStateOf {
            monthBuckets.map { bucket -> incomeMonthlyTotals.firstOrNull { it.month == bucket.key }?.total ?: 0.0 }
        }
    }

    val budgetsFlow = remember(repository, nowMillis) { repository.observeBudgetProgress(nowMillis) }
    val budgets by budgetsFlow.collectAsState(initial = emptyList())
    
    val totalBudgetLimit by remember { derivedStateOf { budgets.sumOf { it.limit } } }
    val perDayBudget by remember {
        derivedStateOf {
            if (rollingDays <= 0 || totalBudgetLimit <= 0.0) 0.0 else totalBudgetLimit / rollingDays
        }
    }
    val budgetTargetSeries by remember {
        derivedStateOf {
            expenseSeries.map { perDayBudget }
        }
    }
    val monthBudgetSeries by remember {
        derivedStateOf {
            monthBuckets.map { bucket -> budgetForMonth(perDayBudget, bucket.startMillis) }
        }
    }
    val currentMonthLabel = remember(nowMillis) { SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(nowMillis)) }

    val recurring by repository.observeRecurring().collectAsState(initial = emptyList())
    val upcomingCutoff = remember(nowMillis) { nowMillis + daysToMillis(30) }
    val upcomingRecurring by remember {
        derivedStateOf {
            recurring.filter { it.recurring.nextRunAt <= upcomingCutoff }.sortedBy { it.recurring.nextRunAt }.take(5)
        }
    }
    val savingGoals by repository.observeSavingGoals().collectAsState(initial = emptyList())

    val lastMonthStart = remember(nowMillis) { startOfLastMonthMillis(nowMillis) }
    val lastMonthEnd = remember(nowMillis) { endOfLastMonthMillis(nowMillis) }
    val lastMonthExpenseFlow = remember(repository, lastMonthStart, lastMonthEnd) {
        repository.observeTotalExpenseBetween(lastMonthStart, lastMonthEnd)
    }
    val lastMonthExpense by lastMonthExpenseFlow.collectAsState(initial = 0.0)
    
    val last7Start = remember(nowMillis) { nowMillis - daysToMillis(7) }
    val prev7Start = remember(nowMillis) { nowMillis - daysToMillis(14) }
    val prev7End = remember(nowMillis) { nowMillis - daysToMillis(7) }
    
    val last7ExpenseFlow = remember(repository, last7Start, nowMillis) {
        repository.observeSumByTypeBetween(TransactionType.EXPENSE, last7Start, nowMillis)
    }
    val prev7ExpenseFlow = remember(repository, prev7Start, prev7End) {
        repository.observeSumByTypeBetween(TransactionType.EXPENSE, prev7Start, prev7End)
    }
    val last7Expense by last7ExpenseFlow.collectAsState(initial = 0.0)
    val prev7Expense by prev7ExpenseFlow.collectAsState(initial = 0.0)

    val topPadding = com.v94studio.moneymanager.LocalTopPadding.current
    val bottomPadding = com.v94studio.moneymanager.LocalBottomPadding.current
    val featureDiscoveryViewModel = LocalFeatureDiscovery.current
    val featureDiscoveryState by featureDiscoveryViewModel.uiState.collectAsState()

    var overviewRect by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(featureDiscoveryState.isLoaded, featureDiscoveryState.hasSeenDashboardTutorial) {
        if (featureDiscoveryState.isLoaded && !featureDiscoveryState.hasSeenDashboardTutorial) {
            featureDiscoveryViewModel.showDashboardIntro()
        }
    }

    LaunchedEffect(overviewRect, featureDiscoveryState.currentTutorial) {
        if (featureDiscoveryState.currentTutorial == TutorialType.DASHBOARD_INTRO && overviewRect != Rect.Zero) {
            featureDiscoveryViewModel.onTargetPositioned(TutorialType.DASHBOARD_INTRO, overviewRect)
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.last()
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 1
            }
        }
    }

    LaunchedEffect(isAtBottom, featureDiscoveryState.currentTutorial) {
        if (isAtBottom && featureDiscoveryState.currentTutorial == TutorialType.DASHBOARD_SCROLL_PENDING) {
            featureDiscoveryViewModel.showDashboardBottom()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.widthIn(max = 800.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp, 
                        top = (topPadding - 60.dp).coerceAtLeast(0.dp) + 45.dp, 
                        end = 16.dp, 
                        bottom = bottomPadding + 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "overview") {
                        Box(modifier = Modifier.discoverable { overviewRect = it }) {
                            OverviewGrid(
                                totalBalance = totalBalance,
                                monthIncome = monthIncomeValue,
                                monthExpense = monthExpenseValue,
                                savingsRate = savingsRate,
                                currencyCode = settings.currencyCode
                            )
                        }
                    }
                    item(key = "upcoming_header") { FintechSectionHeader(title = stringResource(R.string.title_upcoming), subtitle = stringResource(R.string.subtitle_bills_income)) }
                    item(key = "upcoming_card") { UpcomingRecurringCard(items = upcomingRecurring, currencyCode = settings.currencyCode) }
                    
                    if (savingGoals.isNotEmpty()) {
                        item(key = "savings_header") { FintechSectionHeader(title = stringResource(R.string.title_savings), subtitle = stringResource(R.string.subtitle_saving_progress)) }
                        item(key = "savings_card") {
                            DashboardSavingGoalsCard(goals = savingGoals, currencyCode = settings.currencyCode)
                        }
                    }

                    item(key = "charts_header") { FintechSectionHeader(title = stringResource(R.string.title_charts), subtitle = stringResource(R.string.subtitle_spending_patterns)) }
                    item(key = "charts_pager") {
                        val isDark = when (settings.themeMode) {
                            ThemeMode.SYSTEM -> isSystemInDarkTheme()
                            ThemeMode.DARK -> true
                            ThemeMode.LIGHT -> false
                        }
                        ChartsPager(
                            trendDays = trendDays,
                            onTrendDaysChange = { trendDays = it },
                            incomeSeries = incomeSeries,
                            expenseSeries = expenseSeries,
                            trendLabels = trendLabels,
                            incomeCategorySpend = incomeCategorySpend,
                            expenseCategorySpend = categorySpend,
                            currencyCode = settings.currencyCode,
                            monthLabels = monthBuckets.map { it.label },
                            incomeMonthlySeries = incomeMonthlySeries,
                            expenseMonthlySeries = monthlySeries,
                            tabContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else TabRowContainer,
                            tabIndicatorColor = if (isDark) MaterialTheme.colorScheme.primary else TabRowIndicator,
                            tabUnselectedColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else TabRowUnselected,
                            isDark = isDark
                        )
                    }
                    item(key = "insights_header") { FintechSectionHeader(title = stringResource(R.string.title_insights), subtitle = stringResource(R.string.subtitle_helpful_signals)) }
                    item(key = "insights_card") {
                        InsightsCard(
                            monthIncome = monthIncomeValue,
                            monthExpense = monthExpenseValue,
                            lastMonthExpense = lastMonthExpense,
                            savingsRate = savingsRate,
                            last7Expense = last7Expense ?: 0.0,
                            prev7Expense = prev7Expense ?: 0.0,
                            budgets = budgets,
                            topCategories = categorySpend,
                            upcoming = upcomingRecurring,
                            currencyCode = settings.currencyCode
                        )
                    }
                    item(key = "budget_header") { FintechSectionHeader(title = stringResource(R.string.title_budget_analytics), subtitle = stringResource(R.string.subtitle_track_allocate)) }
                    item(key = "budget_pager") {
                        BudgetChartsPager(
                            trendLabels = trendLabels,
                            actualSeries = expenseSeries,
                            targetSeries = budgetTargetSeries,
                            categorySpend = categorySpend,
                            monthLabel = currentMonthLabel,
                            monthLabels = monthBuckets.map { it.label },
                            monthActualSeries = monthlySeries,
                            monthBudgetSeries = monthBudgetSeries,
                            budgets = budgets,
                            currencyCode = settings.currencyCode
                        )
                    }
                }

                FadingLazyColumnScrollbar(
                    listState = listState,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight()
                        .padding(end = 6.dp, top = topPadding + 6.dp, bottom = bottomPadding + 6.dp)
                )
            }
        }

        if (featureDiscoveryState.isVisible && featureDiscoveryState.currentTutorial == TutorialType.DASHBOARD_INTRO) {
            FeatureDiscoveryOverlay(
                isVisible = true,
                targetRect = featureDiscoveryState.targetRect,
                title = "Financial Overview",
                description = if (hasTransactions) {
                    "The Dashboard gives you a snapshot of your current balance, monthly income, and expenses. Scroll down to explore your spending patterns."
                } else {
                    "The Dashboard gives you a snapshot of your finances. Add your first transaction to start seeing income, expenses, and spending patterns."
                },
                onDismiss = { featureDiscoveryViewModel.dismissTutorial() },
                shape = DiscoveryShape.Rectangle
            )
        } else if (featureDiscoveryState.isVisible && featureDiscoveryState.currentTutorial == TutorialType.DASHBOARD_BOTTOM) {
            FeatureDiscoveryOverlay(
                isVisible = true,
                targetRect = featureDiscoveryState.targetRect,
                title = "Create Transactions",
                description = "Ready to add your first entry? Switch to the Activity tab below to create new income or expense transactions.",
                onDismiss = { featureDiscoveryViewModel.dismissTutorial() },
                shape = DiscoveryShape.Circle
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChartsPager(
    trendDays: Int,
    onTrendDaysChange: (Int) -> Unit,
    incomeSeries: List<Double>,
    expenseSeries: List<Double>,
    trendLabels: List<String>,
    incomeCategorySpend: List<CategorySpendNamed>,
    expenseCategorySpend: List<CategorySpendNamed>,
    currencyCode: String?,
    monthLabels: List<String>,
    incomeMonthlySeries: List<Double>,
    expenseMonthlySeries: List<Double>,
    tabContainerColor: Color,
    tabIndicatorColor: Color,
    tabUnselectedColor: Color,
    isDark: Boolean
) {
    val pages = remember {
        listOf(
            "Trend",
            "Categories",
            "Monthly"
        )
    }
    val pagerState = rememberPagerState { pages.size }
    Column {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) { page ->
            when (page) {
                0 -> ChartCard(
                    title = stringResource(R.string.chart_income_expense_trend),
                    modifier = Modifier.fillMaxWidth().height(360.dp)
                ) {
                    val rangeOptions = remember { listOf(7 to "7", 30 to "30", 90 to "90") }
                    val selectedIndex = rangeOptions.indexOfFirst { it.first == trendDays }.coerceAtLeast(0)
                    TabRow(
                        selectedTabIndex = selectedIndex,
                        modifier = Modifier.clip(RoundedCornerShape(15.2.dp)).background(tabContainerColor),
                        containerColor = Color.Transparent,
                        contentColor = tabIndicatorColor,
                        indicator = { tabPositions ->
                            Box(
                                modifier = Modifier
                                    .tabIndicatorOffset(tabPositions[selectedIndex])
                                    .fillMaxHeight()
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(11.2.dp))
                                    .background(tabIndicatorColor)
                            )
                        },
                        divider = {}
                    ) {
                        rangeOptions.forEachIndexed { index, (days, label) ->
                            val isSelected = selectedIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .zIndex(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onTrendDaysChange(days) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 13.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else tabUnselectedColor
                                )
                                if (index < rangeOptions.lastIndex) {
                                    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.2f else 1f)
                                    Box(
                                        modifier = Modifier.align(Alignment.CenterEnd).height(22.8.dp).width(1.dp).background(dividerColor)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TrendLineChartDual(
                        incomeValues = incomeSeries,
                        expenseValues = expenseSeries,
                        labels = trendLabels,
                        incomeColor = if (isDark) Color(0xFF64FFDA) else MaterialTheme.colorScheme.primary,
                        expenseColor = if (isDark) Color(0xFFFF5252) else MaterialTheme.colorScheme.error,
                        incomeLabel = stringResource(R.string.label_income),
                        expenseLabel = stringResource(R.string.label_expenses)
                    )
                }
                1 -> ChartCard(
                    title = stringResource(R.string.chart_category_breakdown),
                    modifier = Modifier.fillMaxWidth().height(360.dp)
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(text = stringResource(R.string.label_income), style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        CategoryBreakdownCards(
                            items = incomeCategorySpend,
                            totalLabel = stringResource(R.string.label_income),
                            currencyCode = currencyCode,
                            colors = paletteFrom(MaterialTheme.colorScheme.primary),
                            maxItems = 10
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = stringResource(R.string.label_expenses), style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        CategoryBreakdownCards(
                            items = expenseCategorySpend,
                            totalLabel = stringResource(R.string.label_expenses),
                            currencyCode = currencyCode,
                            colors = paletteFrom(MaterialTheme.colorScheme.error),
                            maxItems = 10
                        )
                    }
                }
                else -> ChartCard(
                    title = stringResource(R.string.chart_monthly_comparison),
                    modifier = Modifier.fillMaxWidth().height(360.dp)
                ) {
                    MonthlyComparisonChart(
                        labels = monthLabels,
                        incomeValues = incomeMonthlySeries,
                        expenseValues = expenseMonthlySeries,
                        incomeColor = MaterialTheme.colorScheme.primary,
                        expenseColor = MaterialTheme.colorScheme.error,
                        incomeLabel = stringResource(R.string.label_income),
                        expenseLabel = stringResource(R.string.label_expenses)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            pages.forEachIndexed { index, _ ->
                val selected = pagerState.currentPage == index
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                    modifier = Modifier.padding(horizontal = 4.dp).size(if (selected) 8.dp else 6.dp)
                ) {}
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BudgetChartsPager(
    trendLabels: List<String>,
    actualSeries: List<Double>,
    targetSeries: List<Double>,
    categorySpend: List<CategorySpendNamed>,
    monthLabel: String,
    monthLabels: List<String>,
    monthActualSeries: List<Double>,
    monthBudgetSeries: List<Double>,
    budgets: List<BudgetProgress>,
    currencyCode: String?
) {
    val pages = remember {
        listOf(
            "Target",
            "Allocation",
            "Trends",
            "Progress"
        )
    }
    val pagerState = rememberPagerState { pages.size }
    Column {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) { page ->
            when (page) {
                0 -> ChartCard(
                    title = stringResource(R.string.chart_on_track),
                    modifier = Modifier.fillMaxWidth().height(360.dp)
                ) {
                    Text(
                        text = stringResource(R.string.chart_actual_vs_target),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TrendLineChartDual(
                        incomeValues = targetSeries,
                        expenseValues = actualSeries,
                        labels = trendLabels,
                        incomeColor = MaterialTheme.colorScheme.tertiary,
                        expenseColor = MaterialTheme.colorScheme.primary,
                        incomeLabel = stringResource(R.string.label_target),
                        expenseLabel = stringResource(R.string.label_actual)
                    )
                }
                1 -> ChartCard(
                    title = stringResource(R.string.chart_budget_allocation) + " ($monthLabel)",
                    modifier = Modifier.fillMaxWidth().height(360.dp)
                ) {
                    DonutChart(
                        items = categorySpend,
                        totalLabel = stringResource(R.string.label_total_spent),
                        currencyCode = currencyCode,
                        colors = dynamicDistributionPalette(
                            count = categorySpend.count { it.total > 0.0 }.coerceAtLeast(6),
                            colors = MaterialTheme.colorScheme
                        )
                    )
                }
                2 -> ChartCard(
                    title = stringResource(R.string.chart_monthly_trends),
                    modifier = Modifier.fillMaxWidth().height(360.dp)
                ) {
                    MonthlyComparisonChart(
                        labels = monthLabels,
                        incomeValues = monthBudgetSeries,
                        expenseValues = monthActualSeries,
                        incomeColor = MaterialTheme.colorScheme.tertiary,
                        expenseColor = MaterialTheme.colorScheme.primary,
                        incomeLabel = stringResource(R.string.label_target),
                        expenseLabel = stringResource(R.string.label_actual)
                    )
                }
                else -> ChartCard(
                    title = stringResource(R.string.chart_quick_look),
                    modifier = Modifier.fillMaxWidth().height(360.dp)
                ) {
                    BudgetProgressQuickLook(
                        budgets = budgets,
                        currencyCode = currencyCode
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            pages.forEachIndexed { index, _ ->
                val selected = pagerState.currentPage == index
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                    modifier = Modifier.padding(horizontal = 4.dp).size(if (selected) 8.dp else 6.dp)
                ) {}
            }
        }
    }
}

@Composable
private fun OverviewGrid(
    totalBalance: Double,
    monthIncome: Double,
    monthExpense: Double,
    savingsRate: Double,
    currencyCode: String?
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OverviewCard(title = stringResource(R.string.label_total_balance), amount = totalBalance, currencyCode = currencyCode, modifier = Modifier.weight(1f))
            OverviewCard(title = stringResource(R.string.label_month_income), amount = monthIncome, currencyCode = currencyCode, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OverviewCard(title = stringResource(R.string.label_month_expenses), amount = monthExpense, currencyCode = currencyCode, modifier = Modifier.weight(1f))
            OverviewCard(title = stringResource(R.string.label_savings_rate), percent = savingsRate, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun OverviewCard(
    title: String,
    modifier: Modifier = Modifier,
    amount: Double? = null,
    percent: Double? = null,
    currencyCode: String? = null
) {
    FintechCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            if (amount != null) {
                AnimatedAmountText(amount = amount, currencyCode = currencyCode, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            } else if (percent != null) {
                AnimatedPercentText(percent = percent, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FintechCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun UpcomingRecurringCard(
    items: List<RecurringWithCategory>,
    currencyCode: String?
) {
    val context = LocalContext.current
    FintechCard {
        Column(modifier = Modifier.padding(20.dp)) {
            if (items.isEmpty()) {
                Text(text = stringResource(R.string.msg_no_recurring), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = stringResource(R.string.msg_add_recurring_hint), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                return@Column
            }
            items.forEachIndexed { index, item ->
                val isIncome = item.recurring.type == TransactionType.INCOME
                val amountColor = if (isIncome) FintechGreen else FintechRed
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val emoji = getCategoryEmoji(item.categoryName)
                    val localizedName = getLocalizedCategory(item.categoryName ?: "Uncategorized", context)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "$emoji $localizedName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "${relativeDayLabel(context, item.recurring.nextRunAt)} • Every ${item.recurring.intervalDays} days", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AnimatedAmountText(amount = item.recurring.amount, currencyCode = currencyCode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = amountColor)
                }
                if (index != items.lastIndex) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DashboardSavingGoalsCard(
    goals: List<SavingGoalEntity>,
    currencyCode: String?
) {
    FintechCard {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            goals.take(3).forEachIndexed { index, goal ->
                val progress = if (goal.targetAmount <= 0.0) 0f else (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
                var lastSeenProgress by rememberSaveable(goal.id) { mutableStateOf<Float?>(null) }
                val anim = remember { Animatable(lastSeenProgress ?: 0f) }
                LaunchedEffect(progress) { if (lastSeenProgress != progress) { lastSeenProgress = progress; anim.animateTo(progress, animationSpec = tween(1000)) } else { anim.snapTo(progress) } }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = goal.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        AnimatedAmountText(amount = goal.savedAmount, currencyCode = currencyCode, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = FintechGreen)
                    }
                    LinearProgressIndicator(progress = { anim.value }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), trackColor = MaterialTheme.colorScheme.surfaceVariant, color = FintechGreen)
                }
                if (index != goals.take(3).lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
private fun TrendLineChartDual(
    incomeValues: List<Double>,
    expenseValues: List<Double>,
    labels: List<String>,
    incomeColor: Color,
    expenseColor: Color,
    incomeLabel: String = stringResource(R.string.label_income),
    expenseLabel: String = stringResource(R.string.label_expenses)
) {
    val dataHash = remember(incomeValues, expenseValues) { (incomeValues.hashCode() + expenseValues.hashCode()).toString() }
    var lastAnimatedHash by rememberSaveable { mutableStateOf("") }
    val animationProgress = remember { Animatable(if (lastAnimatedHash == dataHash) 1f else 0f) }
    LaunchedEffect(dataHash) {
        if (lastAnimatedHash != dataHash) {
            lastAnimatedHash = dataHash
            animationProgress.snapTo(0f)
            animationProgress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
        }
    }
    val maxValue = remember(incomeValues, expenseValues) { (incomeValues + expenseValues).maxOfOrNull { it }?.coerceAtLeast(1.0) ?: 1.0 }
    val isDark = isSystemInDarkTheme()
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.1f else 0.5f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
            val progress = animationProgress.value
            val leftPad = 12.dp.toPx(); val rightPad = 12.dp.toPx(); val topPad = 8.dp.toPx(); val bottomPad = 20.dp.toPx()
            val chartWidth = size.width - leftPad - rightPad; val chartHeight = size.height - topPad - bottomPad
            val count = incomeValues.size.coerceAtLeast(1)
            val stepX = if (count > 1) chartWidth / (count - 1) else 0f
            val baselineY = size.height - bottomPad
            drawLine(color = lineColor, start = Offset(leftPad, baselineY), end = Offset(size.width - rightPad, baselineY), strokeWidth = 2f, cap = StrokeCap.Round)
            fun buildPath(values: List<Double>): Path {
                val path = Path()
                values.forEachIndexed { index, value ->
                    val x = leftPad + index * stepX
                    val y = baselineY - ((value / maxValue).toFloat() * chartHeight * progress)
                    if (index == 0) path.moveTo(x, baselineY - (baselineY - y) * progress) else path.lineTo(x, y)
                }
                return path
            }
            if (incomeValues.isNotEmpty()) drawPath(path = buildPath(incomeValues), color = incomeColor.copy(alpha = if (isDark) 0.9f else progress), style = Stroke(width = 4f, cap = StrokeCap.Round))
            if (expenseValues.isNotEmpty()) drawPath(path = buildPath(expenseValues), color = expenseColor.copy(alpha = if (isDark) 0.9f else progress), style = Stroke(width = 4f, cap = StrokeCap.Round))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            val labelIndices = when {
                labels.size <= 5 -> labels.indices.toList()
                else -> (0..4).map { step ->
                    ((labels.lastIndex * step) / 4f).roundToInt()
                }.distinct()
            }
            labelIndices.forEachIndexed { visibleIndex, dataIndex ->
                Text(
                    text = labels[dataIndex],
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = when (visibleIndex) {
                        0 -> TextAlign.Start
                        labelIndices.lastIndex -> TextAlign.End
                        else -> TextAlign.Center
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = incomeLabel, color = incomeColor, style = MaterialTheme.typography.labelMedium)
            Text(text = expenseLabel, color = expenseColor, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DonutChart(
    items: List<CategorySpendNamed>,
    totalLabel: String,
    currencyCode: String?,
    colors: List<Color>
) {
    val total = items.sumOf { it.total }.coerceAtLeast(0.0)
    val dataHash = remember(items) { items.hashCode().toString() }
    var lastAnimatedHash by rememberSaveable { mutableStateOf("") }
    val animationProgress = remember { Animatable(if (lastAnimatedHash == dataHash) 1f else 0f) }
    LaunchedEffect(items) {
        if (lastAnimatedHash != dataHash) {
            lastAnimatedHash = dataHash
            animationProgress.snapTo(0f)
            animationProgress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
        }
    }
    val emptyColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    Row(modifier = Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(110.dp).weight(1f), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val progress = animationProgress.value
                val diameter = size.minDimension * 0.85f
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val sweepData = items.filter { it.total > 0.0 }
                var startAngle = -90f
                if (sweepData.isEmpty()) {
                    drawArc(color = emptyColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = Size(diameter, diameter), style = Stroke(width = 20f))
                } else {
                    sweepData.forEachIndexed { index, item ->
                        val sweep = (item.total / total * 360f * progress).toFloat()
                        drawArc(color = colors[index % colors.size], startAngle = startAngle, sweepAngle = sweep, useCenter = false, topLeft = topLeft, size = Size(diameter, diameter), style = Stroke(width = 20f, cap = StrokeCap.Round))
                        startAngle += sweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.label_total), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AnimatedAmountText(amount = total, currencyCode = currencyCode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
            items.filter { it.total > 0.0 }.sortedByDescending { it.total }.take(4).forEachIndexed { index, item ->
                val percent = if (total <= 0.0) 0.0 else item.total / total
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(colors[index % colors.size], CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val localizedName = getLocalizedCategory(item.categoryName, LocalContext.current)
                        Text(text = "${getCategoryEmoji(item.categoryName)} $localizedName", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row {
                            AnimatedPercentText(percent = percent, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = " " + stringResource(R.string.label_of_total), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCards(items: List<CategorySpendNamed>, totalLabel: String, currencyCode: String?, colors: List<Color>, maxItems: Int = 10) {
    val total = items.sumOf { it.total }.coerceAtLeast(0.0)
    val topItems = items.filter { it.total > 0.0 }.sortedByDescending { it.total }.take(maxItems)
    if (total <= 0.0 || topItems.isEmpty()) {
        Text(text = stringResource(R.string.label_no_data), style = MaterialTheme.typography.labelMedium)
        return
    }
    topItems.forEachIndexed { index, item ->
        val color = colors[index % colors.size]
        CategoryCardRow(name = item.categoryName, amount = item.total, currencyCode = currencyCode, percent = (item.total / total).coerceIn(0.0, 1.0), color = color, totalLabel = totalLabel)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CategoryCardRow(name: String, amount: Double, currencyCode: String?, percent: Double, color: Color, totalLabel: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) { Text(text = getCategoryEmoji(name), modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = color, style = MaterialTheme.typography.labelMedium) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        val localizedName = getLocalizedCategory(name, LocalContext.current)
                        Text(text = localizedName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Row {
                            AnimatedPercentText(percent = percent, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = " " + stringResource(R.string.label_of_total), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                AnimatedAmountText(amount = amount, currencyCode = currencyCode, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { percent.toFloat() }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun MonthlyComparisonChart(labels: List<String>, incomeValues: List<Double>, expenseValues: List<Double>, incomeColor: Color, expenseColor: Color, incomeLabel: String = stringResource(R.string.label_income), expenseLabel: String = stringResource(R.string.label_expenses)) {
    val dataHash = remember(labels, incomeValues, expenseValues) { (labels.hashCode() + incomeValues.hashCode() + expenseValues.hashCode()).toString() }
    var lastAnimatedHash by rememberSaveable { mutableStateOf("") }
    val animationProgress = remember { Animatable(if (lastAnimatedHash == dataHash) 1f else 0f) }
    LaunchedEffect(labels, incomeValues, expenseValues) {
        if (lastAnimatedHash != dataHash) {
            lastAnimatedHash = dataHash
            animationProgress.snapTo(0f)
            animationProgress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
        }
    }
    val maxValue = remember(incomeValues, expenseValues) { (incomeValues + expenseValues).maxOfOrNull { it }?.coerceAtLeast(1.0) ?: 1.0 }
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val progress = animationProgress.value
            val barCount = labels.size.coerceAtLeast(1); val gap = size.width * 0.15f; val totalGap = gap * (barCount + 1)
            val groupWidth = ((size.width - totalGap) / barCount).coerceAtLeast(12f); val barWidth = (groupWidth * 0.38f).coerceAtLeast(8f)
            val baselineY = size.height * 0.9f; val maxBarHeight = size.height * 0.75f
            labels.forEachIndexed { index, _ ->
                val groupX = gap + index * (groupWidth + gap)
                val incomeHeight = ((incomeValues.getOrNull(index) ?: 0.0) / maxValue).toFloat() * maxBarHeight * progress
                val expenseHeight = ((expenseValues.getOrNull(index) ?: 0.0) / maxValue).toFloat() * maxBarHeight * progress
                drawRoundRect(color = incomeColor.copy(alpha = progress), topLeft = Offset(groupX, baselineY - incomeHeight), size = Size(barWidth, incomeHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f))
                drawRoundRect(color = expenseColor.copy(alpha = progress), topLeft = Offset(groupX + barWidth + (groupWidth * 0.12f).coerceAtLeast(3f), baselineY - expenseHeight), size = Size(barWidth, expenseHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { index, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    val diff = (incomeValues.getOrNull(index) ?: 0.0) - (expenseValues.getOrNull(index) ?: 0.0)
                    Text(text = if (diff >= 0) stringResource(R.string.label_under_status) else stringResource(R.string.label_over_status), style = MaterialTheme.typography.labelSmall, color = if (diff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            LegendItem(color = incomeColor, label = incomeLabel); Spacer(modifier = Modifier.width(16.dp)); LegendItem(color = expenseColor, label = expenseLabel)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun InsightsCard(monthIncome: Double, monthExpense: Double, lastMonthExpense: Double, savingsRate: Double, last7Expense: Double, prev7Expense: Double, budgets: List<BudgetProgress>, topCategories: List<CategorySpendNamed>, upcoming: List<RecurringWithCategory>, currencyCode: String?) {
    val context = LocalContext.current
    val insights = remember(monthIncome, monthExpense, lastMonthExpense, savingsRate, last7Expense, prev7Expense, budgets, topCategories, upcoming, currencyCode) {
        val list = mutableListOf<InsightItem>()
        if (lastMonthExpense > 0) {
            val diff = (monthExpense - lastMonthExpense) / lastMonthExpense
            list += InsightItem(if (diff <= 0) context.getString(R.string.insight_tone_saving) else context.getString(R.string.insight_tone_spending), context.getString(R.string.insight_monthly_compare, formatPercent(abs(diff)), if (diff > 0) context.getString(R.string.label_more) else context.getString(R.string.label_less)))
        }
        if (monthIncome > 0.0) list += InsightItem(context.getString(R.string.title_savings), context.getString(if (savingsRate >= 0.2) R.string.insight_savings_positive else if (savingsRate >= 0.0) R.string.insight_savings_neutral else R.string.insight_savings_negative, formatPercent(savingsRate)))
        if (prev7Expense > 0.0) {
            val delta = (last7Expense - prev7Expense) / prev7Expense
            list += InsightItem(if (delta <= 0.0) context.getString(R.string.insight_tone_good) else context.getString(R.string.insight_tone_watch), context.getString(R.string.insight_7day_compare, formatPercent(abs(delta)), if (delta >= 0) context.getString(R.string.label_more) else context.getString(R.string.label_less)))
        }
        val totalBudgetLimit = budgets.sumOf { it.limit }; val totalBudgetSpent = budgets.sumOf { it.spent }
        if (totalBudgetLimit > 0.0) {
            val progress = totalBudgetSpent / totalBudgetLimit; val remaining = (totalBudgetLimit - totalBudgetSpent).coerceAtLeast(0.0)
            list += InsightItem(context.getString(R.string.title_budgets), if (progress > 1.0) context.getString(R.string.insight_budget_over, formatCurrency(totalBudgetSpent - totalBudgetLimit, currencyCode)) else if (progress >= 0.8) context.getString(R.string.insight_budget_near, formatCurrency(remaining, currencyCode)) else context.getString(R.string.insight_budget_normal, formatPercent(progress)))
        }
        topCategories.firstOrNull { it.total > 0.0 }?.let { list += InsightItem(context.getString(R.string.chart_category_breakdown), context.getString(R.string.insight_top_spend, getLocalizedCategory(it.categoryName, context), formatCurrency(it.total, currencyCode))) }
        upcoming.firstOrNull()?.let { list += InsightItem(context.getString(R.string.title_upcoming), context.getString(R.string.insight_upcoming_item, getLocalizedCategory(it.categoryName ?: context.getString(R.string.title_recurring), context), relativeDayLabel(context, it.recurring.nextRunAt))) }
        if (list.isEmpty()) list += InsightItem(context.getString(R.string.insight_tone_start), context.getString(R.string.insight_empty_state))
        list
    }
    Card { Column(modifier = Modifier.padding(16.dp)) { insights.take(5).forEachIndexed { index, item -> InsightRow(item = item); if (index != insights.take(5).lastIndex) Spacer(modifier = Modifier.height(12.dp)) } } }
}

@Composable
private fun InsightRow(item: InsightItem) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = CircleShape, modifier = Modifier.size(10.dp).padding(top = 5.dp)) {}
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(text = item.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun BudgetProgressQuickLook(budgets: List<BudgetProgress>, currencyCode: String?) {
    val topBudgets = budgets.filter { it.limit > 0.0 }.sortedByDescending { it.spent / it.limit }.take(6)
    if (topBudgets.isEmpty()) { Text(text = stringResource(R.string.msg_no_budgets), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); return }
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        topBudgets.forEach { budget ->
            val rawProgress = (budget.spent / budget.limit).toFloat().coerceIn(0f, 1.2f)
            val targetProgress = rawProgress.coerceAtMost(1f); val isOverBudget = budget.spent > budget.limit
            var lastSeenProgress by rememberSaveable(budget.budgetId) { mutableStateOf<Float?>(null) }
            val anim = remember { Animatable(lastSeenProgress ?: 0f) }
            LaunchedEffect(targetProgress) { if (lastSeenProgress != targetProgress) { lastSeenProgress = targetProgress; anim.animateTo(targetProgress, animationSpec = tween(1000)) } else { anim.snapTo(targetProgress) } }
            val statusColor = if (isOverBudget) FintechRed else if (rawProgress >= 0.8f) MaterialTheme.colorScheme.tertiary else FintechGreen
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = getLocalizedCategory(budget.categoryName, LocalContext.current), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = if (isOverBudget) stringResource(R.string.label_over_limit) else stringResource(R.string.label_used, formatPercent(rawProgress.toDouble())), style = MaterialTheme.typography.labelSmall, color = if (isOverBudget) FintechRed else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AnimatedAmountText(amount = budget.spent, currencyCode = currencyCode, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = statusColor)
                }
                LinearProgressIndicator(progress = { anim.value }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), trackColor = MaterialTheme.colorScheme.surfaceVariant, color = statusColor)
            }
        }
    }
}
