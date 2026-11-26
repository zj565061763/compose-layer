package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round

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

@Immutable
private data class UIState(
  val targetLayout: LayoutInfo,
  val containerLayout: LayoutInfo,
  val alignment: TargetAlignment,
  val alignmentOffsetX: TargetAlignmentOffset?,
  val alignmentOffsetY: TargetAlignmentOffset?,
  val smartAlignments: SmartAliments?,
  val clipBackgroundDirection: Directions?,
)

@Immutable
private data class LayoutInfo(
  val isAttached: Boolean,
  val offset: IntOffset,
  val size: IntSize,
)

private val EmptyLayoutInfo = LayoutInfo(
  isAttached = false,
  offset = IntOffset.Zero,
  size = IntSize.Zero,
)

internal class TargetLayerImpl : LayerImpl(), TargetLayer {
  private var _uiState by mutableStateOf(
    UIState(
      targetLayout = EmptyLayoutInfo,
      containerLayout = EmptyLayoutInfo,
      alignment = TargetAlignment.Center,
      alignmentOffsetX = null,
      alignmentOffsetY = null,
      smartAlignments = null,
      clipBackgroundDirection = null,
    ),
  )

  /** 目标 */
  private var _target: LayerTarget? = null
  private var _isOffsetTarget by mutableStateOf(false)

  /** 当前命中的智能对齐 */
  private var _currentSmartAlignment: SmartAliment? = null

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
    updateTargetLayout()
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

  /** Tag目标布局信息 */
  private var _tagTargetLayout: LayoutCoordinates? = null
  /** 监听Tag目标布局信息 */
  private val _tagTargetLayoutCallback: LayoutCoordinatesCallback = {
    _tagTargetLayout = it
    updateTargetLayout()
  }

  /** 监听容器布局信息 */
  private val _containerLayoutCallback: LayoutCoordinatesCallback = { layoutCoordinates ->
    val layout = layoutCoordinates.toLayoutInfo()
    if (_uiState.containerLayout != layout) {
      _uiState = _uiState.copy(containerLayout = layout)
    }
  }

  override fun setAlignment(alignment: TargetAlignment) {
    if (_uiState.alignment != alignment) {
      _uiState = _uiState.copy(alignment = alignment)
    }
  }

  override fun setAlignmentOffsetX(offset: TargetAlignmentOffset?) {
    if (_uiState.alignmentOffsetX != offset) {
      _uiState = _uiState.copy(alignmentOffsetX = offset)
    }
  }

  override fun setAlignmentOffsetY(offset: TargetAlignmentOffset?) {
    if (_uiState.alignmentOffsetY != offset) {
      _uiState = _uiState.copy(alignmentOffsetY = offset)
    }
  }

  override fun setSmartAlignments(alignments: SmartAliments?) {
    if (_uiState.smartAlignments != alignments) {
      _uiState = _uiState.copy(smartAlignments = alignments)
      if (alignments == null) {
        _currentSmartAlignment = null
      }
    }
  }

  override fun setClipBackgroundDirection(direction: Directions?) {
    if (_uiState.clipBackgroundDirection != direction) {
      _uiState = _uiState.copy(clipBackgroundDirection = direction)
    }
  }

  override fun onAttach(container: ContainerForLayer) {
    container.registerContainerLayoutCallback(_containerLayoutCallback)
    registerTarget(_target)
  }

  override fun onDetach(container: ContainerForLayer) {
    container.unregisterContainerLayoutCallback(_containerLayoutCallback)
    unregisterTarget(_target)
  }

  override fun onDetached(container: ContainerForLayer) {
    _currentSmartAlignment = null
  }

  /** 更新目标布局信息 */
  private fun updateTargetLayout() {
    val layout = when (val target = _target) {
      is LayerTarget.Tag -> _tagTargetLayout.toLayoutInfo()
      is LayerTarget.Offset -> target.offset?.let { offset ->
        LayoutInfo(
          isAttached = true,
          offset = offset,
          size = IntSize.Zero,
        )
      } ?: EmptyLayoutInfo
      else -> EmptyLayoutInfo
    }

    if (_uiState.targetLayout != layout) {
      _uiState = _uiState.copy(targetLayout = layout)
    }
  }

  @Composable
  override fun getLayerTransition(transition: LayerTransition?): LayerTransition {
    if (transition != null) {
      logMsg { "getLayerTransition from params" }
      return transition
    }

    val direction = LocalLayoutDirection.current

    _currentSmartAlignment?.also { sa ->
      logMsg { "getLayerTransition from smartAlignment $sa" }
      return sa.transition ?: sa.alignment.transition(direction)
    }

    logMsg { "getLayerTransition from default" }
    return _uiState.alignment.transition(direction)
  }

  private fun TargetAlignment.transition(direction: LayoutDirection): LayerTransition {
    return if (_isOffsetTarget) {
      offsetTransition(direction)
    } else {
      defaultTransition(direction)
    }
  }

