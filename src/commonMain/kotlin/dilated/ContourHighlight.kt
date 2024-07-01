package dilated

import GeneralSettings
import org.openrndr.shape.ShapeContour
import patterns.Pattern
import patterns.Point

class ContourHighlight(override val contour: ShapeContour, override val points: List<Point>)
    : Pattern() {
    override val coverRadius: Double = 0.0
    override fun isValid(gs: GeneralSettings): Boolean = true
}