package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * 用来存放layer的容器
 */
@Composable
fun FLayerContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val layerManager = remember { LayerManager() }
    CompositionLocalProvider(LocalLayerManager provides layerManager) {
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
fun rememberFLayer(): FLayer {
    val layerManager = checkNotNull(LocalLayerManager.current) {
        "CompositionLocal LocalLayerManager not present"
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
    val layerManager = checkNotNull(LocalLayerManager.current) {
        "CompositionLocal LocalLayerManager not present"
    }

    DisposableEffect(layerManager, tag) {
        onDispose {
            layerManager.removeTarget(tag)
        }
    }

    this.onGloballyPositioned {
        layerManager.addTarget(tagUpdated, it)
    }
}