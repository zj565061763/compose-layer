package com.sd.lib.compose.layer

import androidx.compose.runtime.Immutable

@Immutable
data class SmartAliments(
   val aliments: List<SmartAliment>,
) {
   constructor(vararg array: SmartAliment) : this(array.toList())

   companion object {
      val Default = SmartAliments(
         SmartAliment(TargetAlignment.BottomEnd),
         SmartAliment(TargetAlignment.BottomStart),
         SmartAliment(TargetAlignment.TopEnd),
         SmartAliment(TargetAlignment.TopStart),
      )
   }
}

@Immutable
data class SmartAliment(
   /** 要对齐的位置 */
   val alignment: TargetAlignment,
   /** 动画 */
   val transition: LayerTransition? = null,
)