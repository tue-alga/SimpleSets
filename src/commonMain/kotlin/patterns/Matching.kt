package patterns

import GeneralSettings
import org.openrndr.math.asRadians
import org.openrndr.shape.ShapeContour

data class Matching(val point1: Point, val point2: Point) : Pattern() {
    override val contour = ShapeContour.fromPoints(listOf(point1.pos, point2.pos), true)
    override val points = listOf(point1, point2)
    override val coverRadius = point1.pos.distanceTo(point2.pos) / 2
    override fun isValid(gs: GeneralSettings) = true

    fun extensionStart(p: Point, gs: GeneralSettings): Pair<Double, Bank>? {
        val angle = angleBetween(point1.pos - point2.pos, p.pos - point1.pos)
        if (angle > gs.maxTurningAngle.asRadians) return null
        if (angle > gs.maxBendAngle.asRadians) return null
        return point1.pos.distanceTo(p.pos) / 2 to Bank(listOf(p, point1, point2))
    }

    fun extensionEnd(p: Point, gs: GeneralSettings): Pair<Double, Bank>? {
        val angle = angleBetween(point2.pos - point1.pos, p.pos - point2.pos)
        if (angle > gs.maxTurningAngle.asRadians) return null
        if (angle > gs.maxBendAngle.asRadians) return null
        return point2.pos.distanceTo(p.pos) / 2 to Bank(listOf(point1, point2, p))
    }

    fun extension(p: Point, gs: GeneralSettings): Pair<Double, Bank>? {
        return listOfNotNull(extensionStart(p, gs), extensionEnd(p, gs)).minByOrNull { it.first }
    }

    fun toBank(): Bank {
        return Bank(points)
    }
}