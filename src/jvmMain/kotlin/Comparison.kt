import geometric.convexHull
import dilated.DilatedPoly
import dilated.ShapeHighlight
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.union
import patterns.Pattern
import patterns.Point
import patterns.coverRadius
import kotlin.math.abs
import kotlin.math.max

fun areaCovered(dilatedPolies: List<Pattern>): Double {
    var total = Shape.EMPTY
    var iters = 0

    for (h in dilatedPolies) {
        iters++
        total = if (h is ShapeHighlight) {
            h.shape.union(total)
        } else {
            h.contour.clockwise.shape.union(total)
        }
    }

    return total.area
}

fun areaCoveredCustom(dilatedPolies: List<Pattern>): Double {
    var total = Shape.EMPTY
    var iters = 0

    for (h in dilatedPolies) {
        if (iters == 61) {
            iters++
            continue
        }
        total = if (h is ShapeHighlight) {
            h.shape.union(total)
        } else {
            h.contour.clockwise.shape.union(total)
        }
        iters++
    }
    total = total.union(dilatedPolies[61].contour.clockwise.shape)
    return total.area
}

fun densityDistortion(dilatedPolies: List<Pattern>, points: List<Point>): Pair<Double, Double> {
    val totalCovered = areaCovered(dilatedPolies)

    var total = 0.0
    var maxim = 0.0

    val grouped = dilatedPolies.groupBy { it.type }
    for ((t, hs) in grouped) {
        val coveredArea = areaCovered(hs)
        val tNumPoints = hs.sumOf { it.points.size }
        val delta = abs(coveredArea / totalCovered - tNumPoints.toDouble() / points.size)
        total += delta
        maxim = max(delta, maxim)
    }

    return total / grouped.size * 100 to maxim * 100
}

fun densityDistortionCustom(dilatedPolies: List<Pattern>, points: List<Point>): Pair<Double, Double> {
    val totalCovered = areaCoveredCustom(dilatedPolies)

    var total = 0.0
    var maxim = 0.0

    val grouped = dilatedPolies.groupBy { it.type }
    for ((t, hs) in grouped) {
        val coveredArea = areaCovered(hs)
        val tNumPoints = hs.sumOf { it.points.size }
        val delta = abs(coveredArea / totalCovered - tNumPoints.toDouble() / points.size)
        total += delta
        maxim = max(delta, maxim)
    }

    return total / grouped.size * 100 to maxim * 100
}

fun maxCoverRadius(dilatedPolies: List<Pattern>): Double {
    return dilatedPolies.maxOf { h ->
        coverRadius(h.points.map { it.pos }, shape = if (h is ShapeHighlight) h.shape else h.contour.shape)
    }
}

fun avgCoverRadius(dilatedPolies: List<Pattern>): Double {
    return dilatedPolies.sumOf { h ->
        coverRadius(h.points.map { it.pos }, shape = if (h is ShapeHighlight) h.shape else h.contour.shape)
    } / dilatedPolies.size
}

fun <T, R> List<T>.windowedCyclic(windowSize: Int, transform: (List<T>) -> R): List<R> =
    windowed(windowSize, 1, false, transform) + (subList(size - windowSize + 1, size) + subList(0, windowSize - 1)).windowed(windowSize, 1, false, transform)

fun perimeterRatio(dilatedPoly: Pattern): Double {
    val ch = ShapeContour.fromPoints(convexHull(dilatedPoly.contour.equidistantPositions(1000)), closed = true)
    val s = if (dilatedPoly is ShapeHighlight) dilatedPoly.shape else dilatedPoly.contour.shape
    return s.contours.sumOf { it.length } / ch.length
}

fun areaRatio(dilatedPoly: Pattern): Double {
    val ch = ShapeContour.fromPoints(convexHull(dilatedPoly.contour.equidistantPositions(1000)), closed = true)
    return ch.shape.area / (if (dilatedPoly is ShapeHighlight) dilatedPoly.shape.area else dilatedPoly.contour.shape.area)
}

inline fun <T> List<T>.avgOf(selector: (T) -> Double): Double =
    sumOf(selector) / size