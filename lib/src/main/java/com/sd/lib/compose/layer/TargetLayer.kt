package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastRoundToInt

/** TargetLayer状态 */
interface TargetLayerState : LayerState

/** 要对齐的目标 */
@Immutable
sealed interface LayerTarget {
  /** 以[tag]标记的元素为目标 */
  data class Tag(val tag: String?) : LayerTarget

  /** 以窗口坐标系（Window）中的[offset]为目标 */
  data class Offset(val offset: IntOffset?) : LayerTarget
}

internal interface TargetLayer : Layer, TargetLayerState {
  /** 要对齐的目标 */
  fun setTarget(target: LayerTarget?)

  /** 对齐目标位置，默认[TargetAlignment.Center] */
  fun setAlignment(alignment: TargetAlignment)

  /** 对齐目标X方向偏移量 */
  fun setAlignmentOffsetX(offset: TargetAlignmentOffset?)

  /** 对齐目标Y方向偏移量 */
  fun setAlignmentOffsetY(offset: TargetAlignmentOffset?)

  /**
   * 智能对齐目标位置，null-关闭智能对齐；非null-开启智能对齐，默认值null。
   * 开启之后，如果默认的[setAlignment]导致内容溢出会使用[alignments]提供的位置按顺序查找溢出最小的位置。
   */
  fun setSmartAlignments(alignments: SmartAliments?)

  /** 裁切背景的方向[Directions] */
  fun setClipBackgroundDirection(direction: Directions?)
}

internal fun TargetLayer.toTargetLayerState(): TargetLayerState {
  return InternalTargetLayerState(this)
}
private class InternalTargetLayerState(layer: TargetLayer) : TargetLayerState by layer

//---------- Impl ----------

internal class TargetLayerImpl : LayerImpl(), TargetLayer {
  private var _target: LayerTarget? = null
  private var _isOffsetTarget by mutableStateOf(false)
  private var _containerRect by mutableStateOf<LayoutRect?>(null)
  private var _targetRect by mutableStateOf<LayoutRect?>(null)

  private var _alignment by mutableStateOf(TargetAlignment.Center)
  private var _alignmentOffsetX by mutableStateOf<TargetAlignmentOffset?>(null)
  private var _alignmentOffsetY by mutableStateOf<TargetAlignmentOffset?>(null)
  private var _smartAlignments by mutableStateOf<SmartAliments?>(null)
  private var _clipBackgroundDirection by mutableStateOf<Directions?>(null)

  private var _currentSmartAlignment by mutableStateOf<SmartAliment?>(null)
  private var _placedContentRect by mutableStateOf<LayoutRect?>(null)

  /** Tag目标窗口矩形 */
  private var _tagTargetRect: LayoutRect? = null
  /** 监听Tag目标矩形变化 */
  private val _tagTargetLayoutCallback: LayoutRectCallback = { rect ->
    _tagTargetRect = rect
    updateTargetRect()
  }

  /** 监听容器矩形变化 */
  private val _containerLayoutCallback: LayoutRectCallback = { rect ->
    if (_containerRect != rect) {
      _containerRect = rect
      syncVisibility()
    }
  }

  /** 绘制期按内容放置矩形裁切背景遮罩 */
  private val _clipBackgroundModifier = Modifier.drawWithContent {
    val clipDirection = _clipBackgroundDirection
    val contentRect = _placedContentRect
    if ((clipDirection == null) || (contentRect == null)) {
      drawContent()
      return@drawWithContent
    }
    val bounds = calculateClipBackgroundBounds(
      containerSize = IntSize(size.width.fastRoundToInt(), size.height.fastRoundToInt()),
      clipBackgroundDirection = clipDirection,
      contentOffset = contentRect.offset,
      contentSize = contentRect.size,
      layoutDirection = layoutDirection,
    )
    if ((bounds.width > 0) && (bounds.height > 0)) {
      clipRect(
        left = bounds.left.toFloat(),
        top = bounds.top.toFloat(),
        right = bounds.right.toFloat(),
        bottom = bounds.bottom.toFloat(),
      ) {
        this@drawWithContent.drawContent()
      }
    }
  }

  internal val isReady: Boolean
    get() {
      return (lifecycleState == LayerLifecycleState.Attached) &&
        (_containerRect != null) &&
        (_targetRect != null)
    }

