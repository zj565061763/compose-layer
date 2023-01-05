package com.sd.lib.compose.layer

import androidx.compose.runtime.*

@Composable
fun rememberLayer(
    onCreate: (Layer) -> Unit = {},
    destroyOnDispose: Boolean = true,
    wrapper: @Composable LayerContentWrapperScope.() -> Unit = { LayerAnimatedVisibility() },
    content: @Composable LayerContentScope.() -> Unit
): Layer {
    return rememberLayer(
        factory = { LayerImpl().also(onCreate) },
        destroyOnDispose = destroyOnDispose,
        wrapper = wrapper,
        content = content,
    )
}

@Composable
fun rememberTargetLayer(
    onCreate: (TargetLayer) -> Unit = {},
    destroyOnDispose: Boolean = true,
    wrapper: @Composable LayerContentWrapperScope.() -> Unit = { LayerAnimatedVisibility() },
    content: @Composable LayerContentScope.() -> Unit
): TargetLayer {
    return rememberLayer(
        factory = { TargetLayerImpl().also(onCreate) },
        destroyOnDispose = destroyOnDispose,
        wrapper = wrapper,
        content = content,
    )
}

@Composable
private fun <T : LayerImpl> rememberLayer(
    factory: () -> T,
    destroyOnDispose: Boolean = true,
    wrapper: @Composable LayerContentWrapperScope.() -> Unit,
    content: @Composable LayerContentScope.() -> Unit
): T {
    val destroyOnDisposeUpdated by rememberUpdatedState(destroyOnDispose)

    val layer = remember { factory() }.apply {
        this.Init()
        this.setContentWrapper(wrapper)
        this.setContent(content)
    }

    DisposableEffect(layer) {
        onDispose {
            if (destroyOnDisposeUpdated) {
                layer.destroy()
            }
        }
    }
    return layer
}