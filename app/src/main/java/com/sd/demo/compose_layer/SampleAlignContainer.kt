package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.Layer
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.rememberLayer

class SampleAlignContainer : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                LayerContainer {
                    Content()
                }
            }
        }
    }
}

@Composable
private fun Content() {
    val layer = layer()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = { layer.attach() }
        ) {
            Text(text = "Attach")
        }
    }
}

@Composable
private fun layer(): Layer {
    return rememberLayer(
        onCreate = {
            it.isDebug = true
            it.setPosition(Layer.Position.StartCenter)
            it.setCanceledOnTouchBackground(true)
            it.registerAttachCallback {
                logMsg { "callback onAttach" }
            }
            it.registerDetachCallback {
                logMsg { "callback onDetach" }
            }
        },
    ) {
        ColorBox(
            color = Color.Red,
            text = "Box",
        )
    }
}