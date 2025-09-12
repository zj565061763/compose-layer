package com.sd.lib.compose.layer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.zIndex

/**
 * 显示Layer的容器
 */
@Composable
fun LayerContainer(
  modifier: Modifier = Modifier,
  layersZIndex: Float = 0f,
  content: @Composable BoxScope.() -> Unit,
) {
  val container = remember { newLayerContainer() }

  DisposableEffect(container) {
    onDispose { container.destroy() }
  }

  CompositionLocalProvider(
    LocalContainerForComposable provides container,
    LocalContainerForLayer provides container,
  ) {
    Box(
      modifier = modifier.onGloballyPositioned { container.updateContainerLayout(it) },
      contentAlignment = Alignment.Center,
    ) {
      content()
      Box(
        modifier = Modifier
          .matchParentSize()
          .zIndex(layersZIndex),
        contentAlignment = Alignment.Center,
      ) {
        container.Layers()
      }
    }
  }
}

//-------------------- Layer --------------------

/**
 * [layer]
 */
@Composable
fun Layer(
  attach: Boolean,
  onDetachRequest: (LayerDetach) -> Unit,
  debug: Boolean = false,
  detachOnBackPress: Boolean? = true,
  detachOnTouchOutside: Boolean? = true,
  backgroundColor: Color = Color.Black.copy(alpha = 0.3f),
  alignment: Alignment = Alignment.Center,
  transition: LayerTransition? = null,
  zIndex: Float = 0f,
  content: @Composable LayerContentScope.() -> Unit,
) {
  layer(
    attach = attach,
    onDetachRequest = onDetachRequest,
    debug = debug,
    detachOnBackPress = detachOnBackPress,
    detachOnTouchOutside = detachOnTouchOutside,
    backgroundColor = backgroundColor,
    alignment = alignment,
    transition = transition,
    zIndex = zIndex,
    content = content,
  )
}

/**
 * 创建Layer
 *
 * @param attach 是否添加Layer，true-添加；false-移除
 * @param onDetachRequest [LayerDetach]触发的移除回调
 * @param debug 是否调试模式，tag:FLayer
 * @param detachOnBackPress 按返回键是否请求移除Layer，true-请求移除；false-请求不移除；null-不处理返回键逻辑，默认true
 * @param detachOnTouchOutside 触摸非内容区域是否请求移除Layer，true-请求移除；false-不请求移除；null-不处理，默认true
 * @param backgroundColor 背景颜色
 * @param alignment 对齐容器位置
 * @param transition 动画（非响应式）
 * @param zIndex [Modifier.zIndex]
 * @param content 内容
 */
@Composable
fun layer(
  attach: Boolean,
  onDetachRequest: (LayerDetach) -> Unit,
  debug: Boolean = false,
  detachOnBackPress: Boolean? = true,
  detachOnTouchOutside: Boolean? = true,
  backgroundColor: Color = Color.Black.copy(alpha = 0.3f),
  alignment: Alignment = Alignment.Center,
  transition: LayerTransition? = null,
  zIndex: Float = 0f,
  content: @Composable LayerContentScope.() -> Unit,
): LayerState {
  val layer = remember { NormalLayerImpl() }.apply {
    this.debug = debug
    this.Init(content)
    this.setDetachRequestCallback(onDetachRequest)
    this.setDetachOnBackPress(detachOnBackPress)
    this.setDetachOnTouchOutside(detachOnTouchOutside)
    this.setBackgroundColor(backgroundColor)
    this.setAlignment(alignment)
    this.setTransition(transition)
    this.setZIndex(zIndex)
  }

  DisposableEffect(layer) {
    onDispose {
      layer.release()
    }
  }

  LaunchedEffect(layer, attach) {
    if (attach) {
      layer.attach()
    } else {
      layer.detach()
    }
  }

  return remember(layer) { layer.toLayerState() }
}

//-------------------- TargetLayer --------------------

/**
 * [targetLayer]
 */
