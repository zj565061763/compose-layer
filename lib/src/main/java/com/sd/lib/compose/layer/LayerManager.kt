package com.sd.lib.compose.layer

import androidx.compose.runtime.*
import androidx.compose.ui.layout.LayoutCoordinates

internal val LocalLayerManager = compositionLocalOf<LayerManager?> { null }

internal class LayerManager {
    private val _layerHolder: MutableList<FLayer> = mutableStateListOf()
    private val _targetLayoutHolder: MutableMap<String, LayoutCoordinates> = mutableMapOf()
    private val _targetLayoutCallbackHolder: MutableMap<String, MutableSet<(LayoutCoordinates?) -> Unit>> = mutableMapOf()

    @Composable
    fun layer(): FLayer {
        val state = remember { FLayer() }
        DisposableEffect(state) {
            _layerHolder.add(state)
            state.attachToManager(this@LayerManager)
            onDispose {
                _layerHolder.remove(state)
                state.detachFromManager()
            }
        }
        return state
    }

    @Composable
    fun Content() {
        _layerHolder.forEach { item ->
            item.Content()
        }
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
        val holder = _targetLayoutCallbackHolder[tag] ?: mutableSetOf<(LayoutCoordinates?) -> Unit>().also {
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

    private fun notifyTargetLayoutCallback(tag: String, layoutCoordinates: LayoutCoordinates?) {
        _targetLayoutCallbackHolder[tag]?.toTypedArray()?.let { holder ->
            holder.forEach {
                it.invoke(layoutCoordinates)
            }
        }
    }
}