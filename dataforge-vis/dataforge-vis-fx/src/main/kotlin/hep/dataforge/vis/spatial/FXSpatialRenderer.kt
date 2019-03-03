package hep.dataforge.vis.spatial

import hep.dataforge.context.Context
import hep.dataforge.io.Output
import hep.dataforge.meta.Meta
import hep.dataforge.meta.int
import hep.dataforge.vis.DisplayGroup
import hep.dataforge.vis.DisplayObject
import hep.dataforge.vis.DisplayObjectPropertyListener
import hep.dataforge.vis.transform
import javafx.event.EventHandler
import javafx.scene.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import org.fxyz3d.geometry.Point3D
import org.fxyz3d.shapes.primitives.CuboidMesh
import org.fxyz3d.utils.CameraTransformer


/**
 * https://github.com/miho/JCSG for operations
 *
 * TODO move world and camera boilerplate to another class
 */
class FXSpatialRenderer(override val context: Context) : Output<DisplayObject> {

    private val world: Group = Group()

    private val camera = PerspectiveCamera().apply {
        nearClip = CAMERA_NEAR_CLIP
        farClip = CAMERA_FAR_CLIP
        translateZ = CAMERA_INITIAL_DISTANCE
    }

    val cameraShift = CameraTransformer().apply {
        val cameraFlip = CameraTransformer()
        cameraFlip.children.add(camera)
        cameraFlip.setRotateZ(180.0)
        children.add(cameraFlip)
    }

    val cameraRotation = CameraTransformer().apply {
        children.add(cameraShift)
        ry.angle = CAMERA_INITIAL_Y_ANGLE
        rx.angle = CAMERA_INITIAL_X_ANGLE
        rz.angle = CAMERA_INITIAL_Z_ANGLE
    }


    val canvas: SubScene = SubScene(
        Group(world, cameraRotation).apply { DepthTest.ENABLE },
        1024.0,
        768.0,
        true,
        SceneAntialiasing.BALANCED
    ).apply {
        fill = Color.GREY
        this.camera = this@FXSpatialRenderer.camera
        id = "canvas"
        handleKeyboard(this)
        handleMouse(this)
    }

    private fun buildObject(obj: DisplayObject): Node {
        return when (obj) {
            is DisplayGroup -> Group(obj.children.map { buildObject(it) })
            is Box -> CuboidMesh(obj.xSize, obj.ySize, obj.zSize).apply {
                val listener = DisplayObjectPropertyListener(obj)
                this.center = Point3D(obj.x.toFloat(), obj.y.toFloat(), obj.z.toFloat())
                this.diffuseColorProperty().bind(listener["color"].transform {
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
        world.children.add(buildObject(obj))
    }

    private fun handleKeyboard(scene: SubScene) {
        scene.onKeyPressed = EventHandler<KeyEvent> { event ->
            if (event.isControlDown) {
                when (event.code) {
                    KeyCode.Z -> {
                        cameraShift.t.x = 0.0
                        cameraShift.t.y = 0.0
                        camera.translateZ = CAMERA_INITIAL_DISTANCE
                        cameraRotation.ry.angle = CAMERA_INITIAL_Y_ANGLE
                        cameraRotation.rx.angle = CAMERA_INITIAL_X_ANGLE
                    }
//                    KeyCode.X -> axisGroup.isVisible = !axisGroup.isVisible
//                    KeyCode.S -> snapshot()
//                    KeyCode.DIGIT1 -> pixelMap.filterKeys { it.getLayerNumber() == 1 }.values.forEach {
//                        toggleTransparency(
//                            it
//                        )
//                    }
//                    KeyCode.DIGIT2 -> pixelMap.filterKeys { it.getLayerNumber() == 2 }.values.forEach {
//                        toggleTransparency(
//                            it
//                        )
//                    }
//                    KeyCode.DIGIT3 -> pixelMap.filterKeys { it.getLayerNumber() == 3 }.values.forEach {
//                        toggleTransparency(
//                            it
//                        )
//                    }
                    else -> {
                    }//do nothing
                }
            }
        }
    }

    private fun handleMouse(scene: SubScene) {

        var mousePosX: Double = 0.0
        var mousePosY: Double = 0.0
        var mouseOldX: Double = 0.0
        var mouseOldY: Double = 0.0
        var mouseDeltaX: Double = 0.0
        var mouseDeltaY: Double = 0.0

        scene.onMousePressed = EventHandler<MouseEvent> { me ->
            mousePosX = me.sceneX
            mousePosY = me.sceneY
            mouseOldX = me.sceneX
            mouseOldY = me.sceneY
        }

        scene.onMouseDragged = EventHandler<MouseEvent> { me ->
            mouseOldX = mousePosX
            mouseOldY = mousePosY
            mousePosX = me.sceneX
            mousePosY = me.sceneY
            mouseDeltaX = mousePosX - mouseOldX
            mouseDeltaY = mousePosY - mouseOldY

            val modifier = when {
                me.isControlDown -> CONTROL_MULTIPLIER
                me.isShiftDown -> SHIFT_MULTIPLIER
                else -> 1.0
            }

            if (me.isPrimaryButtonDown) {
                cameraRotation.rz.angle =
                    cameraRotation.rz.angle + mouseDeltaX * MOUSE_SPEED * modifier * ROTATION_SPEED
                cameraRotation.rx.angle =
                    cameraRotation.rx.angle + mouseDeltaY * MOUSE_SPEED * modifier * ROTATION_SPEED
                //                } else if (me.isSecondaryButtonDown()) {
                //                    double z = camera.getTranslateZ();
                //                    double newZ = z + mouseDeltaX * MOUSE_SPEED * modifier*100;
                //                    camera.setTranslateZ(newZ);
            } else if (me.isSecondaryButtonDown) {
                cameraShift.t.x = cameraShift.t.x + mouseDeltaX * MOUSE_SPEED * modifier * TRACK_SPEED
                cameraShift.t.y = cameraShift.t.y + mouseDeltaY * MOUSE_SPEED * modifier * TRACK_SPEED
            }
        }
        scene.onScroll = EventHandler<ScrollEvent> { event ->
            val z = camera.translateZ
            val newZ = z + MOUSE_SPEED * event.deltaY * RESIZE_SPEED
            camera.translateZ = newZ
        }
    }

    companion object {
        private const val CAMERA_INITIAL_DISTANCE = -4500.0
        private const val CAMERA_INITIAL_X_ANGLE = -50.0
        private const val CAMERA_INITIAL_Y_ANGLE = 0.0
        private const val CAMERA_INITIAL_Z_ANGLE = -210.0
        private const val CAMERA_NEAR_CLIP = 0.1
        private const val CAMERA_FAR_CLIP = 10000.0
        private const val AXIS_LENGTH = 2000.0
        private const val CONTROL_MULTIPLIER = 0.1
        private const val SHIFT_MULTIPLIER = 10.0
        private const val MOUSE_SPEED = 0.1
        private const val ROTATION_SPEED = 2.0
        private const val TRACK_SPEED = 6.0
        private const val RESIZE_SPEED = 50.0
        private const val LINE_WIDTH = 3.0

        private val redMaterial = PhongMaterial().apply {
            diffuseColor = Color.DARKRED
            specularColor = Color.RED
        }

        private val whiteMaterial = PhongMaterial().apply {
            diffuseColor = Color.WHITE
            specularColor = Color.LIGHTBLUE
        }

        private val greyMaterial = PhongMaterial().apply {
            diffuseColor = Color.DARKGREY
            specularColor = Color.GREY
        }

        private val blueMaterial = PhongMaterial(Color.BLUE)

    }
}