  internal val containerRect: LayoutRect?
    get() {
      return _containerRect
    }

  internal val targetRect: LayoutRect?
    get() {
      return _targetRect
    }

  internal val alignment: TargetAlignment
    get() {
      return _alignment
    }

  internal val alignmentOffsetX: TargetAlignmentOffset?
    get() {
      return _alignmentOffsetX
    }

  internal val alignmentOffsetY: TargetAlignmentOffset?
    get() {
      return _alignmentOffsetY
    }

  internal val placedContentRect: LayoutRect?
    get() {
      return _placedContentRect
    }

  override fun setTarget(target: LayerTarget?) {
    if (_target == target) return
    logMsg { "setTarget:$target" }

    val oldTarget = _target
    unregisterTarget(oldTarget)

    _target = target
    if (target != null) {
      _isOffsetTarget = target is LayerTarget.Offset
    }

    registerTarget(target)
    updateTargetRect()
  }

  private fun registerTarget(target: LayerTarget?) {
    if ((lifecycleState == LayerLifecycleState.Attached) && (target is LayerTarget.Tag)) {
      layerContainer?.registerTargetLayoutCallback(target.tag, _tagTargetLayoutCallback)
    }
  }

  private fun unregisterTarget(target: LayerTarget?) {
    if (target is LayerTarget.Tag) {
      layerContainer?.unregisterTargetLayoutCallback(target.tag, _tagTargetLayoutCallback)
    }
  }

  override fun setAlignment(alignment: TargetAlignment) {
    if (_alignment != alignment) {
      _alignment = alignment
    }
  }

  override fun setAlignmentOffsetX(offset: TargetAlignmentOffset?) {
    if (_alignmentOffsetX != offset) {
      _alignmentOffsetX = offset
    }
  }

  override fun setAlignmentOffsetY(offset: TargetAlignmentOffset?) {
    if (_alignmentOffsetY != offset) {
      _alignmentOffsetY = offset
    }
  }

  override fun setSmartAlignments(alignments: SmartAliments?) {
    if (_smartAlignments != alignments) {
      _smartAlignments = alignments
      if (alignments == null) {
        _currentSmartAlignment = null
      }
      syncVisibility()
    }
  }

  override fun setClipBackgroundDirection(direction: Directions?) {
    if (_clipBackgroundDirection != direction) {
      _clipBackgroundDirection = direction
    }
  }

  override fun onAttach(container: ContainerForLayer) {
    container.registerContainerLayoutCallback(_containerLayoutCallback)
    registerTarget(_target)
    syncVisibility()
  }

  override fun onDetach(container: ContainerForLayer) {
    container.unregisterContainerLayoutCallback(_containerLayoutCallback)
    unregisterTarget(_target)
  }

  override fun onDetached(container: ContainerForLayer) {
    _currentSmartAlignment = null
    _placedContentRect = null
  }

  /** 更新目标窗口矩形信息并同步可见状态 */
  private fun updateTargetRect() {
    val rect = when (val target = _target) {
      is LayerTarget.Tag -> _tagTargetRect
      is LayerTarget.Offset -> target.offset?.let { offset ->
        LayoutRect(
          offset = offset,
          size = IntSize.Zero,
        )
      }
      else -> null
    }
    if (_targetRect != rect) {
      _targetRect = rect
    }
    syncVisibility()
  }

  private fun shouldProbeSmartAlignment(): Boolean {
    val aliments = _smartAlignments?.aliments ?: return false
    return aliments.isNotEmpty() &&
      !isInspectionMode &&
      isContentExitIdle() &&
      (!hasExplicitTransition || aliments.any { it.transition != null })
  }

  /** 根据容器与目标就绪状态及智能选位预判进度更新可见状态 */
  private fun syncVisibility() {
    if (!isReady) {
      setContentVisible(false)
      return
    }
    if (!shouldProbeSmartAlignment()) {
      setContentVisible(true)
    }
  }

  internal fun updateSmartAlignment(smartAlignment: SmartAliment?) {
    if (_currentSmartAlignment != smartAlignment) {
      _currentSmartAlignment = smartAlignment
    }
  }

