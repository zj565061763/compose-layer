package com.sd.lib.compose.layer

import androidx.compose.ui.unit.LayoutDirection

/**
 * 目标对齐位置
 */
enum class TargetAlignment {
   /** 顶部开始方向对齐 */
   TopStart,
   /** 顶部中间对齐 */
   TopCenter,
   /** 顶部结束方向对齐 */
   TopEnd,
   /** 顶部对齐，不计算x坐标，默认x坐标为0 */
   Top,

   /** 底部开始方向对齐 */
   BottomStart,
   /** 底部中间对齐 */
   BottomCenter,
   /** 底部结束方向对齐 */
   BottomEnd,
   /** 底部对齐，不计算x坐标，默认x坐标为0 */
   Bottom,

   /** 开始方向顶部对齐 */
   StartTop,
   /** 开始方向中间对齐 */
   StartCenter,
   /** 开始方向底部对齐 */
   StartBottom,
   /** 开始方向对齐，不计算y坐标，默认y坐标为0 */
   Start,

   /** 结束方向顶部对齐 */
   EndTop,
   /** 结束方向中间对齐 */
   EndCenter,
   /** 结束方向底部对齐 */
   EndBottom,
   /** 结束方向对齐，不计算y坐标，默认y坐标为0 */
   End,

   /** 中间对齐 */
   Center,
}

internal fun TargetAlignment.toAlignerPosition(): Aligner.Position {
   return when (this) {
      TargetAlignment.TopStart -> Aligner.Position.TopStart
      TargetAlignment.TopCenter -> Aligner.Position.TopCenter
      TargetAlignment.TopEnd -> Aligner.Position.TopEnd
      TargetAlignment.Top -> Aligner.Position.Top

      TargetAlignment.BottomStart -> Aligner.Position.BottomStart
      TargetAlignment.BottomCenter -> Aligner.Position.BottomCenter
      TargetAlignment.BottomEnd -> Aligner.Position.BottomEnd
      TargetAlignment.Bottom -> Aligner.Position.Bottom

      TargetAlignment.StartTop -> Aligner.Position.StartTop
      TargetAlignment.StartCenter -> Aligner.Position.StartCenter
      TargetAlignment.StartBottom -> Aligner.Position.StartBottom
      TargetAlignment.Start -> Aligner.Position.Start

      TargetAlignment.EndTop -> Aligner.Position.EndTop
      TargetAlignment.EndCenter -> Aligner.Position.EndCenter
      TargetAlignment.EndBottom -> Aligner.Position.EndBottom
      TargetAlignment.End -> Aligner.Position.End

      TargetAlignment.Center -> Aligner.Position.Center
   }
}

internal fun Aligner.Position.toTargetAlignment(): TargetAlignment {
   return when (this) {
      Aligner.Position.TopStart -> TargetAlignment.TopStart
      Aligner.Position.TopCenter -> TargetAlignment.TopCenter
      Aligner.Position.TopEnd -> TargetAlignment.TopEnd
      Aligner.Position.Top -> TargetAlignment.Top

      Aligner.Position.BottomStart -> TargetAlignment.BottomStart
      Aligner.Position.BottomCenter -> TargetAlignment.BottomCenter
      Aligner.Position.BottomEnd -> TargetAlignment.BottomEnd
      Aligner.Position.Bottom -> TargetAlignment.Bottom

      Aligner.Position.StartTop -> TargetAlignment.StartTop
      Aligner.Position.StartCenter -> TargetAlignment.StartCenter
      Aligner.Position.StartBottom -> TargetAlignment.StartBottom
      Aligner.Position.Start -> TargetAlignment.Start

      Aligner.Position.EndTop -> TargetAlignment.EndTop
      Aligner.Position.EndCenter -> TargetAlignment.EndCenter
      Aligner.Position.EndBottom -> TargetAlignment.EndBottom
      Aligner.Position.End -> TargetAlignment.End

      Aligner.Position.Center -> TargetAlignment.Center
   }
}

internal fun TargetAlignment.defaultTransition(
   directions: LayoutDirection = LayoutDirection.Ltr,
): LayerTransition {
   return when (this) {
      TargetAlignment.TopStart,
      TargetAlignment.TopCenter,
      TargetAlignment.TopEnd,
      TargetAlignment.Top,
      -> LayerTransition.SlideBottomToTop

      TargetAlignment.BottomStart,
      TargetAlignment.BottomCenter,
      TargetAlignment.BottomEnd,
      TargetAlignment.Bottom,
      -> LayerTransition.SlideTopToBottom

      TargetAlignment.StartTop,
      TargetAlignment.StartCenter,
      TargetAlignment.StartBottom,
      TargetAlignment.Start,
      -> when (directions) {
         LayoutDirection.Ltr -> LayerTransition.SlideRightToLeft
         LayoutDirection.Rtl -> LayerTransition.SlideLeftToRight
      }

      TargetAlignment.EndTop,
      TargetAlignment.EndCenter,
      TargetAlignment.EndBottom,
      TargetAlignment.End,
      -> when (directions) {
         LayoutDirection.Ltr -> LayerTransition.SlideLeftToRight
         LayoutDirection.Rtl -> LayerTransition.SlideRightToLeft
      }

      else -> LayerTransition.Default
   }
}