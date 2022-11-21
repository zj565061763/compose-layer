package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sd.demo.compose_layer.ui.theme.AppTheme
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

    val layerTopStart = rememberFLayer()
    LaunchedEffect(layerTopStart) {
        layerTopStart.alignment = Alignment.TopStart
        layerTopStart.setContent {
            ColorBox(Color.Red.copy(alpha = 0.8f), "1")
        }
        layerTopStart.attach(tag = "TopStart")
    }

    Column(
        modifier = Modifier
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
                .fLayerTarget("TopStart")
        )
        Box(modifier = Modifier.height(2000.dp))
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