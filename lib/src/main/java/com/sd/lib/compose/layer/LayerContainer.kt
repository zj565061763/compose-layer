package com.sd.lib.compose.layer

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.registerOnLayoutRectChanged
import androidx.compose.ui.node.DelegatableNode.RegistrationHandle
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.UnplacedAwareModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.spatial.RelativeLayoutBounds
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round

internal fun newLayerContainer(): LayerContainer {
  return LayerContainerImpl()
}

/** 窗口坐标系下的布局矩形信息 */
@Immutable
internal data class LayoutRect(
  val offset: IntOffset,
  val size: IntSize,
)

internal typealias LayoutRectCallback = (LayoutRect?) -> Unit

internal interface LayerContainer : ContainerForComposable, ContainerForLayer

internal interface ContainerForComposable {
  val hasAttachedLayers: Boolean

  fun attachContainerNode(node: LayerContainerNode)
  fun detachContainerNode(node: LayerContainerNode)
  fun onContainerRectChanged(rect: LayoutRect?)

  fun attachTargetNode(tag: String, node: LayerTagNode)
  fun detachTargetNode(tag: String, node: LayerTagNode)
  fun onTargetRectChanged(tag: String, node: LayerTagNode, rect: LayoutRect?)

  fun destroy()

  @Composable
  fun Layers()
}

internal interface ContainerForLayer {
  fun initLayer(layer: LayerImpl)
  fun releaseLayer(layer: LayerImpl)

  fun attachLayer(layer: LayerImpl)
  fun detachLayer(layer: LayerImpl): Boolean

  fun registerContainerLayoutCallback(callback: LayoutRectCallback)
  fun unregisterContainerLayoutCallback(callback: LayoutRectCallback)

  fun registerTargetLayoutCallback(tag: String?, callback: LayoutRectCallback)
  fun unregisterTargetLayoutCallback(tag: String?, callback: LayoutRectCallback)
}

internal fun Modifier.layerContainer(container: ContainerForComposable): Modifier {
  return this then LayerContainerElement(container)
}

private data class LayerContainerElement(
  val container: ContainerForComposable,
) : ModifierNodeElement<LayerContainerNode>() {
  override fun create(): LayerContainerNode {
    return LayerContainerNode(container)
  }

  override fun update(node: LayerContainerNode) {}

  override fun InspectorInfo.inspectableProperties() {
    name = "layerContainer"
  }
}

/** 按需订阅窗口矩形变化的节点基类 */
internal abstract class LayoutTrackNode :
  Modifier.Node(),
  LayoutAwareModifierNode,
  UnplacedAwareModifierNode {

  private var _coordinates: LayoutCoordinates? = null
  private var _rectHandle: RegistrationHandle? = null
  private var _observing: Boolean = false

  val isPlaced: Boolean
    get() {
      return _coordinates?.isAttached == true
    }

  /** 获取当前已放置节点的窗口矩形 */
  fun currentLayoutRect(): LayoutRect? {
    return _coordinates?.takeIf { it.isAttached }?.toLayoutRect()
  }

  /** 切换是否监听窗口矩形变化 */
  fun setObserving(observing: Boolean) {
    if (_observing == observing) return
    _observing = observing
    syncRectRegistration()
  }

  protected abstract fun onRectChanged(rect: LayoutRect?)

  protected fun syncRectRegistration() {
    _rectHandle?.unregister()
    _rectHandle = null
    if (isAttached && _observing) {
      _rectHandle = registerOnLayoutRectChanged(
        throttleMillis = 0,
        debounceMillis = 0,
      ) { bounds ->
        onRectChanged(bounds.toLayoutRect())
      }
    }
  }

  override fun onPlaced(coordinates: LayoutCoordinates) {
    _coordinates = coordinates
    if (_observing && coordinates.isAttached) {
      onRectChanged(coordinates.toLayoutRect())
    }
  }

  override fun onUnplaced() {
    _coordinates = null
    if (_observing) {
      onRectChanged(null)
    }
  }

  override fun onDetach() {
    _rectHandle?.unregister()
    _rectHandle = null
    _coordinates = null
    _observing = false
  }
}

internal class LayerContainerNode(
  private val container: ContainerForComposable,
) : LayoutTrackNode() {

  override fun onAttach() {
    container.attachContainerNode(this)
  }

  override fun onDetach() {
    container.detachContainerNode(this)
    super.onDetach()
  }

  override fun onRectChanged(rect: LayoutRect?) {
    container.onContainerRectChanged(rect)
  }
}

private class LayerContainerImpl : LayerContainer {
  private var _containerNode: LayerContainerNode? = null
  private val _targetNodes: MutableMap<String, LayerTagNode> = mutableMapOf()

  private val _attachedLayers = mutableStateListOf<LayerImpl>()
  private val _containerLayoutCallbacks: MutableSet<LayoutRectCallback> = mutableSetOf()
  private val _targetLayoutCallbacks: MutableMap<String, MutableSet<LayoutRectCallback>> = mutableMapOf()

  override val hasAttachedLayers: Boolean
    get() {
      return _attachedLayers.isNotEmpty()
    }

  override fun attachContainerNode(node: LayerContainerNode) {
    _containerNode = node
    val hasObservers = _containerLayoutCallbacks.isNotEmpty()
    node.setObserving(observing = hasObservers)
    if (hasObservers && node.isPlaced) {
      onContainerRectChanged(node.currentLayoutRect())
    }
  }

