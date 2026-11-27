package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.LayerTransition
import com.sd.lib.compose.layer.LayerTarget
import com.sd.lib.compose.layer.TargetAlignment
import com.sd.lib.compose.layer.TargetAlignmentOffset
import com.sd.lib.compose.layer.TargetLayer
import com.sd.lib.compose.layer.layerTag
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

private const val TAG_CAPTURE_TRUE = "tag_capture_true"
private const val TAG_CAPTURE_FALSE = "tag_capture_false"
private const val TRANSITION_DURATION_MS = 1500

class SampleCaptureOnDetach : ComponentActivity() {
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
  defaultData: String? = null,
) {
  var index by remember { mutableIntStateOf(1) }
  var data by remember { mutableStateOf(defaultData) }
  var trueInComposition by remember { mutableStateOf(defaultData != null) }
  var falseInComposition by remember { mutableStateOf(defaultData != null) }

  val attach = data != null
  val slowTransition = remember {
    LayerTransition(
      enter = slideInVertically(tween(400)) { it / 2 } + fadeIn(tween(400)),
      exit = slideOutVertically(tween(TRANSITION_DURATION_MS)) { it / 2 } + fadeOut(tween(TRANSITION_DURATION_MS)),
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .safeDrawingPadding()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = "对比退场动画（${TRANSITION_DURATION_MS}ms）期间清空业务数据：",
      style = MaterialTheme.typography.titleSmall,
    )
    Spacer(modifier = Modifier.height(12.dp))

    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Button(
        enabled = !attach,
        onClick = {
          data = "Data #${index++}"
        },
      ) {
        Text(text = "Attach")
      }

      Button(
        enabled = attach,
        onClick = {
          data = null
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
        ),
      ) {
        Text(text = "Detach & Clear Data")
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      AnchorCard(
        modifier = Modifier
          .weight(1f)
          .layerTag(TAG_CAPTURE_TRUE),
        title = "captureOnDetach = true",
        inComposition = trueInComposition,
      )
      AnchorCard(
        modifier = Modifier
          .weight(1f)
          .layerTag(TAG_CAPTURE_FALSE),
        title = "captureOnDetach = false",
        inComposition = falseInComposition,
      )
    }
  }

  TargetLayer(
    target = LayerTarget.Tag(TAG_CAPTURE_TRUE),
    attach = attach,
    onDetachRequest = { data = null },
    backgroundColor = Color.Transparent,
    detachOnTouchOutside = null,
    alignment = TargetAlignment.BottomCenter,
    alignmentOffsetY = TargetAlignmentOffset.DP(12),
    transition = slowTransition,
    captureOnDetach = true,
    debug = true,
  ) {
    DisposableEffect(Unit) {
      trueInComposition = true
      logMsg { "captureOnDetach=true content enter composition" }
      onDispose {
        trueInComposition = false
        logMsg { "captureOnDetach=true content dispose" }
      }
    }
    LayerCard(
      modifier = Modifier.width(160.dp),
      subtitle = "退场立即销毁组合\n保留最后一帧画面",
      data = data,
    )
  }

  TargetLayer(
    target = LayerTarget.Tag(TAG_CAPTURE_FALSE),
    attach = attach,
    onDetachRequest = { data = null },
    backgroundColor = Color.Transparent,
    detachOnBackPress = null,
    detachOnTouchOutside = null,
    alignment = TargetAlignment.BottomCenter,
    alignmentOffsetY = TargetAlignmentOffset.DP(12),
    transition = slowTransition,
    captureOnDetach = false,
    debug = true,
  ) {
    DisposableEffect(Unit) {
      falseInComposition = true
      logMsg { "captureOnDetach=false content enter composition" }
      onDispose {
        falseInComposition = false
        logMsg { "captureOnDetach=false content dispose" }
      }
    }
    LayerCard(
      modifier = Modifier.width(160.dp),
      subtitle = "退场保留活跃组合\n数据清空立即重组",
      data = data,
    )
  }
}

@Composable
private fun AnchorCard(
  modifier: Modifier = Modifier,
  title: String,
  inComposition: Boolean,
) {
  Column(
    modifier = modifier
      .background(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
      )
      .padding(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = title,
      fontSize = 13.sp,
      fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = if (inComposition) "组合存活: YES" else "组合存活: NO",
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = if (inComposition) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
    )
  }
}

@Composable
private fun LayerCard(
  modifier: Modifier = Modifier,
  subtitle: String,
  data: String?,
) {
  var ticks by remember { mutableIntStateOf(0) }

  LaunchedEffect(Unit) {
    while (true) {
      delay(100.milliseconds)
      ticks++
    }
  }

  val containerColor = if (data != null) {
    MaterialTheme.colorScheme.primaryContainer
  } else {
    MaterialTheme.colorScheme.errorContainer
  }
  val contentColor = if (data != null) {
    MaterialTheme.colorScheme.onPrimaryContainer
  } else {
    MaterialTheme.colorScheme.onErrorContainer
  }

  Column(
    modifier = modifier
      .background(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
      )
      .padding(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = data ?: "data = null",
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
      color = contentColor,
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        strokeWidth = 2.dp,
        color = contentColor,
      )
      Text(
        text = "tick: $ticks",
        fontSize = 13.sp,
        color = contentColor,
      )
    }
    Text(
      text = subtitle,
      fontSize = 11.sp,
      lineHeight = 15.sp,
      color = contentColor,
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewContent() {
  AppTheme {
    LayerContainer {
      Content(defaultData = "Data #1")
    }
  }
}
