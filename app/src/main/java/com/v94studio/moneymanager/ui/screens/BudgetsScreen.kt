package com.v94studio.moneymanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.compose.foundation.isSystemInDarkTheme
import com.v94studio.moneymanager.LocalRepository
import com.v94studio.moneymanager.LocalSettingsRepository
import com.v94studio.moneymanager.LocalUserSettings
import com.v94studio.moneymanager.R
import com.v94studio.moneymanager.data.BudgetEntity
import com.v94studio.moneymanager.data.BudgetProgress
import com.v94studio.moneymanager.data.CategoryEntity
import com.v94studio.moneymanager.data.CategoryType
import com.v94studio.moneymanager.ui.components.*
import com.v94studio.moneymanager.ui.components.featurediscovery.*
import com.v94studio.moneymanager.ui.settings.ThemeMode
import com.v94studio.moneymanager.ui.theme.*
import com.v94studio.moneymanager.ui.util.getLocalizedCategory
import kotlinx.coroutines.launch
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt

private val BudgetCategoryGradient = listOf(Color(0xFF7C4DFF), Color(0xFFA855F7))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetsScreen(
    listState: LazyListState = rememberLazyListState()
) {
    val repository = LocalRepository.current
    val settingsRepository = LocalSettingsRepository.current
    val settings = LocalUserSettings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val featureDiscoveryViewModel = com.v94studio.moneymanager.LocalFeatureDiscovery.current
    val featureDiscoveryState by featureDiscoveryViewModel.uiState.collectAsState()
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val rollingDays = settings.rollingDays
    val nowMillis = remember { System.currentTimeMillis() }

    // Persist totals across navigation to prevent re-animating from 0
    var savedTotalLimit by rememberSaveable { mutableDoubleStateOf(0.0) }
    var savedTotalSpent by rememberSaveable { mutableDoubleStateOf(0.0) }
    var savedTotalRemaining by rememberSaveable { mutableDoubleStateOf(0.0) }

    val budgets by repository.observeBudgetProgress(nowMillis)
        .collectAsState(initial = emptyList())

    val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }

    // Use saved totals as fallback while the list is loading
    val totalLimit = if (budgets.isEmpty() && savedTotalLimit != 0.0) savedTotalLimit else budgets.sumOf { it.limit }
    val totalSpent = if (budgets.isEmpty() && savedTotalSpent != 0.0) savedTotalSpent else budgets.sumOf { it.spent }
    val totalRemaining = if (budgets.isEmpty() && savedTotalRemaining != 0.0) savedTotalRemaining else (totalLimit - totalSpent).coerceAtLeast(0.0)
    
    SideEffect {
        if (budgets.isNotEmpty()) {
            savedTotalLimit = totalLimit
            savedTotalSpent = totalSpent
            savedTotalRemaining = totalRemaining
        }
    }

    var amountText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var editingBudget by remember { mutableStateOf<BudgetProgress?>(null) }
    var showBudgetEditor by remember { mutableStateOf(false) }
    var newlyCreatedBudgetCategoryId by remember { mutableStateOf<Long?>(null) }
    var openBudgetId by remember { mutableStateOf<Long?>(null) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var categoryToRevealId by remember { mutableStateOf<Long?>(null) }
    val budgetCategoryListState = rememberLazyListState()

    // Update fields when editingBudget changes
    LaunchedEffect(editingBudget) {
        editingBudget?.let { budget ->
            amountText = if ((budget.limit % 1.0) == 0.0) budget.limit.toLong().toString() else budget.limit.toString()
            selectedCategory = expenseCategories.find { it.id == budget.categoryId }
        }
    }

    LaunchedEffect(expenseCategories, categoryToRevealId, showBudgetEditor) {
        val categoryId = categoryToRevealId ?: return@LaunchedEffect
        if (!showBudgetEditor) return@LaunchedEffect
        val categoryIndex = expenseCategories.indexOfFirst { it.id == categoryId }
        if (categoryIndex >= 0) {
            // The add-category chip occupies index zero in create mode.
            budgetCategoryListState.animateScrollToItem(categoryIndex + 1)
            categoryToRevealId = null
        }
    }

    // val listState = rememberLazyListState() // Removed to use hoisted state
    val bottomPadding = com.v94studio.moneymanager.LocalBottomPadding.current

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) openBudgetId = null
            }
    }

    LaunchedEffect(budgets, featureDiscoveryState.isLoaded, featureDiscoveryState.currentTutorial, showBudgetEditor) {
        if (
            budgets.isNotEmpty() &&
            !showBudgetEditor &&
            featureDiscoveryState.isLoaded &&
            !featureDiscoveryState.hasSeenBudgetsTutorial &&
            featureDiscoveryState.currentTutorial == null
        ) {
            featureDiscoveryViewModel.showBudgetsTutorial()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 44.dp, end = 16.dp, bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "overview") {
                FintechCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.label_total_overview),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = stringResource(R.string.label_total_budgeted), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                AnimatedAmountText(
                                    amount = totalLimit,
                                    currencyCode = settings.currencyCode,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = stringResource(R.string.label_remaining), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                AnimatedAmountText(
                                    amount = totalRemaining,
                                    currencyCode = settings.currencyCode,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalRemaining > 0) FintechGreen else FintechRed
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = stringResource(R.string.label_spent) + ": ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AnimatedAmountText(
                                amount = totalSpent,
                                currencyCode = settings.currencyCode,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            item(key = "tracking_period") {
                FintechCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.label_tracking_period),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val rangeOptions = listOf(
                            7 to stringResource(R.string.window_week),
                            30 to stringResource(R.string.window_month),
                            90 to stringResource(R.string.window_quarter)
                        )
                        val selectedIndex = rangeOptions.indexOfFirst { it.first == rollingDays }
                            .coerceAtLeast(0)
                        
                        val isDarkTheme = isSystemInDarkTheme()
                        val isDark = when (settings.themeMode) {
                            ThemeMode.SYSTEM -> isDarkTheme
                            ThemeMode.DARK -> true
                            ThemeMode.LIGHT -> false
                        }
                        val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else TabRowContainer
                        val indicatorColor = if (isDark) MaterialTheme.colorScheme.primary else TabRowIndicator
                        val unselectedColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else TabRowUnselected

                        TabRow(
                            selectedTabIndex = selectedIndex,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(containerColor),
                            containerColor = Color.Transparent,
                            contentColor = indicatorColor,
                            indicator = { tabPositions ->
                                Box(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[selectedIndex])
                                        .fillMaxHeight()
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(indicatorColor)
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
                                        ) { scope.launch { settingsRepository.setRollingDays(days) } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(vertical = 14.dp),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else unselectedColor
                                    )
                                    if (index < rangeOptions.lastIndex) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .height(24.dp)
                                                .width(1.dp)
                                                .background(TabRowDivider.copy(alpha = if (isDark) 0.2f else 1f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item(key = "active_budgets_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_active_budgets),
                        modifier = Modifier.offset(x = 7.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        onClick = {
                            editingBudget = null
                            selectedCategory = null
                            amountText = ""
                            amountError = null
                            categoryError = null
                            showBudgetEditor = true
                        },
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .offset(x = (-7).dp)
                            .shadow(8.dp, CircleShape, ambientColor = BrandPurple.copy(alpha = 0.5f), spotColor = BrandPurple)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Brush.horizontalGradient(listOf(BrandPurple, Color(0xFF9333EA))))
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(11.dp), tint = Color.White)
                                Spacer(Modifier.width(3.dp))
                                Text("Add Budget", fontWeight = FontWeight.ExtraBold, fontSize = 10.5.sp, color = Color.White, letterSpacing = 0.1.sp)
                            }
                        }
                    }
                }
            }
            itemsIndexed(budgets, key = { _, it -> it.budgetId.toString() + "_" + it.categoryId }) { index, budget ->
                AnimatedBudgetCardEntry(
                    budgetId = budget.budgetId,
                    animateEntrance = budget.categoryId == newlyCreatedBudgetCategoryId
                ) {
                    SwipeableBudgetCard(
                        budget = budget,
                        currencyCode = settings.currencyCode,
                        isOpen = openBudgetId == budget.budgetId,
                        onOpen = { openBudgetId = budget.budgetId },
                        onClose = { if (openBudgetId == budget.budgetId) openBudgetId = null },
                        onEdit = {
                            editingBudget = budget
                            openBudgetId = null
                            showBudgetEditor = true
                        },
                        onDelete = {
                            scope.launch { repository.deleteBudgetById(budget.budgetId) }
                        },
                        onTargetPositioned = if (index == 0) {
                            { rect ->
                                if (featureDiscoveryState.currentTutorial == TutorialType.BUDGETS_INTRO) {
                                    featureDiscoveryViewModel.onTargetPositioned(TutorialType.BUDGETS_INTRO, rect)
                                }
                            }
                        } else null
                    )
                }
            }
        }
        FadingLazyColumnScrollbar(
            listState = listState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .padding(end = 6.dp, top = 6.dp, bottom = bottomPadding + 6.dp)
        )

        if (featureDiscoveryState.isVisible && featureDiscoveryState.currentTutorial == TutorialType.BUDGETS_INTRO) {
            FeatureDiscoveryOverlay(
                isVisible = true,
                targetRect = featureDiscoveryState.targetRect,
                title = "Your First Category Budget",
                description = "This card tracks spending, remaining money, and progress for the category. Swipe it left to edit or delete the budget.",
                onDismiss = featureDiscoveryViewModel::dismissTutorial,
                shape = DiscoveryShape.Rectangle,
                placement = DiscoveryPlacement.Above
            )
        }

        if (showBudgetEditor) {
            Dialog(
                onDismissRequest = {
                    showBudgetEditor = false
                    editingBudget = null
                    amountText = ""
                    selectedCategory = null
                    amountError = null
                    categoryError = null
                }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .heightIn(max = 680.dp)
                        .imePadding(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 16.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (editingBudget == null) "Add category budget" else "Edit category budget",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black
                            )
                            Text("Choose a category and set its spending limit.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.label_category), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (categoryError != null) {
                                Spacer(Modifier.width(8.dp))
                                Text("Please select", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        LazyRow(
                            state = budgetCategoryListState,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (editingBudget == null) {
                                item {
                                    Surface(
                                        onClick = { showNewCategoryDialog = true },
                                        modifier = Modifier.widthIn(min = 80.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, BrandPurple.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "+ New",
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPurple,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            val displayedCategories = if (editingBudget == null) expenseCategories else expenseCategories.filter { it.id == editingBudget?.categoryId }
                            items(displayedCategories, key = { it.id }) { category ->
                                val isSelected = selectedCategory?.id == category.id
                                Surface(
                                    onClick = {
                                        if (editingBudget == null) {
                                            selectedCategory = if (isSelected) null else category
                                            categoryError = null
                                        }
                                    },
                                    modifier = Modifier.widthIn(min = 80.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.surface,
                                    shadowElevation = if (isSelected) 4.dp else 1.dp,
                                    border = if (isSelected) null else BorderStroke(
                                        1.dp,
                                        if (categoryError != null) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Box(
                                        modifier = if (isSelected) Modifier.background(Brush.horizontalGradient(BudgetCategoryGradient)) else Modifier,
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = getLocalizedCategory(category.name, context),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                            Text("Budget Amount", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = try { Currency.getInstance(settings.currencyCode).getSymbol(Locale.getDefault()) } catch (_: Exception) { "$" },
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Light,
                                    color = if (amountError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                BasicTextField(
                                    value = amountText,
                                    onValueChange = { input ->
                                        val cleaned = input.replace(',', '.')
                                        if (cleaned.count { it == '.' } <= 1) {
                                            amountText = cleaned.filter { it.isDigit() || it == '.' }.take(12)
                                            amountError = null
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    textStyle = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Black, color = if (amountError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    visualTransformation = ThousandsSeparatorVisualTransformation(),
                                    cursorBrush = Brush.verticalGradient(BrandPurpleGradient.take(2)),
                                    decorationBox = { innerTextField ->
                                        if (amountText.isEmpty()) Text("0.00", style = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))
                                        innerTextField()
                                    }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(top = 16.dp), color = if (amountError != null) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant, thickness = 2.dp)
                        }
                    }
                    amountError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Text(stringResource(R.string.label_rolling_window, rollingDays), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    val amountIsValid = (amountText.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                showBudgetEditor = false
                                editingBudget = null
                                amountText = ""
                                selectedCategory = null
                                amountError = null
                                categoryError = null
                            },
                            modifier = Modifier.width(104.dp).height(36.dp),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                            val amount = amountText.replace(',', '.').toDoubleOrNull()
                            if (selectedCategory == null) categoryError = context.getString(R.string.error_select_category)
                            if (amount == null || amount <= 0.0) amountError = context.getString(R.string.error_invalid_amount)
                            if (selectedCategory != null && amount != null && amount > 0.0) {
                                scope.launch {
                                    if (editingBudget != null) {
                                        repository.updateBudget(BudgetEntity(id = editingBudget!!.budgetId, categoryId = selectedCategory!!.id, amountMinor = com.v94studio.moneymanager.data.amountToMinor(amount), rollingDays = editingBudget!!.rollingDays))
                                    } else {
                                        newlyCreatedBudgetCategoryId = selectedCategory!!.id
                                        repository.addBudget(selectedCategory!!.id, amount, rollingDays)
                                    }
                                    showBudgetEditor = false
                                    editingBudget = null
                                    amountText = ""
                                    selectedCategory = null
                                }
                            }
                            },
                            enabled = selectedCategory != null && amountIsValid,
                            modifier = Modifier.width(118.dp).height(36.dp).shadow(6.dp, RoundedCornerShape(50), ambientColor = BrandPurple, spotColor = BrandPurple),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text(if (editingBudget == null) "Create Budget" else "Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        if (showNewCategoryDialog) {
            var newCategoryName by remember { mutableStateOf("") }
            var newCategoryError by remember { mutableStateOf<String?>(null) }

            com.v94studio.moneymanager.ui.components.PremiumAlertDialog(
                onDismissRequest = { showNewCategoryDialog = false },
                title = { Text(stringResource(R.string.label_new_category)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val typeLabel = stringResource(R.string.label_expenses)
                        Text(stringResource(R.string.msg_create_category_desc, typeLabel.lowercase()))
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { 
                                newCategoryName = it
                                newCategoryError = null
                            },
                            label = { Text(stringResource(R.string.label_category_name)) },
                            isError = newCategoryError != null,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        if (newCategoryError != null) {
                            Text(
                                text = newCategoryError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = newCategoryName.trim()
                            if (trimmed.isBlank()) {
                                newCategoryError = context.getString(R.string.error_invalid_name)
                                return@Button
                            }
                            if (categories.any { it.name.trim().equals(trimmed, ignoreCase = true) }) {
                                newCategoryError = "A category with this name already exists"
                                return@Button
                            }
                            scope.launch {
                                val catType = CategoryType.EXPENSE
                                val id = repository.addCategoryAndReturnId(trimmed, catType)
                                selectedCategory = CategoryEntity(id = id, name = trimmed, type = catType)
                                categoryToRevealId = id
                                categoryError = null
                                showNewCategoryDialog = false
                            }
                        },
                        modifier = Modifier.width(104.dp).height(36.dp),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(stringResource(R.string.action_create))
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showNewCategoryDialog = false },
                        modifier = Modifier.width(104.dp).height(36.dp),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun AnimatedBudgetCardEntry(
    budgetId: Long,
    animateEntrance: Boolean,
    content: @Composable () -> Unit
) {
    var visible by remember(budgetId) { mutableStateOf(!animateEntrance) }

    LaunchedEffect(budgetId, animateEntrance) {
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
private fun SwipeableBudgetCard(
    budget: BudgetProgress,
    currencyCode: String?,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTargetPositioned: ((Rect) -> Unit)? = null
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { 160.dp.toPx() }

    val offsetX = remember { androidx.compose.animation.core.Animatable(if (isOpen) -actionWidthPx else 0f) }
    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(isOpen) {
        if (isOpen) offsetX.animateTo(-actionWidthPx)
        else offsetX.animateTo(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp) // Reduction in size
            .height(IntrinsicSize.Min)
    ) {
        // Swipe Actions (Behind)
        Row(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp)),
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

        // Foreground Content
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (isOpen) {
                        scope.launch {
                            offsetX.animateTo(0f)
                            onClose()
                        }
                    }
                }
        ) {
            BudgetProgressCard(
                title = budget.categoryName,
                spent = budget.spent,
                limit = budget.limit,
                rollingDays = budget.rollingDays,
                currencyCode = currencyCode,
                onTargetPositioned = onTargetPositioned
            )
        }
    }
}

@Composable
private fun BudgetProgressCard(
    title: String,
    spent: Double,
    limit: Double,
    rollingDays: Int,
    currencyCode: String?,
    onTargetPositioned: ((Rect) -> Unit)? = null
) {
    val rawProgress = if (limit <= 0.0) 0f else (spent / limit).toFloat().coerceIn(0f, 1.2f)
    val targetProgress = rawProgress.coerceAtMost(1f)
    
    var lastSeenProgress by rememberSaveable { mutableStateOf<Float?>(null) }
    val anim = remember { Animatable(lastSeenProgress ?: 0f) }

    LaunchedEffect(targetProgress) {
        if (lastSeenProgress != targetProgress) {
            lastSeenProgress = targetProgress
            anim.animateTo(
                targetValue = targetProgress,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        } else {
            anim.snapTo(targetProgress)
        }
    }
    
    val remaining = (limit - spent).coerceAtLeast(0.0)
    val overage = (spent - limit).coerceAtLeast(0.0)

    Box(modifier = if (onTargetPositioned != null) Modifier.discoverable(onTargetPositioned) else Modifier) {
    FintechCard {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = stringResource(R.string.label_rolling_budget, rollingDays), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row {
                        AnimatedAmountText(amount = spent, currencyCode = currencyCode, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = " / ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AnimatedAmountText(amount = limit, currencyCode = currencyCode, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (rawProgress > 1f) {
                        Row {
                            AnimatedAmountText(amount = overage, currencyCode = currencyCode, color = FintechRed, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(text = " " + stringResource(R.string.label_over), color = FintechRed, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Row {
                            AnimatedAmountText(amount = remaining, currencyCode = currencyCode, color = FintechGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(text = " " + stringResource(R.string.label_left), color = FintechGreen, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { anim.value },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = if (rawProgress > 1f) FintechRed else MaterialTheme.colorScheme.primary
            )
        }
    }
    }
}
