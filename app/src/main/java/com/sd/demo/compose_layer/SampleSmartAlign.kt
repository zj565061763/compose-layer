package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.LayerTarget
import com.sd.lib.compose.layer.LayerTransition
import com.sd.lib.compose.layer.SmartAliment
import com.sd.lib.compose.layer.SmartAliments
import com.sd.lib.compose.layer.TargetAlignment
import com.sd.lib.compose.layer.TargetAlignmentOffset
import com.sd.lib.compose.layer.TargetLayer
import com.sd.lib.compose.layer.layerTag
import com.sd.lib.compose.layer.relativeAlignment

private const val TAG_SMART_TARGET = "tag_smart_target"

private enum class TargetPreset(
  val label: String,
  val alignment: Alignment,
) {
  BottomEdge("底部边缘", BiasAlignment(0f, 0.92f)),
  TopEdge("顶部边缘", BiasAlignment(0f, -0.92f)),
  TopStart("左上角", BiasAlignment(-0.92f, -0.92f)),
  TopEnd("右上角", BiasAlignment(0.92f, -0.92f)),
}

class SampleSmartAlign : ComponentActivity() {
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
  defaultPreset: TargetPreset = TargetPreset.BottomEdge,
  defaultAttach: Boolean = false,
) {
  var preset by remember { mutableStateOf(defaultPreset) }
  var dragOffset by remember { mutableStateOf(IntOffset.Zero) }
  var attach by remember { mutableStateOf(defaultAttach) }

  val smartAlignments = remember {
    SmartAliments(
      SmartAliment(TargetAlignment.TopCenter, LayerTransition.slideBottomToTop()),
      SmartAliment(TargetAlignment.BottomCenter, LayerTransition.slideTopToBottom()),
      SmartAliment(TargetAlignment.EndCenter, LayerTransition.slideLeftToRight()),
      SmartAliment(TargetAlignment.StartCenter, LayerTransition.slideRightToLeft()),
      SmartAliment(TargetAlignment.BottomStart, LayerTransition.scaleTopLeft()),
      SmartAliment(TargetAlignment.BottomEnd, LayerTransition.scaleTopRight()),
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .safeDrawingPadding()
      .padding(12.dp),
  ) {
    Column(
      modifier = Modifier
        .align(Alignment.Center)
        .background(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          shape = RoundedCornerShape(16.dp),
        )
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = "自定义 SmartAliments（默认 TopCenter，越界自动翻转方向并匹配动画）",
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(8.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TargetPreset.entries.forEach { item ->
          FilterChip(
            selected = (preset == item) && (dragOffset == IntOffset.Zero),
            onClick = {
              preset = item
              dragOffset = IntOffset.Zero
            },
            label = { Text(text = item.label) },
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Button(
        onClick = { attach = !attach },
      ) {
        Text(text = if (attach) "关闭气泡" else "显示气泡")
      }
    }

    Box(
      modifier = Modifier
        .align(preset.alignment)
        .offset { dragOffset }
        .size(88.dp)
        .background(
          color = MaterialTheme.colorScheme.primary,
          shape = RoundedCornerShape(14.dp),
        )
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            dragOffset += dragAmount.round()
          }
        }
        .layerTag(TAG_SMART_TARGET),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "拖动我\nTarget",
        color = MaterialTheme.colorScheme.onPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
    }
  }

  TargetLayer(
    target = LayerTarget.Tag(TAG_SMART_TARGET),
    attach = attach,
    onDetachRequest = { attach = false },
    backgroundColor = Color.Transparent,
    detachOnTouchOutside = null,
    alignment = TargetAlignment.TopCenter,
    alignmentOffsetX = TargetAlignmentOffset.DP(8).relativeAlignment(),
    alignmentOffsetY = TargetAlignmentOffset.DP(8).relativeAlignment(),
    smartAlignments = smartAlignments,
    debug = true,
  ) {
    SmartTooltipCard()
  }
}

@Composable
private fun SmartTooltipCard(
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .width(210.dp)
      .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
      .background(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(12.dp),
      )
      .padding(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = "Smart Popover",
      fontWeight = FontWeight.Bold,
      fontSize = 15.sp,
      color = MaterialTheme.colorScheme.onTertiaryContainer,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "首选 TopCenter，靠近顶边或侧边时自动切换候选方位与进出场动画",
      fontSize = 12.sp,
      lineHeight = 16.sp,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onTertiaryContainer,
    )
  }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun PreviewNearTopEdge() {
  AppTheme {
    LayerContainer {
      Content(
        defaultPreset = TargetPreset.TopEdge,
        defaultAttach = true,
      )
    }
  }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun PreviewNearTopStart() {
  AppTheme {
    LayerContainer {
      Content(
        defaultPreset = TargetPreset.TopStart,
        defaultAttach = true,
      )
    }
  }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun PreviewNearBottomEdge() {
  AppTheme {
    LayerContainer {
      Content(
        defaultPreset = TargetPreset.BottomEdge,
        defaultAttach = true,
      )
    }
  }
}
