package com.sd.lib.compose.layer

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.sd.lib.compose.layer.Layer.Position
import java.util.concurrent.atomic.AtomicLong

interface Layer {
   /** 是否调试模式，tag:FLayer */
   var isDebug: Boolean

   /** 当前Layer是否可见 */
   val isVisibleState: Boolean

   /** 按返回键是否移除Layer，true-移除，false-不移除，null-不处理返回键逻辑，默认-true */
   val dismissOnBackPressState: Boolean?

   /** 触摸非内容区域是否移除Layer，true-移除，false-不移除，null-不处理，事件会透过背景，默认-false */
   val dismissOnTouchOutsideState: Boolean?

   /** 显示位置 */
   val positionState: Position

   /** 背景颜色 */
   val backgroundColorState: Color

   /**
    * [dismissOnBackPressState]
    */
   fun setDismissOnBackPress(value: Boolean?)

   /**
    * [dismissOnTouchOutsideState]
    */
   fun setDismissOnTouchOutside(value: Boolean?)

   /**
    * [position]
    */
   fun setPosition(position: Position)

   /**
    * [backgroundColorState]
    */
   fun setBackgroundColor(color: Color)

   /**
    * 是否裁剪内容区域，默认true
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
}

interface LayerContentScope {
   val layer: Layer
}

interface LayerDisplayScope : LayerContentScope {
   @Composable
   fun Content()
}

//---------- Impl ----------

private val LayerID = AtomicLong(0L)

internal open class LayerImpl : Layer {
   internal val id: Any = LayerID.incrementAndGet()

   internal var layerContainer: ContainerForLayer? = null
      private set

   private var _isAttached = false
   private var _isVisibleState by mutableStateOf(false)

   private val _displayScope = LayerDisplayScopeImpl()
   private val _contentState = mutableStateOf<(@Composable LayerContentScope.() -> Unit)?>(null)
   private val _displayState = mutableStateOf<(@Composable LayerDisplayScope.() -> Unit)>({ Content() })

   private var _dismissOnBackPressState by mutableStateOf<Boolean?>(true)
   private var _dismissOnTouchOutsideState by mutableStateOf<Boolean?>(false)

   private var _positionState by mutableStateOf(Position.Center)
   private var _backgroundColorState by mutableStateOf(Color.Black.copy(alpha = 0.3f))
   private var _clipToBoundsState by mutableStateOf(true)

   final override var isDebug: Boolean = false
   final override val isVisibleState: Boolean get() = _isVisibleState
   final override val dismissOnBackPressState: Boolean? get() = _dismissOnBackPressState
   final override val dismissOnTouchOutsideState: Boolean? get() = _dismissOnTouchOutsideState
   final override val positionState: Position get() = _positionState
   final override val backgroundColorState: Color get() = _backgroundColorState

   final override fun setPosition(position: Position) {
      _positionState = position
   }

   override fun setBackgroundColor(color: Color) {
      _backgroundColorState = color
   }

   override fun setDismissOnBackPress(value: Boolean?) {
      _dismissOnBackPressState = value
   }

   override fun setDismissOnTouchOutside(value: Boolean?) {
      _dismissOnTouchOutsideState = value
   }

   final override fun setClipToBounds(clipToBounds: Boolean) {
      _clipToBoundsState = clipToBounds
   }

   final override fun attach() {
      if (_isAttached) return
      val container = layerContainer ?: return

      logMsg { "attach" }
      _isAttached = true

      container.attachLayer(this)
      onAttach()
   }

   final override fun detach() {
      if (!_isAttached) return

      logMsg { "detach" }
      _isAttached = false

      setContentVisible(false)
      onDetach()
   }

   protected open fun onAttach() {}
   protected open fun onDetach() {}

   @Composable
   internal fun Init(
      content: @Composable LayerContentScope.() -> Unit,
      display: @Composable LayerDisplayScope.() -> Unit,
   ) {
      val layerContainer = checkNotNull(LocalContainerForLayer.current) {
         "Not in LayerContainer scope."
      }
      layerContainer.initLayer(this)

      _contentState.value = content
      _displayState.value = display
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
   }

   /**
    * Layer从[container]上被移除
    */
   internal fun onDestroy(container: ContainerForLayer) {
      logMsg { "onDestroy $container" }
      check(layerContainer === container)
      detach()
      layerContainer = null
   }

   /**
    * 设置内容可见状态
    */
   protected fun setContentVisible(visible: Boolean) {
      val old = isVisibleState

      if (visible) {
         if (_isAttached) {
            _isVisibleState = true
         }
      } else {
         _isVisibleState = false
      }

      if (old != isVisibleState) {
         logMsg { "setContentVisible:$isVisibleState" }
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
   protected fun LayerBox(content: @Composable BoxScope.() -> Unit) {
      Box(modifier = Modifier.fillMaxSize()) {
         content()
      }
   }

   @Composable
   protected fun ContentBox(modifier: Modifier = Modifier) {
      Box(
         modifier = modifier
            .onGloballyPositioned {
               if (it.size == IntSize.Zero) {
                  logMsg { "ContentBox zero size isAttached:$_isAttached isVisible:$isVisibleState" }
                  if (!_isAttached && !isVisibleState) {
                     logMsg { "detachLayer" }
                     layerContainer?.detachLayer(this@LayerImpl)
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
         _displayState.value.invoke(_displayScope)
      }
   }

   @Composable
   protected fun BackgroundBox() {
      Box {
         AnimatedVisibility(
            visible = isVisibleState,
            enter = fadeIn(),
            exit = fadeOut(),
         ) {
            Box(
               modifier = Modifier
                  .fillMaxSize()
                  .background(backgroundColorState)
                  .let {
                     if (dismissOnTouchOutsideState != null) {
                        it.pointerInput(Unit) {
                           awaitEachGesture {
                              awaitFirstDown(pass = PointerEventPass.Initial)
                              if (dismissOnTouchOutsideState == true) {
                                 logMsg { "cancel touch background" }
                                 detach()
                              }
                           }
                        }
                     } else {
                        it
                     }
                  }
            )
         }
      }
   }

   private inner class LayerDisplayScopeImpl : LayerDisplayScope {
      @Composable
      override fun Content() {
         _contentState.value?.invoke(this@LayerDisplayScopeImpl)
      }

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

internal inline fun Layer.logMsg(block: () -> String) {
   if (isDebug) {
      Log.i("FLayer", "$this ${block()}")
   }
}