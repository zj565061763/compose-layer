package com.sd.lib.compose.layer

import androidx.compose.runtime.Immutable

@Immutable
data class SmartAliments(
   val aliments: List<SmartAliment>,
) {
   constructor(vararg array: SmartAliment) : this(array.toList())

   companion object {
      val Default = SmartAliments(
         SmartAliment(
            alignment = TargetAlignment.BottomEnd,
            transition = LayerTransition.SlideTopToBottom,
         ),
         SmartAliment(
            alignment = TargetAlignment.BottomStart,
            transition = LayerTransition.SlideTopToBottom,
         ),
         SmartAliment(
            alignment = TargetAlignment.TopEnd,
            transition = LayerTransition.SlideBottomToTop,
         ),
         SmartAliment(
            alignment = TargetAlignment.TopStart,
            transition = LayerTransition.SlideBottomToTop,
         ),
      )
   }
}

@Immutable
data class SmartAliment(
   val alignment: TargetAlignment,
   val transition: LayerTransition? = null,
)