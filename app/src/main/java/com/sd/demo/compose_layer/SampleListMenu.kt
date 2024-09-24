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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.round
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.LayerTarget
import com.sd.lib.compose.layer.TargetAlignment
import com.sd.lib.compose.layer.TargetLayer

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
   var attach by remember { mutableStateOf(false) }
   var offset: IntOffset? by remember { mutableStateOf(null) }

   LazyColumn(
      modifier = Modifier
         .fillMaxSize()
         .navigationBarsPadding(),
   ) {
      items(100) { index ->
         ListItem(
            text = index.toString(),
            onPress = {
               attach = false
               offset = null
            },
            onLongPress = {
               offset = it
               attach = true
            },
         )
      }
   }

   TargetLayer(
      target = LayerTarget.Offset(offset),
      attach = attach,
      onDetachRequest = { attach = false },
      backgroundColor = Color.Transparent,
      detachOnTouchOutside = null,
      alignment = TargetAlignment.Center,
      smartAlignments = emptyList(),
      debug = true,
   ) {
      VerticalList(
         count = 5,
         modifier = Modifier.width(200.dp),
         onClick = { attach = false }
      )
   }
}

@Composable
private fun ListItem(
   modifier: Modifier = Modifier,
   text: String,
   onPress: () -> Unit,
   onLongPress: (IntOffset) -> Unit,
) {
   val onPressUpdated by rememberUpdatedState(onPress)
   val onLongPressUpdated by rememberUpdatedState(onLongPress)

   val context = LocalContext.current
   var layoutCoordinates: LayoutCoordinates? by remember { mutableStateOf(null) }

   Box(
      modifier = modifier
         .fillMaxSize()
         .height(50.dp)
         .onGloballyPositioned {
            layoutCoordinates = it
         }
         .pointerInput(context) {
            detectTapGestures(
               onPress = {
                  onPressUpdated()
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
                     onLongPressUpdated(offset.round())
                  }
               }
            )
         }
   ) {
      Text(
         text = text,
         modifier = Modifier.align(Alignment.Center),
      )
      HorizontalDivider(
         modifier = Modifier.align(Alignment.BottomCenter)
      )
   }
}