  override fun detachContainerNode(node: LayerContainerNode) {
    if (_containerNode === node) {
      node.setObserving(observing = false)
      _containerNode = null
      onContainerRectChanged(null)
    }
  }

  override fun onContainerRectChanged(rect: LayoutRect?) {
    _containerLayoutCallbacks.forEach { callback ->
      callback.invoke(rect)
    }
  }

  override fun attachTargetNode(tag: String, node: LayerTagNode) {
    val old = _targetNodes[tag]
    if (old === node) return
    check(old == null) { "Tag:$tag already exist." }
    _targetNodes[tag] = node
    val hasObservers = !_targetLayoutCallbacks[tag].isNullOrEmpty()
    node.setObserving(observing = hasObservers)
    if (hasObservers) {
      notifyTargetCallbacks(tag, node.currentLayoutRect())
    }
  }

  override fun detachTargetNode(tag: String, node: LayerTagNode) {
    if (_targetNodes[tag] === node) {
      node.setObserving(observing = false)
      _targetNodes.remove(tag)
      notifyTargetCallbacks(tag, null)
    }
  }

  override fun onTargetRectChanged(tag: String, node: LayerTagNode, rect: LayoutRect?) {
    if (_targetNodes[tag] === node) {
      notifyTargetCallbacks(tag, rect)
    }
  }

  private fun notifyTargetCallbacks(tag: String, rect: LayoutRect?) {
    _targetLayoutCallbacks[tag]?.forEach { callback ->
      callback.invoke(rect)
    }
  }

  override fun initLayer(layer: LayerImpl) {
    if (layer.layerContainer === this) return
    check(layer.layerContainer == null) { "Layer already initialized with another container." }
    layer.onInit(this)
  }

  override fun releaseLayer(layer: LayerImpl) {
    if (layer.layerContainer === this) {
      _attachedLayers.remove(layer)
      layer.onRelease(this)
    }
  }

  override fun attachLayer(layer: LayerImpl) {
    if (layer.layerContainer === this) {
      if (!_attachedLayers.contains(layer)) {
        _attachedLayers.add(layer)
      }
    }
  }

  override fun detachLayer(layer: LayerImpl): Boolean {
    return _attachedLayers.remove(layer)
  }

  override fun registerContainerLayoutCallback(callback: LayoutRectCallback) {
    if (_containerLayoutCallbacks.add(callback)) {
      val node = _containerNode
      node?.setObserving(observing = true)
      callback(node?.currentLayoutRect())
    }
  }

  override fun unregisterContainerLayoutCallback(callback: LayoutRectCallback) {
    if (_containerLayoutCallbacks.remove(callback)) {
      if (_containerLayoutCallbacks.isEmpty()) {
        _containerNode?.setObserving(observing = false)
      }
      callback(null)
    }
  }

  override fun registerTargetLayoutCallback(tag: String?, callback: LayoutRectCallback) {
    if (tag.isNullOrEmpty()) return
    val holder = _targetLayoutCallbacks.getOrPut(tag) { mutableSetOf() }
    if (holder.add(callback)) {
      val node = _targetNodes[tag]
      node?.setObserving(observing = true)
      callback(node?.currentLayoutRect())
    }
  }

  override fun unregisterTargetLayoutCallback(tag: String?, callback: LayoutRectCallback) {
    if (tag.isNullOrEmpty()) return
    val holder = _targetLayoutCallbacks[tag] ?: return
    if (holder.remove(callback)) {
      if (holder.isEmpty()) {
        _targetLayoutCallbacks.remove(tag)
        _targetNodes[tag]?.setObserving(observing = false)
      }
      callback(null)
    }
  }

  override fun destroy() {
    _containerNode?.setObserving(observing = false)
    _containerNode = null
    _targetNodes.values.forEach { it.setObserving(observing = false) }
    _targetNodes.clear()
    _attachedLayers.toTypedArray().forEach { releaseLayer(it) }
    _attachedLayers.clear()
    _containerLayoutCallbacks.clear()
    _targetLayoutCallbacks.clear()
  }

  @Composable
  override fun Layers() {
    HandleBackPress()
    _attachedLayers.forEach { layer ->
      key(layer) { layer.Content() }
    }
  }

  /** 按zIndex与挂载顺序由顶层Layer处理返回键 */
  @Composable
  private fun HandleBackPress() {
    val target = findBackPressLayer() ?: return
    key(target) {
      BackHandler {
        target.handleBackPress()
      }
    }
  }

  private fun findBackPressLayer(): LayerImpl? {
    var target: LayerImpl? = null
    for (layer in _attachedLayers) {
      if (layer.shouldHandleBackPress()) {
        if ((target == null) || (layer.zIndex >= target.zIndex)) {
          target = layer
        }
      }
    }
    return target
  }
}

private fun LayoutCoordinates.toLayoutRect(): LayoutRect {
  return LayoutRect(
    offset = positionInWindow().round(),
    size = size,
  )
}

private fun RelativeLayoutBounds.toLayoutRect(): LayoutRect {
  val bounds = boundsInWindow
  return LayoutRect(
    offset = bounds.topLeft,
    size = bounds.size,
  )
}
