package com.v94studio.moneymanager.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import com.v94studio.moneymanager.ui.components.AppBottomSheet
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v94studio.moneymanager.data.AccountEntity
import com.v94studio.moneymanager.data.AccountWithBalance
import com.v94studio.moneymanager.ui.theme.BrandPurple
import com.v94studio.moneymanager.ui.theme.BrandPurpleGradient
import com.v94studio.moneymanager.ui.theme.FintechSecondary
import com.v94studio.moneymanager.ui.util.formatCurrency

@Composable
fun FintechSectionHeader(
    title: String, 
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 12.dp, horizontal = 4.dp)) {
        Text(
            text = title, 
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle, 
                style = MaterialTheme.typography.bodyMedium,
                color = FintechSecondary
            )
        }
    }
}

@Composable
fun FintechCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = modifier
        .fillMaxWidth()
        .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            content = content
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            content = content
        )
    }
}

@Composable
fun PremiumAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit),
    confirmButton: @Composable (() -> Unit),
    dismissButton: @Composable (() -> Unit),
    modifier: Modifier = Modifier
) {
    val dialogShape = RoundedCornerShape(28.dp)
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = dialogShape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = dialogShape
            ),
        shape = dialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    )
}

@Composable
fun FintechActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    tint: Color, 
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = tint.copy(alpha = 0.12f),
            contentColor = tint
        )
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun MoneyBackButton(
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = Modifier.offset(x = (-8).dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyTopAppBar(
    title: String,
    subtitle: String? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {}
) {
    val bgColor = MaterialTheme.colorScheme.background

    Column(modifier = Modifier.fillMaxWidth()) {
        // Solid Part
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.widthIn(min = 44.dp), contentAlignment = Alignment.Center) {
                    navigationIcon()
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }

            if (title.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp, top = 2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            bottomContent()
        }

        // Dissolve Part - The diagram effect (Solid -> Transparent)
        // Tall gradient with stops to mimic the "thinner" look at the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    brush = Brush.verticalGradient(
                        0.0f to bgColor,
                        0.3f to bgColor.copy(alpha = 0.9f),
                        0.5f to bgColor.copy(alpha = 0.7f),
                        0.7f to bgColor.copy(alpha = 0.4f),
                        0.85f to bgColor.copy(alpha = 0.15f),
                        1.0f to Color.Transparent
                    )
                )
        )
    }
}

@Composable
fun BounceMarqueeText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null
) {
    val scrollState = rememberScrollState()
    
    LaunchedEffect(text) {
        scrollState.scrollTo(0)
        kotlinx.coroutines.delay(1000)
        
        val max = scrollState.maxValue
        if (max > 0) {
            scrollState.animateScrollTo(
                value = max,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = (max * 8).coerceAtLeast(700), 
                    easing = androidx.compose.animation.core.LinearEasing
                )
            )
            kotlinx.coroutines.delay(1000)
            scrollState.animateScrollTo(
                value = 0,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = (max * 8).coerceAtLeast(700),
                    easing = androidx.compose.animation.core.LinearEasing
                )
            )
        }
    }

    Text(
        text = text,
        style = style.copy(
            fontWeight = fontWeight ?: style.fontWeight, 
            textAlign = textAlign ?: style.textAlign
        ),
        color = color,
        maxLines = 1,
        modifier = modifier.horizontalScroll(scrollState, enabled = false)
    )
}

