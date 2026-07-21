package com.v94studio.moneymanager.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v94studio.moneymanager.R
import com.v94studio.moneymanager.ui.theme.*

@Composable
fun LoadingScreen(
    onLogoPositioned: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    
    // Animation for the logo scale
    val infiniteTransition = rememberInfiniteTransition(label = "logoPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1D0B45)), // Match icon background exactly
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = if (isTablet) 800.dp else 500.dp).fillMaxWidth()
        ) {
            // App Icon - Clipped to show just the ring and currencies
            Box(
                modifier = Modifier
                    .widthIn(max = if (isTablet) 440.dp else 330.dp) // Zoomed in by another 5%
                    .fillMaxWidth(if (isTablet) 0.83f else 0.94f) // Zoomed in by another 5%
                    .aspectRatio(1f)
                    .scale(scale)
                    .onGloballyPositioned { coordinates ->
                        onLogoPositioned?.invoke(coordinates.boundsInWindow())
                    }
                    .clip(CircleShape), // Clips the square background out
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground_no_ring),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = stringResource(R.string.app_name),
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 24.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                fontWeight = FontWeight.Black,
                color = BrandPurple, // Use global brand color
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.loading_slogan),
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.25.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Modern Dot Loading Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    LoadingDot(index = index)
                }
            }
        }
    }
}

@Composable
private fun LoadingDot(index: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "dotAnim")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = index * 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
    )
}
