package com.v94studio.moneymanager.ui.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.v94studio.moneymanager.LocalFabVisible
import com.v94studio.moneymanager.LocalUserSettings
import com.v94studio.moneymanager.R
import com.v94studio.moneymanager.ui.settings.ThemeMode
import com.v94studio.moneymanager.ui.settings.UserSettings
import java.util.Locale

data class AppDestination(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    companion object {
        val all = listOf(
            AppDestination(NavRoutes.Dashboard.route, R.string.nav_dashboard, Icons.Filled.Home),
            AppDestination(NavRoutes.Transactions.route, R.string.nav_activity, Icons.Filled.History),
            AppDestination(NavRoutes.Planning.route, R.string.nav_planning, Icons.Filled.CalendarMonth),
            AppDestination(NavRoutes.Settings.route, R.string.nav_settings, Icons.Filled.Settings),
        )
    }
}

sealed class NavRoutes(val route: String) {
    data object Dashboard : NavRoutes("dashboard")
    data object Accounts : NavRoutes("accounts")
    data object Transactions : NavRoutes("transactions")
    data object Planning : NavRoutes("planning")
    data object Budgets : NavRoutes("budgets")
    data object SavingGoals : NavRoutes("saving_goals")
    data object Categories : NavRoutes("categories")
    data object Settings : NavRoutes("settings")
    data object AddTransaction : NavRoutes("add_transaction")
    data object Recurring : NavRoutes("recurring")
    data object MonthlyReport : NavRoutes("monthly_report")
}

val MMActivePurple = Color(0xFF7C4DFF)

@Composable
fun BottomBar(
    currentDestination: NavDestination?,
    onNavigate: (AppDestination) -> Unit,
) {
    val settings = LocalUserSettings.current
    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val isVisible = LocalFabVisible.current.value
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.87f,
        animationSpec = tween(durationMillis = 300),
        label = "bottomBarScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.65f)
            .padding(vertical = 5.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = if (isDark) Color.Black else Color.Black.copy(alpha = 0.5f)
            )
            .then(
                if (isDark) {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF4A3475).copy(alpha = 0.97f),
                                Color(0xFF160D29).copy(alpha = 0.95f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                } else {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.97f),
                                Color(0xFFEDE9FE).copy(alpha = 0.95f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                }
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = if (isDark) 0.14f else 0.34f),
                        0.35f to Color.White.copy(alpha = if (isDark) 0.05f else 0.12f),
                        1.0f to Color.Transparent
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppDestination.all.forEach { destination ->
                val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            onNavigate(destination)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes),
                        modifier = Modifier.size(26.dp),
                        tint = if (selected) {
                            MMActivePurple
                        } else {
                            if (isDark) Color(0xFF424242) else MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun BottomBarPreview() {
    val fabVisible = remember { mutableStateOf(value = true) }
    val settings = UserSettings(
        themeMode = ThemeMode.SYSTEM,
        currencyCode = "USD",
        rollingDays = 30,
        transactionsFilter = com.v94studio.moneymanager.ui.settings.TransactionFilterSetting.ALL,
        transactionsRangeDays = 30,
        transactionsSort = com.v94studio.moneymanager.ui.settings.TransactionSortSetting.NEWEST,
        transactionsQuery = "",
        biometricEnabled = false,
        privacyModeEnabled = false,
        autoApproveRecurring = true,
        onboardingComplete = true,
        hasCompletedTour = false,
        hasSeenFabTutorial = false,
        hasSeenSwipeTutorial = false,
        hasSeenDashboardTutorial = false,
        hasSeenPlanningTutorial = false,
        hasSeenSavingsTutorial = false,
        hasSeenBudgetsTutorial = false,
        languageTag = Locale.getDefault().toLanguageTag(),
    )
    CompositionLocalProvider(
        LocalFabVisible provides fabVisible,
        LocalUserSettings provides settings
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                BottomBar(
                    currentDestination = null,
                    onNavigate = {},
                )
            }
        }
    }
}