  @Composable
  override fun BoxScope.LayerContent() {
    OffsetBox(
      modifier = Modifier.fillMaxSize(),
      uiState = _uiState,
    )
  }

  @Composable
  private fun OffsetBox(
    modifier: Modifier = Modifier,
    uiState: UIState,
  ) {
    val state = remember { OffsetBoxState() }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    SubcomposeLayout(modifier) { cs ->
      @Suppress("NAME_SHADOWING")
      val cs = cs.copy(minWidth = 0, minHeight = 0)
      state.measureScope = this

      val isReady = with(uiState) {
        (lifecycleState == LayerLifecycleState.Attached) &&
          targetLayout.isAttached &&
          containerLayout.isAttached
      }

      logMsg {
        """
               layout start ----------> isVisible:$isVisibleState isReady:$isReady
                  alignment:${uiState.alignment}
                  target:${uiState.targetLayout}
                  container:${uiState.containerLayout}
                  cs:$cs
            """.trimIndent()
      }

      when (isReady) {
        true -> state.layoutDefault(cs, uiState, density.density, layoutDirection)
        false -> state.layoutLastInfo(cs)
      }.also {
        setContentVisible(isReady)
      }
    }
  }

  private inner class OffsetBoxState {

    fun layoutDefault(
      cs: Constraints,
      uiState: UIState,
      density: Float,
      layoutDirection: LayoutDirection,
    ): MeasureResult {
      logMsg { "layoutDefault start" }

      val rawPlaceable = measureRawContent(cs)
      val rawSize = rawPlaceable.intSize()

      var result = alignTarget(
        uiState = uiState,
        contentSize = rawSize,
        density = density,
        layoutDirection = layoutDirection,
      )

      val smartAlignments = uiState.smartAlignments
      if (smartAlignments != null) {
        val (bestResult, smartAlignment) = result.findBestResult(
          layer = this@TargetLayerImpl,
          smartAliments = smartAlignments,
          uiState = uiState,
          contentSize = rawSize,
          density = density,
          layoutDirection = layoutDirection,
        )
        result = bestResult
        _currentSmartAlignment = smartAlignment
      } else {
        _currentSmartAlignment = null
      }

      val (fixOffset, fixSize) = result.fixOverFlow(this@TargetLayerImpl, layoutDirection)
      val contentPlaceable = measureContent(cs.newMax(fixSize))

      val backgroundInfo = backgroundPlaceInfo(
        cs = cs,
        clipBackgroundDirection = uiState.clipBackgroundDirection,
        contentOffset = fixOffset,
        contentSize = fixSize,
        layoutDirection = layoutDirection,
      )
      val backgroundPlaceable = measureBackground(cs.newMax(backgroundInfo.size))

      logMsg {
        """
               layoutDefault end
                  offset:(${result.x}, ${result.y}) -> $fixOffset
                  size:$rawSize -> $fixSize
            """.trimIndent()
      }

      return layoutFinally(
        cs = cs,
        backgroundPlaceable = backgroundPlaceable,
        backgroundOffset = backgroundInfo.offset,
        contentPlaceable = contentPlaceable,
        contentOffset = fixOffset,
      )
    }

    fun layoutLastInfo(cs: Constraints): MeasureResult {
      val backgroundInfo = _lastBackgroundInfo ?: PlaceInfo(IntOffset.Zero, cs.maxIntSize())
      val backgroundPlaceable = measureBackground(cs.newMax(backgroundInfo.size))

      val contentInfo = _lastContentInfo ?: PlaceInfo(IntOffset.Zero, cs.maxIntSize())
      val contentPlaceable = measureContent(cs.newMax(contentInfo.size))

      return layoutFinally(
        cs = cs,
        backgroundPlaceable = backgroundPlaceable,
        backgroundOffset = backgroundInfo.offset,
        contentPlaceable = contentPlaceable,
        contentOffset = contentInfo.offset,
        saveInfo = false,
      )
    }

    private fun layoutFinally(
      cs: Constraints,
      backgroundPlaceable: Placeable,
      backgroundOffset: IntOffset,
      contentPlaceable: Placeable,
      contentOffset: IntOffset,
      saveInfo: Boolean = true,
    ): MeasureResult {
      logMsg { "layoutFinally offset:${contentOffset} size:${contentPlaceable.intSize()}" }
      return measureScope.layout(cs.maxWidth, cs.maxHeight) {
        if (saveInfo) {
          _lastBackgroundInfo = PlaceInfo(
            offset = backgroundOffset,
            size = backgroundPlaceable.intSize(),
          )
          _lastContentInfo = PlaceInfo(
            offset = contentOffset,
            size = contentPlaceable.intSize(),
          )
        }
        backgroundPlaceable.place(backgroundOffset, -1f)
        contentPlaceable.place(contentOffset)
      }
    }

    private fun backgroundPlaceInfo(
      cs: Constraints,
      clipBackgroundDirection: Directions?,
      contentOffset: IntOffset,
      contentSize: IntSize,
      layoutDirection: LayoutDirection,
    ): PlaceInfo {
      if ((clipBackgroundDirection == null) || (contentSize.width <= 0) || (contentSize.height <= 0)) {
        return PlaceInfo(
          offset = IntOffset.Zero,
          size = cs.maxIntSize(),
        )
      }

      val direction = clipBackgroundDirection.takeIf { layoutDirection == LayoutDirection.Ltr }
        ?: clipBackgroundDirection.rtl()

      val left = contentOffset.x.coerceIn(0, cs.maxWidth)
      val top = contentOffset.y.coerceIn(0, cs.maxHeight)
      val right = (contentOffset.x + contentSize.width).coerceIn(left, cs.maxWidth)
      val bottom = (contentOffset.y + contentSize.height).coerceIn(top, cs.maxHeight)

      var x = 0
      var y = 0
      var width = cs.maxWidth
      var height = cs.maxHeight

      if (direction.hasTop()) {
        height -= top.also {
          logMsg { "clip background top:$it" }
        }
        y = top
      }
      if (direction.hasBottom()) {
        height -= (cs.maxHeight - bottom).also {
          logMsg { "clip background bottom:$it" }
        }
      }

      if (direction.hasStart()) {
        width -= left.also {
          logMsg { "clip background start:$it" }
        }
        x = left
      }
      if (direction.hasEnd()) {
        width -= (cs.maxWidth - right).also {
          logMsg { "clip background end:$it" }
        }
      }

      return PlaceInfo(
        offset = IntOffset(x, y),
        size = IntSize(width.coerceAtLeast(0), height.coerceAtLeast(0)),
      )
    }

    lateinit var measureScope: SubcomposeMeasureScope

    private var _lastBackgroundInfo: PlaceInfo? = null
    private var _lastContentInfo: PlaceInfo? = null

    /** 测量背景 */
    private fun measureBackground(constraints: Constraints): Placeable {
      return measureScope.subcompose(SlotId.Background) {
        BackgroundBox()
      }.let {
        check(it.size == 1)
        it.first().measure(constraints)
      }
    }

    /** 测量内容 */
    private fun measureContent(constraints: Constraints): Placeable {
      return measureScope.subcompose(SlotId.Content) {
        ContentBox()
      }.let {
        check(it.size == 1)
        it.first().measure(constraints)
      }
    }

    /** 测量原始内容 */
    private fun measureRawContent(constraints: Constraints): Placeable {
      return measureScope.subcompose(SlotId.RawContent) {
        RawContent()
      }.let {
        check(it.size == 1)
        it.first().measure(constraints)
      }
    }
  }

