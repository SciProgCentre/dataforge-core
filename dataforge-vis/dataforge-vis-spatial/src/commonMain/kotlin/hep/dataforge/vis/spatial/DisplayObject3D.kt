package hep.dataforge.vis.spatial

import hep.dataforge.meta.Meta
import hep.dataforge.vis.DisplayLeaf
import hep.dataforge.vis.DisplayObject
import hep.dataforge.vis.double


open class DisplayObject3D(parent: DisplayObject?, type: String, meta: Meta) : DisplayLeaf(parent, type, meta) {
    var x by double(0.0)
    var y by double(0.0)
    var z by double(0.0)

    companion object {
        const val TYPE = "geometry.spatial"
    }
}

class Box3D(parent: DisplayObject?, meta: Meta) : DisplayObject3D(parent,
    TYPE, meta) {
    var xSize by double(1.0)
    var ySize by double(1.0)
    var zSize by double(1.0)

    companion object {
        const val TYPE = "geometry.spatial.box"
    }
}
