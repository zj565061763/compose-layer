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
import com.sd.lib.compose.layer.TargetLayer.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.properties.Delegates

internal class TargetLayerImpl() : LayerImpl(), TargetLayer {
    private val _uiState = MutableStateFlow(UiState())

    private val _aligner = FAligner()
    private var _alignerResult: Aligner.Result? = null

    private var _offsetTransform: OffsetTransform? = null
    private var _fixOverflowDirectionState: PlusDirection? by mutableStateOf(null)
    private var _clipBackgroundDirectionState: PlusDirection? by mutableStateOf(null)

    private var _target by Delegates.observable("") { _, oldValue, newValue ->
        if (oldValue != newValue) {
            _layerManager?.run {
                unregisterTargetLayoutCallback(oldValue, _targetLayoutCallback)
                registerTargetLayoutCallback(newValue, _targetLayoutCallback)
            }
        }
    }

    private var _targetLayoutCoordinates: LayoutCoordinates? by Delegates.observable(null) { _, _, _ ->
        updateOffset()
    }
    private var _containerLayoutCoordinates: LayoutCoordinates? by Delegates.observable(null) { _, _, _ ->
        updateOffset()
    }
    private var _contentSize by Delegates.observable(IntSize.Zero) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            logMsg { "contentSize (${newValue.width}, ${newValue.height})" }
        }
    }

    private val _targetLayoutCallback: (LayoutCoordinates?) -> Unit = {
        _targetLayoutCoordinates = it
    }
    private val _containerLayoutCallback: (LayoutCoordinates?) -> Unit = {
        _containerLayoutCoordinates = it
    }

    override fun setPosition(position: Layer.Position) {
        val old = positionState
        super.setPosition(position)
        if (old != positionState) {
            updateOffset()
        }
    }

    override fun setTarget(target: String) {
        _target = target
    }

    override fun setOffsetTransform(transform: OffsetTransform?) {
        _offsetTransform = transform
    }

    override fun setFixOverflowDirection(direction: PlusDirection?) {
        _fixOverflowDirectionState = direction
    }

    override fun setClipBackgroundDirection(direction: PlusDirection?) {
        _clipBackgroundDirectionState = direction
    }

    override fun attach() {
        super.attach()
        updateOffset()
        updateUiState()
    }

    override fun detach() {
        super.detach()
        updateUiState()
    }

    override fun attachToManager(manager: LayerManager) {
        super.attachToManager(manager)
        manager.registerContainerLayoutCallback(_containerLayoutCallback)
    }

    override fun detachFromManager(manager: LayerManager) {
        super.detachFromManager(manager)
        manager.unregisterContainerLayoutCallback(_containerLayoutCallback)
    }

    /**
     * 计算位置
     */
    private fun updateOffset() {
        alignTarget()
        updateUiState()
    }

    private fun alignTarget(
        contentSize: IntSize? = null
    ): Aligner.Result? {
        val target = _targetLayoutCoordinates
        if (!target.isReady()) return null

        val container = _containerLayoutCoordinates
        if (!container.isReady()) return null

        val sourceSize = contentSize ?: _contentSize
        if (sourceSize.width <= 0 || sourceSize.height <= 0) return null

        val targetCoordinates = target.coordinate()
        val containerCoordinates = container.coordinate()

        val targetSize = target.size
        val containerSize = container.size

        val input = Aligner.Input(
            position = positionState.toAlignerPosition(),
            targetX = targetCoordinates.x.toInt(),
            targetY = targetCoordinates.y.toInt(),
            containerX = containerCoordinates.x.toInt(),
            containerY = containerCoordinates.y.toInt(),
            targetWidth = targetSize.width,
            targetHeight = targetSize.height,
            containerWidth = containerSize.width,
            containerHeight = containerSize.height,
            sourceWidth = sourceSize.width,
            sourceHeight = sourceSize.height,
        )

        val result = _aligner.align(input)
        return transformResult(result).also {
            _alignerResult = it
        }
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
        val isVisible = if (_isAttached) {
            if (_target.isEmpty()) {
                true
            } else {
                _targetLayoutCoordinates.isReady()
            }
        } else false

        _uiState.value = UiState(
            isVisible = isVisible,
            alignerResult = _alignerResult,
        )
    }

    @Composable
    override fun Content() {
        val uiState by _uiState.collectAsState()

        SideEffect {
            setContentVisible(uiState.isVisible)
        }

        LayerBox(uiState.isVisible) {
            OffsetBox(
                isVisible = uiState.isVisible,
                result = uiState.alignerResult,
                background = {
                    BackgroundBox(uiState.isVisible)
                },
                content = {
                    ContentBox(
                        modifier = Modifier.onSizeChanged {
                            _contentSize = it
                        }
                    )
                }
            )
        }
    }

    @Composable
    private fun OffsetBox(
        isVisible: Boolean,
        result: Aligner.Result?,
        background: @Composable () -> Unit,
        content: @Composable () -> Unit,
    ) {
        var overflowConstraints: Constraints? by remember { mutableStateOf(null) }
        var lastBackgroundPlaceInfo: BackgroundPlaceInfo? by remember { mutableStateOf(null) }

        SubcomposeLayout(Modifier.fillMaxSize()) { cs ->
            val cs = cs.copy(minWidth = 0, minHeight = 0)


            if (!isVisible) {
                val placeable = measureContent(OffsetBoxSlotId.Content, overflowConstraints ?: cs, content)
                val x = result?.x ?: 0
                val y = result?.y ?: 0

                val backgroundPlaceInfo = lastBackgroundPlaceInfo ?: backgroundPlaceInfo(
                    cs = cs,
                    contentOffset = IntOffset(x, y),
                    contentSize = IntSize(placeable.width, placeable.height),
                    direction = _clipBackgroundDirectionState,
                )
                val backgroundPlaceable = measureBackground(
                    slotId = OffsetBoxSlotId.Background,
                    constraints = cs.copy(maxWidth = backgroundPlaceInfo.width, maxHeight = backgroundPlaceInfo.height),
                    content = background,
                )

                logMsg { "layout invisible ($x, $y)" }
                return@SubcomposeLayout layout(cs.maxWidth, cs.maxHeight) {
                    backgroundPlaceable?.place(backgroundPlaceInfo.x, backgroundPlaceInfo.y, -1f)
                    placeable.placeRelative(x, y)
                }
            }


            var nullResultPlaceable: Placeable? = null
            val result = result ?: kotlin.run {
                val placeable = measureContent(null, cs, content).also {
                    nullResultPlaceable = it
                }
                val size = IntSize(placeable.width, placeable.height).also {
                    _contentSize = it
                }
                alignTarget(size)
            }

            if (result == null) {
                val backgroundPlaceable = measureBackground(OffsetBoxSlotId.Background, cs, background)
                logMsg { "layout null result size:$_contentSize" }
                return@SubcomposeLayout layout(cs.maxWidth, cs.maxHeight) {
                    backgroundPlaceable?.place(0, 0, -1f)
                    nullResultPlaceable?.place(Int.MIN_VALUE, Int.MIN_VALUE)
                }
            }


            var x = result.x
            var y = result.y


            val fixOverflowDirection = _fixOverflowDirectionState
            if (fixOverflowDirection == null) {
                val placeable = measureContent(OffsetBoxSlotId.Content, cs, content).also {
                    overflowConstraints = null
                }

                val backgroundPlaceInfo = backgroundPlaceInfo(
                    cs = cs,
                    contentOffset = IntOffset(x, y),
                    contentSize = IntSize(placeable.width, placeable.height),
                    direction = _clipBackgroundDirectionState,
                ).also { lastBackgroundPlaceInfo = it }
                val backgroundPlaceable = measureBackground(
                    slotId = OffsetBoxSlotId.Background,
                    constraints = cs.copy(maxWidth = backgroundPlaceInfo.width, maxHeight = backgroundPlaceInfo.height),
                    content = background,
                )

                logMsg { "layout none overflow direction" }
                return@SubcomposeLayout layout(cs.maxWidth, cs.maxHeight) {
                    backgroundPlaceable?.place(backgroundPlaceInfo.x, backgroundPlaceInfo.y, -1f)
                    placeable.placeRelative(x, y)
                }
            }


            // 原始大小
            val originalPlaceable = nullResultPlaceable ?: measureContent(null, cs, content)
            // 根据原始大小测量的结果
            val originalResult = _aligner.reAlign(result, originalPlaceable.width, originalPlaceable.height)

            val checkConstraints = checkOverflow(originalResult, cs, fixOverflowDirection).also {
                overflowConstraints = it
            }

            val placeable = if (checkConstraints != null) {
                // 约束条件变化后，重新计算坐标
                measureContent(OffsetBoxSlotId.Content, checkConstraints, content).also { placeable ->
                    _aligner.reAlign(result, placeable.width, placeable.height).let {
                        logMsg { "size:(${originalPlaceable.width}, ${originalPlaceable.height}) -> (${placeable.width}, ${placeable.height}) offset:($x, $y) -> (${it.x}, ${it.y})" }
                        x = it.x
                        y = it.y
                    }
                }
            } else {
                originalPlaceable
            }

            val backgroundPlaceInfo = backgroundPlaceInfo(
                cs = cs,
                contentOffset = IntOffset(x, y),
                contentSize = IntSize(placeable.width, placeable.height),
                direction = _clipBackgroundDirectionState,
            ).also { lastBackgroundPlaceInfo = it }
            val backgroundPlaceable = measureBackground(
                slotId = OffsetBoxSlotId.Background,
                constraints = cs.copy(maxWidth = backgroundPlaceInfo.width, maxHeight = backgroundPlaceInfo.height),
                content = background,
            )

            logMsg { "layout last" }
            layout(cs.maxWidth, cs.maxHeight) {
                backgroundPlaceable?.place(backgroundPlaceInfo.x, backgroundPlaceInfo.y, -1f)
                placeable.placeRelative(x, y)
            }
        }
    }

    private fun checkOverflow(
        result: Aligner.Result,
        cs: Constraints,
        direction: PlusDirection,
    ): Constraints? {
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
                            logMsg { "top overflow $top" }
                        }
                    }

                    if (direction.hasBottom()) {
                        if (bottom > 0) {
                            overSize += bottom
                            isBottomOverflow = true
                            logMsg { "bottom overflow $bottom" }
                        }
                    }

                    if (overSize > 0) {
                        hasOverflow = true

                        /**
                         * 居中对齐的时候，如果只有一边溢出，则需要减去双倍溢出的值
                         */
                        if (positionState.isCenterVertical) {
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
                            logMsg { "start overflow $start" }
                        }
                    }

                    if (direction.hasEnd()) {
                        if (end > 0) {
                            overSize += end
                            isEndOverflow = true
                            logMsg { "end overflow $end" }
                        }
                    }

                    if (overSize > 0) {
                        hasOverflow = true

                        /**
                         * 居中对齐的时候，如果只有一边溢出，则需要减去双倍溢出的值
                         */
                        if (positionState.isCenterHorizontal) {
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
                result = _aligner.reAlign(result, cs.maxWidth, cs.maxHeight)
            } else {
                break
            }

            logMsg { "checkOverflow -----> ${++count}" }
        }

        return resultConstraints
    }

    private fun backgroundPlaceInfo(
        cs: Constraints,
        contentOffset: IntOffset,
        contentSize: IntSize,
        direction: PlusDirection?,
    ): BackgroundPlaceInfo {
        if (direction == null || contentSize.width <= 0 || contentSize.height <= 0) {
            return BackgroundPlaceInfo(
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
            height -= contentY
            y = contentY
        }
        if (direction.hasBottom()) {
            height -= (cs.maxHeight - contentY - contentSize.height)
        }

        if (direction.hasStart()) {
            width -= contentX
            x = contentX
        }
        if (direction.hasEnd()) {
            width -= (cs.maxWidth - contentX - contentSize.width)
        }

        return BackgroundPlaceInfo(
            x = x,
            y = y,
            width = width.coerceAtLeast(0),
            height = height.coerceAtLeast(0),
        )
    }

    private data class UiState(
        val isVisible: Boolean = false,
        val alignerResult: Aligner.Result? = null,
    )
}

