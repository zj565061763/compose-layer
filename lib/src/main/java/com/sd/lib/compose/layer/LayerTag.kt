package com.sd.lib.compose.layer

import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalInspectionMode

internal data class LayerTagElement(
  val tag: String,
) : ModifierNodeElement<LayerTagNode>() {
  override fun create(): LayerTagNode {
    return LayerTagNode(tag)
  }

  override fun update(node: LayerTagNode) {
    node.updateTag(tag)
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "layerTag"
    properties["tag"] = tag
  }
}

internal class LayerTagNode(
  tag: String,
) : LayoutTrackNode(),
  CompositionLocalConsumerModifierNode {

  private var _tag: String = tag
  private var _container: ContainerForComposable? = null

  fun updateTag(tag: String) {
    require(tag.isNotEmpty()) { "tag is empty." }
    if (_tag == tag) return
    val container = _container
    container?.detachTargetNode(tag = _tag, node = this)
    _tag = tag
    if (isPlaced) {
      container?.attachTargetNode(tag = tag, node = this)
    }
  }

  @Suppress("SuspiciousCompositionLocalModifierRead")
  override fun onAttach() {
    val container = currentValueOf(LocalContainerForComposable)
      ?: if (currentValueOf(LocalInspectionMode)) return else error("Not in LayerContainer scope.")
    bindContainer(container)
  }

  fun bindContainer(container: ContainerForComposable) {
    _container = container
  }

  override fun onPlaced(coordinates: LayoutCoordinates) {
    super.onPlaced(coordinates)
    _container?.attachTargetNode(tag = _tag, node = this)
  }

  override fun onUnplaced() {
    _container?.detachTargetNode(tag = _tag, node = this)
    super.onUnplaced()
  }

  override fun onDetach() {
    _container?.detachTargetNode(tag = _tag, node = this)
    _container = null
    super.onDetach()
  }

  override fun onRectChanged(rect: LayoutRect?) {
    _container?.onTargetRectChanged(tag = _tag, node = this, rect = rect)
  }

  init {
    require(tag.isNotEmpty()) { "tag is empty." }
  }
}
