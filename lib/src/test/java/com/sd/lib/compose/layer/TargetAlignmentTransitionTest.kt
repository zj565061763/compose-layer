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

  @Test
  fun resolveTargetLayerTransition_followsGranularPriorityOrder() {
    val direction = LayoutDirection.Ltr
    val customSmartTransition = LayerTransition.slideTopToBottom()
    val explicitTransition = LayerTransition.Fade
    val smartItemWithCustomTransition = SmartAliment(
      alignment = TargetAlignment.BottomCenter,
      transition = customSmartTransition,
    )
    val smartItemFromDefault = SmartAliments.Default.aliments.first()

    // 1. SmartAliment.transition 非空时优先级最高，覆盖 TargetLayer.transition
    assertEquals(
      customSmartTransition,
      resolveTargetLayerTransition(
        explicitTransition = explicitTransition,
        smartAlignment = smartItemWithCustomTransition,
        defaultAlignment = TargetAlignment.TopCenter,
        isOffsetTarget = false,
        direction = direction,
      ),
    )

    // 2. SmartAliment.transition 为空（如 SmartAliments.Default）时，回退使用 TargetLayer.transition
    assertEquals(
      explicitTransition,
      resolveTargetLayerTransition(
        explicitTransition = explicitTransition,
        smartAlignment = smartItemFromDefault,
        defaultAlignment = TargetAlignment.BottomCenter,
        isOffsetTarget = true,
        direction = direction,
      ),
    )

    // 3. 两者均为空时，按命中的 SmartAliment.alignment 推导默认动画
    assertEquals(
      smartItemFromDefault.alignment.offsetTransition(direction),
      resolveTargetLayerTransition(
        explicitTransition = null,
        smartAlignment = smartItemFromDefault,
        defaultAlignment = TargetAlignment.BottomCenter,
        isOffsetTarget = true,
        direction = direction,
      ),
    )

    // 4. 未命中 SmartAliment 且未指定 transition 时，按默认 alignment 推导动画
    assertEquals(
      TargetAlignment.TopStart.offsetTransition(direction),
      resolveTargetLayerTransition(
        explicitTransition = null,
        smartAlignment = null,
        defaultAlignment = TargetAlignment.TopStart,
        isOffsetTarget = true,
        direction = direction,
      ),
    )
    assertEquals(
      LayerTransition.Fade,
      resolveTargetLayerTransition(
        explicitTransition = null,
        smartAlignment = null,
        defaultAlignment = TargetAlignment.Center,
        isOffsetTarget = false,
        direction = direction,
      ),
    )
  }

  @Test
  fun isNoneLayerTransition_respectsSmartAndExplicitTransitionPriority() {
    val normalLayer = NormalLayerImpl()
    assertEquals(false, normalLayer.isNoneLayerTransition())
    normalLayer.setTransition(LayerTransition.None)
    assertEquals(true, normalLayer.isNoneLayerTransition())

    val targetLayer = TargetLayerImpl()
    assertEquals(false, targetLayer.isNoneLayerTransition())

    targetLayer.setTransition(LayerTransition.None)
    assertEquals(true, targetLayer.isNoneLayerTransition())

    // SmartAliment.transition 为 null 时，回退遵循 TargetLayer.transition (None)
    targetLayer.updateSmartAlignment(SmartAliment(TargetAlignment.BottomStart, transition = null))
    assertEquals(true, targetLayer.isNoneLayerTransition())

    // SmartAliment.transition 指定非 None 动画时，覆盖 TargetLayer.transition (None)
    targetLayer.updateSmartAlignment(SmartAliment(TargetAlignment.BottomStart, transition = LayerTransition.Fade))
    assertEquals(false, targetLayer.isNoneLayerTransition())

    // SmartAliment.transition 指定 None 时，覆盖 TargetLayer.transition (Fade)
    targetLayer.setTransition(LayerTransition.Fade)
    targetLayer.updateSmartAlignment(SmartAliment(TargetAlignment.BottomStart, transition = LayerTransition.None))
    assertEquals(true, targetLayer.isNoneLayerTransition())
  }
}
