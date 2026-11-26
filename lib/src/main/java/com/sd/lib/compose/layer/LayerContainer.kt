package com.sd.lib.compose.layer

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.layout.LayoutCoordinates

internal fun newLayerContainer(): LayerContainer = LayerContainerImpl()

internal interface LayerContainer : ContainerForComposable, ContainerForLayer

internal interface ContainerForComposable {
  fun updateContainerLayout(layoutCoordinates: LayoutCoordinates)
  fun addTarget(tag: String, owner: Any, layoutCoordinates: LayoutCoordinates)
  fun removeTarget(tag: String, owner: Any)
  fun destroy()

  @Composable
  fun Layers()
}

internal interface ContainerForLayer {
  fun initLayer(layer: LayerImpl)
  fun releaseLayer(layer: LayerImpl)

  fun attachLayer(layer: LayerImpl)
  fun detachLayer(layer: LayerImpl): Boolean

  fun registerContainerLayoutCallback(callback: LayoutCoordinatesCallback)
  fun unregisterContainerLayoutCallback(callback: LayoutCoordinatesCallback)

  fun registerTargetLayoutCallback(tag: String?, callback: LayoutCoordinatesCallback)
  fun unregisterTargetLayoutCallback(tag: String?, callback: LayoutCoordinatesCallback)
}

internal typealias LayoutCoordinatesCallback = (LayoutCoordinates?) -> Unit

private abstract class ComposableLayerContainer : ContainerForComposable {
  var destroyed = false
    private set

  /** 容器布局信息 */
  private var _containerLayout: LayoutCoordinates? = null

  /** 目标布局信息 */
  private val _targetsLayout: MutableMap<String, TargetHolder> = mutableMapOf()

  final override fun updateContainerLayout(layoutCoordinates: LayoutCoordinates) {
    if (destroyed) return
    _containerLayout = layoutCoordinates
    onUpdateContainerLayout(layoutCoordinates)
  }

  final override fun addTarget(tag: String, owner: Any, layoutCoordinates: LayoutCoordinates) {
    if (destroyed) return
    _targetsLayout[tag]?.also { old ->
      check(old.owner === owner) { "Tag:$tag already exist." }
    }
    _targetsLayout[tag] = TargetHolder(
      owner = owner,
      layoutCoordinates = layoutCoordinates,
    )
    onUpdateTargetLayout(tag, layoutCoordinates)
  }

  final override fun removeTarget(tag: String, owner: Any) {
    if (destroyed) return
    val current = _targetsLayout[tag] ?: return
    if (current.owner === owner) {
      _targetsLayout.remove(tag)
      onUpdateTargetLayout(tag, null)
    }
  }

  override fun destroy() {
    destroyed = true
    _containerLayout = null
    _targetsLayout.clear()
  }

  fun getContainerLayout(): LayoutCoordinates? {
    return _containerLayout
  }

  fun getTargetLayout(tag: String): LayoutCoordinates? {
    return _targetsLayout[tag]?.layoutCoordinates
  }

  abstract fun onUpdateContainerLayout(layoutCoordinates: LayoutCoordinates)
  abstract fun onUpdateTargetLayout(tag: String, layoutCoordinates: LayoutCoordinates?)

  private class TargetHolder(
    val owner: Any,
    val layoutCoordinates: LayoutCoordinates,
  )
}

private class LayerContainerImpl : ComposableLayerContainer(), LayerContainer {
  private val _attachedLayers = mutableStateListOf<LayerImpl>()

  private val _containerLayoutCallbacks: MutableSet<LayoutCoordinatesCallback> = mutableSetOf()
  private val _targetLayoutCallbacks: MutableMap<String, MutableSet<LayoutCoordinatesCallback>> = mutableMapOf()

  override fun onUpdateContainerLayout(layoutCoordinates: LayoutCoordinates) {
    _containerLayoutCallbacks.toTypedArray().forEach {
      it.invoke(layoutCoordinates)
    }
  }

  override fun onUpdateTargetLayout(tag: String, layoutCoordinates: LayoutCoordinates?) {
    _targetLayoutCallbacks[tag]?.toTypedArray()?.forEach {
      it.invoke(layoutCoordinates)
    }
  }

  override fun initLayer(layer: LayerImpl) {
    if (destroyed) return
    if (layer.layerContainer === this) return
    layer.release()
    layer.onInit(this)
    check(layer.layerContainer === this)
  }

  override fun releaseLayer(layer: LayerImpl) {
    if (layer.layerContainer === this) {
      _attachedLayers.remove(layer)
      layer.onRelease(this)
      check(layer.layerContainer == null)
    }
  }

  override fun attachLayer(layer: LayerImpl) {
    if (destroyed) return
    if (layer.layerContainer === this) {
      if (!_attachedLayers.contains(layer)) {
        _attachedLayers.add(layer)
      }
    }
  }

  override fun detachLayer(layer: LayerImpl): Boolean {
    return _attachedLayers.remove(layer)
  }

  //---------- container ----------

  override fun registerContainerLayoutCallback(callback: LayoutCoordinatesCallback) {
    if (destroyed) return
    if (_containerLayoutCallbacks.add(callback)) {
      callback(getContainerLayout())
    }
  }

  override fun unregisterContainerLayoutCallback(callback: LayoutCoordinatesCallback) {
    if (_containerLayoutCallbacks.remove(callback)) {
      callback(null)
    }
  }

  //---------- target ----------

  override fun registerTargetLayoutCallback(tag: String?, callback: LayoutCoordinatesCallback) {
    if (destroyed) return
    if (tag.isNullOrEmpty()) return
    val holder = _targetLayoutCallbacks.getOrPut(tag) { mutableSetOf() }
    if (holder.add(callback)) {
      callback(getTargetLayout(tag))
    }
  }

  override fun unregisterTargetLayoutCallback(tag: String?, callback: LayoutCoordinatesCallback) {
    if (tag.isNullOrEmpty()) return
    val holder = _targetLayoutCallbacks[tag] ?: return
    if (holder.remove(callback)) {
      callback(null)
      if (holder.isEmpty()) {
        _targetLayoutCallbacks.remove(tag)
      }
    }
  }

  override fun destroy() {
    super.destroy()
    _attachedLayers.toTypedArray().forEach { releaseLayer(it) }
    _attachedLayers.clear()
    _containerLayoutCallbacks.clear()
    _targetLayoutCallbacks.clear()
  }

  @Composable
  override fun Layers() {
    _attachedLayers.forEach { layer ->
      key(layer) { layer.Content() }
    }
    HandleBackPress()
  }

  /** 按zIndex与挂载顺序由顶层Layer处理返回键 */
  @Composable
  private fun HandleBackPress() {
    val target = findBackPressLayer() ?: return
    BackHandler {
      target.handleBackPress()
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