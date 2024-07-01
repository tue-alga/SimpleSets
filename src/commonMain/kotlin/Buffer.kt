import org.openrndr.math.*
import org.openrndr.shape.Segment
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import patterns.angleBetween

expect fun Shape.buffer(radius: Double): Shape

fun ShapeContour.fix(eps: Double = 0.0): ShapeContour {
    val newSegments = mutableListOf<Segment>()
    var sumLength = 0.0
    for (s in segments) {
        val l = s.length
        if (s.control.isEmpty() && l > eps || sumLength > eps) {
            if (newSegments.size > 0) {
                if (newSegments.last().end != s.start) {
                    newSegments.add(Segment(newSegments.last().end, s.start))
                }
            }
            newSegments.add(s)
            sumLength = 0.0
        } else {
            sumLength += l
        }
    }
    return ShapeContour.fromSegments(newSegments, closed, polarity)
}

fun ShapeContour.buffer(radius: Double): Shape = shape.buffer(radius)

fun ShapeContour.removeSpikes(): ShapeContour {
    val newSegments = mutableListOf<Segment>()

    for (i in segments.indices) {
        val p = segments[(i - 1).mod(segments.size)]
        val c = segments[i]
        val n = segments[(i + 1).mod(segments.size)]
        val p1 = p.start
        val p2 = c.start
        val p3 = n.start
        val p4 = n.end

        if ((i < segments.size / 10 || i > 9 * segments.size / 10) || angleBetween(p2 - p1, p3 - p2) < 45.0.asRadians && angleBetween(p3 - p2, p4 - p3) < 45.0.asRadians) {
            if (newSegments.size > 0) {
                if (newSegments.last().end != c.start) {
                    newSegments.add(Segment(newSegments.last().end, c.start))
                }
            }
            newSegments.add(c)
        }
    }

    return ShapeContour.fromSegments(newSegments, closed, polarity)
}
