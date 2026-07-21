package com.v94studio.moneymanager.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun FadingLazyColumnScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    thickness: Dp = 3.dp,
    hideDelayMillis: Long = 900L
) {
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    
    val scrollInfo by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo
            
            if (totalItems == 0 || visibleItems.isEmpty() || viewportHeightPx <= 0) {
                null
            } else {
                // Calculate how many items are currently visible
                val visibleCount = visibleItems.size
                
                // Calculate progress: (current index + item offset %) / (total - visible)
                val firstItem = visibleItems.first()
                val scrollOffset = listState.firstVisibleItemScrollOffset.toFloat()
                val firstItemSize = firstItem.size.toFloat().coerceAtLeast(1f)
                
                val progress = if (totalItems > visibleCount) {
                    (listState.firstVisibleItemIndex.toFloat() + (scrollOffset / firstItemSize)) / 
                    (totalItems - visibleCount).toFloat()
                } else {
                    0f
                }
                
                val thumbHeightPx = (viewportHeightPx * 0.15f).coerceAtLeast(20f)
                val thumbOffsetPx = (progress.coerceIn(0f, 1f) * (viewportHeightPx - thumbHeightPx))
                
                Pair(thumbHeightPx, thumbOffsetPx)
            }
        }
    }

    val indicatorVisible = rememberScrollIndicatorVisible(
        listState = listState,
        hideDelayMillis = hideDelayMillis
    )
    
    val canScroll = listState.canScrollForward || listState.canScrollBackward
    val alpha by animateFloatAsState(
        targetValue = if (indicatorVisible && canScroll) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "lazyScrollbarAlpha"
    )

    val density = LocalDensity.current

    Box(modifier = modifier.onSizeChanged { viewportHeightPx = it.height }) {
        val info = scrollInfo
        if (alpha > 0f && info != null) {
            val (heightPx, offsetPx) = info
            val thumbHeightDp = with(density) { heightPx.toDp() }
            val thumbOffsetDp = with(density) { offsetPx.toDp() }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = thumbOffsetDp)
                    .width(thickness)
                    .height(thumbHeightDp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun FadingLazyRowScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    thickness: Dp = 3.dp,
    hideDelayMillis: Long = 900L
) {
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    
    val scrollInfo by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo
            
            if (totalItems == 0 || visibleItems.isEmpty() || viewportWidthPx <= 0) {
                null
            } else {
                val visibleCount = visibleItems.size
                val firstItem = visibleItems.first()
                val scrollOffset = listState.firstVisibleItemScrollOffset.toFloat()
                val firstItemSize = firstItem.size.toFloat().coerceAtLeast(1f)
                
                val progress = if (totalItems > visibleCount) {
                    (listState.firstVisibleItemIndex.toFloat() + (scrollOffset / firstItemSize)) / 
                    (totalItems - visibleCount).toFloat()
                } else {
                    0f
                }
                
                val thumbWidthPx = (viewportWidthPx * 0.15f).coerceAtLeast(20f)
                val thumbOffsetPx = (progress.coerceIn(0f, 1f) * (viewportWidthPx - thumbWidthPx))
                
                Pair(thumbWidthPx, thumbOffsetPx)
            }
        }
    }

    val indicatorVisible = rememberScrollIndicatorVisible(
        listState = listState,
        hideDelayMillis = hideDelayMillis
    )
    
    val canScroll = listState.canScrollForward || listState.canScrollBackward
    val alpha by animateFloatAsState(
        targetValue = if (indicatorVisible && canScroll) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "lazyRowScrollbarAlpha"
    )

    val density = LocalDensity.current

    Box(modifier = modifier.onSizeChanged { viewportWidthPx = it.width }) {
        val info = scrollInfo
        if (alpha > 0f && info != null) {
            val (widthPx, offsetPx) = info
            val thumbWidthDp = with(density) { widthPx.toDp() }
            val thumbOffsetDp = with(density) { offsetPx.toDp() }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = thumbOffsetDp)
                    .height(thickness)
                    .width(thumbWidthDp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun rememberScrollIndicatorVisible(
    listState: LazyListState,
    hideDelayMillis: Long
): Boolean {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(
        listState.firstVisibleItemIndex,
        listState.firstVisibleItemScrollOffset
    ) {
        visible = true
        delay(hideDelayMillis)
        visible = false
    }
    return visible
}
