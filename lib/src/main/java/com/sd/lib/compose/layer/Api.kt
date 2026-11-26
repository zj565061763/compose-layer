package com.sd.lib.compose.layer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.CapturedAnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.UnplacedAwareModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
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
      modifier = modifier
        .fillMaxSize()
        .onGloballyPositioned { container.updateContainerLayout(it) },
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
  captureOnDetach: Boolean = true,
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
    captureOnDetach = captureOnDetach,
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
 * @param captureOnDetach 移除时是否捕获最后一帧执行退出动画，true-使用[CapturedAnimatedVisibility]；false-使用[AnimatedVisibility]，默认true
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
  captureOnDetach: Boolean = true,
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
    this.setCaptureOnDetach(captureOnDetach)
    this.setZIndex(zIndex)
    if (!attach) {
      this.detach()
    }
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
  captureOnDetach: Boolean = true,
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
    captureOnDetach = captureOnDetach,
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
 * @param captureOnDetach 移除时是否捕获最后一帧执行退出动画，true-使用[CapturedAnimatedVisibility]；false-使用[AnimatedVisibility]，默认true
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
  captureOnDetach: Boolean = true,
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
    this.setCaptureOnDetach(captureOnDetach)
    this.setZIndex(zIndex)
    if (!attach) {
      this.detach()
    }
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

/** 为当前元素设置容器作用域内唯一的[tag] */
@Stable
fun Modifier.layerTag(tag: String): Modifier {
  return this then LayerTagElement(tag)
}

private data class LayerTagElement(
  val tag: String,
) : ModifierNodeElement<LayerTagNode>() {
  override fun create(): LayerTagNode {
    return LayerTagNode(tag)
  }

  override fun update(node: LayerTagNode) {
    node.updateTag(tag)
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "layerTag"
    properties["tag"] = tag
  }
}

private class LayerTagNode(
  tag: String,
) : Modifier.Node(),
  GlobalPositionAwareModifierNode,
  UnplacedAwareModifierNode,
  CompositionLocalConsumerModifierNode,
  ObserverModifierNode {

  private var _tag: String = tag
  private var _container: ContainerForComposable? = null
  private var _coordinates: LayoutCoordinates? = null

  fun updateTag(tag: String) {
    if (_tag == tag) return
    val container = _container
    container?.removeTarget(tag = _tag, owner = this)
    _tag = tag
    if (container != null) {
      require(tag.isNotEmpty()) { "tag is empty." }
      if (isAttached) {
        sideEffect { syncTarget() }
      }
    }
  }

  override fun onAttach() {
    updateContainer()
  }

  override fun onObservedReadsChanged() {
    updateContainer()
  }

  override fun onDetach() {
    _container?.removeTarget(tag = _tag, owner = this)
    _container = null
    _coordinates = null
  }

  override fun onUnplaced() {
    _coordinates = null
    _container?.removeTarget(tag = _tag, owner = this)
  }

  override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
    _coordinates = coordinates
    _container?.addTarget(tag = _tag, owner = this, layoutCoordinates = coordinates)
  }

  /** 解析并监听当前容器作用域 */
  private fun updateContainer() {
    var inspectionMode = false
    var newContainer: ContainerForComposable? = null
    observeReads {
      inspectionMode = currentValueOf(LocalInspectionMode)
      if (!inspectionMode) {
        newContainer = currentValueOf(LocalContainerForComposable)
      }
    }

    val oldContainer = _container
    if (oldContainer === newContainer) {
      if (!inspectionMode) {
        require(_tag.isNotEmpty()) { "tag is empty." }
        checkNotNull(newContainer) { "Not in LayerContainer scope." }
      }
      return
    }

    oldContainer?.removeTarget(tag = _tag, owner = this)
    if (inspectionMode) {
      _container = null
      return
    }

    require(_tag.isNotEmpty()) { "tag is empty." }
    _container = checkNotNull(newContainer) { "Not in LayerContainer scope." }
    if (isAttached) {
      sideEffect { syncTarget() }
    }
  }

  /** 用已挂载的布局坐标向容器同步当前[tag][_tag] */
  private fun syncTarget() {
    if (!isAttached) return
    val coordinates = _coordinates ?: return
    if (coordinates.isAttached) {
      _container?.addTarget(tag = _tag, owner = this, layoutCoordinates = coordinates)
    }
  }
}

internal val LocalContainerForComposable = staticCompositionLocalOf<ContainerForComposable?> { null }
internal val LocalContainerForLayer = staticCompositionLocalOf<ContainerForLayer?> { null }