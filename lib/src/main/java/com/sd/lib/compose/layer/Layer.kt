package com.sd.lib.compose.layer

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.CallSuper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex

interface LayerState {
  /** Layer可见状态，当Layer开始进入时值为true，当Layer开始退出时值为false */
  val isVisibleState: Boolean

  /** Layer生命周期状态 */
  val lifecycleState: LayerLifecycleState
}

enum class LayerLifecycleState {
  /** 对象刚创建出来 */
  Created,

  /** 已经初始化完毕，可以被添加到容器 */
  Initialized,

  /** 即将被移除 */
  Detaching,

  /** 已经添加到容器 */
  Attached,
}

enum class LayerDetach {
  /** 按返回键 */
  OnBackPress,

  /** 触摸背景区域 */
  OnTouchBackground,
}

interface LayerContentScope

internal interface Layer : LayerState {
  /** 是否调试模式，tag:FLayer */
  var debug: Boolean

  /** [Modifier.zIndex] */
  val zIndexState: Float

  /**
   * 按返回键是否请求移除Layer，true-请求移除；false-不请求移除；null-不处理返回键逻辑，默认true
   */
  fun setDetachOnBackPress(value: Boolean?)

  /**
   * 触摸背景区域是否请求移除Layer，true-请求移除；false-不请求移除；null-不处理，事件会透过背景，默认false
   */
  fun setDetachOnTouchBackground(value: Boolean?)

  /**
   * 背景颜色
   */
  fun setBackgroundColor(color: Color)

  /**
   * 设置移除请求回调
   */
  fun setDetachRequestCallback(callback: (LayerDetach) -> Unit)

  /**
   * 动画（非响应式）
   */
  fun setTransition(transition: LayerTransition?)

  /**
   * [Modifier.zIndex]
   */
  fun setZIndex(zIndex: Float)

  /**
   * 添加到容器
   */
  fun attach()

  /**
   * 从容器上移除
   */
  fun detach()
}

internal fun Layer.toLayerState(): LayerState = InternalLayerState(this)
private class InternalLayerState(layer: Layer) : LayerState by layer

//---------- Impl ----------

internal abstract class LayerImpl : Layer {
  internal var layerContainer: ContainerForLayer? = null
    private set

  private var _lifecycleState by mutableStateOf(LayerLifecycleState.Created)
  private var _isVisibleState by mutableStateOf(false)

  private val _layerScope = LayerScopeImpl()
  private val _contentState = mutableStateOf<(@Composable LayerContentScope.() -> Unit)>({})

  private var _detachOnBackPressState by mutableStateOf<Boolean?>(true)
  private var _detachOnTouchBackgroundState by mutableStateOf<Boolean?>(false)
  private var _backgroundColorState by mutableStateOf(Color.Black.copy(alpha = 0.3f))
  private var _zIndexState by mutableFloatStateOf(0f)

  private var _layerTransition: LayerTransition? = null
  private var _detachRequestCallback: ((LayerDetach) -> Unit)? = null

  /** 是否在移除的过程中被重新添加 */
  private var _attachedFromDetaching = false

  final override var debug: Boolean = false
  final override val zIndexState: Float get() = _zIndexState
  final override val isVisibleState: Boolean get() = _isVisibleState
  final override val lifecycleState: LayerLifecycleState get() = _lifecycleState

  final override fun setDetachOnBackPress(value: Boolean?) {
    _detachOnBackPressState = value
  }

  final override fun setDetachOnTouchBackground(value: Boolean?) {
    _detachOnTouchBackgroundState = value
  }

  final override fun setBackgroundColor(color: Color) {
    _backgroundColorState = color
  }

  final override fun setDetachRequestCallback(callback: (LayerDetach) -> Unit) {
    _detachRequestCallback = callback
  }

  final override fun setTransition(transition: LayerTransition?) {
    _layerTransition = transition
  }

  final override fun setZIndex(zIndex: Float) {
    _zIndexState = zIndex
  }

