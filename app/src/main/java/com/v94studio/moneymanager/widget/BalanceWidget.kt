package com.v94studio.moneymanager.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.v94studio.moneymanager.AppContainer
import com.v94studio.moneymanager.ui.util.formatCurrency
import kotlinx.coroutines.flow.first

class BalanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContainer = AppContainer(context.applicationContext)
        val repository = appContainer.repository
        
        // Fetch data once for the widget
        val totalIncome = repository.observeIncomeSince(0L).first() ?: 0.0
        val totalExpense = repository.observeExpenseSince(0L).first() ?: 0.0
        val balance = totalIncome - totalExpense
        
        // Try to get currency from settings
        val settings = com.v94studio.moneymanager.ui.settings.UserSettingsRepository(context).settingsFlow.first()
        val currencyCode = settings.currencyCode

        provideContent {
            BalanceWidgetContent(balance, currencyCode)
        }
    }
}

@androidx.compose.runtime.Composable
private fun BalanceWidgetContent(balance: Double, currencyCode: String) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF241B34))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Total Balance",
            style = TextStyle(
                color = ColorProvider(Color(0xFF94A3B8)),
                fontSize = 12.sp
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = formatCurrency(balance, currencyCode),
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
}
