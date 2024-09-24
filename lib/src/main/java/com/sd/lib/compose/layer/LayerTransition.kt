package com.sd.lib.compose.layer

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.LayoutDirection

@Immutable
data class LayerTransition(
   val enter: EnterTransition,
   val exit: ExitTransition,
) {
   companion object {
      val Default = LayerTransition(
         enter = fadeIn(),
         exit = fadeOut(),
      )

      val SlideTopToBottom = LayerTransition(
         enter = slideInVertically { -it },
         exit = slideOutVertically { -it },
      )
      val SlideBottomToTop = LayerTransition(
         enter = slideInVertically { it },
         exit = slideOutVertically { it },
      )

      val SlideLeftToRight = LayerTransition(
         enter = slideInHorizontally { -it },
         exit = slideOutHorizontally { -it },
      )
      val SlideRightToLeft = LayerTransition(
         enter = slideInHorizontally { it },
         exit = slideOutHorizontally { it },
      )

      fun slideStartToEnd(direction: LayoutDirection): LayerTransition {
         return when (direction) {
            LayoutDirection.Ltr -> SlideLeftToRight
            LayoutDirection.Rtl -> SlideRightToLeft
         }
      }

      fun slideEndToStart(direction: LayoutDirection): LayerTransition {
         return when (direction) {
            LayoutDirection.Ltr -> SlideRightToLeft
            LayoutDirection.Rtl -> SlideLeftToRight
         }
      }
   }
}