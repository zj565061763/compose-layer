package com.sd.demo.compose_layer

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

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
fun AnimateBlurBox(
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    val height by animateDpAsState(if (visible) 100.dp else 0.dp)
    LaunchedEffect(true) {
        while (true) {
            visible = visible.not()
            delay(2000)
        }
    }

    Box(
        modifier = modifier
            .padding(10.dp)
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f), shape = MaterialTheme.shapes.medium)
            .height(height)
    )
}