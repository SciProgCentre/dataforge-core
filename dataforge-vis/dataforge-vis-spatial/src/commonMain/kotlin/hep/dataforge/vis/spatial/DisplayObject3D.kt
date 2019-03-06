package hep.dataforge.vis.spatial

import hep.dataforge.io.Output
import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import hep.dataforge.vis.*
import hep.dataforge.vis.DisplayObject.Companion.DEFAULT_TYPE


interface DisplayObject3D : DisplayObject {
    val x: Double
    val y: Double
    val z: Double

    companion object {
        const val TYPE = "geometry.spatial"
    }
}

open class DisplayShape3D(parent: DisplayObject?, type: String, meta: Meta) :
    DisplayLeaf(parent, type, meta), DisplayObject3D {
    override var x by double(0.0, inherited = false)
    override var y by double(0.0, inherited = false)
    override var z by double(0.0, inherited = false)
}

class DisplayGroup3D(parent: DisplayObject?, type: String, meta: Meta) : DisplayNode(parent, type, meta),
    DisplayObject3D {
    override var x by double(0.0, inherited = false)
    override var y by double(0.0, inherited = false)
    override var z by double(0.0, inherited = false)
}

fun DisplayGroup.group(meta: Meta = EmptyMeta, action: DisplayGroup.() -> Unit = {}) =
    DisplayNode(this, DEFAULT_TYPE, meta).apply(action).also { addChild(it) }


fun Output<DisplayObject>.render(meta: Meta = EmptyMeta, action: DisplayGroup.() -> Unit) =
    render(DisplayNode(null, DEFAULT_TYPE, EmptyMeta).apply(action), meta)
