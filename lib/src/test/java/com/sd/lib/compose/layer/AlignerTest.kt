package com.sd.lib.compose.layer

import org.junit.Assert.assertEquals
import org.junit.Test

class AlignerTest {

  @Test
  fun toResult_ltr_alignsAll17Positions() {
    assertAligned(Aligner.Position.TopStart, ltr = true, expectedX = 150, expectedY = 190)
    assertAligned(Aligner.Position.TopCenter, ltr = true, expectedX = 130, expectedY = 190)
    assertAligned(Aligner.Position.TopEnd, ltr = true, expectedX = 110, expectedY = 190)
    assertAligned(Aligner.Position.Top, ltr = true, expectedX = 0, expectedY = 190)

    assertAligned(Aligner.Position.BottomStart, ltr = true, expectedX = 150, expectedY = 290)
    assertAligned(Aligner.Position.BottomCenter, ltr = true, expectedX = 130, expectedY = 290)
    assertAligned(Aligner.Position.BottomEnd, ltr = true, expectedX = 110, expectedY = 290)
    assertAligned(Aligner.Position.Bottom, ltr = true, expectedX = 0, expectedY = 290)

    assertAligned(Aligner.Position.StartTop, ltr = true, expectedX = 30, expectedY = 250)
    assertAligned(Aligner.Position.StartCenter, ltr = true, expectedX = 30, expectedY = 240)
    assertAligned(Aligner.Position.StartBottom, ltr = true, expectedX = 30, expectedY = 230)
    assertAligned(Aligner.Position.Start, ltr = true, expectedX = 30, expectedY = 0)

    assertAligned(Aligner.Position.EndTop, ltr = true, expectedX = 230, expectedY = 250)
    assertAligned(Aligner.Position.EndCenter, ltr = true, expectedX = 230, expectedY = 240)
    assertAligned(Aligner.Position.EndBottom, ltr = true, expectedX = 230, expectedY = 230)
    assertAligned(Aligner.Position.End, ltr = true, expectedX = 230, expectedY = 0)

    assertAligned(Aligner.Position.Center, ltr = true, expectedX = 130, expectedY = 240)
  }

  @Test
  fun toResult_rtl_swapsStartAndEndPositions() {
    assertAligned(Aligner.Position.TopStart, ltr = false, expectedX = 110, expectedY = 190)
    assertAligned(Aligner.Position.TopCenter, ltr = false, expectedX = 130, expectedY = 190)
    assertAligned(Aligner.Position.TopEnd, ltr = false, expectedX = 150, expectedY = 190)
    assertAligned(Aligner.Position.Top, ltr = false, expectedX = 0, expectedY = 190)

    assertAligned(Aligner.Position.BottomStart, ltr = false, expectedX = 110, expectedY = 290)
    assertAligned(Aligner.Position.BottomCenter, ltr = false, expectedX = 130, expectedY = 290)
    assertAligned(Aligner.Position.BottomEnd, ltr = false, expectedX = 150, expectedY = 290)
    assertAligned(Aligner.Position.Bottom, ltr = false, expectedX = 0, expectedY = 290)

    assertAligned(Aligner.Position.StartTop, ltr = false, expectedX = 230, expectedY = 250)
    assertAligned(Aligner.Position.StartCenter, ltr = false, expectedX = 230, expectedY = 240)
    assertAligned(Aligner.Position.StartBottom, ltr = false, expectedX = 230, expectedY = 230)
    assertAligned(Aligner.Position.Start, ltr = false, expectedX = 230, expectedY = 0)

    assertAligned(Aligner.Position.EndTop, ltr = false, expectedX = 30, expectedY = 250)
    assertAligned(Aligner.Position.EndCenter, ltr = false, expectedX = 30, expectedY = 240)
    assertAligned(Aligner.Position.EndBottom, ltr = false, expectedX = 30, expectedY = 230)
    assertAligned(Aligner.Position.End, ltr = false, expectedX = 30, expectedY = 0)

    assertAligned(Aligner.Position.Center, ltr = false, expectedX = 130, expectedY = 240)
  }

  @Test
  fun sourceOverflow_insideContainer_returnsZeroTotalOverflow() {
    val result = createSampleInput(Aligner.Position.TopStart).toResult(ltr = true)
    val overflow = result.sourceOverflow
    assertEquals(Aligner.Overflow(start = -150, end = -230, top = -190, bottom = -550), overflow)
    assertEquals(0, overflow.totalOverflow())
  }

  @Test
  fun sourceOverflow_outsideContainer_sumsPositiveOverflowEdges() {
    val result = Aligner.Input(
      position = Aligner.Position.BottomEnd,
      targetX = 10,
      targetY = 260,
      targetWidth = 20,
      targetHeight = 20,
      containerX = 0,
      containerY = 0,
      containerWidth = 200,
      containerHeight = 300,
      sourceWidth = 100,
      sourceHeight = 80,
    ).toResult(ltr = true)

    val overflow = result.sourceOverflow
    assertEquals(Aligner.Overflow(start = 70, end = -170, top = -280, bottom = 60), overflow)
    assertEquals(130, overflow.totalOverflow())
  }
}

private fun assertAligned(
  position: Aligner.Position,
  ltr: Boolean,
  expectedX: Int,
  expectedY: Int,
) {
  val result = createSampleInput(position).toResult(ltr = ltr)
  assertEquals("x mismatch for $position (ltr=$ltr)", expectedX, result.x)
  assertEquals("y mismatch for $position (ltr=$ltr)", expectedY, result.y)
}

private fun createSampleInput(position: Aligner.Position): Aligner.Input {
  return Aligner.Input(
    position = position,
    targetX = 250,
    targetY = 450,
    targetWidth = 80,
    targetHeight = 40,
    containerX = 100,
    containerY = 200,
    containerWidth = 500,
    containerHeight = 800,
    sourceWidth = 120,
    sourceHeight = 60,
  )
}
