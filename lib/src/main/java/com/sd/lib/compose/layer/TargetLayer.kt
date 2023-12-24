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
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.sd.lib.aligner.Aligner
import com.sd.lib.aligner.FAligner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt
import kotlin.properties.Delegates

interface TargetLayer : Layer {
    /**
     * 设置目标
     */
    fun setTarget(target: String?)

    /**
     * 设置目标坐标
     */
    fun setTarget(offset: IntOffset?)

    /**
     * 设置坐标转换（X方向）
     */
    fun setXOffset(offset: TransformOffset?)

    /**
     * 设置坐标转换（Y方向）
     */
    fun setYOffset(offset: TransformOffset?)

    /**
     * 设置是否修复溢出，默认true
     */
    fun setFixOverflow(fixOverflow: Boolean)

    /**
     * 设置是否查找最佳的显示位置，默认false
     */
    fun setFindBestPosition(findBestPosition: Boolean)

    /**
     * 设置要裁切背景的方向[Directions]
     */
    fun setClipBackgroundDirection(direction: Directions?)
}

sealed interface TransformOffset {
    data class PX(val value: Int) : TransformOffset
    data class Percent(val value: Float, val type: Type) : TransformOffset

    enum class Type {
        Target,
        Content,
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

    data object Top : Directions(FlagTop)
    data object Bottom : Directions(FlagBottom)
    data object Start : Directions(FlagStart)
    data object End : Directions(FlagEnd)
    data object All : Directions(FlagAll)

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

internal class TargetLayerImpl : LayerImpl(), TargetLayer {

    @Immutable
    private data class UiState(
        val targetLayout: LayoutInfo,
        val containerLayout: LayoutInfo,
    )

    @Immutable
    private data class LayoutInfo(
        val size: IntSize,
        val offset: IntOffset,
        val isAttached: Boolean,
    )

    private val _uiState = MutableStateFlow(
        UiState(
            targetLayout = LayoutInfo(size = IntSize.Zero, offset = IntOffset.Zero, isAttached = false),
            containerLayout = LayoutInfo(size = IntSize.Zero, offset = IntOffset.Zero, isAttached = false),
        )
    )

    private val _aligner = FAligner()

    private var _xOffset: TransformOffset? = null
    private var _yOffset: TransformOffset? = null

    private var _fixOverflowState by mutableStateOf(true)
    private var _findBestPositionState by mutableStateOf(false)

    private var _clipBackgroundDirectionState by mutableStateOf<Directions?>(null)
    private var _targetOffsetState by mutableStateOf<IntOffset?>(null)

    private var _target by Delegates.observable("") { _, oldValue, newValue ->
        if (oldValue != newValue) {
            logMsg(isDebug) { "${this@TargetLayerImpl} target changed ($oldValue) -> ($newValue)" }
            layerContainer?.run {
                unregisterTargetLayoutCallback(oldValue, _targetLayoutCallback)
                registerTargetLayoutCallback(newValue, _targetLayoutCallback)
            }
        }
    }

    private val _targetLayoutCallback: (LayoutCoordinates?) -> Unit = { _targetLayout = it }
    private val _containerLayoutCallback: (LayoutCoordinates?) -> Unit = { _containerLayout = it }

    private var _targetLayout: LayoutCoordinates? by Delegates.observable(null) { _, _, newValue ->
        logMsg(isDebug) { "${this@TargetLayerImpl} target layout changed $newValue" }
        updateUiState()
    }
    private var _containerLayout: LayoutCoordinates? by Delegates.observable(null) { _, _, newValue ->
        logMsg(isDebug) { "${this@TargetLayerImpl} container layout changed $newValue" }
        updateUiState()
    }

    override fun setTarget(target: String?) {
        _target = target ?: ""
    }

    override fun setTarget(offset: IntOffset?) {
        _targetOffsetState = offset
    }

    override fun setXOffset(offset: TransformOffset?) {
        _xOffset = offset
    }

    override fun setYOffset(offset: TransformOffset?) {
        _yOffset = offset
    }

    override fun setFixOverflow(fixOverflow: Boolean) {
        _fixOverflowState = fixOverflow
    }

    override fun setFindBestPosition(findBestPosition: Boolean) {
        _findBestPositionState = findBestPosition
    }

    override fun setClipBackgroundDirection(direction: Directions?) {
        _clipBackgroundDirectionState = direction
    }

    override fun onAttach() {
        super.onAttach()
        layerContainer?.run {
            registerContainerLayoutCallback(_containerLayoutCallback)
            registerTargetLayoutCallback(_target, _targetLayoutCallback)
        }
    }