  internal fun updatePlacedContentRect(offset: IntOffset, size: IntSize) {
    if ((size.width > 0) && (size.height > 0)) {
      val rect = LayoutRect(offset = offset, size = size)
      if (_placedContentRect != rect) {
        _placedContentRect = rect
      }
    }
  }

  @Composable
  override fun getLayerTransition(transition: LayerTransition?): LayerTransition {
    val direction = LocalLayoutDirection.current
    val smartAlignment = _currentSmartAlignment
    logMsg {
      when {
        smartAlignment?.transition != null -> "getLayerTransition from smartAlignment $smartAlignment"
        transition != null -> "getLayerTransition from params"
        smartAlignment != null -> "getLayerTransition from smartAlignment default $smartAlignment"
        else -> "getLayerTransition from default"
      }
    }
    return resolveTargetLayerTransition(
      explicitTransition = transition,
      smartAlignment = smartAlignment,
      defaultAlignment = _alignment,
      isOffsetTarget = _isOffsetTarget,
      direction = direction,
    )
  }

  @Composable
  override fun BoxScope.LayerContent() {
    val smartAlignments = _smartAlignments
    val needsProbe = isReady && (smartAlignments != null) && shouldProbeSmartAlignment()

    BackgroundBox(modifier = _clipBackgroundModifier)

    if (needsProbe) {
      SmartProbeBox(smartAlignments = smartAlignments)
    } else if (smartAlignments != null) {
      SmartTargetBox(smartAlignments = smartAlignments)
    } else {
      FixedTargetBox()
    }
  }

  /** 首次进入前探测自然尺寸以确定智能选位过渡动画 */
  @Composable
  private fun SmartProbeBox(smartAlignments: SmartAliments) {
    Layout(
      modifier = Modifier.fillMaxSize(),
      content = { Box { RawContent() } },
    ) { measurables, constraints ->
      val cs = constraints.copy(minWidth = 0, minHeight = 0)
      val container = _containerRect
      val target = _targetRect
      if ((container != null) && (target != null)) {
        val rawPlaceable = measurables.first().measure(cs)
        val rawSize = rawPlaceable.intSize()
        val defaultResult = alignTarget(
          targetRect = target,
          containerRect = container,
          alignment = _alignment,
          alignmentOffsetX = _alignmentOffsetX,
          alignmentOffsetY = _alignmentOffsetY,
          contentSize = rawSize,
          density = density,
          layoutDirection = layoutDirection,
        )
        val (_, smartAlignment) = defaultResult.findBestResult(
          layer = this@TargetLayerImpl,
          smartAliments = smartAlignments,
          targetRect = target,
          containerRect = container,
          alignmentOffsetX = _alignmentOffsetX,
          alignmentOffsetY = _alignmentOffsetY,
          contentSize = rawSize,
          density = density,
          layoutDirection = layoutDirection,
        )
        updateSmartAlignment(smartAlignment)
        setContentVisible(true)
      }
      layout(0, 0) {}
    }
  }

  /** 固定对齐单遍布局 */
  @Composable
  private fun FixedTargetBox() {
    Layout(
      modifier = Modifier.fillMaxSize(),
      content = { ContentBox() },
    ) { measurables, constraints ->
      val cs = constraints.copy(minWidth = 0, minHeight = 0)
      val container = _containerRect
      val target = _targetRect
      val ready = (lifecycleState == LayerLifecycleState.Attached) &&
        (container != null) &&
        (target != null)

      if (ready) {
        val currentAlignment = _alignment
        val offsetX = _alignmentOffsetX
        val offsetY = _alignmentOffsetY

        val maxAllowedSize = calculateMaxAllowedSize(
          layer = this@TargetLayerImpl,
          targetRect = target,
          containerRect = container,
          alignment = currentAlignment,
          alignmentOffsetX = offsetX,
          alignmentOffsetY = offsetY,
          maxContainerSize = cs.maxIntSize(),
          density = density,
          layoutDirection = layoutDirection,
        )
        val contentPlaceable = measurables.first().measure(cs.newMax(maxAllowedSize))
        val measuredSize = contentPlaceable.intSize()

        val result = alignTarget(
          targetRect = target,
          containerRect = container,
          alignment = currentAlignment,
          alignmentOffsetX = offsetX,
          alignmentOffsetY = offsetY,
          contentSize = measuredSize,
          density = density,
          layoutDirection = layoutDirection,
        )
        val (fixOffset, fixSize) = result.fixOverFlow(this@TargetLayerImpl, layoutDirection)

        logMsg {
          """
            layoutFixed
               alignment:$currentAlignment
               target:$target
               container:$container
               offset:(${result.x}, ${result.y}) -> $fixOffset
               size:$measuredSize -> $fixSize
          """.trimIndent()
        }

        layout(cs.maxWidth, cs.maxHeight) {
          updatePlacedContentRect(fixOffset, contentPlaceable.intSize())
          contentPlaceable.place(fixOffset)
        }
      } else {
        val lastRect = _placedContentRect
        if (lastRect != null) {
          val contentPlaceable = measurables.first().measure(cs.newMax(lastRect.size))
          layout(cs.maxWidth, cs.maxHeight) {
            contentPlaceable.place(lastRect.offset)
          }
        } else {
          layout(cs.maxWidth, cs.maxHeight) {}
        }
      }
    }
  }

