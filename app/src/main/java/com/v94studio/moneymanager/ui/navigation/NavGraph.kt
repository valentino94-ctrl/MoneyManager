package com.v94studio.moneymanager.ui.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.v94studio.moneymanager.LocalRepository
import com.v94studio.moneymanager.ui.settings.ThemeMode
import com.v94studio.moneymanager.ui.screens.AddTransactionScreen
import com.v94studio.moneymanager.ui.screens.AccountsScreen
import com.v94studio.moneymanager.ui.screens.BudgetsScreen
import com.v94studio.moneymanager.ui.screens.CategoriesScreen
import com.v94studio.moneymanager.ui.screens.DashboardScreen
import com.v94studio.moneymanager.ui.screens.RecurringScreen
import com.v94studio.moneymanager.ui.screens.PlanningScreen
import com.v94studio.moneymanager.ui.screens.SavingGoalsScreen
import com.v94studio.moneymanager.ui.screens.SettingsScreen
import com.v94studio.moneymanager.ui.screens.ActivityScreen
import com.v94studio.moneymanager.ui.screens.MonthlyReportScreen

fun NavGraphBuilder.navGraph(
    navController: NavHostController,
    onThemeModeChange: (ThemeMode) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onExportCsv: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    composable(NavRoutes.Dashboard.route) { 
        DashboardScreen(
            onViewAllTransactions = { navController.navigate(NavRoutes.Transactions.route) }
        ) 
    }
    composable(NavRoutes.Transactions.route) { 
        ActivityScreen(
            onEditTransaction = { id -> 
                navController.navigate("${NavRoutes.AddTransaction.route}?transactionId=$id") 
            },
            onViewReport = { navController.navigate(NavRoutes.MonthlyReport.route) }
        ) 
    }
    composable(NavRoutes.Planning.route) { 
        PlanningScreen()
    }
    composable(NavRoutes.Categories.route) { 
        CategoriesScreen(onBack = { navController.popBackStack() }) 
    }
    composable(NavRoutes.Settings.route) {
        SettingsScreen(
            onThemeModeChange = onThemeModeChange,
            onCurrencyChange = onCurrencyChange,
            onLanguageChange = onLanguageChange,
            onManageAccounts = { navController.navigate(NavRoutes.Accounts.route) },
            onManageCategories = { navController.navigate(NavRoutes.Categories.route) },
            onExportCsv = onExportCsv,
            onExportBackup = onExportBackup,
            onRestoreBackup = onRestoreBackup
        )
    }
    composable(
        route = "${NavRoutes.AddTransaction.route}?transactionId={transactionId}",
        arguments = listOf(
            navArgument("transactionId") {
                type = NavType.LongType
                defaultValue = -1L
            }
        )
    ) { backStackEntry ->
        val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: -1L
        val repository = LocalRepository.current
        val transactions by repository.observeTransactions().collectAsState(initial = emptyList())
        val existingTransaction = transactions.find { it.transaction.id == transactionId }?.transaction
        
        AddTransactionScreen(
            existingTransaction = existingTransaction,
            onDone = { navController.popBackStack() }
        )
    }
    composable(NavRoutes.Recurring.route) { 
        RecurringScreen(onBack = { navController.popBackStack() }) 
    }
    composable(NavRoutes.Accounts.route) { 
        AccountsScreen(onBack = { navController.popBackStack() }) 
    }
    composable(NavRoutes.MonthlyReport.route) {
        MonthlyReportScreen(onBack = { navController.popBackStack() })
    }
}
