package com.v94studio.moneymanager

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.v94studio.moneymanager.data.AppDatabase
import com.v94studio.moneymanager.data.MoneyRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "money_manager.db"
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .build()

    val repository: MoneyRepository = MoneyRepository(database)

    private companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS saving_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        targetAmountMinor INTEGER NOT NULL,
                        savedAmountMinor INTEGER NOT NULL DEFAULT 0,
                        deadline INTEGER,
                        icon TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        startingBalance REAL NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE accounts_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        startingBalanceMinor INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO accounts_new (id, name, type, startingBalanceMinor)
                    SELECT id, name, type, CAST(ROUND(startingBalance * 100) AS INTEGER)
                    FROM accounts
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE accounts")
                db.execSQL("ALTER TABLE accounts_new RENAME TO accounts")

                db.execSQL(
                    """
                    CREATE TABLE transactions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amountMinor INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        categoryId INTEGER,
                        note TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO transactions_new (id, amountMinor, type, categoryId, note, timestamp)
                    SELECT id, CAST(ROUND(amount * 100) AS INTEGER), type, categoryId, note, timestamp
                    FROM transactions
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
                db.execSQL("CREATE INDEX index_transactions_categoryId ON transactions(categoryId)")
                db.execSQL("CREATE INDEX index_transactions_timestamp ON transactions(timestamp)")

                db.execSQL(
                    """
                    CREATE TABLE budgets_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        amountMinor INTEGER NOT NULL,
                        rollingDays INTEGER NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO budgets_new (id, categoryId, amountMinor, rollingDays)
                    SELECT id, categoryId, CAST(ROUND(amount * 100) AS INTEGER), rollingDays
                    FROM budgets
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE budgets")
                db.execSQL("ALTER TABLE budgets_new RENAME TO budgets")
                db.execSQL("CREATE INDEX index_budgets_categoryId ON budgets(categoryId)")

                db.execSQL(
                    """
                    CREATE TABLE recurring_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amountMinor INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        categoryId INTEGER,
                        note TEXT NOT NULL,
                        intervalDays INTEGER NOT NULL,
                        nextRunAt INTEGER NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO recurring_new (id, amountMinor, type, categoryId, note, intervalDays, nextRunAt)
                    SELECT id, CAST(ROUND(amount * 100) AS INTEGER), type, categoryId, note, intervalDays, nextRunAt
                    FROM recurring
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE recurring")
                db.execSQL("ALTER TABLE recurring_new RENAME TO recurring")
                db.execSQL("CREATE INDEX index_recurring_categoryId ON recurring(categoryId)")
                db.execSQL("CREATE INDEX index_recurring_nextRunAt ON recurring(nextRunAt)")
            }
        }
    }
}
