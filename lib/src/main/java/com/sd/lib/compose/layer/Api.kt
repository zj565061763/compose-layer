package com.sd.lib.compose.layer

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

@Composable
fun FLayer(
    dialogEnable: Boolean = true,
    dialogCancelable: Boolean = true,
    dialogCanceledOnTouchOutside: Boolean = true,
    dialogConsumeTouchOutside: Boolean = true,
    dialogBackgroundColor: Color = Color.Black.copy(alpha = 0.3f),
    position: Layer.Position = Layer.Position.Center,
    clipToBounds: Boolean = false,
    zIndex: Float? = null,
    debug: Boolean = false,
    onCreate: (Layer) -> Unit = { it.attach() },
    onDetach: () -> Unit = {},
    content: @Composable Layer.ContentScope.() -> Unit,
) {
    val onDetachUpdated by rememberUpdatedState(onDetach)

    val layer = remember {
        object : FLayer() {
            override fun onDetach() {
                super.onDetach()
                _layerContainer?.destroyLayer(this)
                onDetachUpdated()
            }
        }
    }

    layer.apply {
        this.isDebug = debug
        this.dialogBehavior.apply {
            this.setEnabled(dialogEnable)
            this.setCancelable(dialogCancelable)
            this.setCanceledOnTouchOutside(dialogCanceledOnTouchOutside)
            this.setConsumeTouchOutside(dialogConsumeTouchOutside)
            this.setBackgroundColor(dialogBackgroundColor)
        }
        this.setPosition(position)
        this.setClipToBounds(clipToBounds)
        this.setZIndex(zIndex)
        this.setContent(content)
        this.Init()
    }

    DisposableEffect(layer) {
        onCreate(layer)
        onDispose {
            layer.detach()
        }
    }
}