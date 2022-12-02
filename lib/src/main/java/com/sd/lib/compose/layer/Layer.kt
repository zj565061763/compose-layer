package com.sd.lib.compose.layer

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    fun setContent(content: @Composable ContentScope.() -> Unit)

    /**
     * 设置对齐的位置
     */
    fun setPosition(position: Position)

    /**
     * 设置窗口行为
     */
    fun setDialogBehavior(block: (DialogBehavior) -> DialogBehavior?)

    /**
     * 是否裁剪内容区域，默认false
     */
    fun setClipToBounds(clipToBounds: Boolean)

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
        val backgroundColor: Color = Color.Black.copy(alpha = 0.3f)
    )

    interface ContentScope {
        /**
         * 内容是否可见
         */
        val isVisible: Boolean
    }
}