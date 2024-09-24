package com.sd.lib.compose.layer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun LayerDisplayScope.DisplaySlideTopToBottom() {
   DisplayDefault(
      enter = slideInVertically { -it },
      exit = slideOutVertically { -it },
   )
}

@Composable
fun LayerDisplayScope.DisplaySlideBottomToTop() {
   DisplayDefault(
      enter = slideInVertically { it },
      exit = slideOutVertically { it },
   )
}

@Composable
fun LayerDisplayScope.DisplaySlideStartToEnd() {
   when (LocalLayoutDirection.current) {
      LayoutDirection.Ltr -> DisplayDefault(
         enter = slideInHorizontally { -it },
         exit = slideOutHorizontally { -it },
      )
      LayoutDirection.Rtl -> DisplayDefault(
         enter = slideInHorizontally { it },
         exit = slideOutHorizontally { it },
      )
   }
}

@Composable
fun LayerDisplayScope.DisplaySlideEndToStart() {
   when (LocalLayoutDirection.current) {
      LayoutDirection.Ltr -> DisplayDefault(
         enter = slideInHorizontally { it },
         exit = slideOutHorizontally { it },
      )
      LayoutDirection.Rtl -> DisplayDefault(
         enter = slideInHorizontally { -it },
         exit = slideOutHorizontally { -it },
      )
   }
}

@Composable
fun LayerDisplayScope.DisplayDefault(
   enter: EnterTransition = fadeIn(),
   exit: ExitTransition = fadeOut(),
) {
   AnimatedVisibility(
      visible = isVisibleState,
      enter = enter,
      exit = exit,
   ) {
      Content()
   }
}
