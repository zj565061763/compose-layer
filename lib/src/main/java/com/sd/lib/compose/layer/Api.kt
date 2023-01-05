package com.sd.lib.compose.layer

import androidx.compose.animation.*
import androidx.compose.runtime.*

@Composable
fun rememberLayer(
    onCreate: (Layer) -> Unit = {},
    destroyOnDispose: Boolean = true,
    wrapper: @Composable LayerWrapperScope.() -> Unit = { LayerAnimatedVisibility() },
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
    wrapper: @Composable LayerWrapperScope.() -> Unit = { LayerAnimatedVisibility() },
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
fun LayerWrapperScope.LayerAnimatedVisibility(
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
) {
    AnimatedVisibility(
        visible = layer.isVisibleState,
        enter = enter,
        exit = exit,
    ) {
        Content()
    }
}

@Composable
private fun <T : LayerImpl> rememberLayer(
    factory: () -> T,
    destroyOnDispose: Boolean = true,
    wrapper: @Composable LayerWrapperScope.() -> Unit,
    content: @Composable LayerContentScope.() -> Unit
): T {
    val destroyOnDisposeUpdated by rememberUpdatedState(destroyOnDispose)

    val realContent: @Composable LayerContentScope.() -> Unit = {
        val contentScope = this
        remember {
            object : LayerWrapperScope {
                @Composable
                override fun Content() = content.invoke(contentScope)
                override val layer: Layer get() = contentScope.layer
            }
        }.also {
            wrapper.invoke(it)
        }
    }

    val layer = remember { factory() }.apply {
        this.Init()
        this.setContent(realContent)
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

interface LayerWrapperScope : LayerContentScope {
    @Composable
    fun Content()
}