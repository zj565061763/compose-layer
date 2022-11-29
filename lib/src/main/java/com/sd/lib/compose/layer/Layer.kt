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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.sd.lib.aligner.Aligner
import com.sd.lib.aligner.FAligner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
    private var _offset = IntOffset.Zero
    private var _offsetInterceptor: (OffsetInterceptorScope.() -> IntOffset)? = null

    private var _checkOverflow by mutableStateOf(false)
    private var _overflowUiState = MutableStateFlow(OverflowUiState.None)

    private var _position by Delegates.observable(Position.Center) { _, _, newValue ->
        _aligner.setPosition(newValue.toAlignerPosition())
    }

    private var _target by Delegates.observable("") { _, oldValue, newValue ->
        if (oldValue != newValue) {
            _targetLayoutCoordinates = _layerManager?.findTarget(newValue)
            _layerManager?.unregisterTargetLayoutCallback(oldValue, _targetLayoutCallback)
            _layerManager?.registerTargetLayoutCallback(newValue, _targetLayoutCallback)
        }
    }

    private var _targetLayoutCoordinates: LayoutCoordinates? by Delegates.observable(null) { _, _, newValue ->
        _targetLayoutInfo.layoutCoordinates = newValue
        updateOffset()
    }

    private var _containerLayoutCoordinates: LayoutCoordinates? by Delegates.observable(null) { _, _, newValue ->
        _sourceContainerLayoutInfo.layoutCoordinates = newValue
        updateOffset()
    }

    private var _layerLayoutCoordinates: LayoutCoordinates? = null
    private var _contentLayoutCoordinates: LayoutCoordinates? by Delegates.observable(null) { _, _, newValue ->
        _sourceLayoutInfo.layoutCoordinates = newValue
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

    private val _targetLayoutInfo = ComposeLayoutInfo()
    private val _sourceLayoutInfo = ComposeLayoutInfo()
    private val _sourceContainerLayoutInfo = ComposeLayoutInfo()

    private val _aligner: Aligner by lazy {
        FAligner().apply {
            this.setPosition(_position.toAlignerPosition())
            this.setTargetLayoutInfo(_targetLayoutInfo)
            this.setSourceLayoutInfo(_sourceLayoutInfo)
            this.setSourceContainerLayoutInfo(_sourceContainerLayoutInfo)
        }
    }

    private fun checkOverflow() {
        if (!_checkOverflow) return

        val source = _sourceLayoutInfo
        val parent = _sourceContainerLayoutInfo
        if (!source.isReady || !parent.isReady) {
            _overflowUiState.value = OverflowUiState.None
            return
        }

        val offset = _offset

        val left = source.coordinate[0] + offset.x
        val parentLeft = parent.coordinate[0]
        val overflowStart = if (left < parentLeft) {
            parentLeft - left
        } else _overflowUiState.value.overflowStart

        val right = left + source.width
        val parentRight = parentLeft + parent.width
        val overflowEnd = if (right > parentRight) {
            right - parentRight
        } else _overflowUiState.value.overflowEnd


        val top = source.coordinate[1] + offset.y
        val parentTop = parent.coordinate[1]
        val overflowTop = if (top < parentTop) {
            parentTop - top
        } else _overflowUiState.value.overflowTop

        val bottom = top + source.height
        val parentBottom = parentTop + parent.height
        val overflowBottom = if (bottom > parentBottom) {
            bottom - parentBottom
        } else _overflowUiState.value.overflowBottom

        _overflowUiState.update {
            it.copy(
                overflowStart = overflowStart,
                overflowEnd = overflowEnd,
                overflowTop = overflowTop,
                overflowBottom = overflowBottom,
            )
        }
    }

    /**
     * 计算位置
     */
    private fun updateOffset() {
        _aligner.update()?.also {
            _offset = IntOffset(it.x, it.y)
            checkOverflow()
        }
        updateUiState()
    }

    private fun updateUiState() {
        val isVisible = if (_isAttached) {
            if (_target.isEmpty()) {
                true
            } else {
                _targetLayoutInfo.isReady
            }
        } else false

        _uiState.value = LayerUiState(
            isVisible = isVisible,
            position = _position,
            hasTarget = _target.isNotEmpty(),
            offset = _offset,
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { uiState.offset },
                ) {
                    if (_checkOverflow) {
                        OverflowContentBox(uiState)
                    } else {
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
    private fun OverflowContentBox(
        uiState: LayerUiState
    ) {
        val overflowUiState by _overflowUiState.collectAsState()
        val density = LocalDensity.current

        BoxWithConstraints {
            var modifier: Modifier = Modifier

            if (overflowUiState.overflowStart != 0
                || overflowUiState.overflowEnd != 0
            ) {
                val offsetDp = with(density) { uiState.offset.x.absoluteValue.toDp() }
                val width = maxWidth - offsetDp
                modifier = modifier.width(width)
            }

            if (overflowUiState.overflowTop != 0
                || overflowUiState.overflowBottom != 0
            ) {
                val offsetDp = with(density) { uiState.offset.y.absoluteValue.toDp() }
                val height = maxHeight - offsetDp
                modifier = modifier.height(height)
            }

            Box(modifier = modifier) {
                ContentBox()
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
}

private data class LayerUiState(
    val isVisible: Boolean = false,
    val position: FLayer.Position = FLayer.Position.Center,
    val hasTarget: Boolean = false,
    val offset: IntOffset = IntOffset.Zero,
)

private data class OverflowUiState(
    val overflowStart: Int,
    val overflowEnd: Int,
    val overflowTop: Int,
    val overflowBottom: Int,
) {
    companion object {
        val None = OverflowUiState(
            overflowStart = 0,
            overflowEnd = 0,
            overflowTop = 0,
            overflowBottom = 0,
        )
    }
}

private class ComposeLayoutInfo : Aligner.LayoutInfo {
    private val _coordinateArray = IntArray(2)
    var layoutCoordinates: LayoutCoordinates? = null

    override val isReady: Boolean
        get() = width > 0 && height > 0 && layoutCoordinates?.isAttached == true

    override val coordinate: IntArray
        get() {
            val layout = layoutCoordinates ?: return Aligner.LayoutInfo.CoordinateUnspecified
            val windowOffset = layout.localToWindow(Offset.Zero)
            _coordinateArray[0] = windowOffset.x.toInt()
            _coordinateArray[1] = windowOffset.y.toInt()
            return _coordinateArray
        }

    override val height: Int
        get() = layoutCoordinates?.size?.height ?: 0

    override val width: Int
        get() = layoutCoordinates?.size?.width ?: 0
}

internal inline fun logMsg(block: () -> String) {
    Log.i("FLayer", block())
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