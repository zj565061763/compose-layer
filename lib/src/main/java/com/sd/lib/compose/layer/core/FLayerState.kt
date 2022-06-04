package com.sd.lib.compose.layer.core

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.*

/**
 * layer状态管理对象
 */
class FLayerState {
    /** [alignTarget]为true，表示相对于目标对齐，false表示相对于容器对齐 */
    internal var alignTarget: Boolean = true

    /** 对齐方式 */
    var alignment by mutableStateOf(Alignment.Center)

    /** 对齐后x方向偏移量 */
    var x by mutableStateOf(0.dp)

    /** 对齐后y方向偏移量 */
    var y by mutableStateOf(0.dp)

    /** 屏幕密度 */
    lateinit var density: Density
        internal set

    var positionInterceptor: ((layerSize: IntSize, targetSize: IntSize) -> Unit)? = null

    /** layer内容 */
    internal lateinit var content: @Composable (() -> Unit)
    /** 状态栏信息 */
    internal lateinit var windowInsetsStatusBar: WindowInsets
    /** 当[alignTarget]为true的时候，layer的坐标 */
    internal var layerOffset by mutableStateOf(IntOffset(0, 0))

    /** 目标信息 */
    var targetLayoutCoordinates: LayoutCoordinates? = null
        set(value) {
            field = value
            updatePosition()
        }

    /** layer的大小 */
    internal var layerSize: IntSize? = null
        set(value) {
            field = value
            updatePosition()
        }

    /**
     * 更新layer的位置
     */
    private fun updatePosition() {
        if (alignTarget.not()) return

        val targetInfo = targetLayoutCoordinates ?: return
        if (targetInfo.size.width <= 0 || targetInfo.size.height <= 0) return

        val layerSize = layerSize ?: return
        if (layerSize.width <= 0 || layerSize.height <= 0) return

        positionInterceptor?.invoke(targetInfo.size, layerSize)

        val xInPx = with(density) { x.toPx() }
        val yInPx = with(density) { y.toPx() }

        val offset = when (alignment) {
            Alignment.TopStart -> {
                targetInfo.localToWindow(Offset(xInPx, yInPx))
            }
            Alignment.TopCenter -> {
                val offsetX = Utils.getXCenter(targetInfo.size, layerSize, xInPx)
                targetInfo.localToWindow(Offset(offsetX, yInPx))
            }
            Alignment.TopEnd -> {
                val offsetX = Utils.getXEnd(targetInfo.size, layerSize, xInPx)
                targetInfo.localToWindow(Offset(offsetX, yInPx))
            }
            Alignment.CenterStart -> {
                val offsetY = Utils.getYCenter(targetInfo.size, layerSize, yInPx)
                targetInfo.localToWindow(Offset(xInPx, offsetY))
            }
            Alignment.Center -> {
                val offsetX = Utils.getXCenter(targetInfo.size, layerSize, xInPx)
                val offsetY = Utils.getYCenter(targetInfo.size, layerSize, yInPx)
                targetInfo.localToWindow(Offset(offsetX, offsetY))
            }
            Alignment.CenterEnd -> {
                val offsetX = Utils.getXEnd(targetInfo.size, layerSize, xInPx)
                val offsetY = Utils.getYCenter(targetInfo.size, layerSize, yInPx)
                targetInfo.localToWindow(Offset(offsetX, offsetY))
            }
            Alignment.BottomStart -> {
                val offsetY = Utils.getYEnd(targetInfo.size, layerSize, yInPx)
                targetInfo.localToWindow(Offset(xInPx, offsetY))
            }
            Alignment.BottomCenter -> {
                val offsetX = Utils.getXCenter(targetInfo.size, layerSize, xInPx)
                val offsetY = Utils.getYEnd(targetInfo.size, layerSize, yInPx)
                targetInfo.localToWindow(Offset(offsetX, offsetY))
            }
            Alignment.BottomEnd -> {
                val offsetX = Utils.getXEnd(targetInfo.size, layerSize, xInPx)
                val offsetY = Utils.getYEnd(targetInfo.size, layerSize, yInPx)
                targetInfo.localToWindow(Offset(xInPx + offsetX, yInPx + offsetY))
            }
            else -> {
                Offset.Unspecified
            }
        }

        if (offset != Offset.Unspecified) {
            val statusBarHeight = windowInsetsStatusBar.getTop(density)
            layerOffset = IntOffset(x = offset.x.toInt(), y = offset.y.toInt() - statusBarHeight)
        }
    }

    internal fun transformConstraints(constraints: Constraints): Constraints {
        val targetSize = targetLayoutCoordinates?.size ?: return constraints
        return when (alignment) {
            Alignment.TopStart -> {
                val maxWidth = Utils.getMaxWidthStart(constraints, targetSize, layerOffset)
                val maxHeight = Utils.getMaxHeightTop(constraints, targetSize, layerOffset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.TopCenter -> {
                val maxWidth = Utils.getMaxWidthCenter(constraints, targetSize, layerOffset)
                val maxHeight = Utils.getMaxHeightTop(constraints, targetSize, layerOffset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.TopEnd -> {
                val maxWidth = Utils.getMaxWidthEnd(constraints, targetSize, layerOffset)
                val maxHeight = Utils.getMaxHeightTop(constraints, targetSize, layerOffset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.CenterStart -> {
                val maxWidth = Utils.getMaxWidthStart(constraints, targetSize, layerOffset)
                val maxHeight = Utils.getMaxHeightCenter(constraints, targetSize, layerOffset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.Center -> {
                val maxWidth = Utils.getMaxWidthCenter(constraints, targetSize, layerOffset)
                val maxHeight = Utils.getMaxHeightCenter(constraints, targetSize, layerOffset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.CenterEnd -> {
                val maxWidth = Utils.getMaxWidthEnd(constraints, targetSize, layerOffset)
                val maxHeight = Utils.getMaxHeightCenter(constraints, targetSize, layerOffset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.BottomStart -> {
                val maxWidth = Utils.getMaxWidthStart(constraints, targetSize, layerOffset)
                val maxHeight = Utils.getMaxHeightBottom(constraints, targetSize, layerOffset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.BottomCenter -> {
                val maxWidth = Utils.getMaxWidthCenter(constraints, targetSize, layerOffset)
                val maxHeight = Utils.getMaxHeightBottom(constraints, targetSize, layerOffset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            Alignment.BottomEnd -> {
                val maxWidth = Utils.getMaxWidthEnd(constraints, targetSize, layerOffset)
                val maxHeight = Utils.getMaxHeightBottom(constraints, targetSize, layerOffset)
                constraints.copy(maxWidth = maxWidth, maxHeight = maxHeight)
            }
            else -> {
                constraints
            }
        }
    }
}

private object Utils {
    fun getXCenter(targetSize: IntSize, layerSize: IntSize, delta: Float): Float {
        val offset = (targetSize.width - layerSize.width) / 2
        return offset + delta
    }

    fun getXEnd(targetSize: IntSize, layerSize: IntSize, delta: Float): Float {
        val offset = targetSize.width - layerSize.width
        return offset + delta
    }

    fun getYCenter(targetSize: IntSize, layerSize: IntSize, delta: Float): Float {
        val offset = (targetSize.height - layerSize.height) / 2
        return offset + delta
    }

    fun getYEnd(targetSize: IntSize, layerSize: IntSize, delta: Float): Float {
        val offset = targetSize.height - layerSize.height
        return offset + delta
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