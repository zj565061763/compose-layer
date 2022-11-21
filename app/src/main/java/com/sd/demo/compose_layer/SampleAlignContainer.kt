package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.FLayerContainer
import com.sd.lib.compose.layer.rememberFLayer

class SampleAlignContainer : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val layer = rememberFLayer()
    LaunchedEffect(layer) {
        layer.alignment = Alignment.TopCenter
        layer.setContent {
            ColorBox(Color.Red, "Box")
        }
        layer.attach()
    }
}