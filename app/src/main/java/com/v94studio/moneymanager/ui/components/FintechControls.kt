package com.v94studio.moneymanager.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.v94studio.moneymanager.ui.theme.BrandPurpleGradient
import com.v94studio.moneymanager.ui.theme.FintechSecondary

@Composable
fun <T> FintechSegmentedControl(
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = options.indexOfFirst { it.first == selectedOption }.coerceAtLeast(0)
    
    Surface(
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val itemWidth = maxWidth / options.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(stiffness = 500f),
                label = "indicatorOffset"
            )

            // Sliding Indicator
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(BrandPurpleGradient))
            )

            Row(modifier = Modifier.fillMaxSize()) {
                options.forEach { (option, label) ->
                    val isSelected = option == selectedOption
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onOptionSelect(option) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                            color = if (isSelected) Color.White else FintechSecondary
                        )
                    }
                }
            }
        }
    }
}
