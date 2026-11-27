package com.sd.lib.compose.layer

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.LayoutDirection

/** 方向定义，可通过[plus]组合多个方向 */
@Immutable
sealed class Directions(
  internal val flag: Int,
) {
  /** 顶部方向 */
  data object Top : Directions(TOP)

  /** 底部方向 */
  data object Bottom : Directions(BOTTOM)

  /** 开始方向 */
  data object Start : Directions(START)

  /** 结束方向 */
  data object End : Directions(END)

  /** 组合多个方向 */
  operator fun plus(directions: Directions): Directions {
    val newFlag = flag or directions.flag
    if (newFlag == flag) return this
    if (newFlag == directions.flag) return directions
    return Combine(newFlag)
  }

  internal fun hasTop(): Boolean {
    return TOP and flag != 0
  }

  internal fun hasBottom(): Boolean {
    return BOTTOM and flag != 0
  }

  internal fun hasStart(): Boolean {
    return START and flag != 0
  }

  internal fun hasEnd(): Boolean {
    return END and flag != 0
  }

  internal fun hasLeft(layoutDirection: LayoutDirection): Boolean {
    return if (layoutDirection == LayoutDirection.Ltr) hasStart() else hasEnd()
  }

  internal fun hasRight(layoutDirection: LayoutDirection): Boolean {
    return if (layoutDirection == LayoutDirection.Ltr) hasEnd() else hasStart()
  }

  private data class Combine(val direction: Int) : Directions(direction)

  companion object {
    private const val TOP = 1
    private const val BOTTOM = TOP shl 1
    private const val START = TOP shl 2
    private const val END = TOP shl 3
  }
}