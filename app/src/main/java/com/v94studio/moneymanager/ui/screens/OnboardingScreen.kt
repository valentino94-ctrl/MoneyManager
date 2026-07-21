package com.v94studio.moneymanager.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import com.v94studio.moneymanager.LocalUserSettings
import com.v94studio.moneymanager.R
import com.v94studio.moneymanager.data.CurrencyOption
import com.v94studio.moneymanager.ui.components.AppBottomSheet
import com.v94studio.moneymanager.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Currency
import java.util.Locale

private sealed class OnboardingStep {
    data object Setup : OnboardingStep()
    data class Content(val page: OnboardingPage) : OnboardingStep()
}

private data class OnboardingPage(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val color: Color,
    val titleGradient: List<Color>
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onLanguageChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val userSettings = LocalUserSettings.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val tabletPageWidth = minOf(configuration.screenWidthDp - 64, 720)
        .coerceAtLeast(1)
        .dp
    val tabletPagerPadding = ((configuration.screenWidthDp.dp - tabletPageWidth) / 2)
        .coerceAtLeast(0.dp)
    
    val steps = remember {
        listOf(
            OnboardingStep.Setup,
            OnboardingStep.Content(OnboardingPage(
                titleRes = R.string.onboarding_track_title,
                descriptionRes = R.string.onboarding_track_desc,
                icon = Icons.Default.AccountBalanceWallet,
                color = Color(0xFF3B82F6),
                titleGradient = listOf(Color(0xFF60A5FA), PurplePrimary, Color(0xFFD946EF))
            )),
            OnboardingStep.Content(OnboardingPage(
                titleRes = R.string.onboarding_budget_title,
                descriptionRes = R.string.onboarding_budget_desc,
                icon = Icons.Default.PieChart,
                color = Color(0xFFA78BFA),
                titleGradient = listOf(Color(0xFFDDD6FE), Color(0xFFA78BFA), Color(0xFF7C3AED))
            )),
            OnboardingStep.Content(OnboardingPage(
                titleRes = R.string.onboarding_insights_title,
                descriptionRes = R.string.onboarding_insights_desc,
                icon = Icons.Default.AutoGraph,
                color = Color(0xFF10B981),
                titleGradient = listOf(Color(0xFF58C9A5), Color(0xFF6B4BB5), Color(0xFFD946EF))
            ))
        )
    }

    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()

    var showLanguagePicker by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1D0B45))) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = isTablet || steps[pagerState.currentPage] !is OnboardingStep.Setup,
                beyondViewportPageCount = 1,
                pageSize = if (isTablet) PageSize.Fixed(tabletPageWidth) else PageSize.Fill,
                contentPadding = if (isTablet) {
                    PaddingValues(horizontal = tabletPagerPadding)
                } else {
                    PaddingValues(0.dp)
                },
                pageSpacing = if (isTablet) 24.dp else 0.dp
            ) { index ->
                when (val step = steps[index]) {
                    is OnboardingStep.Setup -> {
                        SetupPage(
                            isTablet = isTablet,
                            onSelectLanguage = { showLanguagePicker = true },
                            onSelectCurrency = { showCurrencyPicker = true }
                        )
                    }
                    is OnboardingStep.Content -> {
                        val page = step.page
                        val pageOffset = (pagerState.currentPage - index) + pagerState.currentPageOffsetFraction
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 40.dp)
                                .graphicsLayer {
                                    // Parallax and fade effect
                                    alpha = 1f - abs(pageOffset)
                                    translationX = pageOffset * size.width * 0.2f
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Immersive Icon Container
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    modifier = Modifier
                                        .size(200.dp)
                                        .graphicsLayer {
                                            scaleX = 1.1f - (abs(pageOffset) * 0.2f)
                                            scaleY = 1.1f - (abs(pageOffset) * 0.2f)
                                        },
                                    shape = CircleShape,
                                    color = page.color.copy(alpha = 0.08f)
                                ) {}
                                
                                Surface(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .shadow(24.dp, CircleShape, spotColor = page.color.copy(alpha = 0.4f)),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                ) {
                                    Icon(
                                        imageVector = page.icon,
                                        contentDescription = null,
                                        tint = page.color,
                                        modifier = Modifier.padding(32.dp).size(84.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(51.dp))
                            
                            Text(
                                text = stringResource(page.titleRes),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    brush = Brush.horizontalGradient(page.titleGradient),
                                    letterSpacing = (-1).sp
                                ),
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = stringResource(page.descriptionRes),
                                modifier = if (isTablet) Modifier.widthIn(max = 560.dp) else Modifier,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 22.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Liquid blob page indicator
                Canvas(
                    modifier = Modifier
                        .width((14 + (steps.size - 1) * 24).dp)
                        .height(18.dp)
                ) {
                    val markerRadius = 4.dp.toPx()
                    val blobHeight = 12.dp.toPx()
                    val stepDistance = 24.dp.toPx()
                    val startCenter = 7.dp.toPx()

                    repeat(steps.size) { index ->
                        drawCircle(
                            color = Color.White.copy(alpha = 0.2f),
                            radius = markerRadius,
                            center = androidx.compose.ui.geometry.Offset(
                                x = startCenter + index * stepDistance,
                                y = size.height / 2f
                            )
                        )
                    }

                    val swipeOffset = pagerState.currentPageOffsetFraction
                    val pagePosition = pagerState.currentPage + swipeOffset
                    val blobWidth = 14.dp.toPx() + abs(swipeOffset) * 22.dp.toPx()
                    val blobCenterX = startCenter + pagePosition * stepDistance
                    val blobTopLeft = androidx.compose.ui.geometry.Offset(
                        x = blobCenterX - blobWidth / 2f,
                        y = (size.height - blobHeight) / 2f
                    )
                    val blobSize = androidx.compose.ui.geometry.Size(blobWidth, blobHeight)

                    drawRoundRect(
                        color = BrandPurple.copy(alpha = 0.24f),
                        topLeft = androidx.compose.ui.geometry.Offset(
                            blobTopLeft.x - 2.dp.toPx(),
                            blobTopLeft.y - 2.dp.toPx()
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            blobSize.width + 4.dp.toPx(),
                            blobSize.height + 4.dp.toPx()
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
                    )
                    drawRoundRect(
                        brush = Brush.horizontalGradient(BrandPurpleGradient),
                        topLeft = blobTopLeft,
                        size = blobSize,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(blobHeight / 2f)
                    )
                }

                // Action Button
                val isLastPage = pagerState.currentPage == steps.size - 1
                Button(
                    onClick = {
                        if (isLastPage) {
                            onDone()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .widthIn(min = 200.dp, max = 240.dp)
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = BrandPurple, spotColor = BrandPurple),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(BrandPurpleGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isLastPage) stringResource(R.string.btn_get_started) else "Next",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Skip Button
        if (pagerState.currentPage > 0 && pagerState.currentPage < steps.size - 1) {
            TextButton(
                onClick = onDone,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 20.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_skip), 
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showLanguagePicker) {
        LanguageSelectionSheet(
            selectedTag = userSettings.languageTag,
            onSelected = { 
                onLanguageChange(it)
                showLanguagePicker = false
            },
            onDismiss = { showLanguagePicker = false }
        )
    }

    if (showCurrencyPicker) {
        val allCurrencies = remember(userSettings.currencyCode) {
            val locale = Locale.getDefault()
            val baseList = Currency.getAvailableCurrencies()
                .map { CurrencyOption(it.currencyCode, it.getDisplayName(locale)) }
                .distinctBy { it.code }
                .sortedBy { it.name }
            
            // Move the detected/selected currency to the top
            val selected = baseList.find { it.code == userSettings.currencyCode }
            if (selected != null) {
                listOf(selected) + baseList.filter { it.code != userSettings.currencyCode }
            } else {
                baseList
            }
        }
        CurrencySelectionSheet(
            currencies = allCurrencies,
            selectedCode = userSettings.currencyCode,
            onSelected = { 
                onCurrencyChange(it)
                showCurrencyPicker = false
            },
            onDismiss = { showCurrencyPicker = false }
        )
    }
}

@Composable
private fun SetupPage(
    isTablet: Boolean,
    onSelectLanguage: () -> Unit,
    onSelectCurrency: () -> Unit
) {
    val userSettings = LocalUserSettings.current
    val currentLocale = remember(userSettings.languageTag) { Locale.forLanguageTag(userSettings.languageTag) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isTablet) 560.dp else 720.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        // Main Illustration - Pure focus on the ring and currencies
        Box(
            modifier = Modifier
                .size(if (isTablet) 220.dp else 260.dp)
                .padding(bottom = 8.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }

        Text(
            text = "Welcome to",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium.copy(
                brush = Brush.horizontalGradient(BrandPurpleGradient),
                letterSpacing = (-1.5).sp
            ),
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Set your language and currency to\npersonalize your experience.",
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 26.sp,
                letterSpacing = 0.2.sp
            ),
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(if (isTablet) 32.dp else 48.dp))
        
        SetupCard(
            title = stringResource(R.string.language),
            value = currentLocale.getDisplayName(currentLocale).replaceFirstChar { it.uppercase() },
            icon = Icons.Default.Language,
            onClick = onSelectLanguage
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val currencyName = remember(userSettings.currencyCode) {
            try {
                val cur = Currency.getInstance(userSettings.currencyCode)
                "${cur.getDisplayName(currentLocale)} (${cur.currencyCode})"
            } catch (e: Exception) { userSettings.currencyCode }
        }
        
        SetupCard(
            title = stringResource(R.string.currency),
            value = currencyName,
            icon = Icons.Default.Public,
            onClick = onSelectCurrency
        )
        
        // Add extra space to push content up if needed, but centering is fine
        Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SetupCard(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.06f), // Slightly more visible glass
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with Glow
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(BrandPurple.copy(alpha = 0.15f), CircleShape)
                    .border(BorderStroke(1.5.dp, BrandPurple.copy(alpha = 0.3f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandPurpleLighter, // Using a lighter purple for the icon itself
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.labelMedium, 
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null, 
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencySelectionSheet(
    currencies: List<CurrencyOption>,
    selectedCode: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCurrencies = remember(searchQuery) {
        if (searchQuery.isBlank()) currencies
        else currencies.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.code.contains(searchQuery, ignoreCase = true) 
        }
    }

    AppBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxHeight(0.55f)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.select_currency),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                placeholder = { Text(stringResource(R.string.search_currency_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Close, contentDescription = null) } }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            val sheetListState = rememberLazyListState()
            val selectedIndex = remember(filteredCurrencies, selectedCode) {
                filteredCurrencies.indexOfFirst { it.code == selectedCode }
            }
            LaunchedEffect(selectedIndex) {
                if (selectedIndex >= 0) {
                    sheetListState.scrollToItem(selectedIndex)
                }
            }
            LazyColumn(
                state = sheetListState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredCurrencies) { option ->
                    val isSelected = option.code == selectedCode
                    Surface(
                        onClick = { onSelected(option.code) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Brand Accent Line
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(4.dp)
                                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                    .background(if (isSelected) BrandPurple else Color.Transparent)
                            )
                            
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        option.name, 
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BrandPurple else MaterialTheme.colorScheme.onSurface
                                    ) 
                                },
                                supportingContent = { Text(option.code) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.weight(1f).padding(start = 12.dp)
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(19.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelectionSheet(
    selectedTag: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = remember {
        listOf(
            "en" to "English",
            "fr" to "Français",
            "es" to "Español",
            "de" to "Deutsch",
            "it" to "Italiano",
            "pt" to "Português",
            "ru" to "Русский",
            "zh" to "中文",
            "ja" to "日本語",
            "ko" to "한국어"
        ).sortedBy { it.second }
    }

    AppBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxHeight(0.55f)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            val sheetListState = rememberLazyListState()
            val selectedIndex = remember(languages, selectedTag) {
                languages.indexOfFirst { selectedTag.startsWith(it.first) }
            }
            LaunchedEffect(selectedIndex) {
                if (selectedIndex >= 0) {
                    sheetListState.scrollToItem(selectedIndex)
                }
            }
            LazyColumn(
                state = sheetListState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(languages) { (tag, name) ->
                    val isSelected = selectedTag.startsWith(tag)
                    Surface(
                        onClick = { onSelected(tag) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Brand Accent Line
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(4.dp)
                                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                    .background(if (isSelected) BrandPurple else Color.Transparent)
                            )
                            
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        name, 
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BrandPurple else MaterialTheme.colorScheme.onSurface
                                    ) 
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.weight(1f).padding(start = 12.dp)
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(19.dp))
        }
    }
}
