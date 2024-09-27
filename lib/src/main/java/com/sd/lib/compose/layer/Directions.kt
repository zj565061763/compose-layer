package com.sd.lib.compose.layer

import androidx.compose.runtime.Immutable

@Immutable
sealed class Directions(
   internal val flag: Int,
) {
   data object Top : Directions(TOP)
   data object Bottom : Directions(BOTTOM)
   data object Start : Directions(START)
   data object End : Directions(END)

   operator fun plus(directions: Directions): Directions {
      return Combine(flag or directions.flag)
   }

   internal fun hasTop() = TOP and flag != 0
   internal fun hasBottom() = BOTTOM and flag != 0
   internal open fun hasStart() = START and flag != 0
   internal open fun hasEnd() = END and flag != 0

   private class Combine(direction: Int) : Directions(direction)

   companion object {
      private const val TOP = 1
      private const val BOTTOM = TOP shl 1
      private const val START = TOP shl 2
      private const val END = TOP shl 3
   }
}

internal fun Directions.rtl(): Directions {
   return if (this is RTLDirections) this
   else RTLDirections(flag)
}

private class RTLDirections(flag: Int) : Directions(flag) {
   override fun hasStart(): Boolean {
      return super.hasEnd()
   }

   override fun hasEnd(): Boolean {
      return super.hasStart()
   }
}