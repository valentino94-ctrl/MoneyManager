package com.v94studio.moneymanager.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v94studio.moneymanager.LocalRepository
import com.v94studio.moneymanager.LocalUserSettings
import com.v94studio.moneymanager.data.*
import com.v94studio.moneymanager.ui.components.*
import com.v94studio.moneymanager.ui.theme.*
import com.v94studio.moneymanager.ui.util.*
import kotlinx.coroutines.launch
import java.util.*

private val FintechGradient = listOf(Color(0xFF7C4DFF), Color(0xFFA855F7))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    existingTransaction: TransactionEntity? = null,
    onDone: () -> Unit,
    onNavigateToRecurring: () -> Unit = {}
) {
    val repository = LocalRepository.current
    val settings = LocalUserSettings.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val accounts by repository.observeAccountsWithBalances().collectAsState(initial = emptyList())

    var type by remember(existingTransaction) { 
        mutableStateOf(existingTransaction?.type ?: TransactionType.EXPENSE) 
    }
    var amountText by remember(existingTransaction) { 
        mutableStateOf(existingTransaction?.amount?.let { if ((it % 1.0) == 0.0) it.toLong().toString() else it.toString() } ?: "") 
    }
    var noteText by remember(existingTransaction) { mutableStateOf(existingTransaction?.note ?: "") }
    var selectedCategory by remember(existingTransaction?.id) { mutableStateOf<CategoryEntity?>(null) }
    var selectedAccount by remember(accounts, existingTransaction) {
        // If we just added a new account, we want to keep it selected even when the list updates
        mutableStateOf(accounts.find { it.account.id == existingTransaction?.accountId } ?: accounts.firstOrNull())
    }
    
    // Explicitly track the last selected ID to survive recompositions/state updates
    var lastSelectedAccountId by remember { mutableStateOf(existingTransaction?.accountId) }

    // Load an existing transaction's category without resetting a newly created
    // or manually selected category when the categories flow emits again.
    LaunchedEffect(categories, existingTransaction?.categoryId) {
        val existingCategoryId = existingTransaction?.categoryId
        if (existingCategoryId != null && selectedCategory?.id != existingCategoryId) {
            selectedCategory = categories.find { it.id == existingCategoryId }
        }
    }

    // Synchronize selectedAccount with lastSelectedAccountId when accounts list changes
    LaunchedEffect(accounts, lastSelectedAccountId) {
        if (lastSelectedAccountId != null) {
            val found = accounts.find { it.account.id == lastSelectedAccountId }
            if (found != null) {
                selectedAccount = found
            }
        } else if (selectedAccount == null && accounts.isNotEmpty()) {
            selectedAccount = accounts.firstOrNull()
            lastSelectedAccountId = selectedAccount?.account?.id
        }
    }
    var timestamp by remember(existingTransaction) { mutableLongStateOf(existingTransaction?.timestamp ?: System.currentTimeMillis()) }

    var categoryError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    var showAccountPicker by remember { mutableStateOf(false) }
    var showAddAccountSheet by remember { mutableStateOf(false) }
    var showAddCategorySheet by remember { mutableStateOf(false) }

    val filteredCategories = categories.filter { it.type == type.toCategoryType() }
    
    val quickAmounts = remember(type) {
        if (type == TransactionType.EXPENSE) listOf(50.0, 100.0, 200.0, 500.0, 1000.0, 2000.0, 5000.0)
        else listOf(500.0, 1000.0, 2000.0, 5000.0, 10000.0, 20000.0, 50000.0)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MoneyTopAppBar(
                title = if (existingTransaction == null) "New Transaction" else "Edit Transaction",
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                // Save Button (Left)
                Button(
                    onClick = {
                        val amount = amountText.replace(",", ".").toDoubleOrNull()
                        val hasAmountError = (amount == null) || (amount <= 0.0)
                        val hasCategoryError = selectedCategory == null
                        
                        amountError = hasAmountError
                        categoryError = hasCategoryError

                        if (hasAmountError || hasCategoryError || (selectedAccount == null)) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            return@Button
                        }
                        scope.launch {
                            val transaction = TransactionEntity(
                                id = existingTransaction?.id ?: 0L,
                                amountMinor = amountToMinor(amount),
                                type = type,
                                categoryId = selectedCategory?.id,
                                accountId = selectedAccount?.account?.id,
                                note = noteText,
                                timestamp = timestamp
                            )
                            repository.upsertTransaction(transaction)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDone()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), spotColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(FintechGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (existingTransaction == null) "Save" else "Update",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Cancel Button (Right)
                Button(
                    onClick = onDone,
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
                        text = "Cancel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height((innerPadding.calculateTopPadding() - 60.dp).coerceAtLeast(0.dp) + 28.dp))

                TransactionTypeToggle(
                    selectedType = type,
                    onTypeSelected = { 
                        type = it
                        if (selectedCategory?.type != it.toCategoryType()) selectedCategory = null
                    }
                )

                HeroAmountCard(
                    amount = amountText,
                    currencyCode = settings.currencyCode,
                    isError = amountError,
                    onAmountChange = { 
                        amountText = it 
                        amountError = false
                    }
                )

                QuickAmountSection(
                    amounts = quickAmounts,
                    currencyCode = settings.currencyCode,
                    onAmountSelect = { 
                        amountText = if ((it % 1.0) == 0.0) it.toLong().toString() else it.toString()
                        amountError = false
                    }
                )

                CategoryGridSection(
                    categories = filteredCategories,
                    selectedCategory = selectedCategory,
                    isError = categoryError,
                    onCategorySelect = { 
                        selectedCategory = it
                        categoryError = false
                    },
                    onAddCategoryClick = { showAddCategorySheet = true }
                )

                selectedAccount?.let {
                    AccountSwitcherPill(
                        account = it.account,
                        balance = it.balance,
                        currencyCode = settings.currencyCode,
                        onClick = { showAccountPicker = true }
                    )
                }

                DateTimeCard(
                    timestamp = timestamp,
                    onDateSelected = { timestamp = mergeDateAndTime(it, timestamp) },
                    onTimeSelected = { hour, minute -> timestamp = updateTime(timestamp, hour, minute) }
                )

                NotesField(
                    value = noteText,
                    onValueChange = { noteText = it }
                )
                
                Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding() + 16.dp))
            }
        }
    }

    if (showAddCategorySheet) {
        CategoryEntrySheet(
            initialType = type.toCategoryType(),
            showTypeSwitcher = false,
            onDismiss = { showAddCategorySheet = false },
            onConfirm = { name, categoryType, emoji ->
                scope.launch {
                    val newId = repository.addCategoryAndReturnId(name, categoryType, emoji)
                    if (categoryType == type.toCategoryType()) {
                        selectedCategory = CategoryEntity(id = newId, name = name, type = categoryType, emoji = emoji)
                        categoryError = false
                    }
                    showAddCategorySheet = false
                }
            }
        )
    }

    if (showAccountPicker) {
        AccountSelectionSheet(
            accounts = accounts,
            selectedAccountId = selectedAccount?.account?.id,
            onAccountSelected = {
                lastSelectedAccountId = it.id
                showAccountPicker = false
            },
            onAddAccount = {
                showAccountPicker = false
                showAddAccountSheet = true
            },
            onDismiss = { showAccountPicker = false }
        )
    }

    if (showAddAccountSheet) {
        AccountEntrySheet(
            onDismiss = { showAddAccountSheet = false },
            onConfirm = { name, accType, emoji ->
                scope.launch {
                    val newId = repository.addAccount(name, accType, emoji = emoji)
                    lastSelectedAccountId = newId
                    showAddAccountSheet = false
                }
            }
        )
    }
}

