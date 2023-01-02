package com.sd.lib.compose.layer

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

@Composable
fun FLayer(
    position: Layer.Position = Layer.Position.Center,
    clipToBounds: Boolean = false,
    dialogEnable: Boolean = true,
    dialogCancelable: Boolean = true,
    dialogCanceledOnTouchOutside: Boolean = true,
    dialogConsumeTouchOutside: Boolean = true,
    dialogBackgroundColor: Color = Color.Black.copy(alpha = 0.3f),
    onDetach: () -> Unit = {},
    content: @Composable Layer.ContentScope.() -> Unit,
) {
    val onDetachUpdated by rememberUpdatedState(onDetach)

    val layer = remember {
        object : FLayer() {
            override fun onDetach() {
                super.onDetach()
                onDetachUpdated()
            }
        }
    }

    layer.apply {
        this.setPosition(position)
        this.setClipToBounds(clipToBounds)
        this.dialogBehavior.apply {
            this.setEnabled(dialogEnable)
            this.setCancelable(dialogCancelable)
            this.setCanceledOnTouchOutside(dialogCanceledOnTouchOutside)
            this.setConsumeTouchOutside(dialogConsumeTouchOutside)
            this.setBackgroundColor(dialogBackgroundColor)
        }
        this.setContent(content)
        this.Init()
    }

    DisposableEffect(layer) {
        layer.attach()
        onDispose {
            layer.detach()
            // TODO destroy layer
        }
    }
}