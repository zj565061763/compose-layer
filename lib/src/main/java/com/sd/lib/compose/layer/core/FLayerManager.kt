package com.sd.lib.compose.layer.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
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
                    val statusBarTop = WindowInsets.statusBars.getTop(LocalDensity.current)
                    var offset by remember { mutableStateOf(IntOffsetUnspecified) }
                    LaunchedEffect(item.alignment, item.layerSize, item.targetLayoutCoordinates) {
                        val position = item.calculatePosition()
                        if (position != IntOffsetUnspecified) {
                            offset = position.copy(y = position.y - statusBarTop)
                        }
                    }

                    Box(modifier = Modifier
                        .layout { measurable, constraints ->
                            val firstMeasure = measurable
                                .measure(constraints)
                                .also {
                                    item.layerSize = IntSize(it.width, it.height)
                                }

                            val placeable = if (offset != IntOffsetUnspecified) {
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
                    ) {
                        item.content()
                    }
                }
            }
        }
    }
}