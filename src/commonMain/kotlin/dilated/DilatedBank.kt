package dilated

import geometric.Arc
import geometric.Orientation
import patterns.Bank
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.*
import geometric.orientation

class DilatedBank(original: Bank, expandRadius: Double): DilatedPoly<Bank>(original, expandRadius) {
    val pairedSegs: List<Pair<LineSegment, LineSegment>> by lazy {
        if (points.size == 1) return@lazy emptyList()

        val simplePairedSegs = circles.zipWithNext { c1, c2 ->
            val dir = c2.center - c1.center
            val n = dir.perpendicular(YPolarity.CCW_POSITIVE_Y).normalized * expandRadius
            val start1 = c2.center - n
            val end1 = c1.center - n
            val segment1 = LineSegment(start1, end1)
            val start2 = c1.center + n
            val end2 = c2.center + n
            val segment2 = LineSegment(start2, end2)
            segment1 to segment2
        }

        val nil = LineSegment(Vector2.INFINITY, Vector2.INFINITY) to LineSegment(Vector2.INFINITY, Vector2.INFINITY)
        val paddedSegs = listOf(nil) + simplePairedSegs + nil

        paddedSegs.windowed(3) { (lsp, lsc, lsn) ->
            val fa = if (lsn == nil) 0.0 else lsc.first.contour.intersections(lsn.first.contour).firstOrNull()?.a?.contourT ?: 0.0
            val fb = if (lsp == nil) 1.0 else lsc.first.contour.intersections(lsp.first.contour).firstOrNull()?.a?.contourT ?: 1.0
            val sa = if (lsp == nil) 0.0 else lsc.second.contour.intersections(lsp.second.contour).firstOrNull()?.a?.contourT ?: 0.0
            val sb = if (lsn == nil) 1.0 else lsc.second.contour.intersections(lsn.second.contour).firstOrNull()?.a?.contourT ?: 1.0
            lsc.first.sub(fa, fb) to lsc.second.sub(sa, sb)
        }
    }

    val segments: List<LineSegment> by lazy { pairedSegs.flatMap { it.toList() } }

    val arcs: List<Arc> by lazy {
        if (points.size == 1) return@lazy emptyList()

        val cf = circles.first()
        val cfn = circles[1]
        val cl = circles.last()
        val clp = circles[circles.lastIndex-1]
        val nf = (cfn.center - cf.center).perpendicular().normalized * expandRadius
        val firstArc = Arc(cf, cf.center + nf, cf.center - nf)
        val nl = (cl.center - clp.center).perpendicular().normalized * expandRadius
        val lastArc = Arc(cl, cl.center - nl, cl.center + nl)

        val middleArcs = circles.windowed(3) { (prev, curr, next) ->
            val d1 = curr.center - prev.center
            val d2 = next.center - curr.center
            val or = orientation(prev.center, curr.center, next.center)
            val pol = when(or) {
                Orientation.RIGHT -> YPolarity.CCW_POSITIVE_Y
                Orientation.LEFT -> YPolarity.CW_NEGATIVE_Y
                else -> return@windowed null
            }
            val n1 = d1.perpendicular(pol).normalized * expandRadius
            val n2 = d2.perpendicular(pol).normalized * expandRadius
            val cp1 = curr.center + if (or == Orientation.RIGHT) n1 else n2
            val cp2 = curr.center + if (or == Orientation.RIGHT) n2 else n1

            Arc(curr, cp1, cp2)
        }

        listOf(firstArc) + middleArcs.filterNotNull() + lastArc
    }

    override val contour: ShapeContour by lazy {
        if (points.size == 1) {
            return@lazy circles.first().contour
        }

        val contours = (segments.map { it.contour } + arcs.map { it.contour }).toMutableList()
        var c = contours.removeFirst()

        while(contours.isNotEmpty()) {
            val next = contours.minBy { (it.segments.first().start - c.segments.last().end).squaredLength }
            c += next
            contours.remove(next)
        }
        c.close().reversed
    }
}

fun Bank.dilate(expandRadius: Double) = DilatedBank(this, expandRadius)
