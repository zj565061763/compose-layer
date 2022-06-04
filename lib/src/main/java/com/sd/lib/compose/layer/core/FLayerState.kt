package com.sd.lib.compose.layer.core

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

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
    internal var relativeOffset by mutableStateOf(Offset(0f, 0f))

    /** layer要依附的目标信息 */
    var layoutCoordinates: LayoutCoordinates? = null
        set(value) {
            field = value
            updatePosition()
        }

    /** layer要依附的目标大小 */
    internal var targetSize: IntSize? = null
        set(value) {
            field = value
            updatePosition()
        }

    /**
     * 更新layer的位置
     */
    private fun updatePosition() {
        if (alignTarget.not()) return
        val layout = layoutCoordinates ?: return
        if (layout.size.width <= 0 || layout.size.height <= 0) return
        val targetSize = targetSize ?: return
        if (targetSize.width <= 0 || targetSize.height <= 0) return
        val statusBarHeight = windowInsetsStatusBar.getTop(density)

        positionInterceptor?.invoke(layout.size, targetSize)

        val xInPx = with(density) { x.toPx() }
        val yInPx = with(density) { y.toPx() }

        val offset = when (alignment) {
            Alignment.TopStart -> {
                layout.localToWindow(Offset(xInPx, yInPx))
            }
            Alignment.TopCenter -> {
                val offset = (layout.size.width - targetSize.width) / 2
                layout.localToWindow(Offset(xInPx + offset, yInPx))
            }
            Alignment.TopEnd -> {
                val offset = layout.size.width - targetSize.width
                layout.localToWindow(Offset(xInPx + offset, yInPx))
            }
            Alignment.CenterStart -> {
                val offset = (layout.size.height - targetSize.height) / 2
                layout.localToWindow(Offset(xInPx, yInPx + offset))
            }
            Alignment.Center -> {
                val offsetX = (layout.size.width - targetSize.width) / 2
                val offsetY = (layout.size.height - targetSize.height) / 2
                layout.localToWindow(Offset(xInPx + offsetX, yInPx + offsetY))
            }
            Alignment.CenterEnd -> {
                val offsetX = layout.size.width - targetSize.width
                val offsetY = (layout.size.height - targetSize.height) / 2
                layout.localToWindow(Offset(xInPx + offsetX, yInPx + offsetY))
            }
            Alignment.BottomStart -> {
                val offset = layout.size.height - targetSize.height
                layout.localToWindow(Offset(xInPx, yInPx + offset))
            }
            Alignment.BottomCenter -> {
                val offsetX = (layout.size.width - targetSize.width) / 2
                val offsetY = layout.size.height - targetSize.height
                layout.localToWindow(Offset(xInPx + offsetX, yInPx + offsetY))
            }
            Alignment.BottomEnd -> {
                val offsetX = layout.size.width - targetSize.width
                val offsetY = layout.size.height - targetSize.height
                layout.localToWindow(Offset(xInPx + offsetX, yInPx + offsetY))
            }
            else -> {
                Offset.Unspecified
            }
        }

        if (offset != Offset.Unspecified) {
            relativeOffset = offset.copy(y = offset.y - statusBarHeight)
        }
    }
}