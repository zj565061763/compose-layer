package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity

internal val LocalFLayerManager = compositionLocalOf<FLayerManager?> { null }

/**
 * 用来存放layer的容器
 */
@Composable
fun FLayerContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val layerManager = remember { FLayerManager() }
    CompositionLocalProvider(LocalFLayerManager provides layerManager) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            content()
            layerManager.Content()
        }
    }
}

@Composable
fun rememberFLayer(): FLayerState {
    val layerManager = checkNotNull(LocalFLayerManager.current) {
        "CompositionLocal LocalFLayerManager not present"
    }
    return layerManager.layer()
}

/**
 * 设置layer要对齐的目标，给目标设置唯一的[tag]
 */
fun Modifier.fLayerTarget(
    tag: String,
) = composed {
    val tagUpdated by rememberUpdatedState(tag)
    val layerManager = checkNotNull(LocalFLayerManager.current) {
        "CompositionLocal LocalFLayerManager not present"
    }
    this.onGloballyPositioned {
        layerManager.layerTarget(tagUpdated, it)
    }
}

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