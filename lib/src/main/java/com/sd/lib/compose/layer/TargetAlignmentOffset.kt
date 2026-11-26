package com.sd.lib.compose.layer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.util.fastRoundToInt

/** 目标对齐位置偏移量 */
@Immutable
sealed interface TargetAlignmentOffset {
  /** 按指定像素[value]偏移，支持正数和负数，以Y轴为例，大于0往下偏移，小于0往上偏移 */
  data class PX(val value: Int) : TargetAlignmentOffset

  /** 按指定DP[value]偏移，支持正数和负数，以Y轴为例，大于0往下偏移，小于0往上偏移 */
  data class DP(val value: Int) : TargetAlignmentOffset

  /** 按目标大小倍数[value]偏移，支持正数和负数，以Y轴为例，1表示往下偏移1倍目标的高度，-1表示往上偏移1倍目标的高度 */
  data class Target(val value: Float) : TargetAlignmentOffset
}

/**
 * 把当前偏移量改为相对[TargetAlignment]偏移量
 *
 * @param downgrade 当[TargetAlignment]无法参考时，是否降级为参考普通坐标轴
 */
fun TargetAlignmentOffset.relativeAlignment(
  downgrade: Boolean = false,
): TargetAlignmentOffset {
  return if (this is RelativeAlignment) {
    this.copy(downgrade = downgrade)
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
    null -> 0
    is TargetAlignmentOffset.PX -> offset.value
    is TargetAlignmentOffset.DP -> (offset.value * density).fastRoundToInt()
    is TargetAlignmentOffset.Target -> (offset.value * targetSize).fastRoundToInt()
    is RelativeAlignment -> {
      val px = offset.raw.pxValue(density, targetSize, alignment, xy)
      when {
        px == 0 -> 0
        xy -> alignment.handleX(px, offset.downgrade)
        else -> alignment.handleY(px, offset.downgrade)
      }
    }
  }
}

private data class RelativeAlignment(
  val raw: TargetAlignmentOffset,
  val downgrade: Boolean,
) : TargetAlignmentOffset

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