package com.sd.lib.compose.layer

import androidx.compose.runtime.*
import androidx.compose.ui.layout.LayoutCoordinates

internal val LocalFLayerManager = compositionLocalOf<FLayerManager?> { null }

internal class FLayerManager {
    private val _layerHolder = mutableStateListOf<FLayer>()
    private val _layerTarget = mutableStateMapOf<String, TargetLayoutCoordinates>()

    @Composable
    fun layer(): FLayer {
        val state = remember { FLayer() }
        DisposableEffect(state) {
            _layerHolder.add(state)
            onDispose {
                _layerHolder.remove(state)
            }
        }
        return state
    }

    fun addTarget(tag: String, layoutCoordinates: LayoutCoordinates) {
        val old = _layerTarget[tag]
        val id = if (old == null) true else !old.id
        _layerTarget[tag] = TargetLayoutCoordinates(
            id = id,
            layoutCoordinates = layoutCoordinates
        )
    }

    fun removeTarget(tag: String) {
        _layerTarget.remove(tag)
    }

    fun findTarget(tag: String): LayoutCoordinates? {
        return _layerTarget[tag]?.layoutCoordinates
    }

    @Composable
    fun Content() {
        _layerHolder.forEach { item ->
            item.Content(this)
        }
    }
}

private data class TargetLayoutCoordinates(
    val id: Boolean,
    val layoutCoordinates: LayoutCoordinates,
)