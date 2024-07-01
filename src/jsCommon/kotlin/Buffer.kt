import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.scale
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import urbanistic.clipper.*

actual fun Shape.buffer(radius: Double): Shape {
    val scaling = 100000.0
    val matrix = Matrix44.scale(Vector3(scaling, scaling, scaling))

    val off = ClipperOffset(arcTolerance = 0.001 * scaling)
    val offsettedPaths = Paths()

    for (c in contours) {
        val pts = c.transform(matrix).sampleLinear(scaling * 0.02).segments.map { it.start }// + c.segments.last().end
        val longPts: Path = pts.map { LongPoint(it.x.toLong(), it.y.toLong()) }.toMutableList()
        off.addPath(longPts, JoinType.Round, EndType.ClosedPolygon)
    }

    off.execute(offsettedPaths, radius * scaling)

    return Shape(offsettedPaths.map {
        ShapeContour.fromPoints(it.map {
            Vector2(it.x / scaling, it.y / scaling)
        }, true).fix(0.1)
    }.filter { !it.empty })
}