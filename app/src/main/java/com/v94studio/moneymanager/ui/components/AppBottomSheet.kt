package com.v94studio.moneymanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A standardized bottom sheet with a premium "Fintech" style.
 * 
 * Features:
 * - Content-driven height.
 * - Snaps to final position.
 * - Closable via swipe down or clicking the background.
 * - Proper inset handling for system navigation and keyboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showDragHandle: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = containerColor,
        tonalElevation = 0.dp, // Clean fintech look (prevents purple tint)
        scrimColor = Color.Black.copy(alpha = 0.4f), // High-quality scrim
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = if (showDragHandle) {
            { 
                Box(
                    Modifier
                        .padding(top = 12.dp, bottom = 12.dp)
                        .size(36.dp, 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                )
            }
        } else null,
        // Reset default window insets to handle them manually and precisely inside
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(modifier) // Apply passed modifiers (like height constraints) to the content
                .navigationBarsPadding() // Respect system navigation
                .imePadding() // Respect keyboard
                .padding(bottom = 16.dp) // Base safety padding
        ) {
            content()
        }
    }
}
