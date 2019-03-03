package hep.dataforge.vis.spatial

import hep.dataforge.context.Context
import hep.dataforge.io.Output
import hep.dataforge.meta.Meta
import hep.dataforge.meta.int
import hep.dataforge.vis.DisplayGroup
import hep.dataforge.vis.DisplayObject
import hep.dataforge.vis.DisplayObjectPropertyListener
import hep.dataforge.vis.transform
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.paint.Color
import org.fxyz3d.geometry.Point3D
import org.fxyz3d.shapes.primitives.CuboidMesh


/**
 * https://github.com/miho/JCSG for operations
 *
 */
class FXSpatialRenderer(override val context: Context) : Output<DisplayObject> {

    val canvas by lazy { Canvas3D() }

    private fun buildObject(obj: DisplayObject): Node {
        return when (obj) {
            is DisplayGroup -> Group(obj.children.map { buildObject(it) })
            is Box -> CuboidMesh(obj.xSize, obj.ySize, obj.zSize).apply {
                val listener = DisplayObjectPropertyListener(obj)
                this.center = Point3D(obj.x.toFloat(), obj.y.toFloat(), obj.z.toFloat())
                this.diffuseColorProperty().bind(listener["color"].transform {
                    //TODO Move to extension
                    val int = it.int ?: 0
                    val red = int and 0x00ff0000 shr 16
                    val green = int and 0x0000ff00 shr 8
                    val blue = int and 0x000000ff
                    return@transform Color.rgb(red, green, blue)
                })
            }
            else -> TODO()
        }
    }

    override fun render(obj: DisplayObject, meta: Meta) {
        canvas.world.children.add(buildObject(obj))
    }
}