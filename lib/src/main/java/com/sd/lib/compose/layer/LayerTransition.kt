package com.sd.lib.compose.layer

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.TransformOrigin
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

      /** 从上向下滑动 */
      fun slideTopToBottom(
         enter: EnterTransition = slideInVertically { -it },
         exit: ExitTransition = slideOutVertically { -it },
      ): LayerTransition = LayerTransition(enter, exit)

      /** 从下向上滑动 */
      fun slideBottomToTop(
         enter: EnterTransition = slideInVertically { it },
         exit: ExitTransition = slideOutVertically { it },
      ): LayerTransition = LayerTransition(enter, exit)

      /** 从左向右滑动 */
      fun slideLeftToRight(
         enter: EnterTransition = slideInHorizontally { -it },
         exit: ExitTransition = slideOutHorizontally { -it },
      ): LayerTransition = LayerTransition(enter, exit)

      /** 从右向左滑动 */
      fun slideRightToLeft(
         enter: EnterTransition = slideInHorizontally { it },
         exit: ExitTransition = slideOutHorizontally { it },
      ): LayerTransition = LayerTransition(enter, exit)

      inline fun slideStartToEnd(
         direction: LayoutDirection,
         ltrBuilder: () -> LayerTransition = { slideLeftToRight() },
         rtlBuilder: () -> LayerTransition = { slideRightToLeft() },
      ): LayerTransition {
         return layoutDirectionTransition(direction, ltrBuilder, rtlBuilder)
      }

      inline fun slideEndToStart(
         direction: LayoutDirection,
         ltrBuilder: () -> LayerTransition = { slideRightToLeft() },
         rtlBuilder: () -> LayerTransition = { slideLeftToRight() },
      ): LayerTransition {
         return layoutDirectionTransition(direction, ltrBuilder, rtlBuilder)
      }

      fun scaleTopStart(direction: LayoutDirection): LayerTransition {
         return when (direction) {
            LayoutDirection.Ltr -> scaleTopLeft()
            LayoutDirection.Rtl -> scaleTopRight()
         }
      }

      fun scaleTopEnd(direction: LayoutDirection): LayerTransition {
         return when (direction) {
            LayoutDirection.Ltr -> scaleTopRight()
            LayoutDirection.Rtl -> scaleTopLeft()
         }
      }

      fun scaleBottomStart(direction: LayoutDirection): LayerTransition {
         return when (direction) {
            LayoutDirection.Ltr -> scaleBottomLeft()
            LayoutDirection.Rtl -> scaleBottomRight()
         }
      }

      fun scaleBottomEnd(direction: LayoutDirection): LayerTransition {
         return when (direction) {
            LayoutDirection.Ltr -> scaleBottomRight()
            LayoutDirection.Rtl -> scaleBottomLeft()
         }
      }

      private fun scaleTopLeft(): LayerTransition {
         val transformOrigin = TransformOrigin(0.0f, 0.0f)
         return LayerTransition(
            enter = scaleIn(transformOrigin = transformOrigin),
            exit = scaleOut(transformOrigin = transformOrigin),
         )
      }

      private fun scaleTopRight(): LayerTransition {
         val transformOrigin = TransformOrigin(1.0f, 0.0f)
         return LayerTransition(
            enter = scaleIn(transformOrigin = transformOrigin),
            exit = scaleOut(transformOrigin = transformOrigin),
         )
      }

      private fun scaleBottomLeft(): LayerTransition {
         val transformOrigin = TransformOrigin(0.0f, 1.0f)
         return LayerTransition(
            enter = scaleIn(transformOrigin = transformOrigin),
            exit = scaleOut(transformOrigin = transformOrigin),
         )
      }

      private fun scaleBottomRight(): LayerTransition {
         val transformOrigin = TransformOrigin(1.0f, 1.0f)
         return LayerTransition(
            enter = scaleIn(transformOrigin = transformOrigin),
            exit = scaleOut(transformOrigin = transformOrigin),
         )
      }
   }
}

@PublishedApi
internal inline fun layoutDirectionTransition(
   direction: LayoutDirection,
   ltrBuilder: () -> LayerTransition,
   rtlBuilder: () -> LayerTransition,
): LayerTransition {
   return when (direction) {
      LayoutDirection.Ltr -> ltrBuilder()
      LayoutDirection.Rtl -> rtlBuilder()
   }
}