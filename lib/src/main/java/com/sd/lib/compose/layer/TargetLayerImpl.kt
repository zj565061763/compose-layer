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

internal class TargetLayerImpl : LayerImpl(), TargetLayer {
    private val _uiState = MutableStateFlow(UiState())
    private val _aligner = FAligner()

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

    private var _targetLayoutCoordinates: LayoutCoordinates? by Delegates.observable(null) { _, _, newValue ->
        logMsg(isDebug) { "${this@TargetLayerImpl} target layout changed $newValue" }
        updateUiState()
    }
    private var _containerLayoutCoordinates: LayoutCoordinates? by Delegates.observable(null) { _, _, newValue ->
        logMsg(isDebug) { "${this@TargetLayerImpl} container layout changed $newValue" }
        updateUiState()
    }

    private val _targetLayoutCallback: (LayoutCoordinates?) -> Unit = {
        _targetLayoutCoordinates = it
    }
    private val _containerLayoutCallback: (LayoutCoordinates?) -> Unit = {
        _containerLayoutCoordinates = it
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

    private fun alignTarget(
        position: Layer.Position,
        target: LayoutInfo,
        container: LayoutInfo,
        contentSize: IntSize,
    ): Aligner.Result? {
        if (!target.isReady) return null
        if (!container.isReady) return null
        if (!contentSize.isReady()) return null

        val targetOffset = target.offset
        val containerOffset = container.offset

        val targetSize = target.size
        val containerSize = container.size

        val input = Aligner.Input(
            position = position.toAlignerPosition(),
            targetX = targetOffset.x,
            targetY = targetOffset.y,
            containerX = containerOffset.x,
            containerY = containerOffset.y,
            targetWidth = targetSize.width,
            targetHeight = targetSize.height,
            containerWidth = containerSize.width,
            containerHeight = containerSize.height,
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
        _uiState.value = UiState(
            targetLayout = LayoutInfo(
                size = _targetLayoutCoordinates.size(),
                offset = _targetLayoutCoordinates.offset(),
                isAttached = _targetLayoutCoordinates?.isAttached ?: false,
            ),
            containerLayout = LayoutInfo(
                size = _containerLayoutCoordinates.size(),
                offset = _containerLayoutCoordinates.offset(),
                isAttached = _containerLayoutCoordinates?.isAttached ?: false,
            )
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
        var visibleBackgroundInfo: BackgroundPlaceInfo? by remember { mutableStateOf(null) }
        var visibleOffset by remember { mutableStateOf(IntOffset.Zero) }
        var visibleConstraints: Constraints? by remember { mutableStateOf(null) }

        SubcomposeLayout(Modifier.fillMaxSize()) { cs ->
            val cs = cs.copy(minWidth = 0, minHeight = 0)


            // 如果状态由可见变为不可见，则要维持可见时候的状态
            if (!isVisibleState) {
                val backgroundInfo = visibleBackgroundInfo ?: BackgroundPlaceInfo(0, 0, cs.maxWidth, cs.maxHeight)
                val backgroundPlaceable = measureBackground(
                    slotId = OffsetBoxSlotId.Background,
                    constraints = cs.copy(maxWidth = backgroundInfo.width, maxHeight = backgroundInfo.height),
                    content = background,
                )

                val placeable = measureContent(OffsetBoxSlotId.Content, visibleConstraints ?: cs, content)
                val offset = visibleOffset
                logMsg(isDebug) { "${this@TargetLayerImpl} layout invisible (${offset.x}, ${offset.y})" }
                setContentVisible(true)
                return@SubcomposeLayout layout(cs.maxWidth, cs.maxHeight) {
                    backgroundPlaceable?.place(backgroundInfo.x, backgroundInfo.y, -1f)
                    placeable.placeRelative(offset.x, offset.y)
                }
            }


            val fixOverflowDirection = _fixOverflowDirectionState


            // 测量原始信息
            val originalPlaceable = measureContent(
                slotId = if (fixOverflowDirection == null) {
                    // 如果不需要修复溢出的话，则用正式的slotId测量
                    OffsetBoxSlotId.Content
                } else null,
                constraints = cs,
                content = content
            )

            val originalSize = IntSize(originalPlaceable.width, originalPlaceable.height)
            val originalResult = alignTarget(
                position = positionState,
                target = uiState.targetLayout,
                container = uiState.containerLayout,
                contentSize = originalSize,
            )

            if (originalResult == null) {
                val backgroundPlaceable = measureBackground(OffsetBoxSlotId.Background, cs, background)
                logMsg(isDebug) { "${this@TargetLayerImpl} layout null result size:$originalSize" }
                return@SubcomposeLayout layout(cs.maxWidth, cs.maxHeight) {
                    visibleBackgroundInfo = null
                    visibleOffset = IntOffset.Zero
                    visibleConstraints = cs
                    backgroundPlaceable?.place(0, 0, -1f)
                    originalPlaceable.place(Int.MIN_VALUE, Int.MIN_VALUE)
                }
            }


            var x = originalResult.x
            var y = originalResult.y


            if (fixOverflowDirection == null) {
                val backgroundInfo = backgroundPlaceInfo(
                    cs = cs,
                    contentOffset = IntOffset(x, y),
                    contentSize = IntSize(originalPlaceable.width, originalPlaceable.height),
                    direction = _clipBackgroundDirectionState,
                )
                val backgroundPlaceable = measureBackground(
                    slotId = OffsetBoxSlotId.Background,
                    constraints = cs.copy(maxWidth = backgroundInfo.width, maxHeight = backgroundInfo.height),
                    content = background,
                )

                logMsg(isDebug) { "${this@TargetLayerImpl} layout none overflow direction" }
                return@SubcomposeLayout layout(cs.maxWidth, cs.maxHeight) {
                    visibleBackgroundInfo = backgroundInfo
                    visibleOffset = IntOffset(x, y)
                    visibleConstraints = cs
                    backgroundPlaceable?.place(backgroundInfo.x, backgroundInfo.y, -1f)
                    originalPlaceable.placeRelative(x, y)
                }
            }

            val (fixedConstraints, fixedResult) = checkOverflow(originalResult, cs, fixOverflowDirection)
            val placeable = if (fixedConstraints != null) {
                measureContent(OffsetBoxSlotId.Content, fixedConstraints, content).also { placeable ->
                    logMsg(isDebug) {
                        "${this@TargetLayerImpl} fix overflow size:(${originalPlaceable.width}, ${originalPlaceable.height}) -> (${placeable.width}, ${placeable.height}) offset:($x, $y) -> (${fixedResult.x}, ${fixedResult.y})"
                    }
                    x = fixedResult.x
                    y = fixedResult.y
                }
            } else {
                measureContent(OffsetBoxSlotId.Content, cs, content)
            }

            val backgroundInfo = backgroundPlaceInfo(
                cs = cs,
                contentOffset = IntOffset(x, y),
                contentSize = IntSize(placeable.width, placeable.height),
                direction = _clipBackgroundDirectionState,
            )
            val backgroundPlaceable = measureBackground(
                slotId = OffsetBoxSlotId.Background,
                constraints = cs.copy(maxWidth = backgroundInfo.width, maxHeight = backgroundInfo.height),
                content = background,
            )

            logMsg(isDebug) { "${this@TargetLayerImpl} layout" }
            layout(cs.maxWidth, cs.maxHeight) {
                visibleBackgroundInfo = backgroundInfo
                visibleOffset = IntOffset(x, y)
                visibleConstraints = fixedConstraints
                backgroundPlaceable?.place(backgroundInfo.x, backgroundInfo.y, -1f)
                placeable.placeRelative(x, y)
            }
        }
    }

    private fun checkOverflow(
        result: Aligner.Result,
        cs: Constraints,
        direction: PlusDirection,
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
                            logMsg(isDebug) { "${this@TargetLayerImpl} top overflow $top" }
                        }
                    }

                    if (direction.hasBottom()) {
                        if (bottom > 0) {
                            overSize += bottom
                            isBottomOverflow = true
                            logMsg(isDebug) { "${this@TargetLayerImpl} bottom overflow $bottom" }
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
                            logMsg(isDebug) { "${this@TargetLayerImpl} start overflow $start" }
                        }
                    }

                    if (direction.hasEnd()) {
                        if (end > 0) {
                            overSize += end
                            isEndOverflow = true
                            logMsg(isDebug) { "${this@TargetLayerImpl} end overflow $end" }
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
                result = _aligner.reAlign(result, cs.maxWidth, cs.maxHeight)
            } else {
                break
            }

            logMsg(isDebug) { "${this@TargetLayerImpl} checkOverflow -----> ${++count}" }
        }

        return Pair(resultConstraints, result)
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
        val targetLayout: LayoutInfo = LayoutInfo(),
        val containerLayout: LayoutInfo = LayoutInfo(),
    )

    private data class LayoutInfo(
        val size: IntSize = IntSize.Zero,
        val offset: IntOffset = IntOffset.Zero,
        val isAttached: Boolean = false,
    ) {
        val isReady: Boolean
            get() = isAttached && size.width > 0 && size.height > 0
    }
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

private fun LayoutCoordinates?.size(): IntSize {
    if (this == null || !this.isAttached) return IntSize.Zero
    return this.size
}

private fun LayoutCoordinates?.offset(): IntOffset {
    if (this == null || !this.isAttached) return IntOffset.Zero
    val offset = this.localToWindow(Offset.Zero)
    return IntOffset(offset.x.toInt(), offset.y.toInt())
}

private fun IntSize.isReady(): Boolean = this.width > 0 && this.height > 0

private fun Aligner.reAlign(result: Aligner.Result, sourceWidth: Int, sourceHeight: Int): Aligner.Result {
    return align(
        result.input.copy(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
        )
    )
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