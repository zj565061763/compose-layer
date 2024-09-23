package com.sd.lib.compose.layer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal val DefaultDisplay: @Composable LayerDisplayScope.() -> Unit = { DisplayDefault() }

@Composable
fun LayerDisplayScope.DisplaySlideUpDown(
   modifier: Modifier = Modifier,
) {
   DisplayDefault(
      modifier = modifier,
      enter = slideInVertically { it },
      exit = slideOutVertically { it },
   )
}

@Composable
fun LayerDisplayScope.DisplaySlideDownUp(
   modifier: Modifier = Modifier,
) {
   DisplayDefault(
      modifier = modifier,
      enter = slideInVertically { -it },
      exit = slideOutVertically { -it },
   )
}

@Composable
fun LayerDisplayScope.DisplayDefault(
   modifier: Modifier = Modifier,
   enter: EnterTransition = fadeIn(),
   exit: ExitTransition = fadeOut(),
) {
   AnimatedVisibility(
      modifier = modifier,
      visible = layer.isVisibleState,
      enter = enter,
      exit = exit,
   ) {
      Content()
   }
}