  private data class PlaceInfo(
    val offset: IntOffset,
    val size: IntSize,
  )

  private enum class SlotId {
    Background,
    Content,
    RawContent,
  }
}

private fun alignTarget(
  uiState: UIState,
  contentSize: IntSize,
  density: Float,
  layoutDirection: LayoutDirection,
): Aligner.Result {
  val alignment = uiState.alignment

  var targetLayout = uiState.targetLayout
  with(uiState) {
    if (alignmentOffsetX != null || alignmentOffsetY != null) {
      val layout = targetLayout
      val x = alignmentOffsetX.pxValue(density, layout.size.width, alignment, xy = true)
      val offset = IntOffset(
        x = if (layoutDirection.isLtr()) x else -x,
        y = alignmentOffsetY.pxValue(density, layout.size.height, alignment, xy = false)
      )
      targetLayout = layout.copy(offset = layout.offset + offset)
    }
  }

  return Aligner.Input(
    position = alignment.toAlignerPosition(),

    targetX = targetLayout.offset.x,
    targetY = targetLayout.offset.y,
    targetWidth = targetLayout.size.width,
    targetHeight = targetLayout.size.height,

    containerX = uiState.containerLayout.offset.x,
    containerY = uiState.containerLayout.offset.y,
    containerWidth = uiState.containerLayout.size.width,
    containerHeight = uiState.containerLayout.size.height,

    sourceWidth = contentSize.width,
    sourceHeight = contentSize.height,
  ).toResult(layoutDirection.isLtr())
}

private fun Aligner.Result.findBestResult(
  layer: Layer,
  smartAliments: SmartAliments,
  uiState: UIState,
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
      uiState = uiState.copy(alignment = item.alignment),
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

private fun LayoutCoordinates?.toLayoutInfo(): LayoutInfo {
  if (this?.isAttached == true) {
    return LayoutInfo(
      isAttached = true,
      offset = positionInWindow().round(),
      size = size,
    )
  }
  return EmptyLayoutInfo
}

private fun Constraints.newMax(size: IntSize): Constraints {
  return this.copy(maxWidth = size.width, maxHeight = size.height)
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