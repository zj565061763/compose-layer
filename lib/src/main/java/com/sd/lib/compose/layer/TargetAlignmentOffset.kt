package com.sd.lib.compose.layer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastRoundToInt

/**
 * 目标对齐位置偏移量
 */
@Immutable
sealed class TargetAlignmentOffset {
   /** 按指定像素[value]偏移，支持正数和负数，以Y轴为例，大于0往下偏移，小于0往上偏移 */
   data class PX(val value: Int) : TargetAlignmentOffset()

   /** 按指定DP[value]偏移，支持正数和负数，以Y轴为例，大于0往下偏移，小于0往上偏移 */
   data class DP(val value: Int) : TargetAlignmentOffset()

   /** 按目标大小倍数[value]偏移，支持正数和负数字，以Y轴为例，1表示往下偏移1倍目标的高度，-1表示往上偏移1倍目标的高度 */
   data class Target(val value: Float) : TargetAlignmentOffset()
}

/**
 * 把当前偏移量改为相对[TargetAlignment]偏移量
 *
 * @param 当[TargetAlignment]无法参考时，是否降级为参考普通坐标轴
 */
fun TargetAlignmentOffset.relativeAlignment(
   downgrade: Boolean = false,
): TargetAlignmentOffset {
   return if (this is RelativeAlignment) {
      this.takeIf { it.downgrade == downgrade } ?: this.copy(downgrade = downgrade)
   } else {
      RelativeAlignment(this, downgrade)
   }
}

internal fun TargetAlignmentOffset?.pxValue(
   density: Float,
   targetSize: Int,
   alignment: TargetAlignment,
   xy: Boolean,
): Int {
   return when (val offset = this) {
      is RelativeAlignment -> offset.toPx(density, targetSize, alignment, xy)
      else -> toPx(density, targetSize)
   }
}

private data class RelativeAlignment(
   val raw: TargetAlignmentOffset,
   val downgrade: Boolean,
) : TargetAlignmentOffset() {
   fun toPx(
      density: Float,
      targetSize: Int,
      alignment: TargetAlignment,
      xy: Boolean,
   ): Int {
      val px = raw.toPx(density, targetSize)
      if (px == 0) return 0
      return if (xy) alignment.handleX(px, downgrade) else alignment.handleY(px, downgrade)
   }

   private fun TargetAlignment.handleX(px: Int, downgrade: Boolean): Int {
      return when (this) {
         TargetAlignment.TopEnd,
         TargetAlignment.BottomEnd,
         TargetAlignment.StartTop,
         TargetAlignment.StartCenter,
         TargetAlignment.StartBottom,
         TargetAlignment.Start,
         -> -px

         TargetAlignment.TopCenter,
         TargetAlignment.BottomCenter,
         TargetAlignment.Center,
         -> if (downgrade) px else 0

         else -> px
      }
   }

   private fun TargetAlignment.handleY(px: Int, downgrade: Boolean): Int {
      return when (this) {
         TargetAlignment.TopStart,
         TargetAlignment.TopCenter,
         TargetAlignment.TopEnd,
         TargetAlignment.Top,
         TargetAlignment.StartBottom,
         TargetAlignment.EndBottom,
         -> -px

         TargetAlignment.StartCenter,
         TargetAlignment.EndCenter,
         TargetAlignment.Center,
         -> if (downgrade) px else 0

         else -> px
      }
   }
}

private fun TargetAlignmentOffset?.toPx(
   density: Float,
   targetSize: Int,
): Int {
   return when (val offset = this) {
      null -> 0
      is TargetAlignmentOffset.PX -> offset.value
      is TargetAlignmentOffset.DP -> (offset.value * density).safeRoundToInt()
      is TargetAlignmentOffset.Target -> (offset.value * targetSize).safeRoundToInt()
      else -> error("Unsupported")
   }
}

private fun Float.safeRoundToInt(): Int {
   return when {
      isInfinite() -> Int.MAX_VALUE
      isNaN() -> 0
      else -> fastRoundToInt()
   }
}