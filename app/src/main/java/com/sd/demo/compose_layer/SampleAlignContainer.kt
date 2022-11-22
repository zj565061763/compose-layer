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
import com.sd.lib.compose.layer.DialogBehavior
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Content() {
    val layer = rememberFLayer()
    LaunchedEffect(layer) {
        layer.setAlignment(Alignment.Center)
        // 设置窗口行为，例如按返回键关闭layer，默认开启
        layer.setDialogBehavior {
            // 关闭窗口行为
            DialogBehavior.Disabled
        }
        layer.setContent {
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                ColorBox(Color.Red, "Box")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Spacer(modifier = Modifier.height(50.dp))
        Button(
            onClick = {
                layer.attach()
            }
        ) {
            Text(text = "Attach")
        }

        Button(
            onClick = {
                layer.detach()
            }
        ) {
            Text(text = "Detach")
        }
    }
}