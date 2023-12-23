package com.sd.lib.compose.layer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable

internal val DefaultDisplay: @Composable LayerDisplayScope.() -> Unit = { DisplayDefault() }

@Composable
fun LayerDisplayScope.DisplaySlideUpDown() {
    DisplayDefault(
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    )
}

@Composable
fun LayerDisplayScope.DisplaySlideDownUp() {
    DisplayDefault(
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
    )
}

@Composable
fun LayerDisplayScope.DisplayDefault(
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
