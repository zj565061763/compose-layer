package com.sd.lib.compose.layer

import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectionsTest {

  @Test
  fun singleDirection_hasExpectedFlags() {
    assertFlags(Directions.Top, top = true, bottom = false, start = false, end = false)
    assertFlags(Directions.Bottom, top = false, bottom = true, start = false, end = false)
    assertFlags(Directions.Start, top = false, bottom = false, start = true, end = false)
    assertFlags(Directions.End, top = false, bottom = false, start = false, end = true)
  }

  @Test
  fun plus_combinesFlagsAndPreservesValueEquality() {
    val a = Directions.Top + Directions.Start
    val b = Directions.Start + Directions.Top

    assertEquals(a, b)
    assertEquals(a.hashCode(), b.hashCode())
    assertFlags(a, top = true, bottom = false, start = true, end = false)

    assertSame(Directions.Top, Directions.Top + Directions.Top)
    assertSame(a, a + Directions.Top)
  }

  @Test
  fun hasLeftAndHasRight_swapsStartAndEndInRtl() {
    val combined = Directions.Top + Directions.Start

    assertTrue(combined.hasLeft(LayoutDirection.Ltr))
    assertFalse(combined.hasRight(LayoutDirection.Ltr))

    assertFalse(combined.hasLeft(LayoutDirection.Rtl))
    assertTrue(combined.hasRight(LayoutDirection.Rtl))
  }
}

private fun assertFlags(
  directions: Directions,
  top: Boolean,
  bottom: Boolean,
  start: Boolean,
  end: Boolean,
) {
  if (top) assertTrue(directions.hasTop()) else assertFalse(directions.hasTop())
  if (bottom) assertTrue(directions.hasBottom()) else assertFalse(directions.hasBottom())
  if (start) assertTrue(directions.hasStart()) else assertFalse(directions.hasStart())
  if (end) assertTrue(directions.hasEnd()) else assertFalse(directions.hasEnd())
}
