package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
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

    private var _offsetTransform: (OffsetTransformScope.() -> IntOffset)? = null
    private var _fixOverflowDirectionState by mutableStateOf(OverflowDirection.None)

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
    private var _contentLayoutCoordinates: LayoutCoordinates? by Delegates.observable(null) { _, _, _ ->
        updateOffset()
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

    override fun setOffsetTransform(transform: (OffsetTransformScope.() -> IntOffset)?) {
        _offsetTransform = transform
    }

    override fun setFixOverflowDirection(direction: Int) {
        _fixOverflowDirectionState = direction
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

    override fun onContentLayoutCoordinatesChanged(layoutCoordinates: LayoutCoordinates) {
        super.onContentLayoutCoordinatesChanged(layoutCoordinates)
        _contentLayoutCoordinates = layoutCoordinates
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
        alignTarget()?.let {
            _alignerResult = it
        }
        updateUiState()
    }

    private fun alignTarget(): Aligner.Result? {
        val target = _targetLayoutCoordinates
        if (!target.isReady()) return null

        val container = _containerLayoutCoordinates
        if (!container.isReady()) return null

        val source = _contentLayoutCoordinates
        if (!source.isReady()) return null

        val targetCoordinates = target.coordinate()
        val containerCoordinates = container.coordinate()
        val sourceCoordinates = source.coordinate()

        val targetSize = target.size
        val containerSize = container.size
        val sourceSize = source.size

        val input = Aligner.Input(
            targetX = targetCoordinates.x.toInt(),
            targetY = targetCoordinates.y.toInt(),
            containerX = containerCoordinates.x.toInt(),
            containerY = containerCoordinates.y.toInt(),
            sourceX = sourceCoordinates.x.toInt(),
            sourceY = sourceCoordinates.y.toInt(),
            targetWidth = targetSize.width,
            targetHeight = targetSize.height,
            containerWidth = containerSize.width,
            containerHeight = containerSize.height,
            sourceWidth = sourceSize.width,
            sourceHeight = sourceSize.height,
        )

        _aligner.setPosition(positionState.toAlignerPosition())
        return _aligner.align(input)
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
            OffsetBox(uiState.alignerResult) {
                ContentBox()
            }
        }
    }

    @Composable
    private fun OffsetBox(
        result: Aligner.Result?,
        content: @Composable () -> Unit,
    ) {
        val fixOverflowDirection = _fixOverflowDirectionState
        var overflowConstraints: Constraints? by remember { mutableStateOf(null) }

        SubcomposeLayout(Modifier.fillMaxSize()) { cs ->
            val cs = cs.copy(minWidth = 0, minHeight = 0)

            if (result == null) {
                val placeable = measureContent(Unit, cs, content)
                return@SubcomposeLayout layout(cs.maxWidth, cs.maxHeight) {
                    placeable.place(Int.MIN_VALUE, Int.MIN_VALUE)
                }
            }

            var x = result.x
            var y = result.y

            if (!_isAttached) {
                val placeable = measureContent(Unit, overflowConstraints ?: cs, content)
                return@SubcomposeLayout layout(cs.maxWidth, cs.maxHeight) {
                    placeable.placeRelative(x, y)
                }
            }

            if (fixOverflowDirection == OverflowDirection.None) {
                val placeable = measureContent(Unit, cs, content)
                return@SubcomposeLayout layout(cs.maxWidth, cs.maxHeight) {
                    placeable.placeRelative(x, y)
                }
            }

            // 原始大小
            val originalPlaceable = measureContent(null, cs, content)
            // 根据原始大小测量的结果
            val originalResult = _aligner.reAlign(result, originalPlaceable.width, originalPlaceable.height)

            val checkConstraints = checkOverflow(originalResult, cs, fixOverflowDirection).also {
                overflowConstraints = it
            }

            val placeable = if (checkConstraints != null) {
                // 约束条件变化后，重新计算坐标
                measureContent(Unit, checkConstraints, content).also { placeable ->
                    _aligner.reAlign(result, placeable.width, placeable.height).let {
                        x = it.x
                        y = it.y
                    }
                    logMsg { "size:(${placeable.width}, ${placeable.height}) offset:($x, $y)" }
                }
            } else {
                originalPlaceable
            }

            layout(cs.maxWidth, cs.maxHeight) {
                placeable.placeRelative(x, y)
            }
        }
    }

    private fun checkOverflow(
        result: Aligner.Result,
        cs: Constraints,
        fixOverflowDirection: Int,
    ): Constraints? {
        var resultConstraints: Constraints? = null

        var cs = cs
        var result = result

        var count = 0
        while (true) {
            var hasOverflow = false
            logMsg { "checkOverflow -----> ${++count}" }

            // 检查是否溢出
            with(result.sourceOverflow) {
                // Vertical
                kotlin.run {
                    var overSize = 0
                    if (OverflowDirection.hasTop(fixOverflowDirection)) {
                        if (top > 0) {
                            overSize += top
                            logMsg { "top overflow $top" }
                        }
                    }
                    if (OverflowDirection.hasBottom(fixOverflowDirection)) {
                        if (bottom > 0) {
                            overSize += bottom
                            logMsg { "bottom overflow $bottom" }
                        }
                    }
                    if (overSize > 0) {
                        hasOverflow = true
                        val maxSize = (cs.maxHeight - overSize).coerceAtLeast(1)
                        cs = cs.copy(maxHeight = maxSize).also {
                            resultConstraints = it
                        }
                    }
                }

                // Horizontal
                kotlin.run {
                    var overSize = 0
                    if (OverflowDirection.hasStart(fixOverflowDirection)) {
                        if (start > 0) {
                            overSize += start
                            logMsg { "start overflow $start" }
                        }
                    }
                    if (OverflowDirection.hasEnd(fixOverflowDirection)) {
                        if (end > 0) {
                            overSize += end
                            logMsg { "end overflow $end" }
                        }
                    }
                    if (overSize > 0) {
                        hasOverflow = true
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
        }

        return resultConstraints
    }

    private data class UiState(
        val isVisible: Boolean = false,
        val alignerResult: Aligner.Result? = null,
    )
}

private fun Layer.Position.toAlignerPosition(): Aligner.Position {
    return when (this) {
        Layer.Position.TopStart -> Aligner.Position.TopStart
        Layer.Position.TopCenter -> Aligner.Position.TopCenter
        Layer.Position.TopEnd -> Aligner.Position.TopEnd

        Layer.Position.BottomStart -> Aligner.Position.BottomStart
        Layer.Position.BottomCenter -> Aligner.Position.BottomCenter
        Layer.Position.BottomEnd -> Aligner.Position.BottomEnd

        Layer.Position.StartTop -> Aligner.Position.StartTop
        Layer.Position.StartCenter -> Aligner.Position.StartCenter
        Layer.Position.StartBottom -> Aligner.Position.StartBottom

        Layer.Position.EndTop -> Aligner.Position.EndTop
        Layer.Position.EndCenter -> Aligner.Position.EndCenter
        Layer.Position.EndBottom -> Aligner.Position.EndBottom

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