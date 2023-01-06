package com.sd.lib.compose.layer

import androidx.compose.runtime.*

@Composable
fun rememberLayerAttach(
    onCreate: (Layer) -> Unit = {},
    destroyOnDispose: Boolean = true,
    wrapper: @Composable LayerContentWrapperScope.() -> Unit = { LayerAnimatedVisibility() },
    content: @Composable LayerContentScope.() -> Unit
): MutableState<Boolean> {
    val attach = remember { mutableStateOf(false) }
    if (attach.value) {
        rememberLayer(
            onCreate = onCreate,
            destroyOnDispose = destroyOnDispose,
            wrapper = wrapper,
            content = content,
        ).also {
            it.SyncAttach(attach)
        }
    }
    return attach
}

@Composable
fun rememberTargetLayerAttach(
    onCreate: (TargetLayer) -> Unit = {},
    destroyOnDispose: Boolean = true,
    wrapper: @Composable LayerContentWrapperScope.() -> Unit = { LayerAnimatedVisibility() },
    content: @Composable LayerContentScope.() -> Unit
): MutableState<Boolean> {
    val attach = remember { mutableStateOf(false) }
    if (attach.value) {
        rememberTargetLayer(
            onCreate = onCreate,
            destroyOnDispose = destroyOnDispose,
            wrapper = wrapper,
            content = content,
        ).also {
            it.SyncAttach(attach)
        }
    }
    return attach
}

@Composable
private fun Layer.SyncAttach(state: MutableState<Boolean>) {
    if (state.value) {
        LaunchedEffect(this) {
            onDetach { state.value = false }
            attach()
        }
    }
}