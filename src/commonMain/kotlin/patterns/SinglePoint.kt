package patterns

import GeneralSettings
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.ShapeContour

data class SinglePoint(val point: Point) : Pattern() {
    override val contour = ShapeContour.fromPoints(listOf(point.pos), true)
    override val points = listOf(point)
    override val coverRadius = 0.0
    override fun isValid(gs: GeneralSettings) = true
}