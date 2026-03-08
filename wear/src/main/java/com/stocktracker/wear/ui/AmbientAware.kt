package com.stocktracker.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal that indicates whether the watch is currently in ambient (always-on display) mode.
 * When true, screens should render simplified, burn-in-safe layouts.
 */
val LocalAmbientMode = compositionLocalOf { false }

/**
 * Wrapper composable that provides ambient mode state via [LocalAmbientMode].
 */
@Composable
fun AmbientAware(
    isAmbient: Boolean,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAmbientMode provides isAmbient) {
        content()
    }
}
