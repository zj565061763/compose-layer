package com.sd.demo.compose_layer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.Layer
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.TargetLayer
import com.sd.lib.compose.layer.rememberTargetLayer

class SampleListMenu : ComponentActivity() {
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
   val layer = layer()

   LazyColumn(
      modifier = Modifier
         .fillMaxSize()
         .navigationBarsPadding(),
   ) {
      items(100) { index ->
         ListItem(
            text = index.toString(),
            layer = layer,
         ) { offset ->
            layer.setTarget(offset)
            layer.attach()
         }
      }
   }
}

@Composable
private fun layer(): TargetLayer {
   return rememberTargetLayer(
      onCreate = {
         it.isDebug = true
         it.setBackgroundColor(Color.Transparent)
         it.setDismissOnTouchOutside(null)
         it.setFindBestPosition(true)
      },
   ) {
      VerticalList(
         count = 5,
         modifier = Modifier.width(200.dp),
         onClick = { layer.detach() }
      )
   }
}

@Composable
private fun ListItem(
   modifier: Modifier = Modifier,
   text: String,
   layer: Layer,
   onClick: (IntOffset) -> Unit,
) {
   val onClickUpdated by rememberUpdatedState(newValue = onClick)

   val context = LocalContext.current
   var layoutCoordinates: LayoutCoordinates? by remember { mutableStateOf(null) }
   Box(
      modifier = modifier
         .fillMaxSize()
         .height(50.dp)
         .onGloballyPositioned {
            layoutCoordinates = it
         }
         .pointerInput(layer, context) {
            detectTapGestures(
               onPress = {
                  layer.detach()
               },
               onTap = {
                  Toast
                     .makeText(context, "try long click", Toast.LENGTH_SHORT)
                     .show()
               },
               onLongPress = {
                  val layout = layoutCoordinates
                  if (layout?.isAttached == true) {
                     val offset = layout.localToWindow(it)
                     onClickUpdated(IntOffset(offset.x.toInt(), offset.y.toInt()))
                  }
               }
            )
         }
   ) {
      Text(
         text = text,
         modifier = Modifier.align(Alignment.Center),
      )
      Divider(
         modifier = Modifier.align(Alignment.BottomCenter)
      )
   }
}