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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.DisplayDefault
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.LayerTarget
import com.sd.lib.compose.layer.TargetAlignment
import com.sd.lib.compose.layer.TargetLayer
import com.sd.lib.compose.layer.layerTag

class SampleOverflow : ComponentActivity() {
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
   var fixOverflow by remember { mutableStateOf(false) }

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
               tag = "button1"
               fixOverflow = false
               attach = true
            },
            modifier = Modifier.layerTag("button1")
         ) {
            Text("Overflow")
         }

         Button(
            onClick = {
               tag = "button2"
               fixOverflow = true
               attach = true
            },
            modifier = Modifier.layerTag("button2")
         ) {
            Text("Fix overflow")
         }
      }
   }

   TargetLayer(
      target = LayerTarget.Tag(tag),
      attach = attach,
      onDetachRequest = { attach = false },
      alignment = TargetAlignment.BottomCenter,
      detachOnTouchOutside = true,
      fixOverflow = fixOverflow,
      display = {
         DisplayDefault(
            enter = scaleIn(transformOrigin = TransformOrigin(0.5f, 0f)),
            exit = scaleOut(transformOrigin = TransformOrigin(0.5f, 0f)),
         )
      },
      debug = true,
   ) {
      VerticalList(
         count = 50,
         modifier = Modifier.navigationBarsPadding(),
      )
   }
}