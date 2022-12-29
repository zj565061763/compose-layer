package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.sd.lib.aligner.Aligner
import com.sd.lib.aligner.FAligner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.properties.Delegates

interface TargetLayer : Layer {
    /**
     * 设置目标
     */
    fun setTarget(target: String)

    /**
     * 设置一个目标坐标，如果不为null，则会显示在该坐标附近，此时[setTarget]设置的目标无效。
     */
    fun setTargetOffset(offset: IntOffset?)

    /**
     * 设置坐标转换
     */
    fun setOffsetTransform(transform: OffsetTransform?)

    /**
     * 设置修复溢出的方向[Directions]
     */
    fun setFixOverflowDirection(direction: Directions?)

    /**
     * 设置要裁切背景的方向[Directions]
     */
    fun setClipBackgroundDirection(direction: Directions?)
}

fun interface OffsetTransform {

    fun transform(params: Params): IntOffset

    interface Params {
        /** 坐标 */
        val offset: IntOffset

        /** 内容大小 */
        val contentSize: IntSize

        /** 目标大小 */
        val targetSize: IntSize
    }
}

sealed class Directions(direction: Int) {
    private val _direction = direction

    fun hasTop() = FlagTop and _direction != 0
    fun hasBottom() = FlagBottom and _direction != 0
    fun hasStart() = FlagStart and _direction != 0
    fun hasEnd() = FlagEnd and _direction != 0

    operator fun plus(direction: Directions): Directions {
        val plusDirection = this._direction or direction._direction
        return Plus(plusDirection)
    }

    object Top : Directions(FlagTop)
    object Bottom : Directions(FlagBottom)
    object Start : Directions(FlagStart)
    object End : Directions(FlagEnd)
    object All : Directions(FlagAll)

    private class Plus(direction: Int) : Directions(direction)

    companion object {
        private const val FlagTop = 1
        private const val FlagBottom = FlagTop shl 1
        private const val FlagStart = FlagTop shl 2
        private const val FlagEnd = FlagTop shl 3
        private const val FlagAll = FlagTop or FlagBottom or FlagStart or FlagEnd
    }
}

//---------- Impl ----------

open class FTargetLayer : FLayer(), TargetLayer {
    private val _uiState = MutableStateFlow(
        UiState(
            targetLayout = LayoutInfo(IntSize.Zero, offset = IntOffset.Zero, false),
            containerLayout = LayoutInfo(IntSize.Zero, offset = IntOffset.Zero, false),
        )
    )

    private val _aligner = FAligner()
    private var _offsetTransform: OffsetTransform? = null

    private var _fixOverflowDirectionState by mutableStateOf<Directions?>(null)
    private var _clipBackgroundDirectionState by mutableStateOf<Directions?>(null)
    private var _targetOffsetState by mutableStateOf<IntOffset?>(null)

    private var _target by Delegates.observable("") { _, oldValue, newValue ->
        if (oldValue != newValue) {
            logMsg(isDebug) { "${this@FTargetLayer} target changed $oldValue -> $newValue" }
            _layerContainer?.run {
                unregisterTargetLayoutCallback(oldValue, _targetLayoutCallback)
                registerTargetLayoutCallback(newValue, _targetLayoutCallback)
            }
        }
    }

    private var _targetLayoutCoordinates: LayoutCoordinates? by Delegates.observable(null) { _, _, _ ->
        updateUiState()
    }
    private var _containerLayoutCoordinates: LayoutCoordinates? by Delegates.observable(null) { _, _, _ ->
        updateUiState()
    }

    private val _targetLayoutCallback: (LayoutCoordinates?) -> Unit = {
        _targetLayoutCoordinates = it
    }
    private val _containerLayoutCallback: (LayoutCoordinates?) -> Unit = {
        _containerLayoutCoordinates = it
    }

    final override fun setTarget(target: String) {
        _target = target
    }

