package com.sd.lib.compose.layer

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.sd.lib.aligner.Aligner
import com.sd.lib.aligner.FAligner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.properties.Delegates

interface FLayerContentScope {
    /** 内容是否可见 */
    val isVisible: Boolean
}

interface OffsetInterceptorScope {
    /** 当前计算的layer坐标 */
    val offset: IntOffset

    /** 内容大小 */
    val contentSize: IntSize

    /** 目标大小 */
    val targetSize: IntSize
}

data class DialogBehavior(
    /** 按返回键是否可以关闭 */
    val cancelable: Boolean = true,

    /** 触摸到非内容区域是否关闭 */
    val canceledOnTouchOutside: Boolean = true,

    /** 背景颜色 */
    val backgroundColor: Color = Color.Black.copy(alpha = 0.25f)
)

class FLayer internal constructor() {
    private val _uiState = MutableStateFlow(LayerUiState())
    private var _content: @Composable FLayerContentScope.() -> Unit by mutableStateOf({ })

    private var _isAttached = false
    private var _offsetInterceptor: (OffsetInterceptorScope.() -> IntOffset)? = null

    private val _fixOverflowInfo = FixOverflowInfo()

    private var _fixOverflowDirection by Delegates.observable(OverflowDirection.None) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            _fixOverflowInfo.reset()
            updateOffset()
        }
    }

    private var _position by Delegates.observable(Position.Center) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            positionState = newValue
            _aligner.setPosition(newValue.toAlignerPosition())
            updateOffset()
        }
    }

    private var _target by Delegates.observable("") { _, oldValue, newValue ->
        if (oldValue != newValue) {
            _targetLayoutCoordinates = _layerManager?.findTarget(newValue)
            _layerManager?.unregisterTargetLayoutCallback(oldValue, _targetLayoutCallback)
            _layerManager?.registerTargetLayoutCallback(newValue, _targetLayoutCallback)
        }
    }

    private var _targetLayoutCoordinates: LayoutCoordinates? by Delegates.observable(null) { _, _, newValue ->
        updateOffset()
    }

    private var _containerLayoutCoordinates: LayoutCoordinates? by Delegates.observable(null) { _, _, newValue ->
        updateOffset()
    }

    private var _layerLayoutCoordinates: LayoutCoordinates? = null
    private var _contentLayoutCoordinates: LayoutCoordinates? by Delegates.observable(null) { _, _, newValue ->
        updateOffset()
    }

    private var _layerManager: LayerManager? = null
    private val _contentScopeImpl = LayerContentScopeImpl()
    private var _dialogBehavior: DialogBehavior? by mutableStateOf(DialogBehavior())

    /** 是否可见 */
    var isVisibleState: Boolean by mutableStateOf(false)
        private set

    /** [Position] */
    var positionState: Position by mutableStateOf(_position)
        private set

    /**
     * 设置内容
     */
    fun setContent(content: @Composable FLayerContentScope.() -> Unit) {
        _content = content
    }

    /**
     * 设置对齐的位置
     */
    fun setPosition(position: Position) {
        _position = position
    }

    /**
     * 设置目标
     */
    fun setTarget(target: String) {
        _target = target
    }

    /**
     * 设置窗口行为
     */
    fun setDialogBehavior(block: (DialogBehavior) -> DialogBehavior?) {
        _dialogBehavior = block(_dialogBehavior ?: DialogBehavior())
    }

    /**
     * 设置坐标拦截器
     */
    fun setOffsetInterceptor(interceptor: (OffsetInterceptorScope.() -> IntOffset)?) {
        _offsetInterceptor = interceptor
    }

    /**
     * 设置修复溢出的方向[OverflowDirection]
     */
    fun setFixOverflowDirection(direction: Int) {
        _fixOverflowDirection = direction
    }

    /**
     * 添加到容器
     */
    fun attach() {
        _isAttached = true
        _layerManager?.notifyLayerAttachState(this, true)
        updateOffset()
        updateUiState()
    }

    /**
     * 移除
     */
    fun detach() {
        _isAttached = false
        _layerManager?.notifyLayerAttachState(this, false)
        updateUiState()
    }

    @Composable
    fun UpdateContainer() {
        val layerManager = checkNotNull(LocalLayerManager.current) {
            "CompositionLocal LocalLayerManager not present"
        }
        LaunchedEffect(layerManager) {
            val currentManager = _layerManager
            if (currentManager != layerManager) {
                currentManager?.detachLayer(this@FLayer)
                layerManager.attachLayer(this@FLayer)
            }
        }
    }

    internal fun attachToManager(manager: LayerManager) {
        _layerManager = manager
        _containerLayoutCoordinates = manager.containerLayout
        manager.registerContainerLayoutCallback(_containerLayoutCallback)
    }

    internal fun detachFromManager() {
        detach()
        _layerManager?.let {
            it.unregisterContainerLayoutCallback(_containerLayoutCallback)
            _containerLayoutCoordinates = null
            _layerManager = null
        }
    }

    private val _targetLayoutCallback: (LayoutCoordinates?) -> Unit by lazy {
        {
            _targetLayoutCoordinates = it
        }
    }

    private val _containerLayoutCallback: (LayoutCoordinates) -> Unit by lazy {
        {
            _containerLayoutCoordinates = it
        }
    }

    private var _alignerResult: Aligner.Result? = null
    private val _aligner: Aligner by lazy {
        FAligner().apply {
            this.setPosition(_position.toAlignerPosition())
        }
    }

    /**
     * 计算位置
     */
    private fun updateOffset() {
        _alignerResult = updateOffsetInternal()
        updateUiState()
    }

    private fun updateOffsetInternal(): Aligner.Result? {
        val target = _targetLayoutCoordinates
        if (!target.isReady()) return null

        val source = _contentLayoutCoordinates
        if (!source.isReady()) return null

        val container = _containerLayoutCoordinates
        if (!container.isReady()) return null

        val targetCoordinates = target.coordinate()
        val sourceCoordinates = source.coordinate()
        val containerCoordinates = container.coordinate()

        val input = Aligner.Input(
            targetX = targetCoordinates.x.toInt(),
            targetY = targetCoordinates.y.toInt(),
            sourceX = sourceCoordinates.x.toInt(),
            sourceY = sourceCoordinates.y.toInt(),
            containerX = containerCoordinates.x.toInt(),
            containerY = containerCoordinates.y.toInt(),
            targetWidth = target.width(),
            targetHeight = target.height(),
            sourceWidth = source.width(),
            sourceHeight = source.height(),
            containerWidth = container.width(),
            containerHeight = container.height(),
        )

        return _aligner.align(input)
    }

    private fun updateUiState() {
        val isVisible = if (_isAttached) {
            if (_target.isEmpty()) {
                true
            } else {
                _targetLayoutCoordinates?.isAttached == true
            }
        } else false

        _uiState.value = LayerUiState(
            isVisible = isVisible,
            position = _position,
            hasTarget = _target.isNotEmpty(),
            alignerResult = _alignerResult,
            fixOverflowDirection = _fixOverflowDirection,
        )

        this.isVisibleState = isVisible
    }

    @Composable
    internal fun Content() {
        val uiState by _uiState.collectAsState()

        SideEffect {
            _contentScopeImpl._isVisible = uiState.isVisible
        }

        _dialogBehavior?.let { behavior ->
            BackHandler(uiState.isVisible) {
                if (behavior.cancelable) {
                    detach()
                }
            }
        }

        if (uiState.hasTarget) {
            LayerBox(uiState.isVisible) {
                BackgroundBox(uiState.isVisible)
                if (uiState.fixOverflowDirection == OverflowDirection.None) {
                    OffsetBox(uiState.alignerResult) {
                        ContentBox()
                    }
                } else {
                    FixOverflowBox(uiState.alignerResult) {
                        ContentBox()
                    }
                }
            }
        } else {
            LayerBox(uiState.isVisible) {
                BackgroundBox(uiState.isVisible)
                ContentBox(modifier = Modifier.align(uiState.position.toAlignment()))
            }
        }
    }

    @Composable
    private fun LayerBox(
        isVisible: Boolean,
        content: @Composable BoxScope.() -> Unit,
    ) {
        var modifier = Modifier.fillMaxSize()

        if (isVisible) {
            modifier = modifier.onGloballyPositioned {
                _layerLayoutCoordinates = it
            }
            _dialogBehavior?.let { behavior ->
                modifier = modifier.pointerInput(behavior) {
                    detectTouchOutside(behavior)
                }
            }
        }

        Box(modifier = modifier) {
            content()
        }
    }


    @Composable
    private fun FixOverflowBox(
        result: Aligner.Result?,
        content: @Composable () -> Unit,
    ) {
        if (result == null) {
            OffsetBox(null, content)
        } else {
            SubcomposeLayout(Modifier.fillMaxSize()) { cs ->
                var constraints = cs.copy(minWidth = 0, minHeight = 0)

                var x = result.x
                var y = result.y

                // 原始大小
                val originalPlaceable = placeable(null, cs.copy(minWidth = 0, minHeight = 0), content)
                // 根据原始大小测量的结果
                val originalResult = _aligner.align(
                    result.input.copy(
                        sourceWidth = originalPlaceable.width,
                        sourceHeight = originalPlaceable.height,
                    )
                )

                // 检查是否溢出
                with(originalResult.sourceOverflow) {
                    if (verticalOverflow > 0) {
                        val maxHeight = (originalResult.input.sourceHeight - verticalOverflow).coerceAtLeast(1)
                        logMsg { "height ${originalResult.input.sourceHeight} - $verticalOverflow = $maxHeight" }
                        constraints = constraints.copy(maxHeight = maxHeight)
                    }
                }

                val placeable = if (constraints != cs) {
                    // 约束条件变化后，重新计算坐标
                    placeable(Unit, constraints, content).also { placeable ->
                        _aligner.align(
                            result.input.copy(
                                sourceWidth = placeable.width,
                                sourceHeight = placeable.height,
                            )
                        ).let {
                            x = it.x
                            y = it.y
                        }
                    }
                } else {
                    originalPlaceable
                }

                layout(cs.maxWidth, cs.maxHeight) {
                    placeable.placeRelative(x, y)
                }
            }
        }
    }

    @Composable
    private fun OffsetBox(
        result: Aligner.Result?,
        content: @Composable () -> Unit,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(result?.x ?: 0, result?.y ?: 0)
                }
        ) {
            content()
        }
    }

    @Composable
    private fun BackgroundBox(
        isVisible: Boolean,
    ) {
        _dialogBehavior?.let { behavior ->
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(behavior.backgroundColor)
                )
            }
        }
    }

    @Composable
    private fun ContentBox(
        modifier: Modifier = Modifier,
    ) {
        Box(
            modifier = modifier.onGloballyPositioned {
                _contentLayoutCoordinates = it
            }
        ) {
            _content.invoke(_contentScopeImpl)
        }
    }

    private suspend fun PointerInputScope.detectTouchOutside(behavior: DialogBehavior) {
        forEachGesture {
            awaitPointerEventScope {
                val down = layerAwaitFirstDown(PointerEventPass.Initial)
                val downPosition = down.position

                val layerLayout = _layerLayoutCoordinates
                val contentLayout = _contentLayoutCoordinates
                if (layerLayout != null && contentLayout != null) {
                    val contentRect = layerLayout.localBoundingBoxOf(contentLayout)
                    if (contentRect.contains(downPosition)) {
                        // 触摸到内容区域
                    } else {
                        down.consume()
                        if (behavior.cancelable && behavior.canceledOnTouchOutside) {
                            detach()
                        }
                    }
                }
            }
        }
    }

    private class LayerContentScopeImpl : FLayerContentScope {
        var _isVisible by mutableStateOf(false)

        override val isVisible: Boolean
            get() = _isVisible
    }

    enum class Position {
        /** 顶部开始对齐 */
        TopStart,
        /** 顶部中间对齐 */
        TopCenter,
        /** 顶部结束对齐 */
        TopEnd,

        /** 中间开始对齐 */
        CenterStart,
        /** 中间对齐 */
        Center,
        /** 中间结束对齐 */
        CenterEnd,

        /** 底部开始对齐 */
        BottomStart,
        /** 底部中间对齐 */
        BottomCenter,
        /** 底部结束对齐 */
        BottomEnd,
    }

    class OverflowDirection {
        companion object {
            const val None = 0
            const val Top = 1
            const val Bottom = 2
            const val Start = 4
            const val End = 8

            fun hasTop(value: Int) = Top and value != 0
            fun hasBottom(value: Int) = Bottom and value != 0
            fun hasStart(value: Int) = Start and value != 0
            fun hasEnd(value: Int) = End and value != 0
        }
    }
}

