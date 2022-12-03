package com.sd.lib.compose.layer

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

interface TargetLayer : Layer {
    /**
     * 设置目标
     */
    fun setTarget(target: String)

    /**
     * 设置一个目标坐标，如果不为null，则会显示在该坐标附近，此时[setTarget]设置的目标无效。
     */
    fun setTargetOffset(offset: IntOffset?)

    /**
     * 设置坐标转换
     */
    fun setOffsetTransform(transform: OffsetTransform?)

    /**
     * 设置修复溢出的方向[PlusDirection]
     */
    fun setFixOverflowDirection(direction: PlusDirection?)

    /**
     * 设置要裁切背景的方向[PlusDirection]
     */
    fun setClipBackgroundDirection(direction: PlusDirection?)
}

fun interface OffsetTransform {

    fun transform(params: Params): IntOffset

    interface Params {
        /** 坐标 */
        val offset: IntOffset

        /** 内容大小 */
        val contentSize: IntSize

        /** 目标大小 */
        val targetSize: IntSize
    }
}

sealed class PlusDirection(direction: Int) {
    private val _direction = direction

    fun hasTop() = FlagTop and _direction != 0
    fun hasBottom() = FlagBottom and _direction != 0
    fun hasStart() = FlagStart and _direction != 0
    fun hasEnd() = FlagEnd and _direction != 0

    operator fun plus(direction: PlusDirection): PlusDirection {
        val plusDirection = this._direction or direction._direction
        return Plus(plusDirection)
    }

    object Top : PlusDirection(FlagTop)
    object Bottom : PlusDirection(FlagBottom)
    object Start : PlusDirection(FlagStart)
    object End : PlusDirection(FlagEnd)

    private class Plus(direction: Int) : PlusDirection(direction)

    companion object {
        private const val FlagTop = 1
        private const val FlagBottom = FlagTop shl 1
        private const val FlagStart = FlagTop shl 2
        private const val FlagEnd = FlagTop shl 3
    }
}