  final override fun attach() {
    when (val state = _lifecycleState) {
      LayerLifecycleState.Initialized,
      LayerLifecycleState.Detaching,
        -> {
        val container = checkNotNull(layerContainer) { "LayerContainer is null when attach" }
        logMsg { "attach" }
        container.attachLayer(this)
        setLifecycleState(LayerLifecycleState.Attached)
        onAttach(container)
      }
      else -> {
        logMsg { "attach ignored with state:$state" }
      }
    }
  }

  final override fun detach() {
    if (_lifecycleState == LayerLifecycleState.Attached) {
      val container = checkNotNull(layerContainer) { "LayerContainer is null when detach" }
      logMsg { "detach" }
      setLifecycleState(LayerLifecycleState.Detaching)
      setContentVisible(false)
      onDetach(container)
    }
  }

  @CallSuper
  protected open fun onAttach(container: ContainerForLayer) {
    container.registerContainerLayoutCallback(_containerLayoutCallback)
  }

  @CallSuper
  protected open fun onDetach(container: ContainerForLayer) {
    container.unregisterContainerLayoutCallback(_containerLayoutCallback)
  }

  protected open fun onDetached(container: ContainerForLayer) = Unit

  /** 容器布局信息 */
  private var _containerLayout: LayoutCoordinates? = null
  /** 监听容器布局信息 */
  private val _containerLayoutCallback: LayoutCoordinatesCallback = { layoutCoordinates ->
    _containerLayout = layoutCoordinates
    onContainerLayoutCallback(layoutCoordinates)
  }

  /** 容器布局信息回调 */
  protected open fun onContainerLayoutCallback(layoutCoordinates: LayoutCoordinates?) = Unit

  @Composable
  internal fun Init(
    content: @Composable LayerContentScope.() -> Unit,
  ) {
    val container = LocalContainerForLayer.current
    if (container == null) {
      if (LocalInspectionMode.current) return
      else error("Not in LayerContainer scope.")
    } else {
      container.initLayer(this)
      _contentState.value = content
    }
  }

  internal fun destroy() {
    layerContainer?.destroyLayer(this)
  }

  /**
   * Layer被添加到[container]
   */
  internal fun onInit(container: ContainerForLayer) {
    logMsg { "onInit $container" }
    check(layerContainer == null)
    layerContainer = container
    setLifecycleState(LayerLifecycleState.Initialized)
  }

  /**
   * Layer从[container]上被移除
   */
  internal fun onDestroy(container: ContainerForLayer) {
    logMsg { "onDestroy $container" }
    check(layerContainer === container)
    detach()
    layerContainer = null
    setLifecycleState(LayerLifecycleState.Created)
  }

  private fun setLifecycleState(state: LayerLifecycleState) {
    val oldState = _lifecycleState
    if (oldState == state) return

    _lifecycleState = state
    logMsg { "state:$_lifecycleState" }

    if (state == LayerLifecycleState.Attached) {
      if (oldState == LayerLifecycleState.Detaching) {
        setAttachedFromDetaching(true)
      }
    } else {
      setAttachedFromDetaching(false)
    }
  }

  /**
   * 设置内容可见状态
   */
  protected fun setContentVisible(visible: Boolean) {
    val oldVisible = _isVisibleState
    if (oldVisible == visible) return

    if (visible) {
      if (_lifecycleState == LayerLifecycleState.Attached) {
        _isVisibleState = true
      }
    } else {
      _isVisibleState = false
    }

    if (oldVisible != _isVisibleState) {
      logMsg { "setContentVisible:$_isVisibleState" }
    }

    if (_isVisibleState) {
      setAttachedFromDetaching(false)
    }
  }

  private fun setAttachedFromDetaching(value: Boolean) {
    if (_attachedFromDetaching != value) {
      _attachedFromDetaching = value
      logMsg { "setAttachedFromDetaching:$value" }
    }
  }

