package com.v94studio.moneymanager.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.v94studio.moneymanager.LocalRepository
import com.v94studio.moneymanager.LocalFeatureDiscovery
import com.v94studio.moneymanager.R
import com.v94studio.moneymanager.ui.components.FintechSegmentedControl
import com.v94studio.moneymanager.ui.components.MoneyTopAppBar
import com.v94studio.moneymanager.ui.navigation.MMActivePurple
import com.v94studio.moneymanager.ui.components.featurediscovery.DiscoveryShape
import com.v94studio.moneymanager.ui.components.featurediscovery.FeatureDiscoveryOverlay
import com.v94studio.moneymanager.ui.components.featurediscovery.TutorialType
import com.v94studio.moneymanager.ui.components.featurediscovery.discoverable
import kotlinx.coroutines.launch

enum class PlanningTab {
    BUDGETS, SAVINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    budgetsListState: LazyListState = rememberLazyListState(),
    savingsListState: LazyListState = rememberLazyListState()
) {
    var selectedTab by rememberSaveable { mutableStateOf(PlanningTab.BUDGETS) }
    val tabStateHolder = rememberSaveableStateHolder()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val featureDiscoveryViewModel = LocalFeatureDiscovery.current
    val featureDiscoveryState by featureDiscoveryViewModel.uiState.collectAsState()
    var planningControlRect by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(
        featureDiscoveryState.isLoaded,
        featureDiscoveryState.hasSeenPlanningTutorial,
        featureDiscoveryState.currentTutorial,
        planningControlRect
    ) {
        if (
            featureDiscoveryState.isLoaded &&
            !featureDiscoveryState.hasSeenPlanningTutorial &&
            featureDiscoveryState.currentTutorial != TutorialType.PLANNING_INTRO
        ) {
            featureDiscoveryViewModel.showPlanningIntro()
        } else if (
            featureDiscoveryState.currentTutorial == TutorialType.PLANNING_INTRO &&
            planningControlRect != Rect.Zero
        ) {
            featureDiscoveryViewModel.onTargetPositioned(TutorialType.PLANNING_INTRO, planningControlRect)
        }
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MoneyTopAppBar(
                title = "Planning",
                subtitle = "Track and allocate",
                scrollBehavior = scrollBehavior,
                bottomContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FintechSegmentedControl(
                            modifier = Modifier
                                .widthIn(max = 480.dp)
                                .discoverable { planningControlRect = it },
                            options = listOf(
                                PlanningTab.BUDGETS to stringResource(R.string.tab_budgeting),
                                PlanningTab.SAVINGS to stringResource(R.string.tab_saving_goals)
                            ),
                            selectedOption = selectedTab,
                            onOptionSelect = { selectedTab = it }
                        )
                    }
                }
            )
        }
    ) { padding ->
        val topPadding = padding.calculateTopPadding()
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.padding(top = (topPadding - 60.dp).coerceAtLeast(0.dp) + 28.dp),
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "PlanningContent"
            ) { tab ->
                tabStateHolder.SaveableStateProvider(tab.name) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (tab) {
                            PlanningTab.BUDGETS -> BudgetsScreen(listState = budgetsListState)
                            PlanningTab.SAVINGS -> SavingGoalsScreen(listState = savingsListState)
                        }
                    }
                }
            }
        }
    }

        if (
            featureDiscoveryState.isVisible &&
            featureDiscoveryState.currentTutorial == TutorialType.PLANNING_INTRO
        ) {
            FeatureDiscoveryOverlay(
                isVisible = true,
                targetRect = featureDiscoveryState.targetRect,
                title = "Plan your money",
                description = "Planning helps you set category budgets and build saving goals. Use the tabs above to switch between your budgets and savings progress.",
                onDismiss = featureDiscoveryViewModel::dismissTutorial,
                shape = DiscoveryShape.Rectangle
            )
        }
    }
}
