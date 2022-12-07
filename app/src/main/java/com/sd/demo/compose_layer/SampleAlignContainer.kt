package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.FLayer
import com.sd.lib.compose.layer.Layer
import com.sd.lib.compose.layer.LayerContainer

class SampleAlignContainer : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface {
                    LayerContainer(modifier = Modifier.fillMaxSize()) {
                        Content()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
private val layer1 = FLayer().apply {
    this.isDebug = true
    this.setPosition(Layer.Position.Center)
    this.setContent {
        AnimatedVisibility(
            visible = isVisibleState,
            enter = scaleIn(),
            exit = scaleOut(),
        ) {
            ColorBox(
                color = Color.Red,
                text = "1",
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
private val layer2 = FLayer().apply {
    this.isDebug = true
    this.setPosition(Layer.Position.BottomCenter)
    this.setContent {
        AnimatedVisibility(
            visible = isVisibleState,
            enter = scaleIn(),
            exit = scaleOut(),
        ) {
            ColorBox(
                color = Color.Red,
                text = "2",
            )
        }
    }
}

@Composable
private fun Content() {
    layer1.Init()
    layer2.Init()

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