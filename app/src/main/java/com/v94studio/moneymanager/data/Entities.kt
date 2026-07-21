package com.v94studio.moneymanager.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val emoji: String = "📦"
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val startingBalanceMinor: Long = 0L,
    val emoji: String = "🏦"
) {
    @get:Ignore
    val startingBalance: Double
        get() = minorToAmount(startingBalanceMinor)
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("accountId"), Index("timestamp")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val type: TransactionType,
    val categoryId: Long?,
    val accountId: Long?,
    val note: String,
    val timestamp: Long
) {
    @get:Ignore
    val amount: Double
        get() = minorToAmount(amountMinor)
}

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val amountMinor: Long,
    val rollingDays: Int = 30
) {
    @get:Ignore
    val amount: Double
        get() = minorToAmount(amountMinor)
}

@Entity(
    tableName = "recurring",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("accountId"), Index("nextRunAt")]
)
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val type: TransactionType,
    val categoryId: Long?,
    val accountId: Long?,
    val note: String,
    val intervalDays: Int,
    val nextRunAt: Long
) {
    @get:Ignore
    val amount: Double
        get() = minorToAmount(amountMinor)
}

data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    @ColumnInfo(name = "categoryName") val categoryName: String?,
    @ColumnInfo(name = "categoryType") val categoryType: CategoryType?,
    @ColumnInfo(name = "accountName") val accountName: String?,
    @ColumnInfo(name = "accountEmoji") val accountEmoji: String?
)

data class BudgetWithCategory(
    @Embedded val budget: BudgetEntity,
    @ColumnInfo(name = "categoryName") val categoryName: String
)

data class RecurringWithCategory(
    @Embedded val recurring: RecurringEntity,
    @ColumnInfo(name = "categoryName") val categoryName: String?,
    @ColumnInfo(name = "accountName") val accountName: String?,
    @ColumnInfo(name = "accountEmoji") val accountEmoji: String?
)

data class AccountWithBalance(
    val account: AccountEntity,
    val balance: Double
)

data class DashboardOverview(
    val totalIncome: Double,
    val totalExpense: Double,
    val monthIncome: Double,
    val monthExpense: Double
)

@Entity(tableName = "saving_goals")
data class SavingGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmountMinor: Long,
    val savedAmountMinor: Long = 0L,
    val deadline: Long? = null,
    val icon: String? = null // Emoji or icon name
) {
    @get:Ignore
    val targetAmount: Double
        get() = minorToAmount(targetAmountMinor)
        
    @get:Ignore
    val savedAmount: Double
        get() = minorToAmount(savedAmountMinor)
}
