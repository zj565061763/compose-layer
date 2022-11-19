package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import java.util.*

internal val LocalFLayerManager = compositionLocalOf<FLayerManager?> { null }

internal class FLayerManager {
    private val layerHolder = mutableStateMapOf<String, FLayerState>()

    @Composable
    internal fun layer(state: FLayerState, content: @Composable () -> Unit): String {
        val tag = remember { UUID.randomUUID().toString() }
        layerHolder[tag] = state
        state.content = content
        state.statusBarHeight = WindowInsets.statusBars.getTop(LocalDensity.current)
        DisposableEffect(true) {
            onDispose {
                layerHolder.remove(tag)
            }
        }
        return tag
    }

    @Composable
    internal fun Content() {
        layerHolder.values.forEach { item ->
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