@Composable
private fun TransactionTypeToggle(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    val transition = updateTransition(targetState = selectedType, label = "ToggleTransition")

    val indicatorColor by transition.animateColor(label = "Color") { state ->
        if (state == TransactionType.EXPENSE) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) 
        else FintechGreen.copy(alpha = 0.15f)
    }

    val indicatorOffset by animateFloatAsState(
        targetValue = if (selectedType == TransactionType.EXPENSE) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "Offset"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        BoxWithConstraints(modifier = Modifier.padding(4.dp)) {
            val width = maxWidth

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(width / 2)
                    .offset(x = width / 2 * indicatorOffset)
                    .clip(RoundedCornerShape(12.dp))
                    .background(indicatorColor)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                    val isSelected = selectedType == type
                    val contentColor by animateColorAsState(
                        if (isSelected) {
                            if (type == TransactionType.EXPENSE) Color(0xFFEF4444) else Color(0xFF22C55E)
                        } else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "content"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTypeSelected(type) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (type == TransactionType.EXPENSE) "Expense" else "Income",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroAmountCard(
    amount: String,
    currencyCode: String,
    isError: Boolean,
    onAmountChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val symbol = try { Currency.getInstance(currencyCode).getSymbol(Locale.getDefault()) } catch (e: Exception) { "$" }
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Light,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = amount,
                    onValueChange = { input ->
                        val cleaned = input.replace(',', '.')
                        if (cleaned.count { it == '.' } <= 1) {
                            val filtered = cleaned.filter { it.isDigit() || it == '.' }
                            if (filtered.length <= 12) onAmountChange(filtered)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    visualTransformation = ThousandsSeparatorVisualTransformation(),
                    cursorBrush = Brush.verticalGradient(FintechGradient),
                    decorationBox = { innerTextField ->
                        if (amount.isEmpty()) {
                            Text("0.00", style = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Black, color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))
                        }
                        innerTextField()
                    }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(top = 16.dp), color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant, thickness = 2.dp)
        }
    }
}

@Composable
private fun QuickAmountSection(
    amounts: List<Double>,
    currencyCode: String,
    onAmountSelect: (Double) -> Unit
) {
    val numberFormat = remember { java.text.NumberFormat.getNumberInstance(Locale.getDefault()) }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(amounts) { amount ->
            val symbol = try { Currency.getInstance(currencyCode).getSymbol(Locale.getDefault()) } catch (e: Exception) { "$" }
            Surface(
                onClick = { onAmountSelect(amount) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                shadowElevation = 1.dp
            ) {
                Text(
                    text = "$symbol${numberFormat.format(amount.toLong())}",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CategoryGridSection(
    categories: List<CategoryEntity>,
    selectedCategory: CategoryEntity?,
    isError: Boolean,
    onCategorySelect: (CategoryEntity) -> Unit,
    onAddCategoryClick: () -> Unit
) {
    val scrollState = rememberLazyListState()

    LaunchedEffect(categories, selectedCategory?.id) {
        val selectedId = selectedCategory?.id ?: return@LaunchedEffect
        val selectedIndex = categories.indexOfFirst { it.id == selectedId }
        if (selectedIndex >= 0) {
            // The add-category chip occupies index zero.
            scrollState.animateScrollToItem(selectedIndex + 1)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (isError) {
                    Spacer(Modifier.width(8.dp))
                    Text("Please select", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        LazyRow(
            state = scrollState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    onClick = onAddCategoryClick,
                    modifier = Modifier.widthIn(min = 80.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, BrandPurple.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ New",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                            fontWeight = FontWeight.Bold,
                            color = BrandPurple,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(categories, key = { it.id }) { category ->
                val isSelected = selectedCategory?.id == category.id
                val localizedName = getLocalizedCategory(category.name, LocalContext.current)

                Surface(
                    onClick = { onCategorySelect(category) },
                    modifier = Modifier.widthIn(min = 80.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.surface,
                    shadowElevation = if (isSelected) 4.dp else 1.dp,
                    border = if (isSelected) null else BorderStroke(1.dp, if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = if (isSelected) Modifier.background(Brush.horizontalGradient(FintechGradient)) else Modifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = localizedName,
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
}

@Composable
private fun DateTimeCard(
    timestamp: Long,
    onDateSelected: (Long) -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    DatePickerField(
                        label = "Date",
                        selectedMillis = timestamp,
                        onSelected = onDateSelected
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .height(32.dp)
                    .padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            Box(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    TimePickerField(
                        label = "Time",
                        selectedMillis = timestamp,
                        onSelected = onTimeSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Add a note...") },
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}
