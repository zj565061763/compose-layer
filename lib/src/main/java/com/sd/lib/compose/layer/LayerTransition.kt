package com.sd.lib.compose.layer

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.IntOffset
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
         enter: EnterTransition = slideInVertically(slideAnimationSpec) { -it },
         exit: ExitTransition = slideOutVertically(slideAnimationSpec) { -it },
      ): LayerTransition = LayerTransition(enter, exit)

      /** 从下向上滑动 */
      fun slideBottomToTop(
         enter: EnterTransition = slideInVertically(slideAnimationSpec) { it },
         exit: ExitTransition = slideOutVertically(slideAnimationSpec) { it },
      ): LayerTransition = LayerTransition(enter, exit)

      /** 从左向右滑动 */
      fun slideLeftToRight(
         enter: EnterTransition = slideInHorizontally(slideAnimationSpec) { -it },
         exit: ExitTransition = slideOutHorizontally(slideAnimationSpec) { -it },
      ): LayerTransition = LayerTransition(enter, exit)

      /** 从右向左滑动 */
      fun slideRightToLeft(
         enter: EnterTransition = slideInHorizontally(slideAnimationSpec) { it },
         exit: ExitTransition = slideOutHorizontally(slideAnimationSpec) { it },
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
            LayoutDirection.Ltr -> ScaleBottomRight
            LayoutDirection.Rtl -> ScaleBottomLeft
         }
      }
   }
}

private val slideAnimationSpec: FiniteAnimationSpec<IntOffset>
   get() = tween(250)

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