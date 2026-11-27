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
}
