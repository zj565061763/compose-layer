package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.sd.lib.compose.layer.Layer
import com.sd.lib.compose.layer.LayerContainer

class SampleDetachControl : ComponentActivity() {
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
) {
  var attach by remember { mutableStateOf(defaultAttach) }
  var detachOnBackPress by remember { mutableStateOf<Boolean?>(true) }
  var detachOnTouchOutside by remember { mutableStateOf<Boolean?>(true) }
  var innerBackHandlerEnabled by remember { mutableStateOf(false) }

  var backgroundClickCount by remember { mutableIntStateOf(0) }
  var pageBackCount by remember { mutableIntStateOf(0) }
  var innerBackCount by remember { mutableIntStateOf(0) }
  var lastEvent by remember { mutableStateOf("无") }

  BackHandler {
    pageBackCount++
    lastEvent = "底层页面 BackHandler 触发 (#$pageBackCount)"
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .safeDrawingPadding()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    PolicySelector(
      title = "detachOnBackPress（true: 请求移除 / false: 拦截不移除 / null: 透传到底层）",
      value = detachOnBackPress,
      onSelect = { detachOnBackPress = it },
    )

    PolicySelector(
      title = "detachOnTouchOutside（true: 请求移除 / false: 拦截不穿透 / null: 穿透点击底层）",
      value = detachOnTouchOutside,
      onSelect = { detachOnTouchOutside = it },
    )

    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Button(
        onClick = { attach = !attach },
      ) {
        Text(text = if (attach) "主动关闭 Layer" else "打开 Layer")
      }

      OutlinedButton(
        onClick = {
          backgroundClickCount++
          lastEvent = "底层按钮被点击 (#$backgroundClickCount)"
        },
      ) {
        Text(text = "底层按钮 ($backgroundClickCount)")
      }
    }

    Text(
      text = "最近事件：$lastEvent",
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.primary,
    )
  }

  Layer(
    attach = attach,
    onDetachRequest = { reason ->
      lastEvent = "onDetachRequest($reason)"
      attach = false
    },
    detachOnBackPress = detachOnBackPress,
    detachOnTouchOutside = detachOnTouchOutside,
    backgroundColor = Color.Black.copy(alpha = 0.25f),
    alignment = Alignment.BottomCenter,
    debug = true,
  ) {
    BackHandler(enabled = innerBackHandlerEnabled) {
      innerBackCount++
      lastEvent = "Layer 内部 BackHandler 优先拦截 (#$innerBackCount)"
    }

    DetachLayerCard(
      modifier = Modifier
        .padding(bottom = 48.dp)
        .width(300.dp),
      innerBackHandlerEnabled = innerBackHandlerEnabled,
      innerBackCount = innerBackCount,
      onCheckedChange = { innerBackHandlerEnabled = it },
      onClickClose = {
        lastEvent = "点击浮层内部关闭按钮"
        attach = false
      },
    )
  }
}

@Composable
private fun PolicySelector(
  modifier: Modifier = Modifier,
  title: String,
  value: Boolean?,
  onSelect: (Boolean?) -> Unit,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
  ) {
    Text(
      text = title,
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      listOf(true, false, null).forEach { item ->
        FilterChip(
          selected = value == item,
          onClick = { onSelect(item) },
          label = { Text(text = item.toString()) },
        )
      }
    }
  }
}

@Composable
private fun DetachLayerCard(
  modifier: Modifier = Modifier,
  innerBackHandlerEnabled: Boolean,
  innerBackCount: Int,
  onCheckedChange: (Boolean) -> Unit,
  onClickClose: () -> Unit,
) {
  Column(
    modifier = modifier
      .background(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
      )
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = "Layer 内容区域",
      style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "内部 BackHandler 优先拦截 ($innerBackCount)",
        fontSize = 13.sp,
      )
      Switch(
        checked = innerBackHandlerEnabled,
        onCheckedChange = onCheckedChange,
      )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Button(
      onClick = onClickClose,
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error,
      ),
    ) {
      Text(text = "关闭浮层")
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewContent() {
  AppTheme {
    LayerContainer {
      Content(defaultAttach = true)
    }
  }
}
