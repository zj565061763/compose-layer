package com.sd.lib.compose.layer

import android.util.Log
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
    val layerManager = remember { LayerManager() }
    CompositionLocalProvider(LocalLayerManager provides layerManager) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    layerManager.updateContainerLayout(it)
                },
            contentAlignment = Alignment.Center,
        ) {
            content()
            layerManager.Layers()
        }
    }
}

/**
 * 创建并记住[Layer]
 */
@Composable
fun rememberLayer(debug: Boolean = false): Layer {
    val layerManager = checkNotNull(LocalLayerManager.current) {
        "CompositionLocal LocalLayerManager not present"
    }
    return layerManager.rememberLayer(debug)
}

/**
 * 创建并记住[TargetLayer]
 */
@Composable
fun rememberTargetLayer(debug: Boolean = false): TargetLayer {
    val layerManager = checkNotNull(LocalLayerManager.current) {
        "CompositionLocal LocalLayerManager not present"
    }
    return layerManager.rememberTargetLayer(debug)
}

/**
 * 设置要对齐的目标并绑定[LayerContainer]作用域内唯一的[tag]
 */
fun Modifier.layerTarget(
    tag: String,
) = composed {
    val layerManager = checkNotNull(LocalLayerManager.current) {
        "CompositionLocal LocalLayerManager not present"
    }

    DisposableEffect(layerManager, tag) {
        onDispose {
            layerManager.removeTarget(tag)
        }
    }

    this.onGloballyPositioned {
        layerManager.addTarget(tag, it)
    }
}

internal inline fun logMsg(isDebug: Boolean, block: () -> String) {
    if (isDebug) {
        Log.i("FLayer", block())
    }
}