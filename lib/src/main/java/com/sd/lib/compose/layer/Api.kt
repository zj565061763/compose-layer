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
    return rememberLayer(
        factory = { LayerImpl().also(onCreate) },
        wrapper = wrapper,
        content = content,
    )
}

@Composable
fun rememberTargetLayer(
    onCreate: (TargetLayer) -> Unit = {},
    wrapper: @Composable LayerContentWrapperScope.() -> Unit = { LayerAnimatedVisibility() },
    content: @Composable LayerContentScope.() -> Unit
): TargetLayer {
    return rememberLayer(
        factory = { TargetLayerImpl().also(onCreate) },
        wrapper = wrapper,
        content = content,
    )
}

@Composable
private fun <T : LayerImpl> rememberLayer(
    factory: () -> T,
    wrapper: @Composable LayerContentWrapperScope.() -> Unit,
    content: @Composable LayerContentScope.() -> Unit
): T {
    val layer = remember { factory() }.apply {
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