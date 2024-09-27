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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

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
   val targetLayout: LayoutInfo,
   val containerLayout: LayoutInfo,
   val alignment: TargetAlignment,
   val alignmentOffsetX: TargetAlignmentOffset?,
   val alignmentOffsetY: TargetAlignmentOffset?,
)

@Immutable
private data class LayoutInfo(
   val offset: IntOffset,
   val size: IntSize,
   val isAttached: Boolean,
)

private val EmptyLayoutInfo = LayoutInfo(
   offset = IntOffset.Zero,
   size = IntSize.Zero,
   isAttached = false,
)

internal class TargetLayerImpl : LayerImpl(), TargetLayer {
   private val _uiState = MutableStateFlow(
      UIState(
         targetLayout = EmptyLayoutInfo,
         containerLayout = EmptyLayoutInfo,
         alignment = TargetAlignment.Center,
         alignmentOffsetX = null,
         alignmentOffsetY = null,
      )
   )

   /** 目标 */
   private var _target: LayerTarget? = null

   /** 智能对齐 */
   private var _smartAlignments: SmartAliments? = null
   private var _currentSmartAlignment: SmartAliment? = null

   /** 裁切背景 */
   private var _clipBackgroundDirection: Directions? = null

   override fun setTarget(target: LayerTarget?) {
      if (_target == target) return
      logMsg { "setTarget:$target" }

      val oldTarget = _target
      unregisterTarget(oldTarget)

      _target = target

      registerTarget(target)
      updateTargetLayout()
   }

   private fun registerTarget(target: LayerTarget?) {
      if (target is LayerTarget.Tag) {
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

   override fun setAlignment(alignment: TargetAlignment) {
      if (_uiState.value.alignment == alignment) return
      _uiState.update {
         it.copy(alignment = alignment)
      }
   }

   override fun setAlignmentOffsetX(offset: TargetAlignmentOffset?) {
      if (_uiState.value.alignmentOffsetX == offset) return
      _uiState.update {
         it.copy(alignmentOffsetX = offset)
      }
   }

   override fun setAlignmentOffsetY(offset: TargetAlignmentOffset?) {
      if (_uiState.value.alignmentOffsetY == offset) return
      _uiState.update {
         it.copy(alignmentOffsetY = offset)
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
      registerTarget(_target)
   }

   override fun onDetach(container: ContainerForLayer) {
      container.unregisterContainerLayoutCallback(_containerLayoutCallback)
      unregisterTarget(_target)
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
      val layout = when (val target = _target) {
         is LayerTarget.Tag -> _tagTargetLayout.toLayoutInfo()
         is LayerTarget.Offset -> target.offset?.let { offset ->
            LayoutInfo(
               offset = offset,
               size = IntSize.Zero,
               isAttached = true,
            )
         } ?: EmptyLayoutInfo
         else -> EmptyLayoutInfo
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
         modifier = Modifier
            .fillMaxSize()
            .zIndex(zIndexState),
         uiState = uiState,
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
            state.layoutDefault(cs, uiState, density.density, layoutDirection)
         } else {
            state.layoutLastInfo(cs)
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

         _smartAlignments?.let {
            val (bestResult, smartAlignment) = result.findBestResult(
               layer = this@TargetLayerImpl,
               smartAliments = it,
               uiState = uiState,
               contentSize = rawSize,
               density = density,
               layoutDirection = layoutDirection,
            )
            result = bestResult
            _currentSmartAlignment = smartAlignment
         }

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

      private var _lastBackgroundInfo: PlaceInfo? = null
      private var _lastContentInfo: PlaceInfo? = null

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
         val offset = IntOffset(
            x = alignmentOffsetX.pxValue(density, layout.size.width, alignment, xy = true),
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
      offset = offset(),
      size = size(),
      isAttached = isAttached(),
   )
}

private fun LayoutCoordinates?.offset(): IntOffset {
   return if (this?.isAttached == true) {
      this.localToWindow(Offset.Zero).round()
   } else {
      IntOffset.Zero
   }
}

private fun LayoutCoordinates?.size(): IntSize {
   return if (this?.isAttached == true) {
      this.size
   } else {
      IntSize.Zero
   }
}

private fun LayoutCoordinates?.isAttached(): Boolean {
   return this?.isAttached == true
}

private fun Constraints.newMax(size: IntSize): Constraints {
   return this.copy(maxWidth = size.width, maxHeight = size.height)
}

private fun Constraints.maxIntSize(): IntSize = IntSize(maxWidth, maxHeight)

private fun Placeable.intSize(): IntSize = IntSize(width, height)

private fun LayoutDirection.isLtr(): Boolean = this == LayoutDirection.Ltr