  /** 智能选位单树布局 */
  @Composable
  private fun SmartTargetBox(smartAlignments: SmartAliments) {
    LookaheadScope {
      Layout(
        modifier = Modifier
          .fillMaxSize()
          .smartTargetLayout(
            layer = this@TargetLayerImpl,
            smartAlignments = smartAlignments,
          ),
        content = { ContentBox() },
      ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)
        layout(placeable.width, placeable.height) {
          placeable.place(0, 0)
        }
      }
    }
  }
}

private fun Modifier.smartTargetLayout(
  layer: TargetLayerImpl,
  smartAlignments: SmartAliments,
): Modifier {
  return this then SmartTargetLayoutElement(
    layer = layer,
    smartAlignments = smartAlignments,
  )
}

private data class SmartTargetLayoutElement(
  val layer: TargetLayerImpl,
  val smartAlignments: SmartAliments,
) : ModifierNodeElement<SmartTargetLayoutNode>() {
  override fun create(): SmartTargetLayoutNode {
    return SmartTargetLayoutNode(
      layer = layer,
      smartAlignments = smartAlignments,
    )
  }

  override fun update(node: SmartTargetLayoutNode) {
    node.layer = layer
    node.smartAlignments = smartAlignments
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "smartTargetLayout"
  }
}

/** 基于Lookahead自然尺寸执行智能选位，并在仍溢出时通过Approach裁剪尺寸 */
private class SmartTargetLayoutNode(
  var layer: TargetLayerImpl,
  var smartAlignments: SmartAliments,
) : Modifier.Node(), ApproachLayoutModifierNode {

  private var _fixOffset: IntOffset = IntOffset.Zero
  private var _fixSize: IntSize = IntSize.Zero
  private var _needsShrink: Boolean = false
  private var _readyInPass: Boolean = false

  override fun MeasureScope.measure(
    measurable: Measurable,
    constraints: Constraints,
  ): MeasureResult {
    val cs = constraints.copy(minWidth = 0, minHeight = 0)
    val container = layer.containerRect
    val target = layer.targetRect
    val ready = layer.isReady && (container != null) && (target != null)
    _readyInPass = ready

    if (!ready) {
      val lastRect = layer.placedContentRect
      _fixSize = lastRect?.size ?: IntSize.Zero
      _fixOffset = lastRect?.offset ?: IntOffset.Zero
      _needsShrink = false
      if (lastRect != null) {
        val placeable = measurable.measure(cs.newMax(lastRect.size))
        return layout(cs.maxWidth, cs.maxHeight) {
          placeable.place(lastRect.offset)
        }
      }
      return layout(cs.maxWidth, cs.maxHeight) {}
    }

    val rawPlaceable = measurable.measure(cs)
    val rawSize = rawPlaceable.intSize()

    val offsetX = layer.alignmentOffsetX
    val offsetY = layer.alignmentOffsetY

    val defaultResult = alignTarget(
      targetRect = target,
      containerRect = container,
      alignment = layer.alignment,
      alignmentOffsetX = offsetX,
      alignmentOffsetY = offsetY,
      contentSize = rawSize,
      density = density,
      layoutDirection = layoutDirection,
    )
    val (bestResult, smartAlignment) = defaultResult.findBestResult(
      layer = layer,
      smartAliments = smartAlignments,
      targetRect = target,
      containerRect = container,
      alignmentOffsetX = offsetX,
      alignmentOffsetY = offsetY,
      contentSize = rawSize,
      density = density,
      layoutDirection = layoutDirection,
    )
    layer.updateSmartAlignment(smartAlignment)

    val (fixOffset, fixSize) = bestResult.fixOverFlow(layer, layoutDirection)
    _fixOffset = fixOffset
    _fixSize = fixSize
    _needsShrink = fixSize != rawSize

    layer.logMsg {
      """
        layoutSmart
           target:$target
           container:$container
           offset:(${bestResult.x}, ${bestResult.y}) -> $fixOffset
           size:$rawSize -> $fixSize
      """.trimIndent()
    }

    return layout(cs.maxWidth, cs.maxHeight) {
      rawPlaceable.place(fixOffset)
    }
  }

  override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean {
    return _needsShrink
  }

  override fun ApproachMeasureScope.approachMeasure(
    measurable: Measurable,
    constraints: Constraints,
  ): MeasureResult {
    val cs = constraints.copy(minWidth = 0, minHeight = 0)
    if (!_readyInPass) {
      val lastRect = layer.placedContentRect
      if (lastRect != null) {
        val placeable = measurable.measure(cs.newMax(lastRect.size))
        return layout(cs.maxWidth, cs.maxHeight) {
          placeable.place(lastRect.offset)
        }
      }
      return layout(cs.maxWidth, cs.maxHeight) {}
    }
    val targetConstraints = if (_needsShrink) {
      cs.newMax(_fixSize)
    } else {
      cs
    }
    val placeable = measurable.measure(targetConstraints)
    val size = placeable.intSize()
    val offset = _fixOffset
    return layout(cs.maxWidth, cs.maxHeight) {
      layer.updatePlacedContentRect(offset, size)
      placeable.place(offset)
    }
  }
}

