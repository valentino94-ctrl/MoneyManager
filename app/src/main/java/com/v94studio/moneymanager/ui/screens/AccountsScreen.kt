package com.v94studio.moneymanager.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import java.util.Currency
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v94studio.moneymanager.LocalFabVisible
import com.v94studio.moneymanager.LocalRepository
import com.v94studio.moneymanager.LocalUserSettings
import com.v94studio.moneymanager.R
import com.v94studio.moneymanager.data.AccountEntity
import com.v94studio.moneymanager.data.AccountWithBalance
import com.v94studio.moneymanager.ui.components.AppBottomSheet
import com.v94studio.moneymanager.ui.components.*
import com.v94studio.moneymanager.ui.settings.ThemeMode
import com.v94studio.moneymanager.ui.theme.*
import com.v94studio.moneymanager.ui.util.formatCurrency
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class AccountSortOrder {
    NAME, BALANCE
}

private data class AccountTypeInfo(
    val name: String,
    val icon: ImageVector,
    val category: String
)

private val accountTypes = listOf(
    AccountTypeInfo("Checking Account", Icons.Default.AccountBalance, "Banking"),
    AccountTypeInfo("Savings Account", Icons.Default.AccountBalance, "Banking"),
    AccountTypeInfo("Fixed Deposit", Icons.Default.Lock, "Banking"),
    AccountTypeInfo("Money Market", Icons.AutoMirrored.Filled.ShowChart, "Banking"),
    AccountTypeInfo("Joint Account", Icons.Default.Group, "Banking"),
    AccountTypeInfo("Business Account", Icons.Default.Business, "Banking"),
    AccountTypeInfo("Cash Wallet", Icons.Default.Payments, "Cash & Wallets"),
    AccountTypeInfo("Mobile Money", Icons.Default.Smartphone, "Cash & Wallets"),
    AccountTypeInfo("Digital Wallet", Icons.Default.AccountBalanceWallet, "Cash & Wallets"),
    AccountTypeInfo("Credit Card", Icons.Default.CreditCard, "Credit & Debt"),
    AccountTypeInfo("Personal Loan", Icons.Default.RequestQuote, "Credit & Debt"),
    AccountTypeInfo("Mortgage", Icons.Default.HomeWork, "Credit & Debt"),
    AccountTypeInfo("Student Loan", Icons.Default.School, "Credit & Debt"),
    AccountTypeInfo("Car Loan", Icons.Default.DirectionsCar, "Credit & Debt"),
    AccountTypeInfo("Line of Credit", Icons.Default.CreditScore, "Credit & Debt"),
    AccountTypeInfo("Brokerage Account", Icons.Default.Timeline, "Investment"),
    AccountTypeInfo("Retirement Account", Icons.Default.VolunteerActivism, "Investment"),
    AccountTypeInfo("Mutual Fund", Icons.Default.PieChart, "Investment"),
    AccountTypeInfo("ETF Portfolio", Icons.Default.QueryStats, "Investment"),
    AccountTypeInfo("Stock Portfolio", Icons.Default.BarChart, "Investment"),
    AccountTypeInfo("Crypto Wallet", Icons.Default.CurrencyBitcoin, "Investment"),
    AccountTypeInfo("Vehicle", Icons.Default.DirectionsCar, "Assets"),
    AccountTypeInfo("Property", Icons.Default.Home, "Assets"),
    AccountTypeInfo("Precious Metals", Icons.Default.Diamond, "Assets"),
    AccountTypeInfo("Collectibles", Icons.Default.Collections, "Assets"),
    AccountTypeInfo("Business Ownership", Icons.Default.CorporateFare, "Assets"),
    AccountTypeInfo("Emergency Fund", Icons.Default.CrisisAlert, "Custom"),
    AccountTypeInfo("Vacation Fund", Icons.Default.BeachAccess, "Custom"),
    AccountTypeInfo("Education Fund", Icons.AutoMirrored.Filled.MenuBook, "Custom"),
    AccountTypeInfo("Wedding Fund", Icons.Default.Favorite, "Custom"),
    AccountTypeInfo("Car Fund", Icons.Default.CarRental, "Custom"),
    AccountTypeInfo("General Goal", Icons.Default.AdsClick, "Custom")
)

