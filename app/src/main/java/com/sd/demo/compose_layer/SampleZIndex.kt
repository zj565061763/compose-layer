package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.Layer
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.LayerDetach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SampleZIndex : ComponentActivity() {
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
private fun Content(
   modifier: Modifier = Modifier,
) {
   var layer1 by remember { mutableStateOf(false) }
   var layer2 by remember { mutableStateOf(false) }

   Layer1(layer1) { layer1 = false }
   Layer2(layer2) { layer2 = false }

   val coroutineScope = rememberCoroutineScope()

   Column(
      modifier = modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
   ) {
      Button(onClick = {
         coroutineScope.launch {
            layer1 = true
            delay(1_000)
            layer2 = true
         }
      }) {
         Text("Show")
      }
   }
}

@Composable
private fun Layer1(
   attach: Boolean,
   onDetachRequest: (LayerDetach) -> Unit,
) {
   Layer(
      attach = attach,
      onDetachRequest = onDetachRequest,
      detachOnTouchBackground = true,
      zIndex = 99f,
   ) {
      ColorBox(text = "Layer1", color = Color.Red)
   }
}

@Composable
private fun Layer2(
   attach: Boolean,
   onDetachRequest: (LayerDetach) -> Unit,
) {
   Layer(
      attach = attach,
      onDetachRequest = onDetachRequest,
      detachOnTouchBackground = true,
   ) {
      ColorBox(text = "I am Layer2", color = Color.Blue)
   }
}
