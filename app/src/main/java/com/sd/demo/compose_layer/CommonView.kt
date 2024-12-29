package com.sd.demo.compose_layer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ColorBox(
  modifier: Modifier = Modifier,
  text: String = "Box",
  color: Color = Color.Red,
) {
  Box(
    modifier = modifier
      .widthIn(50.dp)
      .heightIn(50.dp)
      .background(color)
      .padding(5.dp)
  ) {
    Text(
      text = text,
      modifier = Modifier.align(Alignment.Center),
      color = Color.White,
      fontSize = 16.sp
    )
  }
}

@Composable
fun VerticalList(
  modifier: Modifier = Modifier,
  count: Int,
  onClick: (Int) -> Unit = { },
) {
  val context = LocalContext.current
  LazyColumn(
    modifier = modifier
      .fillMaxWidth()
      .background(Color.Green)
  ) {
    items(count) { index ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .background(Color.Red)
          .clickable {
            Toast
              .makeText(context, index.toString(), Toast.LENGTH_SHORT)
              .show()
            onClick(index)
          },
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "start",
        )
        Text(
          text = index.toString(),
          modifier = Modifier.weight(1f),
          textAlign = TextAlign.Center,
        )
        Text(
          text = "end",
        )
      }

    }
  }
}