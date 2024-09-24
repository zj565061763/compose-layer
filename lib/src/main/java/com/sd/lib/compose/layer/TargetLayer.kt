package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.sd.lib.aligner.Aligner
import com.sd.lib.aligner.toResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

internal interface TargetLayer : Layer {
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
    * 智能对齐目标位置（非响应式），null-关闭智能对齐；非null-开启智能对齐，如果是空列表则采用内置的对齐列表，默认关闭智能对齐。
    * 开启之后，如果默认的[setAlignment]导致内容溢出会使用[alignments]提供的位置按顺序查找溢出最小的位置
    */
   fun setSmartAlignments(alignments: List<TargetAlignment>?)

   /**
    * 裁切背景的方向[Directions]（非响应式）
    */
   fun setClipBackgroundDirection(direction: Directions?)
}

/**
 * 要对齐的目标
 */
@Immutable
sealed interface LayerTarget {
   data class Tag(val tag: String?) : LayerTarget
   data class Offset(val offset: IntOffset?) : LayerTarget
}

/**
 * 目标对齐位置
 */
enum class TargetAlignment {
   /** 顶部开始方向对齐 */
   TopStart,
   /** 顶部中间对齐 */
   TopCenter,
   /** 顶部结束方向对齐 */
   TopEnd,
   /** 顶部对齐，不计算x坐标，默认x坐标为0 */
   Top,

   /** 底部开始方向对齐 */
   BottomStart,
   /** 底部中间对齐 */
   BottomCenter,
   /** 底部结束方向对齐 */
   BottomEnd,
   /** 底部对齐，不计算x坐标，默认x坐标为0 */
   Bottom,

   /** 开始方向顶部对齐 */
   StartTop,
   /** 开始方向中间对齐 */
   StartCenter,
   /** 开始方向底部对齐 */
   StartBottom,
   /** 开始方向对齐，不计算y坐标，默认y坐标为0 */
   Start,

   /** 结束方向顶部对齐 */
   EndTop,
   /** 结束方向中间对齐 */
   EndCenter,
   /** 结束方向底部对齐 */
   EndBottom,
   /** 结束方向对齐，不计算y坐标，默认y坐标为0 */
   End,

   /** 中间对齐 */
   Center,
}

/**
 * 目标对齐位置偏移量
 */
@Immutable
sealed interface TargetAlignmentOffset {
   /**
    * 按指定像素[value]偏移
    */
   data class PX(val value: Int) : TargetAlignmentOffset

   /**
    * 按偏移目标大小倍数[value]偏移，例如：1表示向正方向偏移1倍目标大小，-1表示向负方向偏移1倍目标大小
    */
   data class Percent(val value: Float) : TargetAlignmentOffset
}