/** 计算固定对齐方位下不发生可内收溢出的最大允许宽高 */
internal fun calculateMaxAllowedSize(
  layer: Layer? = null,
  targetRect: LayoutRect,
  containerRect: LayoutRect,
  alignment: TargetAlignment,
  alignmentOffsetX: TargetAlignmentOffset?,
  alignmentOffsetY: TargetAlignmentOffset?,
  maxContainerSize: IntSize,
  density: Float,
  layoutDirection: LayoutDirection,
): IntSize {
  val maxResult = alignTarget(
    targetRect = targetRect,
    containerRect = containerRect,
    alignment = alignment,
    alignmentOffsetX = alignmentOffsetX,
    alignmentOffsetY = alignmentOffsetY,
    contentSize = maxContainerSize,
    density = density,
    layoutDirection = layoutDirection,
  )
  return maxResult.fixOverFlow(layer, layoutDirection).size
}

/** 计算裁切指定外侧方向后保留的背景矩形区域 */
internal fun calculateClipBackgroundBounds(
  containerSize: IntSize,
  clipBackgroundDirection: Directions?,
  contentOffset: IntOffset,
  contentSize: IntSize,
  layoutDirection: LayoutDirection,
): IntRect {
  if ((clipBackgroundDirection == null) || (contentSize.width <= 0) || (contentSize.height <= 0)) {
    return IntRect(IntOffset.Zero, containerSize)
  }

  val direction = clipBackgroundDirection.takeIf { layoutDirection == LayoutDirection.Ltr }
    ?: clipBackgroundDirection.rtl()

  val left = contentOffset.x.coerceIn(0, containerSize.width)
  val top = contentOffset.y.coerceIn(0, containerSize.height)
  val right = (contentOffset.x + contentSize.width).coerceIn(left, containerSize.width)
  val bottom = (contentOffset.y + contentSize.height).coerceIn(top, containerSize.height)

  val clipLeft = if (direction.hasStart()) left else 0
  val clipTop = if (direction.hasTop()) top else 0
  val clipRight = if (direction.hasEnd()) right else containerSize.width
  val clipBottom = if (direction.hasBottom()) bottom else containerSize.height

  return IntRect(
    left = clipLeft,
    top = clipTop,
    right = clipRight.coerceAtLeast(clipLeft),
    bottom = clipBottom.coerceAtLeast(clipTop),
  )
}

