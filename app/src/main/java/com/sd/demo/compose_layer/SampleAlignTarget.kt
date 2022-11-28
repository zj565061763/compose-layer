package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.core.view.WindowCompat
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.FLayer
import com.sd.lib.compose.layer.FLayerContainer
import com.sd.lib.compose.layer.fLayerTarget
import com.sd.lib.compose.layer.rememberFLayer

class SampleAlignTarget : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        FLayerContainer(modifier = Modifier.fillMaxSize()) {
                            Content()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Content() {

    TargetView("hello")

    val layer = rememberFLayer()
    LaunchedEffect(layer) {
        layer.setTarget("hello")
        // 关闭窗口行为
        layer.setDialogBehavior { null }
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
        ButtonRow(
            start = "TopStart",
            center = "TopCenter",
            end = "TopEnd",
            onClickStart = {
                layer.setPosition(FLayer.Position.TopStart)
                layer.attach()
            },
            onClickCenter = {
                layer.setPosition(FLayer.Position.TopCenter)
                layer.attach()
            },
            onClickEnd = {
                layer.setPosition(FLayer.Position.TopEnd)
                layer.attach()
            },
        )

        Button(
            onClick = { layer.detach() }
        ) {
            Text(text = "Detach")
        }
    }
}

@Composable
private fun TargetView(
    target: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.height(200.dp))
        Box(
            modifier = Modifier
                .size(250.dp)
                .background(Color.LightGray)
                .fLayerTarget(target)
        )
        Box(modifier = Modifier.height(2000.dp))
    }
}

@Composable
private fun ButtonRow(
    modifier: Modifier = Modifier,
    start: String,
    center: String,
    end: String,
    onClickStart: () -> Unit,
    onClickCenter: () -> Unit,
    onClickEnd: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Button(
            onClick = onClickStart
        ) {
            Text(text = start)
        }

        Button(
            onClick = onClickCenter
        ) {
            Text(text = center)
        }

        Button(
            onClick = onClickEnd
        ) {
            Text(text = end)
        }
    }
}