package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

interface TargetLayerState : LayerState

/**
 * 要对齐的目标
 */
@Immutable
sealed interface LayerTarget {
   /** 以[tag]为目标 */
   data class Tag(val tag: String?) : LayerTarget

   /** 以[offset]为目标 */
   data class Offset(val offset: IntOffset?) : LayerTarget
}

/**
 * 目标对齐位置偏移量
 */
@Immutable
sealed interface TargetAlignmentOffset {
   /** 按指定像素[value]偏移，支持正数和负数，以Y轴为例，大于0往下偏移，小于0往上偏移 */
   data class PX(val value: Int) : TargetAlignmentOffset

   /** 按目标大小倍数[value]偏移，支持正数和负数字，以Y轴为例，1表示往下偏移1倍目标的高度，-1表示往上偏移1倍目标的高度 */
   data class Target(val value: Float) : TargetAlignmentOffset
}

internal interface TargetLayer : Layer, TargetLayerState {
   /**
    * 要对齐的目标
    */
   fun setTarget(target: LayerTarget?)

   /**
    * 对齐目标位置，默认[TargetAlignment.Center]
    */
   fun setAlignment(alignment: TargetAlignment)

   /**
    * 对齐目标X方向偏移量
    */
   fun setAlignmentOffsetX(offset: TargetAlignmentOffset?)

   /**
    * 对齐目标Y方向偏移量
    */
   fun setAlignmentOffsetY(offset: TargetAlignmentOffset?)

   /**
    * 智能对齐目标位置（非响应式），null-关闭智能对齐；非null-开启智能对齐，默认值null。
    * 开启之后，如果默认的[setAlignment]导致内容溢出会使用[alignments]提供的位置按顺序查找溢出最小的位置
    */
   fun setSmartAlignments(alignments: SmartAliments?)

   /**
    * 裁切背景的方向[Directions]（非响应式）
    */
   fun setClipBackgroundDirection(direction: Directions?)
}

internal fun TargetLayer.toTargetLayerState(): TargetLayerState = InternalTargetLayerState(this)
private class InternalTargetLayerState(layer: TargetLayer) : TargetLayerState by layer

//---------- Impl ----------

@Immutable
private data class UIState(
   val alignment: TargetAlignment,
   val targetLayout: LayoutInfo,
   val containerLayout: LayoutInfo,
)

@Immutable
private data class LayoutInfo(
   val offset: IntOffset,
   val size: IntSize,
   val isAttached: Boolean,
)

private val EmptyLayoutInfo = LayoutInfo(
   size = IntSize.Zero,
   offset = IntOffset.Zero,
   isAttached = false,
)

internal class TargetLayerImpl : LayerImpl(), TargetLayer {
   private val _uiState = MutableStateFlow(
      UIState(
         alignment = TargetAlignment.Center,
         targetLayout = EmptyLayoutInfo,
         containerLayout = EmptyLayoutInfo,
      )
   )

   /** 目标 */
   private var _target: LayerTarget? = null

   /** X方向偏移量 */
   private var _alignmentOffsetX: TargetAlignmentOffset? = null
   /** Y方向偏移量 */
   private var _alignmentOffsetY: TargetAlignmentOffset? = null

   /** 智能对齐 */
   private var _smartAlignments: SmartAliments? = null
   private var _currentSmartAlignment: SmartAliment? = null

   /** 裁切背景 */
   private var _clipBackgroundDirection: Directions? = null

   override fun setTarget(target: LayerTarget?) {
      if (_target == target) return

      val oldTarget = _target
      unregisterTagTargetLayoutCallback(oldTarget)

      _target = target

      registerTagTargetLayoutCallback(target)
      updateTargetLayout()
   }

   private fun registerTagTargetLayoutCallback(target: LayerTarget?) {
      if (target is LayerTarget.Tag) {
         layerContainer?.registerTargetLayoutCallback(target.tag, _tagTargetLayoutCallback)
      }
   }

