package com.sd.lib.compose.layer

import androidx.compose.runtime.*

@Composable
fun rememberLayer(
    onCreate: (Layer) -> Unit = {},
    destroyOnDispose: Boolean = true,
    content: @Composable Layer.ContentScope.() -> Unit
): Layer {
    return rememberLayer(
        factory = { FLayer().also(onCreate) },
        destroyOnDispose = destroyOnDispose,
        content = content,
    )
}

@Composable
fun rememberTargetLayer(
    onCreate: (TargetLayer) -> Unit = {},
    destroyOnDispose: Boolean = true,
    content: @Composable Layer.ContentScope.() -> Unit
): TargetLayer {
    return rememberLayer(
        factory = { FTargetLayer().also(onCreate) },
        destroyOnDispose = destroyOnDispose,
        content = content,
    )
}

@Composable
private fun <T : FLayer> rememberLayer(
    factory: () -> T,
    destroyOnDispose: Boolean = true,
    content: @Composable Layer.ContentScope.() -> Unit
): T {
    val destroyOnDisposeUpdated by rememberUpdatedState(destroyOnDispose)

    val layer = remember { factory() }.apply {
        this.Init()
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