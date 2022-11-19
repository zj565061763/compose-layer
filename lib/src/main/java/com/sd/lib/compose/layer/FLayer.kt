package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
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
    CompositionLocalProvider(LocalFLayerManager provides FLayerManager()) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
            LocalFLayerManager.current.Content()
        }
    }
}

/**
 * 返回layer的状态管理对象，用在[FLayer]或者[fLayer]，不能二者共用
 */
@Composable
fun rememberFLayerState(): FLayerState {
    return remember { FLayerState() }
}

/**
 * 创建layer，对齐容器[FLayerContainer]
 */
@Composable
fun FLayer(
    state: FLayerState,
    content: @Composable () -> Unit,
) {
    state.alignTarget = false
    LocalFLayerManager.current.layer(state, content)
}

/**
 * 创建layer，对齐目标
 */
fun Modifier.fLayer(
    state: FLayerState,
    content: @Composable () -> Unit,
) = composed {
    state.alignTarget = true
    LocalFLayerManager.current.layer(state, content)
    this.onGloballyPositioned {
        state.targetLayoutCoordinates = it
    }
}