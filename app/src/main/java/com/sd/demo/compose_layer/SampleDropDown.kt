package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.*

class SampleDropDown : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
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
            it.setClipToBounds(true)
            it.setClipBackgroundDirection(Directions.Top)
        },
        wrapper = {
            LayerAnimatedVisibility(
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
            )
        }
    ) {
        VerticalList(count = 5)
    }
}