private enum class OffsetBoxSlotId {
    Content,
    Background,
}

private data class BackgroundPlaceInfo(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

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

@OptIn(ExperimentalContracts::class)
private fun LayoutCoordinates?.isReady(): Boolean {
    contract {
        returns(true) implies (this@isReady != null)
    }
    if (this == null) return false
    if (!this.isAttached) return false
    if (this.size.width <= 0 || this.size.height <= 0) return false
    return true
}

private fun LayoutCoordinates.coordinate(): Offset {
    return this.localToWindow(Offset.Zero)
}

private fun Aligner.reAlign(result: Aligner.Result, sourceWidth: Int, sourceHeight: Int): Aligner.Result {
    return align(
        result.input.copy(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
        )
    )
}

private val Layer.Position.isCenterVertical: Boolean
    get() {
        return when (this) {
            Layer.Position.StartCenter,
            Layer.Position.EndCenter,
            Layer.Position.Center,
            -> true
            else -> false
        }
    }

private val Layer.Position.isCenterHorizontal: Boolean
    get() {
        return when (this) {
            Layer.Position.TopCenter,
            Layer.Position.BottomCenter,
            Layer.Position.Center,
            -> true
            else -> false
        }
    }

private fun SubcomposeMeasureScope.measureContent(
    slotId: Any?,
    constraints: Constraints,
    content: @Composable () -> Unit
): Placeable {
    val measurable = subcompose(slotId, content).let {
        check(it.size == 1)
        it.first()
    }
    return measurable.measure(constraints)
}

private fun SubcomposeMeasureScope.measureBackground(
    slotId: Any?,
    constraints: Constraints,
    content: @Composable () -> Unit
): Placeable? {
    val measurable = subcompose(slotId, content).let {
        if (it.isNotEmpty()) check(it.size == 1)
        it.firstOrNull()
    }
    return measurable?.measure(constraints)
}