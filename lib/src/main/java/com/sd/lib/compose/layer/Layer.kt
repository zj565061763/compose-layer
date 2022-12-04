package com.sd.lib.compose.layer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.sd.lib.compose.layer.Layer.ContentScope
import com.sd.lib.compose.layer.Layer.Position

interface Layer {
    /**
     * 当前Layer是否可见
     */
    val isVisibleState: Boolean

    /**
     * 位置
     */
    val positionState: Position

    /**
     * 窗口行为
     */
    val dialogBehavior: DialogBehavior

    /**
     * 设置内容
     */
    fun setContent(content: @Composable ContentScope.() -> Unit)

    /**
     * 设置对齐的位置
     */
    fun setPosition(position: Position)

    /**
     * 是否裁剪内容区域，默认false
     */
    fun setClipToBounds(clipToBounds: Boolean)

    /**
     * 添加到容器
     */
    fun attach()

    /**
     * 从容器上移除
     */
    fun detach()

    enum class Position {
        /** 顶部开始方向对齐 */
        TopStart,
        /** 顶部中间对齐 */
        TopCenter,
        /** 顶部结束方向对齐 */
        TopEnd,
        /** 顶部对齐，不计算x坐标，默认x坐标为0 */
        Top,

        /** 底部开始方向对齐 */
        BottomStart,
        /** 底部中间对齐 */
        BottomCenter,
        /** 底部结束方向对齐 */
        BottomEnd,
        /** 底部对齐，不计算x坐标，默认x坐标为0 */
        Bottom,

        /** 开始方向顶部对齐 */
        StartTop,
        /** 开始方向中间对齐 */
        StartCenter,
        /** 开始方向底部对齐 */
        StartBottom,
        /** 开始方向对齐，不计算y坐标，默认y坐标为0 */
        Start,

        /** 结束方向顶部对齐 */
        EndTop,
        /** 结束方向中间对齐 */
        EndCenter,
        /** 结束方向底部对齐 */
        EndBottom,
        /** 结束方向对齐，不计算y坐标，默认y坐标为0 */
        End,

        /** 中间对齐 */
        Center,
    }

    interface ContentScope {
        /**
         * 内容是否可见
         */
        val isVisible: Boolean
    }
}

class DialogBehavior {
    private var _enabled by mutableStateOf(true)
    private var _cancelable = true
    private var _canceledOnTouchOutside = true
    private var _consumeTouchOutside = true
    private var _backgroundColor by mutableStateOf(Color.Black.copy(alpha = 0.3f))

    /**
     * 窗口行为是否开启，默认true
     */
    val enabled: Boolean get() = _enabled

    /**
     * 按返回键或者[canceledOnTouchOutside]为true的时候，是否可以关闭，默认true
     */
    val cancelable: Boolean get() = _cancelable

    /**
     * 触摸到非内容区域是否关闭，默认true
     */
    val canceledOnTouchOutside: Boolean get() = _canceledOnTouchOutside

    /**
     * 是否消费掉非内容区域的触摸事件，消费掉之后触摸事件不会透过Layer，默认true
     */
    val consumeTouchOutside: Boolean get() = _consumeTouchOutside

    /**
     * 背景颜色
     */
    val backgroundColor: Color get() = _backgroundColor

    /**
     * [enabled]
     */
    fun setEnabled(value: Boolean) = apply {
        _enabled = value
    }

    /**
     * [cancelable]
     */
    fun setCancelable(value: Boolean) = apply {
        _cancelable = value
    }

    /**
     * [canceledOnTouchOutside]
     */
    fun setCanceledOnTouchOutside(value: Boolean) = apply {
        if (value && !_cancelable) {
            _cancelable = true
        }
        _canceledOnTouchOutside = value
    }

    /**
     * [consumeTouchOutside]
     */
    fun setConsumeTouchOutside(value: Boolean) = apply {
        _consumeTouchOutside = value
    }

    /**
     * [backgroundColor]
     */
    fun setBackgroundColor(value: Color) = apply {
        _backgroundColor = value
    }
}

//---------- Impl ----------

internal open class LayerImpl : Layer {
    protected var _layerContainer: LayerContainer? = null
        private set

    private var _isAttached = false

    private val _contentScopeImpl = ContentScopeImpl()
    private var _content: @Composable ContentScope.() -> Unit by mutableStateOf({ })

    private var _layerLayoutCoordinates: LayoutCoordinates? = null
    private var _contentLayoutCoordinates: LayoutCoordinates? = null

    private var _positionState: Position by mutableStateOf(Position.Center)
    private var _clipToBoundsState by mutableStateOf(false)

    var isDebug = false

    final override val isVisibleState: Boolean
        get() = _contentScopeImpl.isVisible

    final override val positionState: Position
        get() = _positionState

    final override val dialogBehavior: DialogBehavior = DialogBehavior()

    final override fun setContent(content: @Composable ContentScope.() -> Unit) {
        _content = content
    }

    final override fun setPosition(position: Position) {
        logMsg(isDebug) { "${this@LayerImpl} setPosition:$position" }
        _positionState = position
    }

    final override fun setClipToBounds(clipToBounds: Boolean) {
        _clipToBoundsState = clipToBounds
    }

    final override fun attach() {
        val container = _layerContainer ?: return
        logMsg(isDebug) { "${this@LayerImpl} attach" }
        _isAttached = true
        container.attachLayer(this)
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
                }
        ) {
            content()
        }
    }

    @Composable
    protected fun BackgroundBox() {
        val behavior = dialogBehavior
        if (behavior.enabled) {
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
                            logMsg(isDebug) { "${this@LayerImpl} detachLayer" }
                            _layerContainer?.detachLayer(this@LayerImpl)
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

    internal fun processDownEvent(event: PointerInputChange) {
        val behavior = dialogBehavior
        if (!behavior.enabled) return

        val layerLayout = _layerLayoutCoordinates ?: return
        val contentLayout = _contentLayoutCoordinates ?: return

        val contentRect = layerLayout.localBoundingBoxOf(contentLayout)
        if (contentRect.contains(event.position)) {
            // 触摸到内容区域
        } else {
            if (behavior.cancelable && behavior.canceledOnTouchOutside) {
                detach()
            }
            if (behavior.consumeTouchOutside) {
                event.consume()
            } else {
                // 事件穿透
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