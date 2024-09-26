package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.LayerState
import com.sd.lib.compose.layer.layer

class SampleAlignContainer : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         AppTheme {
            LayerContainer {
               Content()
            }
         }
      }
   }
}

@Composable
private fun Content() {
   var attach by remember { mutableStateOf(false) }
   var alignment by remember { mutableStateOf(Alignment.Center) }

   /** 创建Layer，并返回[LayerState] */
   val layerState = layer(
      attach = attach,
      onDetachRequest = { attach = false },
      alignment = alignment,
      detachOnTouchOutside = true,
      debug = true,
   ) {
      ColorBox(
         color = Color.Red,
         text = "Box",
      )
   }

   LaunchedEffect(layerState) {
      // 监听Layer生命周期状态
      snapshotFlow { layerState.lifecycleState }
         .collect {
            logMsg { "lifecycleState:${it}" }
         }
   }

   LaunchedEffect(layerState) {
      // 监听Layer可见状态
      snapshotFlow { layerState.isVisibleState }
         .collect {
            logMsg { "isVisibleState:${it}" }
         }
   }

   Box(modifier = Modifier.fillMaxSize()) {
      ButtonsBox(
         modifier = Modifier.fillMaxSize(),
         onClick = {
            alignment = it
            attach = true
         }
      )

      // 演示跟踪Layer可见状态动画
      AnimatedVisibility(
         modifier = Modifier.align(Alignment.Center),
         visible = layerState.isVisibleState,
         enter = scaleIn(),
         exit = scaleOut(),
      ) {
         Text("Sync visible state")
      }
   }
}

@Composable
private fun ButtonsBox(
   modifier: Modifier = Modifier,
   onClick: (Alignment) -> Unit,
) {
   Box(modifier = modifier) {
      Button(
         modifier = Modifier.align(Alignment.TopCenter),
         onClick = { onClick(Alignment.TopCenter) },
      ) {
         Text(text = "TopCenter")
      }

      Button(
         modifier = Modifier.align(Alignment.BottomCenter),
         onClick = { onClick(Alignment.BottomCenter) },
      ) {
         Text(text = "BottomCenter")
      }

      Button(
         modifier = Modifier.align(Alignment.CenterStart),
         onClick = { onClick(Alignment.CenterStart) },
      ) {
         Text(text = "CenterStart")
      }

      Button(
         modifier = Modifier.align(Alignment.CenterEnd),
         onClick = { onClick(Alignment.CenterStart) },
      ) {
         Text(text = "CenterEnd")
      }
   }
}