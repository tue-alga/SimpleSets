package patterns

import GeneralSettings
import geometric.convexHull
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.math.asDegrees
import org.openrndr.math.asRadians
import org.openrndr.shape.*
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sqrt

data class Island(override val points: List<Point>): Pattern() {
    val hull: List<Point> = convexHull(points)
    override val contour by lazy {
        ShapeContour.fromPoints(hull.map { it.pos }, true)
    }
    override val coverRadius by lazy { coverRadius(points.map { it.pos }) }
    override fun isValid(gs: GeneralSettings): Boolean {
        return true
    }
}

fun coverRadius(vecs: List<Vector2>, shape: Shape? = null): Double =
    if (vecs.size < 2) 0.0
    else if (vecs.size == 2) {
        vecs[0].distanceTo(vecs[1]) / 2
    }
    else if (vecs.size == 3) {
        coverRadiusTriangle(vecs[0], vecs[1], vecs[2])
    } else {
        if (shape == null) {
            val delaunay = vecs.delaunayTriangulation()
            coverRadiusVoronoi(vecs, delaunay.hull().shape)
        } else {
            coverRadiusVoronoi(vecs, shape)
        }
    }

fun coverRadiusVoronoi(vecs: List<Vector2>, shape: Shape): Double {
    val delaunay = vecs.delaunayTriangulation()
    val voronoi = delaunay.voronoiDiagram(shape.bounds)
    val cells = voronoi.cellPolygons().map {
        var result = it.shape.intersection(shape.outline.reversed.shape)
        shape.contours.subList(1, shape.contours.size).forEach { hole ->
            result = difference(result, hole.clockwise)
        }
        result.contours.firstOrNull() ?: ShapeContour.EMPTY
    }
    var r = 0.0
    for (i in cells.indices) {
        if (cells[i] == ShapeContour.EMPTY) continue
        val cellVerts = cells[i].segments.map { it.start }
        val c = vecs[i]
        r = r.coerceAtLeast(cellVerts.maxOfOrNull { it.distanceTo(c) }!!)
    }
    return r
}

private fun circumradius(p1: Vector2, p2: Vector2, p3: Vector2): Double {
    val a = (p2 - p1).length
    val b = (p3 - p2).length
    val c = (p1 - p3).length

    return (a * b * c) / sqrt((a + b + c) * (b + c - a) * (c + a - b) * (a + b - c))
}

fun angleBetween(v: Vector2, w: Vector2) = acos((v dot w) / (v.length * w.length))

// Based on the idea from https://math.stackexchange.com/a/2397393
// but corrected the obtuse formula.
fun coverRadiusTriangle(p: Vector2, q: Vector2, r: Vector2): Double {
    val cr = circumradius(p, q, r)
    val pq = q - p
    val pr = r - p
    val angleP = angleBetween(pq, pr).asDegrees
    val qp = p - q
    val qr = r - q
    val angleQ = angleBetween(qp, qr).asDegrees
    val angleR = 180.0 - angleP - angleQ
    val acute = angleP < 90.0 && angleQ < 90.0 && angleR < 90.0
    return if (acute) {
        cr
    } else {
        val (_, b, c) = listOf(angleP to qr.length, angleQ to pr.length, angleR to pq.length).sortedByDescending { it.second }
        b.second / (2 * cos(c.first.asRadians))
    }
}
