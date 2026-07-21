package com.v94studio.moneymanager.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MoneyRepository(
    private val db: AppDatabase
) {
    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
    private val transactionDao = db.transactionDao()
    private val budgetDao = db.budgetDao()
    private val recurringDao = db.recurringDao()
    private val savingGoalDao = db.savingGoalDao()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val transactionsState = transactionDao.observeAllWithCategory()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())
    private val accountsState = accountDao.observeAll()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())
    private val categoriesState = categoryDao.observeAll()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())
    private val budgetsState = budgetDao.observeAllWithCategory()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())
    private val recurringState = recurringDao.observeAllWithCategory()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())
    private val savingGoalsState = savingGoalDao.observeAll()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())
    private val accountsWithBalancesState = combine(accountsState, transactionsState) { accounts, transactions ->
        val transactionsByAccount = transactions.groupBy { it.transaction.accountId }
        accounts.map { account ->
            val accountTransactions = transactionsByAccount[account.id].orEmpty()
            val balanceChange = accountTransactions.sumOf { item ->
                if (item.transaction.type == TransactionType.INCOME) {
                    item.transaction.amount
                } else {
                    -item.transaction.amount
                }
            }
            AccountWithBalance(account, account.startingBalance + balanceChange)
        }
    }.stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    fun observeAccounts(): StateFlow<List<AccountEntity>> = accountsState

    fun observeAccountsWithBalances(): StateFlow<List<AccountWithBalance>> = accountsWithBalancesState

    fun observeCategories(): StateFlow<List<CategoryEntity>> = categoriesState

    fun observeTransactions(): StateFlow<List<TransactionWithCategory>> = transactionsState

    fun observeBudgets(): StateFlow<List<BudgetWithCategory>> = budgetsState

    fun observeRecurring(): StateFlow<List<RecurringWithCategory>> = recurringState

    fun observeSavingGoals(): StateFlow<List<SavingGoalEntity>> = savingGoalsState

    fun observeTotalExpenseBetween(start: Long, end: Long): Flow<Double> =
        transactionDao.observeSumMinorByTypeBetween(TransactionType.EXPENSE, start, end)
            .map { it?.let(::minorToAmount) ?: 0.0 }

    fun observeIncomeSince(since: Long): Flow<Double?> =
        transactionDao.observeSumMinorByTypeSince(TransactionType.INCOME, since)
            .map { it?.let(::minorToAmount) }

    fun observeExpenseSince(since: Long): Flow<Double?> =
        transactionDao.observeSumMinorByTypeSince(TransactionType.EXPENSE, since)
            .map { it?.let(::minorToAmount) }

    fun observeSumByTypeBetween(type: TransactionType, start: Long, end: Long): Flow<Double?> =
        transactionDao.observeSumMinorByTypeBetween(type, start, end)
            .map { it?.let(::minorToAmount) }

    fun observeDailyTotalsSince(type: TransactionType, since: Long): Flow<List<DailyTotal>> =
        transactionDao.observeDailyTotalsSince(type, since)

    fun observeCategorySpendNamedSince(type: TransactionType, since: Long): Flow<List<CategorySpendNamed>> =
        transactionDao.observeCategorySpendNamedSince(type, since)

    fun observeCategorySpendNamedBetween(type: TransactionType, start: Long, end: Long): Flow<List<CategorySpendNamed>> =
        transactionDao.observeCategorySpendNamedBetween(type, start, end)

    fun observeMonthlyTotalsSince(type: TransactionType, since: Long): Flow<List<MonthlyTotal>> =
        transactionDao.observeMonthlyTotalsSince(type, since)

    fun observeActivityDays(start: Long, end: Long): Flow<List<ActivityDay>> =
        transactionDao.observeActivityDays(start, end)

    fun observeDashboardOverview(monthStart: Long): Flow<DashboardOverview> {
        return combine(
            observeIncomeSince(0L),
            observeExpenseSince(0L),
            observeIncomeSince(monthStart),
            observeExpenseSince(monthStart)
        ) { totalIncome, totalExpense, monthIncome, monthExpense ->
            DashboardOverview(
                totalIncome = totalIncome ?: 0.0,
                totalExpense = totalExpense ?: 0.0,
                monthIncome = monthIncome ?: 0.0,
                monthExpense = monthExpense ?: 0.0
            )
        }
    }

    fun observeBudgetProgress(now: Long = System.currentTimeMillis()): Flow<List<BudgetProgress>> {
        return combine(observeBudgets(), observeTransactions()) { budgets, transactions ->
            // Distinct by categoryId to avoid duplicates if DB unique constraint wasn't applied yet
            budgets.distinctBy { it.budget.categoryId }.map { budget ->
                val since = now - budget.budget.rollingDays * DAY_MILLIS
                val spent = transactions
                    .filter { item ->
                        item.transaction.type == TransactionType.EXPENSE &&
                            item.transaction.categoryId == budget.budget.categoryId &&
                            item.transaction.timestamp >= since
                    }
                    .sumOf { it.transaction.amount }
                BudgetProgress(
                    budgetId = budget.budget.id,
                    categoryId = budget.budget.categoryId,
                    categoryName = budget.categoryName,
                    limit = budget.budget.amount,
                    spent = spent,
                    rollingDays = budget.budget.rollingDays
                )
            }
        }
    }

    suspend fun upsertTransaction(item: TransactionEntity) {
        transactionDao.insert(item)
    }

    suspend fun deleteTransaction(item: TransactionEntity) {
        transactionDao.delete(item)
    }

    suspend fun addCategory(name: String, type: CategoryType, emoji: String = "📦") {
        addCategoryAndReturnId(name, type, emoji)
    }

    suspend fun updateCategory(item: CategoryEntity) {
        require(!categoryDao.nameExists(item.name, item.id)) {
            "A category with this name already exists"
        }
        categoryDao.update(item)
    }

    suspend fun deleteCategory(item: CategoryEntity) {
        categoryDao.delete(item)
    }

    suspend fun addAccount(name: String, type: String, startingBalance: Double = 0.0, emoji: String = "🏦"): Long {
        return accountDao.insert(
            AccountEntity(
                name = name.trim(),
                type = type.trim().ifBlank { "Cash" },
                startingBalanceMinor = amountToMinor(startingBalance),
                emoji = emoji
            )
        )
    }

    suspend fun deleteAccount(item: AccountEntity) {
        accountDao.delete(item)
    }

    suspend fun updateAccount(item: AccountEntity) {
        accountDao.update(item)
    }

    suspend fun addCategoryAndReturnId(name: String, type: CategoryType, emoji: String = "📦"): Long {
        val trimmedName = name.trim()
        require(!categoryDao.nameExists(trimmedName)) {
            "A category with this name already exists"
        }
        return categoryDao.insert(CategoryEntity(name = trimmedName, type = type, emoji = emoji))
    }

    suspend fun addBudget(categoryId: Long, amount: Double, rollingDays: Int) {
        budgetDao.insert(
            BudgetEntity(
                categoryId = categoryId,
                amountMinor = amountToMinor(amount),
                rollingDays = rollingDays
            )
        )
    }

    suspend fun updateBudget(budget: BudgetEntity) {
        budgetDao.update(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        budgetDao.delete(budget)
    }

    suspend fun deleteBudgetById(id: Long) {
        budgetDao.deleteById(id)
    }

    suspend fun addRecurring(
        amount: Double,
        type: TransactionType,
        categoryId: Long?,
        accountId: Long?,
        note: String,
        intervalDays: Int,
        nextRunAt: Long
    ) {
        recurringDao.insert(
            RecurringEntity(
                amountMinor = amountToMinor(amount),
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                note = note,
                intervalDays = intervalDays,
                nextRunAt = nextRunAt
            )
        )
    }

    suspend fun deleteRecurring(item: RecurringEntity) {
        recurringDao.delete(item)
    }

    suspend fun addSavingGoal(
        name: String,
        targetAmount: Double,
        deadline: Long? = null,
        icon: String? = null
    ) {
        savingGoalDao.insert(
            SavingGoalEntity(
                name = name,
                targetAmountMinor = amountToMinor(targetAmount),
                deadline = deadline,
                icon = icon
            )
        )
    }

    suspend fun updateSavingGoal(goal: SavingGoalEntity) {
        savingGoalDao.update(goal)
    }

    suspend fun deleteSavingGoal(goal: SavingGoalEntity) {
        savingGoalDao.delete(goal)
    }

    suspend fun addFundsToGoal(goal: SavingGoalEntity, amount: Double) {
        val newSavedMinor = goal.savedAmountMinor + amountToMinor(amount)
        savingGoalDao.update(goal.copy(savedAmountMinor = newSavedMinor))
    }

    suspend fun skipRecurring(item: RecurringEntity, now: Long = System.currentTimeMillis()) {
        recurringDao.update(item.copy(nextRunAt = nextFutureRun(item, now)))
    }

    suspend fun payRecurringNow(item: RecurringEntity, now: Long = System.currentTimeMillis()) {
        db.withTransaction {
            transactionDao.insert(
                TransactionEntity(
                    amountMinor = item.amountMinor,
                    type = item.type,
                    categoryId = item.categoryId,
                    accountId = item.accountId,
                    note = item.note,
                    timestamp = now
                )
            )
            recurringDao.update(item.copy(nextRunAt = nextFutureRun(item, now)))
        }
    }

    suspend fun processDueRecurring(now: Long = System.currentTimeMillis()) {
        db.withTransaction {
            recurringDao.getDue(now).forEach { recurring ->
                if (recurring.intervalDays <= 0) return@forEach

                var runAt = recurring.nextRunAt
                val intervalMillis = recurring.intervalDays * DAY_MILLIS

                while (runAt <= now) {
                    transactionDao.insert(
                        TransactionEntity(
                            amountMinor = recurring.amountMinor,
                            type = recurring.type,
                            categoryId = recurring.categoryId,
                            accountId = recurring.accountId,
                            note = recurring.note,
                            timestamp = runAt
                        )
                    )
                    runAt += intervalMillis
                }

                recurringDao.update(recurring.copy(nextRunAt = runAt))
            }
        }
    }

    suspend fun seedDefaultsIfEmpty() {
        if (categoryDao.count() > 0) return
        val defaults = listOf(
            CategoryEntity(name = "Salary", type = CategoryType.INCOME, emoji = "💰"),
            CategoryEntity(name = "Freelance", type = CategoryType.INCOME, emoji = "💼"),
            CategoryEntity(name = "Food", type = CategoryType.EXPENSE, emoji = "🍲"),
            CategoryEntity(name = "Rent", type = CategoryType.EXPENSE, emoji = "🏠"),
            CategoryEntity(name = "Transport", type = CategoryType.EXPENSE, emoji = "🚗"),
            CategoryEntity(name = "Utilities", type = CategoryType.EXPENSE, emoji = "⚡"),
            CategoryEntity(name = "Shopping", type = CategoryType.EXPENSE, emoji = "🛒")
        )
        defaults.forEach { categoryDao.insert(it) }
    }

    suspend fun seedDefaultAccountIfEmpty() {
        if (accountDao.count() > 0) return
        val defaults = listOf(
            AccountEntity(name = "Checking Account", type = "Checking Account", startingBalanceMinor = 0L, emoji = "🏦"),
            AccountEntity(name = "Savings Account", type = "Savings Account", startingBalanceMinor = 0L, emoji = "💰"),
            AccountEntity(name = "Cash Wallet", type = "Cash Wallet", startingBalanceMinor = 0L, emoji = "💵")
        )
        defaults.forEach { accountDao.insert(it) }
    }

    suspend fun exportCsv(): String {
        val transactions = transactionDao.observeAllWithCategory().first()
        val header = "Date,Amount,Type,Category,Account,Note\n"
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        
        val body = transactions.joinToString(separator = "\n") { item ->
            csvRow(
                dateFormat.format(java.util.Date(item.transaction.timestamp)),
                item.transaction.amount,
                item.transaction.type.name,
                item.categoryName ?: "",
                item.accountName ?: "",
                item.transaction.note
            )
        }
        return header + body
    }

    private fun nextFutureRun(item: RecurringEntity, now: Long): Long {
        return nextFutureRunAt(
            currentRunAt = item.nextRunAt,
            intervalDays = item.intervalDays,
            now = now
        )
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
