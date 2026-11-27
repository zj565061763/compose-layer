package com.sd.lib.compose.layer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class LayerTagTest {

  @Test
  fun attachUnplacedNodeWhileOldNodePlaced_switchesCleanlyOnceOldDetachesAndNewPlaces() {
    val container = newLayerContainer()
    var latestRect: LayoutRect? = null
    container.registerTargetLayoutCallback("active") { rect ->
      latestRect = rect
    }

    val lowerNode = LayerTagNode("active")
    lowerNode.bindContainer(container)
    lowerNode.onPlaced(FakeLayoutCoordinates(IntOffset(20, 200), IntSize(80, 40)))
    assertEquals(LayoutRect(IntOffset(20, 200), IntSize(80, 40)), latestRect)

    // 模拟从下方组件切回上方组件：上方新节点先 attach（尚未 placed），此时下方旧节点仍处于 placed 状态
    val upperNode = LayerTagNode("active")
    upperNode.bindContainer(container)
    // 未放置节点不抢占当前已放置节点
    assertEquals(LayoutRect(IntOffset(20, 200), IntSize(80, 40)), latestRect)

    // 同轮重组中下方旧节点随后 detach
    lowerNode.onDetach()
    assertNull(latestRect)

    // 布局阶段上方新节点完成放置并接管目标矩形上报
    upperNode.onPlaced(FakeLayoutCoordinates(IntOffset(20, 40), IntSize(80, 40)))
    assertEquals(LayoutRect(IntOffset(20, 40), IntSize(80, 40)), latestRect)
  }

  @Test
  fun twoPlacedNodesWithSameTag_throwsIllegalStateException() {
    val container = newLayerContainer()

    val firstNode = LayerTagNode("dup")
    firstNode.bindContainer(container)
    firstNode.onPlaced(FakeLayoutCoordinates(IntOffset(10, 10), IntSize(50, 50)))

    val secondNode = LayerTagNode("dup")
    secondNode.bindContainer(container)

    assertThrows(IllegalStateException::class.java) {
      secondNode.onPlaced(FakeLayoutCoordinates(IntOffset(100, 100), IntSize(50, 50)))
    }
  }
}

private class FakeLayoutCoordinates(
  private val windowOffset: IntOffset,
  override val size: IntSize,
  override val isAttached: Boolean = true,
) : LayoutCoordinates {
  override val providedAlignmentLines: Set<AlignmentLine> = emptySet()
  override val parentLayoutCoordinates: LayoutCoordinates? = null
  override val parentCoordinates: LayoutCoordinates? = null

  override fun windowToLocal(relativeToWindow: Offset): Offset {
    return Offset(
      x = relativeToWindow.x - windowOffset.x,
      y = relativeToWindow.y - windowOffset.y,
    )
  }

  override fun localToWindow(relativeToLocal: Offset): Offset {
    return Offset(
      x = windowOffset.x + relativeToLocal.x,
      y = windowOffset.y + relativeToLocal.y,
    )
  }

  override fun localToRoot(relativeToLocal: Offset): Offset {
    return localToWindow(relativeToLocal)
  }

  override fun localPositionOf(sourceCoordinates: LayoutCoordinates, relativeToSource: Offset): Offset {
    return windowToLocal(sourceCoordinates.localToWindow(relativeToSource))
  }

  override fun localBoundingBoxOf(sourceCoordinates: LayoutCoordinates, clipBounds: Boolean): Rect {
    val topLeft = localPositionOf(sourceCoordinates, Offset.Zero)
    return Rect(
      left = topLeft.x,
      top = topLeft.y,
      right = topLeft.x + sourceCoordinates.size.width,
      bottom = topLeft.y + sourceCoordinates.size.height,
    )
  }

  override fun get(alignmentLine: AlignmentLine): Int {
    return AlignmentLine.Unspecified
  }
}
