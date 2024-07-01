package geometric

import org.openrndr.math.Matrix33
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.math.asDegrees
import patterns.Point
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sign

const val PRECISION = 1e-9

enum class Orientation {
    LEFT, STRAIGHT, RIGHT;

    fun opposite(): Orientation = when(this) {
        LEFT -> RIGHT
        RIGHT -> LEFT
        STRAIGHT -> STRAIGHT
    }

    val polarity get() = when(this) {
        RIGHT -> YPolarity.CCW_POSITIVE_Y
        else -> YPolarity.CW_NEGATIVE_Y
    }
}

fun orientationD(p: Vector2, q: Vector2, r: Vector2) =
    Matrix33(1.0, p.x, p.y,
        1.0, q.x, q.y,
        1.0, r.x, r.y).determinant

fun orientation(p: Vector2, q: Vector2, r: Vector2): Orientation {
    val d = orientationD(p, q, r)

    return if (abs(d) <= PRECISION) {
        Orientation.STRAIGHT
    } else {
        if (d.sign > 0){
            Orientation.LEFT
        } else {
            Orientation.RIGHT
        }
    }
}

fun compare(x1: Double, x2: Double): Orientation {
    val d = x1 - x2
    return if (abs(d) <= PRECISION){
        Orientation.STRAIGHT
    } else {
        if (d.sign < 0){
            Orientation.LEFT
        } else {
            Orientation.RIGHT
        }
    }
}

fun bisector(v1: Vector2, v2: Vector2): Vector2 =
    (v1.normalized + v2.normalized).normalized

/**
 * Clockwise ordering around a point `p`.
 * @param p the reference point
 * @param start the start angle in degrees, counter-clockwise starting at 3 o'clock.
 */
fun compareAround(p: Point, start: Double, dir: Orientation) = Comparator<Point> { p1, p2 ->
    compareAround(p.pos, start, dir).compare(p1.pos, p2.pos)
}

/**
 * Clockwise ordering around a point `p`.
 * @param p the reference point
 * @param start the start angle in degrees, counter-clockwise starting at 3 o'clock.
 */
fun compareAround(p: Vector2, start: Double, dir: Orientation) = Comparator<Vector2> { p1, p2 ->
    val v1 = (p1 - p).rotate(-(start - 180))
    val v2 = (p2 - p).rotate(-(start - 180))
    val a1 = -atan2(v1.y, v1.x)
    val a2 = -atan2(v2.y, v2.x)
    val x = if (a1 < a2 - PRECISION){
        1
    } else if (a1 > a2 + PRECISION) {
        -1
    } else {
        0
    }
    if (dir == Orientation.RIGHT) x else -x
}

fun compareAround(p: Vector2, start: Vector2, dir: Orientation): Comparator<Vector2> =
    compareAround(p, atan2(start.y, start.x).asDegrees, dir)