package hep.dataforge.vis.spatial

import hep.dataforge.context.Context
import hep.dataforge.io.Output
import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.vis.DisplayGroup
import info.laht.threekt.WebGLRenderer
import info.laht.threekt.cameras.PerspectiveCamera
import info.laht.threekt.core.BufferGeometry
import info.laht.threekt.core.Object3D
import info.laht.threekt.external.controls.OrbitControls
import info.laht.threekt.extras.curves.CatmullRomCurve3
import info.laht.threekt.geometries.BoxBufferGeometry
import info.laht.threekt.lights.AmbientLight
import info.laht.threekt.materials.LineBasicMaterial
import info.laht.threekt.materials.MeshBasicMaterial
import info.laht.threekt.materials.MeshPhongMaterial
import info.laht.threekt.math.ColorConstants
import info.laht.threekt.math.Vector3
import info.laht.threekt.objects.Line
import info.laht.threekt.objects.Mesh
import info.laht.threekt.scenes.Scene
import org.w3c.dom.Element
import kotlin.browser.window

class ThreeOutput(override val context: Context) : Output<Any> {

    private val renderer = WebGLRenderer { antialias = true }.apply {
        setClearColor(ColorConstants.skyblue, 1)
        setSize(window.innerWidth, window.innerHeight)
    }

    val scene: Scene = Scene().apply {
        add(AmbientLight())
    }

    val camera = PerspectiveCamera(
        75,
        window.innerWidth.toDouble() / window.innerHeight,
        0.1,
        10000
    ).apply {
        position.setZ(1000)
    }

    val controls: OrbitControls = OrbitControls(camera, renderer.domElement)

    val root get() = renderer.domElement

    private fun animate() {
        window.requestAnimationFrame {
            animate()
        }
        renderer.render(scene, camera)
    }

    fun start(element: Element) {
        window.addEventListener("resize", {
            camera.aspect = window.innerWidth.toDouble() / window.innerHeight;
            camera.updateProjectionMatrix();

            renderer.setSize(window.innerWidth, window.innerHeight)
        }, false)
        element.appendChild(root)
        animate()
    }


    private fun buildNode(obj: Any): Object3D? {
        return when (obj) {
            is DisplayShape3D -> {
//                val listener = DisplayObjectPropertyListener(obj)
//                val x = listener["x"].float()
//                val y = listener["y"].float()
//                val z = listener["z"].float()
//                val center = objectBinding(x, y, z) {
//                    Vector3(x.value ?: 0f, y.value ?: 0f, z.value ?: 0f)
//                }
                when (obj) {
                    is DisplayGroup3D -> Group(obj.children.mapNotNull { buildNode(it) }).apply {
                        this.translateX(obj.x)
                        this.translateY(obj.y)
                        this.translateZ(obj.z)
                    }
                    is Box -> {
                        //TODO add bindings
                        val geometry = BoxBufferGeometry(obj.xSize, obj.ySize, obj.zSize)
                            .translate(obj.x, obj.y, obj.z)
                        Mesh(geometry, obj.properties["color"].material())
                    }
                    else -> {
                        logger.error { "No renderer defined for ${obj::class}" }
                        null
                    }
                }
            }
            is DisplayGroup -> Group(obj.children.mapNotNull { buildNode(it) }) // a logical group
            else -> {
                logger.error { "No renderer defined for ${obj::class}" }
                null
            }
        }
    }

    override fun render(obj: Any, meta: Meta) {
        buildNode(obj)?.let { scene.add(it) }
    }

//    init {
//        val cube: Mesh
//
//        cube = Mesh(
//            BoxBufferGeometry(1, 1, 1),
//            MeshPhongMaterial().apply {
//                this.color.set(ColorConstants.darkgreen)
//            }
//        ).also(scene::add)
//
//        Mesh(cube.geometry as BufferGeometry,
//            MeshBasicMaterial().apply {
//                this.wireframe = true
//                this.color.set(ColorConstants.black)
//            }
//        ).also(cube::add)
//
//        val points = CatmullRomCurve3(
//            arrayOf(
//                Vector3(-10, 0, 10),
//                Vector3(-5, 5, 5),
//                Vector3(0, 0, 0),
//                Vector3(5, -5, 5),
//                Vector3(10, 0, 10)
//            )
//        ).getPoints(50)
//
//        val geometry = BufferGeometry().setFromPoints(points)
//
//        val material = LineBasicMaterial().apply {
//            color.set(0xff0000)
//        }
//
//        // Create the final object to add to the scene
//        Line(geometry, material).apply(scene::add)
//    }

}