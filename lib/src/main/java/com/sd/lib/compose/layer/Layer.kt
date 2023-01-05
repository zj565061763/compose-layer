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
import com.sd.lib.compose.layer.Layer.Position

interface Layer {
    var isDebug: Boolean

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
     * Z坐标
     */
    val zIndexState: Float?

    /**
     * 设置对齐的位置
     */
    fun setPosition(position: Position)

    /**
     * 是否裁剪内容区域，默认false
     */
    fun setClipToBounds(clipToBounds: Boolean)

    /**
     * 设置Z坐标
     */
    fun setZIndex(index: Float?)

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
}

class DialogBehavior {
    private var _enabledState by mutableStateOf(true)
    private var _cancelable = true
    private var _canceledOnTouchOutside = true
    private var _consumeTouchOutside = true
    private var _backgroundColorState by mutableStateOf(Color.Black.copy(alpha = 0.3f))

    /**
     * 窗口行为是否开启，默认true
     */
    val enabledState: Boolean get() = _enabledState

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
    val backgroundColorState: Color get() = _backgroundColorState

    /**
     * [enabledState]
     */
    fun setEnabled(value: Boolean) = apply {
        _enabledState = value
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
     * [backgroundColorState]
     */
    fun setBackgroundColor(value: Color) = apply {
        _backgroundColorState = value
    }
}

interface LayerContentScope {
    val layer: Layer
}

//---------- Impl ----------

internal open class LayerImpl : Layer {
    internal var _layerContainer: LayerContainer? = null
        private set

    private var _isAttached = false
    private var _isVisibleState by mutableStateOf(false)

    private val _contentScopeImpl = ContentScopeImpl()
    private val _contentState = mutableStateOf<(@Composable LayerContentScope.() -> Unit)?>(null)

    private var _layerLayoutCoordinates: LayoutCoordinates? = null
    private var _contentLayoutCoordinates: LayoutCoordinates? = null

    private var _positionState by mutableStateOf(Position.Center)
    private var _clipToBoundsState by mutableStateOf(false)
    private var _zIndex by mutableStateOf<Float?>(null)

    final override var isDebug: Boolean = false
    final override val isVisibleState: Boolean get() = _isVisibleState
    final override val positionState: Position get() = _positionState
    final override val dialogBehavior: DialogBehavior = DialogBehavior()
    final override val zIndexState: Float? get() = _zIndex

    final override fun setPosition(position: Position) {
        _positionState = position
    }

    final override fun setClipToBounds(clipToBounds: Boolean) {
        _clipToBoundsState = clipToBounds
    }

    final override fun setZIndex(index: Float?) {
        _zIndex = index
    }

    final override fun attach() {
        val container = _layerContainer ?: return
        if (_isAttached) return
        logMsg(isDebug) { "${this@LayerImpl} attach" }
        _isAttached = true
        container.attachLayer(this)
        onAttachInternal()
        onAttach()
    }

    final override fun detach() {
        if (_layerContainer == null) return
        if (!_isAttached) return
        logMsg(isDebug) { "${this@LayerImpl} detach" }
        _isAttached = false
        setContentVisible(false)
        onDetachInternal()
    }

    internal open fun onAttachInternal() {}
    internal open fun onDetachInternal() {}

    protected open fun onAttach() {}
    protected open fun onDetach() {}

    @Composable
    internal fun Init() {
        val layerContainer = checkNotNull(LocalLayerContainer.current) {
            "CompositionLocal LocalLayerContainer not present"
        }
        layerContainer.initLayer(this)
    }

    internal fun setContent(content: @Composable LayerContentScope.() -> Unit) {
        _contentState.value = content
    }

    internal fun destroy() {
        _layerContainer?.destroyLayer(this)
    }

    /**
     * Layer被添加到[container]
     */
    internal fun onInit(container: LayerContainer) {
        logMsg(isDebug) { "${this@LayerImpl} onInit $container" }
        check(_layerContainer == null)
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
    internal fun setContentVisible(visible: Boolean) {
        val old = isVisibleState

        if (visible) {
            if (_isAttached) {
                _isVisibleState = true
            }
        } else {
            _isVisibleState = false
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
    internal fun LayerBox(
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
    internal fun BackgroundBox() {
        val behavior = dialogBehavior
        if (behavior.enabledState) {
            AnimatedVisibility(
                visible = isVisibleState,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(behavior.backgroundColorState)
                )
            }
        }
    }

    @Composable
    internal fun ContentBox(
        modifier: Modifier = Modifier,
    ) {
        Box(
            modifier = modifier
                .onGloballyPositioned {
                    _contentLayoutCoordinates = it
                    if (it.size == IntSize.Zero) {
                        logMsg(isDebug) { "${this@LayerImpl} ContentBox zero size isAttached:$_isAttached isVisible:$isVisibleState" }
                        if (!_isAttached && !isVisibleState) {
                            logMsg(isDebug) { "${this@LayerImpl} detachLayer" }
                            if (_layerContainer?.detachLayer(this@LayerImpl) == true) {
                                logMsg(isDebug) { "${this@LayerImpl} onDetach" }
                                onDetach()
                            }
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
            _contentState.value?.invoke(_contentScopeImpl)
        }
    }

    internal fun processDownEvent(event: PointerInputChange) {
        val behavior = dialogBehavior
        if (!behavior.enabledState) return

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

    private inner class ContentScopeImpl : LayerContentScope {
        override val layer: Layer get() = this@LayerImpl
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