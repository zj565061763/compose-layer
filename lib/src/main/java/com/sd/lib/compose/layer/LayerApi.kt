package com.sd.lib.compose.layer

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

interface LayerContentScope {
    /** 内容是否可见 */
    val isVisible: Boolean
}


interface Layer {
    /**
     * 当前Layer是否可见
     */
    val isVisibleState: Boolean

    /**
     * 位置
     */
    val positionState: Position

    /**
     * 窗口行为
     */
    val dialogBehaviorState: DialogBehavior?

    /**
     * 设置内容
     */
    fun setContent(content: @Composable LayerContentScope.() -> Unit)

    /**
     * 设置对齐的位置
     */
    fun setPosition(position: Position)

    /**
     * 设置窗口行为
     */
    fun setDialogBehavior(block: (DialogBehavior) -> DialogBehavior?)

    /**
     * 添加到容器
     */
    fun attach()

    /**
     * 从容器上移除
     */
    fun detach()

    /**
     * 更新当前Layer所在的容器
     */
    @Composable
    fun UpdateContainer()

    enum class Position {
        /** 顶部开始对齐 */
        TopStart,
        /** 顶部中间对齐 */
        TopCenter,
        /** 顶部结束对齐 */
        TopEnd,

        /** 底部开始对齐 */
        BottomStart,
        /** 底部中间对齐 */
        BottomCenter,
        /** 底部结束对齐 */
        BottomEnd,

        /** 开始顶部对齐 */
        StartTop,
        /** 开始中间对齐 */
        StartCenter,
        /** 开始底部对齐 */
        StartBottom,

        /** 开始顶部对齐 */
        EndTop,
        /** 开始中间对齐 */
        EndCenter,
        /** 开始底部对齐 */
        EndBottom,

        /** 中间对齐 */
        Center,
    }

    data class DialogBehavior(
        /** 按返回键是否可以关闭 */
        val cancelable: Boolean = true,

        /** 触摸到非内容区域是否关闭 */
        val canceledOnTouchOutside: Boolean = true,

        /** 是否消费掉触摸到非内容区域的触摸事件 */
        val consumeTouchOutside: Boolean = true,

        /** 背景颜色 */
        val backgroundColor: Color = Color.Black.copy(alpha = 0.25f)
    )
}

interface OffsetTransformScope {
    /** 当前计算的layer坐标 */
    val offset: IntOffset

    /** 内容大小 */
    val contentSize: IntSize

    /** 目标大小 */
    val targetSize: IntSize
}

interface TargetLayer : Layer {
    /**
     * 设置目标
     */
    fun setTarget(target: String)

    /**
     * 设置坐标转换
     */
    fun setOffsetTransform(interceptor: (OffsetTransformScope.() -> IntOffset)?)

    /**
     * 设置修复溢出的方向[OverflowDirection]
     */
    fun setFixOverflowDirection(direction: Int)

    class OverflowDirection {
        companion object {
            const val None = 0
            const val Top = 1
            const val Bottom = 2
            const val Start = 4
            const val End = 8

            fun hasTop(value: Int) = Top and value != 0
            fun hasBottom(value: Int) = Bottom and value != 0
            fun hasStart(value: Int) = Start and value != 0
            fun hasEnd(value: Int) = End and value != 0
        }
    }
}
