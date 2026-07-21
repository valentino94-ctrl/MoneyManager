package com.v94studio.moneymanager.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.v94studio.moneymanager.LocalUserSettings
import com.v94studio.moneymanager.ui.screens.formatPercent
import com.v94studio.moneymanager.ui.util.formatCurrency
import com.v94studio.moneymanager.ui.util.formatSignedAmount

@Composable
fun AnimatedAmountText(
    amount: Double,
    currencyCode: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    useSignedFormat: Boolean = false
) {
    val settings = LocalUserSettings.current
    var lastSeenValue by rememberSaveable { mutableStateOf<Float?>(null) }
    val anim = remember { Animatable(lastSeenValue ?: 0f) }
    val targetValue = amount.toFloat()

    LaunchedEffect(targetValue) {
        if (lastSeenValue != targetValue) {
            // Mark this value as handled before suspending so disposal mid-animation
            // does not cause the same value to replay when the item returns.
            lastSeenValue = targetValue
            anim.animateTo(
                targetValue = targetValue,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else {
            anim.snapTo(targetValue)
        }
    }

    val displayAmount = anim.value.toDouble()
    val formatted = if (settings.privacyModeEnabled) {
        "****"
    } else if (useSignedFormat) {
        formatSignedAmount(displayAmount, currencyCode)
    } else {
        formatCurrency(displayAmount, currencyCode)
    }

    Text(
        text = formatted,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight
    )
}

@Composable
fun AnimatedPercentText(
    percent: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null
) {
    val settings = LocalUserSettings.current
    var lastSeenValue by rememberSaveable { mutableStateOf<Float?>(null) }
    val anim = remember { Animatable(lastSeenValue ?: 0f) }
    val targetValue = percent.toFloat()

    LaunchedEffect(targetValue) {
        if (lastSeenValue != targetValue) {
            lastSeenValue = targetValue
            anim.animateTo(
                targetValue = targetValue,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else {
            anim.snapTo(targetValue)
        }
    }

    val formatted = if (settings.privacyModeEnabled) "****" else formatPercent(anim.value.toDouble())

    Text(
        text = formatted,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight
    )
}
