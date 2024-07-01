@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

import dilated.ContourHighlight
import org.openrndr.extra.marchingsquares.findContours
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2
import org.openrndr.shape.contains
import patterns.Point
import patterns.bounds
import kotlin.math.max

data class VihrovsSettings(
    @DoubleParameter("Resolution", 0.01, 10.0)
    var resolution: Double = 2.5,
    @DoubleParameter("Vertex radius", 0.001, 50.0)
    var vertexRadius: Double = 30.0,
    @DoubleParameter("Influence radius", 0.001, 100.0)
    var influenceRadius: Double = 60.0,
)

fun vihrovs(points: List<Point>, settings: VihrovsSettings): List<ContourHighlight> {
    with(settings) {
        println("Computing Vihrovs...")

        if (influenceRadius < vertexRadius) {
            println("Influence radius should be at least vertex radius")
            influenceRadius = 1.01 * vertexRadius
        }
        val m = 1 / (influenceRadius * influenceRadius)
        val t = 1 / (vertexRadius * vertexRadius) - m

        fun influence(v: Vector2, p: Vector2) =
            max(1 / v.squaredDistanceTo(p) - m, 0.0)

        fun influence(type: Int, p: Vector2) =
            points
                .filter { it.type == type }
                .sumOf { influence(it.pos, p) }

        fun potential(type: Int, p: Vector2) =
            influence(type, p) - points.filter { it.type != type }.sumOf { influence(it.pos, p) } - t

        val r = points.bounds.offsetEdges(vertexRadius * 2)

        val types = points.map { it.type }.toSet()

        val highlights = types.flatMap { type ->
            findContours({ p -> potential(type, p) }, r, resolution)
                .map { c -> ContourHighlight(c, points.filter { p -> p.pos in c }) }
        }

        println("Done")

        return highlights
    }
}