    override fun onDetach() {
        super.onDetach()
        layerContainer?.run {
            unregisterContainerLayoutCallback(_containerLayoutCallback)
            unregisterTargetLayoutCallback(_target, _targetLayoutCallback)
        }
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
        val transformResult = transformResult(result)
        return findBestPosition(transformResult, target)
    }

    private fun transformResult(result: Aligner.Result): Aligner.Result {
        val xOffset = _xOffset
        val yOffset = _yOffset
        if (xOffset == null && yOffset == null) {
            return result
        }

        val x = xOffset.pxValue { type ->
            when (type) {
                TransformOffset.Type.Target -> result.input.targetWidth
                TransformOffset.Type.Content -> result.input.sourceWidth
            }
        }

        val y = yOffset.pxValue { type ->
            when (type) {
                TransformOffset.Type.Target -> result.input.targetHeight
                TransformOffset.Type.Content -> result.input.sourceHeight
            }
        }

        if (x == 0 && y == 0) {
            return result
        }

        val newInput = result.input.run {
            copy(
                targetX = this.targetX + x,
                targetY = this.targetY + y,
            )
        }

        return _aligner.align(newInput)
    }

    private fun findBestPosition(result: Aligner.Result, targetLayout: LayoutInfo): Aligner.Result {
        if (!_findBestPositionState) return result

        val overflowDefault = result.sourceOverflow.totalOverflow()
        if (overflowDefault == 0) return result

        val preferPosition = mutableListOf(
            Aligner.Position.BottomEnd,
            Aligner.Position.BottomStart,
            Aligner.Position.TopEnd,
            Aligner.Position.TopStart,
        ).apply {
            remove(result.input.position)
        }

        var bestResult = result
        var minOverflow = overflowDefault
        var bestPosition = result.input.position

        for (position in preferPosition) {
            val newResult = _aligner.align(
                result.input.copy(
                    position = position,
                    targetX = targetLayout.offset.x,
                    targetY = targetLayout.offset.y,
                    targetWidth = targetLayout.size.width,
                    targetHeight = targetLayout.size.height,
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
            "${this@TargetLayerImpl} findBestPosition:$bestPosition"
        }

        return bestResult
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
                size = _targetLayout.size(),
                offset = _targetLayout.offset(),
                isAttached = _targetLayout.isAttached(),
            )
        }

        val containerLayout = LayoutInfo(
            size = _containerLayout.size(),
            offset = _containerLayout.offset(),
            isAttached = _containerLayout.isAttached(),
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
                background = { BackgroundBox() },
                content = { ContentBox() },
            )
        }
    }

    @Composable
    private fun OffsetBox(
        uiState: UiState,
        background: @Composable () -> Unit,
        content: @Composable () -> Unit,
    ) {
        val state = remember { OffsetBoxState() }.apply {
            this.backgroundState = background
            this.contentState = content
        }

        SubcomposeLayout(Modifier.fillMaxSize()) { cs ->
            val cs = cs.copy(minWidth = 0, minHeight = 0)
            state.measureScope = this

            val isTargetReady = uiState.targetLayout.isAttached
            val isContainerReady = uiState.containerLayout.isAttached
            val isReady = isTargetReady && isContainerReady

            logMsg(isDebug) { "${this@TargetLayerImpl} layout start isVisible:$isVisibleState isTargetReady:${isTargetReady} isContainerReady:${isContainerReady}" }

            if (!isVisibleState) {
                return@SubcomposeLayout state.layoutLastVisible(cs).also {
                    logMsg(isDebug) { "${this@TargetLayerImpl} layout invisible" }
                    if (isReady) {
                        setContentVisible(true)
                    }
                }
            }

            if (!isReady) {
                return@SubcomposeLayout state.layoutLastVisible(cs).also {
                    logMsg(isDebug) { "${this@TargetLayerImpl} layout not ready" }
                    setContentVisible(false)
                }
            }

            if (_fixOverflowState) {
                state.layoutFixOverflow(
                    cs = cs,
                    uiState = uiState,
                )
            } else {
                state.layoutNoneOverflow(
                    cs = cs,
                    uiState = uiState,
                )
            }
        }
    }

    private inner class OffsetBoxState {

        var backgroundState by mutableStateOf<@Composable () -> Unit>({})
        var contentState by mutableStateOf<@Composable () -> Unit>({})

        lateinit var measureScope: SubcomposeMeasureScope

        private var _visibleBackgroundInfo: PlaceInfo? = null
        private var _visibleContentInfo: PlaceInfo? = null

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
            )

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

