package com.sd.demo.compose_layer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ColorBox(
    color: Color,
    text: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clickable {
                Toast
                    .makeText(context, text, Toast.LENGTH_SHORT)
                    .show()
            }
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
fun VerticalList() {
    LazyColumn(modifier = Modifier.width(50.dp)) {
        items(50) {
            ColorBox(
                color = Color.Red,
                text = it.toString(),
                modifier = Modifier.size(50.dp)
            )
        }
    }
}