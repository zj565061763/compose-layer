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
     * 设置目标X方向偏移量
     */
    fun setTargetOffsetX(offset: TargetOffset?)

    /**
     * 设置目标Y方向偏移量
     */
    fun setTargetOffsetY(offset: TargetOffset?)

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

sealed interface TargetOffset {
    /**
     * 偏移指定像素
     */
    data class PX(val value: Int) : TargetOffset

    /**
     * 偏移目标大小的倍数，例如：1表示向正方向偏移1倍目标的大小，-1表示向负方向偏移1倍目标的大小
     */
    data class Percent(val value: Float) : TargetOffset
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

@Immutable
private data class UIState(
    val targetLayout: LayoutInfo,
    val containerLayout: LayoutInfo,
)

@Immutable
private data class LayoutInfo(
    val size: IntSize,
    val offset: IntOffset,
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
            targetLayout = EmptyLayoutInfo,
            containerLayout = EmptyLayoutInfo,
        )
    )

    private var _fixOverflow = true
    private var _findBestPosition = false
    private var _clipBackgroundDirection: Directions? = null

    private var _targetOffset by Delegates.observable<IntOffset?>(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            updateTargetLayout()
        }
    }
    private var _targetOffsetX by Delegates.observable<TargetOffset?>(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            updateTargetLayout()
        }
    }
    private var _targetOffsetY by Delegates.observable<TargetOffset?>(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            updateTargetLayout()
        }
    }

    private var _target by Delegates.observable("") { _, oldValue, newValue ->
        if (oldValue != newValue) {
            logMsg { "target changed ($oldValue) -> ($newValue)" }
            layerContainer?.run {
                unregisterTargetLayoutCallback(oldValue, _targetLayoutCallback)
                registerTargetLayoutCallback(newValue, _targetLayoutCallback)
            }
        }
    }

    private val _targetLayoutCallback: (LayoutCoordinates?) -> Unit = { _targetLayout = it }
    private var _targetLayout: LayoutCoordinates? by Delegates.observable(null) { _, _, _ ->
        updateTargetLayout()
    }

    override fun setTarget(target: String?) {
        _target = target ?: ""
    }

    override fun setTarget(offset: IntOffset?) {
        _targetOffset = offset
    }

    override fun setTargetOffsetX(offset: TargetOffset?) {
        _targetOffsetX = offset
    }

    override fun setTargetOffsetY(offset: TargetOffset?) {
        _targetOffsetY = offset
    }

    override fun setFixOverflow(fixOverflow: Boolean) {
        _fixOverflow = fixOverflow
    }

    override fun setFindBestPosition(findBestPosition: Boolean) {
        _findBestPosition = findBestPosition
    }

    override fun setClipBackgroundDirection(direction: Directions?) {
        _clipBackgroundDirection = direction
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

    private val _containerLayoutCallback: (LayoutCoordinates?) -> Unit = { layout ->
        _uiState.update {
            it.copy(containerLayout = layout.toLayoutInfo())
        }
    }

    /**
     * 更新目标布局信息
     */
    private fun updateTargetLayout() {
        val layout = _targetOffset?.toLayoutInfo() ?: _targetLayout.toLayoutInfo()
        _uiState.update { it.copy(targetLayout = layout) }
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
        uiState: UIState,
        background: @Composable () -> Unit,
        content: @Composable () -> Unit,
    ) {
        val state = remember { OffsetBoxState() }.apply {
            this.backgroundState = background
            this.contentState = content
        }

        SubcomposeLayout(Modifier.fillMaxSize()) { cs ->
            @Suppress("NAME_SHADOWING")
            val cs = cs.copy(minWidth = 0, minHeight = 0)
            state.measureScope = this

            val isTargetReady = uiState.targetLayout.isAttached
            val isContainerReady = uiState.containerLayout.isAttached
            val isReady = isTargetReady && isContainerReady

            logMsg {
                "layout start isVisible:$isVisibleState isTargetReady:${isTargetReady} isContainerReady:${isContainerReady}"
            }

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

            if (_fixOverflow) {
                state.layoutFixOverflow(cs, uiState)
            } else {
                state.layoutNoneOverflow(cs, uiState)
            }
        }
    }

    private inner class OffsetBoxState {

        var backgroundState by mutableStateOf<@Composable () -> Unit>({})
        var contentState by mutableStateOf<@Composable () -> Unit>({})

        lateinit var measureScope: SubcomposeMeasureScope

        private var _visibleBackgroundInfo: PlaceInfo? = null
        private var _visibleContentInfo: PlaceInfo? = null

        fun layoutLastVisible(cs: Constraints): MeasureResult {
            val backgroundInfo = _visibleBackgroundInfo ?: PlaceInfo(0, 0, cs.maxWidth, cs.maxHeight)
            val backgroundPlaceable = measureBackground(cs.newMax(backgroundInfo.width, backgroundInfo.height))

            val contentInfo = _visibleContentInfo ?: PlaceInfo(0, 0, cs.maxWidth, cs.maxHeight)
            val contentPlaceable = measureContent(cs.newMax(contentInfo.width, contentInfo.height))

            return layoutFinally(
                cs = cs,
                backgroundPlaceable = backgroundPlaceable,
                backgroundX = backgroundInfo.x,
                backgroundY = backgroundInfo.y,
                contentPlaceable = contentPlaceable,
                contentX = contentInfo.x,
                contentY = contentInfo.y,
                saveInfo = false,
            )
        }

        fun layoutNoneOverflow(cs: Constraints, uiState: UIState): MeasureResult {
            val contentPlaceable = measureContent(cs)

            val result = alignTarget(
                position = positionState,
                target = uiState.targetLayout,
                container = uiState.containerLayout,
                contentSize = IntSize(contentPlaceable.width, contentPlaceable.height),
            ).let {
                if (_findBestPosition) it.findBestPosition() else it
            }

            val offset = IntOffset(result.x, result.y)
            val size = IntSize(contentPlaceable.width, contentPlaceable.height)

            val backgroundInfo = backgroundPlaceInfo(
                cs = cs,
                contentOffset = offset,
                contentSize = size,
            )
            val backgroundPlaceable = measureBackground(cs.newMax(backgroundInfo.width, backgroundInfo.height))

            logMsg {
                "layout none overflow (${offset.x},${offset.y}) (${size.width},${size.height})"
            }

            return layoutFinally(
                cs = cs,
                backgroundPlaceable = backgroundPlaceable,
                backgroundX = backgroundInfo.x,
                backgroundY = backgroundInfo.y,
                contentPlaceable = contentPlaceable,
                contentX = offset.x,
                contentY = offset.y,
            )
        }

        fun layoutFixOverflow(cs: Constraints, uiState: UIState): MeasureResult {
            val originalPlaceable = measureContent(cs, slotId = null)

            val result = alignTarget(
                position = positionState,
                target = uiState.targetLayout,
                container = uiState.containerLayout,
                contentSize = IntSize(originalPlaceable.width, originalPlaceable.height),
            ).let {
                if (_findBestPosition) it.findBestPosition() else it
            }

            val fixOverFlow = result.fixOverFlow(this@TargetLayerImpl)

            val x = fixOverFlow.x
            val y = fixOverFlow.y
            val width = fixOverFlow.width
            val height = fixOverFlow.height

            val contentPlaceable = measureContent(cs.newMax(width, height))

            logMsg {
                "fixOverFlow \n" +
                        "offset:(${result.x}, ${result.y})->(${x}, ${y}) \n" +
                        "size:(${originalPlaceable.width}, ${originalPlaceable.height})->(${width}, ${height}) \n" +
                        "realSize:(${contentPlaceable.width},${contentPlaceable.height})"
            }

            val backgroundInfo = backgroundPlaceInfo(
                cs = cs,
                contentOffset = IntOffset(x, y),
                contentSize = IntSize(contentPlaceable.width, contentPlaceable.height),
            )
            val backgroundPlaceable = measureBackground(cs.newMax(backgroundInfo.width, backgroundInfo.height))

            logMsg { "layout fix overflow" }
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
            saveInfo: Boolean = true,
        ): MeasureResult {
            return measureScope.layout(cs.maxWidth, cs.maxHeight) {
                if (saveInfo) {
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
        ): PlaceInfo {
            val direction = _clipBackgroundDirection
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
                x = x,
                y = y,
                width = width.coerceAtLeast(0),
                height = height.coerceAtLeast(0),
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
}

private fun alignTarget(
    position: Layer.Position,
    target: LayoutInfo,
    container: LayoutInfo,
    contentSize: IntSize,
): Aligner.Result {
    check(target.isAttached)
    check(container.isAttached)
    return Aligner.Input(
        position = position.toAlignerPosition(),

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

private fun IntOffset?.toLayoutInfo(): LayoutInfo? {
    if (this == null) return null
    return LayoutInfo(
        size = IntSize.Zero,
        offset = this,
        isAttached = true,
    )
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

private fun TargetOffset?.pxValue(targetSize: Int): Int {
    return when (val offset = this) {
        null -> 0
        is TargetOffset.PX -> offset.value
        is TargetOffset.Percent -> {
            val px = targetSize * offset.value
            if (px.isInfinite()) 0 else px.roundToInt()
        }
    }
}

private fun Constraints.newMax(width: Int, height: Int): Constraints {
    return this.copy(maxWidth = width, maxHeight = height)
}

private fun Aligner.Result.findBestPosition(
    list: MutableList<Aligner.Position> = mutableListOf(
        Aligner.Position.BottomEnd,
        Aligner.Position.BottomStart,
        Aligner.Position.TopEnd,
        Aligner.Position.TopStart,
    ),
): Aligner.Result {
    val overflowDefault = sourceOverflow.totalOverflow()
    if (overflowDefault == 0) return this

    val listPosition = list.apply { remove(input.position) }
    if (listPosition.isEmpty()) return this

    var bestResult = this
    var minOverflow = overflowDefault

    for (position in listPosition) {
        val newResult = input.copy(position = position).toResult()
        val newOverflow = newResult.sourceOverflow.totalOverflow()
        if (newOverflow < minOverflow) {
            minOverflow = newOverflow
            bestResult = newResult
            if (newOverflow == 0) break
        }
    }
    return bestResult
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
            "checkOverflow -----> ${++count} ($resultWith,$resultHeight)"
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
                        "width overflow:${overSize}${startLog}${endLog} ($oldWidth) -> ($resultWith)"
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
                        "height overflow:${overSize}${topLog}${bottomLog} ($oldHeight) -> ($resultHeight)"
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

private fun Aligner.Position.isCenterVertical(): Boolean = when (this) {
    Aligner.Position.StartCenter,
    Aligner.Position.EndCenter,
    Aligner.Position.Center,
    -> true

    else -> false
}

private fun Aligner.Position.isCenterHorizontal(): Boolean = when (this) {
    Aligner.Position.TopCenter,
    Aligner.Position.BottomCenter,
    Aligner.Position.Center,
    -> true

    else -> false
}