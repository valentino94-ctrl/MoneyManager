package com.v94studio.moneymanager.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.v94studio.moneymanager.LocalSettingsRepository
import com.v94studio.moneymanager.LocalUserSettings
import com.v94studio.moneymanager.R
import com.v94studio.moneymanager.data.CurrencyOption
import com.v94studio.moneymanager.ui.components.AppBottomSheet
import com.v94studio.moneymanager.ui.components.FadingLazyColumnScrollbar
import com.v94studio.moneymanager.ui.settings.ThemeMode
import com.v94studio.moneymanager.ui.theme.BrandPurple
import com.v94studio.moneymanager.ui.util.BackupHelper
import kotlinx.coroutines.launch
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onThemeModeChange: (ThemeMode) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onManageAccounts: () -> Unit,
    onManageCategories: () -> Unit,
    onExportCsv: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings = LocalUserSettings.current
    val settingsRepository = LocalSettingsRepository.current
    val scope = rememberCoroutineScope()
    
    val allCurrencies = remember {
        val locale = Locale.getDefault()
        Currency.getAvailableCurrencies()
            .map { CurrencyOption(it.currencyCode, it.getDisplayName(locale)) }
            .distinctBy { it.code }
            .sortedBy { it.name }
    }
    
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    
    val currentTopPadding = com.v94studio.moneymanager.LocalTopPadding.current
    val currentBottomPadding = com.v94studio.moneymanager.LocalBottomPadding.current
    // Keep Settings stationary while the app-level header and bottom bar exit.
    // Scaffold updates both insets before navigation replaces this destination;
    // using the live values makes the outgoing Settings frame visibly jump.
    var topPadding by remember { mutableStateOf(currentTopPadding) }
    var bottomPadding by remember { mutableStateOf(currentBottomPadding) }

    LaunchedEffect(currentTopPadding, currentBottomPadding) {
        // Settings can compose one frame before the retained app bar reports its
        // size. Accept larger insets, but never shrink during route handoff.
        if (currentTopPadding > topPadding) topPadding = currentTopPadding
        if (currentBottomPadding > bottomPadding) bottomPadding = currentBottomPadding
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, 
                    top = (topPadding - 60.dp).coerceAtLeast(0.dp) + 40.dp, 
                    end = 16.dp, 
                    bottom = bottomPadding + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Appearance Section
                item {
                    SettingsSection(title = stringResource(R.string.section_appearance)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconCircle(icon = Icons.Default.Palette, color = MaterialTheme.colorScheme.primary, contentDescription = stringResource(R.string.theme_mode))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = stringResource(R.string.theme_mode),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                val themeOptions = listOf(
                                    ThemeMode.DARK to stringResource(R.string.theme_dark),
                                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                                    ThemeMode.SYSTEM to stringResource(R.string.theme_system)
                                )
                                val selectedIndex = themeOptions.indexOfFirst { it.first == settings.themeMode }
                                    .coerceAtLeast(0)
                                
                                TabRow(
                                    selectedTabIndex = selectedIndex,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    indicator = { tabPositions ->
                                        Box(
                                            modifier = Modifier
                                                .tabIndicatorOffset(tabPositions[selectedIndex])
                                                .fillMaxHeight()
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    },
                                    divider = {}
                                ) {
                                    themeOptions.forEachIndexed { index, (mode, label) ->
                                        val isSelected = selectedIndex == index
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .zIndex(1f)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) { onThemeModeChange(mode) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                modifier = Modifier.padding(vertical = 12.dp),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Security Section
                item {
                    SettingsSection(title = stringResource(R.string.section_security)) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            SettingsSwitchItem(
                                title = stringResource(R.string.biometric_lock),
                                subtitle = stringResource(R.string.biometric_subtitle),
                                icon = Icons.Default.Fingerprint,
                                iconColor = Color(0xFFE91E63),
                                checked = settings.biometricEnabled,
                                onCheckedChange = { scope.launch { settingsRepository.setBiometricEnabled(it) } }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            SettingsSwitchItem(
                                title = stringResource(R.string.privacy_mode),
                                subtitle = stringResource(R.string.privacy_subtitle),
                                icon = Icons.Default.VisibilityOff,
                                iconColor = Color(0xFF673AB7),
                                checked = settings.privacyModeEnabled,
                                onCheckedChange = { scope.launch { settingsRepository.setPrivacyModeEnabled(it) } }
                            )
                        }
                    }
                }

                // Automation Section
                item {
                    SettingsSection(title = stringResource(R.string.section_automation)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            SettingsSwitchItem(
                                title = stringResource(R.string.auto_approve),
                                subtitle = stringResource(R.string.auto_approve_subtitle),
                                icon = Icons.Default.AutoMode,
                                iconColor = Color(0xFF4CAF50),
                                checked = settings.autoApproveRecurring,
                                onCheckedChange = { scope.launch { settingsRepository.setAutoApproveRecurring(it) } }
                            )
                        }
                    }
                }

                // Regional Section
                item {
                    SettingsSection(title = stringResource(R.string.section_regional)) {
                        val selectedCurrency = allCurrencies.find { it.code == settings.currencyCode }
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            SettingsItem(
                                title = stringResource(R.string.currency),
                                subtitle = selectedCurrency?.let { "${it.name} (${it.code})" } ?: settings.currencyCode,
                                icon = Icons.Default.Language,
                                iconColor = MaterialTheme.colorScheme.secondary,
                                onClick = { showCurrencySheet = true },
                                showDivider = true
                            )
                            
                            val selectedLocale = Locale.forLanguageTag(settings.languageTag)
                            SettingsItem(
                                title = stringResource(R.string.language),
                                subtitle = selectedLocale.getDisplayName(selectedLocale).replaceFirstChar { it.uppercase() },
                                icon = Icons.Default.Translate,
                                iconColor = Color(0xFF009688),
                                onClick = { showLanguageSheet = true }
                            )
                        }
                    }
                }

                // Planning Section
                item {
                    SettingsSection(title = stringResource(R.string.section_planning_settings)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconCircle(icon = Icons.AutoMirrored.Filled.TrendingUp, color = Color(0xFFFF9800), contentDescription = stringResource(R.string.budget_window))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = stringResource(R.string.budget_window),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                val rollingOptions = listOf(
                                    7 to stringResource(R.string.window_week),
                                    30 to stringResource(R.string.window_month),
                                    90 to stringResource(R.string.window_quarter)
                                )
                                val selectedIndex = rollingOptions.indexOfFirst { it.first == settings.rollingDays }
                                    .coerceAtLeast(0)
                                
                                TabRow(
                                    selectedTabIndex = selectedIndex,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    indicator = { tabPositions ->
                                        Box(
                                            modifier = Modifier
                                                .tabIndicatorOffset(tabPositions[selectedIndex])
                                                .fillMaxHeight()
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    },
                                    divider = {}
                                ) {
                                    rollingOptions.forEachIndexed { index, (days, label) ->
                                        val isSelected = selectedIndex == index
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .zIndex(1f)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) { scope.launch { settingsRepository.setRollingDays(days) } },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                modifier = Modifier.padding(vertical = 12.dp),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Management Section
                item {
                    SettingsSection(title = stringResource(R.string.section_management)) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            SettingsItem(
                                title = stringResource(R.string.manage_accounts),
                                subtitle = stringResource(R.string.manage_accounts_subtitle),
                                icon = Icons.Default.AccountBalanceWallet,
                                iconColor = MaterialTheme.colorScheme.tertiary,
                                onClick = onManageAccounts,
                                showDivider = true
                            )
                            SettingsItem(
                                title = stringResource(R.string.manage_categories),
                                subtitle = stringResource(R.string.manage_categories_subtitle),
                                icon = Icons.Default.Category,
                                iconColor = Color(0xFF4CAF50),
                                onClick = onManageCategories
                            )
                        }
                    }
                }

                // Data Section
                item {
                    SettingsSection(title = stringResource(R.string.section_support)) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            SettingsItem(
                                title = stringResource(R.string.send_feedback),
                                subtitle = stringResource(R.string.send_feedback_subtitle),
                                icon = Icons.Default.Email,
                                iconColor = Color(0xFF673AB7),
                                onClick = { showFeedbackDialog = true },
                                showDivider = true
                            )
                            SettingsItem(
                                title = stringResource(R.string.export_csv),
                                subtitle = stringResource(R.string.export_csv_subtitle),
                                icon = Icons.Default.FileUpload,
                                iconColor = Color(0xFF2196F3),
                                onClick = onExportCsv,
                                showDivider = true
                            )
                            SettingsItem(
                                title = stringResource(R.string.export_database),
                                subtitle = stringResource(R.string.export_database_subtitle),
                                icon = Icons.Default.Backup,
                                iconColor = Color(0xFFFF9800),
                                onClick = onExportBackup,
                                showDivider = true
                            )
                            SettingsItem(
                                title = stringResource(R.string.restore_backup),
                                subtitle = stringResource(R.string.restore_backup_subtitle),
                                icon = Icons.Default.SettingsBackupRestore,
                                iconColor = Color(0xFF9C27B0),
                                onClick = onRestoreBackup,
                                showDivider = true
                            )
                            SettingsItem(
                                title = stringResource(R.string.privacy_policy),
                                subtitle = stringResource(R.string.privacy_policy_subtitle),
                                icon = Icons.Default.PrivacyTip,
                                iconColor = Color(0xFF4CAF50),
                                onClick = { showPrivacyPolicy = true }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Money Manager v1.0.0",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            
            FadingLazyColumnScrollbar(
                listState = listState,
                modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().padding(end = 6.dp, top = topPadding + 6.dp, bottom = bottomPadding + 6.dp)
            )
        }
    }

    if (showCurrencySheet) {
        CurrencySelectionSheet(
            currencies = allCurrencies,
            selectedCode = settings.currencyCode,
            onSelected = { 
                onCurrencyChange(it)
            },
            onDismiss = { showCurrencySheet = false }
        )
    }

    if (showLanguageSheet) {
        LanguageSelectionSheet(
            selectedTag = settings.languageTag,
            onSelected = { 
                onLanguageChange(it)
                showLanguageSheet = false
            },
            onDismiss = { showLanguageSheet = false }
        )
    }

    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = { showFeedbackDialog = false },
            onSend = { feedback ->
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("v94studio.apps@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "User Feedback Money Manager")
                    putExtra(Intent.EXTRA_TEXT, feedback)
                }
                try {
                    context.startActivity(Intent.createChooser(intent, "Send email..."))
                } catch (_: Exception) { }
                showFeedbackDialog = false
            }
        )
    }

    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyPolicy = false },
            onOpenOnline = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://valentino94-ctrl.github.io/MoneyManager/privacy-policy.html")
                )
                try {
                    context.startActivity(intent)
                } catch (_: Exception) { }
            }
        )
    }
}

