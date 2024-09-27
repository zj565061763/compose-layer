package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.LayerTarget
import com.sd.lib.compose.layer.LayerTransition
import com.sd.lib.compose.layer.TargetAlignment
import com.sd.lib.compose.layer.TargetLayer
import com.sd.lib.compose.layer.layerTag

class SampleFixOverflow : ComponentActivity() {
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
   var tag by remember { mutableStateOf("") }

   Box(modifier = Modifier.fillMaxSize()) {
      Row(
         modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .offset(y = (-300).dp),
         verticalAlignment = Alignment.CenterVertically,
         horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
         Button(
            onClick = {
               tag = "button1"
               attach = true
            },
            modifier = Modifier.layerTag("button1")
         ) {
            Text("button1")
         }

         Button(
            onClick = {
               tag = "button2"
               attach = true
            },
            modifier = Modifier.layerTag("button2")
         ) {
            Text("button2")
         }
      }
   }

   TargetLayer(
      target = LayerTarget.Tag(tag),
      attach = attach,
      onDetachRequest = { attach = false },
      alignment = TargetAlignment.BottomCenter,
      detachOnTouchBackground = true,
      transition = LayerTransition(
         enter = scaleIn(transformOrigin = TransformOrigin(0.5f, 0f)),
         exit = scaleOut(transformOrigin = TransformOrigin(0.5f, 0f)),
      ),
      debug = true,
   ) {
      VerticalList(
         count = 50,
         modifier = Modifier.navigationBarsPadding(),
      )
   }
}