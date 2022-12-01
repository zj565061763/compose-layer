package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.FLayer
import com.sd.lib.compose.layer.FLayerContainer
import com.sd.lib.compose.layer.fLayerTarget
import com.sd.lib.compose.layer.rememberFLayer

class SampleDropdown : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    FLayerContainer(modifier = Modifier.fillMaxSize()) {
                        Content()
                    }
                }
            }
        }
    }
}

@Composable
private fun Content() {
    val layer = createLayer()
    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(300.dp))
        Button(
            onClick = {
                layer.setTarget("button")
                layer.attach()
            },
            modifier = Modifier.fLayerTarget("button")
        ) {
            Text("Attach")
        }
    }
}

@Composable
private fun createLayer(): FLayer {
    val layer = rememberFLayer()
    LaunchedEffect(layer) {
        layer.setPosition(FLayer.Position.BottomCenter)
        layer.setFixOverflowDirection(FLayer.OverflowDirection.Bottom)
        layer.setContent {
            LayerContent(isVisible)
        }
    }
    return layer
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LayerContent(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(transformOrigin = TransformOrigin(0.5f, 0f)),
        exit = scaleOut(transformOrigin = TransformOrigin(0.5f, 0f)),
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .width(200.dp)
                .navigationBarsPadding()
        ) {
            items(50) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color.Red)
                ) {
                    Text(
                        index.toString(),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}