@Composable
private fun PrivacyPolicyDialog(
    onDismiss: () -> Unit,
    onOpenOnline: () -> Unit
) {
    com.v94studio.moneymanager.ui.components.PremiumAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.privacy_policy)) },
        text = {
            Text(
                text = stringResource(R.string.privacy_policy_body),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onOpenOnline,
                modifier = Modifier
                    .widthIn(min = 160.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.open_online_policy),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .width(104.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = stringResource(R.string.btn_close),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var feedbackText by remember { mutableStateOf("") }

    com.v94studio.moneymanager.ui.components.PremiumAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feedback_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.feedback_dialog_text),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    placeholder = { Text(stringResource(R.string.feedback_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (feedbackText.isNotBlank()) onSend(feedbackText) },
                enabled = feedbackText.isNotBlank(),
                modifier = Modifier
                    .widthIn(min = 140.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_send_email),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .width(104.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_cancel),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )
        content()
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconCircle(icon = icon, color = iconColor, contentDescription = title)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    showDivider: Boolean = false
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconCircle(icon = icon, color = iconColor, contentDescription = title)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun IconCircle(
    icon: ImageVector,
    color: Color,
    contentDescription: String? = null
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
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
        modifier = Modifier.fillMaxHeight(0.68f)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.select_currency),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                placeholder = { Text(stringResource(R.string.search_currency_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.cd_search)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close)) } }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
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
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = sheetListState, 
                    modifier = Modifier.fillMaxSize(),
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
                    }
                }
                com.v94studio.moneymanager.ui.components.FadingLazyColumnScrollbar(
                    listState = sheetListState,
                    modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().padding(end = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
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
        modifier = Modifier.fillMaxHeight(0.68f)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.headlineSmall,
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
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = sheetListState, 
                    modifier = Modifier.fillMaxSize(),
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
                    }
                }
                com.v94studio.moneymanager.ui.components.FadingLazyColumnScrollbar(
                    listState = sheetListState,
                    modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().padding(end = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
