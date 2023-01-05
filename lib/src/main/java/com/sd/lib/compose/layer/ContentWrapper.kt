package com.sd.lib.compose.layer

import androidx.compose.animation.*
import androidx.compose.runtime.Composable

@Composable
fun LayerContentWrapperScope.LayerAnimatedSlideUpDown() {
    LayerAnimatedVisibility(
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    )
}

@Composable
fun LayerContentWrapperScope.LayerAnimatedSlideDownUp() {
    LayerAnimatedVisibility(
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
    )
}

@Composable
fun LayerContentWrapperScope.LayerAnimatedVisibility(
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
