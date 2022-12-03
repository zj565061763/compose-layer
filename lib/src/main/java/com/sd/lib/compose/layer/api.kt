package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * 用来存放Layer的容器
 */
@Composable
fun LayerContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val layerContainer = remember { LayerContainer() }
    CompositionLocalProvider(LocalLayerContainer provides layerContainer) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    layerContainer.updateContainerLayout(it)
                },
            contentAlignment = Alignment.Center,
        ) {
            content()
            layerContainer.Layers()
        }
    }
}

/**
 * 创建并记住[Layer]
 */
@Composable
fun rememberLayer(debug: Boolean = false): Layer {
    val layerContainer = checkNotNull(LocalLayerContainer.current) {
        "CompositionLocal LocalLayerContainer not present"
    }
    return layerContainer.rememberLayer(debug)
}

/**
 * 创建并记住[TargetLayer]
 */
@Composable
fun rememberTargetLayer(debug: Boolean = false): TargetLayer {
    val layerContainer = checkNotNull(LocalLayerContainer.current) {
        "CompositionLocal LocalLayerContainer not present"
    }
    return layerContainer.rememberTargetLayer(debug)
}

/**
 * 设置要对齐的目标并绑定[LayerContainer]作用域内唯一的[tag]
 */
fun Modifier.layerTarget(
    tag: String,
) = composed {
    val layerContainer = checkNotNull(LocalLayerContainer.current) {
        "CompositionLocal LocalLayerContainer not present"
    }

    DisposableEffect(layerContainer, tag) {
        onDispose {
            layerContainer.removeTarget(tag)
        }
    }

    this.onGloballyPositioned {
        layerContainer.addTarget(tag, it)
    }
}
