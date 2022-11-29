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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.sd.lib.aligner.Aligner
import com.sd.lib.aligner.FAligner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.absoluteValue
import kotlin.properties.Delegates

interface FLayerScope {
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
    private var _content: @Composable FLayerScope.() -> Unit by mutableStateOf({ })

    private var _isAttached = false
    private var _offsetInterceptor: (OffsetInterceptorScope.() -> IntOffset)? = null

    private var _checkOverflowDirection: Direction? = Direction.Top
    private var _overflowFixedWidth: Int? = null
    private var _overflowFixedHeight: Int? = null

    private var _position by Delegates.observable(Position.Center) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            _overflowFixedWidth = null
            _overflowFixedHeight = null
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
    private val _scopeImpl = LayerScopeImpl()
    private var _dialogBehavior: DialogBehavior? by mutableStateOf(DialogBehavior())

    /** 是否可见 */
    var isVisible by mutableStateOf(false)
        private set

    /**
     * 设置内容
     */
    fun setContent(content: @Composable FLayerScope.() -> Unit) {
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
        val target = _targetLayoutCoordinates
        if (!target.isReady()) {
            updateUiState()
            return
        }

        val source = _contentLayoutCoordinates
        if (!source.isReady()) return

        val container = _containerLayoutCoordinates
        if (!container.isReady()) return

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

        _alignerResult = _aligner.align(input)
        updateUiState()
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
        )

        this.isVisible = isVisible
    }

    @Composable
    internal fun Content() {
        val uiState by _uiState.collectAsState()

        LaunchedEffect(uiState.isVisible) {
            _scopeImpl._isVisible = uiState.isVisible
        }

        val behavior = _dialogBehavior
        if (behavior != null) {
            BackHandler(uiState.isVisible) {
                if (behavior.cancelable) {
                    detach()
                }
            }
        }

        if (uiState.hasTarget) {
            LayerBox(uiState.isVisible) {
                BackgroundBox(uiState.isVisible)
                HasTargetBox(uiState.alignerResult) {
                    ContentBox()
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
    private fun HasTargetBox(
        result: Aligner.Result?,
        content: @Composable () -> Unit,
    ) {
        SubcomposeLayout(Modifier.fillMaxSize()) { constraints ->
            val measurable = subcompose(Unit) { content() }.let {
                check(it.size == 1)
                it.first()
            }

            var x = result?.x ?: 0
            var y = result?.y ?: 0

            var overflowResult: Aligner.Result? = null
            var constraintsCopy = constraints.copy(minWidth = 0, minHeight = 0)

            if (result != null) {
                _checkOverflowDirection?.let {
                    when (it) {
                        Direction.Start -> {
                            StartOverflowHandler().fixOverflow(result)?.let {
                                constraintsCopy = constraintsCopy.copy(maxWidth = it)
                                overflowResult = result
                            }
                        }
                        Direction.End -> {
                            EndOverflowHandler().fixOverflow(result)?.let {
                                constraintsCopy = constraintsCopy.copy(maxWidth = it)
                                overflowResult = result
                            }
                        }
                        Direction.Top -> {
                            TopOverflowHandler().fixOverflow(result)?.let {
                                constraintsCopy = constraintsCopy.copy(maxHeight = it)
                                overflowResult = result
                            }
                        }
                        Direction.Bottom -> {
                            BottomOverflowHandler().fixOverflow(result)?.let {
                                constraintsCopy = constraintsCopy.copy(maxHeight = it)
                                overflowResult = result
                            }
                        }
                    }
                }
            }

            val placeable = measurable.measure(constraintsCopy)
            logMsg { "placeable (${placeable.width}, ${placeable.height})" }

            overflowResult?.let {
                logMsg { "old result (${it.x}, ${it.y})" }

                val newResult = _aligner.align(
                    it.input.copy(
                        sourceWidth = placeable.width,
                        sourceHeight = placeable.height,
                    )
                )
                x = newResult.x
                y = newResult.y

                logMsg { "new result (${newResult.x}, ${newResult.y})" }
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.placeRelative(x, y)
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
            _content.invoke(_scopeImpl)
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

    private abstract class OverflowHandler {
        fun fixOverflow(result: Aligner.Result): Int? {
            val overflow = getValue(result.overflow)
            if (overflow > 0) {
                val newSize = (getSize(result) - overflow).coerceAtLeast(1)
                return newSize.also { setCacheSize(it) }
            } else {
                val cacheSize = getCacheSize()
                if (cacheSize != null) {
                    return if (overflow < 0) {
                        (cacheSize + overflow.absoluteValue).also { setCacheSize(it) }
                    } else {
                        cacheSize
                    }
                }
            }
            return null
        }

        protected abstract fun getSize(result: Aligner.Result): Int

        protected abstract fun getValue(overflow: Aligner.Overflow): Int

        protected abstract fun getCacheSize(): Int?

        protected abstract fun setCacheSize(size: Int)
    }

    private abstract inner class HorizontalOverflowHandler : OverflowHandler() {
        override fun getSize(result: Aligner.Result): Int = result.input.sourceWidth
        override fun getCacheSize(): Int? = _overflowFixedWidth
        override fun setCacheSize(size: Int) {
            _overflowFixedWidth = size
        }
    }

    private abstract inner class VerticalOverflowHandler : OverflowHandler() {
        override fun getSize(result: Aligner.Result): Int = result.input.sourceHeight
        override fun getCacheSize(): Int? = _overflowFixedHeight
        override fun setCacheSize(size: Int) {
            _overflowFixedHeight = size
        }
    }

    private inner class StartOverflowHandler : HorizontalOverflowHandler() {
        override fun getValue(overflow: Aligner.Overflow): Int = overflow.start
    }

    private inner class EndOverflowHandler : HorizontalOverflowHandler() {
        override fun getValue(overflow: Aligner.Overflow): Int = overflow.end
    }

    private inner class TopOverflowHandler : VerticalOverflowHandler() {
        override fun getValue(overflow: Aligner.Overflow): Int = overflow.top
    }

    private inner class BottomOverflowHandler : VerticalOverflowHandler() {
        override fun getValue(overflow: Aligner.Overflow): Int = overflow.bottom
    }

    private class LayerScopeImpl : FLayerScope {
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

    enum class Direction {
        Start,
        End,
        Top,
        Bottom,
    }
}

private data class LayerUiState(
    val isVisible: Boolean = false,
    val position: FLayer.Position = FLayer.Position.Center,
    val hasTarget: Boolean = false,
    val alignerResult: Aligner.Result? = null,
)

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