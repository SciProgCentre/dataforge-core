package hep.dataforge.vis.spatial

import hep.dataforge.meta.EmptyMeta
import hep.dataforge.meta.Meta
import hep.dataforge.vis.DisplayGroup
import hep.dataforge.vis.DisplayObject

class Extruded(parent: DisplayObject?, meta: Meta) : DisplayShape3D(parent, TYPE, meta) {


    companion object {
        const val TYPE = "geometry.spatial.extruded"
    }
}

fun DisplayGroup.extrude(meta: Meta = EmptyMeta, action: Extruded.() -> Unit = {}) =
    Extruded(this, meta).apply(action).also { addChild(it) }