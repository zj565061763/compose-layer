package com.sd.lib.compose.layer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.zIndex

internal val LocalLayerManager = compositionLocalOf<LayerManager?> { null }

internal class LayerManager {
    private val _layerHolder: MutableSet<LayerImpl> = hashSetOf()
    private val _attachedLayerHolder: MutableList<LayerImpl> = mutableStateListOf()

    private val _targetLayoutHolder: MutableMap<String, LayoutCoordinates> = hashMapOf()
    private val _targetLayoutCallbackHolder: MutableMap<String, MutableSet<(LayoutCoordinates?) -> Unit>> = hashMapOf()

    private var _containerLayout: LayoutCoordinates? = null
    private val _containerLayoutCallbackHolder: MutableSet<(LayoutCoordinates?) -> Unit> = hashSetOf()

    @Composable
    fun rememberLayer(debug: Boolean): Layer {
        val layer = remember {
            LayerImpl().also {
                it.isDebug = debug
                addLayer(it)
            }
        }

        LaunchedEffect(debug) {
            layer.isDebug = debug
        }

        DisposableEffect(layer) {
            onDispose {
                removeLayer(layer)
            }
        }
        return layer
    }

    @Composable
    fun rememberTargetLayer(debug: Boolean): TargetLayer {
        val layer = remember {
            TargetLayerImpl().also {
                it.isDebug = debug
                addLayer(it)
            }
        }

        LaunchedEffect(debug) {
            layer.isDebug = debug
        }

        DisposableEffect(layer) {
            onDispose {
                removeLayer(layer)
            }
        }
        return layer
    }

    @Composable
    fun Layers() {
        _attachedLayerHolder.forEachIndexed { index, item ->
            Box(modifier = Modifier.zIndex(index.toFloat())) {
                item.Content()
            }

            item.dialogBehaviorState?.let { behavior ->
                BackHandler(item.isVisibleState) {
                    if (behavior.cancelable) {
                        item.detach()
                    }
                }
            }
        }
    }

    fun addLayer(layer: LayerImpl) {
        if (!_layerHolder.contains(layer)) {
            _layerHolder.add(layer)
            layer.attachToManager(this)
        }
    }

    fun removeLayer(layer: LayerImpl) {
        if (_layerHolder.remove(layer)) {
            _attachedLayerHolder.remove(layer)
            layer.detachFromManager(this)
        }
    }

    fun notifyLayerAttached(layer: LayerImpl) {
        if (_layerHolder.contains(layer)) {
            if (!_attachedLayerHolder.contains(layer)) {
                _attachedLayerHolder.add(layer)
            }
        }
    }

    fun notifyLayerDetached(layer: LayerImpl) {
        _attachedLayerHolder.remove(layer)
    }

    fun updateContainerLayout(layoutCoordinates: LayoutCoordinates) {
        _containerLayout = layoutCoordinates
        _containerLayoutCallbackHolder.toTypedArray().forEach {
            it.invoke(layoutCoordinates)
        }
    }

    fun registerContainerLayoutCallback(callback: (LayoutCoordinates?) -> Unit) {
        _containerLayoutCallbackHolder.add(callback)
        callback(_containerLayout)
    }

    fun unregisterContainerLayoutCallback(callback: (LayoutCoordinates?) -> Unit) {
        _containerLayoutCallbackHolder.remove(callback)
        callback(null)
    }

    fun addTarget(tag: String, layoutCoordinates: LayoutCoordinates) {
        if (tag.isEmpty()) return
        val old = _targetLayoutHolder[tag]
        if (old != null) {
            check(old === layoutCoordinates) { "Tag:$tag has already specified." }
        }
        _targetLayoutHolder[tag] = layoutCoordinates
        notifyTargetLayoutCallback(tag, layoutCoordinates)
    }

    fun removeTarget(tag: String) {
        if (_targetLayoutHolder.remove(tag) != null) {
            notifyTargetLayoutCallback(tag, null)
        }
    }

    fun registerTargetLayoutCallback(tag: String, callback: (LayoutCoordinates?) -> Unit) {
        if (tag.isEmpty()) return
        val holder = _targetLayoutCallbackHolder[tag] ?: hashSetOf<(LayoutCoordinates?) -> Unit>().also {
            _targetLayoutCallbackHolder[tag] = it
        }
        holder.add(callback)
        callback(_targetLayoutHolder[tag])
    }

    fun unregisterTargetLayoutCallback(tag: String, callback: (LayoutCoordinates?) -> Unit) {
        if (tag.isEmpty()) return
        val holder = _targetLayoutCallbackHolder[tag] ?: return
        if (holder.remove(callback)) {
            callback(null)
            if (holder.isEmpty()) {
                _targetLayoutCallbackHolder.remove(tag)
            }
        }
    }

    private fun notifyTargetLayoutCallback(tag: String, layoutCoordinates: LayoutCoordinates?) {
        _targetLayoutCallbackHolder[tag]?.toTypedArray()?.forEach {
            it.invoke(layoutCoordinates)
        }
    }
}