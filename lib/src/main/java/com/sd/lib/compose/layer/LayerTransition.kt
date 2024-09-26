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
      /** 默认淡入淡出动画 */
      val Default = LayerTransition(
         enter = fadeIn(),
         exit = fadeOut(),
      )

      /** 无动画 */
      val None = LayerTransition(
         enter = EnterTransition.None,
         exit = ExitTransition.None,
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

      /** 左上角锚点缩放 */
      fun scaleTopLeft(): LayerTransition {
         val transformOrigin = TransformOrigin(0.0f, 0.0f)
         return LayerTransition(
            enter = scaleIn(scaleAnimationSpec, transformOrigin = transformOrigin),
            exit = scaleOut(scaleAnimationSpec, transformOrigin = transformOrigin),
         )
      }

      /** 右上角锚点缩放 */
      fun scaleTopRight(): LayerTransition {
         val transformOrigin = TransformOrigin(1.0f, 0.0f)
         return LayerTransition(
            enter = scaleIn(scaleAnimationSpec, transformOrigin = transformOrigin),
            exit = scaleOut(scaleAnimationSpec, transformOrigin = transformOrigin),
         )
      }

      /** 左下角锚点缩放 */
      fun scaleBottomLeft(): LayerTransition {
         val transformOrigin = TransformOrigin(0.0f, 1.0f)
         return LayerTransition(
            enter = scaleIn(scaleAnimationSpec, transformOrigin = transformOrigin),
            exit = scaleOut(scaleAnimationSpec, transformOrigin = transformOrigin),
         )
      }

      /** 右下角锚点缩放 */
      fun scaleBottomRight(): LayerTransition {
         val transformOrigin = TransformOrigin(1.0f, 1.0f)
         return LayerTransition(
            enter = scaleIn(scaleAnimationSpec, transformOrigin = transformOrigin),
            exit = scaleOut(scaleAnimationSpec, transformOrigin = transformOrigin),
         )
      }

      inline fun scaleTopStart(
         direction: LayoutDirection,
         ltrBuilder: () -> LayerTransition = { scaleTopLeft() },
         rtlBuilder: () -> LayerTransition = { scaleTopRight() },
      ): LayerTransition {
         return layoutDirectionTransition(direction, ltrBuilder, rtlBuilder)
      }

      inline fun scaleTopEnd(
         direction: LayoutDirection,
         ltrBuilder: () -> LayerTransition = { scaleTopRight() },
         rtlBuilder: () -> LayerTransition = { scaleTopLeft() },
      ): LayerTransition {
         return layoutDirectionTransition(direction, ltrBuilder, rtlBuilder)
      }

      inline fun scaleBottomStart(
         direction: LayoutDirection,
         ltrBuilder: () -> LayerTransition = { scaleBottomLeft() },
         rtlBuilder: () -> LayerTransition = { scaleBottomRight() },
      ): LayerTransition {
         return layoutDirectionTransition(direction, ltrBuilder, rtlBuilder)
      }

      inline fun scaleBottomEnd(
         direction: LayoutDirection,
         ltrBuilder: () -> LayerTransition = { scaleBottomRight() },
         rtlBuilder: () -> LayerTransition = { scaleBottomLeft() },
      ): LayerTransition {
         return layoutDirectionTransition(direction, ltrBuilder, rtlBuilder)
      }
   }
}

private val slideAnimationSpec: FiniteAnimationSpec<IntOffset>
   get() = tween(200)

private val scaleAnimationSpec: FiniteAnimationSpec<Float>
   get() = tween(200)

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