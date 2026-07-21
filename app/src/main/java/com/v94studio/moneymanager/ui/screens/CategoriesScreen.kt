package com.v94studio.moneymanager.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v94studio.moneymanager.LocalFabVisible
import com.v94studio.moneymanager.LocalRepository
import com.v94studio.moneymanager.LocalUserSettings
import com.v94studio.moneymanager.R
import com.v94studio.moneymanager.data.CategoryEntity
import com.v94studio.moneymanager.data.CategoryType
import com.v94studio.moneymanager.data.toCategoryType
import com.v94studio.moneymanager.ui.components.AppBottomSheet
import com.v94studio.moneymanager.ui.components.*
import com.v94studio.moneymanager.ui.settings.ThemeMode
import com.v94studio.moneymanager.ui.theme.*
import com.v94studio.moneymanager.ui.util.formatCurrency
import com.v94studio.moneymanager.ui.util.getCategoryColor
import com.v94studio.moneymanager.ui.util.getCategoryEmoji
import com.v94studio.moneymanager.ui.util.getLocalizedCategory
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

private enum class SortOrder {
    NAME, TRANSACTIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val repository = LocalRepository.current
    val settings = LocalUserSettings.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val fabVisibleState = LocalFabVisible.current
    val fabOffset by animateDpAsState(
        targetValue = if (fabVisibleState.value) 0.dp else 10.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "fabOffset"
    )

