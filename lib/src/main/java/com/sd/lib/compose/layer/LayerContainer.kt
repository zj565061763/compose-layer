package com.sd.lib.compose.layer

import androidx.annotation.CallSuper
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.layout.LayoutCoordinates

internal fun newLayerContainer(): LayerContainer = LayerContainerImpl()

internal interface LayerContainer : ContainerForComposable, ContainerForLayer

internal interface ContainerForComposable {
  fun updateContainerLayout(layoutCoordinates: LayoutCoordinates)
  fun addTarget(tag: String, layoutCoordinates: LayoutCoordinates)
  fun removeTarget(tag: String)
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
  protected var destroyed = false
    private set

  /** 容器布局信息 */
  private var _containerLayout: LayoutCoordinates? = null

  /** 目标布局信息 */
  private val _targetsLayout: MutableMap<String, LayoutCoordinates> = mutableMapOf()

  final override fun updateContainerLayout(layoutCoordinates: LayoutCoordinates) {
    if (destroyed) return
    _containerLayout = layoutCoordinates
    onUpdateContainerLayout(layoutCoordinates)
  }

  final override fun addTarget(tag: String, layoutCoordinates: LayoutCoordinates) {
    if (destroyed) return
    _targetsLayout.put(tag, layoutCoordinates)?.also { old ->
      check(layoutCoordinates === old) { "Tag:$tag already exist." }
    }
    onUpdateTargetLayout(tag, layoutCoordinates)
  }

  final override fun removeTarget(tag: String) {
    if (destroyed) return
    if (_targetsLayout.remove(tag) != null) {
      onUpdateTargetLayout(tag, null)
    }
  }

  @CallSuper
  override fun destroy() {
    destroyed = true
    _containerLayout = null
    _targetsLayout.clear()
  }

  protected fun getContainerLayout(): LayoutCoordinates? = _containerLayout
  protected fun getTargetLayout(tag: String): LayoutCoordinates? = _targetsLayout[tag]

  protected abstract fun onUpdateContainerLayout(layoutCoordinates: LayoutCoordinates)
  protected abstract fun onUpdateTargetLayout(tag: String, layoutCoordinates: LayoutCoordinates?)
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
    Box {
      _attachedLayers.forEach { layer ->
        key(layer) {
          layer.Content()
        }
      }
    }
  }
}