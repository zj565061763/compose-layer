package com.sd.lib.compose.layer

import androidx.compose.animation.*
import androidx.compose.runtime.Composable

@Composable
fun LayerContentWrapperScope.LayerAnimatedSlideUpDown() {
    LayerAnimatedDefault(
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    )
}

@Composable
fun LayerContentWrapperScope.LayerAnimatedSlideDownUp() {
    LayerAnimatedDefault(
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
    )
}

@Composable
fun LayerContentWrapperScope.LayerAnimatedDefault(
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
