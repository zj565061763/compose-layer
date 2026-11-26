package com.sd.lib.compose.layer

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

internal data class FixOverFlow(
  val offset: IntOffset,
  val size: IntSize,
)

/** 在可随尺寸内收的边越界时循环缩减宽高并重算对齐坐标 */
internal fun Aligner.Result.fixOverFlow(
  layer: Layer? = null,
  layoutDirection: LayoutDirection,
): FixOverFlow {
  val isLtr = layoutDirection == LayoutDirection.Ltr
  val resolvedPosition = input.position.takeIf { isLtr } ?: input.position.rtl()

  val isCenterHorizontal = resolvedPosition.isCenterHorizontal()
  val canShrinkStart = isCenterHorizontal || resolvedPosition.isEndAnchoredHorizontal()
  val canShrinkEnd = isCenterHorizontal || !resolvedPosition.isEndAnchoredHorizontal()

  val isCenterVertical = resolvedPosition.isCenterVertical()
  val canShrinkTop = isCenterVertical || resolvedPosition.isBottomAnchoredVertical()
  val canShrinkBottom = isCenterVertical || !resolvedPosition.isBottomAnchoredVertical()

  var result = this
  var resultWidth = result.input.sourceWidth
  var resultHeight = result.input.sourceHeight

  var count = 0
  while (true) {
    layer?.logMsg {
      "checkOverflow ${++count} ($resultWidth,$resultHeight)"
    }

    var hasOverflow = false

    with(result.sourceOverflow) {
      val overWidth = calculateAxisShrink(
        canShrinkLeading = canShrinkStart,
        leadingOverflow = start,
        canShrinkTrailing = canShrinkEnd,
        trailingOverflow = end,
        isCenter = isCenterHorizontal,
      )
      if (overWidth > 0) {
        hasOverflow = true
        val oldWidth = resultWidth
        resultWidth = oldWidth - overWidth
        layer?.logMsg {
          val startLog = if (canShrinkStart && (start > 0)) " start:$start" else ""
          val endLog = if (canShrinkEnd && (end > 0)) " end:$end" else ""
          "width overflow:${overWidth}${startLog}${endLog} ($oldWidth)->($resultWidth)"
        }
      }

      val overHeight = calculateAxisShrink(
        canShrinkLeading = canShrinkTop,
        leadingOverflow = top,
        canShrinkTrailing = canShrinkBottom,
        trailingOverflow = bottom,
        isCenter = isCenterVertical,
      )
      if (overHeight > 0) {
        hasOverflow = true
        val oldHeight = resultHeight
        resultHeight = oldHeight - overHeight
        layer?.logMsg {
          val topLog = if (canShrinkTop && (top > 0)) " top:$top" else ""
          val bottomLog = if (canShrinkBottom && (bottom > 0)) " bottom:$bottom" else ""
          "height overflow:${overHeight}${topLog}${bottomLog} ($oldHeight)->($resultHeight)"
        }
      }
    }

    if (!hasOverflow || (resultWidth <= 0) || (resultHeight <= 0)) {
      break
    }
    result = result.input.copy(
      sourceWidth = resultWidth,
      sourceHeight = resultHeight,
    ).toResult(isLtr)
  }

  return FixOverFlow(
    offset = IntOffset(result.x, result.y),
    size = IntSize(resultWidth.coerceAtLeast(0), resultHeight.coerceAtLeast(0)),
  )
}

/** 计算单轴需要内收缩减的溢出尺寸 */
private fun calculateAxisShrink(
  canShrinkLeading: Boolean,
  leadingOverflow: Int,
  canShrinkTrailing: Boolean,
  trailingOverflow: Int,
  isCenter: Boolean,
): Int {
  val isLeadingOverflow = canShrinkLeading && (leadingOverflow > 0)
  val isTrailingOverflow = canShrinkTrailing && (trailingOverflow > 0)

  var overSize = 0
  if (isLeadingOverflow) overSize += leadingOverflow
  if (isTrailingOverflow) overSize += trailingOverflow

  /** 居中对齐且仅单侧溢出时，按双倍溢出量缩减以维持居中对齐关系 */
  if ((overSize > 0) && isCenter && !(isLeadingOverflow && isTrailingOverflow)) {
    overSize *= 2
  }
  return overSize
}

private fun Aligner.Position.isCenterHorizontal(): Boolean {
  return when (this) {
    Aligner.Position.TopCenter,
    Aligner.Position.BottomCenter,
    Aligner.Position.Center,
      -> true
    else -> false
  }
}

private fun Aligner.Position.isEndAnchoredHorizontal(): Boolean {
  return when (this) {
    Aligner.Position.TopEnd,
    Aligner.Position.BottomEnd,
    Aligner.Position.StartTop,
    Aligner.Position.StartCenter,
    Aligner.Position.StartBottom,
    Aligner.Position.Start,
      -> true
    else -> false
  }
}

private fun Aligner.Position.isCenterVertical(): Boolean {
  return when (this) {
    Aligner.Position.StartCenter,
    Aligner.Position.EndCenter,
    Aligner.Position.Center,
      -> true
    else -> false
  }
}

private fun Aligner.Position.isBottomAnchoredVertical(): Boolean {
  return when (this) {
    Aligner.Position.TopStart,
    Aligner.Position.TopCenter,
    Aligner.Position.TopEnd,
    Aligner.Position.Top,
    Aligner.Position.StartBottom,
    Aligner.Position.EndBottom,
      -> true
    else -> false
  }
}
