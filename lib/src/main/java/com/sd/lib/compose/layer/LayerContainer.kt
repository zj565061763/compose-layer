package com.sd.lib.compose.layer

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.CallSuper
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

    fun attachLayer(layer: LayerImpl)

    fun detachLayer(layer: LayerImpl): Boolean

    fun destroyLayer(layer: LayerImpl)

    fun registerContainerLayoutCallback(callback: (LayoutCoordinates?) -> Unit)

    fun unregisterContainerLayoutCallback(callback: (LayoutCoordinates?) -> Unit)

    fun registerTargetLayoutCallback(tag: String, callback: (LayoutCoordinates?) -> Unit)

    fun unregisterTargetLayoutCallback(tag: String, callback: (LayoutCoordinates?) -> Unit)
}

private abstract class ComposableLayerContainer : ContainerForComposable {
    protected var destroyed = false
        private set

    /** 容器的布局信息 */
    private var _containerLayout: LayoutCoordinates? = null

    /** 目标的布局信息 */
    private val _targetLayouts: MutableMap<String, LayoutCoordinates> = hashMapOf()

    final override fun updateContainerLayout(layoutCoordinates: LayoutCoordinates) {
        if (destroyed) return
        _containerLayout = layoutCoordinates
        onUpdateContainerLayout(layoutCoordinates)
    }

    final override fun addTarget(tag: String, layoutCoordinates: LayoutCoordinates) {
        if (destroyed) return
        if (tag.isEmpty()) error("tag is empty.")

        _targetLayouts.put(tag, layoutCoordinates)?.let { old ->
            if (old !== layoutCoordinates) error("Tag:$tag already exist.")
        }
        onUpdateTargetLayout(tag, layoutCoordinates)
    }

    final override fun removeTarget(tag: String) {
        if (_targetLayouts.remove(tag) != null) {
            onUpdateTargetLayout(tag, null)
        }
    }

    @CallSuper
    override fun destroy() {
        destroyed = true
        _containerLayout = null
        _targetLayouts.clear()
    }

    protected fun getContainerLayout(): LayoutCoordinates? = _containerLayout

    protected fun getTargetLayout(tag: String): LayoutCoordinates? = _targetLayouts[tag]

    protected abstract fun onUpdateContainerLayout(layoutCoordinates: LayoutCoordinates)

    protected abstract fun onUpdateTargetLayout(tag: String, layoutCoordinates: LayoutCoordinates?)
}

private class LayerContainerImpl : ComposableLayerContainer(), LayerContainer {
    private val _attachedLayers: MutableList<LayerImpl> = mutableStateListOf()

    private val _containerLayoutCallbacks: MutableSet<(LayoutCoordinates?) -> Unit> = hashSetOf()
    private val _targetLayoutCallbacks: MutableMap<String, MutableSet<(LayoutCoordinates?) -> Unit>> = hashMapOf()

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
        if (layer.layerContainer === this) {
            // 已经初始化过了
            return
        }
        layer.destroy()
        layer.onInit(this)
        check(layer.layerContainer === this)
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

    override fun destroyLayer(layer: LayerImpl) {
        if (layer.layerContainer === this) {
            _attachedLayers.remove(layer)
            layer.onDestroy(this)
            check(layer.layerContainer == null)
        }
    }

    //---------- container ----------

    override fun registerContainerLayoutCallback(callback: (LayoutCoordinates?) -> Unit) {
        if (destroyed) return
        if (_containerLayoutCallbacks.add(callback)) {
            callback(getContainerLayout())
        }
    }

    override fun unregisterContainerLayoutCallback(callback: (LayoutCoordinates?) -> Unit) {
        if (_containerLayoutCallbacks.remove(callback)) {
            callback(null)
        }
    }

    //---------- target ----------

    override fun registerTargetLayoutCallback(tag: String, callback: (LayoutCoordinates?) -> Unit) {
        if (destroyed) return
        if (tag.isEmpty()) return
        val holder = _targetLayoutCallbacks.getOrPut(tag) { hashSetOf() }
        if (holder.add(callback)) {
            callback(getTargetLayout(tag))
        }
    }

    override fun unregisterTargetLayoutCallback(tag: String, callback: (LayoutCoordinates?) -> Unit) {
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
        _attachedLayers.clear()
        _containerLayoutCallbacks.clear()
        _targetLayoutCallbacks.clear()
    }

    @Composable
    override fun Layers() {
        _attachedLayers.forEach { layer ->
            key(layer.id) {
                layer.Content()
            }

            val behavior = layer.dialogBehavior
            if (behavior.enabledState) {
                BackHandler(layer.isVisibleState) {
                    if (behavior.cancelable) {
                        layer.detach()
                    }
                }
            }
        }
    }
}

internal inline fun logMsg(isDebug: Boolean, block: () -> String) {
    if (isDebug) {
        Log.i("FLayer", block())
    }
}