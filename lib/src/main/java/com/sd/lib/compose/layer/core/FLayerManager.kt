package com.sd.lib.compose.layer.core

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.util.*

internal val LocalFLayerManager = compositionLocalOf { FLayerManager() }

internal class FLayerManager {
    private val layerHolder = mutableStateMapOf<String, FLayerState>()

    @Composable
    internal fun layer(state: FLayerState, content: @Composable () -> Unit): String {
        val tag = remember { UUID.randomUUID().toString() }
        layerHolder[tag] = state
        state.content = content
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
                    val density = LocalDensity.current
                    val statusBarTop = WindowInsets.statusBars.getTop(density)
                    var offset by remember { mutableStateOf(IntOffset.Zero) }
                    LaunchedEffect(item.alignment, item.x, item.y, item.layerSize, item.targetLayoutCoordinates, density) {
                        val position = item.calculatePosition(density)
                        offset = position.copy(y = position.y - statusBarTop)
                    }

                    Box(modifier = Modifier
                        .layout { measurable, constraints ->
                            val firstMeasure = measurable
                                .measure(constraints)
                                .also {
                                    item.layerSize = IntSize(it.width, it.height)
                                }

                            val placeable = if (offset != IntOffset.Zero) {
                                measurable.measure(item.transformConstraints(constraints, offset))
                            } else {
                                firstMeasure
                            }

                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(offset.x, offset.y)
                            }
                        }
                    ) {
                        item.content()
                    }
                } else {
                    Box(modifier = Modifier
                        .align(item.alignment)
                        .offset(x = item.x, y = item.y)
                    ) {
                        item.content()
                    }
                }
            }
        }
    }
}