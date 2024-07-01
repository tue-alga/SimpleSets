package geometric

import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.contour

data class Arc(val circle: Circle, val start: Vector2, val end: Vector2) {
    val contour by lazy {
        contour {
            val largeArcFlag = orientation(circle.center, start, end) != Orientation.RIGHT
            moveTo(start)
            arcTo(circle.radius, circle.radius, 90.0, largeArcFlag=largeArcFlag, sweepFlag=false, end)
        }
    }
}