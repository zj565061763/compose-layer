package com.sd.lib.compose.layer

import androidx.compose.runtime.Immutable

/** 智能对齐候选位置列表 */
@Immutable
data class SmartAliments(
  val aliments: List<SmartAliment>,
) {
  constructor(vararg array: SmartAliment) : this(array.toList())

  companion object {
    /** 默认的智能对齐位置，各项[SmartAliment.transition]均为null，命中时优先使用[TargetLayer]的transition，未指定时使用命中位置的默认动画 */
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
  /** 动画，非null时优先级高于[TargetLayer]的transition；为null时依次回退到[TargetLayer]的transition与当前[alignment]的默认动画 */
  val transition: LayerTransition? = null,
)