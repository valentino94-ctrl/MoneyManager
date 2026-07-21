package com.v94studio.moneymanager

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.v94studio.moneymanager.ui.theme.PurplePrimary
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.v94studio.moneymanager.data.MoneyRepository
import androidx.compose.ui.text.font.FontWeight
import com.v94studio.moneymanager.ui.navigation.AppDestination
import com.v94studio.moneymanager.ui.navigation.BottomBar
import com.v94studio.moneymanager.ui.navigation.MMActivePurple
import com.v94studio.moneymanager.ui.navigation.NavRoutes
import com.v94studio.moneymanager.ui.screens.*
import com.v94studio.moneymanager.ui.settings.ThemeMode
import com.v94studio.moneymanager.ui.settings.UserSettings
import com.v94studio.moneymanager.ui.settings.UserSettingsRepository
import com.v94studio.moneymanager.ui.theme.BrandPurple
import com.v94studio.moneymanager.ui.theme.BrandPurpleGradient
import com.v94studio.moneymanager.ui.theme.LocalGradientColors
import com.v94studio.moneymanager.ui.theme.MoneyManagerTheme
import com.v94studio.moneymanager.ui.util.CurrencyDetector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import com.v94studio.moneymanager.ui.util.NotificationHelper
import com.v94studio.moneymanager.ui.util.BackupHelper
import com.v94studio.moneymanager.worker.ReminderWorker
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.v94studio.moneymanager.ui.theme.FintechBackground
import com.v94studio.moneymanager.ui.components.featurediscovery.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// App-wide CompositionLocals
val LocalRepository = compositionLocalOf<MoneyRepository> { error("No Repository") }
val LocalUserSettings = compositionLocalOf<UserSettings> { error("No Settings") }
val LocalSettingsRepository = compositionLocalOf<UserSettingsRepository> { error("No SettingsRepo") }
val LocalFabVisible = compositionLocalOf<MutableState<Boolean>> { error("No FabState") }
val LocalTopPadding = compositionLocalOf { 0.dp }
val LocalBottomPadding = compositionLocalOf { 0.dp }
val LocalFeatureDiscovery = compositionLocalOf<FeatureDiscoveryViewModel> { error("No FeatureDiscoveryViewModel") }
val LocalActivityCalendarListState = compositionLocalOf<LazyListState> { error("No Activity calendar state") }