            logMsg(isDebug) { "${this@TargetLayerImpl} layout none overflow ($x,$y) (${contentPlaceable.width},${contentPlaceable.height})" }
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
        ): MeasureResult {
            val originalPlaceable = measureContent(cs, slotId = null)
            val result = alignTarget(
                position = positionState,
                target = uiState.targetLayout,
                container = uiState.containerLayout,
                contentSize = IntSize(originalPlaceable.width, originalPlaceable.height),
            )

            val fixOverFlow = fixOverFlow(result)

            val x = fixOverFlow.x
            val y = fixOverFlow.y
            val width = fixOverFlow.width
            val height = fixOverFlow.height

            val contentPlaceable = measureContent(cs.copy(maxWidth = width, maxHeight = height))

            logMsg(isDebug) {
                "${this@TargetLayerImpl} fixOverFlow \n" +
                        "offset:(${result.x}, ${result.y})->(${x}, ${y}) \n" +
                        "size:(${originalPlaceable.width}, ${originalPlaceable.height})->(${width}, ${height}) \n" +
                        "realSize:(${contentPlaceable.width},${contentPlaceable.height})"
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

            logMsg(isDebug) { "${this@TargetLayerImpl} layout fix overflow" }
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
                    logMsg(isDebug) { "${this@TargetLayerImpl} clip background top:$it" }
                }
                y = contentY
            }
            if (direction.hasBottom()) {
                height -= (cs.maxHeight - contentY - contentSize.height).also {
                    logMsg(isDebug) { "${this@TargetLayerImpl} clip background bottom:$it" }
                }
            }

            if (direction.hasStart()) {
                width -= contentX.also {
                    logMsg(isDebug) { "${this@TargetLayerImpl} clip background start:$it" }
                }
                x = contentX
            }
            if (direction.hasEnd()) {
                width -= (cs.maxWidth - contentX - contentSize.width).also {
                    logMsg(isDebug) { "${this@TargetLayerImpl} clip background end:$it" }
                }
            }

            return PlaceInfo(
                x = x,
                y = y,
                width = width.coerceAtLeast(0),
                height = height.coerceAtLeast(0),
            )
        }


        private fun fixOverFlow(result: Aligner.Result): FixOverFlow {
            var result = result

            var resultWith = result.input.sourceWidth
            var resultHeight = result.input.sourceHeight

            var count = 0
            while (true) {
                var hasOverflow = false
                logMsg(isDebug) { "${this@TargetLayerImpl} checkOverflow -----> ${++count} ($resultWith,$resultHeight)" }

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
                            if (positionState.isCenterHorizontal()) {
                                if (isStartOverflow && isEndOverflow) {
                                } else {
                                    overSize *= 2
                                }
                            }

                            val oldWidth = resultWith
                            resultWith = oldWidth - overSize
                            logMsg(isDebug) {
                                val startLog = if (start > 0) " start:$start" else ""
                                val endLog = if (end > 0) " end:$end" else ""
                                "${this@TargetLayerImpl} width overflow:${overSize}${startLog}${endLog} ($oldWidth) -> ($resultWith)"
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
                            if (positionState.isCenterVertical()) {
                                if (isTopOverflow && isBottomOverflow) {
                                } else {
                                    overSize *= 2
                                }
                            }

                            val oldHeight = resultHeight
                            resultHeight = oldHeight - overSize
                            logMsg(isDebug) {
                                val topLog = if (top > 0) " top:$top" else ""
                                val bottomLog = if (bottom > 0) " bottom:$bottom" else ""
                                "${this@TargetLayerImpl} height overflow:${overSize}${topLog}${bottomLog} ($oldHeight) -> ($resultHeight)"
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
                    result = _aligner.align(newInput)
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

        private fun measureContent(
            constraints: Constraints,
            slotId: SlotId? = SlotId.Content,
        ): Placeable {
            val measurable = measureScope.subcompose(slotId, contentState).let {
                check(it.size == 1)
                it.first()
            }
            return measurable.measure(constraints)
        }

        private fun measureBackground(constraints: Constraints): Placeable? {
            val measurable = measureScope.subcompose(SlotId.Background, backgroundState).let {
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

    private data class FixOverFlow(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
    )
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

private inline fun TransformOffset?.pxValue(typeValue: (TransformOffset.Type) -> Int): Int {
    return when (val offset = this) {
        null -> 0
        is TransformOffset.PX -> offset.value
        is TransformOffset.Percent -> {
            val typedValue = typeValue(offset.type)
            val px = typedValue * offset.value
            if (px.isInfinite()) 0 else px.roundToInt()
        }
    }
}