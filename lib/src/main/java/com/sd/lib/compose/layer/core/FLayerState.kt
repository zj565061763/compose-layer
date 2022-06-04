package com.sd.lib.compose.layer.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

internal val IntOffsetUnspecified = IntOffset(Int.MIN_VALUE, Int.MIN_VALUE)

interface OffsetInterceptorInfo {
    val offset: IntOffset
    val layerSize: IntSize
    val targetSize: IntSize
}

/**
 * layer状态管理对象
 */
class FLayerState {
    /** 对齐方式 */
    var alignment by mutableStateOf(Alignment.BottomCenter)

    /** 四条边上居中的时候，是否对齐外边 */
    var centerOutside: Boolean = true

    /** 坐标拦截 */
    var offsetInterceptor: (OffsetInterceptorInfo.() -> IntOffset?)? = null

    /** [alignTarget]为true，表示相对于目标对齐，false表示相对于容器对齐 */
    internal var alignTarget: Boolean = true
    /** layer内容 */
    internal lateinit var content: @Composable (() -> Unit)
    /** 对齐坐标 */
    internal var layerOffset by mutableStateOf(IntOffsetUnspecified)

    /** 状态栏高度 */
    internal var statusBarHeight = 0
        set(value) {
            field = value
            updatePosition()
        }

    /** 目标信息 */
    internal var targetLayoutCoordinates: LayoutCoordinates? = null
        set(value) {
            field = value
            updatePosition()
        }

    /** layer的大小 */
    internal var layerSize = IntSize.Zero
        set(value) {
            field = value
            updatePosition()
        }


    /**
     * 计算layer的位置
     */
    internal fun updatePosition() {
        val targetInfo = targetLayoutCoordinates ?: return
        val targetSize = targetInfo.size
        if (targetSize.width <= 0 || targetSize.height <= 0) return

        val layerSize = layerSize
        if (layerSize.width <= 0 || layerSize.height <= 0) return

        val offset = when (alignment) {
            Alignment.TopStart -> {
                Offset(0f, 0f)
            }
            Alignment.TopCenter -> {
                val offsetX = Utils.getXCenter(targetSize, layerSize)
                Offset(offsetX, 0f.takeUnless { centerOutside } ?: -layerSize.height.toFloat())
            }
            Alignment.TopEnd -> {
                val offsetX = Utils.getXEnd(targetSize, layerSize)
                Offset(offsetX, 0f)
            }
            Alignment.CenterStart -> {
                val offsetY = Utils.getYCenter(targetSize, layerSize)
                Offset(0f.takeUnless { centerOutside } ?: -layerSize.width.toFloat(), offsetY)
            }
            Alignment.Center -> {
                val offsetX = Utils.getXCenter(targetSize, layerSize)
                val offsetY = Utils.getYCenter(targetSize, layerSize)
                Offset(offsetX, offsetY)
            }
            Alignment.CenterEnd -> {
                val offsetX = Utils.getXEnd(targetSize, layerSize)
                val offsetY = Utils.getYCenter(targetSize, layerSize)
                Offset(offsetX + (0f.takeUnless { centerOutside } ?: layerSize.width.toFloat()), offsetY)
            }
            Alignment.BottomStart -> {
                val offsetY = Utils.getYEnd(targetSize, layerSize)
                Offset(0f, offsetY)
            }
            Alignment.BottomCenter -> {
                val offsetX = Utils.getXCenter(targetSize, layerSize)
                val offsetY = Utils.getYEnd(targetSize, layerSize)
                Offset(offsetX, offsetY + (0f.takeUnless { centerOutside } ?: layerSize.height.toFloat()))
            }
            Alignment.BottomEnd -> {
                val offsetX = Utils.getXEnd(targetSize, layerSize)
                val offsetY = Utils.getYEnd(targetSize, layerSize)
                Offset(offsetX, offsetY)
            }
            else -> {
                Offset.Unspecified
            }
        }

        if (offset != Offset.Unspecified) {
            val windowOffset = targetInfo.localToWindow(offset)
            val intOffset = IntOffset(x = windowOffset.x.toInt(), y = windowOffset.y.toInt() - statusBarHeight)

            val offsetInterceptorInfo = object : OffsetInterceptorInfo {
                override val offset: IntOffset
                    get() = intOffset
                override val layerSize: IntSize
                    get() = layerSize
                override val targetSize: IntSize
                    get() = targetSize
            }
            layerOffset = offsetInterceptor?.invoke(offsetInterceptorInfo) ?: intOffset
        }
    }

    internal fun transformConstraints(constraints: Constraints, offset: IntOffset): Constraints {
        val targetSize = targetLayoutCoordinates?.size ?: return constraints
        return when (alignment) {
            Alignment.TopStart -> {
                val maxWidth = Utils.getMaxWidthStart(constraints, targetSize, offset)
                val maxHeight = Utils.getMaxHeightTop(constraints, targetSize, offset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.TopCenter -> {
                val maxWidth = Utils.getMaxWidthCenter(constraints, targetSize, offset)
                val maxHeight = Utils.getMaxHeightTop(constraints, targetSize, offset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.TopEnd -> {
                val maxWidth = Utils.getMaxWidthEnd(constraints, targetSize, offset)
                val maxHeight = Utils.getMaxHeightTop(constraints, targetSize, offset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.CenterStart -> {
                val maxWidth = Utils.getMaxWidthStart(constraints, targetSize, offset)
                val maxHeight = Utils.getMaxHeightCenter(constraints, targetSize, offset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.Center -> {
                val maxWidth = Utils.getMaxWidthCenter(constraints, targetSize, offset)
                val maxHeight = Utils.getMaxHeightCenter(constraints, targetSize, offset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.CenterEnd -> {
                val maxWidth = Utils.getMaxWidthEnd(constraints, targetSize, offset)
                val maxHeight = Utils.getMaxHeightCenter(constraints, targetSize, offset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.BottomStart -> {
                val maxWidth = Utils.getMaxWidthStart(constraints, targetSize, offset)
                val maxHeight = Utils.getMaxHeightBottom(constraints, targetSize, offset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.BottomCenter -> {
                val maxWidth = Utils.getMaxWidthCenter(constraints, targetSize, offset)
                val maxHeight = Utils.getMaxHeightBottom(constraints, targetSize, offset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.BottomEnd -> {
                val maxWidth = Utils.getMaxWidthEnd(constraints, targetSize, offset)
                val maxHeight = Utils.getMaxHeightBottom(constraints, targetSize, offset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            else -> {
                constraints
            }
        }
    }
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

    fun getMaxWidthStart(constraints: Constraints, targetSize: IntSize, offset: IntOffset): Int {
        return (constraints.maxWidth - offset.x).coerceAtLeast(0)
    }

    fun getMaxWidthCenter(constraints: Constraints, targetSize: IntSize, offset: IntOffset): Int {
        return constraints.maxWidth
    }

    fun getMaxWidthEnd(constraints: Constraints, targetSize: IntSize, offset: IntOffset): Int {
        return (offset.x + targetSize.width)
    }

    fun getMaxHeightTop(constraints: Constraints, targetSize: IntSize, offset: IntOffset): Int {
        return (constraints.maxHeight - offset.y).coerceAtLeast(0)
    }

    fun getMaxHeightCenter(constraints: Constraints, targetSize: IntSize, offset: IntOffset): Int {
        return constraints.maxHeight
    }

    fun getMaxHeightBottom(constraints: Constraints, targetSize: IntSize, offset: IntOffset): Int {
        return (offset.y + targetSize.height)
    }
}