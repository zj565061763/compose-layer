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

      val SlideTopToBottom = LayerTransition(
         enter = slideInVertically { -it },
         exit = slideOutVertically { -it },
      )
      val SlideBottomToTop = LayerTransition(
         enter = slideInVertically { it },
         exit = slideOutVertically { it },
      )
      private val SlideLeftToRight = LayerTransition(
         enter = slideInHorizontally { -it },
         exit = slideOutHorizontally { -it },
      )
      private val SlideRightToLeft = LayerTransition(
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

      private val ScaleTopLeft = run {
         val transformOrigin = TransformOrigin(0.1f, 0.1f)
         LayerTransition(
            enter = scaleIn(transformOrigin = transformOrigin),
            exit = scaleOut(transformOrigin = transformOrigin),
         )
      }
      private val ScaleTopRight = run {
         val transformOrigin = TransformOrigin(0.9f, 0.1f)
         LayerTransition(
            enter = scaleIn(transformOrigin = transformOrigin),
            exit = scaleOut(transformOrigin = transformOrigin),
         )
      }
      private val ScaleBottomLeft = run {
         val transformOrigin = TransformOrigin(0.1f, 0.9f)
         LayerTransition(
            enter = scaleIn(transformOrigin = transformOrigin),
            exit = scaleOut(transformOrigin = transformOrigin),
         )
      }
      private val ScaleBottomRight = run {
         val transformOrigin = TransformOrigin(0.9f, 0.9f)
         LayerTransition(
            enter = scaleIn(transformOrigin = transformOrigin),
            exit = scaleOut(transformOrigin = transformOrigin),
         )
      }

      fun scaleTopStart(direction: LayoutDirection): LayerTransition {
         return when (direction) {
            LayoutDirection.Ltr -> ScaleTopLeft
            LayoutDirection.Rtl -> ScaleTopRight
         }
      }

      fun scaleTopEnd(direction: LayoutDirection): LayerTransition {
         return when (direction) {
            LayoutDirection.Ltr -> ScaleTopRight
            LayoutDirection.Rtl -> ScaleTopLeft
         }
      }

      fun scaleBottomStart(direction: LayoutDirection): LayerTransition {
         return when (direction) {
            LayoutDirection.Ltr -> ScaleBottomLeft
            LayoutDirection.Rtl -> ScaleBottomRight
         }
      }

      fun scaleBottomEnd(direction: LayoutDirection): LayerTransition {
         return when (direction) {
            LayoutDirection.Ltr -> ScaleBottomLeft
            LayoutDirection.Rtl -> ScaleBottomRight
         }
      }
   }
}