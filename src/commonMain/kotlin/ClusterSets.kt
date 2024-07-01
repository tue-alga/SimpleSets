@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.shape.LineSegment
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import org.openrndr.shape.intersections
import patterns.Point
import patterns.angleBetween
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2

data class ClusterSetsSettings(
    @DoubleParameter("Beta", 0.0, 1.0)
    var beta: Double = 0.534,
)

fun betaSkeleton(points: List<Point>, beta: Double): List<LineSegment> {
    println("Computing skeleton")
    val theta = if (beta <= 1) PI - asin(beta) else asin(1 / beta)

    return buildList {
        for (i in points.indices) {
            val p = points[i]
            for (j in i + 1 until points.size) {
                val q = points[j]
                if (p.type == points[j].type) {
                    val include = points.all { r ->
                        r == p || r == q || angleBetween(p.pos - r.pos, q.pos - r.pos) <= theta
                    }
                    if (include) {
                        add(LineSegment(p.pos, q.pos))
                    }
                }
            }
        }
    }
}

fun greedy(points: List<Point>, skeleton: List<LineSegment>): List<LineSegment> =
    buildList {
        println("Computing greedy")
        var left = skeleton.toMutableList()
        val component = points.withIndex().associate { it.value.pos to it.index }.toMutableMap()

        while (left.isNotEmpty()) {
            // (1)
            left = left.filter { e -> component[e.start]!! != component[e.end]!! }.toMutableList()
            if (left.isEmpty()) return@buildList
            // (2)
            val crossings = left.associateWith {
                e -> left.filter {
                    it != e && e.contour.sub(0.01, 0.99).intersections(it.contour.sub(0.01, 0.99)).isNotEmpty()
                }
            }
            val e = crossings.minBy { it.value.size }
            add(e.key)
            val oldComp = component[e.key.end]!!
            val newComp = component[e.key.start]!!
            component.filter { (_, value) -> value == oldComp }.keys.forEach {
                component[it] = newComp
            }
            left.remove(e.key)
            // (3)
            left.removeAll(e.value)
        }
    }

fun augment(points: List<Point>, forest: List<LineSegment>, skeleton: List<LineSegment>): List<LineSegment> {
    println("Augmenting")
    val candidates = (skeleton.toSet() - forest.toSet()).toMutableSet()
    val result = forest.toMutableList()

    val type = points.associate { it.pos to it.type }

    while (candidates.isNotEmpty()) {
        val e = candidates.first()
        val intersects = result.any {
            type[it.start]!! != type[e.start]!! && it.contour.sub(0.01, 0.99).intersections(e.contour.sub(0.01, 0.99)).isNotEmpty()
        }
        if (!intersects) {
            result.add(e)
        }
        candidates.remove(e)
    }

    return result
}
