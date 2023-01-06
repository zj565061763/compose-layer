package com.sd.lib.compose.layer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun rememberLayerApi(
    onCreate: (Layer) -> Unit = {},
    destroyOnDispose: Boolean = true,
    wrapper: @Composable LayerContentWrapperScope.() -> Unit = { LayerAnimatedVisibility() },
    content: @Composable LayerContentScope.() -> Unit
): LayerApi {
    val layerApi = remember { LayerApiImpl() }
    if (layerApi.isAttached) {
        rememberLayer(
            onCreate = onCreate,
            destroyOnDispose = destroyOnDispose,
            wrapper = wrapper,
            content = content,
        ).also {
            layerApi.syncAttachState(it)
        }
    }
    return layerApi
}

@Composable
fun rememberTargetLayerApi(
    onCreate: (TargetLayer) -> Unit = {},
    destroyOnDispose: Boolean = true,
    wrapper: @Composable LayerContentWrapperScope.() -> Unit = { LayerAnimatedVisibility() },
    content: @Composable LayerContentScope.() -> Unit
): LayerApi {
    val layerApi = remember { LayerApiImpl() }
    if (layerApi.isAttached) {
        rememberTargetLayer(
            onCreate = onCreate,
            destroyOnDispose = destroyOnDispose,
            wrapper = wrapper,
            content = content,
        ).also {
            layerApi.syncAttachState(it)
        }
    }
    return layerApi
}

interface LayerApi {
    fun attach()
}

internal class LayerApiImpl : LayerApi {
    private val _attach = mutableStateOf(false)

    val isAttached: Boolean get() = _attach.value

    override fun attach() {
        _attach.value = true
    }

    @Composable
    fun syncAttachState(layer: Layer) {
        if (isAttached) {
            DisposableEffect(layer) {
                val callback: (Layer) -> Unit = {
                    _attach.value = false
                }
                layer.registerDetachCallback(callback)
                layer.attach()
                onDispose {
                    layer.unregisterDetachCallback(callback)
                }
            }
        }
    }
}