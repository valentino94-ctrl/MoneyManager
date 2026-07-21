package com.v94studio.moneymanager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND id != :excludeId)")
    suspend fun nameExists(name: String, excludeId: Long = -1L): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)
}

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT t.*, c.name AS categoryName, c.type AS categoryType, a.name AS accountName, a.emoji AS accountEmoji
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        LEFT JOIN accounts a ON t.accountId = a.id
        ORDER BY t.timestamp DESC
        """
    )
    fun observeAllWithCategory(): Flow<List<TransactionWithCategory>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT SUM(amountMinor) FROM transactions WHERE type = :type AND timestamp >= :since")
    fun observeSumMinorByTypeSince(type: TransactionType, since: Long): Flow<Long?>

    @Query("SELECT SUM(amountMinor) FROM transactions WHERE type = :type AND timestamp BETWEEN :start AND :end")
    fun observeSumMinorByTypeBetween(type: TransactionType, start: Long, end: Long): Flow<Long?>

    @Query(
        """
        SELECT strftime('%Y-%m-%d', datetime(timestamp / 1000, 'unixepoch', 'localtime')) AS day,
               SUM(amountMinor) / 100.0 AS total
        FROM transactions
        WHERE type = :type AND timestamp >= :since
        GROUP BY day
        ORDER BY day ASC
        """
    )
    fun observeDailyTotalsSince(type: TransactionType, since: Long): Flow<List<DailyTotal>>

    @Query(
        """
        SELECT categoryId AS categoryId, SUM(amountMinor) / 100.0 AS total
        FROM transactions
        WHERE type = :type AND timestamp >= :since AND categoryId IS NOT NULL
        GROUP BY categoryId
        """
    )
    fun observeCategorySpendSince(type: TransactionType, since: Long): Flow<List<CategorySpend>>

    @Query(
        """
        SELECT c.id AS categoryId, c.name AS categoryName, SUM(t.amountMinor) / 100.0 AS total
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.type = :type AND t.timestamp >= :since
        GROUP BY c.id, c.name
        ORDER BY total DESC
        """
    )
    fun observeCategorySpendNamedSince(type: TransactionType, since: Long): Flow<List<CategorySpendNamed>>

    @Query(
        """
        SELECT c.id AS categoryId, c.name AS categoryName, SUM(t.amountMinor) / 100.0 AS total
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.type = :type AND t.timestamp BETWEEN :start AND :end
        GROUP BY c.id, c.name
        ORDER BY total DESC
        """
    )
    fun observeCategorySpendNamedBetween(type: TransactionType, start: Long, end: Long): Flow<List<CategorySpendNamed>>

    @Query(
        """
        SELECT strftime('%Y-%m', datetime(timestamp / 1000, 'unixepoch', 'localtime')) AS month,
               SUM(amountMinor) / 100.0 AS total
        FROM transactions
        WHERE type = :type AND timestamp >= :since
        GROUP BY month
        ORDER BY month ASC
        """
    )
    fun observeMonthlyTotalsSince(type: TransactionType, since: Long): Flow<List<MonthlyTotal>>

    @Query(
        """
        SELECT strftime('%Y-%m-%d', datetime(timestamp / 1000, 'unixepoch', 'localtime')) AS day,
               SUM(CASE WHEN type = 'INCOME' THEN 1 ELSE 0 END) > 0 AS hasIncome,
               SUM(CASE WHEN type = 'EXPENSE' THEN 1 ELSE 0 END) > 0 AS hasExpense
        FROM transactions
        WHERE timestamp BETWEEN :start AND :end
        GROUP BY day
        """
    )
    fun observeActivityDays(start: Long, end: Long): Flow<List<ActivityDay>>
}

@Dao
interface BudgetDao {
    @Query(
        """
        SELECT b.*, IFNULL(c.name, 'Uncategorized') AS categoryName
        FROM budgets b
        LEFT JOIN categories c ON b.categoryId = c.id
        ORDER BY categoryName ASC
        """
    )
    fun observeAllWithCategory(): Flow<List<BudgetWithCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)
}

@Dao
interface RecurringDao {
    @Query(
        """
        SELECT r.*, c.name AS categoryName, a.name AS accountName, a.emoji AS accountEmoji
        FROM recurring r
        LEFT JOIN categories c ON r.categoryId = c.id
        LEFT JOIN accounts a ON r.accountId = a.id
        ORDER BY r.nextRunAt ASC
        """
    )
    fun observeAllWithCategory(): Flow<List<RecurringWithCategory>>

    @Query("SELECT * FROM recurring WHERE nextRunAt <= :now ORDER BY nextRunAt ASC")
    suspend fun getDue(now: Long): List<RecurringEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurring: RecurringEntity): Long

    @Update
    suspend fun update(recurring: RecurringEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(recurring: RecurringEntity)
}

@Dao
interface SavingGoalDao {
    @Query("SELECT * FROM saving_goals ORDER BY deadline ASC")
    fun observeAll(): Flow<List<SavingGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingGoalEntity): Long

    @Update
    suspend fun update(goal: SavingGoalEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(goal: SavingGoalEntity)
}
