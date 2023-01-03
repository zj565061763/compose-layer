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
    debug: Boolean = false,
    onAttach: () -> Unit = {},
    onDetach: () -> Unit = {},
    content: @Composable Layer.ContentScope.() -> Unit,
) {
    val onAttachUpdated by rememberUpdatedState(onAttach)
    val onDetachUpdated by rememberUpdatedState(onDetach)

    val layer = remember {
        object : FLayer() {
            override fun onAttach() {
                super.onAttach()
                onAttachUpdated()
            }

            override fun onDetach() {
                super.onDetach()
                _layerContainer?.destroyLayer(this)
                onDetachUpdated()
            }
        }
    }

    layer.apply {
        this.isDebug = debug
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
        }
    }
}