    final override fun setTargetOffset(offset: IntOffset?) {
        _targetOffsetState = offset
    }

    final override fun setOffsetTransform(transform: OffsetTransform?) {
        _offsetTransform = transform
    }

    final override fun setFixOverflowDirection(direction: Directions?) {
        _fixOverflowDirectionState = direction
    }

    final override fun setClipBackgroundDirection(direction: Directions?) {
        _clipBackgroundDirectionState = direction
    }

    override fun onAttachInternal() {
        super.onAttachInternal()
        _layerContainer?.run {
            registerContainerLayoutCallback(_containerLayoutCallback)
            registerTargetLayoutCallback(_target, _targetLayoutCallback)
        }
        updateUiState()
    }

    override fun onDetachInternal() {
        super.onDetachInternal()
        _layerContainer?.run {
            unregisterContainerLayoutCallback(_containerLayoutCallback)
            unregisterTargetLayoutCallback(_target, _targetLayoutCallback)
        }
        updateUiState()
    }

    private fun alignTarget(
        position: Layer.Position,
        target: LayoutInfo,
        container: LayoutInfo,
        contentSize: IntSize,
    ): Aligner.Result {
        if (!target.isAttached) error("target is not ready")
        if (!container.isAttached) error("container is not ready")

        val input = Aligner.Input(
            position = position.toAlignerPosition(),
            targetX = target.offset.x,
            targetY = target.offset.y,

            containerX = container.offset.x,
            containerY = container.offset.y,

            targetWidth = target.size.width,
            targetHeight = target.size.height,

            containerWidth = container.size.width,
            containerHeight = container.size.height,

            sourceWidth = contentSize.width,
            sourceHeight = contentSize.height,
        )

        val result = _aligner.align(input)
        return transformResult(result)
    }

    private fun transformResult(result: Aligner.Result): Aligner.Result {
        val transform = _offsetTransform ?: return result

        val params = object : OffsetTransform.Params {
            override val offset: IntOffset get() = IntOffset(result.x, result.y)
            override val contentSize: IntSize get() = IntSize(result.input.sourceWidth, result.input.sourceHeight)
            override val targetSize: IntSize get() = IntSize(result.input.targetWidth, result.input.targetHeight)
        }

        val offset = transform.transform(params)
        return result.copy(
            x = offset.x,
            y = offset.y
        )
    }

    private fun updateUiState() {
        val targetOffset = _targetOffsetState
        val targetLayout = if (targetOffset != null) {
            LayoutInfo(
                size = IntSize.Zero,
                offset = targetOffset,
                isAttached = true,
            )
        } else {
            LayoutInfo(
                size = _targetLayoutCoordinates.size(),
                offset = _targetLayoutCoordinates.offset(),
                isAttached = _targetLayoutCoordinates.isAttached(),
            )
        }

        val containerLayout = LayoutInfo(
            size = _containerLayoutCoordinates.size(),
            offset = _containerLayoutCoordinates.offset(),
            isAttached = _containerLayoutCoordinates.isAttached(),
        )

        _uiState.value = UiState(
            targetLayout = targetLayout,
            containerLayout = containerLayout,
        )
    }

    @Composable
    override fun Content() {
        val uiState by _uiState.collectAsState()
        LayerBox {
            OffsetBox(
                uiState = uiState,
                background = {
                    BackgroundBox()
                },
                content = {
                    ContentBox()
                }
            )
        }
    }

