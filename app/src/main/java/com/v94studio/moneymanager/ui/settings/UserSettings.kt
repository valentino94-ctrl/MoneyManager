package com.v94studio.moneymanager.ui.settings

data class UserSettings(
    val themeMode: ThemeMode,
    val currencyCode: String,
    val rollingDays: Int,
    val transactionsFilter: TransactionFilterSetting,
    val transactionsRangeDays: Int,
    val transactionsSort: TransactionSortSetting,
    val transactionsQuery: String,
    val biometricEnabled: Boolean,
    val privacyModeEnabled: Boolean,
    val autoApproveRecurring: Boolean,
    val onboardingComplete: Boolean,
    val hasCompletedTour: Boolean,
    val hasSeenFabTutorial: Boolean,
    val hasSeenSwipeTutorial: Boolean,
    val hasSeenDashboardTutorial: Boolean,
    val hasSeenPlanningTutorial: Boolean,
    val hasSeenSavingsTutorial: Boolean,
    val hasSeenBudgetsTutorial: Boolean,
    val languageTag: String
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class TransactionFilterSetting {
    ALL,
    INCOME,
    EXPENSE
}

enum class TransactionSortSetting {
    NEWEST,
    OLDEST,
    AMOUNT_DESC,
    AMOUNT_ASC
}
