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
import kotlin.properties.Delegates

private val LayoutOffsetUnspecified = IntOffset.Zero

/**
 * layer状态管理对象
 */
class FLayer internal constructor() {
    private var _isAttached: Boolean by mutableStateOf(false)
    private var _content: @Composable FLayerScope.() -> Unit by mutableStateOf({ })

    /** 对齐方式 */
    var alignment: Alignment by mutableStateOf(Alignment.BottomCenter)

    /** 坐标拦截 */
    var offsetInterceptor: (OffsetInterceptorScope.() -> IntOffset?)? = null

    /** 对齐坐标 */
    private var _layerOffset by mutableStateOf(LayoutOffsetUnspecified)

    /** layer的大小 */
    private var _layerSize by Delegates.observable(IntSize.Zero) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            updatePosition()
        }
    }

    /** 目标 */
    private var _target by Delegates.observable("") { _, oldValue, newValue ->
        if (oldValue != newValue) {
            _layerManager?.unregisterTargetLayoutCallback(oldValue, _targetLayoutCallback)
            _layerManager?.registerTargetLayoutCallback(newValue, _targetLayoutCallback)
        }
    }

    /** 目标信息 */
    private var _targetLayoutCoordinates: LayoutCoordinates? = null
        set(value) {
            field = value
            updatePosition()
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
     * 添加到容器
     */
    fun attach(target: String = "") {
        _target = target
        _targetLayoutCoordinates = _layerManager?.findTarget(target)
        _isAttached = true
    }

    /**
     * 移除
     */
    fun detach() {
        _isAttached = false
        _target = ""
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
    private fun updatePosition() {
        val targetInfo = _targetLayoutCoordinates
        if (targetInfo == null) {
            _layerOffset = LayoutOffsetUnspecified
            return
        }

        val targetSize = targetInfo.size
        if (targetSize.width <= 0 || targetSize.height <= 0) {
            _layerOffset = LayoutOffsetUnspecified
            return
        }

        val layerSize = _layerSize
        if (layerSize.width <= 0 || layerSize.height <= 0) {
            _layerOffset = LayoutOffsetUnspecified
            return
        }

        var offsetX = 0f
        var offsetY = 0f

        when (alignment) {
            Alignment.TopStart -> {
                offsetX = 0f
                offsetY = 0f
            }
            Alignment.TopCenter -> {
                offsetX = Utils.getXCenter(targetSize, layerSize)
                offsetY = 0f
            }
            Alignment.TopEnd -> {
                offsetX = Utils.getXEnd(targetSize, layerSize)
                offsetY = 0f
            }
            Alignment.CenterStart -> {
                offsetX = 0f
                offsetY = Utils.getYCenter(targetSize, layerSize)
            }
            Alignment.Center -> {
                offsetX = Utils.getXCenter(targetSize, layerSize)
                offsetY = Utils.getYCenter(targetSize, layerSize)
            }
            Alignment.CenterEnd -> {
                offsetX = Utils.getXEnd(targetSize, layerSize)
                offsetY = Utils.getYCenter(targetSize, layerSize)
            }
            Alignment.BottomStart -> {
                offsetX = 0f
                offsetY = Utils.getYEnd(targetSize, layerSize)
            }
            Alignment.BottomCenter -> {
                offsetX = Utils.getXCenter(targetSize, layerSize)
                offsetY = Utils.getYEnd(targetSize, layerSize)
            }
            Alignment.BottomEnd -> {
                offsetX = Utils.getXEnd(targetSize, layerSize)
                offsetY = Utils.getYEnd(targetSize, layerSize)
            }
            else -> {
                error("unknown Alignment:$alignment")
            }
        }

        val localOffset = Offset(
            x = offsetX.takeIf { !it.isNaN() } ?: 0f,
            y = offsetY.takeIf { !it.isNaN() } ?: 0f,
        )

        val windowOffset = targetInfo.localToWindow(localOffset)
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

        _layerOffset = offsetInterceptor?.invoke(offsetInterceptorScope) ?: intOffset
    }

    @Composable
    internal fun Content() {
        _scopeImpl._isVisible = if (_isAttached) {
            if (_target.isEmpty()) {
                true
            } else {
                _targetLayoutCoordinates?.isAttached ?: false
            }
        } else {
            false
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (_targetLayoutCoordinates != null) {
                LaunchedEffect(alignment) {
                    updatePosition()
                }

                Box(modifier = Modifier
                    .onSizeChanged {
                        _layerSize = it
                    }
                    .offset {
                        _layerOffset
                    }
                ) {
                    _content.invoke(_scopeImpl)
                }
            } else {
                Box(
                    modifier = Modifier.align(alignment)
                ) {
                    _content.invoke(_scopeImpl)
                }
            }
        }
    }

    private class LayerScopeImpl : FLayerScope {
        var _isVisible by mutableStateOf(false)

        override val isVisible: Boolean
            get() = _isVisible
    }
}

interface FLayerScope {
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

private object Utils {
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

internal inline fun logMsg(block: () -> String) {
    Log.i("FLayer", block())
}