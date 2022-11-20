package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity

internal val LocalFLayerManager = compositionLocalOf<FLayerManager?> { null }

internal class FLayerManager {
    private val _layerHolder = mutableStateListOf<FLayer>()
    private val _layerTarget = mutableStateMapOf<String, LayoutCoordinates>()

    @Composable
    fun layer(): FLayer {
        val state = remember { FLayer() }.apply {
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

    fun addTarget(tag: String, layoutCoordinates: LayoutCoordinates) {
        logMsg { "addTarget $tag -> $layoutCoordinates" }
        _layerTarget[tag] = layoutCoordinates
    }

    fun removeTarget(tag: String) {
        logMsg { "removeTarget $tag" }
        _layerTarget.remove(tag)
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