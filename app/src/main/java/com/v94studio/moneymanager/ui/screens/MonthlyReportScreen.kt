package com.v94studio.moneymanager.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v94studio.moneymanager.LocalRepository
import com.v94studio.moneymanager.LocalUserSettings
import com.v94studio.moneymanager.R
import com.v94studio.moneymanager.data.*
import com.v94studio.moneymanager.ui.components.*
import com.v94studio.moneymanager.ui.theme.*
import com.v94studio.moneymanager.ui.util.formatCurrency
import com.v94studio.moneymanager.ui.util.getCategoryColor
import com.v94studio.moneymanager.ui.util.getCategoryEmoji
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    onBack: () -> Unit
) {
    val repository = LocalRepository.current
    val settings = LocalUserSettings.current
    
    var selectedDate by remember { mutableStateOf(Calendar.getInstance().time) }
    var isVisible by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val monthStart = remember(selectedDate) { 
        Calendar.getInstance().apply { 
            time = selectedDate; set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) 
        }.timeInMillis 
    }
    val monthEnd = remember(selectedDate) { 
        Calendar.getInstance().apply { 
            time = selectedDate; set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) 
        }.timeInMillis 
    }
    val prevMonthStart = remember(selectedDate) { 
        Calendar.getInstance().apply { 
            time = selectedDate; add(Calendar.MONTH, -1); set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) 
        }.timeInMillis 
    }
    val prevMonthEnd = remember(selectedDate) { 
        Calendar.getInstance().apply { 
            time = selectedDate; set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); add(Calendar.MILLISECOND, -1) 
        }.timeInMillis 
    }

    val incomeTotal by repository.observeSumByTypeBetween(TransactionType.INCOME, monthStart, monthEnd).collectAsState(initial = 0.0)
    val expenseTotal by repository.observeSumByTypeBetween(TransactionType.EXPENSE, monthStart, monthEnd).collectAsState(initial = 0.0)
    val prevIncomeTotal by repository.observeSumByTypeBetween(TransactionType.INCOME, prevMonthStart, prevMonthEnd).collectAsState(initial = 0.0)
    val prevExpenseTotal by repository.observeSumByTypeBetween(TransactionType.EXPENSE, prevMonthStart, prevMonthEnd).collectAsState(initial = 0.0)

    val dailyIncomeSinceMonth by repository.observeDailyTotalsSince(TransactionType.INCOME, monthStart).collectAsState(initial = emptyList())
    val dailyExpenseSinceMonth by repository.observeDailyTotalsSince(TransactionType.EXPENSE, monthStart).collectAsState(initial = emptyList())
    val categorySpend by repository.observeCategorySpendNamedBetween(TransactionType.EXPENSE, monthStart, monthEnd).collectAsState(initial = emptyList())
    val monthEndKey = remember(monthEnd) { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(monthEnd)) }
    val dailyIncome = remember(dailyIncomeSinceMonth, monthEndKey) { dailyIncomeSinceMonth.filter { it.day <= monthEndKey } }
    val dailyExpense = remember(dailyExpenseSinceMonth, monthEndKey) { dailyExpenseSinceMonth.filter { it.day <= monthEndKey } }
    
    val allTransactions by repository.observeTransactions().collectAsState(initial = emptyList())
    val monthTransactions = remember(allTransactions, monthStart, monthEnd) {
        allTransactions.filter { it.transaction.timestamp in monthStart..monthEnd }
            .sortedByDescending { abs(it.transaction.amount) }
    }

    val activityDays by repository.observeActivityDays(monthStart, monthEnd).collectAsState(initial = emptyList())
    val budgetProgress by repository.observeBudgetProgress(monthEnd).collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val radialColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(radialColor.copy(alpha = 0.03f), Color.Transparent),
                    center = center,
                    radius = size.maxDimension
                )
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MoneyTopAppBar(
                    title = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedDate) + " Report",
                    subtitle = "Your financial overview for this month",
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        MoneyBackButton(label = "Activity", onClick = onBack)
                    }
                )
            }
        ) { padding ->
            val topPadding = padding.calculateTopPadding()
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = (topPadding - 60.dp).coerceAtLeast(0.dp) + 40.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        AnimatedUpwardItem(visible = isVisible, delay = 100) {
                            MonthPicker(
                                date = selectedDate,
                                onDateChange = { selectedDate = it }
                            )
                        }
                    }

                    item {
                        AnimatedUpwardItem(visible = isVisible, delay = 200) {
                            SummaryCard(
                                income = incomeTotal ?: 0.0,
                                expense = expenseTotal ?: 0.0,
                                currencyCode = settings.currencyCode
                            )
                        }
                    }

                    item {
                        AnimatedUpwardItem(visible = isVisible, delay = 300) {
                            BarChartCard(
                                dailyIncome = dailyIncome,
                                dailyExpense = dailyExpense,
                                selectedDate = selectedDate,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            )
                        }
                    }

                    item {
                        AnimatedUpwardItem(visible = isVisible, delay = 350) {
                            DonutChartCard(
                                categorySpend = categorySpend,
                                currencyCode = settings.currencyCode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            )
                        }
                    }

                    item {
                        AnimatedUpwardItem(visible = isVisible, delay = 400) {
                            InsightsSection(
                                income = incomeTotal ?: 0.0,
                                expense = expenseTotal ?: 0.0,
                                prevIncome = prevIncomeTotal ?: 0.0,
                                prevExpense = prevExpenseTotal ?: 0.0,
                                categorySpend = categorySpend,
                                currencyCode = settings.currencyCode
                            )
                        }
                    }

                    item {
                        AnimatedUpwardItem(visible = isVisible, delay = 500) {
                            TopTransactionsCard(
                                transactions = monthTransactions.take(5),
                                currencyCode = settings.currencyCode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            )
                        }
                    }

                    item {
                        AnimatedUpwardItem(visible = isVisible, delay = 550) {
                            HeatmapCard(
                                activityDays = activityDays,
                                selectedDate = selectedDate,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            )
                        }
                    }

                    item {
                        AnimatedUpwardItem(visible = isVisible, delay = 600) {
                            HealthScoreCard(
                                income = incomeTotal ?: 0.0,
                                expense = expenseTotal ?: 0.0,
                                budgetProgress = budgetProgress
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedUpwardItem(
    visible: Boolean,
    delay: Int,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = delay, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 40f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "offset"
    )

    Box(modifier = Modifier
        .graphicsLayer {
            this.alpha = alpha
            this.translationY = offsetY
        }
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthPicker(
    date: Date,
    onDateChange: (Date) -> Unit
) {
    val monthYear = remember(date) { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date) }
    var showDatePicker by remember { mutableStateOf(false) }
    val currentMonth = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
    val isCurrentMonth = remember(date, currentMonth) {
        val selected = Calendar.getInstance().apply { time = date }
        val current = Calendar.getInstance().apply { time = currentMonth }
        selected.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
            selected.get(Calendar.MONTH) == current.get(Calendar.MONTH)
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDateChange(Date(it)) }
                    showDatePicker = false
                }) { Text("Select date") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                val cal = Calendar.getInstance().apply { time = date; add(Calendar.MONTH, -1) }
                onDateChange(cal.time)
            },
            modifier = Modifier.size(44.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Surface(
            modifier = Modifier
                .height(46.dp)
                .shadow(4.dp, RoundedCornerShape(23.dp)),
            shape = RoundedCornerShape(23.dp),
            color = MaterialTheme.colorScheme.surface,
            onClick = { showDatePicker = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = monthYear, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        IconButton(
            onClick = {
                val cal = Calendar.getInstance().apply { time = date; add(Calendar.MONTH, 1) }
                onDateChange(cal.time)
            },
            modifier = Modifier.size(44.dp),
            enabled = !isCurrentMonth
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun SummaryCard(
    income: Double,
    expense: Double,
    currencyCode: String
) {
    val net = income - expense
    val isPositive = net >= 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Net Cash Flow",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = (if (net < 0) "-" else "+") + formatCurrency(abs(net), currencyCode),
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            color = if (isPositive) FintechGreen else FintechRed
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryMetricBox(
                label = "Total Income",
                amount = income,
                color = FintechGreen,
                currencyCode = currencyCode,
                modifier = Modifier.weight(1f)
            )
            SummaryMetricBox(
                label = "Total Expenses",
                amount = expense,
                color = FintechRed,
                currencyCode = currencyCode,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryMetricBox(
    label: String,
    amount: Double,
    color: Color,
    currencyCode: String,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatCurrency(amount, currencyCode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
private fun BarChartCard(
    dailyIncome: List<DailyTotal>,
    dailyExpense: List<DailyTotal>,
    selectedDate: Date,
    modifier: Modifier
) {
    val dataHash = remember(dailyIncome, dailyExpense, selectedDate) { dailyIncome.hashCode() * 31 + dailyExpense.hashCode() * 17 + selectedDate.hashCode() }
    var lastAnimatedHash by rememberSaveable { mutableStateOf<Int?>(null) }
    val chartAnimation = remember { Animatable(if (lastAnimatedHash == dataHash) 1f else 0f) }
    LaunchedEffect(dataHash) {
        if (lastAnimatedHash != dataHash) {
            lastAnimatedHash = dataHash
            chartAnimation.snapTo(0f)
            chartAnimation.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
        } else chartAnimation.snapTo(1f)
    }
    val chartProgress = chartAnimation.value

    val data = remember(dailyIncome, dailyExpense, selectedDate) {
        val cal = Calendar.getInstance().apply { time = selectedDate }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        val segments = 4
        val daysPerSegment = daysInMonth / segments
        
        List(segments) { i ->
            val startDay = i * daysPerSegment + 1
            val endDay = if (i == segments - 1) daysInMonth else (i + 1) * daysPerSegment
            
            var incomeSum = 0.0
            var expenseSum = 0.0
            
            for (d in startDay..endDay) {
                cal.set(Calendar.DAY_OF_MONTH, d)
                val key = sdf.format(cal.time)
                incomeSum += dailyIncome.find { it.day == key }?.total ?: 0.0
                expenseSum += dailyExpense.find { it.day == key }?.total ?: 0.0
            }
            incomeSum to expenseSum
        }
    }

    val maxVal = remember(data) { (data.flatMap { listOf(it.first, it.second) }.maxOrNull() ?: 1.0) * 1.2 }

    Card(
        modifier = modifier.height(292.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Income vs expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                ChartLegendDot("Income", FintechGreen)
                Spacer(Modifier.width(12.dp))
                ChartLegendDot("Expense", FintechRed)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barWidth = 10.dp.toPx()
                    val spacing = (canvasWidth - (data.size * 2 * barWidth)) / (data.size + 1)
                    
                    for (i in 0..3) {
                        val y = canvasHeight - (i * canvasHeight / 3)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    
                    data.forEachIndexed { index, (income, expense) ->
                        val x = spacing + index * (barWidth * 2 + spacing)
                        
                        val incomeHeight = (income / maxVal * canvasHeight * chartProgress).toFloat()
                        drawRoundRect(
                            color = Color(0xFF22C55E),
                            topLeft = Offset(x, canvasHeight - incomeHeight),
                            size = Size(barWidth, incomeHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        
                        val expenseHeight = (expense / maxVal * canvasHeight * chartProgress).toFloat()
                        drawRoundRect(
                            color = Color(0xFFEF4444),
                            topLeft = Offset(x + barWidth + 2.dp.toPx(), canvasHeight - expenseHeight),
                            size = Size(barWidth, expenseHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                data.indices.forEach { index -> Text("Week ${index + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DonutChartCard(
    categorySpend: List<CategorySpendNamed>,
    currencyCode: String,
    modifier: Modifier
) {
    val dataHash = remember(categorySpend) { categorySpend.hashCode() }
    var lastAnimatedHash by rememberSaveable { mutableStateOf<Int?>(null) }
    val chartAnimation = remember { Animatable(if (lastAnimatedHash == dataHash) 1f else 0f) }
    LaunchedEffect(dataHash) {
        if (lastAnimatedHash != dataHash) {
            lastAnimatedHash = dataHash
            chartAnimation.snapTo(0f)
            chartAnimation.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
        } else chartAnimation.snapTo(1f)
    }
    val chartProgress = chartAnimation.value

    val total = remember(categorySpend) { categorySpend.sumOf { it.total } }
    val categories = remember(categorySpend) {
        val sorted = categorySpend.sortedByDescending { it.total }
        val top = sorted.take(4)
        val otherSum = sorted.drop(4).sumOf { it.total }
        if (otherSum > 0) {
            top + CategorySpendNamed(-1, "Other", otherSum)
        } else top
    }
    
    val colors = listOf(Color(0xFF7C4DFF), Color(0xFF3B82F6), Color(0xFFF59E0B), Color(0xFF22C55E), Color(0xFFEF4444))

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Spending by category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start), color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    categories.forEachIndexed { index, data ->
                        val sweepAngle = (data.total / (if (total > 0) total else 1.0) * 360f * chartProgress).toFloat()
                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                        startAngle += (data.total / (if (total > 0) total else 1.0) * 360f).toFloat()
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Total", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatCurrency(total, currencyCode),
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEachIndexed { index, data ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(colors[index % colors.size], CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = data.categoryName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.weight(1f))
                        Text(text = if (total > 0) "${(data.total / total * 100).toInt()}%" else "0%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsSection(
    income: Double,
    expense: Double,
    prevIncome: Double,
    prevExpense: Double,
    categorySpend: List<CategorySpendNamed>,
    currencyCode: String
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val insights = remember(income, expense, prevIncome, prevExpense, categorySpend, primaryColor, currencyCode) {
        val list = mutableListOf<InsightData>()
        
        val savings = income - expense
        val prevSavings = prevIncome - prevExpense
        if (savings > prevSavings) {
            list.add(InsightData("You saved ${formatCurrency(savings - prevSavings, currencyCode)} more than last month", Icons.AutoMirrored.Filled.TrendingUp, Color(0xFF22C55E)))
        } else if (prevSavings > 0) {
            list.add(InsightData("Savings decreased by ${((prevSavings - savings)/prevSavings * 100).toInt()}%", Icons.Default.Warning, Color(0xFFEF4444)))
        }
        
        categorySpend.maxByOrNull { it.total }?.let {
            list.add(InsightData("${it.categoryName} is your biggest expense", Icons.AutoMirrored.Filled.ReceiptLong, Color(0xFFF59E0B)))
        }
        
        if (income > 0 && expense < income * 0.5) {
            list.add(InsightData("Excellent spending ratio this month", Icons.Default.Star, Color(0xFF7C4DFF)))
        } else if (expense > income && income > 0) {
            list.add(InsightData("Spending exceeded income", Icons.Default.Warning, Color(0xFFEF4444)))
        }
        
        if (categorySpend.isNotEmpty()) {
            list.add(InsightData("${categorySpend.size} spending categories were active", Icons.Default.Analytics, primaryColor))
        }
        
        list.take(4)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Key Takeaways",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            insights.forEachIndexed { index, insight ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(insight.color.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(insight.icon, null, tint = insight.color, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = insight.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (index < insights.size - 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

private data class InsightData(val text: String, val icon: ImageVector, val color: Color)

@Composable
private fun TopTransactionsCard(
    transactions: List<TransactionWithCategory>,
    currencyCode: String,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Top Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                transactions.forEachIndexed { index, tx ->
                    val color = getCategoryColor(tx.categoryName ?: "")
                    val isIncome = tx.transaction.type == TransactionType.INCOME
                    
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(color.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(getCategoryEmoji(tx.categoryName ?: ""), fontSize = 18.sp)
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.categoryName ?: "General",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = listOfNotNull(
                                        tx.accountName ?: "Wallet",
                                        tx.transaction.note.takeIf { it.isNotBlank() }
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Text(
                                text = (if (isIncome) "+" else "-") + formatCurrency(tx.transaction.amount, currencyCode),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isIncome) Color(0xFF22C55E) else Color(0xFFEF4444)
                            )
                        }
                        
                        if (index < transactions.size - 1) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(start = 52.dp)
                            )
                        }
                    }
                }
                
                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inbox,
                                null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("No activity this month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapCard(
    activityDays: List<ActivityDay>,
    selectedDate: Date,
    modifier: Modifier
) {
    val dataHash = remember(activityDays, selectedDate) { activityDays.hashCode() * 31 + selectedDate.hashCode() }
    var lastAnimatedHash by rememberSaveable { mutableStateOf<Int?>(null) }
    val heatmapAnimation = remember { Animatable(if (lastAnimatedHash == dataHash) 1f else 0f) }
    LaunchedEffect(dataHash) {
        if (lastAnimatedHash != dataHash) {
            lastAnimatedHash = dataHash
            heatmapAnimation.snapTo(0f)
            heatmapAnimation.animateTo(1f, tween(1000, delayMillis = 600))
        } else heatmapAnimation.snapTo(1f)
    }
    val heatmapAlpha = heatmapAnimation.value

    Card(
        modifier = modifier
            .graphicsLayer { alpha = heatmapAlpha },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Monthly activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Income and spending days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(20.dp))
            
            val days = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { day ->
                    Text(day, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val cal = Calendar.getInstance().apply { 
                time = selectedDate
                set(Calendar.DAY_OF_MONTH, 1)
                // Find start of week for the 1st
                while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    add(Calendar.DAY_OF_YEAR, -1)
                }
            }
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(6) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        repeat(7) {
                            val key = sdf.format(cal.time)
                            val activity = activityDays.find { it.day == key }
                            val intensity = if (activity?.hasExpense == true) (if (activity.hasIncome) 3 else 2) else 0
                            val isInSelectedMonth = cal.get(Calendar.MONTH) == Calendar.getInstance().apply { time = selectedDate }.get(Calendar.MONTH) &&
                                cal.get(Calendar.YEAR) == Calendar.getInstance().apply { time = selectedDate }.get(Calendar.YEAR)
                            
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (!isInSelectedMonth) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f) else
                                        when(intensity) {
                                            0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            2 -> Color(0xFFC084FC).copy(alpha = 0.6f)
                                            3 -> Color(0xFF7C4DFF)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        }
                                    )
                            )
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("No activity", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                repeat(3) { i ->
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(
                        when(i) {
                            0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            1 -> Color(0xFFC084FC).copy(alpha = 0.6f)
                            else -> Color(0xFF7C4DFF)
                        }
                    ))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Income + expense", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HealthScoreCard(
    income: Double,
    expense: Double,
    budgetProgress: List<BudgetProgress>
) {
    val hasActivity = income != 0.0 || expense != 0.0
    val score = remember(income, expense, budgetProgress) {
        if (income == 0.0 && expense == 0.0) return@remember 0
        
        var baseScore = 70 // Start with 70
        
        // Savings Rate Component (up to +20 points)
        if (income > 0) {
            val savingsRate = (income - expense) / income
            baseScore += when {
                savingsRate > 0.4 -> 20
                savingsRate > 0.2 -> 15
                savingsRate > 0.1 -> 10
                savingsRate > 0 -> 5
                else -> -10 // Penalty for dissaving
            }
        }
        
        // Budget Compliance Component (up to +10 points)
        if (budgetProgress.isNotEmpty()) {
            val overBudgetCount = budgetProgress.count { it.spent > it.limit }
            val complianceRate = (budgetProgress.size - overBudgetCount).toFloat() / budgetProgress.size
            baseScore += (complianceRate * 10).toInt()
        } else {
            baseScore += 5 // Neutral bonus for no budgets
        }
        
        baseScore.coerceIn(0, 100)
    }

    val dataHash = remember(income, expense, budgetProgress) { income.hashCode() * 31 + expense.hashCode() * 17 + budgetProgress.hashCode() }
    var lastAnimatedHash by rememberSaveable { mutableStateOf<Int?>(null) }
    val scoreAnimation = remember { Animatable(if (lastAnimatedHash == dataHash) score / 100f else 0f) }
    LaunchedEffect(dataHash) {
        if (lastAnimatedHash != dataHash) {
            lastAnimatedHash = dataHash
            scoreAnimation.snapTo(0f)
            scoreAnimation.animateTo(score / 100f, tween(2000, easing = FastOutSlowInEasing))
        } else scoreAnimation.snapTo(score / 100f)
    }
    val scoreProgress = scoreAnimation.value

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(6.dp, RoundedCornerShape(30.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = trackColor,
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = when {
                            score > 80 -> Color(0xFF22C55E)
                            score > 50 -> Color(0xFF7C4DFF)
                            else -> Color(0xFFEF4444)
                        },
                        startAngle = 140f,
                        sweepAngle = 260f * scoreProgress,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = (scoreProgress * 100).toInt().toString(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "/ 100", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("Financial Health Score", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        !hasActivity -> "Add income or expenses to calculate your score."
                        score > 80 -> "Excellent! You're managing your finances like a pro."
                        score > 60 -> "Good job! You're on the right track."
                        score > 40 -> "Fair. Try to keep your spending in check."
                        else -> "Alert! Your spending is exceeding your income."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
            
        }
    }
}
