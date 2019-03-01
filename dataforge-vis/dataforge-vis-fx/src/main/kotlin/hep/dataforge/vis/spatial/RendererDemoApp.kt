package hep.dataforge.vis.spatial

import hep.dataforge.context.Global
import hep.dataforge.meta.EmptyMeta
import javafx.scene.Parent
import tornadofx.*


class RendererDemoApp: App(RendererDemoView::class)


class RendererDemoView: View(){
    val renderer = FXSpatialRenderer(Global)
    override val root: Parent = borderpane{
        center = renderer.canvas
    }

    init {
        val cube = Box3D(null, EmptyMeta).apply {
            xSize = 100.0
            ySize = 100.0
            zSize = 100.0
        }
        renderer.render(cube)

        renderer.camera.apply {
            nearClip = 0.1
            farClip = 10000.0
            translateX = -200.0
            translateY = -200.0
            fieldOfView = 20.0
        }

        renderer.cameraTransform.apply{
            ry.angle = -30.0
            rx.angle = -15.0
        }
    }
}


fun main() {
    launch<RendererDemoApp>()
}