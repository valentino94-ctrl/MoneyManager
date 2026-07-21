package com.v94studio.moneymanager.data

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class CategoryType {
    INCOME,
    EXPENSE
}

fun TransactionType.toCategoryType(): CategoryType {
    return when (this) {
        TransactionType.INCOME -> CategoryType.INCOME
        TransactionType.EXPENSE -> CategoryType.EXPENSE
    }
}

data class CategorySpend(
    val categoryId: Long,
    val total: Double
)

data class CategorySpendNamed(
    val categoryId: Long,
    val categoryName: String,
    val total: Double
)

data class BudgetProgress(
    val budgetId: Long,
    val categoryId: Long,
    val categoryName: String,
    val limit: Double,
    val spent: Double,
    val rollingDays: Int
)

data class DailyTotal(
    val day: String,
    val total: Double
)

data class ActivityDay(
    val day: String, // yyyy-MM-dd
    val hasIncome: Boolean,
    val hasExpense: Boolean
)

data class MonthlyTotal(
    val month: String,
    val total: Double
)

data class CurrencyOption(
    val code: String,
    val name: String
)
