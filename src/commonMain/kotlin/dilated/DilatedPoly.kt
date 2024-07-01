package dilated

import GeneralSettings
import org.openrndr.shape.Circle
import patterns.*

// Dilated polygon, polyline, or single point
abstract class DilatedPoly<P: Pattern>(val original: P, val expandRadius: Double): Pattern() {
    final override val points = original.points
    override val coverRadius = original.coverRadius
    override fun isValid(gs: GeneralSettings) = original.isValid(gs)

    val circles: List<Circle> = points.map { Circle(it.pos, expandRadius) }
}

fun Pattern.dilate(expandRadius: Double) = when(this) {
    is Island -> dilate(expandRadius)
    is Bank -> dilate(expandRadius)
    is SinglePoint -> dilate(expandRadius)
    is Matching -> dilate(expandRadius)
    else -> error("Unsupported pattern $this")
}
