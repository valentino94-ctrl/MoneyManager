package com.v94studio.moneymanager.ui.screens

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

internal fun paletteFrom(base: Color): List<Color> {
    return listOf(
        base,
        base.copy(alpha = 0.9f),
        base.copy(alpha = 0.8f),
        base.copy(alpha = 0.7f),
        base.copy(alpha = 0.6f)
    )
}

internal fun dynamicDistributionPalette(count: Int, colors: ColorScheme): List<Color> {
    val base = colors.primary
    val fallback = budgetDistributionPalette(colors)
    if (count <= fallback.size) return fallback.take(count)
    val result = mutableListOf<Color>()
    val startHue = hueFromColor(base)
    val paletteSize = maxOf(count, 20)
    val step = 360f / paletteSize.coerceAtLeast(1)
    for (i in 0 until paletteSize) {
        val hue = (startHue + step * i) % 360f
        result += Color.hsl(hue, 0.6f, 0.55f)
    }
    return replaceGreenWithMaroon(result)
}

private fun budgetDistributionPalette(colors: ColorScheme): List<Color> {
    val palette = listOf(
        colors.primary,
        colors.secondary,
        colors.tertiary,
        colors.error,
        colors.primaryContainer,
        colors.secondaryContainer,
        colors.tertiaryContainer,
        colors.errorContainer,
        colors.outline
    )
    return replaceGreenWithMaroon(palette)
}

private fun replaceGreenWithMaroon(colors: List<Color>): List<Color> {
    return colors.map { color ->
        val hue = hueFromColor(color)
        if (hue in 80f..160f) {
            Color.hsl(345f, 0.6f, 0.45f)
        } else {
            color
        }
    }
}

private fun hueFromColor(color: Color): Float {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    if (delta == 0f) return 0f
    return when (max) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
}
