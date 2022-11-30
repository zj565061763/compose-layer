package com.sd.demo.compose_layer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sd.lib.compose.layer.FLayer
import com.sd.lib.compose.layer.fLayerTarget

@Composable
fun AlignTargetUi(
    layer: FLayer,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        TargetView("hello")
        ButtonsView(layer)
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
        Box(modifier = Modifier.height(300.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showTarget) {
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .background(Color.LightGray)
                        .fLayerTarget(target)
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
    layer: FLayer,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Top
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

        // Center
        ButtonRow(
            start = "CenterStart",
            center = "Center",
            end = "CenterEnd",
            onClickStart = {
                layer.setPosition(FLayer.Position.CenterStart)
                layer.attach()
            },
            onClickCenter = {
                layer.setPosition(FLayer.Position.Center)
                layer.attach()
            },
            onClickEnd = {
                layer.setPosition(FLayer.Position.CenterEnd)
                layer.attach()
            },
        )

        // Bottom
        ButtonRow(
            start = "BottomStart",
            center = "BottomCenter",
            end = "BottomEnd",
            onClickStart = {
                layer.setPosition(FLayer.Position.BottomStart)
                layer.attach()
            },
            onClickCenter = {
                layer.setPosition(FLayer.Position.BottomCenter)
                layer.attach()
            },
            onClickEnd = {
                layer.setPosition(FLayer.Position.BottomEnd)
                layer.attach()
            },
        )

        Button(
            onClick = { layer.detach() }
        ) {
            Text(text = "Detach layer")
        }
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