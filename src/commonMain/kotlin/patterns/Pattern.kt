package patterns

import GeneralSettings
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.ShapeContour

abstract class Pattern {
    abstract val contour: ShapeContour
    abstract val points: List<Point>
    abstract val coverRadius: Double
    abstract fun isValid(gs: GeneralSettings): Boolean

    val type: Int get() = points.first().type
}
