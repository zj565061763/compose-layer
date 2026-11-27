package com.sd.lib.compose.layer

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class TargetLayerLayoutTest {

  @Test
  fun calculateMaxAllowedSize_matchesFixOverflowAcrossAlignments() {
    val containerRect = LayoutRect(
      offset = IntOffset(0, 0),
      size = IntSize(300, 400),
    )

    val bottomStartMax = calculateMaxAllowedSize(
      targetRect = LayoutRect(offset = IntOffset(220, 300), size = IntSize(40, 40)),
      containerRect = containerRect,
      alignment = TargetAlignment.BottomStart,
      alignmentOffsetX = null,
      alignmentOffsetY = null,
      maxContainerSize = containerRect.size,
      density = 1f,
      layoutDirection = LayoutDirection.Ltr,
    )
    assertEquals(IntSize(80, 60), bottomStartMax)

    val topEndMax = calculateMaxAllowedSize(
      targetRect = LayoutRect(offset = IntOffset(50, 60), size = IntSize(40, 40)),
      containerRect = containerRect,
      alignment = TargetAlignment.TopEnd,
      alignmentOffsetX = null,
      alignmentOffsetY = null,
      maxContainerSize = containerRect.size,
      density = 1f,
      layoutDirection = LayoutDirection.Ltr,
    )
    assertEquals(IntSize(90, 60), topEndMax)

    val bottomCenterMax = calculateMaxAllowedSize(
      targetRect = LayoutRect(offset = IntOffset(230, 100), size = IntSize(40, 40)),
      containerRect = containerRect,
      alignment = TargetAlignment.BottomCenter,
      alignmentOffsetX = null,
      alignmentOffsetY = null,
      maxContainerSize = containerRect.size,
      density = 1f,
      layoutDirection = LayoutDirection.Ltr,
    )
    assertEquals(IntSize(100, 260), bottomCenterMax)

    val centerMax = calculateMaxAllowedSize(
      targetRect = LayoutRect(offset = IntOffset(100, 150), size = IntSize(40, 40)),
      containerRect = containerRect,
      alignment = TargetAlignment.Center,
      alignmentOffsetX = null,
      alignmentOffsetY = null,
      maxContainerSize = containerRect.size,
      density = 1f,
      layoutDirection = LayoutDirection.Ltr,
    )
    assertEquals(IntSize(240, 340), centerMax)

    val outsideFixedEdgeMax = calculateMaxAllowedSize(
      targetRect = LayoutRect(offset = IntOffset(-20, 100), size = IntSize(40, 40)),
      containerRect = containerRect,
      alignment = TargetAlignment.BottomStart,
      alignmentOffsetX = null,
      alignmentOffsetY = null,
      maxContainerSize = containerRect.size,
      density = 1f,
      layoutDirection = LayoutDirection.Ltr,
    )
    assertEquals(IntSize(300, 260), outsideFixedEdgeMax)
  }

  @Test
  fun calculateClipBackgroundBounds_nullOrEmptyContent_returnsFullContainer() {
    val containerSize = IntSize(300, 400)
    assertEquals(
      IntRect(0, 0, 300, 400),
      calculateClipBackgroundBounds(
        containerSize = containerSize,
        clipBackgroundDirection = null,
        contentOffset = IntOffset(50, 80),
        contentSize = IntSize(100, 120),
        layoutDirection = LayoutDirection.Ltr,
      ),
    )
    assertEquals(
      IntRect(0, 0, 300, 400),
      calculateClipBackgroundBounds(
        containerSize = containerSize,
        clipBackgroundDirection = Directions.Top,
        contentOffset = IntOffset(50, 80),
        contentSize = IntSize.Zero,
        layoutDirection = LayoutDirection.Ltr,
      ),
    )
  }

  @Test
  fun calculateClipBackgroundBounds_singleAndCombinedDirections_respectsLtrAndRtl() {
    val containerSize = IntSize(300, 400)
    val contentOffset = IntOffset(40, 80)
    val contentSize = IntSize(100, 120)

    assertEquals(
      IntRect(0, 80, 300, 400),
      calculateClipBackgroundBounds(
        containerSize = containerSize,
        clipBackgroundDirection = Directions.Top,
        contentOffset = contentOffset,
        contentSize = contentSize,
        layoutDirection = LayoutDirection.Ltr,
      ),
    )

    assertEquals(
      IntRect(40, 0, 300, 200),
      calculateClipBackgroundBounds(
        containerSize = containerSize,
        clipBackgroundDirection = Directions.Start + Directions.Bottom,
        contentOffset = contentOffset,
        contentSize = contentSize,
        layoutDirection = LayoutDirection.Ltr,
      ),
    )

    assertEquals(
      IntRect(0, 0, 140, 200),
      calculateClipBackgroundBounds(
        containerSize = containerSize,
        clipBackgroundDirection = Directions.Start + Directions.Bottom,
        contentOffset = contentOffset,
        contentSize = contentSize,
        layoutDirection = LayoutDirection.Rtl,
      ),
    )
  }

  @Test
  fun calculateClipBackgroundBounds_outOfBoundsContent_clampsToContainer() {
    val containerSize = IntSize(300, 400)
    assertEquals(
      IntRect(0, 0, 300, 380),
      calculateClipBackgroundBounds(
        containerSize = containerSize,
        clipBackgroundDirection = Directions.Start + Directions.Top + Directions.End + Directions.Bottom,
        contentOffset = IntOffset(-30, -20),
        contentSize = IntSize(400, 400),
        layoutDirection = LayoutDirection.Ltr,
      ),
    )
  }

  @Test
  fun planSmartTargetLayout_horizontalShrink_preservesFullVerticalAllowanceAndRealignsWrappedContent() {
    val containerRect = LayoutRect(
      offset = IntOffset(0, 0),
      size = IntSize(300, 400),
    )
    val targetRect = LayoutRect(
      offset = IntOffset(10, 250),
      size = IntSize(60, 40),
    )
    // 单行自然尺寸 200x40 在默认与候选方位均水平越界 130，保留默认 TopEnd
    val plan = planSmartTargetLayout(
      smartAlignments = SmartAliments(SmartAliment(TargetAlignment.BottomEnd)),
      targetRect = targetRect,
      containerRect = containerRect,
      defaultAlignment = TargetAlignment.TopEnd,
      alignmentOffsetX = null,
      alignmentOffsetY = null,
      rawSize = IntSize(200, 40),
      maxContainerSize = containerRect.size,
      density = 1f,
      layoutDirection = LayoutDirection.Ltr,
    )

    assertEquals(TargetAlignment.TopEnd, plan.effectiveAlignment)
    assertEquals(true, plan.needsShrink)
    // 水平缩窄至目标右边缘 70，垂直保留目标上方完整可用高度 250（不锁死在单行高度 40）
    assertEquals(IntSize(70, 250), plan.maxAllowedSize)

    // 宽度受限后文本折行测得实际尺寸 68x90，按实际尺寸重算对齐坐标后右边缘对齐 70、底边对齐 250
    val wrappedFixed = resolveAlignedTargetLayout(
      targetRect = targetRect,
      containerRect = containerRect,
      alignment = plan.effectiveAlignment,
      alignmentOffsetX = null,
      alignmentOffsetY = null,
      contentSize = IntSize(68, 90),
      density = 1f,
      layoutDirection = LayoutDirection.Ltr,
    )
    assertEquals(IntOffset(2, 160), wrappedFixed.offset)
    assertEquals(IntSize(68, 90), wrappedFixed.size)
  }

  @Test
  fun resolveSmartApproachOffset_noShrinkDuringSizeTransition_realignsToTargetUntilSettled() {
    val containerRect = LayoutRect(
      offset = IntOffset(0, 0),
      size = IntSize(300, 400),
    )
    val targetRect = LayoutRect(
      offset = IntOffset(120, 260),
      size = IntSize(60, 40),
    )
    val rawSize = IntSize(160, 180)
    val plan = planSmartTargetLayout(
      smartAlignments = SmartAliments.Default,
      targetRect = targetRect,
      containerRect = containerRect,
      defaultAlignment = TargetAlignment.TopCenter,
      alignmentOffsetX = null,
      alignmentOffsetY = null,
      rawSize = rawSize,
      maxContainerSize = containerRect.size,
      density = 1f,
      layoutDirection = LayoutDirection.Ltr,
    )

    assertEquals(false, plan.needsShrink)
    assertEquals(IntOffset(70, 80), plan.offset)

    // 未触发溢出裁剪但处于尺寸过渡动画期间（如高度从 80 展开到 180），按当前尺寸贴边重算对齐坐标
    val animatingOffset = resolveSmartApproachOffset(
      needsShrink = plan.needsShrink,
      rawSize = rawSize,
      fixOffset = plan.offset,
      targetRect = targetRect,
      containerRect = containerRect,
      alignment = plan.effectiveAlignment,
      alignmentOffsetX = null,
      alignmentOffsetY = null,
      approachSize = IntSize(160, 80),
      density = 1f,
      layoutDirection = LayoutDirection.Ltr,
    )
    assertEquals(IntOffset(70, 180), animatingOffset)

    // 尺寸过渡结束回到自然尺寸时，直接复用预计算的对齐坐标
    val settledOffset = resolveSmartApproachOffset(
      needsShrink = plan.needsShrink,
      rawSize = rawSize,
      fixOffset = plan.offset,
      targetRect = targetRect,
      containerRect = containerRect,
      alignment = plan.effectiveAlignment,
      alignmentOffsetX = null,
      alignmentOffsetY = null,
      approachSize = rawSize,
      density = 1f,
      layoutDirection = LayoutDirection.Ltr,
    )
    assertEquals(plan.offset, settledOffset)
  }
}
