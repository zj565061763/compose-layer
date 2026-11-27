package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.LayerState
import com.sd.lib.compose.layer.layer

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
private fun Content(
  modifier: Modifier = Modifier,
  defaultAttach: Boolean = false,
  defaultAlignment: Alignment = Alignment.TopCenter,
) {
  var attach by remember { mutableStateOf(defaultAttach) }
  var alignment by remember { mutableStateOf(defaultAlignment) }

  /** 创建Layer，并返回[LayerState] */
  val layerState = layer(
    attach = attach,
    onDetachRequest = { attach = false },
    alignment = alignment,
    debug = true,
  ) {
    Box(modifier = Modifier.safeDrawingPadding()) {
      ColorBox(text = "Layer (${alignment.displayName()})")
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .safeDrawingPadding()
      .padding(12.dp),
  ) {
    ButtonsBox(
      modifier = Modifier.fillMaxSize(),
      onClick = {
        alignment = it
        attach = true
      },
    )

    // 演示跟踪Layer可见状态动画
    AnimatedVisibility(
      modifier = Modifier
        .align(Alignment.Center)
        .padding(top = 120.dp),
      visible = layerState.isVisibleState,
      enter = scaleIn(),
      exit = scaleOut(),
    ) {
      Text(
        text = "Sync visible state (${layerState.lifecycleState})",
        color = Color.Red,
      )
    }
  }

  LaunchedEffect(layerState) {
    snapshotFlow { layerState.lifecycleState }
      .collect {
        logMsg { "lifecycleState:$it" }
      }
  }
}

@Composable
private fun ButtonsBox(
  modifier: Modifier = Modifier,
  onClick: (Alignment) -> Unit,
) {
  Box(modifier = modifier) {
    Button(
      modifier = Modifier.align(Alignment.TopCenter),
      onClick = { onClick(Alignment.TopCenter) },
    ) {
      Text(text = "TopCenter")
    }

    Button(
      modifier = Modifier.align(Alignment.BottomCenter),
      onClick = { onClick(Alignment.BottomCenter) },
    ) {
      Text(text = "BottomCenter")
    }

    Button(
      modifier = Modifier.align(Alignment.CenterStart),
      onClick = { onClick(Alignment.CenterStart) },
    ) {
      Text(text = "CenterStart")
    }

    Button(
      modifier = Modifier.align(Alignment.CenterEnd),
      onClick = { onClick(Alignment.CenterEnd) },
    ) {
      Text(text = "CenterEnd")
    }

    Button(
      modifier = Modifier.align(Alignment.Center),
      onClick = { onClick(Alignment.Center) },
    ) {
      Text(text = "Center")
    }
  }
}

private fun Alignment.displayName(): String {
  return when (this) {
    Alignment.TopCenter -> "TopCenter"
    Alignment.BottomCenter -> "BottomCenter"
    Alignment.CenterStart -> "CenterStart"
    Alignment.CenterEnd -> "CenterEnd"
    Alignment.Center -> "Center"
    else -> toString()
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewAttached() {
  AppTheme {
    LayerContainer {
      Content(
        defaultAttach = true,
        defaultAlignment = Alignment.TopCenter,
      )
    }
  }
}