    val categories by repository.observeCategories().collectAsState()
    val transactions by repository.observeTransactions().collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var openCardId by remember { mutableStateOf<Long?>(null) }
    var newlyCreatedCategoryId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) openCardId = null
            }
    }
    
    var sortOrder by remember { mutableStateOf(SortOrder.NAME) }
    var showSortMenu by remember { mutableStateOf(false) }

    val incomeCategories = remember(categories) { categories.filter { it.type == CategoryType.INCOME } }
    val expenseCategories = remember(categories) { categories.filter { it.type == CategoryType.EXPENSE } }
    
    val incomeCount = incomeCategories.size
    val expenseCount = expenseCategories.size
    val totalCount = categories.size

    val categoryTransactionCounts = remember(transactions) {
        transactions.groupBy { it.transaction.categoryId }.mapValues { it.value.size }
    }

    val filteredCategories = remember(categories, searchQuery) {
        categories.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val sortedCategories = remember(filteredCategories, sortOrder, categoryTransactionCounts) {
        when (sortOrder) {
            SortOrder.NAME -> filteredCategories.sortedBy { it.name }
            SortOrder.TRANSACTIONS -> filteredCategories.sortedByDescending { categoryTransactionCounts[it.id] ?: 0 }
        }
    }

    val displayExpenseCategories = remember(sortedCategories) {
        sortedCategories.filter { it.type == CategoryType.EXPENSE }
    }
    val displayIncomeCategories = remember(sortedCategories) {
        sortedCategories.filter { it.type == CategoryType.INCOME }
    }

    LaunchedEffect(displayExpenseCategories, displayIncomeCategories, newlyCreatedCategoryId) {
        val categoryId = newlyCreatedCategoryId ?: return@LaunchedEffect
        val expenseIndex = displayExpenseCategories.indexOfFirst { it.id == categoryId }
        val targetIndex = if (expenseIndex >= 0) {
            // Overview, filters, expense header, then expense cards.
            3 + expenseIndex
        } else {
            val incomeIndex = displayIncomeCategories.indexOfFirst { it.id == categoryId }
            if (incomeIndex < 0) return@LaunchedEffect
            val incomeHeaderIndex = 2 + if (displayExpenseCategories.isNotEmpty()) 1 + displayExpenseCategories.size else 0
            incomeHeaderIndex + 1 + incomeIndex
        }
        listState.animateScrollToItem(targetIndex)
        newlyCreatedCategoryId = null
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MoneyTopAppBar(
                title = "Categories",
                subtitle = "Manage your income and expense categories",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MoneyBackButton(label = "Settings", onClick = onBack)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .offset(y = fabOffset)
                    .size(56.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = if (isDark) 0.8f else 0.5f),
                        spotColor = Color.Black.copy(alpha = if (isDark) 0.8f else 0.5f)
                    ),
                shape = CircleShape,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(BrandPurpleGradient))
                        .then(
                            if (isDark) Modifier.background(Color.Black.copy(alpha = 0.4f)) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Category",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        val topPadding = padding.calculateTopPadding()
        val bottomPadding = padding.calculateBottomPadding()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = (topPadding - 60.dp).coerceAtLeast(0.dp) + 30.dp, 
                    bottom = bottomPadding + 130.dp
                )
            ) {
                item {
                    HeroOverviewCard(incomeCount, expenseCount, totalCount)
                }

                item {
                    SearchAndFilterSection(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        sortOrder = sortOrder,
                        onSortOrderChange = { sortOrder = it },
                        showSortMenu = showSortMenu,
                        onShowSortMenuChange = { showSortMenu = it }
                    )
                }

                if (displayExpenseCategories.isNotEmpty()) {
                    item {
                        CategorySectionHeader(
                            title = "Expense Categories",
                            count = "${displayExpenseCategories.size} categories"
                        )
                    }
                    items(displayExpenseCategories, key = { it.id }) { category ->
                        AnimatedCategoryCardEntry(category.id, category.id == newlyCreatedCategoryId) {
                        PremiumSwipeableCategoryCard(
                            category = category,
                            currencyCode = settings.currencyCode,
                            transactionCount = categoryTransactionCounts[category.id] ?: 0,
                            monthlyAmount = transactions.filter { 
                                it.transaction.categoryId == category.id && isThisMonth(it.transaction.timestamp) 
                            }.sumOf { it.transaction.amount },
                            isOpen = openCardId == category.id,
                            onOpen = { openCardId = category.id },
                            onClose = { if (openCardId == category.id) openCardId = null },
                            onEdit = { editingCategory = category },
                            onDelete = { categoryToDelete = category }
                        )
                        }
                    }
                }

                if (displayIncomeCategories.isNotEmpty()) {
                    item {
                        CategorySectionHeader(
                            title = "Income Categories",
                            count = "${displayIncomeCategories.size} categories"
                        )
                    }
                    items(displayIncomeCategories, key = { it.id }) { category ->
                        AnimatedCategoryCardEntry(category.id, category.id == newlyCreatedCategoryId) {
                        PremiumSwipeableCategoryCard(
                            category = category,
                            currencyCode = settings.currencyCode,
                            transactionCount = categoryTransactionCounts[category.id] ?: 0,
                            monthlyAmount = transactions.filter { 
                                it.transaction.categoryId == category.id && isThisMonth(it.transaction.timestamp) 
                            }.sumOf { it.transaction.amount },
                            isOpen = openCardId == category.id,
                            onOpen = { openCardId = category.id },
                            onClose = { if (openCardId == category.id) openCardId = null },
                            onEdit = { editingCategory = category },
                            onDelete = { categoryToDelete = category }
                        )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        CategoryEntrySheet(
            onDismiss = { showAddSheet = false },
            onConfirm = { name, type, emoji ->
                scope.launch {
                    newlyCreatedCategoryId = repository.addCategoryAndReturnId(name, type, emoji)
                    showAddSheet = false
                }
            }
        )
    }

    if (editingCategory != null) {
        CategoryEntrySheet(
            category = editingCategory,
            onDismiss = { editingCategory = null },
            onConfirm = { name, type, emoji ->
                scope.launch {
                    repository.updateCategory(editingCategory!!.copy(name = name, type = type, emoji = emoji))
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    editingCategory = null
                }
            }
        )
    }

    if (categoryToDelete != null) {
        com.v94studio.moneymanager.ui.components.PremiumAlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category?") },
            text = { Text("Are you sure you want to delete this category?") },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        val toDelete = categoryToDelete!!
                        scope.launch {
                            repository.deleteCategory(toDelete)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            categoryToDelete = null
                            
                            val result = snackbarHostState.showSnackbar(
                                message = "Category deleted",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                repository.addCategory(toDelete.name, toDelete.type, toDelete.emoji)
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
                    onClick = { categoryToDelete = null },
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
private fun AnimatedCategoryCardEntry(
    categoryId: Long,
    animateEntrance: Boolean,
    content: @Composable () -> Unit
) {
    var visible by remember(categoryId) { mutableStateOf(!animateEntrance) }
    LaunchedEffect(categoryId, animateEntrance) {
        if (animateEntrance) visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(420, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(500, easing = FastOutSlowInEasing)) { it / 3 } +
            scaleIn(tween(420, easing = FastOutSlowInEasing), initialScale = 0.96f)
    ) { content() }
}

@Composable
private fun HeroOverviewCard(income: Int, expense: Int, total: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BrandPurple
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricItem("Income", income, FintechGreen, Icons.AutoMirrored.Filled.TrendingUp, Modifier.weight(1f))
                VerticalDivider(modifier = Modifier.height(44.dp).padding(horizontal = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                MetricItem("Expense", expense, FintechRed, Icons.AutoMirrored.Filled.TrendingDown, Modifier.weight(1f))
                VerticalDivider(modifier = Modifier.height(44.dp).padding(horizontal = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                MetricItem("Total", total, BrandPurple, Icons.Default.Layers, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, count: Int, color: Color, icon: ImageVector, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = color)
        }
        Spacer(Modifier.height(12.dp))
        Text(text = count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SearchAndFilterSection(
    query: String, 
    onQueryChange: (String) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    showSortMenu: Boolean,
    onShowSortMenuChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search categories...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
            modifier = Modifier
                .weight(1f)
                .shadow(4.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = BrandPurple
            ),
            singleLine = true
        )
        Box {
            OutlinedIconButton(
                onClick = { onShowSortMenuChange(true) },
                modifier = Modifier
                    .size(56.dp)
                    .shadow(4.dp, RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                border = null
            ) {
                Icon(Icons.Default.FilterList, null, tint = BrandPurple)
            }
            
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { onShowSortMenuChange(false) },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("Sort by Name") },
                    onClick = {
                        onSortOrderChange(SortOrder.NAME)
                        onShowSortMenuChange(false)
                    },
                    leadingIcon = { Icon(Icons.Default.SortByAlpha, null) }
                )
                DropdownMenuItem(
                    text = { Text("Sort by Transactions") },
                    onClick = {
                        onSortOrderChange(SortOrder.TRANSACTIONS)
                        onShowSortMenuChange(false)
                    },
                    leadingIcon = { Icon(Icons.Default.Analytics, null) }
                )
            }
        }
    }
}

@Composable
private fun CategorySectionHeader(title: String, count: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = count, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PremiumSwipeableCategoryCard(
    category: CategoryEntity,
    currencyCode: String?,
    transactionCount: Int,
    monthlyAmount: Double,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val actionWidth = 72.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    
    val offsetX = remember { Animatable(if (isOpen) -actionWidthPx * 2 else 0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isOpen) {
        if (!isOpen && offsetX.value != 0f) offsetX.animateTo(0f)
        else if (isOpen && offsetX.value == 0f) offsetX.animateTo(-actionWidthPx * 2)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .height(88.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp)),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(actionWidth)
                    .background(BrandPurple)
                    .clickable { onEdit() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text("Edit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(actionWidth)
                    .background(FintechRed)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val targetWidthPx = actionWidthPx * 2
                                val target = if (offsetX.value < -targetWidthPx * 0.4f) {
                                    onOpen()
                                    -targetWidthPx
                                } else {
                                    onClose()
                                    0f
                                }
                                offsetX.animateTo(target)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val targetWidthPx = actionWidthPx * 2
                            val newOffset = (offsetX.value + dragAmount).coerceIn(-targetWidthPx, 0f)
                            
                            if (newOffset < -targetWidthPx * 0.8f && offsetX.value >= -targetWidthPx * 0.8f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            
                            scope.launch { offsetX.snapTo(newOffset) }
                        }
                    )
                }
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                .clickable {
                    if (isOpen) {
                        scope.launch { 
                            offsetX.animateTo(0f)
                            onClose()
                        }
                    }
                }
        ) {
            CategoryCardContent(category, currencyCode, transactionCount, monthlyAmount)
        }
    }
}

@Composable
private fun CategoryCardContent(
    category: CategoryEntity,
    currencyCode: String?,
    transactionCount: Int,
    monthlyAmount: Double
) {
    val isExpense = category.type == CategoryType.EXPENSE
    val emoji = category.emoji
    val color = getCategoryColor(category.name)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 24.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = getLocalizedCategory(category.name, LocalContext.current),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$transactionCount transactions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = (if (isExpense) FintechRed else FintechGreen).copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isExpense) "Expense" else "Income",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (isExpense) FintechRed else FintechGreen,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatCurrency(monthlyAmount, currencyCode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "This month",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryEntrySheet(
    category: CategoryEntity? = null,
    initialType: CategoryType = CategoryType.EXPENSE,
    showTypeSwitcher: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (String, CategoryType, String) -> Unit
) {
    val repository = LocalRepository.current
    val existingCategories by repository.observeCategories().collectAsState(initial = emptyList())
    var name by remember { mutableStateOf(category?.name ?: "") }
    var type by remember { mutableStateOf(category?.type ?: initialType) }
    var emoji by remember { mutableStateOf(category?.emoji ?: "📦") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    AppBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = if (category == null) "New Category" else "Edit Category",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Premium Form Card with Dividers
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column {
                    val buttonScale by animateFloatAsState(
                        targetValue = if (showEmojiPicker) 0.9f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "buttonScale"
                    )

                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showEmojiPicker = !showEmojiPicker 
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .graphicsLayer {
                                    scaleX = buttonScale
                                    scaleY = buttonScale
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 24.sp)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Category Name", style = MaterialTheme.typography.labelMedium, color = BrandPurple, fontWeight = FontWeight.Bold)
                            BasicTextField(
                                value = name,
                                onValueChange = { 
                                    name = it 
                                    nameError = null
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (nameError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                cursorBrush = Brush.verticalGradient(BrandPurpleGradient.take(2)),
                                decorationBox = { innerTextField ->
                                    if (name.isEmpty()) {
                                        Text("e.g. Groceries", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                    if (nameError != null) {
                        Text(nameError!!, color = FintechRed, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 20.dp, bottom = 12.dp))
                    }

                    // Animation Choice: Change enter/exit to swap styles
                    // Option 1 (Modern): expandVertically() + fadeIn()
                    // Option 2 (Playful): scaleIn(spring(...)) + fadeIn()
                    // Option 3 (Slide): slideInVertically { it / 2 } + fadeIn()
                    AnimatedVisibility(
                        visible = showEmojiPicker,
                        enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                        exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut()
                    ) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp), 
                                thickness = 0.5.dp, 
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Box(modifier = Modifier.padding(20.dp)) {
                                EmojiPicker(
                                    selectedEmoji = emoji,
                                    onEmojiSelected = { 
                                        emoji = it
                                        showEmojiPicker = false
                                    }
                                )
                            }
                        }
                    }

                    if (showTypeSwitcher) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Category Type", style = MaterialTheme.typography.labelMedium, color = BrandPurple, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                PremiumTypeButton("Expense", type == CategoryType.EXPENSE, FintechRed, Modifier.weight(1f)) {
                                    type = CategoryType.EXPENSE
                                }
                                PremiumTypeButton("Income", type == CategoryType.INCOME, FintechGreen, Modifier.weight(1f)) {
                                    type = CategoryType.INCOME
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save Button (Left)
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = "Please enter a name"
                            return@Button
                        }
                        if (existingCategories.any { it.id != category?.id && it.name.trim().equals(name.trim(), ignoreCase = true) }) {
                            nameError = "A category with this name already exists"
                            return@Button
                        }
                        onConfirm(name.trim(), type, emoji)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = BrandPurple, spotColor = BrandPurple),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(BrandPurpleGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }

                // Cancel Button (Right)
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .border(1.dp, Color(0xFFFFFDF5), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFF1F2), // Premium Solid Soft Rose
                        contentColor = FintechRed
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        "Cancel", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EmojiPicker(
    selectedEmoji: String,
    onEmojiSelected: (String) -> Unit
) {
    val emojis = listOf(
        "💰", "💼", "📈", "🎁", "🏦", "💵", "💎", "💳",
        "🍲", "☕", "🍕", "🍔", "🍎", "🏠", "🚗", "⛽", 
        "🛒", "🎮", "🎓", "🏥", "📱", "✈️", "⚡", "🎾",
        "👕", "🎬", "🛁", "🛠️", "📦", "📫", "🔒", "🐾"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Select Icon", style = MaterialTheme.typography.labelMedium, color = BrandPurple, fontWeight = FontWeight.Bold)
        
        val emojiListState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                state = emojiListState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val chunkedEmojis = emojis.chunked(4)
                items(chunkedEmojis) { columnEmojis ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        columnEmojis.forEach { e ->
                            val isSelected = e == selectedEmoji
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) BrandPurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) BrandPurple else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { onEmojiSelected(e) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = e, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
            FadingLazyRowScrollbar(
                listState = emojiListState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, bottom = 0.dp)
            )
        }
    }
}

@Composable
internal fun PremiumTypeButton(label: String, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (selected) color.copy(alpha = 0.12f) else Color.Transparent,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold
            )
        }
    }
}

private fun isThisMonth(timestamp: Long): Boolean {
    val cal = Calendar.getInstance()
    val nowMonth = cal.get(Calendar.MONTH)
    val nowYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = timestamp
    return cal.get(Calendar.MONTH) == nowMonth && cal.get(Calendar.YEAR) == nowYear
}
