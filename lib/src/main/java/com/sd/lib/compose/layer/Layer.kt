package com.sd.lib.compose.layer

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.CapturedAnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.zIndex

/** Layer状态 */
interface LayerState {
  /** Layer可见状态，当Layer开始进入时值为true，当Layer开始退出时值为false */
  val isVisibleState: Boolean
}

/** Layer触发移除的原因 */
enum class LayerDetach {
  /** 按返回键 */
  OnBackPress,

  /** 触摸非内容区域 */
  OnTouchOutside,
}

/** Layer内容作用域 */
interface LayerContentScope

internal abstract class LayerImpl : LayerState {
  var layerContainer: ContainerForLayer? = null
    private set

  protected var isInspectionMode: Boolean = false
    private set

  private var _attachRequested by mutableStateOf(false)
  private var _attachState by mutableStateOf(AttachState.Idle)
  private val _contentVisibleState = MutableTransitionState(false)
  private val _backgroundVisibleState = MutableTransitionState(false)

  private val _layerScope = LayerScopeImpl()
  private val _contentState = mutableStateOf<(@Composable LayerContentScope.() -> Unit)>({})
  private val _movableContent = movableContentOf { _contentState.value.invoke(_layerScope) }

  private var _detachOnBackPressState by mutableStateOf<Boolean?>(true)
  private var _detachOnTouchOutsideState by mutableStateOf<Boolean?>(true)
  private var _backgroundColorState by mutableStateOf(Color.Black.copy(alpha = 0.3f))
  private var _captureOnDetachState by mutableStateOf(true)
  private var _zIndexState by mutableFloatStateOf(0f)

  private var _layerTransition: LayerTransition? = null
  private var _detachRequestCallback: ((LayerDetach) -> Unit)? = null

  var debug: Boolean = false

  val zIndex: Float
    get() {
      return _zIndexState
    }

  protected val isAttachRequested: Boolean
    get() {
      return _attachRequested
    }

  protected val isAttached: Boolean
    get() {
      return _attachState == AttachState.Attached
    }

  protected val hasExplicitTransition: Boolean
    get() {
      return _layerTransition != null
    }

  final override val isVisibleState: Boolean
    get() {
      return _contentVisibleState.targetState
    }

  fun syncInspectionMode(value: Boolean) {
    isInspectionMode = value
  }

  /** 组合期同步声明式挂载意图，在请求移除同帧立即关闭目标可见态以冻结退场捕获帧 */
  fun syncAttach(attach: Boolean) {
    _attachRequested = attach
    if (!attach) {
      _contentVisibleState.targetState = false
      _backgroundVisibleState.targetState = false
    }
  }

  fun syncContent(content: @Composable LayerContentScope.() -> Unit) {
    _contentState.value = content
  }

  fun setDetachOnBackPress(value: Boolean?) {
    _detachOnBackPressState = value
  }

  fun setDetachOnTouchOutside(value: Boolean?) {
    _detachOnTouchOutsideState = value
  }

  fun setBackgroundColor(color: Color) {
    _backgroundColorState = color
  }

  fun setDetachRequestCallback(callback: (LayerDetach) -> Unit) {
    _detachRequestCallback = callback
  }

  fun setTransition(transition: LayerTransition?) {
    _layerTransition = transition
  }

  fun setCaptureOnDetach(value: Boolean) {
    _captureOnDetachState = value
  }

  fun setZIndex(zIndex: Float) {
    _zIndexState = zIndex
  }

  fun attach() {
    val container = checkNotNull(layerContainer) { "LayerContainer is null when attach" }
    if (_attachState != AttachState.Attached) {
      logMsg { "attach $_attachState" }
      container.attachLayer(this)
      setAttachState(AttachState.Attached)
      onAttach(container)
    }
  }

  fun detach() {
    if (_attachState == AttachState.Attached) {
      logMsg { "detach" }
      val container = checkNotNull(layerContainer) { "LayerContainer is null when detach" }
      setAttachState(AttachState.Detaching)
      setContentVisible(false)
      onDetach(container)
      checkDetachCompleted()
    }
  }

  protected open fun onAttach(container: ContainerForLayer) {}

  protected open fun onDetach(container: ContainerForLayer) {}

  protected open fun onDetached(container: ContainerForLayer) {}

  fun release() {
    layerContainer?.releaseLayer(this)
  }

  /** 初始化回调 */
  fun onInit(container: ContainerForLayer) {
    logMsg { "onInit $container" }
    check(layerContainer == null)
    layerContainer = container
  }

  /** 释放回调 */
  fun onRelease(container: ContainerForLayer) {
    logMsg { "onRelease $container" }
    check(layerContainer === container)
    detach()
    if (_attachState == AttachState.Detaching) {
      setAttachState(AttachState.Idle)
      onDetached(container)
    }
    _attachRequested = false
    layerContainer = null
  }

