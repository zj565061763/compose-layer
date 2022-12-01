package com.sd.lib.compose.layer

import android.util.Log
import androidx.annotation.CallSuper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.sd.lib.compose.layer.Layer.DialogBehavior
import com.sd.lib.compose.layer.Layer.Position
import kotlin.properties.Delegates

internal open class FLayer : Layer {
    protected var _layerManager: LayerManager? = null
        private set

    protected var _isAttached by mutableStateOf(false)
        private set

    private var _content: @Composable LayerContentScope.() -> Unit by mutableStateOf({ })
    private val _contentScopeImpl = LayerContentScopeImpl()

    private var _positionState: Position by mutableStateOf(Position.Center)

    private var _layerLayoutCoordinates: LayoutCoordinates? = null
    private var _contentLayoutCoordinates: LayoutCoordinates? by Delegates.observable(null) { _, _, _ ->
        onContentLayoutCoordinatesChanged()
    }

    private var _dialogBehaviorState: DialogBehavior? by mutableStateOf(DialogBehavior())

    final override val isVisibleState: Boolean
        get() = _contentScopeImpl.isVisible

    final override val positionState: Position
        get() = _positionState

    final override val dialogBehaviorState: DialogBehavior?
        get() = _dialogBehaviorState

    final override fun setContent(content: @Composable LayerContentScope.() -> Unit) {
        _content = content
    }

    override fun setPosition(position: Position) {
        _positionState = position
    }

    final override fun setDialogBehavior(block: (DialogBehavior) -> DialogBehavior?) {
        _dialogBehaviorState = block(_dialogBehaviorState ?: DialogBehavior())
    }

    @CallSuper
    override fun attach() {
        _isAttached = true
        _layerManager?.notifyLayerAttachState(this, true)
    }

    @CallSuper
    override fun detach() {
        _isAttached = false
        _layerManager?.notifyLayerAttachState(this, false)
    }

    @Composable
    override fun UpdateContainer() {
        val layerManager = checkNotNull(LocalLayerManager.current) {
            "CompositionLocal LocalLayerManager not present"
        }
        LaunchedEffect(layerManager) {
            val currentManager = _layerManager
            if (currentManager != layerManager) {
                currentManager?.detachLayer(this@FLayer)
                layerManager.attachLayer(this@FLayer)
            }
        }
    }

    /**
     * Layer被添加到[manager]
     */
    @CallSuper
    internal open fun attachToManager(manager: LayerManager) {
        _layerManager = manager
    }

    /**
     * Layer从[manager]上被移除
     */
    @CallSuper
    internal open fun detachFromManager(manager: LayerManager) {
        check(_layerManager === manager)
        detach()
        _layerManager = null
    }

    /**
     * 设置内容可见状态
     */
    protected fun setContentVisible(visible: Boolean) {
        _contentScopeImpl._isVisible = visible
    }

    /**
     * 内容布局变化回调
     */
    protected open fun onContentLayoutCoordinatesChanged() {}

    /**
     * 渲染Layer内容
     */
    @Composable
    internal open fun Content() {
        SideEffect {
            setContentVisible(_isAttached)
        }

        LayerBox(_isAttached) {
            ContentBox(modifier = Modifier.align(positionState.toAlignment()))
        }
    }

    @Composable
    protected fun LayerBox(
        isVisible: Boolean,
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit,
    ) {
        var modifier = modifier.fillMaxSize()

        if (isVisible) {
            modifier = modifier.onGloballyPositioned {
                _layerLayoutCoordinates = it
            }
            _dialogBehaviorState?.let { behavior ->
                modifier = modifier.pointerInput(behavior) {
                    detectTouchOutside(behavior)
                }
            }
        }

        Box(modifier = modifier) {
            BackgroundBox(isVisible)
            content()
        }
    }

    @Composable
    private fun BackgroundBox(
        isVisible: Boolean,
    ) {
        _dialogBehaviorState?.let { behavior ->
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(behavior.backgroundColor)
                )
            }
        }
    }

    @Composable
    protected fun ContentBox(
        modifier: Modifier = Modifier,
    ) {
        Box(
            modifier = modifier.onGloballyPositioned {
                _contentLayoutCoordinates = it
            }
        ) {
            _content.invoke(_contentScopeImpl)
        }
    }

    private suspend fun PointerInputScope.detectTouchOutside(behavior: DialogBehavior) {
        forEachGesture {
            detectTouchOutsideOnce(behavior)
        }
    }

    private suspend fun PointerInputScope.detectTouchOutsideOnce(behavior: DialogBehavior) {
        awaitPointerEventScope {
            val down = layerAwaitFirstDown(PointerEventPass.Initial)
            val downPosition = down.position

            val layerLayout = _layerLayoutCoordinates
            val contentLayout = _contentLayoutCoordinates
            if (layerLayout != null && contentLayout != null) {
                val contentRect = layerLayout.localBoundingBoxOf(contentLayout)
                if (contentRect.contains(downPosition)) {
                    // 触摸到内容区域
                } else {
                    if (behavior.cancelable && behavior.canceledOnTouchOutside) {
                        detach()
                    }
                    if (behavior.consumeTouchOutside) {
                        down.consume()
                    } else {
                        // TODO 事件穿透
                    }
                }
            }
        }
    }

    internal class LayerContentScopeImpl : LayerContentScope {
        var _isVisible by mutableStateOf(false)

        override val isVisible: Boolean
            get() = _isVisible
    }
}


internal inline fun logMsg(block: () -> String) {
    Log.i("FLayer", block())
}

private fun Position.toAlignment(): Alignment {
    return when (this) {
        Position.TopStart, Position.StartTop -> Alignment.TopStart
        Position.TopCenter -> Alignment.TopCenter
        Position.TopEnd, Position.EndTop -> Alignment.TopEnd

        Position.StartCenter -> Alignment.CenterStart
        Position.Center -> Alignment.Center
        Position.EndCenter -> Alignment.CenterEnd

        Position.BottomStart, Position.StartBottom -> Alignment.BottomStart
        Position.BottomCenter -> Alignment.BottomCenter
        Position.BottomEnd, Position.EndBottom -> Alignment.BottomEnd
    }
}

private suspend fun AwaitPointerEventScope.layerAwaitFirstDown(
    pass: PointerEventPass
): PointerInputChange {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(pass)
    } while (
        !event.changes.all { it.changedToDown() }
    )
    return event.changes[0]
}