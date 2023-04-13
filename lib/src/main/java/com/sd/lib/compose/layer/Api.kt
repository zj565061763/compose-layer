package com.sd.lib.compose.layer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

@Composable
fun rememberLayer(
    onCreate: (Layer) -> Unit = {},
    wrapper: @Composable LayerContentWrapperScope.() -> Unit = { LayerAnimatedVisibility() },
    content: @Composable LayerContentScope.() -> Unit
): Layer {
    val layer = remember {
        LayerImpl().also(onCreate)
    }.apply {
        this.Init()
        this.setContentWrapper(wrapper)
        this.setContent(content)
    }
    DisposableEffect(layer) {
        onDispose {
            layer.destroy()
        }
    }
    return layer
}

@Composable
fun rememberTargetLayer(
    onCreate: (TargetLayer) -> Unit = {},
    wrapper: @Composable LayerContentWrapperScope.() -> Unit = { LayerAnimatedVisibility() },
    content: @Composable LayerContentScope.() -> Unit
): TargetLayer {
    val layer = remember {
        TargetLayerImpl().also(onCreate)
    }.apply {
        this.Init()
        this.setContentWrapper(wrapper)
        this.setContent(content)
    }
    DisposableEffect(layer) {
        onDispose {
            layer.destroy()
        }
    }
    return layer
}