  private fun setAttachState(state: AttachState) {
    if (_attachState == state) return
    _attachState = state
    logMsg { "state:$_attachState" }
  }

  /** 设置内容可见状态 */
  protected fun setContentVisible(visible: Boolean) {
    val newVisible = visible && _attachRequested && isAttached
    if (_contentVisibleState.targetState == newVisible) return
    _contentVisibleState.targetState = newVisible
    _backgroundVisibleState.targetState = newVisible
    logMsg { "setContentVisible:$newVisible" }
  }

  /** 内容是否已完全处于未显示且无动画的静止状态 */
  protected fun isContentExitIdle(): Boolean {
    return !_contentVisibleState.targetState &&
      !_contentVisibleState.currentState &&
      _contentVisibleState.isIdle
  }

  private fun isExitIdle(): Boolean {
    return isContentExitIdle() &&
      !_backgroundVisibleState.targetState &&
      !_backgroundVisibleState.currentState &&
      _backgroundVisibleState.isIdle
  }

  /** 检查退场动画是否已结束并执行容器移除 */
  private fun checkDetachCompleted() {
    if (_attachState != AttachState.Detaching) return
    if (!isExitIdle()) return

    val container = checkNotNull(layerContainer)
    if (container.detachLayer(this)) {
      logMsg { "detachLayer" }
      setAttachState(AttachState.Idle)
      onDetached(container)
    }
  }

  /** 渲染Layer内容 */
  @Composable
  fun Content() {
    if (_attachState == AttachState.Detaching) {
      LaunchedEffect(this) {
        snapshotFlow { isExitIdle() }
          .collect { idle ->
            if (idle) {
              checkDetachCompleted()
            }
          }
      }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .zIndex(_zIndexState),
    ) {
      HandleOnTouchOutside()
      LayerContent()
    }
  }

  @Composable
  protected abstract fun getLayerTransition(transition: LayerTransition?): LayerTransition

  @Composable
  protected abstract fun BoxScope.LayerContent()

  @Composable
  protected fun BackgroundBox(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
      Box(
        modifier = modifier
          .fillMaxSize()
          .drawBehind {
            if (isVisibleState) {
              drawRect(_backgroundColorState)
            }
          },
      )
    } else {
      AnimatedVisibility(
        modifier = modifier.fillMaxSize(),
        visibleState = _backgroundVisibleState,
        enter = LayerTransition.Fade.enter,
        exit = LayerTransition.Fade.exit,
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(_backgroundColorState),
        )
      }
    }
  }

  @Composable
  protected fun ContentBox(modifier: Modifier = Modifier) {
    Box(
      modifier = modifier
        .clipToBounds()
        .pointerInput(Unit) { detectTapGestures() },
    ) {
      if (LocalInspectionMode.current) {
        if (_attachRequested && isAttached) {
          RawContent()
        }
      } else {
        val transition = getLayerTransition(_layerTransition)
        if (_captureOnDetachState) {
          CapturedAnimatedVisibility(
            visibleState = _contentVisibleState,
            enter = transition.enter,
            exit = transition.exit,
          ) {
            RawContent()
          }
        } else {
          AnimatedVisibility(
            visibleState = _contentVisibleState,
            enter = transition.enter,
            exit = transition.exit,
          ) {
            RawContent()
          }
        }
      }
    }
  }

  @Composable
  protected fun RawContent() {
    _movableContent()
  }

  /** 触摸非内容区域逻辑 */
  @Composable
  private fun BoxScope.HandleOnTouchOutside() {
    if (!isVisibleState) return
    val detachOnTouchOutside = _detachOnTouchOutsideState ?: return
    Box(
      modifier = Modifier
        .matchParentSize()
        .pointerInput(detachOnTouchOutside) {
          detectTapGestures(
            onPress = {
              if (detachOnTouchOutside) {
                requestDetach(LayerDetach.OnTouchOutside)
              }
            },
          )
        },
    )
  }

  /** 是否需要拦截返回键 */
  fun shouldHandleBackPress(): Boolean {
    return isVisibleState && (_detachOnBackPressState != null)
  }

  /** 处理返回键事件 */
  fun handleBackPress() {
    if (!isVisibleState) return
    if (_detachOnBackPressState == true) {
      requestDetach(LayerDetach.OnBackPress)
    }
  }

  private fun requestDetach(layerDetach: LayerDetach) {
    logMsg { "requestDetach:$layerDetach" }
    _detachRequestCallback?.invoke(layerDetach)
  }

  private enum class AttachState {
    Idle,
    Attached,
    Detaching,
  }

  private class LayerScopeImpl : LayerContentScope
}

internal inline fun LayerImpl.logMsg(block: () -> String) {
  if (debug) {
    Log.d("FLayer", Snapshot.withoutReadObservation(block))
  }
}
