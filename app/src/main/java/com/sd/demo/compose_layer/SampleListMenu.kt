package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerTarget("item_$index")
                    .height(50.dp)
                    .clickable {
                        layer.setTarget("item_$index")
                        layer.attach()
                    }
            ) {
                Text(
                    index.toString(),
                    modifier = Modifier.align(Alignment.Center),
                )
                Divider(
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun createLayer(): TargetLayer {
    val layer = rememberTargetLayer()
    LaunchedEffect(layer) {
        layer.setPosition(Layer.Position.Bottom)
        layer.setDialogBehavior {
            it.copy(consumeTouchOutside = false)
        }
        layer.setFixOverflowDirection(PlusDirection.Bottom)
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