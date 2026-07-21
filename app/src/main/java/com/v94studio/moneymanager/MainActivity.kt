package com.v94studio.moneymanager

import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.v94studio.moneymanager.ui.util.BiometricHelper
import com.v94studio.moneymanager.ui.settings.UserSettingsRepository
import com.v94studio.moneymanager.ui.screens.LoadingScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.flow.first

class MainActivity : AppCompatActivity() {
    private var loadingLogoBounds: androidx.compose.ui.geometry.Rect? = null

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val iconView = splashScreenView.iconView
            val iconLocation = IntArray(2)
            iconView.getLocationInWindow(iconLocation)
            val sourceCenterX = iconLocation[0] + iconView.width / 2f
            val sourceCenterY = iconLocation[1] + iconView.height / 2f
            val targetBounds = loadingLogoBounds
            val fallbackTargetWidth = minOf(
                330f,
                resources.configuration.screenWidthDp * 0.94f
            ) * resources.displayMetrics.density
            val targetWidth = ((targetBounds?.width ?: fallbackTargetWidth) -
                16f * resources.displayMetrics.density).times(0.95f * 0.98f).coerceAtLeast(1f)
            val targetCenterX = targetBounds?.center?.x ?: sourceCenterX
            val targetCenterY = (targetBounds?.center?.y ?: sourceCenterY) +
                17f * resources.displayMetrics.density
            val targetScale = targetWidth / iconView.width.coerceAtLeast(1)

            iconView.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .translationX(targetCenterX - sourceCenterX)
                .translationY(targetCenterY - sourceCenterY)
                .setDuration(620L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            splashScreenView.view.animate()
                .alpha(0f)
                .setDuration(620L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { splashScreenView.remove() }
                .start()
        }
        
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val appContainer = remember { AppContainer(applicationContext) }
            val settingsRepository = remember { UserSettingsRepository(applicationContext) }
            
            var isUnlocked by remember { mutableStateOf(false) }
            var isLoadingSettings by remember { mutableStateOf(true) }
            var biometricEnabledInSettings by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                val settings = settingsRepository.settingsFlow.first()
                biometricEnabledInSettings = settings.biometricEnabled
                
                if (settings.biometricEnabled && BiometricHelper.canAuthenticate(this@MainActivity)) {
                    BiometricHelper.showBiometricPrompt(
                        activity = this@MainActivity,
                        onSuccess = { 
                            isUnlocked = true
                            isLoadingSettings = false
                        },
                        onError = { 
                            // In case of error, we still need to stop loading but don't unlock
                            isLoadingSettings = false
                        }
                    )
                } else {
                    isUnlocked = true
                    isLoadingSettings = false
                }
            }
            
            if (isLoadingSettings) {
                // Render the real loading logo beneath the system splash so the
                // exit animation can morph directly into the same visual target.
                LoadingScreen { bounds -> loadingLogoBounds = bounds }
            } else if (isUnlocked || !biometricEnabledInSettings) {
                MoneyManagerApp(
                    appContainer = appContainer,
                    windowSizeClass = windowSizeClass
                )
            } else {
                // Locked Screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.msg_locked),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.msg_unlock_to_view),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = {
                                BiometricHelper.showBiometricPrompt(
                                    activity = this@MainActivity,
                                    onSuccess = { isUnlocked = true },
                                    onError = { }
                                )
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.btn_unlock))
                        }
                    }
                }
            }
        }
    }
}