private val accountEmojis = listOf(
    "🏦", "💰", "💵", "💳", "🏧", "🪙", "💹", "📈", "📉", "👛", 
    "👜", "💼", "🏢", "🏠", "🏡", "🏠", "🏪", "🏬", "🏫", "🏭", 
    "🏰", "🏯", "🗼", "🗽", "⛲", "🎢", "🎡", "🎢", "🚢", "🛳️", 
    "🚤", "🚣", "🛥️", "🛶", "🚢", "🚇", "🚇", "🚆", "🚈", "🚞"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(onBack: () -> Unit) {
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

    val accounts by repository.observeAccountsWithBalances().collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountWithBalance?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountWithBalance?>(null) }
    var openCardId by remember { mutableStateOf<Long?>(null) }
    var newlyCreatedAccountId by remember { mutableStateOf<Long?>(null) }
    var backNavigationPending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val navigateBackAfterFrame: () -> Unit = {
        if (!backNavigationPending) {
            backNavigationPending = true
            scope.launch {
                repeat(3) { withFrameNanos { } }
                onBack()
            }
        }
    }

    BackHandler(onBack = navigateBackAfterFrame)

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) openCardId = null
            }
    }
    
    var sortOrder by remember { mutableStateOf(AccountSortOrder.NAME) }
    var showSortMenu by remember { mutableStateOf(false) }

    val accountCount = accounts.size

    val filteredAccounts = remember(accounts, searchQuery) {
        accounts.filter { it.account.name.contains(searchQuery, ignoreCase = true) || it.account.type.contains(searchQuery, ignoreCase = true) }
    }

    val sortedAccounts = remember(filteredAccounts, sortOrder) {
        when (sortOrder) {
            AccountSortOrder.NAME -> filteredAccounts.sortedBy { it.account.name }
            AccountSortOrder.BALANCE -> filteredAccounts.sortedByDescending { it.balance }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MoneyTopAppBar(
                title = "Accounts",
                subtitle = "Manage your bank accounts",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MoneyBackButton(label = "Settings", onClick = navigateBackAfterFrame)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 1.dp)
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
                        contentDescription = "Add Account",
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
                    top = (topPadding - 60.dp).coerceAtLeast(0.dp) + 28.dp,
                    bottom = bottomPadding + 130.dp
                )
            ) {
                    item {
                        AccountSearchSection(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            sortOrder = sortOrder,
                            onSortOrderChange = { sortOrder = it },
                            showSortMenu = showSortMenu,
                            onShowSortMenuChange = { showSortMenu = it }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(text = "$accountCount accounts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    items(sortedAccounts, key = { it.account.id }) { wrapper ->
                        AnimatedAccountCardEntry(
                            accountId = wrapper.account.id,
                            animateEntrance = wrapper.account.id == newlyCreatedAccountId
                        ) {
                        PremiumSwipeableAccountCard(
                            account = wrapper.account,
                            balance = wrapper.balance,
                            currencyCode = settings.currencyCode,
                            isOpen = openCardId == wrapper.account.id,
                            onOpen = { openCardId = wrapper.account.id },
                            onClose = { if (openCardId == wrapper.account.id) openCardId = null },
                            onEdit = { editingAccount = wrapper },
                            onDelete = { accountToDelete = wrapper }
                        )
                        }
                    }
            }
        }
    }

    if (showAddSheet) {
        AccountEntrySheet(
            onDismiss = { showAddSheet = false },
            onConfirm = { name, type, emoji ->
                scope.launch {
                    newlyCreatedAccountId = repository.addAccount(name, type, emoji = emoji)
                    showAddSheet = false
                }
            }
        )
    }

    if (editingAccount != null) {
        AccountEntrySheet(
            account = editingAccount!!.account,
            onDismiss = { editingAccount = null },
            onConfirm = { name, type, emoji ->
                scope.launch {
                    repository.updateAccount(editingAccount!!.account.copy(
                        name = name, 
                        type = type, 
                        emoji = emoji
                    ))
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    editingAccount = null
                }
            }
        )
    }

    if (accountToDelete != null) {
        com.v94studio.moneymanager.ui.components.PremiumAlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete Account?") },
            text = { Text("Are you sure you want to delete this account? Transactions linked to it will lose their account reference.") },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        val toDelete = accountToDelete!!.account
                        scope.launch {
                            repository.deleteAccount(toDelete)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            accountToDelete = null
                            snackbarHostState.showSnackbar("Account deleted")
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
                    onClick = { accountToDelete = null },
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
private fun AnimatedAccountCardEntry(
    accountId: Long,
    animateEntrance: Boolean,
    content: @Composable () -> Unit
) {
    var visible by remember(accountId) { mutableStateOf(!animateEntrance) }
    LaunchedEffect(accountId, animateEntrance) {
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
private fun AccountSearchSection(
    query: String, 
    onQueryChange: (String) -> Unit,
    sortOrder: AccountSortOrder,
    onSortOrderChange: (AccountSortOrder) -> Unit,
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
            placeholder = { Text("Search accounts...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
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
                        onSortOrderChange(AccountSortOrder.NAME)
                        onShowSortMenuChange(false)
                    },
                    leadingIcon = { Icon(Icons.Default.SortByAlpha, null) }
                )
                DropdownMenuItem(
                    text = { Text("Sort by Balance") },
                    onClick = {
                        onSortOrderChange(AccountSortOrder.BALANCE)
                        onShowSortMenuChange(false)
                    },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, null) }
                )
            }
        }
    }
}

