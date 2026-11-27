package com.sd.demo.compose_layer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_layer.ui.theme.AppTheme

private val SAMPLE_ACTIVITIES = listOf<Class<out Activity>>(
  SampleAlignContainer::class.java,
  SampleAlignTarget::class.java,
  SampleDropDown::class.java,
  SampleTargetOffset::class.java,
  SampleFixOverflow::class.java,
  SampleZIndex::class.java,
  SampleListMenu::class.java,
  SampleSmartAlign::class.java,
  SampleDetachControl::class.java,
  SampleCaptureOnDetach::class.java,
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AppTheme {
        Content(
          listActivity = SAMPLE_ACTIVITIES,
          onClickItem = {
            startActivity(Intent(this, it))
          },
        )
      }
    }
  }
}

@Composable
private fun Content(
  modifier: Modifier = Modifier,
  listActivity: List<Class<out Activity>>,
  onClickItem: (Class<out Activity>) -> Unit,
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .safeDrawingPadding(),
    contentPadding = PaddingValues(vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    items(listActivity) { item ->
      Button(
        onClick = { onClickItem(item) },
      ) {
        Text(text = item.simpleName)
      }
    }
  }
}

inline fun logMsg(block: () -> String) {
  Log.i("FLayer-demo", block())
}

@Preview(showBackground = true)
@Composable
private fun PreviewContent() {
  AppTheme {
    Content(
      listActivity = SAMPLE_ACTIVITIES,
      onClickItem = {},
    )
  }
}
