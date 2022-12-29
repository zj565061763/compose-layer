package com.sd.demo.compose_layer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalAnimationApi::class)
private val layer = FTargetLayer().apply {
    this.isDebug = true
    this.setPosition(Layer.Position.BottomEnd)
    this.dialogBehavior
        .setBackgroundColor(Color.Transparent)
        .setConsumeTouchOutside(false)
    this.setFixOverflowDirection(Directions.All)
    this.setContent {
        AnimatedVisibility(
            visible = isVisibleState,
            enter = scaleIn(),
            exit = scaleOut(),
        ) {
            VerticalList(
                count = 5,
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

@Composable
private fun Content() {
    layer.Init()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        items(100) { index ->
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
    val context = LocalContext.current
    var layoutCoordinates: LayoutCoordinates? by remember { mutableStateOf(null) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .height(50.dp)
            .onGloballyPositioned {
                layoutCoordinates = it
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        Toast
                            .makeText(context, "try long click", Toast.LENGTH_SHORT)
                            .show()
                    },
                    onLongPress = {
                        val layout = layoutCoordinates
                        if (layout?.isAttached == true) {
                            val offset = layout.localToWindow(it)
                            onClick(IntOffset(offset.x.toInt(), offset.y.toInt()))
                        }
                    }
                )
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