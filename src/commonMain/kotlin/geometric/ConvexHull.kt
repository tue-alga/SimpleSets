package geometric

import org.openrndr.math.Vector2
import patterns.Point
import kotlin.jvm.JvmName

@JvmName("convexHullPoint")
fun convexHull(points: List<Point>): List<Point> = convexHull(points, Point::pos)
@JvmName("convexHullVector2")
fun convexHull(points: List<Vector2>): List<Vector2> = convexHull(points) { this }

fun <T> convexHull(points: List<T>, v2: T.() -> Vector2): List<T> {
    if (points.size <= 1) return points
    val hull = mutableListOf<T>()
    val sorted = points.sortedWith(compareBy({ it.v2().x }, { it.v2().y }))

    fun rightTurn(p: T, q: T, r: T) =
        orientation(p.v2(), q.v2(), r.v2()) == Orientation.RIGHT

    for (p in sorted) {
        while (hull.size >= 2 && !rightTurn(hull[hull.size-2], hull.last(), p)) {
            hull.removeLast()
        }
        hull.add(p)
    }

    val t = hull.size + 1
    for (i in sorted.size - 2 downTo 0) {
        val p = sorted[i]
        while (hull.size >= t && !rightTurn(hull[hull.size-2], hull.last(), p)) {
            hull.removeLast()
        }
        hull.add(p)
    }

    hull.removeLast()
    return hull
}
