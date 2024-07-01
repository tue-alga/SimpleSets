package dilated

import GeneralSettings
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import patterns.Pattern
import patterns.Point

class ShapeHighlight(val shape: Shape, override val contour: ShapeContour, override val points: List<Point>, expandRadius: Double)
    : Pattern() {
    override val coverRadius: Double = 0.0
    override fun isValid(gs: GeneralSettings) = true
}