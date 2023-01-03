package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.FLayer
import com.sd.lib.compose.layer.Layer
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.setContent

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

@Composable
private fun Content() {
    val layerAttach1 = layerAttach1()
    val layer2 = layer2()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Button(
            onClick = {
                layerAttach1.value = true
                layer2.attach()
            }
        ) {
            Text(text = "Attach")
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun layerAttach1(): MutableState<Boolean> {
    val attach = remember { mutableStateOf(false) }
    if (attach.value) {
        FLayer(
            onDetach = { attach.value = false },
            debug = true,
            zIndex = 1f,
        ) {
            AnimatedVisibility(
                visible = isVisibleState,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                ColorBox(
                    color = Color.Red,
                    text = "Box1",
                )
            }
        }
    }
    return attach
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun layer2(): Layer {
    return remember { FLayer() }.also {
        it.setPosition(Layer.Position.BottomCenter)
        it.setContent {
            AnimatedVisibility(
                visible = isVisibleState,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                ColorBox(
                    color = Color.Red,
                    text = "Box2",
                )
            }
        }
        it.Init()
    }
}