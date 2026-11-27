package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerLifecycleTest {

  @Test
  fun syncAttachFalse_turnsOffVisibilityAndTargetReadyBeforeSideEffectDetach() {
    val container = newLayerContainer()
    val containerNode = LayerContainerNode(container)
    containerNode.onAttach()
    containerNode.onPlaced(FakeLayoutCoordinates(IntOffset.Zero, IntSize(400, 800)))

    val anchorNode = LayerTagNode("anchor")
    anchorNode.bindContainer(container)
    anchorNode.onPlaced(FakeLayoutCoordinates(IntOffset(40, 100), IntSize(80, 40)))

    val layer = TargetLayerImpl()
    layer.syncAttach(true)
    container.initLayer(layer)
    layer.setTarget(LayerTarget.Tag("anchor"))
    layer.attach()
    layer.updatePlacedContentRect(IntOffset(40, 140), IntSize(120, 60))

    assertTrue(layer.isVisibleState)
    assertTrue(layer.isReady)

    // 组合期先执行 syncAttach(false)，同帧内关闭可见态与 isReady 并保留上一帧放置矩形
    layer.syncAttach(false)
    assertFalse(layer.isVisibleState)
    assertFalse(layer.isReady)
    assertEquals(LayoutRect(IntOffset(40, 140), IntSize(120, 60)), layer.placedContentRect)

    // 提交期 SideEffect 执行 detach() 完成退订与容器移除
    layer.detach()
    assertFalse(container.hasAttachedLayers)
    assertNull(layer.placedContentRect)
  }

  @Test
  fun detachBeforeTargetSwitch_doesNotSubscribeNewTargetUntilReattached() {
    val container = newLayerContainer()
    val containerNode = LayerContainerNode(container)
    containerNode.onAttach()
    containerNode.onPlaced(FakeLayoutCoordinates(IntOffset.Zero, IntSize(400, 800)))

    val nodeA = LayerTagNode("a")
    nodeA.bindContainer(container)
    nodeA.onPlaced(FakeLayoutCoordinates(IntOffset(10, 20), IntSize(50, 30)))

    val nodeB = LayerTagNode("b")
    nodeB.bindContainer(container)
    nodeB.onPlaced(FakeLayoutCoordinates(IntOffset(100, 200), IntSize(60, 40)))

    val layer = TargetLayerImpl()
    layer.syncAttach(true)
    container.initLayer(layer)
    layer.setTarget(LayerTarget.Tag("a"))
    layer.attach()
    assertEquals(LayoutRect(IntOffset(10, 20), IntSize(50, 30)), layer.targetRect)

    // 模拟 attach = false 同帧切换 target：先 detach 再更新 target 不会订阅新目标
    layer.syncAttach(false)
    layer.detach()
    layer.setTarget(LayerTarget.Tag("b"))
    assertNull(layer.targetRect)

    // 再次 attach = true 时订阅新目标并恢复就绪
    layer.syncAttach(true)
    layer.attach()
    assertEquals(LayoutRect(IntOffset(100, 200), IntSize(60, 40)), layer.targetRect)
    assertTrue(layer.isReady)
    assertTrue(layer.isVisibleState)

    layer.release()
    assertFalse(container.hasAttachedLayers)
    assertFalse(layer.isVisibleState)
  }

  @Test
  fun attachDetachAndRelease_pairsCallbacksCleanly() {
    val container = newLayerContainer()
    val layer = RecordingLayer()

    layer.syncAttach(true)
    container.initLayer(layer)
    layer.attach()
    layer.attach()
    assertEquals(1, layer.attachCount)
    assertEquals(0, layer.detachCount)
    assertTrue(layer.isVisibleState)

    layer.syncAttach(false)
    layer.detach()
    layer.detach()
    assertEquals(1, layer.attachCount)
    assertEquals(1, layer.detachCount)
    assertEquals(1, layer.detachedCount)
    assertFalse(container.hasAttachedLayers)

    layer.syncAttach(true)
    layer.attach()
    assertEquals(2, layer.attachCount)
    assertTrue(container.hasAttachedLayers)

    layer.release()
    assertEquals(2, layer.detachCount)
    assertEquals(2, layer.detachedCount)
    assertFalse(container.hasAttachedLayers)
  }
}

private class RecordingLayer : LayerImpl() {
  var attachCount: Int = 0
  var detachCount: Int = 0
  var detachedCount: Int = 0

  override fun onAttach(container: ContainerForLayer) {
    attachCount++
    setContentVisible(true)
  }

  override fun onDetach(container: ContainerForLayer) {
    detachCount++
  }

  override fun onDetached(container: ContainerForLayer) {
    detachedCount++
  }

  @Composable
  override fun getLayerTransition(transition: LayerTransition?): LayerTransition {
    return transition ?: LayerTransition.Fade
  }

  @Composable
  override fun BoxScope.LayerContent() {}
}
