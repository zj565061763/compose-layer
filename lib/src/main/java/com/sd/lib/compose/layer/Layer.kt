package com.sd.lib.compose.layer

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * 用来存放Layer的容器
 */
@Composable
fun LayerContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val layerManager = remember { LayerManager() }
    CompositionLocalProvider(LocalLayerManager provides layerManager) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    layerManager.updateContainerLayout(it)
                },
            contentAlignment = Alignment.Center,
        ) {
            content()
            layerManager.Layers()
        }
    }
}

/**
 * 创建并记住[Layer]
 */
@Composable
fun rememberLayer(): Layer {
    val layerManager = checkNotNull(LocalLayerManager.current) {
        "CompositionLocal LocalLayerManager not present"
    }
    return layerManager.rememberLayer()
}

/**
 * 创建并记住[TargetLayer]
 */
@Composable
fun rememberTargetLayer(): TargetLayer {
    val layerManager = checkNotNull(LocalLayerManager.current) {
        "CompositionLocal LocalLayerManager not present"
    }
    return layerManager.rememberTargetLayer()
}

/**
 * 设置要对齐的目标并绑定唯一的[tag]
 */
fun Modifier.layerTarget(
    tag: String,
) = composed {
    val layerManager = checkNotNull(LocalLayerManager.current) {
        "CompositionLocal LocalLayerManager not present"
    }

    DisposableEffect(layerManager, tag) {
        onDispose {
            layerManager.removeTarget(tag)
        }
    }

    this.onGloballyPositioned {
        layerManager.addTarget(tag, it)
    }
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
        val backgroundColor: Color = Color.Black.copy(alpha = 0.25f)
    )

    interface ContentScope {
        /**
         * 内容是否可见
         */
        val isVisible: Boolean
    }
}

interface TargetLayer : Layer {
    /**
     * 设置目标
     */
    fun setTarget(target: String)

    /**
     * 设置坐标转换
     */
    fun setOffsetTransform(transform: (OffsetTransformScope.(IntOffset) -> IntOffset)?)

    /**
     * 设置修复溢出的方向[OverflowDirection]
     */
    fun setFixOverflowDirection(direction: OverflowDirection?)

    interface OffsetTransformScope {
        /** 内容大小 */
        val contentSize: IntSize

        /** 目标大小 */
        val targetSize: IntSize
    }
}

sealed class OverflowDirection(direction: Int) {
    private val _direction = direction

    fun hasTop() = FlagTop and _direction != 0
    fun hasBottom() = FlagBottom and _direction != 0
    fun hasStart() = FlagStart and _direction != 0
    fun hasEnd() = FlagEnd and _direction != 0

    operator fun plus(direction: OverflowDirection): OverflowDirection {
        val plusDirection = this._direction or direction._direction
        return Plus(plusDirection)
    }

    object Top : OverflowDirection(FlagTop)
    object Bottom : OverflowDirection(FlagBottom)
    object Start : OverflowDirection(FlagStart)
    object End : OverflowDirection(FlagEnd)

    private class Plus(direction: Int) : OverflowDirection(direction)

    companion object {
        private const val FlagTop = 1
        private const val FlagBottom = FlagTop shl 1
        private const val FlagStart = FlagTop shl 2
        private const val FlagEnd = FlagTop shl 3
    }
}

internal inline fun logMsg(block: () -> String) {
    Log.i("FLayer", block())
}