private fun activityCalendarStartIndex(): Int {
    val start = Calendar.getInstance().apply {
        add(Calendar.MONTH, -6)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    var todayIndex = 0
    while (start.before(today)) {
        todayIndex++
        start.add(Calendar.DAY_OF_YEAR, 1)
    }
    return (todayIndex - 3).coerceAtLeast(0)
}

private data class FabConfig(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

enum class MoneyNavigationType {
    BOTTOM_NAVIGATION, NAVIGATION_RAIL, PERMANENT_DRAWER
}

@Composable
fun MoneyManagerApp(
    appContainer: AppContainer,
    windowSizeClass: WindowSizeClass
) {
    val navController = rememberNavController()
    
    // Hoisted Scroll States for primary screens
    val dashboardListState = rememberLazyListState()
    val activityListState = rememberLazyListState()
    val activityCalendarStartIndex = remember { activityCalendarStartIndex() }
    val activityCalendarListState = rememberLazyListState(
        initialFirstVisibleItemIndex = activityCalendarStartIndex
    )
    val settingsListState = rememberLazyListState()

    // Planning sub-screen states
    val budgetsListState = rememberLazyListState()
    val savingsListState = rememberLazyListState()

    val repository = appContainer.repository
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember(context) { UserSettingsRepository(context) }
    val resetTutorialsForFreshInstall = remember(context) {
        val installMarker = File(context.noBackupFilesDir, "money_manager_install_marker_v2")
        if (installMarker.exists()) {
            false
        } else {
            runCatching { installMarker.createNewFile() }.getOrDefault(false)
        }
    }
    val featureDiscoveryViewModel = remember(settingsRepository, resetTutorialsForFreshInstall) {
        FeatureDiscoveryViewModel(settingsRepository, resetTutorialsForFreshInstall)
    }

    var isAppReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        repository.seedDefaultsIfEmpty()
        repository.seedDefaultAccountIfEmpty()
        repository.processDueRecurring()
        
        // Setup Reminders
        NotificationHelper.createNotificationChannel(context)
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "recurring_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
        
        // Show loading screen for at least 1.5 seconds for branding
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < 1500) {
            kotlinx.coroutines.delay(1500 - elapsed)
        }
        isAppReady = true
    }

    val detectedCurrency = remember(context) { CurrencyDetector.detectCurrencyCode(context) }
    val settings by settingsRepository.settingsFlow.collectAsState(
        initial = UserSettings(
            themeMode = ThemeMode.DARK,
            currencyCode = detectedCurrency,
            rollingDays = 30,
            transactionsFilter = com.v94studio.moneymanager.ui.settings.TransactionFilterSetting.ALL,
            transactionsRangeDays = 30,
            transactionsSort = com.v94studio.moneymanager.ui.settings.TransactionSortSetting.NEWEST,
            transactionsQuery = "",
            biometricEnabled = false,
            privacyModeEnabled = false,
            autoApproveRecurring = true,
            onboardingComplete = false,
            hasCompletedTour = false,
            hasSeenFabTutorial = false,
            hasSeenSwipeTutorial = false,
            hasSeenDashboardTutorial = false,
            hasSeenPlanningTutorial = false,
            hasSeenSavingsTutorial = false,
            hasSeenBudgetsTutorial = false,
            languageTag = Locale.getDefault().toLanguageTag(),
        )
    )
    
    // Auto-save the detected currency on first run if nothing is saved yet
    LaunchedEffect(detectedCurrency) {
        if (!settingsRepository.isCurrencySaved()) {
            settingsRepository.setCurrencyCode(detectedCurrency)
        }
    }
    
    // Apply saved language on startup
    LaunchedEffect(Unit) {
        val currentSettings = settingsRepository.settingsFlow.first()
        val appLocale = LocaleListCompat.forLanguageTags(currentSettings.languageTag)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    val fabVisible = rememberSaveable { mutableStateOf(true) }

    // Hoisted data to prevent list resets when switching tabs
    val allTransactions by repository.observeTransactions().collectAsState()

    // Optimized spending status to trigger theme recompositions only when thresholds are crossed
    val budgetProgressFlow = remember(repository) { repository.observeBudgetProgress() }
    val budgetProgress by budgetProgressFlow.collectAsState(initial = emptyList())
    val spendingStatus by remember {
        derivedStateOf {
            val totalLimit = budgetProgress.sumOf { it.limit }
            val totalSpent = budgetProgress.sumOf { it.spent }
            val ratio = if (totalLimit > 0) (totalSpent / totalLimit).toFloat() else 0f
            when {
                ratio > 1.0f -> 2 // Over budget
                ratio > 0.8f -> 1 // Near limit
                else -> 0 // Normal
            }
        }
    }

    val navigationType = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> MoneyNavigationType.BOTTOM_NAVIGATION
        WindowWidthSizeClass.Medium -> MoneyNavigationType.NAVIGATION_RAIL
        WindowWidthSizeClass.Expanded -> MoneyNavigationType.PERMANENT_DRAWER
        else -> MoneyNavigationType.BOTTOM_NAVIGATION
    }

    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    MoneyManagerTheme(themeMode = settings.themeMode, spendingStatus = spendingStatus) {
        CompositionLocalProvider(
            LocalRepository provides repository,
            LocalUserSettings provides settings,
            LocalSettingsRepository provides settingsRepository,
            LocalFabVisible provides fabVisible,
            LocalActivityCalendarListState provides activityCalendarListState,
            LocalFeatureDiscovery provides featureDiscoveryViewModel
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (!isAppReady) {
                        LoadingScreen()
                    } else if (settings.onboardingComplete) {
                        AppScaffold(
                            navController = navController,
                            navigationType = navigationType,
                            dashboardListState = dashboardListState,
                            activityListState = activityListState,
                            settingsListState = settingsListState,
                            budgetsListState = budgetsListState,
                            savingsListState = savingsListState,
                            allTransactions = allTransactions,
                            onThemeModeChange = { mode -> scope.launch { settingsRepository.setThemeMode(mode) } },
                            onCurrencyChange = { code -> scope.launch { settingsRepository.setCurrencyCode(code) } },
                            onLanguageChange = { tag ->
                                scope.launch {
                                    settingsRepository.setLanguageTag(tag)
                                    // Apply language change using AppCompatDelegate
                                    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(tag)
                                    AppCompatDelegate.setApplicationLocales(appLocale)
                                }
                            }
                        )
                    } else {
                        OnboardingScreen(
                            onLanguageChange = { tag ->
                                scope.launch {
                                    settingsRepository.setLanguageTag(tag)
                                    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(tag)
                                    AppCompatDelegate.setApplicationLocales(appLocale)
                                }
                            },
                            onCurrencyChange = { code -> scope.launch { settingsRepository.setCurrencyCode(code) } },
                            onDone = {
                                scope.launch { settingsRepository.setOnboardingComplete(true) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(
    navController: NavHostController,
    navigationType: MoneyNavigationType,
    dashboardListState: LazyListState,
    activityListState: LazyListState,
    settingsListState: LazyListState,
    budgetsListState: LazyListState,
    savingsListState: LazyListState,
    allTransactions: List<com.v94studio.moneymanager.data.TransactionWithCategory>,
    onThemeModeChange: (ThemeMode) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showUi = currentRoute in listOf(
        NavRoutes.Dashboard.route,
        NavRoutes.Transactions.route,
        NavRoutes.Planning.route,
        NavRoutes.Settings.route
    )

    Row(modifier = Modifier.fillMaxSize()) {
        if (showUi && navigationType == MoneyNavigationType.PERMANENT_DRAWER) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(240.dp),
                        drawerContainerColor = Color.Transparent
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF6B21A8), // Strong Purple
                                        Color(0xFFEC4899), // Pink
                                        Color(0xFFF0ABFC)  // Light Pink
                                    )
                                )
                            ),
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.ExtraBold
                        )
                        AppDestination.all.forEach { destination ->
                            NavigationDrawerItem(
                                label = { Text(stringResource(destination.labelRes)) },
                                selected = currentDestination?.route == destination.route,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(destination.icon, contentDescription = stringResource(destination.labelRes)) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                            )
                        }
                    }
                }
            ) {
                MainContent(
                    navController = navController,
                    currentDestination = currentDestination,
                    showUi = true,
                    navigationType = navigationType,
                    dashboardListState = dashboardListState,
                    activityListState = activityListState,
                    settingsListState = settingsListState,
                    budgetsListState = budgetsListState,
                    savingsListState = savingsListState,
                    allTransactions = allTransactions,
                    onThemeModeChange = onThemeModeChange,
                    onCurrencyChange = onCurrencyChange,
                    onLanguageChange = onLanguageChange,
                    repository = LocalRepository.current
                )
            }
        } else {
            if (showUi && navigationType == MoneyNavigationType.NAVIGATION_RAIL) {
                NavigationRail(
                    containerColor = Color.Transparent,
                    header = {
                        IconButton(onClick = { navController.navigate(NavRoutes.Dashboard.route) }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                ) {
                    Spacer(Modifier.weight(1f))
                    AppDestination.all.forEach { destination ->
                        NavigationRailItem(
                            selected = currentDestination?.route == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = stringResource(destination.labelRes)) },
                            label = { Text(stringResource(destination.labelRes)) }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
            MainContent(
                navController = navController,
                currentDestination = currentDestination,
                showUi = showUi,
                navigationType = navigationType,
                dashboardListState = dashboardListState,
                activityListState = activityListState,
                settingsListState = settingsListState,
                budgetsListState = budgetsListState,
                savingsListState = savingsListState,
                allTransactions = allTransactions,
                onThemeModeChange = onThemeModeChange,
                onCurrencyChange = onCurrencyChange,
                onLanguageChange = onLanguageChange,
                repository = LocalRepository.current
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    navController: NavHostController,
    currentDestination: NavDestination?,
    showUi: Boolean,
    navigationType: MoneyNavigationType,
    dashboardListState: LazyListState,
    activityListState: LazyListState,
    settingsListState: LazyListState,
    budgetsListState: LazyListState,
    savingsListState: LazyListState,
    allTransactions: List<com.v94studio.moneymanager.data.TransactionWithCategory>,
    onThemeModeChange: (ThemeMode) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    repository: MoneyRepository
) {
    val featureDiscoveryViewModel = LocalFeatureDiscovery.current
    val featureDiscoveryState by featureDiscoveryViewModel.uiState.collectAsState()

    val localContext = LocalContext.current
    val scope = rememberCoroutineScope()
    val fabVisibleState = LocalFabVisible.current
    val settings = LocalUserSettings.current
    val route = currentDestination?.route ?: ""
    val isActivityScreen = route == NavRoutes.Transactions.route
    val initialFabOffset = when {
        isActivityScreen && settings.hasSeenFabTutorial && fabVisibleState.value -> 20f
        isActivityScreen && settings.hasSeenFabTutorial -> 30f
        else -> 0f
    }
    val fabOffset = remember(isActivityScreen) { Animatable(initialFabOffset) }
    var previousFabRoute by remember { mutableStateOf<String?>(null) }
    val targetFabOffset = when {
        featureDiscoveryState.isVisible && featureDiscoveryState.currentTutorial == TutorialType.FAB -> 0f
        isActivityScreen && !settings.hasSeenFabTutorial -> 0f
        fabVisibleState.value -> if (isActivityScreen) 20f else 0f
        else -> if (isActivityScreen) 30f else 10f
    }

    LaunchedEffect(route, targetFabOffset) {
        val stayedOnSameScreen = previousFabRoute == route
        previousFabRoute = route
        if (!isActivityScreen) {
            return@LaunchedEffect
        }
        if (stayedOnSameScreen) {
            fabOffset.animateTo(
                targetValue = targetFabOffset,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )
        } else {
            fabOffset.snapTo(targetFabOffset)
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (consumed.y < -5f) {
                    fabVisibleState.value = false
                } else if (consumed.y > 5f) {
                    fabVisibleState.value = true
                }
                return super.onPostScroll(consumed, available, source)
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LocalGradientColors.current.top, LocalGradientColors.current.bottom)
                )
            )
            .nestedScroll(nestedScrollConnection)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        val route = currentDestination?.route ?: ""
        val activityCalendarListState = LocalActivityCalendarListState.current
        val activityCalendarResetIndex = remember { activityCalendarStartIndex() }
        var previousCalendarRoute by remember { mutableStateOf(route) }
        var previousTutorialRoute by remember { mutableStateOf(route) }

        LaunchedEffect(route) {
            // An empty route is Navigation Compose initializing, not the user
            // leaving a screen. Dismissing here consumes the Dashboard tour
            // before its first frame can be drawn.
            if (previousTutorialRoute.isNotBlank() && previousTutorialRoute != route) {
                featureDiscoveryViewModel.dismissTutorialForNavigation()
            }
            previousTutorialRoute = route
        }

        LaunchedEffect(route) {
            if (
                previousCalendarRoute == NavRoutes.Transactions.route &&
                route != NavRoutes.Transactions.route
            ) {
                kotlinx.coroutines.delay(500)
                activityCalendarListState.scrollToItem(activityCalendarResetIndex)
            }
            previousCalendarRoute = route
        }
        
        LaunchedEffect(route, featureDiscoveryState.isLoaded) {
            if (featureDiscoveryState.isLoaded && route == NavRoutes.Transactions.route) {
                featureDiscoveryViewModel.showFabTutorial()
            }
        }

        val hasCustomHeader = route == NavRoutes.MonthlyReport.route ||
                            route == NavRoutes.Accounts.route || 
                            route == NavRoutes.Categories.route || 
                            route == NavRoutes.Transactions.route || 
                            route == NavRoutes.Planning.route || 
                            route.startsWith(NavRoutes.AddTransaction.route)

        var retainedHeaderData by remember {
            mutableStateOf(
                when (route) {
                    NavRoutes.Dashboard.route -> "Overview" to "Quick Snapshot"
                    NavRoutes.Settings.route -> "Settings" to "Customize your experience"
                    else -> currentDestination.title(localContext) to null
                }
            )
        }
        var showRetainedHeader by remember { mutableStateOf(!hasCustomHeader) }

        LaunchedEffect(route) {
            if (!hasCustomHeader) {
                retainedHeaderData = when (route) {
                    NavRoutes.Dashboard.route -> "Overview" to "Quick Snapshot"
                    NavRoutes.Settings.route -> "Settings" to "Customize your experience"
                    else -> currentDestination.title(localContext) to null
                }
                showRetainedHeader = true
            } else if (
                route == NavRoutes.Accounts.route ||
                route == NavRoutes.Categories.route ||
                route == NavRoutes.Transactions.route
            ) {
                // Keep the outgoing header geometry stable until the custom destination
                // app bar has completed its initial measurement and draw.
                repeat(8) { withFrameNanos { } }
                showRetainedHeader = false
            } else {
                showRetainedHeader = false
            }
        }

        val fabConfig = when (route) {
            NavRoutes.Transactions.route -> {
                FabConfig(Icons.Default.Add, stringResource(R.string.fab_add_transaction)) {
                    featureDiscoveryViewModel.dismissTutorial()
                    navController.navigate(NavRoutes.AddTransaction.route)
                }
            }
            else -> null
        }
        var isActivityFabReady by remember { mutableStateOf(false) }
        LaunchedEffect(route) {
            isActivityFabReady = false
            if (route == NavRoutes.Transactions.route) {
                withFrameNanos { }
                isActivityFabReady = true
            }
        }
        val showFab = fabConfig != null && showUi && isActivityFabReady

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (showRetainedHeader) {
                    com.v94studio.moneymanager.ui.components.MoneyTopAppBar(
                        title = retainedHeaderData.first,
                        subtitle = retainedHeaderData.second,
                        scrollBehavior = scrollBehavior
                    )
                }
            },
            snackbarHost = { },
            floatingActionButton = {
                if (fabConfig != null && showFab) {
                        FloatingActionButton(
                            onClick = fabConfig.onClick,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(
                                    bottom = if (navigationType == MoneyNavigationType.BOTTOM_NAVIGATION) 77.dp else 1.dp
                                )
                                .offset(y = fabOffset.value.dp)
                                .size(56.dp)
                                .discoverable { featureDiscoveryViewModel.onTargetPositioned(TutorialType.FAB, it) }
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
                                    .discoverable { featureDiscoveryViewModel.onTargetPositioned(TutorialType.FAB, it) }
                                    .background(Brush.linearGradient(BrandPurpleGradient))
                                    .then(
                                        if (isDark) Modifier.background(Color.Black.copy(alpha = 0.4f)) else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = fabConfig.icon,
                                    contentDescription = fabConfig.label,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                }
            },
            floatingActionButtonPosition = FabPosition.End
        ) { innerPadding ->
            // Use system top and bottom padding
            val topPadding = innerPadding.calculateTopPadding()
            val bottomPadding = if (
                showUi && navigationType == MoneyNavigationType.BOTTOM_NAVIGATION
            ) {
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 76.dp
            } else {
                innerPadding.calculateBottomPadding()
            }

            // Adaptive content area
            val contentModifier = remember(navigationType) {
                if (navigationType != MoneyNavigationType.BOTTOM_NAVIGATION) {
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 1200.dp)
                } else {
                    Modifier
                        .fillMaxSize()
                }
            }

            CompositionLocalProvider(
                LocalTopPadding provides topPadding,
                LocalBottomPadding provides bottomPadding
            ) {
                val mainTabRoutes = remember {
                    setOf(
                        NavRoutes.Dashboard.route,
                        NavRoutes.Transactions.route,
                        NavRoutes.Planning.route,
                        NavRoutes.Settings.route,
                        NavRoutes.Accounts.route,
                        NavRoutes.Categories.route
                    )
                }
                val addTransactionRoute = "${NavRoutes.AddTransaction.route}?transactionId={transactionId}"
                val shouldCrossfade: (String?, String?) -> Boolean = { from, to ->
                    (from in mainTabRoutes && to in mainTabRoutes) ||
                        (from == NavRoutes.Transactions.route && to == addTransactionRoute) ||
                        (from == addTransactionRoute && to == NavRoutes.Transactions.route)
                }
                Box(modifier = contentModifier) {
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.Dashboard.route,
                        enterTransition = {
                            if (shouldCrossfade(initialState.destination.route, targetState.destination.route)) {
                                fadeIn(tween(120))
                            } else EnterTransition.None
                        },
                        exitTransition = {
                            if (shouldCrossfade(initialState.destination.route, targetState.destination.route)) {
                                fadeOut(tween(120))
                            } else ExitTransition.None
                        },
                        popEnterTransition = {
                            if (shouldCrossfade(initialState.destination.route, targetState.destination.route)) {
                                fadeIn(tween(120))
                            } else EnterTransition.None
                        },
                        popExitTransition = {
                            if (shouldCrossfade(initialState.destination.route, targetState.destination.route)) {
                                fadeOut(tween(120))
                            } else ExitTransition.None
                        }
                    ) {
                        composable(NavRoutes.Dashboard.route) {
                            DashboardScreen(
                                onViewAllTransactions = { navController.navigate(NavRoutes.Transactions.route) },
                                listState = dashboardListState
                            )
                        }
                        composable(NavRoutes.Transactions.route) {
                            ActivityScreen(
                                onEditTransaction = { id ->
                                    navController.navigate("${NavRoutes.AddTransaction.route}?transactionId=$id")
                                },
                                onViewReport = { navController.navigate(NavRoutes.MonthlyReport.route) },
                                listState = activityListState,
                                transactions = allTransactions
                            )
                        }
                        composable(NavRoutes.Planning.route) { 
                            PlanningScreen(
                                budgetsListState = budgetsListState,
                                savingsListState = savingsListState
                            )
                        }
                        composable(NavRoutes.Settings.route) {
                            var showRestoreConfirmation by rememberSaveable { mutableStateOf(false) }
                            val csvExportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
                            ) { uri ->
                                uri?.let {
                                    scope.launch {
                                        val csv = repository.exportCsv()
                                        val success = BackupHelper.writeCsvToUri(localContext, it, csv)
                                        if (success) {
                                            android.widget.Toast.makeText(localContext, "CSV exported successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(localContext, "Failed to export CSV.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }

                            val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
                            ) { uri ->
                                uri?.let {
                                    val success = BackupHelper.writeBackupToUri(localContext, it)
                                    if (success) {
                                        android.widget.Toast.makeText(localContext, "Backup saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(localContext, "Failed to save backup.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            val restoreLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                            ) { uri ->
                                uri?.let {
                                    val success = BackupHelper.restoreBackup(localContext, it)
                                    if (success) {
                                        android.widget.Toast.makeText(localContext, "Backup restored! Please restart the app.", android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        android.widget.Toast.makeText(localContext, "Failed to restore backup.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            SettingsScreen(
                                onThemeModeChange = onThemeModeChange,
                                onCurrencyChange = onCurrencyChange,
                                onLanguageChange = onLanguageChange,
                                onManageAccounts = { navController.navigate(NavRoutes.Accounts.route) },
                                onManageCategories = { navController.navigate(NavRoutes.Categories.route) },
                                onExportCsv = {
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    csvExportLauncher.launch("money_manager_$timestamp.csv")
                                },
                                onExportBackup = {
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    exportLauncher.launch("MoneyManager_Backup_$timestamp.mmback")
                                },
                                onRestoreBackup = { showRestoreConfirmation = true },
                                listState = settingsListState
                            )

                            if (showRestoreConfirmation) {
                                com.v94studio.moneymanager.ui.components.PremiumAlertDialog(
                                    onDismissRequest = { showRestoreConfirmation = false },
                                    title = { Text("Restore backup?") },
                                    text = { Text("Restoring a backup replaces your current app data. You can cancel now or choose a backup file to continue.") },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                showRestoreConfirmation = false
                                                restoreLauncher.launch("*/*")
                                            },
                                            modifier = Modifier.width(104.dp).height(36.dp),
                                            shape = RoundedCornerShape(50),
                                            contentPadding = PaddingValues(horizontal = 10.dp)
                                        ) {
                                            Text("Continue", fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        OutlinedButton(
                                            onClick = { showRestoreConfirmation = false },
                                            modifier = Modifier.width(104.dp).height(36.dp),
                                            shape = RoundedCornerShape(50),
                                            contentPadding = PaddingValues(horizontal = 10.dp)
                                        ) {
                                            Text("Cancel", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                )
                            }
                        }
                        composable(NavRoutes.Categories.route) {
                            CategoriesScreen(onBack = { navController.popBackStack() })
                        }
                        composable(NavRoutes.Recurring.route) {
                            RecurringScreen(onBack = { navController.popBackStack() })
                        }
                        composable(NavRoutes.Accounts.route) {
                            AccountsScreen(onBack = { navController.popBackStack() })
                        }
                        composable(
                            route = NavRoutes.MonthlyReport.route,
                            enterTransition = {
                                slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(durationMillis = 240)
                                ) + fadeIn(animationSpec = tween(durationMillis = 180))
                            },
                            popExitTransition = {
                                slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(durationMillis = 220)
                                ) + fadeOut(animationSpec = tween(durationMillis = 160))
                            }
                        ) {
                            MonthlyReportScreen(onBack = { navController.popBackStack() })
                        }
                        composable(
                            route = addTransactionRoute,
                            arguments = listOf(androidx.navigation.navArgument("transactionId") {
                                type = androidx.navigation.NavType.LongType
                                defaultValue = -1L
                            })
                        ) { entry ->
                            val id = entry.arguments?.getLong("transactionId") ?: -1L
                            val transactions by repository.observeTransactions().collectAsState(initial = emptyList())
                            val existing = transactions.find { it.transaction.id == id }?.transaction
                            AddTransactionScreen(
                                existingTransaction = existing,
                                onDone = { navController.popBackStack() },
                                onNavigateToRecurring = { navController.navigate(NavRoutes.Recurring.route) }
                            )
                        }
                    }
                }
            }
        }

        // Overlay navigation keeps the NavHost at a constant full-screen size.
        // Showing or hiding this bar no longer changes Scaffold measurements.
        if (navigationType == MoneyNavigationType.BOTTOM_NAVIGATION) {
            AnimatedVisibility(
                visible = showUi,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                label = "BottomBarOverlayVisibility"
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(90.dp)
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.15f to MaterialTheme.colorScheme.background.copy(alpha = 0.15f),
                                    0.3f to MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                                    0.5f to MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                                    0.7f to MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                                    1.0f to MaterialTheme.colorScheme.background
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 17.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BottomBar(currentDestination = currentDestination) { dest ->
                            if (currentDestination?.route == dest.route) {
                                scope.launch {
                                    when (dest.route) {
                                        NavRoutes.Dashboard.route -> dashboardListState.animateScrollToItem(0)
                                        NavRoutes.Transactions.route -> activityListState.animateScrollToItem(0)
                                        NavRoutes.Planning.route -> {
                                            launch { budgetsListState.animateScrollToItem(0) }
                                            launch { savingsListState.animateScrollToItem(0) }
                                        }
                                        NavRoutes.Settings.route -> settingsListState.animateScrollToItem(0)
                                    }
                                }
                            } else {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                }
            }
        }


        // Top-aligned Snackbar (Notification style)
        SnackbarHost(
            hostState = remember { SnackbarHostState() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
                .widthIn(max = 480.dp),
            snackbar = { data ->
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallet,
                            contentDescription = null,
                            tint = MMActivePurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = data.visuals.message,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (data.visuals.actionLabel != null) {
                            TextButton(
                                onClick = { data.performAction() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MMActivePurple)
                            ) {
                                Text(
                                    text = data.visuals.actionLabel!!,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

private fun NavDestination?.title(context: android.content.Context): String {
    val route = this?.route ?: return context.getString(R.string.app_name)
    return when {
        route.startsWith(NavRoutes.AddTransaction.route) -> context.getString(R.string.title_transaction)
        route == NavRoutes.Recurring.route -> context.getString(R.string.title_recurring)
        route == NavRoutes.Accounts.route -> context.getString(R.string.title_accounts)
        route == NavRoutes.Settings.route -> context.getString(R.string.nav_settings)
        route == NavRoutes.Planning.route -> context.getString(R.string.nav_planning)
        route == NavRoutes.Transactions.route -> context.getString(R.string.nav_activity)
        route == NavRoutes.MonthlyReport.route -> "Monthly Report"
        else -> {
            val dest = AppDestination.all.find { it.route == route }
            if (dest != null) context.getString(dest.labelRes) else context.getString(R.string.app_name)
        }
    }
}
