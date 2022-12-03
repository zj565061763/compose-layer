package com.sd.lib.compose.layer

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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.sd.lib.compose.layer.Layer.*

internal open class LayerImpl : Layer {
    protected var _layerContainer: LayerContainer? = null
        private set

    protected var _isAttached = false
        private set

    private val _contentScopeImpl = ContentScopeImpl()
    private var _content: @Composable ContentScope.() -> Unit by mutableStateOf({ })

    private var _layerLayoutCoordinates: LayoutCoordinates? = null
    private var _contentLayoutCoordinates: LayoutCoordinates? = null

    private var _positionState: Position by mutableStateOf(Position.Center)
    private var _clipToBoundsState by mutableStateOf(false)
    private var _dialogBehaviorState: DialogBehavior? by mutableStateOf(DialogBehavior())

    var isDebug = false

    final override val isVisibleState: Boolean
        get() = _contentScopeImpl.isVisible

    final override val positionState: Position
        get() = _positionState

    final override val dialogBehaviorState: DialogBehavior?
        get() = _dialogBehaviorState

    final override fun setContent(content: @Composable ContentScope.() -> Unit) {
        _content = content
    }

    final override fun setPosition(position: Position) {
        logMsg(isDebug) { "${this@LayerImpl} setPosition:$position" }
        _positionState = position
    }

    final override fun setDialogBehavior(block: (DialogBehavior) -> DialogBehavior?) {
        val currentBehavior = _dialogBehaviorState ?: DialogBehavior()
        _dialogBehaviorState = block(currentBehavior)
    }

    final override fun setClipToBounds(clipToBounds: Boolean) {
        _clipToBoundsState = clipToBounds
    }

    final override fun attach() {
        val container = _layerContainer ?: return
        logMsg(isDebug) { "${this@LayerImpl} attach" }
        _isAttached = true
        container.notifyLayerAttached(this)
        onAttach()
    }

    final override fun detach() {
        if (_layerContainer == null) return
        logMsg(isDebug) { "${this@LayerImpl} detach" }
        _isAttached = false
        setContentVisible(false)
        onDetach()
    }

    protected open fun onAttach() {}
    protected open fun onDetach() {}

    /**
     * Layer被添加到[container]
     */
    internal fun onCreate(container: LayerContainer) {
        logMsg(isDebug) { "${this@LayerImpl} onCreate $container" }
        _layerContainer = container
    }

    /**
     * Layer从[container]上被移除
     */
    internal fun onDestroy(container: LayerContainer) {
        logMsg(isDebug) { "${this@LayerImpl} onDestroy $container" }
        check(_layerContainer === container)
        detach()
        _layerContainer = null
    }

    /**
     * 设置内容可见状态
     */
    protected fun setContentVisible(visible: Boolean) {
        val old = isVisibleState

        if (visible) {
            if (_isAttached) {
                _contentScopeImpl._isVisible = true
            }
        } else {
            _contentScopeImpl._isVisible = false
        }

        if (old != isVisibleState) {
            logMsg(isDebug) { "${this@LayerImpl} setContentVisible:$isVisibleState" }
        }
    }

    /**
     * 渲染Layer内容
     */
    @Composable
    internal open fun Content() {
        SideEffect {
            setContentVisible(true)
        }

        LayerBox {
            BackgroundBox()
            ContentBox(modifier = Modifier.align(positionState.toAlignment()))
        }
    }

    @Composable
    protected fun LayerBox(
        content: @Composable BoxScope.() -> Unit,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    _layerLayoutCoordinates = it
                }.let { modifier ->
                    _dialogBehaviorState?.let { behavior ->
                        modifier.pointerInput(behavior) {
                            detectTouchOutside(behavior)
                        }
                    } ?: modifier
                }
        ) {
            content()
        }
    }

    @Composable
    protected fun BackgroundBox() {
        _dialogBehaviorState?.let { behavior ->
            AnimatedVisibility(
                visible = isVisibleState,
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
            modifier = modifier
                .onGloballyPositioned {
                    _contentLayoutCoordinates = it
                    if (!_isAttached) {
                        if (it.size == IntSize.Zero) {
                            logMsg(isDebug) { "${this@LayerImpl} notifyLayerDetached" }
                            _layerContainer?.notifyLayerDetached(this@LayerImpl)
                        }
                    }
                }
                .let {
                    if (_clipToBoundsState) {
                        it.clipToBounds()
                    } else {
                        it
                    }
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

    private class ContentScopeImpl : ContentScope {
        var _isVisible by mutableStateOf(false)

        override val isVisible: Boolean
            get() = _isVisible
    }
}

private fun Position.toAlignment(): Alignment {
    return when (this) {
        Position.TopStart, Position.StartTop -> Alignment.TopStart
        Position.TopCenter, Position.Top -> Alignment.TopCenter
        Position.TopEnd, Position.EndTop -> Alignment.TopEnd

        Position.StartCenter, Position.Start -> Alignment.CenterStart
        Position.Center -> Alignment.Center
        Position.EndCenter, Position.End -> Alignment.CenterEnd

        Position.BottomStart, Position.StartBottom -> Alignment.BottomStart
        Position.BottomCenter, Position.Bottom -> Alignment.BottomCenter
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