package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.FLayer
import com.sd.lib.compose.layer.FLayerContainer
import com.sd.lib.compose.layer.rememberFLayer

class SampleAlignContainer : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        FLayerContainer(modifier = Modifier.fillMaxSize()) {
                            Content()
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun Content() {
    val layer1 = createLayer("1", FLayer.Position.Center)
    val layer2 = createLayer("2", FLayer.Position.BottomCenter)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Button(
            onClick = {
                layer2.attach()
                layer1.attach()
            }
        ) {
            Text(text = "Attach")
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun createLayer(
    text: String,
    position: FLayer.Position,
): FLayer {
    val layer = rememberFLayer()
    LaunchedEffect(layer, position) {
        layer.setPosition(position)
        layer.setContent {
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                ColorBox(Color.Red, text)
            }
        }
    }
    return layer
}