@Composable
private fun PremiumSwipeableAccountCard(
    account: AccountEntity,
    balance: Double,
    currencyCode: String?,
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
                        scope.launch { offsetX.animateTo(0f); onClose() }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(BrandPurple.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = account.emoji, fontSize = 24.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    Text(text = account.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = formatCurrency(balance, currencyCode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Balance", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountEntrySheet(
    account: AccountEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var type by remember { mutableStateOf(account?.type ?: "Checking Account") }
    var emoji by remember { mutableStateOf(account?.emoji ?: "🏦") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var showTypePicker by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    val selectedTypeInfo = remember(type) {
        accountTypes.find { it.name == type } ?: accountTypes.first()
    }

    AppBottomSheet(
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = if (account == null) "New Account" else "Edit Account",
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
                    // Emoji Selection Row
                    Row(
                        modifier = Modifier
                            .clickable { showEmojiPicker = true }
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Account Icon", style = MaterialTheme.typography.labelMedium, color = BrandPurple, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(text = "Identify with emoji", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 22.sp)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Name Section
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Account Name", style = MaterialTheme.typography.labelMedium, color = BrandPurple, fontWeight = FontWeight.Bold)
                        BasicTextField(
                            value = name,
                            onValueChange = { name = it; nameError = null },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (nameError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            cursorBrush = Brush.verticalGradient(BrandPurpleGradient.take(2)),
                            decorationBox = { innerTextField ->
                                if (name.isEmpty()) {
                                    Text("e.g. Cash Wallet", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
                                }
                                innerTextField()
                            }
                        )
                        if (nameError != null) {
                            Text(text = nameError!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Type Section
                    Row(
                        modifier = Modifier
                            .clickable { showTypePicker = true }
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Type", style = MaterialTheme.typography.labelMedium, color = BrandPurple, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(text = selectedTypeInfo.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
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

        if (showTypePicker) {
            AccountTypePickerSheet(
                selectedType = type,
                onTypeSelected = { 
                    type = it
                    showTypePicker = false
                },
                onDismiss = { showTypePicker = false }
            )
        }

        if (showEmojiPicker) {
            AccountEmojiPickerSheet(
                selectedEmoji = emoji,
                onEmojiSelected = { 
                    emoji = it
                    showEmojiPicker = false
                },
                onDismiss = { showEmojiPicker = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AccountEmojiPickerSheet(
    selectedEmoji: String,
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "Choose Emoji",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
            )

            val emojiListState = rememberLazyListState()
            Box(modifier = Modifier.fillMaxWidth()) {
                LazyRow(
                    state = emojiListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val chunkedEmojis = accountEmojis.chunked(4)
                    items(chunkedEmojis) { columnEmojis ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            columnEmojis.forEach { e ->
                                val isSelected = selectedEmoji == e
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
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
                                    Text(text = e, fontSize = 26.sp)
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
                        .padding(start = 48.dp, end = 48.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTypePickerSheet(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxHeight(0.5f)
    ) {
        val groupedTypes = remember { accountTypes.groupBy { it.category } }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "Choose Account Type",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            val sheetListState = rememberLazyListState()
            val selectedFlatIndex = remember(groupedTypes, selectedType) {
                var count = 0
                var found = -1
                for (group in groupedTypes) {
                    count++ // Category header item
                    val types = group.value
                    val idxInGroup = types.indexOfFirst { it.name == selectedType }
                    if (idxInGroup != -1) {
                        found = count + idxInGroup
                        break
                    }
                    count += types.size
                }
                found
            }

            LaunchedEffect(selectedFlatIndex) {
                if (selectedFlatIndex >= 0) {
                    sheetListState.scrollToItem(selectedFlatIndex)
                }
            }

            LazyColumn(
                state = sheetListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                groupedTypes.forEach { (category, types) ->
                    item {
                        Text(
                            text = category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandPurple,
                            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                        )
                    }
                    items(types) { typeInfo ->
                        val isSelected = typeInfo.name == selectedType
                        Surface(
                            onClick = { onTypeSelected(typeInfo.name) },
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Brand Accent Line
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(4.dp)
                                        .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                        .background(if (isSelected) BrandPurple else Color.Transparent)
                                )

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Icon Container
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                if (isSelected) BrandPurple.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            typeInfo.icon, 
                                            null, 
                                            modifier = Modifier.size(20.dp), 
                                            tint = if (isSelected) BrandPurple else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = typeInfo.name, 
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BrandPurple else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
