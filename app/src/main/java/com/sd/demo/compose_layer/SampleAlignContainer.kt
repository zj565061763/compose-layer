package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.Layer
import com.sd.lib.compose.layer.LayerContainer

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

   Box(modifier = Modifier.fillMaxSize()) {
      Button(
         modifier = Modifier.align(Alignment.TopCenter),
         onClick = {
            alignment = Alignment.TopCenter
            attach = true
         }
      ) {
         Text(text = "TopCenter")
      }

      Button(
         modifier = Modifier.align(Alignment.BottomCenter),
         onClick = {
            alignment = Alignment.BottomCenter
            attach = true
         }
      ) {
         Text(text = "BottomCenter")
      }

      Button(
         modifier = Modifier.align(Alignment.CenterStart),
         onClick = {
            alignment = Alignment.CenterStart
            attach = true
         }
      ) {
         Text(text = "CenterStart")
      }

      Button(
         modifier = Modifier.align(Alignment.CenterEnd),
         onClick = {
            alignment = Alignment.CenterEnd
            attach = true
         }
      ) {
         Text(text = "CenterEnd")
      }
   }

   Layer(
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
}