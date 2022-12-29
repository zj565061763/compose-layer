package com.sd.lib.compose.layer

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex

/**
 * 用来存放Layer的容器
 */
@Composable
fun LayerContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val layerContainer = remember { LayerContainer() }

    var pointerInputStarted by remember { mutableStateOf(false) }
    val shouldPointerInput by remember {
        derivedStateOf { layerContainer.hasAttachedLayer || pointerInputStarted }
    }

    DisposableEffect(layerContainer) {
        onDispose {
            layerContainer.destroy()
        }
    }

    CompositionLocalProvider(LocalLayerContainer provides layerContainer) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    layerContainer.updateContainerLayout(it)
                }
                .let {
                    if (shouldPointerInput) {
                        it.pointerInput(Unit) {
                            pointerInputStarted = true
                            forEachGesture {
                                awaitPointerEventScope {
                                    if (!layerContainer.hasAttachedLayer) pointerInputStarted = false
                                    val down = layerAwaitFirstDown(PointerEventPass.Initial)
                                    layerContainer.processDownEvent(down)
                                }
                            }
                        }
                    } else {
                        it
                    }
                },
        ) {
            content()
            layerContainer.Layers()
        }
    }
}

/**
 * 设置要对齐的目标并绑定[LayerContainer]作用域内唯一的[tag]
 */
fun Modifier.layerTarget(
    tag: String,
) = composed {
    val layerContainer = checkNotNull(LocalLayerContainer.current) {
        "CompositionLocal LocalLayerContainer not present"
    }

    DisposableEffect(layerContainer, tag) {
        onDispose {
            layerContainer.removeTarget(tag)
        }
    }

    this.onGloballyPositioned {
        layerContainer.addTarget(tag, it)
    }
}

internal val LocalLayerContainer = staticCompositionLocalOf<LayerContainer?> { null }

internal class LayerContainer {
    private var _destroyed = false

    private val _layerHolder: MutableSet<FLayer> = hashSetOf()
    private val _attachedLayerHolder: MutableList<FLayer> = mutableStateListOf()

    private var _containerLayout: LayoutCoordinates? = null
    private val _containerLayoutCallbackHolder: MutableSet<(LayoutCoordinates?) -> Unit> = hashSetOf()

    private val _targetLayoutHolder: MutableMap<String, LayoutCoordinates> = hashMapOf()
    private val _targetLayoutCallbackHolder: MutableMap<String, MutableSet<(LayoutCoordinates?) -> Unit>> = hashMapOf()

    val hasAttachedLayer by derivedStateOf { _attachedLayerHolder.isNotEmpty() }

    @Composable
    fun Layers() {
        _attachedLayerHolder.forEachIndexed { index, item ->
            Box(modifier = Modifier.zIndex(index.toFloat())) {
                item.Content()
            }

            val behavior = item.dialogBehavior
            if (behavior.enabledState) {
                BackHandler(item.isVisibleState) {
                    if (behavior.cancelable) {
                        item.detach()
                    }
                }
            }
        }
    }

    fun initLayer(layer: FLayer) {
        if (_destroyed) return
        if (_layerHolder.contains(layer)) return

        // 如果layer已经被添加别的容器，则先把它从别的容器移除
        layer._layerContainer?.destroyLayer(layer)

        _layerHolder.add(layer)
        layer.onInit(this)
        check(layer._layerContainer === this)
    }

    fun attachLayer(layer: FLayer) {
        if (_destroyed) return
        if (_layerHolder.contains(layer)) {
            if (!_attachedLayerHolder.contains(layer)) {
                _attachedLayerHolder.add(layer)
            }
        }
    }

    fun detachLayer(layer: FLayer) {
        _attachedLayerHolder.remove(layer)
    }

    fun processDownEvent(event: PointerInputChange) {
        if (_destroyed) return
        val copyHolder = _attachedLayerHolder.toTypedArray()
        for (index in copyHolder.lastIndex downTo 0) {
            val layer = copyHolder[index]
            layer.processDownEvent(event)
            if (event.isConsumed) break
        }
    }

    fun updateContainerLayout(layoutCoordinates: LayoutCoordinates) {
        if (_destroyed) return
        _containerLayout = layoutCoordinates
        _containerLayoutCallbackHolder.toTypedArray().forEach {
            it.invoke(layoutCoordinates)
        }
    }

    fun registerContainerLayoutCallback(callback: (LayoutCoordinates?) -> Unit) {
        if (_destroyed) return
        if (_containerLayoutCallbackHolder.add(callback)) {
            callback(_containerLayout)
        }
    }

    fun unregisterContainerLayoutCallback(callback: (LayoutCoordinates?) -> Unit) {
        if (_containerLayoutCallbackHolder.remove(callback)) {
            callback(null)
        }
    }

    fun addTarget(tag: String, layoutCoordinates: LayoutCoordinates) {
        if (_destroyed) return
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
        if (_destroyed) return
        if (tag.isEmpty()) return
        val holder = _targetLayoutCallbackHolder[tag] ?: hashSetOf<(LayoutCoordinates?) -> Unit>().also {
            _targetLayoutCallbackHolder[tag] = it
        }
        if (holder.add(callback)) {
            callback(_targetLayoutHolder[tag])
        }
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

    fun destroy() {
        _destroyed = true
        _layerHolder.toTypedArray().forEach {
            destroyLayer(it)
        }
        _layerHolder.clear()
        _attachedLayerHolder.clear()
    }

    private fun destroyLayer(layer: FLayer) {
        if (_layerHolder.remove(layer)) {
            _attachedLayerHolder.remove(layer)
            layer.onDestroy(this)
            check(layer._layerContainer == null)
        }
    }
}

internal inline fun logMsg(isDebug: Boolean, block: () -> String) {
    if (isDebug) {
        Log.i("FLayer", block())
    }
}

private suspend fun AwaitPointerEventScope.layerAwaitFirstDown(
    pass: PointerEventPass
): PointerInputChange {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(pass)
    } while (
        !event.changes.all { it.changedToDown() }
    )
    return event.changes[0]
}