   private fun unregisterTagTargetLayoutCallback(target: LayerTarget?) {
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

   override fun setAlignment(alignment: TargetAlignment) {
      _uiState.update {
         if (it.alignment != alignment) {
            it.copy(alignment = alignment)
         } else {
            it
         }
      }
   }

   override fun setAlignmentOffsetX(offset: TargetAlignmentOffset?) {
      if (_alignmentOffsetX != offset) {
         _alignmentOffsetX = offset
         updateTargetLayout()
      }
   }

   override fun setAlignmentOffsetY(offset: TargetAlignmentOffset?) {
      if (_alignmentOffsetY != offset) {
         _alignmentOffsetY = offset
         updateTargetLayout()
      }
   }

   override fun setSmartAlignments(alignments: SmartAliments?) {
      _smartAlignments = alignments
   }

   override fun setClipBackgroundDirection(direction: Directions?) {
      _clipBackgroundDirection = direction
   }

   override fun onAttach(container: ContainerForLayer) {
      container.registerContainerLayoutCallback(_containerLayoutCallback)
      registerTagTargetLayoutCallback(_target)
   }

   override fun onDetach(container: ContainerForLayer) {
      container.unregisterContainerLayoutCallback(_containerLayoutCallback)
      unregisterTagTargetLayoutCallback(_target)
   }

   override fun onDetached(container: ContainerForLayer) {
      _currentSmartAlignment = null
   }

   /** 监听容器布局信息 */
   private val _containerLayoutCallback: LayoutCoordinatesCallback = { layout ->
      _uiState.update {
         it.copy(containerLayout = layout.toLayoutInfo())
      }
   }

   /**
    * 更新目标布局信息
    */
   private fun updateTargetLayout() {
      var layout = when (val target = _target) {
         is LayerTarget.Tag -> {
            if (target.tag.isNullOrEmpty()) return
            _tagTargetLayout.toLayoutInfo()
         }
         is LayerTarget.Offset -> {
            val offset = target.offset ?: return
            LayoutInfo(
               size = IntSize.Zero,
               offset = offset,
               isAttached = true,
            )
         }
         else -> return
      }

      val offsetX = _alignmentOffsetX
      val offsetY = _alignmentOffsetY
      if (offsetX != null || offsetY != null) {
         val offset = IntOffset(
            x = offsetX.pxValue(layout.size.width),
            y = offsetY.pxValue(layout.size.height)
         )
         layout = layout.copy(offset = layout.offset + offset)
      }

      _uiState.update {
         it.copy(targetLayout = layout)
      }
   }

   @Composable
   override fun getLayerTransition(transition: LayerTransition?): LayerTransition {
      val direction = LocalLayoutDirection.current

      _currentSmartAlignment?.let {
         logMsg { "getLayerTransition from smartAlignment $it" }
         return it.transition ?: it.alignment.transition(direction)
      }

      transition?.let {
         logMsg { "getLayerTransition from params" }
         return it
      }

      logMsg { "getLayerTransition from default" }
      val uiState by _uiState.collectAsStateWithLifecycle()
      return uiState.alignment.transition(direction)
   }

   private fun TargetAlignment.transition(direction: LayoutDirection): LayerTransition {
      return if (_target is LayerTarget.Offset) {
         offsetTransition(direction)
      } else {
         defaultTransition(direction)
      }
   }

   @Composable
   override fun LayerContent() {
      val uiState by _uiState.collectAsStateWithLifecycle()
      OffsetBox(
         modifier = Modifier.fillMaxSize(),
         uiState = uiState,
      )
   }

   @Composable
   private fun OffsetBox(
      modifier: Modifier = Modifier,
      uiState: UIState,
   ) {
      val state = remember { OffsetBoxState() }
      val layoutDirection = LocalLayoutDirection.current

      SubcomposeLayout(modifier) { cs ->
         @Suppress("NAME_SHADOWING")
         val cs = cs.copy(minWidth = 0, minHeight = 0)
         state.measureScope = this

         val isReady = uiState.targetLayout.isAttached
            && uiState.containerLayout.isAttached

         logMsg {
            """
               layout start ----------> isVisible:$isVisibleState isReady:$isReady
                  alignment:${uiState.alignment}
                  target:${uiState.targetLayout}
                  container:${uiState.containerLayout}
                  cs:$cs
            """.trimIndent()
         }

         if (isReady) {
            state.layoutDefault(cs, uiState, layoutDirection)
         } else {
            state.layoutLastInfo(cs)
         }.also {
            if (isReady) {
               if (!isVisibleState) {
                  setContentVisible(true)
               }
            } else {
               if (isVisibleState) {
                  setContentVisible(false)
               }
            }
         }
      }
   }

   private inner class OffsetBoxState {

      fun layoutDefault(
         cs: Constraints,
         uiState: UIState,
         layoutDirection: LayoutDirection,
      ): MeasureResult {
         logMsg { "layoutDefault start" }

         val rawPlaceable = measureRawContent(cs)
         val rawSize = rawPlaceable.intSize()

         val (result, smartAlignment) = alignTarget(
            alignment = uiState.alignment,
            target = uiState.targetLayout,
            container = uiState.containerLayout,
            contentSize = rawSize,
            layoutDirection = layoutDirection,
         ).findBestResult(this@TargetLayerImpl, _smartAlignments, layoutDirection)

         _currentSmartAlignment = smartAlignment

         val (fixOffset, fixSize) = result.fixOverFlow(this@TargetLayerImpl, layoutDirection)

         val contentPlaceable = measureContent(cs.newMax(fixSize))

         val backgroundInfo = backgroundPlaceInfo(
            cs = cs,
            contentOffset = fixOffset,
            contentSize = fixSize,
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
         val backgroundInfo = _visibleBackgroundInfo ?: PlaceInfo(IntOffset.Zero, cs.maxIntSize())
         val backgroundPlaceable = measureBackground(cs.newMax(backgroundInfo.size))

         val contentInfo = _visibleContentInfo ?: PlaceInfo(IntOffset.Zero, cs.maxIntSize())
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
               _visibleBackgroundInfo = PlaceInfo(
                  offset = backgroundOffset,
                  size = backgroundPlaceable.intSize(),
               )
               _visibleContentInfo = PlaceInfo(
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
         contentOffset: IntOffset,
         contentSize: IntSize,
      ): PlaceInfo {
         val direction = _clipBackgroundDirection
         if (direction == null || contentSize.width <= 0 || contentSize.height <= 0) {
            return PlaceInfo(
               offset = IntOffset.Zero,
               size = cs.maxIntSize(),
            )
         }

         val contentX = contentOffset.x.coerceAtLeast(0)
         val contentY = contentOffset.y.coerceAtLeast(0)

         var x = 0
         var y = 0
         var width = cs.maxWidth
         var height = cs.maxHeight

         if (direction.hasTop()) {
            height -= contentY.also {
               logMsg { "clip background top:$it" }
            }
            y = contentY
         }
         if (direction.hasBottom()) {
            height -= (cs.maxHeight - contentY - contentSize.height).also {
               logMsg { "clip background bottom:$it" }
            }
         }

         if (direction.hasStart()) {
            width -= contentX.also {
               logMsg { "clip background start:$it" }
            }
            x = contentX
         }
         if (direction.hasEnd()) {
            width -= (cs.maxWidth - contentX - contentSize.width).also {
               logMsg { "clip background end:$it" }
            }
         }

         return PlaceInfo(
            offset = IntOffset(x, y),
            size = IntSize(width.coerceAtLeast(0), height.coerceAtLeast(0)),
         )
      }

      lateinit var measureScope: SubcomposeMeasureScope

      private var _visibleBackgroundInfo: PlaceInfo? = null
      private var _visibleContentInfo: PlaceInfo? = null

      /**
       * 测量背景
       */
      private fun measureBackground(constraints: Constraints): Placeable {
         return measureScope.subcompose(SlotId.Background) {
            BackgroundBox()
         }.let {
            check(it.size == 1)
            it.first().measure(constraints)
         }
      }

      /**
       * 测量内容
       */
      private fun measureContent(constraints: Constraints): Placeable {
         return measureScope.subcompose(SlotId.Content) {
            ContentBox()
         }.let {
            check(it.size == 1)
            it.first().measure(constraints)
         }
      }

      /**
       * 测量原始内容
       */
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
   alignment: TargetAlignment,
   target: LayoutInfo,
   container: LayoutInfo,
   contentSize: IntSize,
   layoutDirection: LayoutDirection,
): Aligner.Result {
   return Aligner.Input(
      position = alignment.toAlignerPosition(),

      targetX = target.offset.x,
      targetY = target.offset.y,
      targetWidth = target.size.width,
      targetHeight = target.size.height,

      containerX = container.offset.x,
      containerY = container.offset.y,
      containerWidth = container.size.width,
      containerHeight = container.size.height,

      sourceWidth = contentSize.width,
      sourceHeight = contentSize.height,
   ).toResult(layoutDirection.isLtr())
}

private fun Aligner.Result.findBestResult(
   layer: Layer,
   alignments: SmartAliments?,
   layoutDirection: LayoutDirection,
): Pair<Aligner.Result, SmartAliment?> {
   if (alignments == null) return this to null

   val list = alignments.aliments
   if (list.isEmpty()) return this to null

   val overflowDefault = sourceOverflow.totalOverflow()
   if (overflowDefault == 0) return this to null

   var bestResult = this
   var minOverflow = overflowDefault
   var smartAliment: SmartAliment? = null

   for (item in list) {
      val position = item.alignment.toAlignerPosition()
      val newResult = this.input.copy(position = position).toResult(layoutDirection.isLtr())
      val newOverflow = newResult.sourceOverflow.totalOverflow()
      if (newOverflow < minOverflow) {
         minOverflow = newOverflow
         bestResult = newResult
         smartAliment = item
      }
      if (newOverflow == 0) break
   }

   return (bestResult to smartAliment).also {
      layer.logMsg {
         "findBestResult ${this.input.position} -> ${bestResult.input.position}"
      }
   }
}

private data class FixOverFlow(
   val offset: IntOffset,
   val size: IntSize,
)

private fun Aligner.Result.fixOverFlow(
   layer: Layer,
   layoutDirection: LayoutDirection,
): FixOverFlow {
   val position = input.position
   var result = this

   var resultWidth = result.input.sourceWidth
   var resultHeight = result.input.sourceHeight

   var count = 0
   while (true) {
      layer.logMsg {
         "checkOverflow ${++count} ($resultWidth,$resultHeight)"
      }

      var hasOverflow = false

      // 检查是否溢出
      with(result.sourceOverflow) {
         // Horizontal
         kotlin.run {
            var overSize = 0
            var isStartOverflow = false
            var isEndOverflow = false

            if (start > 0) {
               overSize += start
               isStartOverflow = true
            }

            if (end > 0) {
               overSize += end
               isEndOverflow = true
            }

            if (overSize > 0) {
               hasOverflow = true

               /**
                * 居中对齐的时候，如果只有一边溢出，则需要减去双倍溢出的值
                */
               if (position.isCenterHorizontal()) {
                  if (isStartOverflow && isEndOverflow) {
                     // 正常流程
                  } else {
                     overSize *= 2
                  }
               }

               val oldWidth = resultWidth
               resultWidth = oldWidth - overSize

               layer.logMsg {
                  val startLog = if (start > 0) " start:$start" else ""
                  val endLog = if (end > 0) " end:$end" else ""
                  "width overflow:${overSize}${startLog}${endLog} ($oldWidth)->($resultWidth)"
               }
            }
         }

         // Vertical
         kotlin.run {
            var overSize = 0
            var isTopOverflow = false
            var isBottomOverflow = false

            if (top > 0) {
               overSize += top
               isTopOverflow = true
            }

            if (bottom > 0) {
               overSize += bottom
               isBottomOverflow = true
            }

            if (overSize > 0) {
               hasOverflow = true

               /**
                * 居中对齐的时候，如果只有一边溢出，则需要减去双倍溢出的值
                */
               if (position.isCenterVertical()) {
                  if (isTopOverflow && isBottomOverflow) {
                     // 正常流程
                  } else {
                     overSize *= 2
                  }
               }

               val oldHeight = resultHeight
               resultHeight = oldHeight - overSize

               layer.logMsg {
                  val topLog = if (top > 0) " top:$top" else ""
                  val bottomLog = if (bottom > 0) " bottom:$bottom" else ""
                  "height overflow:${overSize}${topLog}${bottomLog} ($oldHeight)->($resultHeight)"
               }
            }
         }
      }

      if (hasOverflow) {
         if (resultWidth <= 0 || resultHeight <= 0) {
            break
         }
         result = result.input.copy(
            sourceWidth = resultWidth,
            sourceHeight = resultHeight,
         ).toResult(layoutDirection.isLtr())
      } else {
         break
      }
   }

   return FixOverFlow(
      offset = IntOffset(result.x, result.y),
      size = IntSize(resultWidth.coerceAtLeast(0), resultHeight.coerceAtLeast(0))
   )
}

private fun Aligner.Position.isCenterHorizontal(): Boolean {
   return when (this) {
      Aligner.Position.TopCenter,
      Aligner.Position.BottomCenter,
      Aligner.Position.Center,
      -> true

      else -> false
   }
}

private fun Aligner.Position.isCenterVertical(): Boolean {
   return when (this) {
      Aligner.Position.StartCenter,
      Aligner.Position.EndCenter,
      Aligner.Position.Center,
      -> true

      else -> false
   }
}

private fun LayoutCoordinates?.toLayoutInfo(): LayoutInfo {
   return LayoutInfo(
      size = size(),
      offset = offset(),
      isAttached = isAttached(),
   )
}

private fun LayoutCoordinates?.size(): IntSize {
   if (this == null || !this.isAttached) return IntSize.Zero
   return this.size
}

private fun LayoutCoordinates?.offset(): IntOffset {
   if (this == null || !this.isAttached) return IntOffset.Zero
   val offset = this.localToWindow(Offset.Zero)
   return IntOffset(offset.x.toInt(), offset.y.toInt())
}

private fun LayoutCoordinates?.isAttached(): Boolean {
   if (this == null) return false
   return this.isAttached
}

private fun TargetAlignmentOffset?.pxValue(targetSize: Int): Int {
   return when (val offset = this) {
      null -> 0
      is TargetAlignmentOffset.PX -> offset.value
      is TargetAlignmentOffset.Target -> {
         val px = targetSize * offset.value
         if (px.isInfinite()) 0 else px.roundToInt()
      }
   }
}

private fun Constraints.newMax(size: IntSize): Constraints {
   return this.copy(maxWidth = size.width, maxHeight = size.height)
}

private fun Constraints.maxIntSize(): IntSize = IntSize(maxWidth, maxHeight)

private fun Placeable.intSize(): IntSize = IntSize(width, height)

private fun LayoutDirection.isLtr(): Boolean = this == LayoutDirection.Ltr