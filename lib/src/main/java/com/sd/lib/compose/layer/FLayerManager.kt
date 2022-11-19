package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity

internal val LocalFLayerManager = compositionLocalOf<FLayerManager?> { null }

internal class FLayerManager {
    private val _layerHolder = mutableStateListOf<FLayerState>()

    @Composable
    internal fun Layer(state: FLayerState, content: @Composable () -> Unit) {
        state.content = content
        state.statusBarHeight = WindowInsets.statusBars.getTop(LocalDensity.current)

        DisposableEffect(state) {
            _layerHolder.add(state)
            onDispose {
                _layerHolder.remove(state)
            }
        }
    }

    @Composable
    internal fun Content() {
        _layerHolder.forEach { item ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (item.alignTarget) {
                    LaunchedEffect(item.alignment) {
                        item.updatePosition()
                    }

                    Box(modifier = Modifier
                        .onSizeChanged {
                            item.layerSize = it
                        }
                        .offset {
                            item.layerOffset
                        }
                    ) {
                        item.content()
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(item.alignment)
                    ) {
                        item.content()
                    }
                }
            }
        }
    }
}