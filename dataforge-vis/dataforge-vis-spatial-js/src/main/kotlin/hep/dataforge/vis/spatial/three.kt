package hep.dataforge.vis.spatial

import info.laht.threekt.core.Object3D

/**
 * Utility methods for three.kt.
 * TODO move to three project
 */

@Suppress("FunctionName")
fun Group(children: Collection<Object3D>) = info.laht.threekt.objects.Group().apply {
    children.forEach { this.add(it) }
}