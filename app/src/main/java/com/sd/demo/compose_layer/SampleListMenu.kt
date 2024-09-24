package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.LayerTarget
import com.sd.lib.compose.layer.SmartAliments
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
   // 是否添加Layer
   var attach by remember { mutableStateOf(false) }
   // 目标坐标点
   var offset: IntOffset? by remember { mutableStateOf(null) }

   LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(100) { index ->
         ListItem(
            text = index.toString(),
            onOffset = {
               // 保存目标坐标点，并添加Layer
               offset = it
               attach = true
            },
         )
      }
   }

   // Layer
   TargetLayer(
      // 目标坐标点
      target = LayerTarget.Offset(offset),
      // 是否添加Layer
      attach = attach,
      // Layer请求移除回调
      onDetachRequest = { attach = false },
      // 背景颜色透明
      backgroundColor = Color.Transparent,
      // 触摸非内容区域请求移除回调
      detachOnTouchOutside = true,
      // 设置居中对齐
      alignment = TargetAlignment.Center,
      // 如果默认的对齐方式溢出，会使用[smartAlignments]提供的位置按顺序查找溢出最小的位置
      smartAlignments = SmartAliments.Default,
      // 调试模式
      debug = true,
   ) {
      // Layer内容
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
   /** 坐标点回调 */
   onOffset: (IntOffset?) -> Unit,
) {
   val onOffsetUpdated by rememberUpdatedState(onOffset)
   var coordinates: LayoutCoordinates? by remember { mutableStateOf(null) }

   Box(
      modifier = modifier
         .fillMaxSize()
         .height(50.dp)
         .onGloballyPositioned {
            coordinates = it
         }
         .pointerInput(Unit) {
            detectTapGestures {
               val offset = coordinates
                  ?.localToWindow(it)
                  ?.round()
               // 通知坐标点
               onOffsetUpdated(offset)
            }
         }
   ) {
      Text(text = text, modifier = Modifier.align(Alignment.Center))
      HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
   }
}