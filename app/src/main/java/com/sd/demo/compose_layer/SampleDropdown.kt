package com.sd.demo.compose_layer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sd.demo.compose_layer.ui.theme.AppTheme
import com.sd.lib.compose.layer.*
import java.util.*

class SampleDropdown : ComponentActivity() {
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
        layer.setContent { LayerContent(isVisible, count = 5) }
    }

    val layerFixOverflow = rememberTargetLayer()
    LaunchedEffect(layerFixOverflow) {
        layerFixOverflow.setPosition(Layer.Position.BottomCenter)
        layerFixOverflow.setFixOverflowDirection(
            TargetLayer.OverflowDirection.Bottom
                    or TargetLayer.OverflowDirection.Start
                    or TargetLayer.OverflowDirection.End
        )
        layerFixOverflow.setContent { LayerContent(isVisible, count = 50) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
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
                    layerFixOverflow.run {
                        setTarget("button1")
                        attach()
                    }
                },
                modifier = Modifier.layerTarget("button1")
            ) {
                Text("Attach1")
            }

            Button(
                onClick = {
                    layer.run {
                        setTarget("button2")
                        layer.attach()
                    }
                },
                modifier = Modifier.layerTarget("button2")
            ) {
                Text("Attach2")
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LayerContent(
    isVisible: Boolean,
    count: Int,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(transformOrigin = TransformOrigin(0.5f, 0f)),
        exit = scaleOut(transformOrigin = TransformOrigin(0.5f, 0f)),
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            items(count) { index ->
                ListItem(index)
            }
        }
    }
}

@Composable
private fun ListItem(
    index: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.Red),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "start",
        )
        Text(
            text = index.toString(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "end",
        )
    }
}