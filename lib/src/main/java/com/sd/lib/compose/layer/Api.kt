package com.sd.lib.compose.layer

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset

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
    onDetach: () -> Unit = {},
    content: @Composable Layer.ContentScope.() -> Unit,
) {
    Layer(
        factory = { FLayer() },
        apply = {},

        dialogEnable = dialogEnable,
        dialogCancelable = dialogCancelable,
        dialogCanceledOnTouchOutside = dialogCanceledOnTouchOutside,
        dialogConsumeTouchOutside = dialogConsumeTouchOutside,
        dialogBackgroundColor = dialogBackgroundColor,
        position = position,
        clipToBounds = clipToBounds,
        zIndex = zIndex,
        debug = debug,
        onDetach = onDetach,
        content = content,
    )
}

@Composable
fun FTargetLayer(
    dialogEnable: Boolean = true,
    dialogCancelable: Boolean = true,
    dialogCanceledOnTouchOutside: Boolean = true,
    dialogConsumeTouchOutside: Boolean = true,
    dialogBackgroundColor: Color = Color.Black.copy(alpha = 0.3f),
    position: Layer.Position = Layer.Position.Center,
    clipToBounds: Boolean = false,
    zIndex: Float? = null,
    target: String? = null,
    targetOffset: IntOffset? = null,
    offsetTransform: OffsetTransform? = null,
    fixOverflowDirection: Directions? = null,
    clipBackgroundDirection: Directions? = null,
    debug: Boolean = false,
    onDetach: () -> Unit = {},
    content: @Composable Layer.ContentScope.() -> Unit,
) {
    Layer(
        factory = { FTargetLayer() },
        apply = {
            it.setTarget(target)
            it.setTargetOffset(targetOffset)
            it.setOffsetTransform(offsetTransform)
            it.setFixOverflowDirection(fixOverflowDirection)
            it.setClipBackgroundDirection(clipBackgroundDirection)
        },

        dialogEnable = dialogEnable,
        dialogCancelable = dialogCancelable,
        dialogCanceledOnTouchOutside = dialogCanceledOnTouchOutside,
        dialogConsumeTouchOutside = dialogConsumeTouchOutside,
        dialogBackgroundColor = dialogBackgroundColor,
        position = position,
        clipToBounds = clipToBounds,
        zIndex = zIndex,
        debug = debug,
        onDetach = onDetach,
        content = content,
    )
}

@Composable
private fun <T : FLayer> Layer(
    factory: () -> T,
    apply: (T) -> Unit,

    dialogEnable: Boolean = true,
    dialogCancelable: Boolean = true,
    dialogCanceledOnTouchOutside: Boolean = true,
    dialogConsumeTouchOutside: Boolean = true,
    dialogBackgroundColor: Color = Color.Black.copy(alpha = 0.3f),
    position: Layer.Position = Layer.Position.Center,
    clipToBounds: Boolean = false,
    zIndex: Float? = null,
    debug: Boolean = false,
    onDetach: () -> Unit = {},
    content: @Composable Layer.ContentScope.() -> Unit,
) {
    val onDetachUpdated by rememberUpdatedState(onDetach)

    val layer = remember {
        factory().apply {
            attachCallback = object : AttachCallback {
                override fun onAttach() {
                }

                override fun onDetach() {
                    _layerContainer?.destroyLayer(this@apply)
                    onDetachUpdated()
                }
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
        apply(this)
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