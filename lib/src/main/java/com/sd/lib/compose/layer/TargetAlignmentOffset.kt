package com.sd.lib.compose.layer

import androidx.compose.runtime.Immutable

/**
 * 目标对齐位置偏移量
 */
@Immutable
sealed class TargetAlignmentOffset {
   /** 按指定像素[value]偏移，支持正数和负数，以Y轴为例，大于0往下偏移，小于0往上偏移 */
   data class PX(val value: Int) : TargetAlignmentOffset()

   /** 按目标大小倍数[value]偏移，支持正数和负数字，以Y轴为例，1表示往下偏移1倍目标的高度，-1表示往上偏移1倍目标的高度 */
   data class Target(val value: Float) : TargetAlignmentOffset()
}

/**
 * 把当前偏移量改为相对[TargetAlignment]偏移量
 */
fun TargetAlignmentOffset.relativeAlignment(): TargetAlignmentOffset {
   return if (this is RelativeAlignment) this
   else RelativeAlignment(this)
}

internal fun TargetAlignmentOffset?.pxValue(
   targetSize: Int,
   alignment: TargetAlignment,
   xy: Boolean,
): Int {
   return when (val offset = this) {
      is RelativeAlignment -> offset.toPx(targetSize, alignment, xy)
      else -> toPx(targetSize)
   }
}

private data class RelativeAlignment(
   val raw: TargetAlignmentOffset,
) : TargetAlignmentOffset() {
   fun toPx(
      targetSize: Int,
      alignment: TargetAlignment,
      xy: Boolean,
   ): Int {
      val px = raw.toPx(targetSize)
      if (px == 0) return 0
      return if (xy) alignment.handleX(px) else alignment.handleY(px)
   }

   private fun TargetAlignment.handleX(px: Int): Int {
      return when (this) {
         TargetAlignment.TopEnd,
         TargetAlignment.BottomEnd,
         TargetAlignment.StartTop,
         TargetAlignment.StartCenter,
         TargetAlignment.StartBottom,
         TargetAlignment.Start,
         -> -px
         else -> px
      }
   }

   private fun TargetAlignment.handleY(px: Int): Int {
      return when (this) {
         TargetAlignment.TopStart,
         TargetAlignment.TopCenter,
         TargetAlignment.TopEnd,
         TargetAlignment.Top,
         TargetAlignment.StartBottom,
         TargetAlignment.EndBottom,
         -> -px
         else -> px
      }
   }
}

private fun TargetAlignmentOffset?.toPx(targetSize: Int): Int {
   return when (val offset = this) {
      null -> 0
      is TargetAlignmentOffset.PX -> offset.value
      is TargetAlignmentOffset.Target -> (offset.value * targetSize).toInt()
      else -> error("Unsupported")
   }
}