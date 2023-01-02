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
@Composable
private fun Content() {
    var showLayer by remember { mutableStateOf(false) }

    if (showLayer) {
        FLayer(
            onDetach = {
                showLayer = false
            }
        ) {
            AnimatedVisibility(
                visible = isVisibleState,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                ColorBox(
                    color = Color.Red,
                    text = "Box",
                )
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Button(
            onClick = {
                showLayer = true
            }
        ) {
            Text(text = "Attach")
        }
    }
}