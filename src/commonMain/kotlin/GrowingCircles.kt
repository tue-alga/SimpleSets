import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.math.min

fun growCircles(pts: List<Vector2>, maxRadius: Double): List<Circle> {
    val growingCircles = pts.map { p ->
        GrowingCircle(p, 0.0)
    }

    while(growingCircles.any { !it.frozen }) {
        var minD = Double.POSITIVE_INFINITY
        lateinit var minPair: Pair<GrowingCircle, GrowingCircle>
        for ((i, c1) in growingCircles.withIndex()) {
            for (c2 in growingCircles.subList(i + 1, growingCircles.size)) {
                if (c1.frozen && c2.frozen) continue
                val d = c1.center.distanceTo(c2.center)

                val realD = when {
                    !c1.frozen && !c2.frozen -> d / 2
                    c1.frozen -> d - c1.r
                    else -> d - c2.r
                }

                if (realD < minD) {
                    minD = min(realD, maxRadius)
                    minPair = c1 to c2
                }
            }
        }

        val c = minPair.toList().first { !it.frozen }
        c.r = minD
        c.frozen = true
    }

    return growingCircles.map {
        Circle(it.center, it.r)
    }
}

fun growCircles(pts1: List<Vector2>, pts2: List<Vector2>, maxRadius1: Double, maxRadius2: Double = maxRadius1): Pair<List<Circle>, List<Circle>> {
    val growingCircles1 = pts1.map { p ->
        TypedGrowingCircle(p, 0.0, type = 1)
    }
    val growingCircles2 = pts2.map { p ->
        TypedGrowingCircle(p, 0.0, type = 2)
    }
    val growingCircles = growingCircles1 + growingCircles2

    if (growingCircles1.isNotEmpty() && growingCircles2.isNotEmpty()) {
        while (growingCircles.any { !it.frozen }) {
            var minD = Double.POSITIVE_INFINITY
            lateinit var minPair: Pair<TypedGrowingCircle, TypedGrowingCircle>
            for (c1 in growingCircles1) {
                for (c2 in growingCircles2) {
                    if (c1.frozen && c2.frozen) continue
                    val centerD = c1.center.distanceTo(c2.center)

                    val growD = when {
                        !c1.frozen && !c2.frozen -> centerD / 2
                        c1.frozen -> centerD - c1.r
                        else -> centerD - c2.r
                    }

                    if (growD < minD) {
                        minPair = c1 to c2
                        minD = growD
                    }
                }
            }

            val (c1, c2) = minPair
            when {
                !c1.frozen && !c2.frozen -> {
                    val d = min(minD, min(maxRadius1, maxRadius2))
                    when (d) {
                        minD -> {
                            c1.frozen = true
                            c2.frozen = true
                            c1.r = minD
                            c2.r = minD
                        }

                        maxRadius1 -> {
                            c1.r = maxRadius1
                            c2.r = maxRadius1
                            c1.frozen = true
                        }

                        else -> {
                            c1.r = maxRadius2
                            c2.r = maxRadius2
                            c2.frozen = true
                        }
                    }
                }

                c1.frozen -> {
                    c2.r = min(minD, maxRadius2)
                    c2.frozen = true
                }

                else -> {
                    c1.r = min(minD, maxRadius1)
                    c1.frozen = true
                }
            }
        }
    } else {
        growingCircles.forEach {
            it.r = if (it.type == 1) maxRadius1 else maxRadius2
        }
    }

    return growingCircles1.map { Circle(it.center, it.r) } to growingCircles2.map { Circle(it.center, it.r) }
}

data class GrowingCircle(val center: Vector2, var r: Double, var frozen: Boolean = false)

data class TypedGrowingCircle(val center: Vector2, var r: Double, val type: Int, var frozen: Boolean = false)

fun <T> Pair<T, T>.theOther(t: T): T = when {
    first == t -> second
    second == t -> first
    else -> error("Value $t not part of pair $this")
}