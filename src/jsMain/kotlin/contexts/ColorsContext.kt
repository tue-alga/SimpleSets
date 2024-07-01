package contexts

import org.openrndr.color.ColorRGBa
import react.createContext

interface Colors {
    val defaultColors: List<ColorRGBa>
    var colors: List<ColorRGBa>
}

val ColorsContext = createContext<Colors>()