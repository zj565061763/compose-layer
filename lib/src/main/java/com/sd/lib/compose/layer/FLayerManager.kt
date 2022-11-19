package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity

internal val LocalFLayerManager = compositionLocalOf<FLayerManager?> { null }

internal class FLayerManager {
    private val _layerHolder = mutableStateListOf<FLayerState>()

    @Composable
    fun layer(
        alignTarget: Boolean,
        content: @Composable (layerState: FLayerState) -> Unit
    ): FLayerState {
        val state = remember { FLayerState(alignTarget, content) }.apply {
            this.statusBarHeight = WindowInsets.statusBars.getTop(LocalDensity.current)
        }
        DisposableEffect(state) {
            _layerHolder.add(state)
            onDispose {
                _layerHolder.remove(state)
            }
        }
        return state
    }

    @Composable
    fun Content() {
        _layerHolder.forEach { item ->
            item.Content()
        }
    }
}