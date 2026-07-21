package com.v94studio.moneymanager.ui.components.featurediscovery

import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v94studio.moneymanager.ui.settings.UserSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class FeatureDiscoveryState(
    val isLoaded: Boolean = false,
    val isVisible: Boolean = false,
    val targetRect: Rect = Rect.Zero,
    val hasSeenFabTutorial: Boolean = true,
    val hasSeenSwipeTutorial: Boolean = true,
    val hasSeenDashboardTutorial: Boolean = true,
    val hasSeenPlanningTutorial: Boolean = true,
    val hasSeenSavingsTutorial: Boolean = true,
    val hasSeenBudgetsTutorial: Boolean = true,
    val currentTutorial: TutorialType? = null
)

enum class TutorialType {
    FAB, SWIPE, DASHBOARD_INTRO, DASHBOARD_SCROLL_PENDING, DASHBOARD_BOTTOM, PLANNING_INTRO, SAVINGS_INTRO, BUDGETS_INTRO
}

class FeatureDiscoveryViewModel(
    private val repository: UserSettingsRepository,
    private val resetTutorialsForFreshInstall: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeatureDiscoveryState())
    val uiState: StateFlow<FeatureDiscoveryState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (resetTutorialsForFreshInstall) {
                repository.resetTutorialFlags()
            }
            val settings = repository.settingsFlow.first()
            _uiState.value = _uiState.value.copy(
                isLoaded = true,
                hasSeenFabTutorial = settings.hasSeenFabTutorial,
                hasSeenSwipeTutorial = settings.hasSeenSwipeTutorial,
                hasSeenDashboardTutorial = settings.hasSeenDashboardTutorial,
                hasSeenPlanningTutorial = settings.hasSeenPlanningTutorial,
                hasSeenSavingsTutorial = settings.hasSeenSavingsTutorial,
                hasSeenBudgetsTutorial = settings.hasSeenBudgetsTutorial
            )
        }
    }

    fun onTargetPositioned(type: TutorialType, rect: Rect) {
        if (rect != Rect.Zero && _uiState.value.currentTutorial == type && rect != _uiState.value.targetRect) {
            val shouldCommitFabTutorial = type == TutorialType.FAB && !_uiState.value.hasSeenFabTutorial
            val shouldCommitPlanningTutorial = type == TutorialType.PLANNING_INTRO && !_uiState.value.hasSeenPlanningTutorial
            val shouldCommitDashboardTutorial = type == TutorialType.DASHBOARD_INTRO && !_uiState.value.hasSeenDashboardTutorial
            val shouldCommitSavingsTutorial = type == TutorialType.SAVINGS_INTRO && !_uiState.value.hasSeenSavingsTutorial
            val shouldCommitBudgetsTutorial = type == TutorialType.BUDGETS_INTRO && !_uiState.value.hasSeenBudgetsTutorial
            _uiState.value = _uiState.value.copy(
                targetRect = rect,
                hasSeenFabTutorial = if (shouldCommitFabTutorial) true else _uiState.value.hasSeenFabTutorial,
                hasSeenPlanningTutorial = if (shouldCommitPlanningTutorial) true else _uiState.value.hasSeenPlanningTutorial,
                hasSeenDashboardTutorial = if (shouldCommitDashboardTutorial) true else _uiState.value.hasSeenDashboardTutorial,
                hasSeenSavingsTutorial = if (shouldCommitSavingsTutorial) true else _uiState.value.hasSeenSavingsTutorial,
                hasSeenBudgetsTutorial = if (shouldCommitBudgetsTutorial) true else _uiState.value.hasSeenBudgetsTutorial
            )
            if (shouldCommitFabTutorial) {
                viewModelScope.launch { repository.setHasSeenFabTutorial(true) }
            }
            if (shouldCommitPlanningTutorial) {
                viewModelScope.launch { repository.setHasSeenPlanningTutorial(true) }
            }
            if (shouldCommitDashboardTutorial) {
                viewModelScope.launch { repository.setHasSeenDashboardTutorial(true) }
            }
            if (shouldCommitSavingsTutorial) {
                viewModelScope.launch { repository.setHasSeenSavingsTutorial(true) }
            }
            if (shouldCommitBudgetsTutorial) {
                viewModelScope.launch { repository.setHasSeenBudgetsTutorial(true) }
            }
        }
    }

    fun showFabTutorial() {
        if (
            _uiState.value.isLoaded &&
            !_uiState.value.hasSeenFabTutorial &&
            _uiState.value.currentTutorial != TutorialType.FAB
        ) {
            _uiState.value = _uiState.value.copy(
                isVisible = true,
                targetRect = Rect.Zero,
                currentTutorial = TutorialType.FAB
            )
        }
    }

    fun showSwipeTutorial() {
        if (_uiState.value.isLoaded && !_uiState.value.hasSeenSwipeTutorial && _uiState.value.currentTutorial == null) {
            _uiState.value = _uiState.value.copy(
                hasSeenSwipeTutorial = true,
                isVisible = true,
                targetRect = Rect.Zero,
                currentTutorial = TutorialType.SWIPE
            )
            viewModelScope.launch { repository.setHasSeenSwipeTutorial(true) }
        }
    }

    fun showDashboardIntro() {
        if (_uiState.value.isLoaded && !_uiState.value.hasSeenDashboardTutorial && _uiState.value.currentTutorial == null) {
            _uiState.value = _uiState.value.copy(
                isVisible = true,
                targetRect = Rect.Zero,
                currentTutorial = TutorialType.DASHBOARD_INTRO
            )
        }
    }

    fun showDashboardBottom() {
        if (_uiState.value.currentTutorial == TutorialType.DASHBOARD_SCROLL_PENDING) {
            _uiState.value = _uiState.value.copy(
                isVisible = true,
                targetRect = Rect.Zero,
                currentTutorial = TutorialType.DASHBOARD_BOTTOM
            )
        }
    }

    fun showPlanningIntro() {
        if (
            _uiState.value.isLoaded &&
            !_uiState.value.hasSeenPlanningTutorial &&
            _uiState.value.currentTutorial != TutorialType.PLANNING_INTRO
        ) {
            _uiState.value = _uiState.value.copy(
                isVisible = true,
                targetRect = Rect.Zero,
                currentTutorial = TutorialType.PLANNING_INTRO
            )
        }
    }

    fun showSavingsTutorial() {
        if (_uiState.value.isLoaded && !_uiState.value.hasSeenSavingsTutorial && _uiState.value.currentTutorial == null) {
            _uiState.value = _uiState.value.copy(
                isVisible = true,
                targetRect = Rect.Zero,
                currentTutorial = TutorialType.SAVINGS_INTRO
            )
        }
    }

    fun showBudgetsTutorial() {
        if (_uiState.value.isLoaded && !_uiState.value.hasSeenBudgetsTutorial && _uiState.value.currentTutorial == null) {
            _uiState.value = _uiState.value.copy(isVisible = true, targetRect = Rect.Zero, currentTutorial = TutorialType.BUDGETS_INTRO)
        }
    }

    fun dismissTutorial() {
        val type = _uiState.value.currentTutorial
        when (type) {
                TutorialType.FAB -> {
                    _uiState.value = _uiState.value.copy(hasSeenFabTutorial = true, isVisible = false, currentTutorial = null, targetRect = Rect.Zero)
                }
                TutorialType.SWIPE -> {
                    _uiState.value = _uiState.value.copy(hasSeenSwipeTutorial = true, isVisible = false, currentTutorial = null, targetRect = Rect.Zero)
                }
                TutorialType.DASHBOARD_INTRO -> {
                    // Transition to pending scroll state
                    _uiState.value = _uiState.value.copy(isVisible = false, currentTutorial = TutorialType.DASHBOARD_SCROLL_PENDING, targetRect = Rect.Zero)
                }
                TutorialType.DASHBOARD_BOTTOM -> {
                    _uiState.value = _uiState.value.copy(hasSeenDashboardTutorial = true, isVisible = false, currentTutorial = null, targetRect = Rect.Zero)
                }
                TutorialType.DASHBOARD_SCROLL_PENDING -> {
                    _uiState.value = _uiState.value.copy(isVisible = false, currentTutorial = null, targetRect = Rect.Zero)
                }
                TutorialType.PLANNING_INTRO -> {
                    _uiState.value = _uiState.value.copy(hasSeenPlanningTutorial = true, isVisible = false, currentTutorial = null, targetRect = Rect.Zero)
                }
                TutorialType.SAVINGS_INTRO -> {
                    _uiState.value = _uiState.value.copy(hasSeenSavingsTutorial = true, isVisible = false, currentTutorial = null, targetRect = Rect.Zero)
                }
                TutorialType.BUDGETS_INTRO -> {
                    _uiState.value = _uiState.value.copy(hasSeenBudgetsTutorial = true, isVisible = false, currentTutorial = null, targetRect = Rect.Zero)
                }
                null -> {}
        }
    }

    fun dismissTutorialForNavigation() {
        val type = _uiState.value.currentTutorial ?: return
        _uiState.value = when (type) {
            TutorialType.FAB -> _uiState.value.copy(hasSeenFabTutorial = true)
            TutorialType.SWIPE -> _uiState.value.copy(hasSeenSwipeTutorial = true)
            TutorialType.DASHBOARD_INTRO,
            TutorialType.DASHBOARD_SCROLL_PENDING,
            TutorialType.DASHBOARD_BOTTOM -> _uiState.value.copy(hasSeenDashboardTutorial = true)
            TutorialType.PLANNING_INTRO -> _uiState.value.copy(hasSeenPlanningTutorial = true)
            TutorialType.SAVINGS_INTRO -> _uiState.value.copy(hasSeenSavingsTutorial = true)
            TutorialType.BUDGETS_INTRO -> _uiState.value.copy(hasSeenBudgetsTutorial = true)
        }.copy(
            isVisible = false,
            currentTutorial = null,
            targetRect = Rect.Zero
        )

        viewModelScope.launch {
            when (type) {
                TutorialType.FAB -> repository.setHasSeenFabTutorial(true)
                TutorialType.SWIPE -> repository.setHasSeenSwipeTutorial(true)
                TutorialType.DASHBOARD_INTRO,
                TutorialType.DASHBOARD_SCROLL_PENDING,
                TutorialType.DASHBOARD_BOTTOM -> repository.setHasSeenDashboardTutorial(true)
                TutorialType.PLANNING_INTRO -> repository.setHasSeenPlanningTutorial(true)
                TutorialType.SAVINGS_INTRO -> repository.setHasSeenSavingsTutorial(true)
                TutorialType.BUDGETS_INTRO -> repository.setHasSeenBudgetsTutorial(true)
            }
        }
    }
}
