package com.sd.lib.compose.layer.core

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import java.util.*

internal val LocalFLayerManager = compositionLocalOf { FLayerManager() }

internal class FLayerManager {
    private val layerHolder = mutableStateMapOf<String, FLayerState>()

    @Composable
    internal fun layer(state: FLayerState, content: @Composable () -> Unit): String {
        val tag = remember { UUID.randomUUID().toString() }
        layerHolder[tag] = state
        state.content = content
        state.windowInsetsStatusBar = WindowInsets.statusBars
        state.density = LocalDensity.current
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
                val modifier = if (item.alignTarget) {
                    Modifier
                        .onSizeChanged { item.targetSize = it }
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(item.relativeOffset.x.toInt(), item.relativeOffset.y.toInt())
                            }
                        }
                } else {
                    Modifier
                        .align(item.alignment)
                        .offset(x = item.x, y = item.y)
                }
                Box(modifier = modifier) {
                    item.content()
                }
            }
        }
    }
}