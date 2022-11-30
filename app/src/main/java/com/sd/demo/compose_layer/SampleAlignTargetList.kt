package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.demo.compose_layer.view.AlignTargetUi
import com.sd.demo.compose_layer.view.VerticalList
import com.sd.lib.compose.layer.FLayer
import com.sd.lib.compose.layer.FLayerContainer
import com.sd.lib.compose.layer.rememberFLayer
import com.sd.lib.compose.systemui.rememberStatusBarController

class SampleAlignTargetList : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AppTheme {
                val statusBarController = rememberStatusBarController()
                SideEffect {
                    statusBarController.isLight = true
                    statusBarController.color = Color.Transparent
                }

                Surface(color = MaterialTheme.colors.background) {
                    FLayerContainer(modifier = Modifier.fillMaxSize()) {
                        val layer = createLayer()
                        AlignTargetUi(layer)
                    }
                }
            }
        }
    }
}


@Composable
private fun createLayer(): FLayer {
    val layer = rememberFLayer()
    LaunchedEffect(layer) {
        layer.setTarget("hello")
        // 关闭窗口行为
        layer.setDialogBehavior { null }
        layer.setFixOverflowDirection(
            FLayer.OverflowDirection.Top or FLayer.OverflowDirection.Bottom
        )
        layer.setContent {
            LayerContent(isVisible)
        }
    }

//    LaunchedEffect(layer.positionState) {
//        when (layer.positionState) {
//            FLayer.Position.TopStart,
//            FLayer.Position.TopCenter,
//            FLayer.Position.TopEnd -> {
//                layer.setFixOverflowDirection(FLayer.OverflowDirection.Bottom)
//            }
//            FLayer.Position.BottomStart,
//            FLayer.Position.BottomCenter,
//            FLayer.Position.BottomEnd -> {
//                layer.setFixOverflowDirection(FLayer.OverflowDirection.Top)
//            }
//            else -> {
//                layer.setFixOverflowDirection(FLayer.OverflowDirection.None)
//            }
//        }
//    }

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
        VerticalList()
    }
}