import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.shape.CubicBezierCurve
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.scale
import org.openrndr.shape.*
import urbanistic.clipper.*

fun Vector2.toCoordinate() = Coordinate(x, y)

fun Coordinate.toVector2() = Vector2(x, y)

fun ShapeContour.toJtsGeometry(): Geometry {
    val gf = GeometryFactory()
    val pts = segments.map { it.start } + segments.last().end
    val coords = pts.map { it.toCoordinate() }.toTypedArray()
//    return if (closed) {
//        gf.createPolygon(gf.createLinearRing(coords + coords.first()), emptyList<LinearRing>().toTypedArray())
//    } else {
        val controls = segments.flatMap { it.cubic.control.toList() }.map { it.toCoordinate() }.toTypedArray()
        return CubicBezierCurve.bezierCurve(gf.createLineString(coords), gf.createLineString(controls))
//    }
}

fun ShapeContour.toJtsPolygon(): Polygon {
    val gf = GeometryFactory()
    val pts = segments.map { it.start } + segments.last().end
    val coords = pts.map { it.toCoordinate() }.toTypedArray()
    return gf.createPolygon(gf.createLinearRing(coords + coords.first()), emptyList<LinearRing>().toTypedArray())
}

fun Shape.toJtsPolygon(): Polygon {
    val gf = GeometryFactory()
    val rings = contours.map {
        it.run {
            val pts = segments.map { it.start } + segments.last().end
            val coords = pts.map { it.toCoordinate() }.toTypedArray()
            gf.createLinearRing(coords + coords.first())
        }
    }
    return gf.createPolygon(rings[0], rings.drop(1).toTypedArray())
}

fun LinearRing.toShapeContour() = ShapeContour.fromPoints(coordinates.map { it.toVector2() }, closed = true)

fun Geometry.toShape(): Shape =
    when(this) {
        is Polygon -> exteriorRing.toShapeContour().shape
        is MultiPolygon -> Shape(List(numGeometries) { getGeometryN(it).toShape().contours }.flatten())
        else -> error("Unknown geometry")
    }

actual fun Shape.buffer(radius: Double): Shape {
    return Shape(contours.map { it.sampleLinear(distanceTolerance = 0.1).toJtsPolygon().buffer(radius, 16).toShape().contours[0] })
}
