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
    if (_tag == tag) return
    val container = _container
    container?.detachTargetNode(tag = _tag, node = this)
    _tag = tag
    if (container != null) {
      require(tag.isNotEmpty()) { "tag is empty." }
      container.attachTargetNode(tag = tag, node = this)
    }
  }

  @Suppress("SuspiciousCompositionLocalModifierRead")
  override fun onAttach() {
    val container = currentValueOf(LocalContainerForComposable)
      ?: if (currentValueOf(LocalInspectionMode)) return else error("Not in LayerContainer scope.")
    require(_tag.isNotEmpty()) { "tag is empty." }
    _container = container
    container.attachTargetNode(tag = _tag, node = this)
  }

  override fun onPlaced(coordinates: LayoutCoordinates) {
    val wasPlaced = isPlaced
    super.onPlaced(coordinates)
    if (!wasPlaced) {
      _container?.onTargetNodePlaced(tag = _tag, node = this)
    }
  }

  override fun onNodeUnplaced() {
    _container?.onTargetRectChanged(tag = _tag, node = this, rect = null)
  }

  override fun onDetach() {
    _container?.detachTargetNode(tag = _tag, node = this)
    _container = null
    super.onDetach()
  }

  override fun onRectChanged(rect: LayoutRect?) {
    _container?.onTargetRectChanged(tag = _tag, node = this, rect = rect)
  }
}
