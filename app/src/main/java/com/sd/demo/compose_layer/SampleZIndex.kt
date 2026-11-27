package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.Layer
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.LayerDetach
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SampleZIndex : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AppTheme {
        SampleZIndexScreen()
      }
    }
  }
}

@Composable
private fun SampleZIndexScreen(
  modifier: Modifier = Modifier,
  defaultLayer1: Boolean = false,
  defaultLayer2: Boolean = false,
  defaultLayersZIndex: Float = 0f,
) {
  var layersZIndex by remember { mutableFloatStateOf(defaultLayersZIndex) }

  LayerContainer(
    modifier = modifier,
    layersZIndex = layersZIndex,
  ) {
    Content(
      defaultLayer1 = defaultLayer1,
      defaultLayer2 = defaultLayer2,
      layersZIndex = layersZIndex,
      onSelectLayersZIndex = { layersZIndex = it },
    )

    Box(
      modifier = Modifier
        .align(Alignment.Center)
        .padding(top = 150.dp)
        .zIndex(1f)
        .background(
          color = Color(0xFF2E7D32),
          shape = RoundedCornerShape(8.dp),
        )
        .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
      Text(
        text = "容器内子元素 (zIndex = 1f)",
        color = Color.White,
        fontSize = 13.sp,
      )
    }
  }
}

@Composable
private fun Content(
  modifier: Modifier = Modifier,
  defaultLayer1: Boolean = false,
  defaultLayer2: Boolean = false,
  layersZIndex: Float = 0f,
  onSelectLayersZIndex: (Float) -> Unit = {},
) {
  var layer1 by remember { mutableStateOf(defaultLayer1) }
  var layer2 by remember { mutableStateOf(defaultLayer2) }
  val coroutineScope = rememberCoroutineScope()

  Layer1(
    attach = layer1,
    onDetachRequest = { layer1 = false },
  )
  Layer2(
    attach = layer2,
    onDetachRequest = { layer2 = false },
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .safeDrawingPadding()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Button(
      onClick = {
        coroutineScope.launch {
          layer1 = true
          delay(1.seconds)
          layer2 = true
        }
      },
    ) {
      Text(text = "先开 Layer1(99f)，1s 后开 Layer2(0f)")
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedButton(onClick = { layer1 = !layer1 }) {
        Text(text = if (layer1) "关闭 Layer1" else "打开 Layer1(99f)")
      }
      OutlinedButton(onClick = { layer2 = !layer2 }) {
        Text(text = if (layer2) "关闭 Layer2" else "打开 Layer2(0f)")
      }
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "layersZIndex:",
        style = MaterialTheme.typography.bodySmall,
      )
      listOf(0f, 2f).forEach { value ->
        FilterChip(
          selected = layersZIndex == value,
          onClick = { onSelectLayersZIndex(value) },
          label = { Text(text = value.toString()) },
        )
      }
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
    zIndex = 99f,
    debug = true,
  ) {
    ColorBox(
      modifier = Modifier.size(140.dp),
      text = "Layer1\n(zIndex = 99f)",
      color = Color.Red,
    )
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
    zIndex = 0f,
    debug = true,
  ) {
    ColorBox(
      modifier = Modifier.size(230.dp),
      text = "Layer2 (zIndex = 0f)",
      color = Color.Blue,
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLayersZIndex0() {
  AppTheme {
    SampleZIndexScreen(
      defaultLayer1 = true,
      defaultLayer2 = true,
      defaultLayersZIndex = 0f,
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLayersZIndex2() {
  AppTheme {
    SampleZIndexScreen(
      defaultLayer1 = true,
      defaultLayer2 = true,
      defaultLayersZIndex = 2f,
    )
  }
}
