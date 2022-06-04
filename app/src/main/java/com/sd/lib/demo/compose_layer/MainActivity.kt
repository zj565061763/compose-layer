package com.sd.lib.demo.compose_layer

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.sd.lib.compose.layer.FLayer
import com.sd.lib.compose.layer.FLayerContainer
import com.sd.lib.compose.layer.fLayer
import com.sd.lib.compose.layer.rememberFLayerState
import com.sd.lib.demo.compose_layer.ui.theme.ComposelayerTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            ComposelayerTheme {
                val controller = rememberSystemUiController()
                SideEffect {
                    controller.isStatusBarVisible = false
                }
                Surface(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxSize(),
                    color = MaterialTheme.colors.background,
                ) {
                    FLayerContainer() {
//                        AlignContainer()
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
    }) {
        AnimatedVisibility(
            visible = visible,
            enter = expandIn(),
            exit = shrinkOut(),
        ) {
            ColorBox(Color.Green, "111")
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

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .size(250.dp)
            .background(Color.LightGray)
            .align(Alignment.Center)
            .onSizeChanged {
                Log.i("TAG", "size")
            }
            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.TopStart
            }) {
                ColorBox(Color.Red.copy(alpha = 0.8f), "1")
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.TopCenter
                this.centerOutside = false
            }) {
                ColorBox(Color.Green.copy(alpha = 0.8f), "2")
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.TopEnd
            }) {
                ColorBox(Color.Blue.copy(alpha = 0.8f), "3")
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.CenterStart
                this.centerOutside = false
            }) {
                ColorBox(Color.Red.copy(alpha = 0.5f), "4")
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.Center
            }) {
                ColorBox(Color.Green.copy(alpha = 0.5f), "5")
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.CenterEnd
                this.centerOutside = false
            }) {
                ColorBox(Color.Blue.copy(alpha = 0.5f), "6")
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.BottomStart
            }) {
                ColorBox(Color.Red.copy(alpha = 0.2f), "7")
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.BottomCenter
                this.centerOutside = false
            }) {
                ColorBox(Color.Green.copy(alpha = 0.2f), "8")
            }

            .fLayer(rememberFLayerState().apply {
                this.alignment = Alignment.BottomEnd
            }) {
                ColorBox(Color.Blue.copy(alpha = 0.2f), "9")
            }

            .fLayer(rememberFLayerState()) {
                BlurBox()
            }
        )
    }
}

@Composable
private fun ColorBox(
    color: Color,
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier
        .widthIn(50.dp)
        .heightIn(50.dp)
        .background(color)
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
private fun BlurBox() {
    Box(modifier = Modifier
        .fillMaxWidth()
        .background(Color.Black.copy(alpha = 0.3f))
        .height(300.dp)
    ) {
    }
}
