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
    val attach1 = remember { mutableStateOf(false) }
    val attach2 = remember { mutableStateOf(false) }

    Layer1(attach1)
    Layer2(attach2)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Button(
            onClick = {
                attach1.value = true
                attach2.value = true
            }
        ) {
            Text(text = "Attach")
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Layer1(
    attach: MutableState<Boolean>,
) {
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
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Layer2(
    attach: MutableState<Boolean>,
) {
    if (attach.value) {
        FLayer(
            onDetach = { attach.value = false },
            position = Layer.Position.BottomCenter,
        ) {
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
    }
}