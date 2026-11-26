package com.sd.lib.compose.layer

import androidx.compose.runtime.Immutable

/** 智能对齐候选位置列表 */
@Immutable
data class SmartAliments(
  val aliments: List<SmartAliment>,
) {
  constructor(vararg array: SmartAliment) : this(array.toList())

  companion object {
    /** 默认的智能对齐位置 */
    val Default = SmartAliments(
      SmartAliment(TargetAlignment.BottomEnd),
      SmartAliment(TargetAlignment.BottomStart),
      SmartAliment(TargetAlignment.TopEnd),
      SmartAliment(TargetAlignment.TopStart),
    )
  }
}

/** 智能对齐单项配置 */
@Immutable
data class SmartAliment(
  /** 要对齐的位置 */
  val alignment: TargetAlignment,
  /** 动画 */
  val transition: LayerTransition? = null,
)