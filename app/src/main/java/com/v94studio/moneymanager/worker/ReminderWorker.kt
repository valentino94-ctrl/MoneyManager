package com.v94studio.moneymanager.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.v94studio.moneymanager.AppContainer
import com.v94studio.moneymanager.data.minorToAmount
import com.v94studio.moneymanager.ui.util.NotificationHelper
import com.v94studio.moneymanager.ui.util.formatCurrency
import kotlinx.coroutines.flow.first

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appContainer = AppContainer(applicationContext)
        val repository = appContainer.repository
        
        val recurringList = repository.observeRecurring().first()
        val now = System.currentTimeMillis()
        val within24Hours = now + (24 * 60 * 60 * 1000L)
        
        recurringList.forEach { item ->
            // If next run is between now and next 24 hours
            if (item.recurring.nextRunAt in now..within24Hours) {
                NotificationHelper.showRecurringReminder(
                    context = applicationContext,
                    categoryName = item.categoryName ?: "Scheduled",
                    amount = formatCurrency(item.recurring.amount)
                )
            }
        }
        
        return Result.success()
    }
}
