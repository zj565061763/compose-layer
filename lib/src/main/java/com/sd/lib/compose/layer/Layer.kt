package com.sd.lib.compose.layer

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.sd.lib.aligner.Aligner
import com.sd.lib.aligner.FAligner
import kotlinx.coroutines.flow.MutableStateFlow
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

    private var _alignment by Delegates.observable(Alignment.Center) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            updateAlignment()
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
        _targetLayoutInfo.layoutCoordinates = newValue
        updateOffset()
    }

    private var _containerLayoutCoordinates: LayoutCoordinates? by Delegates.observable(null) { _, _, newValue ->
        _sourceLayoutInfo.parentLayerLayoutInfo.layoutCoordinates = newValue
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
     * 设置对齐
     */
    fun setAlignment(alignment: Alignment) {
        _alignment = alignment
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

    private val _targetLayoutInfo = LayerLayoutInfo()
    private val _sourceLayoutInfo = SourceLayerLayoutInfo()
    private val _aligner by lazy {
        FAligner().apply {
            this.targetLayoutInfo = _targetLayoutInfo
            this.sourceLayoutInfo = _sourceLayoutInfo
            this.callback = object : Aligner.Callback() {
                override fun onUpdate(x: Int, y: Int, source: Aligner.SourceLayoutInfo, target: Aligner.LayoutInfo) {
                    val intOffset = IntOffset(x, y)

                    val offsetInterceptorScope = object : OffsetInterceptorScope {
                        override val offset: IntOffset get() = intOffset
                        override val contentSize: IntSize get() = _contentLayoutCoordinates?.size ?: IntSize.Zero
                        override val targetSize: IntSize get() = _targetLayoutCoordinates?.size ?: IntSize.Zero
                    }

                    _offset = _offsetInterceptor?.invoke(offsetInterceptorScope) ?: intOffset
                    updateUiState()
                }
            }
        }
    }

    private fun updateAlignment() {
        when (_alignment) {
            Alignment.TopStart -> _aligner.position = Aligner.Position.TopLeft
            Alignment.TopCenter -> _aligner.position = Aligner.Position.TopCenter
            Alignment.TopEnd -> _aligner.position = Aligner.Position.TopRight

            Alignment.CenterStart -> _aligner.position = Aligner.Position.LeftCenter
            Alignment.Center -> _aligner.position = Aligner.Position.Center
            Alignment.CenterEnd -> _aligner.position = Aligner.Position.RightCenter

            Alignment.BottomStart -> _aligner.position = Aligner.Position.BottomLeft
            Alignment.BottomCenter -> _aligner.position = Aligner.Position.BottomCenter
            Alignment.BottomEnd -> _aligner.position = Aligner.Position.BottomRight
        }
    }

    /**
     * 计算位置
     */
    private fun updateOffset(): Boolean {
        return _aligner.update()
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
            alignment = _alignment,
            alignTarget = _targetLayoutCoordinates != null,
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

        if (uiState.alignTarget) {
            LayerBox(uiState.isVisible) {
                BackgroundBox(uiState.isVisible)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { uiState.offset },
                ) {
                    ContentBox()
                }
            }
        } else {
            LayerBox(uiState.isVisible) {
                BackgroundBox(uiState.isVisible)
                ContentBox(modifier = Modifier.align(uiState.alignment))
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
            val alpha by animateFloatAsState(targetValue = if (isVisible) 1.0f else 0f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha)
                    .background(behavior.backgroundColor)
            )
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

    companion object {
        fun getXCenter(targetSize: IntSize, contentSize: IntSize): Float {
            return (targetSize.width - contentSize.width) / 2f
        }

        fun getXEnd(targetSize: IntSize, contentSize: IntSize): Float {
            return (targetSize.width - contentSize.width).toFloat()
        }

        fun getYCenter(targetSize: IntSize, contentSize: IntSize): Float {
            return (targetSize.height - contentSize.height) / 2f
        }

        fun getYEnd(targetSize: IntSize, contentSize: IntSize): Float {
            return (targetSize.height - contentSize.height).toFloat()
        }
    }
}

private data class LayerUiState(
    val isVisible: Boolean = false,
    val alignment: Alignment = Alignment.Center,
    val alignTarget: Boolean = false,
    val offset: IntOffset = IntOffset.Zero,
)

private open class LayerLayoutInfo() : Aligner.LayoutInfo {
    private val _coordinateArray = IntArray(2)
    var layoutCoordinates: LayoutCoordinates? = null

    override val isReady: Boolean
        get() = width > 0 && height > 0

    override val coordinate: IntArray
        get() {
            val layout = layoutCoordinates ?: return Aligner.LayoutInfo.coordinateUnspecified
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

private class SourceLayerLayoutInfo() : LayerLayoutInfo(), Aligner.SourceLayoutInfo {
    val parentLayerLayoutInfo = LayerLayoutInfo()

    override val parentLayoutInfo: Aligner.LayoutInfo
        get() = parentLayerLayoutInfo
}

internal inline fun logMsg(block: () -> String) {
    Log.i("FLayer", block())
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