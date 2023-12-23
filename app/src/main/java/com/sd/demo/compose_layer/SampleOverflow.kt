package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.*

class SampleOverflow : ComponentActivity() {
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
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(300.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                onClick = {
                    layer.setTarget("button1")
                    layer.setFixOverflowDirection(null)
                    layer.attach()
                },
                modifier = Modifier.layerTarget("button1")
            ) {
                Text("Overflow")
            }

            Button(
                onClick = {
                    layer.setTarget("button2")
                    layer.setFixOverflowDirection(
                        Directions.Bottom + Directions.Start + Directions.End
                    )
                    layer.attach()
                },
                modifier = Modifier.layerTarget("button2")
            ) {
                Text("Fix overflow")
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun layer(): TargetLayer {
    return rememberTargetLayer(
        onCreate = {
            it.isDebug = true
            it.setPosition(Layer.Position.BottomCenter)
        },
        wrapper = {
            LayerAnimatedDefault(
                enter = scaleIn(transformOrigin = TransformOrigin(0.5f, 0f)),
                exit = scaleOut(transformOrigin = TransformOrigin(0.5f, 0f)),
            )
        }
    ) {
        VerticalList(
            count = 50,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}