@Immutable
sealed class Directions(
   private val flag: Int,
) {
   data object Top : Directions(TOP)
   data object Bottom : Directions(BOTTOM)
   data object Start : Directions(START)
   data object End : Directions(END)

   fun hasTop() = TOP and flag != 0
   fun hasBottom() = BOTTOM and flag != 0
   fun hasStart() = START and flag != 0
   fun hasEnd() = END and flag != 0

   operator fun plus(directions: Directions): Directions {
      return Combine(flag or directions.flag)
   }

   private class Combine(direction: Int) : Directions(direction)

   companion object {
      private const val TOP = 1
      private const val BOTTOM = TOP shl 1
      private const val START = TOP shl 2
      private const val END = TOP shl 3
   }
}

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

   private var _smartAlignments: List<TargetAlignment>? = null
   private var _clipBackgroundDirection: Directions? = null

   /** 目标 */
   private var _target: LayerTarget? = null

   /** X方向偏移量 */
   private var _alignmentOffsetX: TargetAlignmentOffset? = null
   /** Y方向偏移量 */
   private var _alignmentOffsetY: TargetAlignmentOffset? = null

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

   private var _tagTargetLayout: LayoutCoordinates? = null
   private val _tagTargetLayoutCallback: (LayoutCoordinates?) -> Unit = {
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

   override fun setSmartAlignments(alignments: List<TargetAlignment>?) {
      _smartAlignments = if (alignments?.isEmpty() == true) {
         listOf(
            TargetAlignment.BottomEnd,
            TargetAlignment.BottomStart,
            TargetAlignment.TopEnd,
            TargetAlignment.TopStart,
         )
      } else {
         alignments
      }
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

   private val _containerLayoutCallback: (LayoutCoordinates?) -> Unit = { layout ->
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
   override fun LayerContent() {
      val uiState by _uiState.collectAsState()
      OffsetBox(
         modifier = Modifier.fillMaxSize(),
         uiState = uiState,
         background = { BackgroundBox() },
         content = { ContentBox() },
      )
   }

   @Composable
   private fun OffsetBox(
      modifier: Modifier = Modifier,
      uiState: UIState,
      background: @Composable () -> Unit,
      content: @Composable () -> Unit,
   ) {
      val state = remember { OffsetBoxState() }.apply {
         this.backgroundState = background
         this.contentState = content
      }

      SubcomposeLayout(modifier) { cs ->
         @Suppress("NAME_SHADOWING")
         val cs = cs.copy(minWidth = 0, minHeight = 0)
         state.measureScope = this

         logMsg {
            """
               layout start -----> isVisible:$isVisibleState
                  alignment:${uiState.alignment}
                  target:${uiState.targetLayout}
                  container:${uiState.containerLayout}
                  cs:$cs
            """.trimIndent()
         }

         val isReady = uiState.targetLayout.isAttached
            && uiState.containerLayout.isAttached

         if (!isVisibleState) {
            return@SubcomposeLayout state.layoutLastVisible(cs).also {
               logMsg { "layout invisible" }
               if (isReady) {
                  setContentVisible(true)
               }
            }
         }

         if (!isReady) {
            return@SubcomposeLayout state.layoutLastVisible(cs).also {
               logMsg { "layout not ready" }
               setContentVisible(false)
            }
         }

         state.layoutFixOverflow(cs, uiState)
      }
   }

   private inner class OffsetBoxState {
      var backgroundState by mutableStateOf<@Composable () -> Unit>({})
      var contentState by mutableStateOf<@Composable () -> Unit>({})

      lateinit var measureScope: SubcomposeMeasureScope

      private var _visibleBackgroundInfo: PlaceInfo? = null
      private var _visibleContentInfo: PlaceInfo? = null

      fun layoutLastVisible(cs: Constraints): MeasureResult {
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

      /**
       * 检查溢出
       */
      fun layoutFixOverflow(cs: Constraints, uiState: UIState): MeasureResult {
         val originalPlaceable = measureContent(cs, slotId = null)
         val originalSize = originalPlaceable.intSize()

         val result = alignTarget(
            alignment = uiState.alignment,
            target = uiState.targetLayout,
            container = uiState.containerLayout,
            contentSize = originalSize,
         ).findBestResult(this@TargetLayerImpl, _smartAlignments)

         val fixOverFlow = result.fixOverFlow(this@TargetLayerImpl)
         val fixOffset = IntOffset(fixOverFlow.x, fixOverFlow.y)
         val fixSize = IntSize(fixOverFlow.width, fixOverFlow.height)

         val contentPlaceable = measureContent(cs.newMax(fixSize))

         val backgroundInfo = backgroundPlaceInfo(
            cs = cs,
            contentOffset = fixOffset,
            contentSize = fixSize,
         )
         val backgroundPlaceable = measureBackground(cs.newMax(backgroundInfo.size))

         logMsg {
            """
               layout fix overflow
                  offset:(${result.x}, ${result.y}) -> $fixOffset
                  size:$originalSize -> $fixSize
                  realSize:${contentPlaceable.intSize()}
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

      private fun layoutFinally(
         cs: Constraints,
         backgroundPlaceable: Placeable,
         backgroundOffset: IntOffset,
         contentPlaceable: Placeable,
         contentOffset: IntOffset,
         saveInfo: Boolean = true,
      ): MeasureResult {
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
            backgroundPlaceable.placeRelative(backgroundOffset, -1f)
            contentPlaceable.placeRelative(contentOffset)
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

      /**
       * 测量内容
       */
      private fun measureContent(constraints: Constraints, slotId: SlotId? = SlotId.Content): Placeable {
         val measurable = measureScope.subcompose(slotId, contentState).let {
            check(it.size == 1)
            it.first()
         }
         return measurable.measure(constraints)
      }

      /**
       * 测量背景
       */
      private fun measureBackground(constraints: Constraints): Placeable {
         val measurable = measureScope.subcompose(SlotId.Background, backgroundState).let {
            check(it.size == 1)
            it.first()
         }
         return measurable.measure(constraints)
      }
   }

   private data class PlaceInfo(
      val offset: IntOffset,
      val size: IntSize,
   )

   private enum class SlotId {
      Content,
      Background,
   }
}

private fun alignTarget(
   alignment: TargetAlignment,
   target: LayoutInfo,
   container: LayoutInfo,
   contentSize: IntSize,
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
   ).toResult()
}

private fun TargetAlignment.toAlignerPosition(): Aligner.Position {
   return when (this) {
      TargetAlignment.TopStart -> Aligner.Position.TopStart
      TargetAlignment.TopCenter -> Aligner.Position.TopCenter
      TargetAlignment.TopEnd -> Aligner.Position.TopEnd
      TargetAlignment.Top -> Aligner.Position.Top

      TargetAlignment.BottomStart -> Aligner.Position.BottomStart
      TargetAlignment.BottomCenter -> Aligner.Position.BottomCenter
      TargetAlignment.BottomEnd -> Aligner.Position.BottomEnd
      TargetAlignment.Bottom -> Aligner.Position.Bottom

      TargetAlignment.StartTop -> Aligner.Position.StartTop
      TargetAlignment.StartCenter -> Aligner.Position.StartCenter
      TargetAlignment.StartBottom -> Aligner.Position.StartBottom
      TargetAlignment.Start -> Aligner.Position.Start

      TargetAlignment.EndTop -> Aligner.Position.EndTop
      TargetAlignment.EndCenter -> Aligner.Position.EndCenter
      TargetAlignment.EndBottom -> Aligner.Position.EndBottom
      TargetAlignment.End -> Aligner.Position.End

      TargetAlignment.Center -> Aligner.Position.Center
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
      is TargetAlignmentOffset.Percent -> {
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

private fun Aligner.Result.findBestResult(
   layer: Layer,
   list: List<TargetAlignment>?,
): Aligner.Result {
   if (list.isNullOrEmpty()) return this

   val overflowDefault = sourceOverflow.totalOverflow()
   if (overflowDefault == 0) return this

   var bestResult = this
   var minOverflow = overflowDefault

   for (item in list) {
      val position = item.toAlignerPosition()
      val newResult = this.input.copy(position = position).toResult()
      val newOverflow = newResult.sourceOverflow.totalOverflow()
      if (newOverflow < minOverflow) {
         minOverflow = newOverflow
         bestResult = newResult
      }
      if (newOverflow == 0) break
   }

   return bestResult.also {
      layer.logMsg {
         "findBestResult ${this.input.position} -> ${bestResult.input.position}"
      }
   }
}

private data class FixOverFlow(
   val x: Int,
   val y: Int,
   val width: Int,
   val height: Int,
)

private fun Aligner.Result.fixOverFlow(layer: Layer): FixOverFlow {
   val position = input.position
   var result = this

   var resultWith = result.input.sourceWidth
   var resultHeight = result.input.sourceHeight

   var count = 0
   while (true) {
      layer.logMsg {
         "checkOverflow ${++count} ($resultWith,$resultHeight)"
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

               val oldWidth = resultWith
               resultWith = oldWidth - overSize

               layer.logMsg {
                  val startLog = if (start > 0) " start:$start" else ""
                  val endLog = if (end > 0) " end:$end" else ""
                  "width overflow:${overSize}${startLog}${endLog} ($oldWidth)->($resultWith)"
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
         if (resultWith <= 0 || resultHeight <= 0) {
            break
         }
         val newInput = result.input.copy(
            sourceWidth = resultWith,
            sourceHeight = resultHeight,
         )
         result = newInput.toResult()
      } else {
         break
      }
   }

   return FixOverFlow(
      x = result.x,
      y = result.y,
      width = resultWith.coerceAtLeast(0),
      height = resultHeight.coerceAtLeast(0),
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