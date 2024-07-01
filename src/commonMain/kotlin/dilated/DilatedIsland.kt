package dilated

import geometric.Arc
import org.openrndr.math.YPolarity
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.ShapeContour
import patterns.Island
import patterns.Matching
import kotlin.math.exp

class DilatedIsland(original: Island, expandRadius: Double): DilatedPoly<Island>(original, expandRadius) {
    val hull = original.hull
    val hullCircles = hull.map { Circle(it.pos, expandRadius) }

    val segments: List<LineSegment> by lazy {
        if (hull.size == 1) return@lazy emptyList()
        (hullCircles + hullCircles.first()).zipWithNext { c1, c2 ->
            val dir = c2.center - c1.center
            val n = dir.perpendicular(YPolarity.CCW_POSITIVE_Y).normalized * expandRadius
            val start = c1.center + n
            val end = c2.center + n
            val segment = LineSegment(start, end)
            segment
        }
    }

    val arcs: List<Arc> by lazy {
        if (hull.size == 1) return@lazy emptyList()
        (listOf(hullCircles.last()) + hullCircles + hullCircles.first()).windowed(3) { (prev, curr, next) ->
            val d1 = curr.center - prev.center
            val d2 = next.center - curr.center
            val n1 = d1.perpendicular(YPolarity.CCW_POSITIVE_Y).normalized * expandRadius
            val n2 = d2.perpendicular(YPolarity.CCW_POSITIVE_Y).normalized * expandRadius

            Arc(curr, curr.center + n1, curr.center + n2)
        }
    }

    override val contour: ShapeContour by lazy {
        if (hull.size == 1) {
            return@lazy hullCircles.first().contour
        }

        var c = ShapeContour.EMPTY
        for (i in hull.indices) {
            c += arcs[i].contour
            c += segments[i].contour
        }
        c.close().reversed
    }
}

fun Island.dilate(expandRadius: Double) = DilatedIsland(this, expandRadius)
fun Matching.dilate(expandRadius: Double) = DilatedIsland(Island(points), expandRadius)
