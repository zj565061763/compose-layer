package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.DisplayDefault
import com.sd.lib.compose.layer.Layer
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.TargetLayer
import com.sd.lib.compose.layer.layerTarget
import com.sd.lib.compose.layer.rememberTargetLayer

class SampleOverflow : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      WindowCompat.setDecorFitsSystemWindows(window, false)
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
   val layer = layer()

   Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
   ) {
      Spacer(modifier = Modifier.height(300.dp))

      Row(
         modifier = Modifier.fillMaxWidth(),
         verticalAlignment = Alignment.CenterVertically,
         horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
         Button(
            onClick = {
               layer.setTarget("button1")
               layer.setFixOverflow(false)
               layer.attach()
            },
            modifier = Modifier.layerTarget("button1")
         ) {
            Text("Overflow")
         }

         Button(
            onClick = {
               layer.setTarget("button2")
               layer.setFixOverflow(true)
               layer.attach()
            },
            modifier = Modifier.layerTarget("button2")
         ) {
            Text("Fix overflow")
         }
      }
   }
}

@Composable
private fun layer(): TargetLayer {
   return rememberTargetLayer(
      onCreate = {
         it.debug = true
         it.setPosition(Layer.Position.BottomCenter)
         it.setDismissOnTouchOutside(true)
      },
      display = {
         DisplayDefault(
            enter = scaleIn(transformOrigin = TransformOrigin(0.5f, 0f)),
            exit = scaleOut(transformOrigin = TransformOrigin(0.5f, 0f)),
         )
      }
   ) {
      VerticalList(
         count = 50,
         modifier = Modifier.navigationBarsPadding(),
      )
   }
}