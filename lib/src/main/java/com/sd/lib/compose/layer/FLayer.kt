package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * 用来存放layer的容器
 */
@Composable
fun FLayerContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val layerManager = remember { FLayerManager() }
    CompositionLocalProvider(LocalFLayerManager provides layerManager) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            content()
            layerManager.Content()
        }
    }
}

/**
 * 创建layer，对齐容器[FLayerContainer]
 */
@Composable
fun FLayer(content: @Composable (FLayerState) -> Unit) {
    val layerManager = checkNotNull(LocalFLayerManager.current) {
        "CompositionLocal LocalFLayerManager not present"
    }
    layerManager.layer(false) {
        content(it)
    }
}

/**
 * 创建layer，对齐目标
 */
fun Modifier.fLayer(
    content: @Composable (FLayerState) -> Unit,
) = composed {
    val layerManager = checkNotNull(LocalFLayerManager.current) {
        "CompositionLocal LocalFLayerManager not present"
    }
    var targetLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    layerManager.layer(true) {
        it.targetLayoutCoordinates = targetLayoutCoordinates
        content(it)
    }
    this.onGloballyPositioned {
        targetLayoutCoordinates = it
    }
}