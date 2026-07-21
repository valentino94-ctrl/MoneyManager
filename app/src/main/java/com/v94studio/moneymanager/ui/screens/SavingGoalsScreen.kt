package com.v94studio.moneymanager.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v94studio.moneymanager.LocalRepository
import com.v94studio.moneymanager.LocalUserSettings
import com.v94studio.moneymanager.R
import com.v94studio.moneymanager.data.SavingGoalEntity
import com.v94studio.moneymanager.ui.components.*
import com.v94studio.moneymanager.ui.components.featurediscovery.*
import com.v94studio.moneymanager.ui.navigation.MMActivePurple
import com.v94studio.moneymanager.ui.theme.*
import com.v94studio.moneymanager.ui.util.formatCurrency
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SavingGoalsScreen(
    listState: LazyListState = rememberLazyListState()
) {
    val repository = LocalRepository.current
    val settings = LocalUserSettings.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val goals by repository.observeSavingGoals().collectAsState(initial = emptyList())
    val featureDiscoveryViewModel = com.v94studio.moneymanager.LocalFeatureDiscovery.current
    val featureDiscoveryState by featureDiscoveryViewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var openGoalId by remember { mutableStateOf<Long?>(null) }
    var goalToEdit by remember { mutableStateOf<SavingGoalEntity?>(null) }
    var goalToAddFundsTo by remember { mutableStateOf<SavingGoalEntity?>(null) }

    val isSavingsTourVisible =
        featureDiscoveryState.isVisible &&
            featureDiscoveryState.currentTutorial == TutorialType.SAVINGS_INTRO
    val savingsTourSpaceHeight by animateDpAsState(
        targetValue = if (isSavingsTourVisible) 150.dp else 0.dp,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "savingsTourSpaceHeight"
    )
    
    // val listState = rememberLazyListState() // Removed to use hoisted state
    val bottomPadding = com.v94studio.moneymanager.LocalBottomPadding.current

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { it }
            .collect { openGoalId = null }
    }

    LaunchedEffect(
        goals,
        featureDiscoveryState.isLoaded,
        featureDiscoveryState.hasSeenSavingsTutorial,
        featureDiscoveryState.currentTutorial,
        showAddDialog,
        goalToEdit
    ) {
        if (
            goals.isNotEmpty() &&
            !showAddDialog &&
            goalToEdit == null &&
            featureDiscoveryState.isLoaded &&
            !featureDiscoveryState.hasSeenSavingsTutorial &&
            featureDiscoveryState.currentTutorial == null
        ) {
            featureDiscoveryViewModel.showSavingsTutorial()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                openGoalId = null
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomPadding + 16.dp, top = 44.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Saving Goals",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Plan your future, one step at a time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Surface(
                        onClick = { showAddDialog = true },
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier.shadow(8.dp, CircleShape, ambientColor = BrandPurple.copy(alpha = 0.5f), spotColor = BrandPurple)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Brush.horizontalGradient(listOf(BrandPurple, Color(0xFF9333EA))))
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(13.dp), tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("New Goal", fontWeight = FontWeight.ExtraBold, fontSize = 10.5.sp, color = Color.White, letterSpacing = 0.1.sp)
                            }
                        }
                    }
                }
            }

            if (goals.isEmpty()) {
                item(key = "empty_state") {
                    FintechCard {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                color = BrandPurple.copy(alpha = 0.08f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Flag, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(36.dp),
                                        tint = BrandPurple
                                    )
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            Text(stringResource(R.string.msg_no_goals), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.msg_goals_hint), 
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (goals.isNotEmpty()) {
                item(key = "savings_tour_space") {
                    Spacer(modifier = Modifier.height(savingsTourSpaceHeight))
                }
            }

            itemsIndexed(goals, key = { _, goal -> goal.id }) { index, goal ->
                PremiumSwipeableSavingGoalCard(
                    goal = goal,
                    currencyCode = settings.currencyCode,
                    isOpen = openGoalId == goal.id,
                    onOpen = { openGoalId = goal.id },
                    onClose = { if (openGoalId == goal.id) openGoalId = null },
                    onAddFunds = { goalToAddFundsTo = goal },
                    onEdit = { goalToEdit = goal },
                    onDelete = {
                        scope.launch {
                            repository.deleteSavingGoal(goal)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onTargetPositioned = if (index == 0) {
                        { rect ->
                            if (featureDiscoveryState.currentTutorial == TutorialType.SAVINGS_INTRO) {
                                featureDiscoveryViewModel.onTargetPositioned(TutorialType.SAVINGS_INTRO, rect)
                            }
                        }
                    } else null
                )
            }
        }

        FadingLazyColumnScrollbar(
            listState = listState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .padding(end = 6.dp, top = 6.dp, bottom = bottomPadding + 6.dp)
        )

        if (
            featureDiscoveryState.isVisible &&
            featureDiscoveryState.currentTutorial == TutorialType.SAVINGS_INTRO
        ) {
            FeatureDiscoveryOverlay(
                isVisible = true,
                targetRect = featureDiscoveryState.targetRect,
                title = "Your First Saving Goal",
                description = "This card tracks how much you've saved and how close you are to your target. Swipe it left to add funds, edit, or delete the goal.",
                onDismiss = featureDiscoveryViewModel::dismissTutorial,
                shape = DiscoveryShape.Rectangle,
                placement = DiscoveryPlacement.Above
            )
        }
    }

    if (showAddDialog || goalToEdit != null) {
        AddGoalDialog(
            existingGoal = goalToEdit,
            onDismiss = { 
                showAddDialog = false
                goalToEdit = null
                openGoalId = null
            },
            onConfirm = { name, target, deadline ->
                val goalBeingEdited = goalToEdit
                showAddDialog = false
                goalToEdit = null
                openGoalId = null
                scope.launch {
                    if (goalBeingEdited != null) {
                        repository.updateSavingGoal(goalBeingEdited.copy(
                            name = name,
                            targetAmountMinor = com.v94studio.moneymanager.data.amountToMinor(target),
                            deadline = deadline
                        ))
                    } else {
                        repository.addSavingGoal(name, target, deadline)
                    }
                }
            }
        )
    }

    if (goalToAddFundsTo != null) {
        AddFundsDialog(
            goalName = goalToAddFundsTo!!.name,
            onDismiss = { goalToAddFundsTo = null },
            onConfirm = { amount ->
                scope.launch {
                    repository.addFundsToGoal(goalToAddFundsTo!!, amount)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    goalToAddFundsTo = null
                }
            }
        )
    }
}

@Composable
private fun PremiumSwipeableSavingGoalCard(
    goal: SavingGoalEntity,
    currencyCode: String?,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onAddFunds: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTargetPositioned: ((Rect) -> Unit)? = null
) {
    val density = LocalDensity.current
    val actionWidth = 72.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    
    val totalActionsWidthPx = actionWidthPx * 3
    val offsetX = remember { Animatable(if (isOpen) -totalActionsWidthPx else 0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isOpen) {
        if (!isOpen && offsetX.value != 0f) offsetX.animateTo(0f)
        else if (isOpen && offsetX.value == 0f) offsetX.animateTo(-totalActionsWidthPx)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .height(IntrinsicSize.Min)
    ) {
        // Swipe Actions (Behind)
        Row(
            modifier = Modifier
                .padding(vertical = 6.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp)),
            horizontalArrangement = Arrangement.End
        ) {
            // Add Funds
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(actionWidth)
                    .background(FintechGreen)
                    .clickable { onAddFunds() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Payments, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text("Add", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            // Edit
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(actionWidth)
                    .background(BrandPurple)
                    .clickable { onEdit() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text("Edit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            // Delete
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(actionWidth)
                    .background(FintechRed)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val target = if (offsetX.value < -totalActionsWidthPx * 0.4f) {
                                    onOpen()
                                    -totalActionsWidthPx
                                } else {
                                    onClose()
                                    0f
                                }
                                offsetX.animateTo(target)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (offsetX.value + dragAmount).coerceIn(-totalActionsWidthPx, 0f)
                            if (newOffset < -totalActionsWidthPx * 0.8f && offsetX.value >= -totalActionsWidthPx * 0.8f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            scope.launch { offsetX.snapTo(newOffset) }
                        }
                    )
                }
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (isOpen) {
                        scope.launch { offsetX.animateTo(0f); onClose() }
                    }
                }
        ) {
            SavingGoalCard(
                goal = goal,
                currencyCode = currencyCode,
                onTargetPositioned = onTargetPositioned
            )
        }
    }
}

@Composable
private fun SavingGoalCard(
    goal: SavingGoalEntity,
    currencyCode: String?,
    onTargetPositioned: ((Rect) -> Unit)? = null
) {
    val progress = if (goal.targetAmount <= 0.0) 0f else (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .then(
                if (onTargetPositioned != null) {
                    Modifier.discoverable(onTargetPositioned)
                } else Modifier
            )
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = BrandPurple.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Flag, null, modifier = Modifier.size(18.dp), tint = BrandPurple)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = goal.name, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Target: ${formatCurrency(goal.targetAmount, currencyCode)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(goal.savedAmount, currencyCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = FintechGreen
                    )
                    Text(
                        text = "Saved",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(5.dp).background(FintechGreen, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${(progress * 100).toInt()}% reached",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                val remaining = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)
                Text(
                    text = "${formatCurrency(remaining, currencyCode)} left",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(FintechGreen, Color(0xFF4ADE80))))
                )
            }
        }
    }
}

@Composable
private fun AddGoalDialog(
    existingGoal: SavingGoalEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Long?) -> Unit
) {
    val settings = LocalUserSettings.current
    var name by remember { mutableStateOf(existingGoal?.name ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var target by remember { mutableStateOf(existingGoal?.targetAmount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var targetError by remember { mutableStateOf<String?>(null) }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            tonalElevation = 2.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (existingGoal == null) "New Saving Goal" else "Edit Saving Goal",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Name your goal and set the amount you want to reach.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Goal details",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it 
                        nameError = null
                    },
                    label = { Text("Goal Name") },
                    placeholder = { Text("e.g. New Car") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    isError = nameError != null,
                    singleLine = true
                )
                if (nameError != null) {
                    Text(text = nameError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                        Text(
                            text = "Target Amount",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val symbol = try {
                                Currency.getInstance(settings.currencyCode).getSymbol(Locale.getDefault())
                            } catch (_: Exception) {
                                "$"
                            }
                            Text(
                                text = symbol,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Light,
                                color = if (targetError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = target,
                                onValueChange = { input ->
                                    val cleaned = input.replace(',', '.')
                                    if (cleaned.count { it == '.' } <= 1) {
                                        val filtered = cleaned.filter { it.isDigit() || it == '.' }
                                        if (filtered.length <= 12) {
                                            target = filtered
                                            targetError = null
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (targetError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                visualTransformation = ThousandsSeparatorVisualTransformation(),
                                cursorBrush = Brush.verticalGradient(BrandPurpleGradient.take(2)),
                                decorationBox = { innerTextField ->
                                    if (target.isEmpty()) {
                                        Text(
                                            "0.00",
                                            style = TextStyle(
                                                fontSize = 38.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (targetError != null) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 16.dp),
                            color = if (targetError != null) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                            thickness = 2.dp
                        )
                    }
                }
                if (targetError != null) {
                    Text(text = targetError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.width(104.dp).height(36.dp),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val amount = target.replace(",", ".").toDoubleOrNull()
                            var hasError = false

                            if (name.isBlank()) {
                                nameError = "Enter a goal name"
                                hasError = true
                            }
                            if (amount == null || amount <= 0.0) {
                                targetError = "Enter a valid target"
                                hasError = true
                            }

                            if (!hasError) {
                                onConfirm(name.trim(), amount!!, null)
                            }
                        },
                        modifier = Modifier
                            .width(104.dp)
                            .height(36.dp)
                            .shadow(6.dp, RoundedCornerShape(50), ambientColor = BrandPurple, spotColor = BrandPurple),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(
                            text = if (existingGoal == null) "Create Goal" else "Save Changes",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFundsDialog(
    goalName: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    
    com.v94studio.moneymanager.ui.components.PremiumAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Funds to $goalName", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> 
                        val cleaned = input.replace(',', '.')
                        if (cleaned.count { it == '.' } <= 1) {
                            val filtered = cleaned.filter { it.isDigit() || it == '.' }
                            if (filtered.length <= 12) {
                                amountText = filtered
                                amountError = null
                            }
                        }
                    },
                    label = { Text("Contribution Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = amountError != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    visualTransformation = ThousandsSeparatorVisualTransformation(),
                    singleLine = true
                )
                if (amountError != null) {
                    Text(text = amountError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = amountText.replace(",", ".").toDoubleOrNull()
                    if (value == null || value <= 0) {
                        amountError = "Enter a valid amount"
                    } else {
                        onConfirm(value)
                    }
                },
                modifier = Modifier.width(104.dp).height(36.dp),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) { Text("Add") }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.width(104.dp).height(36.dp),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) { Text("Cancel") }
        }
    )
}
