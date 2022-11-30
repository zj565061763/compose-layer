package com.sd.lib.compose.layer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.zIndex

internal val LocalLayerManager = compositionLocalOf<LayerManager?> { null }

internal class LayerManager {
    private val _layerHolder: MutableList<FLayer> = mutableStateListOf()
    private val _attachedLayerHolder: MutableList<FLayer> = mutableStateListOf()

    private val _targetLayoutHolder: MutableMap<String, LayoutCoordinates> = hashMapOf()
    private val _targetLayoutCallbackHolder: MutableMap<String, MutableSet<(LayoutCoordinates?) -> Unit>> = hashMapOf()

    private var _containerLayout: LayoutCoordinates? = null
    private val _containerLayoutCallbackHolder: MutableSet<(LayoutCoordinates) -> Unit> = hashSetOf()

    val containerLayout: LayoutCoordinates?
        get() = _containerLayout

    @Composable
    fun layer(): FLayer {
        val layer = remember { FLayer() }
        DisposableEffect(layer) {
            attachLayer(layer)
            onDispose {
                detachLayer(layer)
            }
        }
        return layer
    }

    @Composable
    fun Content() {
        _layerHolder.forEach { item ->
            val zIndex = _attachedLayerHolder.indexOf(item).coerceAtLeast(0)
            Box(modifier = Modifier.zIndex(zIndex.toFloat())) {
                item.Content()
            }
        }

        _attachedLayerHolder.forEach { item ->
            item.dialogBehaviorState?.let { behavior ->
                BackHandler(item.isVisibleState) {
                    if (behavior.cancelable) {
                        item.detach()
                    }
                }
            }
        }
    }

    fun attachLayer(layer: FLayer) {
        if (!_layerHolder.contains(layer)) {
            _layerHolder.add(layer)
            layer.attachToManager(this)
        }
    }

    fun detachLayer(layer: FLayer) {
        if (_layerHolder.remove(layer)) {
            _attachedLayerHolder.remove(layer)
            layer.detachFromManager()
        }
    }

    fun updateContainerLayout(layoutCoordinates: LayoutCoordinates) {
        _containerLayout = layoutCoordinates
        _containerLayoutCallbackHolder.toTypedArray().forEach {
            it.invoke(layoutCoordinates)
        }
    }

    fun registerContainerLayoutCallback(callback: (LayoutCoordinates) -> Unit) {
        _containerLayoutCallbackHolder.add(callback)
    }

    fun unregisterContainerLayoutCallback(callback: (LayoutCoordinates) -> Unit) {
        _containerLayoutCallbackHolder.remove(callback)
    }

    fun addTarget(tag: String, layoutCoordinates: LayoutCoordinates) {
        if (tag.isEmpty()) return
        val old = _targetLayoutHolder[tag]
        if (old != null) {
            check(old == layoutCoordinates) { "Tag:$tag has already specified." }
        }

        _targetLayoutHolder[tag] = layoutCoordinates
        notifyTargetLayoutCallback(tag, layoutCoordinates)
    }

    fun removeTarget(tag: String) {
        _targetLayoutHolder.remove(tag)
        notifyTargetLayoutCallback(tag, null)
    }

    fun findTarget(tag: String): LayoutCoordinates? {
        if (tag.isEmpty()) return null
        return _targetLayoutHolder[tag]
    }

    fun registerTargetLayoutCallback(tag: String, callback: (LayoutCoordinates?) -> Unit) {
        if (tag.isEmpty()) return
        val holder = _targetLayoutCallbackHolder[tag] ?: hashSetOf<(LayoutCoordinates?) -> Unit>().also {
            _targetLayoutCallbackHolder[tag] = it
        }
        holder.add(callback)
    }

    fun unregisterTargetLayoutCallback(tag: String, callback: (LayoutCoordinates?) -> Unit) {
        if (tag.isEmpty()) return
        val holder = _targetLayoutCallbackHolder[tag] ?: return
        if (holder.remove(callback)) {
            if (holder.isEmpty()) {
                _targetLayoutCallbackHolder.remove(tag)
            }
        }
    }

    fun notifyLayerAttachState(layer: FLayer, isAttached: Boolean) {
        _attachedLayerHolder.remove(layer)
        if (isAttached && _layerHolder.contains(layer)) {
            _attachedLayerHolder.add(layer)
        }
    }

    private fun notifyTargetLayoutCallback(tag: String, layoutCoordinates: LayoutCoordinates?) {
        _targetLayoutCallbackHolder[tag]?.toTypedArray()?.let { holder ->
            holder.forEach {
                it.invoke(layoutCoordinates)
            }
        }
    }
}