package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.*
import java.util.*

class SampleDropDown : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
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
    val layer = rememberTargetLayer()
    LaunchedEffect(layer) {
        layer.setPosition(Layer.Position.BottomCenter)
        layer.setOffsetTransform { it.copy(x = 0) }
        layer.setContent {
            LayerContent(isVisible)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
    ) {
        Spacer(modifier = Modifier.height(300.dp))
        Button(
            onClick = {
                layer.run {
                    setTarget("button")
                    layer.attach()
                }
            },
            modifier = Modifier.layerTarget("button")
        ) {
            Text("Click")
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LayerContent(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(transformOrigin = TransformOrigin(0.5f, 0f)),
        exit = scaleOut(transformOrigin = TransformOrigin(0.5f, 0f)),
        modifier = modifier,
    ) {
        VerticalList(
            count = 50,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}