@Composable
fun TargetLayer(
  target: LayerTarget?,
  attach: Boolean,
  onDetachRequest: (LayerDetach) -> Unit,
  debug: Boolean = false,
  detachOnBackPress: Boolean? = true,
  detachOnTouchOutside: Boolean? = true,
  backgroundColor: Color = Color.Black.copy(alpha = 0.3f),
  alignment: TargetAlignment = TargetAlignment.Center,
  alignmentOffsetX: TargetAlignmentOffset? = null,
  alignmentOffsetY: TargetAlignmentOffset? = null,
  smartAlignments: SmartAliments? = null,
  clipBackgroundDirection: Directions? = null,
  transition: LayerTransition? = null,
  zIndex: Float = 0f,
  content: @Composable LayerContentScope.() -> Unit,
) {
  targetLayer(
    target = target,
    attach = attach,
    onDetachRequest = onDetachRequest,
    debug = debug,
    detachOnBackPress = detachOnBackPress,
    detachOnTouchOutside = detachOnTouchOutside,
    backgroundColor = backgroundColor,
    alignment = alignment,
    alignmentOffsetX = alignmentOffsetX,
    alignmentOffsetY = alignmentOffsetY,
    smartAlignments = smartAlignments,
    clipBackgroundDirection = clipBackgroundDirection,
    transition = transition,
    zIndex = zIndex,
    content = content,
  )
}

/**
 * 创建TargetLayer
 *
 * @param target 要对齐的目标
 * @param attach 是否添加Layer，true-添加；false-移除
 * @param onDetachRequest [LayerDetach]触发的移除回调
 * @param debug 是否调试模式，tag:FLayer
 * @param detachOnBackPress 按返回键是否请求移除Layer，true-请求移除；false-不请求移除；null-不处理返回键逻辑，默认值true
 * @param detachOnTouchOutside 触摸非内容区域是否请求移除Layer，true-请求移除；false-不请求移除；null-不处理，默认true
 * @param backgroundColor 背景颜色
 * @param alignment 对齐目标位置
 * @param alignmentOffsetX 对齐目标X方向偏移量
 * @param alignmentOffsetY 对齐目标Y方向偏移量
 * @param smartAlignments 智能对齐目标位置（非响应式），null-关闭智能对齐；非null-开启智能对齐。
 * 开启之后，如果默认的[alignment]导致内容溢出会使用[smartAlignments]提供的位置按顺序查找溢出最小的位置
 * @param clipBackgroundDirection 裁切背景的方向[Directions]（非响应式）
 * @param transition 动画（非响应式）
 * @param zIndex [Modifier.zIndex]
 * @param content 内容
 */
@Composable
fun targetLayer(
  target: LayerTarget?,
  attach: Boolean,
  onDetachRequest: (LayerDetach) -> Unit,
  debug: Boolean = false,
  detachOnBackPress: Boolean? = true,
  detachOnTouchOutside: Boolean? = true,
  backgroundColor: Color = Color.Black.copy(alpha = 0.3f),
  alignment: TargetAlignment = TargetAlignment.Center,
  alignmentOffsetX: TargetAlignmentOffset? = null,
  alignmentOffsetY: TargetAlignmentOffset? = null,
  smartAlignments: SmartAliments? = null,
  clipBackgroundDirection: Directions? = null,
  transition: LayerTransition? = null,
  zIndex: Float = 0f,
  content: @Composable LayerContentScope.() -> Unit,
): TargetLayerState {
  val layer = remember { TargetLayerImpl() }.apply {
    this.debug = debug
    this.Init(content)
    this.setDetachRequestCallback(onDetachRequest)
    this.setDetachOnBackPress(detachOnBackPress)
    this.setDetachOnTouchOutside(detachOnTouchOutside)
    this.setBackgroundColor(backgroundColor)
    this.setTarget(target)
    this.setAlignment(alignment)
    this.setAlignmentOffsetX(alignmentOffsetX)
    this.setAlignmentOffsetY(alignmentOffsetY)
    this.setSmartAlignments(smartAlignments)
    this.setClipBackgroundDirection(clipBackgroundDirection)
    this.setTransition(transition)
    this.setZIndex(zIndex)
  }

  DisposableEffect(layer) {
    onDispose {
      layer.release()
    }
  }

  LaunchedEffect(layer, attach) {
    if (attach) {
      layer.attach()
    } else {
      layer.detach()
    }
  }

  return remember(layer) { layer.toTargetLayerState() }
}

/**
 * 为当前元素设置容器作用域内唯一的[tag]
 */
fun Modifier.layerTag(tag: String): Modifier = composed {
  if (LocalInspectionMode.current) return@composed this
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