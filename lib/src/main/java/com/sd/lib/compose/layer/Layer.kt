package com.sd.lib.compose.layer

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onSizeChanged
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

    /** layer大小 */
    val layerSize: IntSize

    /** 目标大小 */
    val targetSize: IntSize
}

class FLayer internal constructor() {
    private val _uiState = MutableStateFlow(LayerUiState())
    private var _content: @Composable FLayerScope.() -> Unit by mutableStateOf({ })

    private var _isAttached = false
    private var _offset = IntOffset.Zero
    private var _offsetInterceptor: (OffsetInterceptorScope.() -> IntOffset)? = null

    private var _layerSize by Delegates.observable(IntSize.Zero) { _, oldValue, newValue ->
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

        val layerSize = _layerSize
        if (layerSize.width <= 0 || layerSize.height <= 0) {
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
                offsetX = getXCenter(targetSize, layerSize)
                offsetY = 0f
            }
            Alignment.TopEnd -> {
                offsetX = getXEnd(targetSize, layerSize)
                offsetY = 0f
            }
            Alignment.CenterStart -> {
                offsetX = 0f
                offsetY = getYCenter(targetSize, layerSize)
            }
            Alignment.Center -> {
                offsetX = getXCenter(targetSize, layerSize)
                offsetY = getYCenter(targetSize, layerSize)
            }
            Alignment.CenterEnd -> {
                offsetX = getXEnd(targetSize, layerSize)
                offsetY = getYCenter(targetSize, layerSize)
            }
            Alignment.BottomStart -> {
                offsetX = 0f
                offsetY = getYEnd(targetSize, layerSize)
            }
            Alignment.BottomCenter -> {
                offsetX = getXCenter(targetSize, layerSize)
                offsetY = getYEnd(targetSize, layerSize)
            }
            Alignment.BottomEnd -> {
                offsetX = getXEnd(targetSize, layerSize)
                offsetY = getYEnd(targetSize, layerSize)
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
            override val layerSize: IntSize get() = layerSize
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

        if (uiState.alignTarget) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        uiState.offset
                    }
            ) {
                Box(
                    modifier = Modifier.onSizeChanged {
                        _layerSize = it
                    }
                ) {
                    _content.invoke(_scopeImpl)
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = uiState.alignment
            ) {
                _content.invoke(_scopeImpl)
            }
        }
    }

    private class LayerScopeImpl : FLayerScope {
        var _isVisible by mutableStateOf(false)

        override val isVisible: Boolean
            get() = _isVisible
    }

    companion object {
        fun getXCenter(targetSize: IntSize, layerSize: IntSize): Float {
            return (targetSize.width - layerSize.width) / 2f
        }

        fun getXEnd(targetSize: IntSize, layerSize: IntSize): Float {
            return (targetSize.width - layerSize.width).toFloat()
        }

        fun getYCenter(targetSize: IntSize, layerSize: IntSize): Float {
            return (targetSize.height - layerSize.height) / 2f
        }

        fun getYEnd(targetSize: IntSize, layerSize: IntSize): Float {
            return (targetSize.height - layerSize.height).toFloat()
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