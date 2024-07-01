package geometric

import org.openrndr.math.Vector2
import org.openrndr.math.times
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.ShapeContour
import kotlin.jvm.JvmName
import kotlin.math.PI
import kotlin.math.atan2

fun directedAngleBetween(v1: Vector2, v2: Vector2): Double {
    val dot = v1.x * v2.x + v1.y * v2.y
    val det = v1.x * v2.y - v1.y * v2.x
    return atan2(-det, -dot) + PI
}

@JvmName("convexHullCircles")
fun convexHull(circles: List<Circle>): List<Circle> {
    if (circles.size <= 1) {
        return circles
    } else {
        val a = circles.subList(0, circles.size / 2)
        val b = circles.subList(circles.size / 2, circles.size)
        return merge(convexHull(a), convexHull(b))
    }
}

fun supportingLine(c1: Circle,  c2: Circle): Pair<Vector2, Vector2>? =
    c1.tangents(c2).firstOrNull { (a, b) -> orientation(a, b, c2.center) != Orientation.RIGHT }


fun merge(ch1: List<Circle>, ch2: List<Circle>): List<Circle> {
    val ch = mutableListOf<Circle>()

    fun add(c: Circle) {
        if (ch.lastOrNull() != c) {
            ch.add(c)
        }
    }

    fun Circle.extremeAlong(v: Vector2) = (center + radius * v.perpendicular()) dot v.perpendicular()

    var lStarStart = Vector2(1.0, 0.0)
    var c1 = ch1.withIndex().maxBy { (_, it) ->
        it.extremeAlong(lStarStart)
    }
    var c2 = ch2.withIndex().maxBy { (_, it) ->
        it.extremeAlong(lStarStart)
    }
    if (ch1.size > 1 && supportingLine(ch1[0], ch1[1]) != null) {
        lStarStart = supportingLine(ch1[0], ch1[1])!!.let { it.second - it.first }.normalized
        c1 = IndexedValue(1, ch1[1])
        c2 = ch2.withIndex().maxBy { (_, it) ->
            it.extremeAlong(lStarStart)
        }
    } else if (ch2.size > 1 && supportingLine(ch2[0], ch2[1]) != null) {
        lStarStart = supportingLine(ch2[0], ch2[1])!!.let { it.second - it.first }.normalized
        c2 = IndexedValue(1, ch2[1])
        c1 = ch1.withIndex().maxBy { (_, it) ->
            it.extremeAlong(lStarStart)
        }
    }
    var lStar = lStarStart

    var visited1 = 0
    var visited2 = 0

    fun advance(b: Boolean){
        val x = if (b) c1.index else c2.index
        val y = if (b) c2.index else c1.index
        val cx = if (b) ch1[x] else ch2[x]
        val cy = if (b) ch2[y] else ch1[y]
        val xs = (x+1) % (if (b) ch1.size else ch2.size)
        val ys = (y+1) % (if (b) ch2.size else ch1.size)
        val cxs = if (b) ch1[xs] else ch2[xs]
        val cys = if (b) ch2[ys] else ch1[ys]
        val lxsc = supportingLine(cx, cxs)
        val lysc = supportingLine(cy, cys)
        val a1 = supportingLine(cx, cy)?.let { directedAngleBetween(lStar, it.second - it.first) }
        val a2 = lxsc?.let { directedAngleBetween(lStar, it.second - it.first) }
        val a3 = lysc?.let { directedAngleBetween(lStar, it.second - it.first) }
        val a4 = supportingLine(cy, cx)?.let { directedAngleBetween(lStar, it.second - it.first) }
        if (a1 != null && a1 == listOfNotNull(a1, a2, a3).min()) {
            add(cy)
            if (a4 != null && a4 == listOfNotNull(a4, a2, a3).min()) {
                add(cx)
            }
        }
        if (a2 != null && (a3 == null || a2 < a3)) {
            lStar = (lxsc.second - lxsc.first).normalized
            if (b) {
                c1 = IndexedValue(xs, cxs)
                visited1++
            } else {
                c2 = IndexedValue(xs, cxs)
                visited2++
            }
        } else if (a3 != null && (a2 == null || a3 < a2)) {
            lStar = (lysc.second - lysc.first).normalized
            if (b) {
                c2 = IndexedValue(ys, cys)
                visited2++
            } else {
                c1 = IndexedValue(ys, cys)
                visited1++
            }
        }
    }

    do {
        if (c1.value.extremeAlong(lStar) < c2.value.extremeAlong(lStar)) {
            add(c2.value)
            advance(false)
        } else {
            add(c1.value)
            advance(true)
        }
    } while ((visited1 < ch1.size && ch1.size > 1) || (visited2 < ch2.size && ch2.size > 1))

    if (ch.size > 1 && ch.first() == ch.last()) {
        ch.removeLast()
    }

    return ch
}


fun tangentLoop(circles: List<Circle>): ShapeContour {
    if (circles.size == 1) return circles[0].contour


    val tangents = (circles + circles.first()).zipWithNext { c1, c2 ->
        supportingLine(c1, c2)!!
    }

    var hull = ShapeContour.EMPTY

    fun <T, R> Iterable<T>.zipWithNextWrap(transform: (a: T, b: T) -> R): List<R> =
        (this + this.first()).zipWithNext(transform)

    circles.zip(tangents).zipWithNextWrap { (c1, t1), (c2, t2) ->
        hull += LineSegment(t1.first, t1.second).contour
        hull += c2.subVO(t1.second, t2.first, Orientation.LEFT)
    }

    return hull.close()
}
