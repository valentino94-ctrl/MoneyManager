package com.v94studio.moneymanager.ui.components.featurediscovery

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.v94studio.moneymanager.ui.theme.BrandPurple
import kotlin.math.roundToInt
import kotlin.math.min

enum class DiscoveryShape {
    Circle, Rectangle
}

enum class DiscoveryPlacement {
    Auto, Above, Below
}

@Composable
fun FeatureDiscoveryOverlay(
    isVisible: Boolean,
    targetRect: Rect,
    title: String,
    description: String,
    onDismiss: () -> Unit,
    shape: DiscoveryShape = DiscoveryShape.Circle,
    placement: DiscoveryPlacement = DiscoveryPlacement.Auto
) {
    if (!isVisible || targetRect == Rect.Zero) return

    val density = LocalDensity.current
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    val localTargetRect = Rect(
        left = targetRect.left - overlayOrigin.x,
        top = targetRect.top - overlayOrigin.y,
        right = targetRect.right - overlayOrigin.x,
        bottom = targetRect.bottom - overlayOrigin.y
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(2000f)
            .onGloballyPositioned { coordinates ->
                overlayOrigin = coordinates.positionInWindow()
            }
            .pointerInput(localTargetRect) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.first()
                        val position = change.position
                        
                        // Dismiss on any press event outside target
                        if (event.type == PointerEventType.Press && !localTargetRect.contains(position)) {
                            onDismiss()
                        }

                        if (!localTargetRect.contains(position)) {
                            // Touch outside target area: block interaction with underlying UI
                            change.consume()
                        }
                    }
                }
            }
    ) {
        val overlayWidthDp = maxWidth.value
        val overlayHeightDp = maxHeight.value

        // Dimmed background with circular or rectangular cutout using EvenOdd fill type
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                addRect(Rect(Offset.Zero, size))
                if (localTargetRect != Rect.Zero) {
                    when (shape) {
                        DiscoveryShape.Circle -> {
                            val radius = (localTargetRect.width / 2f)
                            addOval(Rect(localTargetRect.center, radius))
                        }
                        DiscoveryShape.Rectangle -> {
                            addRoundRect(
                                RoundRect(
                                    rect = localTargetRect,
                                    cornerRadius = CornerRadius(24.dp.toPx())
                                )
                            )
                        }
                    }
                }
                fillType = PathFillType.EvenOdd
            }
            drawPath(path, Color.Black.copy(alpha = 0.8f))
        }

        if (localTargetRect != Rect.Zero) {
            // Pulse Animation
            when (shape) {
                DiscoveryShape.Circle -> {
                    FabPulse(center = localTargetRect.center, baseRadius = localTargetRect.width / 2f)
                }
                DiscoveryShape.Rectangle -> {
                    RectPulse(targetRect = localTargetRect)
                }
            }

            // Tooltip Bubble
            Box(modifier = Modifier.fillMaxSize()) {
                val screenWidthDp = overlayWidthDp
                val screenHeightDp = overlayHeightDp
                val tooltipWidthValue = min(320f, screenWidthDp - 32f).coerceAtLeast(240f)
                val tooltipWidth = tooltipWidthValue.dp
                var tooltipHeightPx by remember { mutableIntStateOf(0) }
                val tooltipHeightDp = tooltipHeightPx / density.density
                val targetCenterDp = localTargetRect.center.x / density.density
                val tooltipX = (targetCenterDp - tooltipWidthValue / 2f)
                    .coerceIn(16f, (screenWidthDp - tooltipWidthValue - 16f).coerceAtLeast(16f))
                val spaceAbove = localTargetRect.top / density.density - 16f
                val spaceBelow = screenHeightDp - localTargetRect.bottom / density.density - 16f
                val placeBelow = when (placement) {
                    DiscoveryPlacement.Above -> false
                    DiscoveryPlacement.Below -> true
                    DiscoveryPlacement.Auto -> spaceBelow >= tooltipHeightDp + 20f || spaceBelow > spaceAbove
                }
                val desiredY = if (placeBelow) {
                    localTargetRect.bottom / density.density + 20f
                } else {
                    localTargetRect.top / density.density - tooltipHeightDp - 20f
                }
                val tooltipY = desiredY.coerceIn(
                    16f,
                    (screenHeightDp - tooltipHeightDp - 16f).coerceAtLeast(16f)
                )
                val arrowCenter = (targetCenterDp - tooltipX)
                    .coerceIn(20f, tooltipWidthValue - 20f)

                TooltipBubble(
                    title = title,
                    description = description,
                    onDismiss = onDismiss,
                    modifier = Modifier
                        .width(tooltipWidth)
                        .onSizeChanged { tooltipHeightPx = it.height }
                        .offset {
                            IntOffset(
                                x = tooltipX.dp.roundToPx(),
                                y = tooltipY.dp.roundToPx()
                            )
                        },
                    arrowPosition = if (placeBelow) ArrowPosition.Top else ArrowPosition.Bottom,
                    arrowCenter = arrowCenter.dp,
                    maxBubbleHeight = (screenHeightDp * 0.55f).dp
                )
            }
        }
    }
}

enum class ArrowPosition { Top, Bottom }

@Composable
private fun FabPulse(center: Offset, baseRadius: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = baseRadius,
        targetValue = baseRadius + 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = BrandPurple.copy(alpha = alpha),
            radius = pulseRadius,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
private fun RectPulse(targetRect: Rect) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "rectPulse")
    val inflation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = with(density) { 8.dp.toPx() },
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inflation"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val inflatedRect = targetRect.inflate(inflation)
        drawRoundRect(
            color = BrandPurple.copy(alpha = alpha),
            topLeft = inflatedRect.topLeft,
            size = inflatedRect.size,
            cornerRadius = CornerRadius(24.dp.toPx() + inflation),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
private fun TooltipBubble(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    arrowPosition: ArrowPosition = ArrowPosition.Bottom,
    arrowCenter: androidx.compose.ui.unit.Dp,
    maxBubbleHeight: androidx.compose.ui.unit.Dp
) {
    val bubbleShape = RoundedCornerShape(24.dp)
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        if (arrowPosition == ArrowPosition.Top) {
            TooltipArrow(ArrowPosition.Top, arrowCenter, surfaceColor)
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxBubbleHeight)
                .shadow(16.dp, bubbleShape),
            shape = bubbleShape,
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            TooltipContent(title, description, onDismiss)
        }

        if (arrowPosition == ArrowPosition.Bottom) {
            TooltipArrow(ArrowPosition.Bottom, arrowCenter, surfaceColor)
        }
    }
}

@Composable
private fun TooltipArrow(position: ArrowPosition, center: androidx.compose.ui.unit.Dp, color: Color) {
    Box(Modifier.fillMaxWidth().height(12.dp)) {
        Canvas(
            modifier = Modifier
                .offset(x = center - 12.dp, y = if (position == ArrowPosition.Top) 1.dp else (-1).dp)
                .size(24.dp, 12.dp)
        ) {
            val path = Path().apply {
                if (position == ArrowPosition.Top) {
                    moveTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                } else {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                }
                close()
            }
            drawPath(path, color = color)
        }
    }
}

@Composable
private fun TooltipContent(
    title: String, 
    description: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Okay", fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Modifier to track the position of a discoverable feature.
 */
fun Modifier.discoverable(onPositioned: (Rect) -> Unit): Modifier = onGloballyPositioned { coords ->
    onPositioned(coords.positionInWindow().let { pos ->
        Rect(pos, coords.size.toSize())
    })
}
