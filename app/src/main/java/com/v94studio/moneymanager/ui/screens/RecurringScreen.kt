package com.v94studio.moneymanager.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import com.v94studio.moneymanager.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v94studio.moneymanager.LocalRepository
import com.v94studio.moneymanager.LocalUserSettings
import com.v94studio.moneymanager.R
import com.v94studio.moneymanager.data.CategoryEntity
import com.v94studio.moneymanager.data.CategoryType
import com.v94studio.moneymanager.data.TransactionType
import com.v94studio.moneymanager.ui.components.*
import com.v94studio.moneymanager.ui.theme.*
import com.v94studio.moneymanager.ui.util.formatDateTime
import com.v94studio.moneymanager.ui.util.getCategoryEmoji
import com.v94studio.moneymanager.ui.util.getLocalizedCategory
import com.v94studio.moneymanager.ui.util.mergeDateAndTime
import com.v94studio.moneymanager.ui.util.updateTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(onBack: () -> Unit = {}) {
    val repository = LocalRepository.current
    val scope = rememberCoroutineScope()
    val settings = LocalUserSettings.current
    val categories by repository.observeCategories().collectAsState(initial = emptyList())
    val recurring by repository.observeRecurring().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var amountText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var noteText by remember { mutableStateOf("") }
    var intervalText by remember { mutableStateOf("30") }
    var intervalError by remember { mutableStateOf<String?>(null) }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var nextRunAt by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val filteredCategories = categories.filter { category ->
        if (type == TransactionType.EXPENSE) category.type == CategoryType.EXPENSE
        else category.type == CategoryType.INCOME
    }

    val listState = rememberLazyListState()
    val bottomPadding = com.v94studio.moneymanager.LocalBottomPadding.current

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MoneyTopAppBar(
                title = stringResource(R.string.title_recurring_schedules),
                subtitle = stringResource(R.string.subtitle_recurring),
                navigationIcon = {
                    MoneyBackButton(label = "Back", onClick = onBack)
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        val topPadding = padding.calculateTopPadding()
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, 
                    end = 16.dp, 
                    bottom = bottomPadding + 16.dp, 
                    top = (topPadding - 60.dp).coerceAtLeast(0.dp) + 28.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FintechCard {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(R.string.title_add_schedule),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val typeOptions = listOf(
                        TransactionType.EXPENSE to stringResource(R.string.label_expenses),
                        TransactionType.INCOME to stringResource(R.string.label_income)
                    )
                    val selectedIndex = typeOptions.indexOfFirst { it.first == type }.coerceAtLeast(0)
                    
                    TabRow(
                        selectedTabIndex = selectedIndex,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(TabRowContainer),
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            Box(
                                modifier = Modifier
                                    .tabIndicatorOffset(tabPositions[selectedIndex])
                                    .fillMaxHeight()
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.horizontalGradient(BrandPurpleGradient))
                            )
                        },
                        divider = {}
                    ) {
                        typeOptions.forEachIndexed { index, (value, label) ->
                            val isSelected = selectedIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { type = value },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 14.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                                    color = if (isSelected) Color.White else TabRowUnselected
                                )
                                if (index < typeOptions.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .height(24.dp)
                                            .width(1.dp)
                                            .background(TabRowDivider)
                                    )
                                }
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { 
                            amountText = it 
                            amountError = null
                        },
                        label = { Text(stringResource(R.string.label_total)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        isError = amountError != null
                    )
                    if (amountError != null) {
                        Text(
                            text = amountError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    OutlinedTextField(
                        value = intervalText,
                        onValueChange = { 
                            intervalText = it 
                            intervalError = null
                        },
                        label = { Text(stringResource(R.string.label_repeat_days)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        isError = intervalError != null
                    )
                    if (intervalError != null) {
                        Text(
                            text = intervalError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = stringResource(R.string.title_categories), style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredCategories.forEach { category ->
                                val isSelected = selectedCategory?.id == category.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { 
                                        selectedCategory = if (isSelected) null else category 
                                        categoryError = null
                                    },
                                    label = { 
                                        val localizedName = getLocalizedCategory(category.name, LocalContext.current)
                                        val emoji = getCategoryEmoji(category.name)
                                        Text("$emoji $localizedName") 
                                    },
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.Transparent,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.White
                                    ),
                                    border = null,
                                    elevation = FilterChipDefaults.filterChipElevation(elevation = if (isSelected) 0.dp else 2.dp),
                                    modifier = if (isSelected) Modifier.background(Brush.horizontalGradient(BrandPurpleGradient), CircleShape) else Modifier
                                )
                            }
                        }
                        if (categoryError != null) {
                            Text(
                                text = categoryError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text(stringResource(R.string.label_note_optional)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            DatePickerField(
                                label = stringResource(R.string.label_next_run),
                                selectedMillis = nextRunAt,
                                onSelected = { nextRunAt = mergeDateAndTime(it, nextRunAt) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            TimePickerField(
                                label = stringResource(R.string.label_time),
                                selectedMillis = nextRunAt,
                                onSelected = { hour, minute ->
                                    nextRunAt = updateTime(nextRunAt, hour, minute)
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            val normalizedAmount = amountText.replace(",", ".")
                            val amount = normalizedAmount.toDoubleOrNull()
                            val interval = intervalText.toIntOrNull()
                            
                            if (selectedCategory == null) {
                                categoryError = context.getString(R.string.error_select_category)
                                return@Button
                            }
                            
                            if (amount == null || amount <= 0.0) {
                                amountError = context.getString(R.string.error_invalid_amount)
                                return@Button
                            }
                            
                            if (interval == null || interval <= 0) {
                                intervalError = context.getString(R.string.error_enter_valid_days)
                                return@Button
                            }

                            scope.launch {
                                repository.addRecurring(
                                    amount = amount,
                                    type = type,
                                    categoryId = selectedCategory?.id,
                                    accountId = null,
                                    note = noteText,
                                    intervalDays = interval,
                                    nextRunAt = nextRunAt
                                )
                                amountText = ""
                                noteText = ""
                                intervalText = "30"
                                selectedCategory = null
                                nextRunAt = System.currentTimeMillis()
                            }
                        },
                        modifier = Modifier
                            .widthIn(max = 400.dp)
                            .fillMaxWidth()
                            .height(52.dp)
                            .align(Alignment.CenterHorizontally)
                            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = BrandPurple, spotColor = BrandPurple),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(BrandPurpleGradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.btn_add_recurring), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text(text = stringResource(R.string.title_upcoming_payments), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
            items(recurring, key = { it.recurring.id }) { item ->
                FintechCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Text(
                                    text = getLocalizedCategory(item.categoryName ?: "Uncategorized", LocalContext.current),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.label_every_x_days, item.recurring.intervalDays),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FintechSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            AnimatedAmountText(
                                amount = item.recurring.amount,
                                currencyCode = settings.currencyCode,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (item.recurring.type == TransactionType.INCOME) FintechGreen else FintechRed,
                                modifier = Modifier.wrapContentWidth()
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.label_next_date, formatDateTime(item.recurring.nextRunAt, is24Hour)),
                            style = MaterialTheme.typography.bodySmall,
                            color = FintechSecondary
                        )
                        
                        if (item.recurring.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.recurring.note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { scope.launch { repository.payRecurringNow(item.recurring) } },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.btn_pay_now))
                            }
                            
                            FintechActionButton(
                                icon = Icons.Default.SkipNext,
                                tint = MaterialTheme.colorScheme.tertiary,
                                contentDescription = stringResource(R.string.cd_skip),
                                onClick = { scope.launch { repository.skipRecurring(item.recurring) } }
                            )
                            
                            FintechActionButton(
                                icon = Icons.Default.Delete,
                                tint = FintechRed,
                                contentDescription = stringResource(R.string.cd_delete),
                                onClick = { scope.launch { repository.deleteRecurring(item.recurring) } }
                            )
                        }
                    }
                }
            }
        }
        FadingLazyColumnScrollbar(
            listState = listState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .padding(end = 6.dp, top = 6.dp, bottom = bottomPadding + 6.dp)
        )
    }
}
}