private data class LayerUiState(
    val isVisible: Boolean = false,
    val position: FLayer.Position = FLayer.Position.Center,
    val hasTarget: Boolean = false,
    val alignerResult: Aligner.Result? = null,
    val fixOverflowDirection: Int = FLayer.OverflowDirection.None,
)

private class FixOverflowInfo {
    var isTopFixed = false
    var isBottomFixed = false
    var isStartFixed = false
    var isEndFixed = false

    var fixedWidth: Int? = null
    var fixedHeight: Int? = null

    fun reset() {
        isTopFixed = false
        isBottomFixed = false
        isStartFixed = false
        isEndFixed = false
        fixedWidth = null
        fixedHeight = null
    }
}

internal inline fun logMsg(block: () -> String) {
    Log.i("FLayer", block())
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

private fun LayoutCoordinates.width(): Int {
    return this.size.width
}

private fun LayoutCoordinates.height(): Int {
    return this.size.height
}

private fun FLayer.Position.toAlignment(): Alignment {
    return when (this) {
        FLayer.Position.TopStart -> Alignment.TopStart
        FLayer.Position.TopCenter -> Alignment.TopCenter
        FLayer.Position.TopEnd -> Alignment.TopEnd

        FLayer.Position.CenterStart -> Alignment.CenterStart
        FLayer.Position.Center -> Alignment.Center
        FLayer.Position.CenterEnd -> Alignment.CenterEnd

        FLayer.Position.BottomStart -> Alignment.BottomStart
        FLayer.Position.BottomCenter -> Alignment.BottomCenter
        FLayer.Position.BottomEnd -> Alignment.BottomEnd
    }
}

private fun FLayer.Position.toAlignerPosition(): Aligner.Position {
    return when (this) {
        FLayer.Position.TopStart -> Aligner.Position.TopStart
        FLayer.Position.TopCenter -> Aligner.Position.TopCenter
        FLayer.Position.TopEnd -> Aligner.Position.TopEnd

        FLayer.Position.CenterStart -> Aligner.Position.CenterStart
        FLayer.Position.Center -> Aligner.Position.Center
        FLayer.Position.CenterEnd -> Aligner.Position.CenterEnd

        FLayer.Position.BottomStart -> Aligner.Position.BottomStart
        FLayer.Position.BottomCenter -> Aligner.Position.BottomCenter
        FLayer.Position.BottomEnd -> Aligner.Position.BottomEnd
    }
}

private suspend fun AwaitPointerEventScope.layerAwaitFirstDown(
    pass: PointerEventPass
): PointerInputChange {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(pass)
    } while (
        !event.changes.all { it.changedToDown() }
    )
    return event.changes[0]
}

private fun SubcomposeMeasureScope.placeable(
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