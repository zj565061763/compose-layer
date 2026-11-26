package com.sd.lib.compose.layer

import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class TargetAlignmentTransitionTest {

  @Test
  fun offsetTransition_ltr_anchorsScaleAtCornerTouchingTarget() {
    val direction = LayoutDirection.Ltr

    assertEquals(LayerTransition.scaleBottomLeft(), TargetAlignment.TopStart.offsetTransition(direction))
    assertEquals(LayerTransition.scaleBottomLeft(), TargetAlignment.EndBottom.offsetTransition(direction))

    assertEquals(LayerTransition.scaleBottomRight(), TargetAlignment.TopEnd.offsetTransition(direction))
    assertEquals(LayerTransition.scaleBottomRight(), TargetAlignment.StartBottom.offsetTransition(direction))

    assertEquals(LayerTransition.scaleTopLeft(), TargetAlignment.BottomStart.offsetTransition(direction))
    assertEquals(LayerTransition.scaleTopLeft(), TargetAlignment.EndTop.offsetTransition(direction))

    assertEquals(LayerTransition.scaleTopRight(), TargetAlignment.BottomEnd.offsetTransition(direction))
    assertEquals(LayerTransition.scaleTopRight(), TargetAlignment.StartTop.offsetTransition(direction))

    assertEquals(LayerTransition.Fade, TargetAlignment.Center.offsetTransition(direction))
  }

  @Test
  fun offsetTransition_rtl_mirrorsHorizontalScaleAnchors() {
    val direction = LayoutDirection.Rtl

    assertEquals(LayerTransition.scaleBottomRight(), TargetAlignment.TopStart.offsetTransition(direction))
    assertEquals(LayerTransition.scaleBottomRight(), TargetAlignment.EndBottom.offsetTransition(direction))

    assertEquals(LayerTransition.scaleBottomLeft(), TargetAlignment.TopEnd.offsetTransition(direction))
    assertEquals(LayerTransition.scaleBottomLeft(), TargetAlignment.StartBottom.offsetTransition(direction))

    assertEquals(LayerTransition.scaleTopRight(), TargetAlignment.BottomStart.offsetTransition(direction))
    assertEquals(LayerTransition.scaleTopRight(), TargetAlignment.EndTop.offsetTransition(direction))

    assertEquals(LayerTransition.scaleTopLeft(), TargetAlignment.BottomEnd.offsetTransition(direction))
    assertEquals(LayerTransition.scaleTopLeft(), TargetAlignment.StartTop.offsetTransition(direction))

    assertEquals(LayerTransition.Fade, TargetAlignment.Center.offsetTransition(direction))
  }
}
