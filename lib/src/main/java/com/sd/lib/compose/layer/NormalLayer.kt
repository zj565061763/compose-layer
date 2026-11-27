package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection

internal class NormalLayerImpl : LayerImpl() {
  private var _alignment by mutableStateOf(Alignment.Center)

  fun setAlignment(alignment: Alignment) {
    _alignment = alignment
  }

  override fun onAttach(container: ContainerForLayer) {
    setContentVisible(true)
  }

  @Composable
  override fun getLayerTransition(transition: LayerTransition?): LayerTransition {
    if (transition != null) return transition
    return when (_alignment) {
      Alignment.TopCenter -> LayerTransition.slideTopToBottom()
      Alignment.BottomCenter -> LayerTransition.slideBottomToTop()
      Alignment.CenterStart -> LayerTransition.slideStartToEnd(LocalLayoutDirection.current)
      Alignment.CenterEnd -> LayerTransition.slideEndToStart(LocalLayoutDirection.current)
      else -> LayerTransition.Fade
    }
  }

  @Composable
  override fun BoxScope.LayerContent() {
    BackgroundBox()
    ContentBox(Modifier.align(_alignment))
  }
}