@Composable
fun AccountSwitcherPill(
    account: AccountEntity,
    balance: Double,
    currencyCode: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(36.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ),
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Premium circular icon container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFF3E8FF), Color(0xFFE6E0FF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = account.emoji,
                    fontSize = 22.sp
                )
            }

            Spacer(Modifier.width(16.dp))

            // Center: Vertical column with premium typography
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = account.name,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)) { 8 })
                            .togetherWith(fadeOut(animationSpec = tween(400)) + slideOutVertically(animationSpec = tween(400)) { -8 })
                    },
                    label = "AccountNameAnimation"
                ) { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                
                Spacer(Modifier.height(2.dp))

                AnimatedContent(
                    targetState = balance,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)) { 8 })
                            .togetherWith(fadeOut(animationSpec = tween(400)) + slideOutVertically(animationSpec = tween(400)) { -8 })
                    },
                    label = "AccountBalanceAnimation"
                ) { currentBalance ->
                    Text(
                        text = "Balance: ${formatCurrency(currentBalance, currencyCode)}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // Right: Subtle dropdown indicator
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select Account",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.width(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectionSheet(
    accounts: List<AccountWithBalance>,
    selectedAccountId: Long?,
    onAccountSelected: (AccountEntity) -> Unit,
    onAddAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxHeight(0.7f)
    ) {
        val groupedAccounts = remember(accounts) {
            accounts
                .groupBy { it.account.type.ifBlank { "Other" } }
                .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        }
        val listState = rememberLazyListState()
        val selectedIndex = remember(groupedAccounts, selectedAccountId) {
            var itemIndex = 0
            var found = -1
            groupedAccounts.forEach { (_, typeAccounts) ->
                itemIndex++
                val indexInType = typeAccounts.indexOfFirst { it.account.id == selectedAccountId }
                if (indexInType >= 0 && found < 0) found = itemIndex + indexInType
                itemIndex += typeAccounts.size
            }
            found
        }

        LaunchedEffect(selectedIndex) {
            if (selectedIndex >= 0) listState.scrollToItem(selectedIndex)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(start = 24.dp, top = 14.dp, end = 24.dp, bottom = 12.dp)) {
                Text(
                    text = "Choose Account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.4).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Select where this transaction belongs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No accounts yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedAccounts.forEach { (accountType, typeAccounts) ->
                        item(key = "type_$accountType") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 14.dp, end = 4.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = accountType.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(BrandPurple.copy(alpha = 0.1f), CircleShape)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = typeAccounts.size.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPurple
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                )
                            }
                        }
                        items(typeAccounts, key = { it.account.id }) { wrapper ->
                            val isSelected = wrapper.account.id == selectedAccountId
                            Surface(
                                onClick = { onAccountSelected(wrapper.account) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = if (isSelected) 12.dp else 5.dp,
                                        shape = RoundedCornerShape(22.dp),
                                        ambientColor = BrandPurple.copy(alpha = if (isSelected) 0.18f else 0.04f),
                                        spotColor = BrandPurple.copy(alpha = if (isSelected) 0.16f else 0.04f)
                                    ),
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) BrandPurple.copy(alpha = 0.38f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(4.dp)
                                            .background(
                                                if (isSelected) Brush.verticalGradient(BrandPurpleGradient)
                                                else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                                            )
                                    )
                                    Row(
                                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(46.dp).background(BrandPurple.copy(alpha = 0.09f), RoundedCornerShape(14.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = wrapper.account.emoji, fontSize = 22.sp)
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = wrapper.account.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                text = formatCurrency(wrapper.balance, null),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                            )
                                        }
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier.size(28.dp).background(Brush.linearGradient(BrandPurpleGradient), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(17.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
            Surface(
                onClick = onAddAccount,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .align(Alignment.CenterHorizontally)
                    .shadow(8.dp, CircleShape, ambientColor = BrandPurple.copy(alpha = 0.35f), spotColor = BrandPurple.copy(alpha = 0.28f)),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .background(Brush.horizontalGradient(BrandPurpleGradient))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(24.dp).background(Color.White.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Create New Account",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.1.sp
                    )
                }
            }
        }
    }
}

class ThousandsSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val parts = original.split('.')
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) "." + parts[1] else ""

        val formattedInteger = StringBuilder()
        val originalToTransformed = IntArray(original.length + 1)
        
        var currentTransformedIndex = 0
        for (i in integerPart.indices) {
            val digitsFromEnd = integerPart.length - i
            if (i > 0 && digitsFromEnd % 3 == 0) {
                formattedInteger.append(',')
                currentTransformedIndex++
            }
            originalToTransformed[i] = currentTransformedIndex
            formattedInteger.append(integerPart[i])
            currentTransformedIndex++
        }
        originalToTransformed[integerPart.length] = currentTransformedIndex
        
        val integerTransformedLength = formattedInteger.length
        
        formattedInteger.append(decimalPart)
        for (i in integerPart.length + 1..original.length) {
            originalToTransformed[i] = i + (integerTransformedLength - integerPart.length)
        }

        val output = formattedInteger.toString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return originalToTransformed[offset.coerceIn(0, original.length)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                var lastOriginal = 0
                for (i in originalToTransformed.indices) {
                    if (originalToTransformed[i] <= offset) {
                        lastOriginal = i
                    } else {
                        break
                    }
                }
                return lastOriginal
            }
        }

        return TransformedText(AnnotatedString(output), offsetMapping)
    }
}
