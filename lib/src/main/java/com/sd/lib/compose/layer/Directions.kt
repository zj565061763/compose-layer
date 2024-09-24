package com.sd.lib.compose.layer

import androidx.compose.runtime.Immutable

@Immutable
sealed class Directions(
   private val flag: Int,
) {
   data object Top : Directions(TOP)
   data object Bottom : Directions(BOTTOM)
   data object Start : Directions(START)
   data object End : Directions(END)

   fun hasTop() = TOP and flag != 0
   fun hasBottom() = BOTTOM and flag != 0
   fun hasStart() = START and flag != 0
   fun hasEnd() = END and flag != 0

   operator fun plus(directions: Directions): Directions {
      return Combine(flag or directions.flag)
   }

   private class Combine(direction: Int) : Directions(direction)

   companion object {
      private const val TOP = 1
      private const val BOTTOM = TOP shl 1
      private const val START = TOP shl 2
      private const val END = TOP shl 3
   }
}