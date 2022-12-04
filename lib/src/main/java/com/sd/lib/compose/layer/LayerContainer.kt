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
    CompositionLocalProvider(LocalLayerContainer provides layerContainer) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    layerContainer.updateContainerLayout(it)
                }
                .let {
                    if (layerContainer.hasAttachedLayer) {
                        it.pointerInput(Unit) {
                            forEachGesture {
                                awaitPointerEventScope {
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
 * 创建并记住[Layer]
 */
@Composable
fun rememberLayer(debug: Boolean = false): Layer {
    val layerContainer = checkNotNull(LocalLayerContainer.current) {
        "CompositionLocal LocalLayerContainer not present"
    }
    return layerContainer.rememberLayer(debug)
}

/**
 * 创建并记住[TargetLayer]
 */
@Composable
fun rememberTargetLayer(debug: Boolean = false): TargetLayer {
    val layerContainer = checkNotNull(LocalLayerContainer.current) {
        "CompositionLocal LocalLayerContainer not present"
    }
    return layerContainer.rememberTargetLayer(debug)
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

private val LocalLayerContainer = staticCompositionLocalOf<LayerContainer?> { null }

internal class LayerContainer {
    private val _attachedLayerHolder: MutableList<LayerImpl> = mutableStateListOf()

    private val _targetLayoutHolder: MutableMap<String, LayoutCoordinates> = hashMapOf()
    private val _targetLayoutCallbackHolder: MutableMap<String, MutableSet<(LayoutCoordinates?) -> Unit>> = hashMapOf()

    private var _containerLayout: LayoutCoordinates? = null
    private val _containerLayoutCallbackHolder: MutableSet<(LayoutCoordinates?) -> Unit> = hashSetOf()

    val hasAttachedLayer by derivedStateOf { _attachedLayerHolder.isNotEmpty() }

    @Composable
    fun rememberLayer(debug: Boolean): Layer {
        val layer = remember {
            LayerImpl().also { initLayer(it, debug) }
        }.apply {
            this.isDebug = debug
        }

        DisposableEffect(layer) {
            onDispose {
                destroyLayer(layer)
            }
        }
        return layer
    }

    @Composable
    fun rememberTargetLayer(debug: Boolean): TargetLayer {
        val layer = remember {
            TargetLayerImpl().also { initLayer(it, debug) }
        }.apply {
            this.isDebug = debug
        }

        DisposableEffect(layer) {
            onDispose {
                destroyLayer(layer)
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

            val behavior = item.dialogBehavior
            if (behavior.enabled) {
                BackHandler(item.isVisibleState) {
                    if (behavior.cancelable) {
                        item.detach()
                    }
                }
            }
        }
    }

    private fun initLayer(layer: LayerImpl, debug: Boolean) {
        layer.isDebug = debug
        layer.onCreate(this)
    }

    private fun destroyLayer(layer: LayerImpl) {
        _attachedLayerHolder.remove(layer)
        layer.onDestroy(this)
    }

    fun attachLayer(layer: LayerImpl) {
        if (!_attachedLayerHolder.contains(layer)) {
            _attachedLayerHolder.add(layer)
        }
    }

    fun detachLayer(layer: LayerImpl) {
        _attachedLayerHolder.remove(layer)
    }

    fun processDownEvent(event: PointerInputChange) {
        val copyHolder = _attachedLayerHolder.toTypedArray()
        for (index in copyHolder.lastIndex downTo 0) {
            val layer = copyHolder[index]
            layer.processDownEvent(event)
            if (event.isConsumed) break
        }
    }

    fun updateContainerLayout(layoutCoordinates: LayoutCoordinates) {
        _containerLayout = layoutCoordinates
        _containerLayoutCallbackHolder.toTypedArray().forEach {
            it.invoke(layoutCoordinates)
        }
    }

    fun registerContainerLayoutCallback(callback: (LayoutCoordinates?) -> Unit) {
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