/** 按 SmartAliment.transition > explicitTransition > 生效方位默认动画 的优先级解析过渡动画 */
internal fun resolveTargetLayerTransition(
  explicitTransition: LayerTransition?,
  smartAlignment: SmartAliment?,
  defaultAlignment: TargetAlignment,
  isOffsetTarget: Boolean,
  direction: LayoutDirection,
): LayerTransition {
  smartAlignment?.transition?.also { return it }
  if (explicitTransition != null) return explicitTransition
  val effectiveAlignment = smartAlignment?.alignment ?: defaultAlignment
  return if (isOffsetTarget) {
    effectiveAlignment.offsetTransition(direction)
  } else {
    effectiveAlignment.defaultTransition(direction)
  }
}

private fun alignTarget(
  targetRect: LayoutRect,
  containerRect: LayoutRect,
  alignment: TargetAlignment,
  alignmentOffsetX: TargetAlignmentOffset?,
  alignmentOffsetY: TargetAlignmentOffset?,
  contentSize: IntSize,
  density: Float,
  layoutDirection: LayoutDirection,
): Aligner.Result {
  var adjustedTarget = targetRect
  if ((alignmentOffsetX != null) || (alignmentOffsetY != null)) {
    val x = alignmentOffsetX.pxValue(density, adjustedTarget.size.width, alignment, xy = true)
    val offset = IntOffset(
      x = if (layoutDirection.isLtr()) x else -x,
      y = alignmentOffsetY.pxValue(density, adjustedTarget.size.height, alignment, xy = false),
    )
    adjustedTarget = adjustedTarget.copy(offset = adjustedTarget.offset + offset)
  }

  return Aligner.Input(
    position = alignment.toAlignerPosition(),

    targetX = adjustedTarget.offset.x,
    targetY = adjustedTarget.offset.y,
    targetWidth = adjustedTarget.size.width,
    targetHeight = adjustedTarget.size.height,

    containerX = containerRect.offset.x,
    containerY = containerRect.offset.y,
    containerWidth = containerRect.size.width,
    containerHeight = containerRect.size.height,

    sourceWidth = contentSize.width,
    sourceHeight = contentSize.height,
  ).toResult(layoutDirection.isLtr())
}

private fun Aligner.Result.findBestResult(
  layer: Layer,
  smartAliments: SmartAliments,
  targetRect: LayoutRect,
  containerRect: LayoutRect,
  alignmentOffsetX: TargetAlignmentOffset?,
  alignmentOffsetY: TargetAlignmentOffset?,
  contentSize: IntSize,
  density: Float,
  layoutDirection: LayoutDirection,
): Pair<Aligner.Result, SmartAliment?> {
  val list = smartAliments.aliments
  if (list.isEmpty()) return this to null

  val defaultOverflow = sourceOverflow.totalOverflow()
  if (defaultOverflow == 0) return this to null

  var bestResult = this
  var minOverflow = defaultOverflow
  var smartAliment: SmartAliment? = null

  for (item in list) {
    val newResult = alignTarget(
      targetRect = targetRect,
      containerRect = containerRect,
      alignment = item.alignment,
      alignmentOffsetX = alignmentOffsetX,
      alignmentOffsetY = alignmentOffsetY,
      contentSize = contentSize,
      density = density,
      layoutDirection = layoutDirection,
    )

    val newOverflow = newResult.sourceOverflow.totalOverflow()
    if (newOverflow < minOverflow) {
      bestResult = newResult
      minOverflow = newOverflow
      smartAliment = item
    }

    if (newOverflow == 0) {
      // 没有溢出了，停止查找
      break
    }
  }

  return (bestResult to smartAliment).also {
    layer.logMsg {
      "findBestResult ${this.input.position} -> ${bestResult.input.position}"
    }
  }
}

private fun Constraints.newMax(size: IntSize): Constraints {
  return this.copy(
    maxWidth = size.width.coerceIn(minWidth, maxWidth),
    maxHeight = size.height.coerceIn(minHeight, maxHeight),
  )
}

private fun Constraints.maxIntSize(): IntSize {
  return IntSize(maxWidth, maxHeight)
}

private fun Placeable.intSize(): IntSize {
  return IntSize(width, height)
}

private fun LayoutDirection.isLtr(): Boolean {
  return this == LayoutDirection.Ltr
}
