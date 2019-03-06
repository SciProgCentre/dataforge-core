package hep.dataforge.vis.spatial

import hep.dataforge.vis.DisplayObjectPropertyListener
import javafx.scene.Group
import javafx.scene.Node
import org.fxyz3d.shapes.primitives.CuboidMesh
import tornadofx.*

/**
 * https://github.com/miho/JCSG for operations
 *
 */
class FX3DOutput(override val context: Context) : Output<Any> {
    val canvas by lazy { Canvas3D() }


    private fun buildNode(obj: Any): Node? {
        return when (obj) {
            is DisplayShape3D -> {
                val listener = DisplayObjectPropertyListener(obj)
                val x = listener["x"].float()
                val y = listener["y"].float()
                val z = listener["z"].float()
                val center = objectBinding(x, y, z) {
                    Point3D(x.value ?: 0f, y.value ?: 0f, z.value ?: 0f)
                }
                when (obj) {
                    is DisplayGroup3D -> Group(obj.children.map { buildNode(it) }).apply {
                        this.translateXProperty().bind(x)
                        this.translateYProperty().bind(y)
                        this.translateZProperty().bind(z)
                    }
                    is Box -> CuboidMesh(obj.xSize, obj.ySize, obj.zSize).apply {
                        this.centerProperty().bind(center)
                        this.materialProperty().bind(listener["color"].transform { it.material() })
                    }
                    else -> {
                        logger.error { "No renderer defined for ${obj::class}" }
                        null
                    }
                }
            }
            is DisplayGroup -> Group(obj.children.map { buildNode(it) }) // a logical group
            else -> {
                logger.error { "No renderer defined for ${obj::class}" }
                null
            }
        }
    }

    override fun render(obj: Any, meta: Meta) {
        buildNode(obj)?.let { canvas.world.children.add(it) }
    }
}