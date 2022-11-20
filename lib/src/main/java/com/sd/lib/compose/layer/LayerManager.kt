package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity

internal val LocalFLayerManager = compositionLocalOf<FLayerManager?> { null }

internal class FLayerManager {
    private val _layerHolder = mutableStateListOf<FLayerState>()
    private val _layerTarget = mutableStateMapOf<String, LayoutCoordinates>()

    @Composable
    fun layer(): FLayerState {
        val state = remember { FLayerState() }.apply {
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

    fun layerTarget(tag: String, layoutCoordinates: LayoutCoordinates) {
        _layerTarget[tag] = layoutCoordinates
    }

    fun findTarget(tag: String): LayoutCoordinates? {
        return _layerTarget[tag]
    }

    @Composable
    fun Content() {
        _layerHolder.forEach { item ->
            item.Content(this)
        }
    }
}