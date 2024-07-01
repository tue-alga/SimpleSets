import org.openrndr.color.ColorRGBa

fun ColorRGBa.toSvgString() = "rgb(${(r*255).toInt()}, ${(g*255).toInt()}, ${(b*255).toInt()})"
fun ColorRGBa.toHex(): String = "#" +
        (1 shl 24 or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt())
            .toString(16).drop(1)