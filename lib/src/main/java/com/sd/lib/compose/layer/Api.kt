package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
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
 * 创建Layer
 *
 * @param attach 是否添加Layer，true-添加；false-移除
 * @param onDetachRequest [LayerDetach]触发的移除回调
 * @param debug 是否调试模式，tag:FLayer
 * @param detachOnBackPress 按返回键是否移除Layer，true-移除；false-不移除；null-不处理返回键逻辑，默认true
 * @param detachOnTouchOutside 触摸非内容区域是否移除Layer，true-移除；false-不移除；null-不处理，事件会透过背景，默认false
 * @param backgroundColor 背景颜色
 * @param alignment 对齐容器位置
 * @param display Layer显示
 * @param content Layer内容
 */
@Composable
fun Layer(
   attach: Boolean,
   onDetachRequest: (LayerDetach) -> Unit,
   debug: Boolean = false,
   detachOnBackPress: Boolean? = true,
   detachOnTouchOutside: Boolean? = false,
   backgroundColor: Color = Color.Black.copy(alpha = 0.3f),
   alignment: Alignment = Alignment.Center,
   display: @Composable LayerDisplayScope.() -> Unit = DefaultDisplay,
   content: @Composable LayerContentScope.() -> Unit,
) {
   val layer = remember { NormalLayerImpl() }.apply {
      this.debug = debug
      this.Init(content = content, display = display)
      this.setDetachRequestCallback(onDetachRequest)
      this.setDetachOnBackPress(detachOnBackPress)
      this.setDetachOnTouchOutside(detachOnTouchOutside)
      this.setBackgroundColor(backgroundColor)
      this.setAlignment(alignment)
   }

   DisposableEffect(layer) {
      onDispose {
         layer.destroy()
      }
   }

   LaunchedEffect(layer, attach) {
      if (attach) {
         layer.attach()
      } else {
         layer.detach()
      }
   }
}

/**
 * 创建TargetLayer
 *
 * @param target 要对齐的目标
 * @param attach 是否添加Layer，true-添加；false-移除
 * @param onDetachRequest [LayerDetach]触发的移除回调
 * @param debug 是否调试模式，tag:FLayer
 * @param detachOnBackPress 按返回键是否移除Layer，true-移除；false-不移除；null-不处理返回键逻辑，默认true
 * @param detachOnTouchOutside 触摸非内容区域是否移除Layer，true-移除；false-不移除；null-不处理，事件会透过背景，默认false
 * @param backgroundColor 背景颜色
 * @param alignment 对齐目标位置
 * @param alignmentOffsetX 对齐目标X方向偏移量
 * @param alignmentOffsetY 对齐目标Y方向偏移量
 * @param fixOverflow 是否修复溢出，默认true（此参数非响应式）
 * @param findBestPosition 是否查找最佳的显示位置，默认false（此参数非响应式）
 * @param clipBackgroundDirection 裁切背景的方向[Directions]（此参数非响应式）
 * @param display Layer显示
 * @param content Layer内容
 */
@Composable
fun TargetLayer(
   target: LayerTarget?,
   attach: Boolean,
   onDetachRequest: (LayerDetach) -> Unit,
   debug: Boolean = false,
   detachOnBackPress: Boolean? = true,
   detachOnTouchOutside: Boolean? = false,
   backgroundColor: Color = Color.Black.copy(alpha = 0.3f),
   alignment: TargetAlignment = TargetAlignment.Center,
   alignmentOffsetX: TargetAlignmentOffset? = null,
   alignmentOffsetY: TargetAlignmentOffset? = null,
   fixOverflow: Boolean = true,
   findBestPosition: Boolean = false,
   clipBackgroundDirection: Directions? = null,
   display: @Composable LayerDisplayScope.() -> Unit = DefaultDisplay,
   content: @Composable LayerContentScope.() -> Unit,
) {
   val layer = remember { TargetLayerImpl() }.apply {
      this.debug = debug
      this.Init(content = content, display = display)
      this.setDetachRequestCallback(onDetachRequest)
      this.setDetachOnBackPress(detachOnBackPress)
      this.setDetachOnTouchOutside(detachOnTouchOutside)
      this.setBackgroundColor(backgroundColor)
      this.setTarget(target)
      this.setAlignment(alignment)
      this.setAlignmentOffsetX(alignmentOffsetX)
      this.setAlignmentOffsetY(alignmentOffsetY)
      this.setFixOverflow(fixOverflow)
      this.setFindBestPosition(findBestPosition)
      this.setClipBackgroundDirection(clipBackgroundDirection)
   }

   DisposableEffect(layer) {
      onDispose {
         layer.destroy()
      }
   }

   LaunchedEffect(layer, attach) {
      if (attach) {
         layer.attach()
      } else {
         layer.detach()
      }
   }
}

/**
 * 为当前元素设置容器作用域内唯一的[tag]
 */
fun Modifier.layerTag(tag: String): Modifier = composed {
   require(tag.isNotEmpty()) { "tag is empty." }

   val container = checkNotNull(LocalContainerForComposable.current) {
      "Not in LayerContainer scope."
   }

   DisposableEffect(container, tag) {
      onDispose {
         container.removeTarget(tag)
      }
   }

   onGloballyPositioned {
      container.addTarget(tag, it)
   }
}

internal val LocalContainerForComposable = staticCompositionLocalOf<ContainerForComposable?> { null }
internal val LocalContainerForLayer = staticCompositionLocalOf<ContainerForLayer?> { null }

internal val DefaultDisplay: @Composable LayerDisplayScope.() -> Unit = { DisplayDefault() }