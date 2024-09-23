package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * 显示Layer的容器
 */
@Composable
fun LayerContainer(
   modifier: Modifier = Modifier,
   content: @Composable () -> Unit,
) {
   val container = remember { newLayerContainer() }

   DisposableEffect(container) {
      onDispose {
         container.destroy()
      }
   }

   CompositionLocalProvider(
      LocalContainerForComposable provides container,
      LocalContainerForLayer provides container,
   ) {
      Box(
         modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
               container.updateContainerLayout(it)
            },
      ) {
         content()
         container.Layers()
      }
   }
}

/**
 * 添加Layer
 *
 * @param visible 是否显示Layer
 * @param onDismissRequest 由[dismissOnBackPress]或者[dismissOnTouchOutside]触发的移除回调
 * @param dismissOnBackPress 按返回键是否移除Layer，true-移除，false-不移除，null-不处理返回键逻辑，默认-true
 * @param dismissOnTouchOutside 触摸非内容区域是否移除Layer，true-移除，false-不移除，null-不处理，事件会透过背景，默认-false
 * @param position 显示位置
 * @param backgroundColor 背景颜色
 */
@Composable
fun Layer(
   visible: Boolean,
   onDismissRequest: () -> Unit,
   dismissOnBackPress: Boolean? = true,
   dismissOnTouchOutside: Boolean? = false,
   position: Layer.Position = Layer.Position.Center,
   backgroundColor: Color = Color.Black.copy(alpha = 0.3f),
   display: @Composable LayerDisplayScope.() -> Unit = DefaultDisplay,
   content: @Composable LayerContentScope.() -> Unit,
) {
   val layer = rememberLayer(
      display = display,
      content = content,
   ).apply {
      setDismissOnBackPress(dismissOnBackPress)
      setDismissOnTouchOutside(dismissOnTouchOutside)
      setPosition(position)
      setBackgroundColor(backgroundColor)
   }

   LaunchedEffect(layer, visible) {
      if (visible) {
         layer.attach()
      } else {
         layer.detach()
      }
   }
}

/**
 * 把当前元素设置为目标，并绑定容器作用域内唯一的[tag]
 */
fun Modifier.layerTarget(tag: String): Modifier = composed {
   require(tag.isNotEmpty()) { "tag is empty." }

   val container = checkNotNull(LocalContainerForComposable.current) {
      "Not in LayerContainer scope."
   }

   DisposableEffect(container, tag) {
      onDispose {
         container.removeTarget(tag)
      }
   }

   this.onGloballyPositioned {
      container.addTarget(tag, it)
   }
}

@Composable
fun rememberLayer(
   onCreate: ((Layer) -> Unit)? = null,
   display: @Composable LayerDisplayScope.() -> Unit = DefaultDisplay,
   content: @Composable LayerContentScope.() -> Unit,
): Layer {
   val layer = remember {
      LayerImpl().also { onCreate?.invoke(it) }
   }.apply {
      this.Init(content = content, display = display)
   }
   DisposableEffect(layer) {
      onDispose {
         layer.destroy()
      }
   }
   return layer
}

@Composable
fun rememberTargetLayer(
   onCreate: ((TargetLayer) -> Unit)? = null,
   display: @Composable LayerDisplayScope.() -> Unit = DefaultDisplay,
   content: @Composable LayerContentScope.() -> Unit,
): TargetLayer {
   val layer = remember {
      TargetLayerImpl().also { onCreate?.invoke(it) }
   }.apply {
      this.Init(content = content, display = display)
   }
   DisposableEffect(layer) {
      onDispose {
         layer.destroy()
      }
   }
   return layer
}

internal val LocalContainerForComposable = staticCompositionLocalOf<ContainerForComposable?> { null }
internal val LocalContainerForLayer = staticCompositionLocalOf<ContainerForLayer?> { null }

internal val DefaultDisplay: @Composable LayerDisplayScope.() -> Unit = {
   DisplayDefault()
}