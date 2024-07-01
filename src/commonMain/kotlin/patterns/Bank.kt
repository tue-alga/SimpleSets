package patterns

import GeneralSettings
import geometric.Orientation
import geometric.orientation
import org.openrndr.math.asRadians
import org.openrndr.shape.LineSegment
import org.openrndr.shape.ShapeContour
import kotlin.math.max

// A polyline formed by a sequence of input points.
data class Bank(override val points: List<Point>): Pattern() {
    override val contour by lazy {
        ShapeContour.fromPoints(points.map { it.pos }, false)
    }

    val maxDistance by lazy {
        points.zipWithNext { a, b -> a.pos.distanceTo(b.pos) }.maxOrNull() ?: 0.0
    }

    override val coverRadius = maxDistance / 2

    // A bank consists of bends: subsequences that form polylines that bend in one direction.
    val bends: List<Bend> = buildList {
        var orientation: Orientation? = null
        var bendTotalAngle = 0.0
        var bendMaxAngle = 0.0
        var startIndex = 0

        for (i in points.indices) {
            if (i + 2 !in points.indices) break
            val or = orientation(points[i].pos, points[i + 1].pos, points[i + 2].pos)
            val angle = angleBetween(points[i + 1].pos - points[i].pos, points[i + 2].pos - points[i + 1].pos)
            if (orientation == or.opposite()) {
                // Switched orientation
                add(Bend(orientation, bendMaxAngle, bendTotalAngle, startIndex, i + 1))
                orientation = or
                bendTotalAngle = angle
                bendMaxAngle = angle
                startIndex = i
            } else {
                orientation = or
                bendTotalAngle += angle
                bendMaxAngle = max(bendMaxAngle, angle)
            }
        }

        if (orientation == null) {
//            println("Very strange stuff is happening $points")

        } else
            add(Bend(orientation, bendMaxAngle, bendTotalAngle, startIndex, points.lastIndex))
    }

    override fun isValid(gs: GeneralSettings): Boolean {
        val inflectionIsFine = bends.size <= 1 || gs.bendInflection && bends.size <= 2
        val anglesAreFine = bends.all { it.maxAngle <= gs.maxTurningAngle.asRadians }
        val totalAngleIsFine = bends.sumOf { it.totalAngle } <= gs.maxBendAngle.asRadians

        return inflectionIsFine && anglesAreFine && totalAngleIsFine
    }

    fun extensionStart(p: Point, gs: GeneralSettings): Pair<Double, Bank>? {
        val angle = angleBetween(start.pos - points[1].pos, p.pos - start.pos)
        if (angle > gs.maxTurningAngle.asRadians) return null
        if (angle + bends[0].totalAngle > gs.maxBendAngle.asRadians) return null
        val orient = orientation(p.pos, start.pos, points[1].pos)
        if (orient != bends.first().orientation && (bends.size >= 2 || !gs.bendInflection)) return null
        return start.pos.distanceTo(p.pos) / 2 to Bank(listOf(p) + points)
    }

    fun extensionEnd(p: Point, gs: GeneralSettings): Pair<Double, Bank>? {
        val angle = angleBetween(end.pos - points[points.lastIndex - 1].pos, p.pos - end.pos)
        if (angle > gs.maxTurningAngle.asRadians) return null
        if (angle + bends.last().totalAngle > gs.maxBendAngle.asRadians) return null
        val orient = orientation(points[points.lastIndex - 1].pos, points.last().pos, p.pos)
        if (orient != bends.last().orientation && (bends.size >= 2 || !gs.bendInflection)) return null
        return end.pos.distanceTo(p.pos) / 2 to Bank(points + listOf(p))
    }

    fun extensionStart(other: Bank, gs: GeneralSettings): Pair<Double, Bank>? {
        val newBank1 = Bank(other.points + this.points)
        val newBank2 = Bank(other.points.reversed() + this.points)
        val newBank = listOf(newBank1, newBank2).filter { it.isValid(gs) }.minByOrNull { it.coverRadius }

        return if (newBank != null) {
            newBank.coverRadius to newBank
        } else null
    }

    fun extensionEnd(other: Bank, gs: GeneralSettings): Pair<Double, Bank>? {
        val newBank1 = Bank(this.points + other.points)
        val newBank2 = Bank(this.points + other.points.reversed())
        val newBank = listOf(newBank1, newBank2).filter { it.isValid(gs) }.minByOrNull { it.coverRadius }

        return if (newBank != null) {
            newBank.coverRadius to newBank
        } else null
    }

    fun extension(p: Point, gs: GeneralSettings): Pair<Double, Bank>? {
        return listOfNotNull(extensionStart(p, gs), extensionEnd(p, gs))
            .filter { it.second.isValid(gs) }
            .minByOrNull { it.first }
    }

    fun extension(other: Bank, gs: GeneralSettings): Pair<Double, Bank>? {
        return listOfNotNull(extensionStart(other, gs), extensionEnd(other, gs))
            .filter { it.second.isValid(gs) }
            .minByOrNull { it.first }
    }

    fun extension(other: Matching, gs: GeneralSettings): Pair<Double, Bank>? {
        return extension(other.toBank(), gs)
    }

    val start get() = points.first()
    val end get() = points.last()
}

data class Bend(val orientation: Orientation, val maxAngle: Double, val totalAngle: Double,
                val startIndex: Int, val endIndex: Int)
