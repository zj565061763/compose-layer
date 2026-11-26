package com.sd.lib.compose.layer

import org.junit.Assert.assertEquals
import org.junit.Test

class TargetAlignmentOffsetTest {

  @Test
  fun pxValue_basicOffsets_convertsCorrectly() {
    val nullOffset: TargetAlignmentOffset? = null
    assertEquals(0, nullOffset.pxValue(density = 2f, targetSize = 100, alignment = TargetAlignment.Center, xy = true))

    assertEquals(24, TargetAlignmentOffset.PX(24).pxValue(density = 2f, targetSize = 100, alignment = TargetAlignment.Center, xy = true))
    assertEquals(-16, TargetAlignmentOffset.PX(-16).pxValue(density = 2f, targetSize = 100, alignment = TargetAlignment.Center, xy = false))

    assertEquals(25, TargetAlignmentOffset.DP(10).pxValue(density = 2.5f, targetSize = 100, alignment = TargetAlignment.Center, xy = true))
    assertEquals(-25, TargetAlignmentOffset.DP(-10).pxValue(density = 2.5f, targetSize = 100, alignment = TargetAlignment.Center, xy = false))

    assertEquals(40, TargetAlignmentOffset.Target(0.5f).pxValue(density = 2f, targetSize = 80, alignment = TargetAlignment.Center, xy = true))
    assertEquals(-120, TargetAlignmentOffset.Target(-1.5f).pxValue(density = 2f, targetSize = 80, alignment = TargetAlignment.Center, xy = false))
  }

  @Test
  fun relativeAlignment_xAxis_appliesDirectionAndDowngrade() {
    val noDowngrade = TargetAlignmentOffset.PX(10).relativeAlignment(downgrade = false)
    val withDowngrade = TargetAlignmentOffset.PX(10).relativeAlignment(downgrade = true)

    val negativeAlignments = listOf(
      TargetAlignment.TopEnd,
      TargetAlignment.BottomEnd,
      TargetAlignment.StartTop,
      TargetAlignment.StartCenter,
      TargetAlignment.StartBottom,
      TargetAlignment.Start,
    )
    for (alignment in negativeAlignments) {
      assertEquals(-10, noDowngrade.pxValue(density = 1f, targetSize = 50, alignment = alignment, xy = true))
      assertEquals(-10, withDowngrade.pxValue(density = 1f, targetSize = 50, alignment = alignment, xy = true))
    }

    val centerAlignments = listOf(
      TargetAlignment.TopCenter,
      TargetAlignment.BottomCenter,
      TargetAlignment.Center,
    )
    for (alignment in centerAlignments) {
      assertEquals(0, noDowngrade.pxValue(density = 1f, targetSize = 50, alignment = alignment, xy = true))
      assertEquals(10, withDowngrade.pxValue(density = 1f, targetSize = 50, alignment = alignment, xy = true))
    }

    val positiveAlignments = listOf(
      TargetAlignment.TopStart,
      TargetAlignment.BottomStart,
      TargetAlignment.EndTop,
      TargetAlignment.EndCenter,
      TargetAlignment.EndBottom,
      TargetAlignment.End,
      TargetAlignment.Top,
      TargetAlignment.Bottom,
    )
    for (alignment in positiveAlignments) {
      assertEquals(10, noDowngrade.pxValue(density = 1f, targetSize = 50, alignment = alignment, xy = true))
      assertEquals(10, withDowngrade.pxValue(density = 1f, targetSize = 50, alignment = alignment, xy = true))
    }
  }

  @Test
  fun relativeAlignment_yAxis_appliesDirectionAndDowngrade() {
    val noDowngrade = TargetAlignmentOffset.PX(10).relativeAlignment(downgrade = false)
    val withDowngrade = TargetAlignmentOffset.PX(10).relativeAlignment(downgrade = true)

    val negativeAlignments = listOf(
      TargetAlignment.TopStart,
      TargetAlignment.TopCenter,
      TargetAlignment.TopEnd,
      TargetAlignment.Top,
      TargetAlignment.StartBottom,
      TargetAlignment.EndBottom,
    )
    for (alignment in negativeAlignments) {
      assertEquals(-10, noDowngrade.pxValue(density = 1f, targetSize = 50, alignment = alignment, xy = false))
      assertEquals(-10, withDowngrade.pxValue(density = 1f, targetSize = 50, alignment = alignment, xy = false))
    }

    val centerAlignments = listOf(
      TargetAlignment.StartCenter,
      TargetAlignment.EndCenter,
      TargetAlignment.Center,
    )
    for (alignment in centerAlignments) {
      assertEquals(0, noDowngrade.pxValue(density = 1f, targetSize = 50, alignment = alignment, xy = false))
      assertEquals(10, withDowngrade.pxValue(density = 1f, targetSize = 50, alignment = alignment, xy = false))
    }

    val positiveAlignments = listOf(
      TargetAlignment.BottomStart,
      TargetAlignment.BottomCenter,
      TargetAlignment.BottomEnd,
      TargetAlignment.Bottom,
      TargetAlignment.StartTop,
      TargetAlignment.EndTop,
      TargetAlignment.Start,
      TargetAlignment.End,
    )
    for (alignment in positiveAlignments) {
      assertEquals(10, noDowngrade.pxValue(density = 1f, targetSize = 50, alignment = alignment, xy = false))
      assertEquals(10, withDowngrade.pxValue(density = 1f, targetSize = 50, alignment = alignment, xy = false))
    }
  }

  @Test
  fun relativeAlignment_chainedCall_updatesDowngradeWithoutDoubleWrapping() {
    val chained = TargetAlignmentOffset.PX(10)
      .relativeAlignment(downgrade = false)
      .relativeAlignment(downgrade = true)

    assertEquals(-10, chained.pxValue(density = 1f, targetSize = 50, alignment = TargetAlignment.TopEnd, xy = true))
    assertEquals(10, chained.pxValue(density = 1f, targetSize = 50, alignment = TargetAlignment.Center, xy = true))
  }
}
