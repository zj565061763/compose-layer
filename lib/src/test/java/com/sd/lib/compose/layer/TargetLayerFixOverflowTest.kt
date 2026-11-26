package com.sd.lib.compose.layer

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class TargetLayerFixOverflowTest {

  @Test
  fun fixOverFlow_bottomStart_shrinksEndAndBottomToContainer() {
    val result = Aligner.Input(
      position = Aligner.Position.BottomStart,
      targetX = 220,
      targetY = 300,
      targetWidth = 40,
      targetHeight = 40,
      containerX = 0,
      containerY = 0,
      containerWidth = 300,
      containerHeight = 400,
      sourceWidth = 150,
      sourceHeight = 120,
    ).toResult(ltr = true)

    val fixed = result.fixOverFlow(layoutDirection = LayoutDirection.Ltr)
    assertEquals(IntOffset(220, 340), fixed.offset)
    assertEquals(IntSize(80, 60), fixed.size)
  }

  @Test
  fun fixOverFlow_topEnd_shrinksStartAndTopKeepingAnchor() {
    val result = Aligner.Input(
      position = Aligner.Position.TopEnd,
      targetX = 50,
      targetY = 60,
      targetWidth = 40,
      targetHeight = 40,
      containerX = 0,
      containerY = 0,
      containerWidth = 300,
      containerHeight = 400,
      sourceWidth = 150,
      sourceHeight = 100,
    ).toResult(ltr = true)

    val fixed = result.fixOverFlow(layoutDirection = LayoutDirection.Ltr)
    assertEquals(IntOffset(0, 0), fixed.offset)
    assertEquals(IntSize(90, 60), fixed.size)
  }

  @Test
  fun fixOverFlow_bottomCenterSingleSideOverflow_shrinksDoubleOverflowMaintainingCenter() {
    val result = Aligner.Input(
      position = Aligner.Position.BottomCenter,
      targetX = 230,
      targetY = 100,
      targetWidth = 40,
      targetHeight = 40,
      containerX = 0,
      containerY = 0,
      containerWidth = 300,
      containerHeight = 400,
      sourceWidth = 160,
      sourceHeight = 80,
    ).toResult(ltr = true)

    val fixed = result.fixOverFlow(layoutDirection = LayoutDirection.Ltr)
    assertEquals(IntOffset(200, 140), fixed.offset)
    assertEquals(IntSize(100, 80), fixed.size)
  }

  @Test
  fun fixOverFlow_centerBothSidesOverflow_convergesToCenteredInBounds() {
    val result = Aligner.Input(
      position = Aligner.Position.Center,
      targetX = 100,
      targetY = 150,
      targetWidth = 40,
      targetHeight = 40,
      containerX = 0,
      containerY = 0,
      containerWidth = 300,
      containerHeight = 400,
      sourceWidth = 500,
      sourceHeight = 600,
    ).toResult(ltr = true)

    val fixed = result.fixOverFlow(layoutDirection = LayoutDirection.Ltr)
    assertEquals(IntOffset(0, 0), fixed.offset)
    assertEquals(IntSize(240, 340), fixed.size)
  }

  @Test
  fun fixOverFlow_fixedAnchorEdgeOutsideContainer_doesNotShrinkToZero() {
    val ltrResult = Aligner.Input(
      position = Aligner.Position.BottomStart,
      targetX = -20,
      targetY = 100,
      targetWidth = 40,
      targetHeight = 40,
      containerX = 0,
      containerY = 0,
      containerWidth = 300,
      containerHeight = 400,
      sourceWidth = 100,
      sourceHeight = 80,
    ).toResult(ltr = true)

    val ltrFixed = ltrResult.fixOverFlow(layoutDirection = LayoutDirection.Ltr)
    assertEquals(IntOffset(-20, 140), ltrFixed.offset)
    assertEquals(IntSize(100, 80), ltrFixed.size)

    val rtlResult = Aligner.Input(
      position = Aligner.Position.BottomStart,
      targetX = 280,
      targetY = 100,
      targetWidth = 40,
      targetHeight = 40,
      containerX = 0,
      containerY = 0,
      containerWidth = 300,
      containerHeight = 400,
      sourceWidth = 100,
      sourceHeight = 80,
    ).toResult(ltr = false)

    val rtlFixed = rtlResult.fixOverFlow(layoutDirection = LayoutDirection.Rtl)
    assertEquals(IntOffset(220, 140), rtlFixed.offset)
    assertEquals(IntSize(100, 80), rtlFixed.size)
  }
}
