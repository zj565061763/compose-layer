package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material3.*
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
    val layer = rememberTargetLayer(true)
    LaunchedEffect(layer) {
        layer.setTarget("hello")
        // 关闭窗口行为
        layer.dialogBehavior.setEnabled(false)
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
            Button(
                onClick = {
                    showTarget = !showTarget
                },
            ) {
                Text(if (showTarget) "Hide Target" else "Show Target")
            }

            if (showTarget) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.LightGray)
                        .layerTarget(target)
                ) {
                    Text(text = "Target", modifier = Modifier.align(Alignment.Center))
                }
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
            list = listOf(
                Layer.Position.TopStart.name,
                Layer.Position.TopCenter.name,
                Layer.Position.TopEnd.name,
            ),
            onClick = {
                onClick(Layer.Position.valueOf(it))
            }
        )

        // Bottom
        ButtonRow(
            list = listOf(
                Layer.Position.BottomStart.name,
                Layer.Position.BottomCenter.name,
                Layer.Position.BottomEnd.name,
            ),
            onClick = {
                onClick(Layer.Position.valueOf(it))
            }
        )

        // Center
        ButtonRow(
            list = listOf(
                Layer.Position.Top.name,
                Layer.Position.Bottom.name,
                Layer.Position.Center.name,
                Layer.Position.Start.name,
                Layer.Position.End.name,
            ),
            onClick = {
                onClick(Layer.Position.valueOf(it))
            }
        )

        // Start
        ButtonRow(
            list = listOf(
                Layer.Position.StartTop.name,
                Layer.Position.StartCenter.name,
                Layer.Position.StartBottom.name,
            ),
            onClick = {
                onClick(Layer.Position.valueOf(it))
            }
        )

        // End
        ButtonRow(
            list = listOf(
                Layer.Position.EndTop.name,
                Layer.Position.EndCenter.name,
                Layer.Position.EndBottom.name,
            ),
            onClick = {
                onClick(Layer.Position.valueOf(it))
            }
        )

        Button(
            onClick = {
                onClickDetach()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(text = "Detach")
        }
    }
}

@Composable
private fun ButtonRow(
    modifier: Modifier = Modifier,
    list: List<String>,
    onClick: (String) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        list.forEach {
            Button(
                onClick = {
                    onClick(it)
                },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(text = it)
            }
        }
    }
}