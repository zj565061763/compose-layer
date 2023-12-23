package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.Directions
import com.sd.lib.compose.layer.Layer
import com.sd.lib.compose.layer.DisplaySlideDownUp
import com.sd.lib.compose.layer.LayerContainer
import com.sd.lib.compose.layer.TargetLayer
import com.sd.lib.compose.layer.layerTarget
import com.sd.lib.compose.layer.rememberTargetLayer

class SampleDropDown : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
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
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
    ) {
        Spacer(modifier = Modifier.height(300.dp))
        Button(
            onClick = {
                layer.attach()
            },
            modifier = Modifier.layerTarget("button")
        ) {
            Text("Click")
        }
    }
}

@Composable
private fun layer(): TargetLayer {
    return rememberTargetLayer(
        onCreate = {
            it.isDebug = true
            it.setTarget("button")
            it.setPosition(Layer.Position.Bottom)
            it.setClipBackgroundDirection(Directions.Top)
        },
        display = {
            DisplaySlideDownUp()
        }
    ) {
        VerticalList(count = 5)
    }
}