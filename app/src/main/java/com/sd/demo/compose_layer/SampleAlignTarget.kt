package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.FLayer
import com.sd.lib.compose.layer.FLayerContainer
import com.sd.lib.compose.layer.fLayerTarget
import com.sd.lib.compose.layer.rememberFLayer

class SampleAlignTarget : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    FLayerContainer(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Content()
                    }
                }
            }
        }
    }
}

@Composable
private fun Content() {

    val target = "target"
    val listLayer = layers()

    TargetView(target)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Spacer(modifier = Modifier.height(50.dp))
        Button(
            onClick = {
                listLayer.forEach { it.attach(target) }
            }
        ) {
            Text(text = "Attach")
        }

        Button(
            onClick = {
                listLayer.forEach { it.detach() }
            }
        ) {
            Text(text = "Detach")
        }
    }


//    Box(modifier = Modifier
//        .size(250.dp)
//        .background(Color.LightGray)
//
//        .fLayer {
//            it.alignment = Alignment.TopStart
//            ColorBox(Color.Red.copy(alpha = 0.8f), "1")
//        }
//
//        .fLayer {
//            it.alignment = Alignment.TopCenter
//            it.centerOutside = false
//            ColorBox(Color.Green.copy(alpha = 0.8f), "2")
//        }
//
//        .fLayer {
//            it.alignment = Alignment.TopEnd
//            ColorBox(Color.Blue.copy(alpha = 0.8f), "3")
//        }
//
//        .fLayer {
//            it.alignment = Alignment.CenterStart
//            it.centerOutside = false
//            ColorBox(Color.Red.copy(alpha = 0.5f), "4")
//        }
//
//        .fLayer {
//            it.alignment = Alignment.Center
//            ColorBox(Color.Green.copy(alpha = 0.5f), "5")
//        }
//
//        .fLayer {
//            it.alignment = Alignment.CenterEnd
//            it.centerOutside = false
//            ColorBox(Color.Blue.copy(alpha = 0.5f), "6")
//        }
//
//        .fLayer {
//            it.alignment = Alignment.BottomStart
//            ColorBox(Color.Red.copy(alpha = 0.2f), "7")
//        }
//
//        .fLayer {
//            it.alignment = Alignment.BottomCenter
//            it.centerOutside = false
//            ColorBox(Color.Green.copy(alpha = 0.2f), "8")
//        }
//
//        .fLayer {
//            it.alignment = Alignment.BottomEnd
//            ColorBox(Color.Blue.copy(alpha = 0.2f), "9")
//        }
//
//        .fLayer {
//            AnimateBlurBox()
//        }
//    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun layers(): List<FLayer> {
    // TopStart
    val layerTopStart = rememberFLayer().also { layer ->
        LaunchedEffect(layer) {
            layer.alignment = Alignment.TopStart
            layer.setContent {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = scaleIn() + slideIn { IntOffset(-it.width / 2, -it.height / 2) },
                    exit = scaleOut() + slideOut { IntOffset(-it.width / 2, -it.height / 2) },
                ) {
                    ColorBox(Color.Red.copy(alpha = 0.8f), "1")
                }
            }
        }
    }

    // TopCenter
    val layerTopCenter = rememberFLayer().also { layer ->
        LaunchedEffect(layer) {
            layer.alignment = Alignment.TopCenter
            layer.setContent {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = scaleIn() + slideInVertically(),
                    exit = scaleOut() + slideOutVertically(),
                ) {
                    ColorBox(Color.Green.copy(alpha = 0.8f), "2")
                }
            }
        }
    }

    return listOf(
        layerTopStart,
        layerTopCenter,
    )
}

@Composable
private fun TargetView(
    target: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.height(200.dp))
        Box(
            modifier = Modifier
                .size(250.dp)
                .background(Color.LightGray)
                .fLayerTarget(target)
        )
        Box(modifier = Modifier.height(2000.dp))
    }
}