package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.LayerTarget
import com.sd.lib.compose.layer.TargetAlignment
import com.sd.lib.compose.layer.TargetLayer
import com.sd.lib.compose.layer.layerTag

class SampleAlignTarget : ComponentActivity() {
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
   val tag = "hello"
   var attach by remember { mutableStateOf(false) }
   var alignment by remember { mutableStateOf(TargetAlignment.Center) }

   Box(modifier = Modifier.fillMaxSize()) {
      TargetView(tag = tag)
      ButtonsView(
         onClickDetach = { attach = false },
         onClick = { alignment = it },
      )
   }

   TargetLayer(
      target = LayerTarget.Tag(tag),
      attach = attach,
      onDetachRequest = { attach = false },
      alignment = alignment,
      backgroundColor = Color.Transparent,
      detachOnBackPress = null,
      detachOnTouchOutside = null,
      debug = true,
   ) {
      ColorBox(
         color = Color.Red,
         text = "Box",
      )
   }
}

@Composable
private fun TargetView(
   modifier: Modifier = Modifier,
   tag: String,
) {
   var showTarget by remember { mutableStateOf(true) }

   Column(
      modifier = modifier
         .fillMaxSize()
         .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
   ) {
      Box(modifier = Modifier.height(400.dp))

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
         Button(
            onClick = {
               showTarget = !showTarget
            },
         ) {
            Text(if (showTarget) "Hide Target" else "Show Target")
         }

         if (showTarget) {
            Box(
               modifier = Modifier
                  .size(150.dp)
                  .background(Color.LightGray)
                  .layerTag(tag)
            ) {
               Text(text = "Target", modifier = Modifier.align(Alignment.Center))
            }
         }
      }

      Box(modifier = Modifier.height(2000.dp))
   }
}

@Composable
private fun ButtonsView(
   modifier: Modifier = Modifier,
   onClickDetach: () -> Unit,
   onClick: (TargetAlignment) -> Unit,
) {
   Column(
      modifier = modifier
         .fillMaxWidth()
         .systemBarsPadding(),
      horizontalAlignment = Alignment.CenterHorizontally,
   ) {
      // Top
      ButtonRow(
         list = listOf(
            TargetAlignment.TopStart,
            TargetAlignment.TopCenter,
            TargetAlignment.TopEnd,
         ),
         onClick = onClick,
      )

      // Bottom
      ButtonRow(
         list = listOf(
            TargetAlignment.BottomStart,
            TargetAlignment.BottomCenter,
            TargetAlignment.BottomEnd,
         ),
         onClick = onClick,
      )

      // Center
      ButtonRow(
         list = listOf(
            TargetAlignment.Top,
            TargetAlignment.Bottom,
            TargetAlignment.Center,
            TargetAlignment.Start,
            TargetAlignment.End,
         ),
         onClick = onClick,
      )

      // Start
      ButtonRow(
         list = listOf(
            TargetAlignment.StartTop,
            TargetAlignment.StartCenter,
            TargetAlignment.StartBottom,
         ),
         onClick = onClick,
      )

      // End
      ButtonRow(
         list = listOf(
            TargetAlignment.EndTop,
            TargetAlignment.EndCenter,
            TargetAlignment.EndBottom,
         ),
         onClick = onClick,
      )

      Button(
         onClick = { onClickDetach() },
         colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
         )
      ) {
         Text(text = "Detach")
      }
   }
}

@Composable
private fun ButtonRow(
   modifier: Modifier = Modifier,
   list: List<TargetAlignment>,
   onClick: (TargetAlignment) -> Unit,
) {
   Row(
      modifier = modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
   ) {
      list.forEach { item ->
         Button(
            onClick = { onClick(item) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(0.dp)
         ) {
            Text(text = item.name)
         }
      }
   }
}