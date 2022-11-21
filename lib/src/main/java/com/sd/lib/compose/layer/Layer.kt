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

internal val IntOffsetUnspecified = IntOffset(Int.MIN_VALUE, Int.MIN_VALUE)

interface OffsetInterceptorInfo {
    /** 当前计算的layer坐标 */
    val offset: IntOffset

    /** layer大小 */
    val layerSize: IntSize

    /** 目标大小 */
    val targetSize: IntSize
}

/**
 * layer状态管理对象
 */
class FLayer internal constructor() {
    private var _isAttached: Boolean by mutableStateOf(false)
    private var _content: @Composable FLayerScope.() -> Unit by mutableStateOf({ })

    /** 目标Tag */
    private var _targetTag: String by mutableStateOf("")

    /** 对齐方式 */
    var alignment: Alignment by mutableStateOf(Alignment.BottomCenter)

    /** 四条边上居中的时候，是否对齐外边 */
    var centerOutside: Boolean = true
        set(value) {
            field = value
            updatePosition()
        }

    /** 坐标拦截 */
    var offsetInterceptor: (OffsetInterceptorInfo.() -> IntOffset?)? = null

    /** 对齐坐标 */
    private var _layerOffset by mutableStateOf(IntOffsetUnspecified)

    /** layer的大小 */
    private var _layerSize = IntSize.Zero
        set(value) {
            field = value
            updatePosition()

        }

    /** 目标信息 */
    private var _targetLayoutCoordinates: LayoutCoordinates? = null
        set(value) {
            field = value
            updatePosition()

        }

    private val _scopeImpl = FLayerScopeImpl()

    /**
     * 设置内容
     */
    fun setContent(content: @Composable FLayerScope.() -> Unit) {
        _content = content
    }

    /**
     * 添加到容器
     */
    fun attach(tag: String = "") {
        _targetTag = tag
        _isAttached = true
    }

    /**
     * 移除
     */
    fun detach() {
        _isAttached = false
    }

    /**
     * 计算layer的位置
     */
    private fun updatePosition() {
        val targetInfo = _targetLayoutCoordinates
        if (targetInfo == null) {
            _layerOffset = IntOffsetUnspecified
            return
        }

        val targetSize = targetInfo.size
        if (targetSize.width <= 0 || targetSize.height <= 0) {
            _layerOffset = IntOffsetUnspecified
            return
        }

        val layerSize = _layerSize
        if (layerSize.width <= 0 || layerSize.height <= 0) {
            _layerOffset = IntOffsetUnspecified
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
                if (centerOutside) {
                    offsetX = Float.NaN
                    offsetY = -layerSize.height.toFloat()
                }
            }
            Alignment.TopEnd -> {
                offsetX = Utils.getXEnd(targetSize, layerSize)
                offsetY = 0f
            }
            Alignment.CenterStart -> {
                offsetX = 0f
                offsetY = Utils.getYCenter(targetSize, layerSize)
                if (centerOutside) {
                    offsetX = -layerSize.width.toFloat()
                    offsetY = Float.NaN
                }
            }
            Alignment.Center -> {
                offsetX = Utils.getXCenter(targetSize, layerSize)
                offsetY = Utils.getYCenter(targetSize, layerSize)
            }
            Alignment.CenterEnd -> {
                offsetX = Utils.getXEnd(targetSize, layerSize)
                offsetY = Utils.getYCenter(targetSize, layerSize)
                if (centerOutside) {
                    offsetX += layerSize.width.toFloat()
                    offsetY = Float.NaN
                }
            }
            Alignment.BottomStart -> {
                offsetX = 0f
                offsetY = Utils.getYEnd(targetSize, layerSize)
            }
            Alignment.BottomCenter -> {
                offsetX = Utils.getXCenter(targetSize, layerSize)
                offsetY = Utils.getYEnd(targetSize, layerSize)
                if (centerOutside) {
                    offsetX = Float.NaN
                    offsetY += layerSize.height.toFloat()
                }
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

        val offsetInterceptorInfo = object : OffsetInterceptorInfo {
            override val offset: IntOffset
                get() = intOffset
            override val layerSize: IntSize
                get() = layerSize
            override val targetSize: IntSize
                get() = targetSize
        }

        _layerOffset = offsetInterceptor?.invoke(offsetInterceptorInfo) ?: intOffset
    }

    @Composable
    internal fun Content(manager: FLayerManager) {
        _scopeImpl._isVisible = if (_isAttached) {
            if (_targetTag.isEmpty()) {
                true
            } else {
                manager.findTarget(_targetTag).let {
                    _targetLayoutCoordinates = it
                    it != null && it.isAttached
                }
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
}

interface FLayerScope {
    val isVisible: Boolean
}

private class FLayerScopeImpl : FLayerScope {
    var _isVisible by mutableStateOf(false)

    override val isVisible: Boolean
        get() = _isVisible
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