    @Composable
    private fun OffsetBox(
        uiState: UiState,
        background: @Composable () -> Unit,
        content: @Composable () -> Unit,
    ) {
        val state = remember {
            OffsetBoxState(
                background = background,
                content = content,
            )
        }

        SubcomposeLayout(Modifier.fillMaxSize()) { cs ->
            val cs = cs.copy(minWidth = 0, minHeight = 0)
            state.bindMeasureScope(this)

            val isTargetReady = uiState.targetLayout.isAttached
            val isContainerReady = uiState.containerLayout.isAttached
            val isReady = isTargetReady && isContainerReady

            logMsg(isDebug) { "${this@FTargetLayer} layout start isVisible:$isVisibleState isTargetReady:${isTargetReady} isContainerReady:${isContainerReady}" }

            if (!isVisibleState) {
                return@SubcomposeLayout state.layoutLastVisible(cs).also {
                    logMsg(isDebug) { "${this@FTargetLayer} layout invisible" }
                    if (isReady) {
                        setContentVisible(true)
                    }
                }
            }

            if (!isReady) {
                return@SubcomposeLayout state.layoutLastVisible(cs).also {
                    logMsg(isDebug) { "${this@FTargetLayer} layout not ready" }
                    setContentVisible(false)
                }
            }

            val fixOverflowDirection = _fixOverflowDirectionState
                ?: return@SubcomposeLayout state.layoutNoneOverflow(
                    cs = cs,
                    uiState = uiState,
                )

            state.layoutFixOverflow(
                cs = cs,
                uiState = uiState,
                direction = fixOverflowDirection,
            )
        }
    }

    private data class UiState(
        val targetLayout: LayoutInfo,
        val containerLayout: LayoutInfo,
    )

    private data class LayoutInfo(
        val size: IntSize,
        val offset: IntOffset,
        val isAttached: Boolean,
    )

