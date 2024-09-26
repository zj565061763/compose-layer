package com.sd.lib.compose.layer

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

interface LayerState {
   /** Layer可见状态，当Layer开始进入时值为true，当Layer开始退出时值为false */
   val isVisibleState: Boolean

   /** Layer生命周期状态 */
   val lifecycleState: LayerLifecycleState
}

enum class LayerLifecycleState {
   /** 原始状态，对象刚创建出来 */
   None,

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

   /** 触摸非内容区域 */
   OnTouchOutside,
}

interface LayerContentScope

internal interface Layer : LayerState {
   /** 是否调试模式，tag:FLayer */
   var debug: Boolean

   /**
    * 按返回键是否移除Layer，true-移除；false-不移除；null-不处理返回键逻辑，默认值true
    */
   fun setDetachOnBackPress(value: Boolean?)

   /**
    * 触摸非内容区域是否移除Layer，true-移除；false-不移除；null-不处理，事件会透过背景，默认值false
    */
   fun setDetachOnTouchOutside(value: Boolean?)

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

   private var _lifecycleState by mutableStateOf(LayerLifecycleState.None)
   private var _isVisibleState by mutableStateOf(false)

   private val _layerScope = LayerScopeImpl()
   private val _contentState = mutableStateOf<(@Composable LayerContentScope.() -> Unit)>({})

   private var _detachOnBackPressState by mutableStateOf<Boolean?>(true)
   private var _detachOnTouchOutsideState by mutableStateOf<Boolean?>(false)
   private var _backgroundColorState by mutableStateOf(Color.Black.copy(alpha = 0.3f))

   private var _layerTransition: LayerTransition? = null
   private var _detachRequestCallback: ((LayerDetach) -> Unit)? = null

   /** 是否在移除的过程中被重新添加 */
   private var _attachedFromDetaching = false

   final override var debug: Boolean = false
   final override val isVisibleState: Boolean get() = _isVisibleState
   final override val lifecycleState: LayerLifecycleState get() = _lifecycleState

   final override fun setDetachOnBackPress(value: Boolean?) {
      _detachOnBackPressState = value
   }

   final override fun setDetachOnTouchOutside(value: Boolean?) {
      _detachOnTouchOutsideState = value
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

   protected open fun onAttach(container: ContainerForLayer) = Unit
   protected open fun onDetach(container: ContainerForLayer) = Unit
   protected open fun onDetached(container: ContainerForLayer) = Unit

   @Composable
   internal fun Init(
      content: @Composable LayerContentScope.() -> Unit,
   ) {
      val layerContainer = checkNotNull(LocalContainerForLayer.current) {
         "Not in LayerContainer scope."
      }
      layerContainer.initLayer(this)

      _contentState.value = content
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
      setLifecycleState(LayerLifecycleState.None)
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
      if (!_isVisibleState) {
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
               logMsg { "OnBackPress" }
               _detachRequestCallback?.invoke(LayerDetach.OnBackPress)
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
                  .let { m ->
                     if (_detachOnTouchOutsideState != null) {
                        m.pointerInput(Unit) {
                           awaitEachGesture {
                              awaitFirstDown(pass = PointerEventPass.Initial)
                              if (_detachOnTouchOutsideState == true) {
                                 logMsg { "OnTouchOutside" }
                                 _detachRequestCallback?.invoke(LayerDetach.OnTouchOutside)
                              }
                           }
                        }
                     } else {
                        m
                     }
                  }
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

   private class LayerScopeImpl : LayerContentScope
}

internal inline fun Layer.logMsg(block: () -> String) {
   if (debug) {
      Log.d("FLayer", block())
   }
}