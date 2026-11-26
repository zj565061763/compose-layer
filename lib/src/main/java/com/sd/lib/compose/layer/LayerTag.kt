package com.sd.lib.compose.layer

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.UnplacedAwareModifierNode
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
) : Modifier.Node(),
  GlobalPositionAwareModifierNode,
  UnplacedAwareModifierNode,
  CompositionLocalConsumerModifierNode {

  private var _tag: String = tag
  private var _container: ContainerForComposable? = null
  private var _coordinates: LayoutCoordinates? = null

  fun updateTag(tag: String) {
    if (_tag == tag) return
    val container = _container
    container?.removeTarget(tag = _tag, owner = this)
    _tag = tag
    if (container != null) {
      require(tag.isNotEmpty()) { "tag is empty." }
      _coordinates?.takeIf { it.isAttached }?.also {
        container.addTarget(tag = tag, owner = this, layoutCoordinates = it)
      }
    }
  }

  @Suppress("SuspiciousCompositionLocalModifierRead")
  override fun onAttach() {
    val container = currentValueOf(LocalContainerForComposable)
      ?: if (currentValueOf(LocalInspectionMode)) return else error("Not in LayerContainer scope.")
    require(_tag.isNotEmpty()) { "tag is empty." }
    _container = container
  }

  override fun onDetach() {
    _container?.removeTarget(tag = _tag, owner = this)
    _container = null
    _coordinates = null
  }

  override fun onUnplaced() {
    _coordinates = null
    _container?.removeTarget(tag = _tag, owner = this)
  }

  override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
    _coordinates = coordinates
    _container?.addTarget(tag = _tag, owner = this, layoutCoordinates = coordinates)
  }
}