    private inner class OffsetBoxState(
        private val background: @Composable () -> Unit,
        private val content: @Composable () -> Unit,
    ) {
        private var _measureScope: SubcomposeMeasureScope? = null
        private var _visibleBackgroundInfo: PlaceInfo? = null
        private var _visibleContentInfo: PlaceInfo? = null

        val measureScope: SubcomposeMeasureScope
            get() = checkNotNull(_measureScope) { "bindMeasureScope() before this." }

        fun bindMeasureScope(scope: SubcomposeMeasureScope) {
            _measureScope = scope
        }

        fun layoutLastVisible(
            cs: Constraints,
        ): MeasureResult {
            val backgroundInfo = _visibleBackgroundInfo ?: PlaceInfo(0, 0, cs.maxWidth, cs.maxHeight)
            val backgroundPlaceable = measureBackground(
                constraints = cs.copy(maxWidth = backgroundInfo.width, maxHeight = backgroundInfo.height),
            )

            val contentInfo = _visibleContentInfo ?: PlaceInfo(0, 0, cs.maxWidth, cs.maxHeight)
            val contentPlaceable = measureContent(
                constraints = cs.copy(maxWidth = contentInfo.width, maxHeight = contentInfo.height),
            )

            return layoutFinally(
                cs = cs,
                backgroundPlaceable = backgroundPlaceable,
                backgroundX = backgroundInfo.x,
                backgroundY = backgroundInfo.y,
                contentPlaceable = contentPlaceable,
                contentX = contentInfo.x,
                contentY = contentInfo.y,
                saveVisibleInfo = false,
            )
        }

        fun layoutNoneOverflow(
            cs: Constraints,
            uiState: UiState,
        ): MeasureResult {
            val contentPlaceable = measureContent(cs)
            val result = alignTarget(
                position = positionState,
                target = uiState.targetLayout,
                container = uiState.containerLayout,
                contentSize = IntSize(contentPlaceable.width, contentPlaceable.height),
            ).let {
                findBestResult(it)
            }

            val x = result.x
            val y = result.y

            val backgroundInfo = backgroundPlaceInfo(
                cs = cs,
                contentOffset = IntOffset(x, y),
                contentSize = IntSize(contentPlaceable.width, contentPlaceable.height),
                direction = _clipBackgroundDirectionState,
            )
            val backgroundPlaceable = measureBackground(
                constraints = cs.copy(maxWidth = backgroundInfo.width, maxHeight = backgroundInfo.height),
            )

            logMsg(isDebug) { "${this@FTargetLayer} layout none overflow ($x, $y)" }
            return layoutFinally(
                cs = cs,
                backgroundPlaceable = backgroundPlaceable,
                backgroundX = backgroundInfo.x,
                backgroundY = backgroundInfo.y,
                contentPlaceable = contentPlaceable,
                contentX = x,
                contentY = y,
            )
        }

        fun layoutFixOverflow(
            cs: Constraints,
            uiState: UiState,
            direction: Directions,
        ): MeasureResult {
            val originalPlaceable = measureContent(cs, slotId = null)
            val result = alignTarget(
                position = positionState,
                target = uiState.targetLayout,
                container = uiState.containerLayout,
                contentSize = IntSize(originalPlaceable.width, originalPlaceable.height),
            ).let {
                findBestResult(it)
            }

            var x = result.x
            var y = result.y

            val (fixedConstraints, fixedResult) = checkOverflow(result, cs, direction)
            val contentPlaceable = if (fixedConstraints != null) {
                measureContent(fixedConstraints).also { placeable ->
                    logMsg(isDebug) {
                        "${this@FTargetLayer} fix overflow size:(${originalPlaceable.width}, ${originalPlaceable.height})->(${placeable.width}, ${placeable.height}) offset:($x, $y)->(${fixedResult.x}, ${fixedResult.y})"
                    }
                    x = fixedResult.x
                    y = fixedResult.y
                }
            } else {
                measureContent(cs)
            }

            val backgroundInfo = backgroundPlaceInfo(
                cs = cs,
                contentOffset = IntOffset(x, y),
                contentSize = IntSize(contentPlaceable.width, contentPlaceable.height),
                direction = _clipBackgroundDirectionState,
            )
            val backgroundPlaceable = measureBackground(
                constraints = cs.copy(maxWidth = backgroundInfo.width, maxHeight = backgroundInfo.height),
            )

            logMsg(isDebug) { "${this@FTargetLayer} layout fix overflow ($x, $y)" }
            return layoutFinally(
                cs = cs,
                backgroundPlaceable = backgroundPlaceable,
                backgroundX = backgroundInfo.x,
                backgroundY = backgroundInfo.y,
                contentPlaceable = contentPlaceable,
                contentX = x,
                contentY = y,
            )
        }

        private fun layoutFinally(
            cs: Constraints,
            backgroundPlaceable: Placeable?,
            backgroundX: Int,
            backgroundY: Int,
            contentPlaceable: Placeable,
            contentX: Int,
            contentY: Int,
            saveVisibleInfo: Boolean = true,
        ): MeasureResult {
            return measureScope.layout(cs.maxWidth, cs.maxHeight) {
                if (saveVisibleInfo) {
                    _visibleBackgroundInfo = backgroundPlaceable?.let {
                        PlaceInfo(
                            x = backgroundX,
                            y = backgroundY,
                            width = it.width,
                            height = it.height,
                        )
                    }
                    _visibleContentInfo = PlaceInfo(
                        x = contentX,
                        y = contentY,
                        width = contentPlaceable.width,
                        height = contentPlaceable.height,
                    )
                }
                backgroundPlaceable?.placeRelative(backgroundX, backgroundY, -1f)
                contentPlaceable.placeRelative(contentX, contentY)
            }
        }

        private fun backgroundPlaceInfo(
            cs: Constraints,
            contentOffset: IntOffset,
            contentSize: IntSize,
            direction: Directions?,
        ): PlaceInfo {
            if (direction == null || contentSize.width <= 0 || contentSize.height <= 0) {
                return PlaceInfo(
                    x = 0,
                    y = 0,
                    width = cs.maxWidth,
                    height = cs.maxHeight,
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
                    logMsg(isDebug) { "${this@FTargetLayer} clip background top:$it" }
                }
                y = contentY
            }
            if (direction.hasBottom()) {
                height -= (cs.maxHeight - contentY - contentSize.height).also {
                    logMsg(isDebug) { "${this@FTargetLayer} clip background bottom:$it" }
                }
            }

            if (direction.hasStart()) {
                width -= contentX.also {
                    logMsg(isDebug) { "${this@FTargetLayer} clip background start:$it" }
                }
                x = contentX
            }
            if (direction.hasEnd()) {
                width -= (cs.maxWidth - contentX - contentSize.width).also {
                    logMsg(isDebug) { "${this@FTargetLayer} clip background end:$it" }
                }
            }

            return PlaceInfo(
                x = x,
                y = y,
                width = width.coerceAtLeast(0),
                height = height.coerceAtLeast(0),
            )
        }

        private fun findBestResult(result: Aligner.Result): Aligner.Result {
            val targetOffset = _targetOffsetState ?: return result

            val overflowSizeDefault = result.sourceOverflow.totalOverflow()
            if (overflowSizeDefault == 0) {
                return result
            }

            val preferPosition = mutableListOf(
                Aligner.Position.BottomEnd,
                Aligner.Position.BottomStart,
                Aligner.Position.TopEnd,
                Aligner.Position.TopStart,
            ).apply {
                remove(result.input.position)
            }

            var bestResult = result
            var minOverflow = overflowSizeDefault
            var bestPosition = result.input.position

            for (position in preferPosition) {
                val newResult = _aligner.align(
                    result.input.copy(
                        position = position,
                        targetX = targetOffset.x,
                        targetY = targetOffset.y,
                        targetWidth = 0,
                        targetHeight = 0,
                    )
                )

                val newOverflow = newResult.sourceOverflow.totalOverflow()
                if (newOverflow < minOverflow) {
                    minOverflow = newOverflow
                    bestResult = newResult
                    bestPosition = position
                    if (newOverflow == 0) break
                }
            }

            logMsg(isDebug) {
                "${this@FTargetLayer} findBestResult position:$bestPosition (${bestResult.x}, ${bestResult.y})"
            }

            return bestResult
        }

        private fun checkOverflow(
            result: Aligner.Result,
            cs: Constraints,
            direction: Directions,
        ): Pair<Constraints?, Aligner.Result> {
            var resultConstraints: Constraints? = null

            var cs = cs
            var result = result

            var count = 0
            while (true) {
                var hasOverflow = false

                // 检查是否溢出
                with(result.sourceOverflow) {
                    // Vertical
                    kotlin.run {
                        var overSize = 0
                        var isTopOverflow = false
                        var isBottomOverflow = false

                        if (direction.hasTop()) {
                            if (top > 0) {
                                overSize += top
                                isTopOverflow = true
                                logMsg(isDebug) { "${this@FTargetLayer} top overflow:$top" }
                            }
                        }

                        if (direction.hasBottom()) {
                            if (bottom > 0) {
                                overSize += bottom
                                isBottomOverflow = true
                                logMsg(isDebug) { "${this@FTargetLayer} bottom overflow:$bottom" }
                            }
                        }

                        if (overSize > 0) {
                            hasOverflow = true

                            /**
                             * 居中对齐的时候，如果只有一边溢出，则需要减去双倍溢出的值
                             */
                            if (positionState.isCenterVertical()) {
                                if (isTopOverflow && isBottomOverflow) {
                                } else {
                                    overSize *= 2
                                }
                            }

                            val maxSize = (cs.maxHeight - overSize).coerceAtLeast(1)
                            cs = cs.copy(maxHeight = maxSize).also {
                                resultConstraints = it
                            }
                        }
                    }

                    // Horizontal
                    kotlin.run {
                        var overSize = 0
                        var isStartOverflow = false
                        var isEndOverflow = false

                        if (direction.hasStart()) {
                            if (start > 0) {
                                overSize += start
                                isStartOverflow = true
                                logMsg(isDebug) { "${this@FTargetLayer} start overflow:$start" }
                            }
                        }

                        if (direction.hasEnd()) {
                            if (end > 0) {
                                overSize += end
                                isEndOverflow = true
                                logMsg(isDebug) { "${this@FTargetLayer} end overflow:$end" }
                            }
                        }

                        if (overSize > 0) {
                            hasOverflow = true

                            /**
                             * 居中对齐的时候，如果只有一边溢出，则需要减去双倍溢出的值
                             */
                            if (positionState.isCenterHorizontal()) {
                                if (isStartOverflow && isEndOverflow) {
                                } else {
                                    overSize *= 2
                                }
                            }

                            val maxSize = (cs.maxWidth - overSize).coerceAtLeast(1)
                            cs = cs.copy(maxWidth = maxSize).also {
                                resultConstraints = it
                            }
                        }
                    }
                }

                if (hasOverflow) {
                    val newInput = result.input.copy(
                        sourceWidth = cs.maxWidth,
                        sourceHeight = cs.maxHeight,
                    )
                    result = _aligner.align(newInput)
                } else {
                    break
                }

                logMsg(isDebug) { "${this@FTargetLayer} checkOverflow -----> ${++count}" }
            }

            return Pair(resultConstraints, result)
        }

        private fun measureContent(
            constraints: Constraints,
            slotId: SlotId? = SlotId.Content,
        ): Placeable {
            val measurable = measureScope.subcompose(slotId, content).let {
                check(it.size == 1)
                it.first()
            }
            return measurable.measure(constraints)
        }

        private fun measureBackground(constraints: Constraints): Placeable? {
            val measurable = measureScope.subcompose(SlotId.Background, background).let {
                if (it.isNotEmpty()) check(it.size == 1)
                it.firstOrNull()
            }
            return measurable?.measure(constraints)
        }
    }

