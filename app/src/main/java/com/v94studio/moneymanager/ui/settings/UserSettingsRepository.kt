package com.v94studio.moneymanager.ui.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.Currency
import java.util.Locale
import com.v94studio.moneymanager.ui.util.CurrencyDetector
import kotlinx.coroutines.flow.first

private const val SETTINGS_DATASTORE_NAME = "user_settings"

val Context.userSettingsDataStore by preferencesDataStore(name = SETTINGS_DATASTORE_NAME)

// DataStore-backed repository for theme, currency, and rolling range settings.
class UserSettingsRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val ROLLING_DAYS = intPreferencesKey("rolling_days")
        val TX_FILTER = stringPreferencesKey("tx_filter")
        val TX_RANGE_DAYS = intPreferencesKey("tx_range_days")
        val TX_SORT = stringPreferencesKey("tx_sort")
        val TX_QUERY = stringPreferencesKey("tx_query")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PRIVACY_MODE_ENABLED = booleanPreferencesKey("privacy_mode_enabled")
        val AUTO_APPROVE_RECURRING = booleanPreferencesKey("auto_approve_recurring")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val HAS_COMPLETED_TOUR = booleanPreferencesKey("has_completed_tour")
        val HAS_SEEN_FAB_TUTORIAL = booleanPreferencesKey("has_seen_fab_tutorial")
        val HAS_SEEN_SWIPE_TUTORIAL = booleanPreferencesKey("has_seen_swipe_tutorial")
        val HAS_SEEN_DASHBOARD_TUTORIAL = booleanPreferencesKey("has_seen_dashboard_tutorial")
        val HAS_SEEN_PLANNING_TUTORIAL = booleanPreferencesKey("has_seen_planning_tutorial")
        // Versioned separately from the former Dashboard savings summary tour.
        val HAS_SEEN_SAVINGS_TUTORIAL = booleanPreferencesKey("has_seen_savings_card_tutorial_v2")
        val HAS_SEEN_BUDGETS_TUTORIAL = booleanPreferencesKey("has_seen_budgets_card_tutorial_v1")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
    }

    // Stream settings with safe defaults on read failures.
    val settingsFlow: Flow<UserSettings> = context.userSettingsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val themeMode = prefs[Keys.THEME_MODE]?.let { stored ->
                runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.DARK)
            } ?: ThemeMode.DARK
            
            val currencyCode = prefs[Keys.CURRENCY_CODE] ?: CurrencyDetector.detectCurrencyCode(context)
            val rollingDays = prefs[Keys.ROLLING_DAYS]?.takeIf { it in setOf(7, 30, 90) } ?: 30
            val txFilter = prefs[Keys.TX_FILTER]?.let { stored ->
                runCatching { TransactionFilterSetting.valueOf(stored) }.getOrDefault(TransactionFilterSetting.ALL)
            } ?: TransactionFilterSetting.ALL
            val txRange = prefs[Keys.TX_RANGE_DAYS]?.takeIf { it in setOf(7, 30, 90) } ?: 30
            val txSort = prefs[Keys.TX_SORT]?.let { stored ->
                runCatching { TransactionSortSetting.valueOf(stored) }.getOrDefault(TransactionSortSetting.NEWEST)
            } ?: TransactionSortSetting.NEWEST
            val txQuery = prefs[Keys.TX_QUERY] ?: ""
            val biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false
            val privacyModeEnabled = prefs[Keys.PRIVACY_MODE_ENABLED] ?: false
            val autoApproveRecurring = prefs[Keys.AUTO_APPROVE_RECURRING] ?: true
            val onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false
            val hasCompletedTour = prefs[Keys.HAS_COMPLETED_TOUR] ?: false
            val hasSeenFabTutorial = prefs[Keys.HAS_SEEN_FAB_TUTORIAL] ?: false
            val hasSeenSwipeTutorial = prefs[Keys.HAS_SEEN_SWIPE_TUTORIAL] ?: false
            val hasSeenDashboardTutorial = prefs[Keys.HAS_SEEN_DASHBOARD_TUTORIAL] ?: false
            val hasSeenPlanningTutorial = prefs[Keys.HAS_SEEN_PLANNING_TUTORIAL] ?: false
            val hasSeenSavingsTutorial = prefs[Keys.HAS_SEEN_SAVINGS_TUTORIAL] ?: false
            val hasSeenBudgetsTutorial = prefs[Keys.HAS_SEEN_BUDGETS_TUTORIAL] ?: false
            val languageTag = prefs[Keys.LANGUAGE_TAG] ?: Locale.getDefault().toLanguageTag()
            UserSettings(
                themeMode = themeMode,
                currencyCode = currencyCode,
                rollingDays = rollingDays,
                transactionsFilter = txFilter,
                transactionsRangeDays = txRange,
                transactionsSort = txSort,
                transactionsQuery = txQuery,
                biometricEnabled = biometricEnabled,
                privacyModeEnabled = privacyModeEnabled,
                autoApproveRecurring = autoApproveRecurring,
                onboardingComplete = onboardingComplete,
                hasCompletedTour = hasCompletedTour,
                hasSeenFabTutorial = hasSeenFabTutorial,
                hasSeenSwipeTutorial = hasSeenSwipeTutorial,
                hasSeenDashboardTutorial = hasSeenDashboardTutorial,
                hasSeenPlanningTutorial = hasSeenPlanningTutorial,
                hasSeenSavingsTutorial = hasSeenSavingsTutorial,
                hasSeenBudgetsTutorial = hasSeenBudgetsTutorial,
                languageTag = languageTag,
            )
        }

    // Persist theme mode.
    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
        }
    }

    // Persist currency code.
    suspend fun setCurrencyCode(currencyCode: String) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.CURRENCY_CODE] = currencyCode
        }
    }

    // Persist rolling-days filter (guarded to allowed values).
    suspend fun setRollingDays(days: Int) {
        val safeDays = if (days in setOf(7, 30, 90)) days else 30
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.ROLLING_DAYS] = safeDays
        }
    }

    suspend fun setTransactionsFilter(filter: TransactionFilterSetting) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.TX_FILTER] = filter.name
        }
    }

    suspend fun setTransactionsRangeDays(days: Int) {
        val safeDays = if (days in setOf(7, 30, 90)) days else 30
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.TX_RANGE_DAYS] = safeDays
        }
    }

    suspend fun setTransactionsSort(sort: TransactionSortSetting) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.TX_SORT] = sort.name
        }
    }

    suspend fun setTransactionsQuery(query: String) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.TX_QUERY] = query
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETE] = complete
        }
    }

    suspend fun setHasCompletedTour(complete: Boolean) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.HAS_COMPLETED_TOUR] = complete
        }
    }

    suspend fun setHasSeenFabTutorial(seen: Boolean) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_FAB_TUTORIAL] = seen
        }
    }

    suspend fun setHasSeenSwipeTutorial(seen: Boolean) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_SWIPE_TUTORIAL] = seen
        }
    }

    suspend fun setHasSeenDashboardTutorial(seen: Boolean) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_DASHBOARD_TUTORIAL] = seen
        }
    }

    suspend fun setHasSeenPlanningTutorial(seen: Boolean) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_PLANNING_TUTORIAL] = seen
        }
    }

    suspend fun setHasSeenSavingsTutorial(seen: Boolean) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_SAVINGS_TUTORIAL] = seen
        }
    }

    suspend fun setHasSeenBudgetsTutorial(seen: Boolean) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_BUDGETS_TUTORIAL] = seen
        }
    }

    suspend fun resetTutorialFlags() {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.HAS_COMPLETED_TOUR] = false
            prefs[Keys.HAS_SEEN_FAB_TUTORIAL] = false
            prefs[Keys.HAS_SEEN_SWIPE_TUTORIAL] = false
            prefs[Keys.HAS_SEEN_DASHBOARD_TUTORIAL] = false
            prefs[Keys.HAS_SEEN_PLANNING_TUTORIAL] = false
            prefs[Keys.HAS_SEEN_SAVINGS_TUTORIAL] = false
            prefs[Keys.HAS_SEEN_BUDGETS_TUTORIAL] = false
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setPrivacyModeEnabled(enabled: Boolean) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.PRIVACY_MODE_ENABLED] = enabled
        }
    }

    suspend fun setAutoApproveRecurring(enabled: Boolean) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.AUTO_APPROVE_RECURRING] = enabled
        }
    }

    suspend fun setLanguageTag(tag: String) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_TAG] = tag
        }
    }

    /**
     * Checks if the currency code has been explicitly saved in preferences.
     */
    suspend fun isCurrencySaved(): Boolean {
        val prefs = context.userSettingsDataStore.data.first()
        return prefs.contains(Keys.CURRENCY_CODE)
    }
}
