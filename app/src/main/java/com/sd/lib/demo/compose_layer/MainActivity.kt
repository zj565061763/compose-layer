package com.sd.lib.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sd.lib.compose.layer.FLayer
import com.sd.lib.compose.layer.FLayerContainer
import com.sd.lib.compose.layer.fLayer
import com.sd.lib.compose.layer.rememberFLayerState
import com.sd.lib.demo.compose_layer.ui.theme.ComposelayerTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposelayerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    FLayerContainer() {
                        AlignContainer()
                        AlignTarget()
                    }
                }
            }
        }
    }
}

@Composable
fun AlignContainer() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(true) {
        while (true) {
            delay(2000)
            visible = visible.not()
        }
    }

    // 相对于容器
    FLayer(state = rememberFLayerState().apply {
        this.alignment = Alignment.BottomCenter
        this.y = (-20).dp
    }) {
        AnimatedVisibility(
            visible = visible,
            enter = expandIn(),
            exit = shrinkOut(),
        ) {
            ColorBox(Color.Green)
        }
    }
}

@Composable
fun AlignTarget() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(true) {
        while (true) {
            delay(2000)
            visible = visible.not()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)
        .background(Color.Red.copy(0.3f))
    ) {
        Box(modifier = Modifier
            .size(150.dp)
            .background(Color.Black.copy(alpha = 0.3f))
            .align(Alignment.Center)
            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.TopStart
                this.x = 10.dp
                this.y = 10.dp
            }) {
                ColorBox(Color.Red)
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.TopCenter
                this.positionInterceptor = { layerSize, targetSize ->
                    this.y = -with(density) { targetSize.height.toDp() }
                }
            }) {
                AnimatedVisibility(
                    visible = visible,
                    enter = expandIn(),
                    exit = shrinkOut(),
                ) {
                    ColorBox(Color.Green)
                }
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.TopEnd
            }) {
                ColorBox(Color.Blue)
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.CenterStart
            }) {
                ColorBox(Color.Red.copy(alpha = 0.6f))
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.Center
            }) {
                ColorBox(Color.Green.copy(alpha = 0.6f))
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.CenterEnd
            }) {
                ColorBox(Color.Blue.copy(alpha = 0.6f))
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.BottomStart
            }) {
                ColorBox(Color.Red.copy(alpha = 0.3f))
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.BottomCenter
            }) {
                AnimatedVisibility(
                    visible = visible,
                    enter = expandIn(),
                    exit = shrinkOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ColorBox(Color.Green.copy(alpha = 0.3f))
                }
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.BottomEnd
            }) {
                ColorBox(Color.Blue.copy(alpha = 0.3f))
            }
        )
    }
}

@Composable
private fun ColorBox(color: Color) {
    Box(modifier = Modifier
        .size(20.dp)
        .background(color)
    )
}
