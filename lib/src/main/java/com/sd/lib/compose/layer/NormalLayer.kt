package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection

internal interface NormalLayer : Layer {
  /**
   * 对齐容器位置，默认[Alignment.Center]
   */
  fun setAlignment(alignment: Alignment)
}

internal class NormalLayerImpl : LayerImpl(), NormalLayer {
  private var _alignment by mutableStateOf(Alignment.Center)

  override fun setAlignment(alignment: Alignment) {
    _alignment = alignment
  }

  @Composable
  override fun BoxScope.LayerContent() {
    if (LocalInspectionMode.current) {
      setContentVisible(true)
    } else {
      SideEffect {
        setContentVisible(true)
      }
    }

    BackgroundBox()
    ContentBox(Modifier.align(_alignment))
  }

  @Composable
  override fun getLayerTransition(transition: LayerTransition?): LayerTransition {
    if (transition != null) return transition
    return when (_alignment) {
      Alignment.TopCenter -> LayerTransition.slideTopToBottom()
      Alignment.BottomCenter -> LayerTransition.slideBottomToTop()
      Alignment.CenterStart -> LayerTransition.slideStartToEnd(LocalLayoutDirection.current)
      Alignment.CenterEnd -> LayerTransition.slideEndToStart(LocalLayoutDirection.current)
      else -> LayerTransition.Default
    }
  }
}