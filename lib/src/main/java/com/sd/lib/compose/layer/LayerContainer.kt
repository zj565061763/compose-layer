package com.sd.lib.compose.layer

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
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
                            awaitEachGesture {
                                if (!layerContainer.hasAttachedLayer) pointerInputStarted = false
                                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                layerContainer.processDownEvent(down)
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
 * 把当前元素设置为目标，并绑定容器作用域内唯一的[tag]
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

    private val _attachedLayerHolder: MutableList<LayerImpl> = mutableStateListOf()
    private val _sortedLayerHolder by derivedStateOf {
        _attachedLayerHolder.sortedBy { it.zIndexState ?: 0f }
    }

    /** 容器的布局信息 */
    private var _containerLayout: LayoutCoordinates? = null
    private val _containerLayoutCallbackHolder: MutableSet<(LayoutCoordinates?) -> Unit> = hashSetOf()

    /** 目标的布局信息 */
    private val _targetLayoutHolder: MutableMap<String, LayoutCoordinates> = hashMapOf()
    private val _targetLayoutCallbackHolder: MutableMap<String, MutableSet<(LayoutCoordinates?) -> Unit>> = hashMapOf()

    val hasAttachedLayer by derivedStateOf { _attachedLayerHolder.isNotEmpty() }

    @Composable
    fun Layers() {
        _sortedLayerHolder.forEach { item ->
            val zIndex = item.zIndexState ?: 0f
            Box(modifier = Modifier.zIndex(zIndex)) {
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

    fun initLayer(layer: LayerImpl) {
        if (_destroyed) return
        if (layer.layerContainer === this) return

        layer.destroy()
        layer.onInit(this)
        check(layer.layerContainer === this)
    }

    fun destroyLayer(layer: LayerImpl) {
        if (layer.layerContainer === this) {
            _attachedLayerHolder.remove(layer)
            layer.onDestroy(this)
            check(layer.layerContainer == null)
        }
    }

    fun attachLayer(layer: LayerImpl) {
        if (_destroyed) return
        if (layer.layerContainer === this) {
            if (!_attachedLayerHolder.contains(layer)) {
                _attachedLayerHolder.add(layer)
            }
        }
    }

    fun detachLayer(layer: LayerImpl): Boolean {
        return _attachedLayerHolder.remove(layer)
    }

    fun processDownEvent(event: PointerInputChange) {
        if (_destroyed) return
        val copyHolder = _sortedLayerHolder.toTypedArray()
        for (index in copyHolder.lastIndex downTo 0) {
            val layer = copyHolder[index]
            layer.processDownEvent(event)
            if (event.isConsumed) break
        }
    }

    //---------- container ----------

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

    //---------- target ----------

    fun addTarget(tag: String, layoutCoordinates: LayoutCoordinates) {
        if (_destroyed) return
        if (tag.isEmpty()) return

        _targetLayoutHolder[tag]?.let { old ->
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

        _attachedLayerHolder.toTypedArray().forEach {
            destroyLayer(it)
        }
        _attachedLayerHolder.clear()

        _containerLayout = null
        _containerLayoutCallbackHolder.clear()

        _targetLayoutHolder.clear()
        _targetLayoutCallbackHolder.clear()
    }
}

internal inline fun logMsg(isDebug: Boolean, block: () -> String) {
    if (isDebug) {
        Log.i("FLayer", block())
    }
}