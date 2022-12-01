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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.*
import com.sd.lib.compose.systemui.rememberStatusBarController

class SampleAlignTarget : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val statusBarController = rememberStatusBarController()
            SideEffect {
                statusBarController.isLight = true
                statusBarController.color = Color.Transparent
            }

            AppTheme {
                Surface(color = MaterialTheme.colors.background) {
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
    val layer = createLayer()

    Box(modifier = Modifier.fillMaxSize()) {
        TargetView("hello")
        ButtonsView(
            onClickDetach = {
                layer.detach()
            },
            onClick = { position ->
                layer.setPosition(position)
                layer.attach()
            }
        )
    }
}

@Composable
private fun createLayer(): TargetLayer {
    val layer = rememberTargetLayer()
    LaunchedEffect(layer) {
        layer.setTarget("hello")
        // 关闭窗口行为
        layer.setDialogBehavior { null }
        layer.setContent {
            LayerContent(isVisible)
        }
    }
    return layer
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LayerContent(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(),
        exit = scaleOut(),
        modifier = modifier,
    ) {
        ColorBox(
            color = Color.Red,
            text = "Box",
        )
    }
}

@Composable
private fun TargetView(
    target: String,
    modifier: Modifier = Modifier,
) {
    var showTarget by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.height(400.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showTarget) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.LightGray)
                        .layerTarget(target)
                )
            }

            Button(
                onClick = {
                    showTarget = !showTarget
                },
            ) {
                Text(if (showTarget) "Hide target" else "Show target")
            }
        }

        Box(modifier = Modifier.height(2000.dp))
    }
}

@Composable
private fun ButtonsView(
    modifier: Modifier = Modifier,
    onClickDetach: () -> Unit,
    onClick: (Layer.Position) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top
        ButtonRow(
            start = "TopStart",
            center = "TopCenter",
            end = "TopEnd",
            onClickStart = {
                onClick(Layer.Position.TopStart)
            },
            onClickCenter = {
                onClick(Layer.Position.TopCenter)
            },
            onClickEnd = {
                onClick(Layer.Position.TopEnd)
            },
        )

        // Bottom
        ButtonRow(
            start = "BottomStart",
            center = "BottomCenter",
            end = "BottomEnd",
            onClickStart = {
                onClick(Layer.Position.BottomStart)
            },
            onClickCenter = {
                onClick(Layer.Position.BottomCenter)
            },
            onClickEnd = {
                onClick(Layer.Position.BottomEnd)
            },
        )

        // Center
        ButtonRow(
            start = "",
            center = "Center",
            end = "Detach",
            onClickStart = {},
            onClickCenter = {
                onClick(Layer.Position.Center)
            },
            onClickEnd = {
                onClickDetach()
            },
        )

        // Start
        ButtonRow(
            start = "StartTop",
            center = "StartCenter",
            end = "StartBottom",
            onClickStart = {
                onClick(Layer.Position.StartTop)
            },
            onClickCenter = {
                onClick(Layer.Position.StartCenter)
            },
            onClickEnd = {
                onClick(Layer.Position.StartBottom)
            },
        )

        // End
        ButtonRow(
            start = "EndTop",
            center = "EndCenter",
            end = "EndBottom",
            onClickStart = {
                onClick(Layer.Position.EndTop)
            },
            onClickCenter = {
                onClick(Layer.Position.EndCenter)
            },
            onClickEnd = {
                onClick(Layer.Position.EndBottom)
            },
        )
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
        horizontalArrangement = Arrangement.Center,
    ) {
        if (start.isNotEmpty()) {
            Button(
                onClick = onClickStart
            ) {
                Text(text = start)
            }
        }

        if (center.isNotEmpty()) {
            Spacer(modifier = Modifier.width(5.dp))
            Button(
                onClick = onClickCenter,
            ) {
                Text(text = center)
            }
            Spacer(modifier = Modifier.width(5.dp))
        }

        if (end.isNotEmpty()) {
            Button(
                onClick = onClickEnd
            ) {
                Text(text = end)
            }
        }
    }
}