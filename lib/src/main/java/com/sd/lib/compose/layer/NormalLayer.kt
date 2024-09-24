package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

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
   override fun LayerContent() {
      SideEffect {
         setContentVisible(true)
      }

      Box(modifier = Modifier.fillMaxSize()) {
         BackgroundBox()
         ContentBox(Modifier.align(_alignment))
      }
   }

   override val defaultDisplay: @Composable (LayerDisplayScope.() -> Unit) = {
      when (_alignment) {
         Alignment.TopCenter -> DisplaySlideTopToBottom()
         Alignment.BottomCenter -> DisplaySlideBottomToTop()
         Alignment.CenterStart -> DisplaySlideStartToEnd()
         Alignment.CenterEnd -> DisplaySlideEndToStart()
         else -> DisplayDefault()
      }
   }
}