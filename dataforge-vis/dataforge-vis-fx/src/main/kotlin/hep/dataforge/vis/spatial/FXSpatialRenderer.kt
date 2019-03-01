package hep.dataforge.vis.spatial

import hep.dataforge.context.Context
import hep.dataforge.io.Output
import hep.dataforge.meta.Meta
import javafx.scene.*
import javafx.scene.paint.Color
import org.fxyz3d.geometry.Point3D
import org.fxyz3d.shapes.primitives.CuboidMesh
import org.fxyz3d.utils.CameraTransformer

class FXSpatialRenderer(override val context: Context) : Output<DisplayObject3D> {

    private val world: Group = Group()

    val camera = PerspectiveCamera()

    val cameraTransform = CameraTransformer().apply {
        children.add(camera)
    }

    val canvas: SubScene = SubScene(
        Group(world, cameraTransform).apply { DepthTest.ENABLE },
        1024.0,
        768.0,
        true,
        SceneAntialiasing.BALANCED
    ).apply {
        fill = Color.GREY
        this.camera = this@FXSpatialRenderer.camera
        id = "canvas"
    }

    private fun buildObject(obj: DisplayObject3D): Node {
        val center = Point3D(obj.x.toFloat(), obj.y.toFloat(), obj.z.toFloat())
        return when (obj) {
            is Box3D -> CuboidMesh(obj.xSize, obj.ySize, obj.zSize).apply { this.center = center }
            else -> TODO()
        }
    }

    override fun render(obj: DisplayObject3D, meta: Meta) {
        world.children.add(buildObject(obj))
    }
}