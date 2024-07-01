package patterns

import kotlinx.serialization.Serializable
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import times
import kotlin.math.min
import kotlin.math.roundToInt

@Serializable
data class Point(val pos: Vector2, val type: Int, val originalPoint: Point? = null) {
    override fun toString(): String {
        return "(${pos.x.roundToDecimals(1)}, ${pos.y.roundToDecimals(1)})"
    }
}

// https://stackoverflow.com/questions/61225315/is-there-a-way-in-kotlin-multiplatform-to-format-a-float-to-a-number-of-decimal
fun Double.roundToDecimals(decimals: Int): Double {
    var dotAt = 1
    repeat(decimals) { dotAt *= 10 }
    val roundedValue = (this * dotAt).roundToInt()
    return (roundedValue / dotAt) + (roundedValue % dotAt).toDouble() / dotAt
}

infix fun Double.v(y: Double) = Vector2(this, y)
infix fun Int.v(y: Int) = Vector2(this.toDouble(), y.toDouble())
infix fun Double.p0(y: Double) = Point(Vector2(this, y), 0)
infix fun Int.p0(y: Int) = Point(Vector2(this.toDouble(), y.toDouble()), 0)
infix fun Double.p1(y: Double) = Point(Vector2(this, y), 1)
infix fun Int.p1(y: Int) = Point(Vector2(this.toDouble(), y.toDouble()), 1)
infix fun Double.p2(y: Double) = Point(Vector2(this, y), 2)
infix fun Int.p2(y: Int) = Point(Vector2(this.toDouble(), y.toDouble()), 2)
infix fun Double.p3(y: Double) = Point(Vector2(this, y), 3)
infix fun Int.p3(y: Int) = Point(Vector2(this.toDouble(), y.toDouble()), 3)
infix fun Double.p4(y: Double) = Point(Vector2(this, y), 4)
infix fun Int.p4(y: Int) = Point(Vector2(this.toDouble(), y.toDouble()), 4)

val List<Point>.bounds: Rectangle get() = map { it.pos }.bounds

fun Rectangle.fit(destination: Rectangle): Matrix44 {
    val scaleFactor = min(destination.width / width, destination.height / height)

    return transform {
        translate(destination.center)
        scale(scaleFactor)
        translate(-center)
    }
}

fun List<Point>.fit(r: Rectangle, margin: Double): List<Point> {
    val m = bounds.offsetEdges(margin).fit(r)
    return map { m * it }
}