package hep.dataforge.vis.spatial

import hep.dataforge.io.Output
import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import hep.dataforge.vis.*
import hep.dataforge.vis.DisplayObject.Companion.DEFAULT_TYPE


open class DisplayObject3D(parent: DisplayObject?, type: String, meta: Meta) : DisplayLeaf(parent, type, meta) {
    var x by double(0.0)
    var y by double(0.0)
    var z by double(0.0)

    companion object {
        const val TYPE = "geometry.spatial"
    }
}

fun DisplayGroup.group(meta: Meta = EmptyMeta, action: DisplayGroup.() -> Unit = {}) =
    DisplayNode(this, DEFAULT_TYPE, meta).apply(action).also{addChild(it)}


fun Output<DisplayObject>.render(meta: Meta = EmptyMeta, action: DisplayGroup.() -> Unit) =
    render(DisplayNode(null, DEFAULT_TYPE, EmptyMeta).apply(action), meta)
