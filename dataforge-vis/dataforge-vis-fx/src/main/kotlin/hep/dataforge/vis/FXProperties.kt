package hep.dataforge.vis

import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial

object Materials{
    val RED = PhongMaterial().apply {
        diffuseColor = Color.DARKRED
        specularColor = Color.RED
    }

    val WHITE = PhongMaterial().apply {
        diffuseColor = Color.WHITE
        specularColor = Color.LIGHTBLUE
    }

    val GREY = PhongMaterial().apply {
        diffuseColor = Color.DARKGREY
        specularColor = Color.GREY
    }

    val BLUE = PhongMaterial(Color.BLUE)
}