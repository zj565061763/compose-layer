package com.sd.lib.compose.layer

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
)

class FLayer internal constructor() {
    private val _uiState = MutableStateFlow(LayerUiState())
    private var _content: @Composable FLayerScope.() -> Unit by mutableStateOf({ })

    private var _isAttached = false
    private var _offset = IntOffset.Zero
    private var _offsetInterceptor: (OffsetInterceptorScope.() -> IntOffset)? = null

    private var _layerLayout: LayoutCoordinates? = null
    private var _contentLayout: LayoutCoordinates? = null

    private var _contentSize by Delegates.observable(IntSize.Zero) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            updateOffset()
        }
    }

    private var _alignment by Delegates.observable(Alignment.Center) { _, oldValue, newValue ->
        if (oldValue != newValue) {
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

    private var _targetLayoutCoordinates: LayoutCoordinates? = null
        set(value) {
            field = value
            updateOffset()
            updateUiState()
        }

    private var _layerManager: LayerManager? = null
    private val _scopeImpl = LayerScopeImpl()
    private var _dialogBehavior: DialogBehavior? by mutableStateOf(DialogBehavior())

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
        updateOffset()
        updateUiState()
    }

    /**
     * 移除
     */
    fun detach() {
        _isAttached = false
        updateUiState()
    }

    internal fun attachToManager(manager: LayerManager) {
        _layerManager = manager
    }

    internal fun detachFromManager() {
        detach()
        _layerManager = null
    }

    private val _targetLayoutCallback: (LayoutCoordinates?) -> Unit by lazy {
        {
            _targetLayoutCoordinates = it
        }
    }

    /**
     * 计算layer的位置
     */
    private fun updateOffset(): Boolean {
        val targetLayout = _targetLayoutCoordinates ?: return false

        val targetSize = targetLayout.size
        if (targetSize.width <= 0 || targetSize.height <= 0) {
            return false
        }

        val contentSize = _contentSize
        if (contentSize.width <= 0 || contentSize.height <= 0) {
            return false
        }

        var offsetX = 0f
        var offsetY = 0f

        when (_alignment) {
            Alignment.TopStart -> {
                offsetX = 0f
                offsetY = 0f
            }
            Alignment.TopCenter -> {
                offsetX = getXCenter(targetSize, contentSize)
                offsetY = 0f
            }
            Alignment.TopEnd -> {
                offsetX = getXEnd(targetSize, contentSize)
                offsetY = 0f
            }
            Alignment.CenterStart -> {
                offsetX = 0f
                offsetY = getYCenter(targetSize, contentSize)
            }
            Alignment.Center -> {
                offsetX = getXCenter(targetSize, contentSize)
                offsetY = getYCenter(targetSize, contentSize)
            }
            Alignment.CenterEnd -> {
                offsetX = getXEnd(targetSize, contentSize)
                offsetY = getYCenter(targetSize, contentSize)
            }
            Alignment.BottomStart -> {
                offsetX = 0f
                offsetY = getYEnd(targetSize, contentSize)
            }
            Alignment.BottomCenter -> {
                offsetX = getXCenter(targetSize, contentSize)
                offsetY = getYEnd(targetSize, contentSize)
            }
            Alignment.BottomEnd -> {
                offsetX = getXEnd(targetSize, contentSize)
                offsetY = getYEnd(targetSize, contentSize)
            }
            else -> {
                error("unknown Alignment:$_alignment")
            }
        }

        val localOffset = Offset(
            x = offsetX.takeIf { !it.isNaN() } ?: 0f,
            y = offsetY.takeIf { !it.isNaN() } ?: 0f,
        )

        val windowOffset = targetLayout.localToWindow(localOffset)
        val x = windowOffset.x.takeIf { !offsetX.isNaN() } ?: 0f
        val y = windowOffset.y.takeIf { !offsetX.isNaN() } ?: 0f

        val intOffset = IntOffset(
            x = x.toInt(),
            y = y.toInt(),
        )

        val offsetInterceptorScope = object : OffsetInterceptorScope {
            override val offset: IntOffset get() = intOffset
            override val contentSize: IntSize get() = contentSize
            override val targetSize: IntSize get() = targetSize
        }

        _offset = _offsetInterceptor?.invoke(offsetInterceptorScope) ?: intOffset
        updateUiState()

        return true
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

        val modifier = createModifier(uiState.isVisible)
        if (uiState.alignTarget) {
            Box(
                modifier = modifier.offset { uiState.offset }
            ) {
                ContentBox()
            }
        } else {
            Box(
                modifier = modifier,
                contentAlignment = uiState.alignment
            ) {
                ContentBox()
            }
        }
    }

    @Composable
    private fun createModifier(isVisible: Boolean): Modifier {
        val behavior = _dialogBehavior
        var modifier = Modifier.fillMaxSize()
        if (behavior != null && isVisible) {
            modifier = modifier
                .onGloballyPositioned { _layerLayout = it }
                .pointerInput(behavior) {
                    forEachGesture {
                        awaitPointerEventScope {
                            val down = layerAwaitFirstDown()
                            val downPosition = down.position

                            val layerLayout = _layerLayout
                            val contentLayout = _contentLayout
                            if (layerLayout != null && contentLayout != null) {
                                val contentRect = layerLayout.localBoundingBoxOf(contentLayout)
                                if (contentRect.contains(downPosition)) {
                                    // 触摸到内容区域
                                } else {
                                    if (behavior.cancelable && behavior.canceledOnTouchOutside) {
                                        detach()
                                    }
                                }
                            }
                        }
                    }
                }
        }
        return modifier
    }

    @Composable
    private fun ContentBox(
        modifier: Modifier = Modifier,
    ) {
        Box(
            modifier = modifier.onGloballyPositioned {
                _contentLayout = it
                _contentSize = it.size
            }
        ) {
            _content.invoke(_scopeImpl)
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

internal inline fun logMsg(block: () -> String) {
    Log.i("FLayer", block())
}

private suspend fun AwaitPointerEventScope.layerAwaitFirstDown(): PointerInputChange {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(PointerEventPass.Initial)
    } while (
        !event.changes.all { it.changedToDownIgnoreConsumed() }
    )
    return event.changes[0]
}