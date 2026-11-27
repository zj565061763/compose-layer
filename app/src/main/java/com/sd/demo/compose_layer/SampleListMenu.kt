package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.LayerTarget
import com.sd.lib.compose.layer.SmartAliments
import com.sd.lib.compose.layer.TargetAlignment
import com.sd.lib.compose.layer.TargetAlignmentOffset
import com.sd.lib.compose.layer.TargetLayer
import com.sd.lib.compose.layer.relativeAlignment

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
private fun Content(
  modifier: Modifier = Modifier,
  defaultOffset: IntOffset? = null,
) {
  var attach by remember { mutableStateOf(defaultOffset != null) }
  var offset: IntOffset? by remember { mutableStateOf(defaultOffset) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .safeDrawingPadding(),
  ) {
    items(100) { index ->
      ListItem(
        text = "Item $index（点击任意位置弹出菜单）",
        onClick = { clickedOffset ->
          offset = clickedOffset
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
    alignment = TargetAlignment.BottomCenter,
    // 默认位置溢出时，按 SmartAliments 候选顺序查找溢出最小的位置
    smartAlignments = SmartAliments.Default,
    /** X轴相对[TargetAlignment]偏移22dp */
    alignmentOffsetX = TargetAlignmentOffset.DP(22).relativeAlignment(),
    /** Y轴相对[TargetAlignment]偏移22dp */
    alignmentOffsetY = TargetAlignmentOffset.DP(22).relativeAlignment(),
    debug = true,
  ) {
    Menus(
      onClickItem = { attach = false },
    )
  }
}

@Composable
private fun ListItem(
  modifier: Modifier = Modifier,
  text: String,
  onClick: (IntOffset) -> Unit,
) {
  val onClickUpdated by rememberUpdatedState(onClick)
  var coordinates: LayoutCoordinates? by remember { mutableStateOf(null) }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(50.dp)
      .onGloballyPositioned {
        coordinates = it
      }
      .pointerInput(Unit) {
        detectTapGestures { localOffset ->
          coordinates
            ?.localToWindow(localOffset)
            ?.round()
            ?.also { windowOffset -> onClickUpdated(windowOffset) }
        }
      },
  ) {
    Text(
      text = text,
      modifier = Modifier.align(Alignment.Center),
    )
    HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
  }
}

@Composable
private fun Menus(
  modifier: Modifier = Modifier,
  onClickItem: (String) -> Unit,
) {
  Column(
    modifier = modifier
      .width(200.dp)
      .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
      .background(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
      ),
  ) {
    remember { listOf("Menu1", "Menu2", "Menu3") }
      .forEach { item ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(56.dp)
            .clickable { onClickItem(item) }
            .padding(start = 20.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          Text(text = item, fontSize = 16.sp)
        }
      }
  }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun PreviewTopRightClick() {
  val density = LocalDensity.current
  val clickOffset = with(density) { IntOffset(320.dp.roundToPx(), 100.dp.roundToPx()) }
  AppTheme {
    LayerContainer {
      Content(defaultOffset = clickOffset)
    }
  }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun PreviewBottomRightClick() {
  val density = LocalDensity.current
  val clickOffset = with(density) { IntOffset(320.dp.roundToPx(), 580.dp.roundToPx()) }
  AppTheme {
    LayerContainer {
      Content(defaultOffset = clickOffset)
    }
  }
}