  private fun handleContentZeroSize() {
    logMsg { "handleContentZeroSize isVisible:$_isVisibleState state:$_lifecycleState" }
    if (_isVisibleState) return
    when (_lifecycleState) {
      LayerLifecycleState.Attached -> {
        if (_attachedFromDetaching) {
          logMsg { "attached from detaching set content visible" }
          setContentVisible(true)
        }
      }
      LayerLifecycleState.Detaching -> {
        val container = checkNotNull(layerContainer)
        if (container.detachLayer(this@LayerImpl)) {
          logMsg { "detachLayer" }
          setLifecycleState(LayerLifecycleState.Initialized)
          onDetached(container)
        }
      }
      else -> {}
    }
  }

  /**
   * 渲染Layer内容
   */
  @Composable
  fun Content() {
    LayerContent()
    if (_isVisibleState && _detachOnBackPressState != null) {
      BackHandler {
        if (_detachOnBackPressState == true) {
          requestDetach(LayerDetach.OnBackPress)
        }
      }
    }
  }

  @Composable
  abstract fun LayerContent()

  @Composable
  protected abstract fun getLayerTransition(transition: LayerTransition?): LayerTransition

  @Composable
  protected fun ContentBox(modifier: Modifier = Modifier) {
    Box(
      modifier = modifier
        .onGloballyPositioned {
          _contentLayout = it
          if (it.size == IntSize.Zero) {
            handleContentZeroSize()
          }
        }
        .clipToBounds()
    ) {
      AnimatedContent()
    }
  }

  @Composable
  protected fun BackgroundBox() {
    Box {
      AnimatedVisibility(
        visible = _isVisibleState,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(_backgroundColorState)
            .handleDetachOnTouchBackground(_detachOnTouchBackgroundState)
        )
      }
    }
  }

  @Composable
  protected fun RawContent() {
    _contentState.value.invoke(_layerScope)
  }

  @Composable
  private fun AnimatedContent() {
    val transition = getLayerTransition(_layerTransition)
    AnimatedVisibility(
      visible = _isVisibleState,
      enter = transition.enter,
      exit = transition.exit,
    ) {
      _contentState.value.invoke(_layerScope)
    }
  }

  /** 内容布局 */
  private var _contentLayout: LayoutCoordinates? = null
  /** 背景布局 */
  private var _backgroundLayout: LayoutCoordinates? = null

  /** 监听触摸背景区域 */
  private fun Modifier.handleDetachOnTouchBackground(state: Boolean?): Modifier {
    if (state == null) return this
    return this
      .onGloballyPositioned { _backgroundLayout = it }
      .pointerInput(state) {
        detectTapGestures(onPress = { offset ->
          if (state) {
            handleOnTouchBackground(offset)
          }
        })
      }
  }

  /** 处理触摸背景区域逻辑 */
  private fun handleOnTouchBackground(offset: Offset) {
    val containerLayout = _containerLayout
    if (containerLayout == null || !containerLayout.isAttached) {
      logMsg { "handleOnTouchBackground _containerLayout is null or detached" }
      return
    }

    val backgroundLayout = _backgroundLayout
    if (backgroundLayout == null) {
      logMsg { "handleOnTouchBackground _backgroundLayout is null" }
      return
    }

    val contentLayout = _contentLayout
    if (contentLayout == null) {
      logMsg { "handleOnTouchBackground _contentLayout is null" }
      return
    }

    val offsetInContainer = containerLayout.localPositionOf(backgroundLayout, offset)
    val contentBoundsInContainer = containerLayout.localBoundingBoxOf(contentLayout)
    logMsg { "handleOnTouchBackground $offset -> $offsetInContainer $contentBoundsInContainer" }

    if (contentBoundsInContainer.contains(offsetInContainer)) {
      // 触摸内容区域，不处理
    } else {
      requestDetach(LayerDetach.OnTouchBackground)
    }
  }

  private fun requestDetach(layerDetach: LayerDetach) {
    logMsg { "requestDetach:$layerDetach" }
    _detachRequestCallback?.invoke(layerDetach)
  }

  private class LayerScopeImpl : LayerContentScope
}

internal inline fun Layer.logMsg(block: () -> String) {
  if (debug) {
    Log.d("FLayer", block())
  }
}