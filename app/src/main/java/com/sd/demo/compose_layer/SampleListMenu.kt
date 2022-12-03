package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.*
import com.sd.lib.compose.systemui.rememberStatusBarController

class SampleListMenu : ComponentActivity() {
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

    LazyColumn(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        items(50) { index ->
            ListItem(
                text = index.toString()
            ) {
                layer.setTargetOffset(it)
                layer.attach()
            }
        }
    }
}

@Composable
private fun ListItem(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (IntOffset) -> Unit,
) {
    var layoutCoordinates: LayoutCoordinates? by remember { mutableStateOf(null) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .height(50.dp)
            .onGloballyPositioned {
                layoutCoordinates = it
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    val layout = layoutCoordinates
                    if (layout?.isAttached == true) {
                        val offset = layout.localToWindow(Offset.Zero) + it
                        onClick(IntOffset(offset.x.toInt(), offset.y.toInt()))
                    }
                }
            }
    ) {
        Text(
            text = text,
            modifier = Modifier.align(Alignment.Center),
        )
        Divider(
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun createLayer(): TargetLayer {
    val layer = rememberTargetLayer(true)
    LaunchedEffect(layer) {
        layer.setPosition(Layer.Position.TopEnd)
        layer.setDialogBehavior {
            it.copy(consumeTouchOutside = false, backgroundColor = Color.Transparent)
        }
        layer.setFixOverflowDirection(PlusDirection.All)
        layer.setContent {
            LayerContent(isVisible)
        }
        layer.setTarget("item_0")
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
        VerticalList(
            count = 5,
            modifier = Modifier.width(200.dp)
        )
    }
}