    private data class PlaceInfo(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
    )

    private enum class SlotId {
        Content,
        Background,
    }
}

private fun Layer.Position.toAlignerPosition(): Aligner.Position {
    return when (this) {
        Layer.Position.TopStart -> Aligner.Position.TopStart
        Layer.Position.TopCenter -> Aligner.Position.TopCenter
        Layer.Position.TopEnd -> Aligner.Position.TopEnd
        Layer.Position.Top -> Aligner.Position.Top

        Layer.Position.BottomStart -> Aligner.Position.BottomStart
        Layer.Position.BottomCenter -> Aligner.Position.BottomCenter
        Layer.Position.BottomEnd -> Aligner.Position.BottomEnd
        Layer.Position.Bottom -> Aligner.Position.Bottom

        Layer.Position.StartTop -> Aligner.Position.StartTop
        Layer.Position.StartCenter -> Aligner.Position.StartCenter
        Layer.Position.StartBottom -> Aligner.Position.StartBottom
        Layer.Position.Start -> Aligner.Position.Start

        Layer.Position.EndTop -> Aligner.Position.EndTop
        Layer.Position.EndCenter -> Aligner.Position.EndCenter
        Layer.Position.EndBottom -> Aligner.Position.EndBottom
        Layer.Position.End -> Aligner.Position.End

        Layer.Position.Center -> Aligner.Position.Center
    }
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

private fun Aligner.Overflow.totalOverflow(): Int {
    var size = 0
    if (top > 0) size += top
    if (bottom > 0) size += bottom
    if (start > 0) size += start
    if (end > 0) size += end
    return size
}

private fun Layer.Position.isCenterVertical(): Boolean = when (this) {
    Layer.Position.StartCenter,
    Layer.Position.EndCenter,
    Layer.Position.Center,
    -> true
    else -> false
}

private fun Layer.Position.isCenterHorizontal(): Boolean = when (this) {
    Layer.Position.TopCenter,
    Layer.Position.BottomCenter,
    Layer.Position.Center,
    -> true
    else -> false
}