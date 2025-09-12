package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.Directions
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.LayerTarget
import com.sd.lib.compose.layer.TargetAlignment
import com.sd.lib.compose.layer.TargetAlignmentOffset
import com.sd.lib.compose.layer.TargetLayer
import com.sd.lib.compose.layer.layerTag

class SampleTargetOffset : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AppTheme {
        var isRtl by remember { mutableStateOf(false) }
        val direction = LayoutDirection.Rtl.takeIf { isRtl } ?: LayoutDirection.Ltr
        CompositionLocalProvider(
          LocalLayoutDirection provides direction
        ) {
          LayerContainer {
            Content(
              onClickChangeLayoutDirection = {
                isRtl = !isRtl
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun Content(
  onClickChangeLayoutDirection: () -> Unit,
) {
  val tag = "hello"
  var attach by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(modifier = Modifier.height(500.dp))
    Button(
      onClick = { attach = true },
      modifier = Modifier.layerTag(tag)
    ) {
      Text("Click")
    }

    Button(
      onClick = onClickChangeLayoutDirection,
    ) {
      Text("Change layout direction")
    }
  }

  TargetLayer(
    target = LayerTarget.Tag(tag),
    attach = attach,
    onDetachRequest = { attach = false },
    alignment = TargetAlignment.TopCenter,
    alignmentOffsetX = TargetAlignmentOffset.PX(100),
    alignmentOffsetY = TargetAlignmentOffset.Target(0.5f),
    clipBackgroundDirection = Directions.Bottom + Directions.Start,
    debug = true,
  ) {
    VerticalList(
      count = 5,
      modifier = Modifier.width(200.dp),
    )
  }
}

@Preview
@Composable
private fun PreviewContent() {
  Content(
    onClickChangeLayoutDirection = {},
  )
}