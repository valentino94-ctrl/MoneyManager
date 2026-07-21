package com.v94studio.moneymanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v94studio.moneymanager.LocalRepository
import com.v94studio.moneymanager.R
import com.v94studio.moneymanager.data.TransactionWithCategory
import com.v94studio.moneymanager.ui.util.formatCurrency
import com.v94studio.moneymanager.LocalUserSettings
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen() {
    val repository = LocalRepository.current
    val settings = LocalUserSettings.current
    
    val calendar = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf(calendar.time) }
    var currentDisplayMonth by remember { mutableStateOf(calendar.apply { set(Calendar.DAY_OF_MONTH, 1) }.time) }
    
    val transactions by repository.observeTransactions().collectAsState(initial = emptyList())
    
    val selectedDayTransactions = remember(selectedDate, transactions) {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val selectedDayStr = sdf.format(selectedDate)
        transactions.filter { 
            sdf.format(Date(it.transaction.timestamp)) == selectedDayStr
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Month Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                val cal = Calendar.getInstance().apply { 
                    time = currentDisplayMonth
                    add(Calendar.MONTH, -1)
                }
                currentDisplayMonth = cal.time
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
            }
            
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            Text(
                text = monthFormat.format(currentDisplayMonth),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { 
                val cal = Calendar.getInstance().apply { 
                    time = currentDisplayMonth
                    add(Calendar.MONTH, 1)
                }
                currentDisplayMonth = cal.time
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Calendar Grid
        CalendarGrid(
            currentMonthDate = currentDisplayMonth,
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it },
            transactions = transactions
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Agenda for selected day
        val dayFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        val isToday = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(selectedDate) == 
                      SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        
        Text(
            text = if (isToday) stringResource(R.string.label_today) else dayFormat.format(selectedDate),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (selectedDayTransactions.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.msg_no_transactions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(selectedDayTransactions) { tx ->
                    TransactionItem(tx, settings.currencyCode)
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonthDate: Date,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    transactions: List<TransactionWithCategory>
) {
    val cal = Calendar.getInstance().apply { time = currentMonthDate }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday in our grid logic?
    // Calendar.SUNDAY is 1, MONDAY is 2.
    // Let's make it 0 for Sunday, 1 for Monday...
    val offset = cal.get(Calendar.DAY_OF_WEEK) - 1

    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")

    Column {
        // Day Labels
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val selectedStr = sdf.format(selectedDate)

        // Rows of days
        for (row in 0..5) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val dayNumber = row * 7 + col - offset + 1
                    if (dayNumber in 1..daysInMonth) {
                        val dayDate = Calendar.getInstance().apply {
                            time = currentMonthDate
                            set(Calendar.DAY_OF_MONTH, dayNumber)
                        }.time
                        val dayStr = sdf.format(dayDate)
                        
                        CalendarDay(
                            dayNumber = dayNumber,
                            isSelected = dayStr == selectedStr,
                            isToday = dayStr == todayStr,
                            hasTransactions = transactions.any { 
                                sdf.format(Date(it.transaction.timestamp)) == dayStr 
                            },
                            onClick = { onDateSelected(dayDate) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if ((row * 7 + 7 - offset) >= daysInMonth) break
        }
    }
}

@Composable
fun CalendarDay(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasTransactions: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = contentColor
        )
        if (hasTransactions && !isSelected) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun TransactionItem(tx: TransactionWithCategory, currencyCode: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (tx.categoryName ?: "?").take(1).uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = tx.categoryName ?: stringResource(R.string.cat_uncategorized),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!tx.transaction.note.isNullOrBlank()) {
                        Text(
                            text = tx.transaction.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                text = formatCurrency(tx.transaction.amount, currencyCode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (tx.transaction.type == com.v94studio.moneymanager.data.TransactionType.EXPENSE) 
                    MaterialTheme.colorScheme.error 
                else 
                    Color(0xFF4CAF50)
            )
        }
    }
}
