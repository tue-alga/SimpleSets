import geometric.distanceTo
import geometric.overlaps
import dilated.dilate
import org.openrndr.shape.Circle
import org.openrndr.shape.contains
import org.openrndr.shape.intersection
import org.openrndr.shape.union
import patterns.*
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sqrt

data class PossibleMergeEvent(var time: Double, val p1: Pattern, val p2: Pattern, val mergeResult: Pattern, var final: Boolean = false)

fun intersectionDelay(points: List<Point>, p: Pattern, q: Pattern, newPattern: Pattern,
                      gs: GeneralSettings, tgs: GrowSettings
): Double {
    if (!tgs.postponeIntersections) return 0.0
    var intersectionArea = 0.0
    for (pt in points) {
        if (pt !in newPattern.points && newPattern.contour.distanceTo(pt.pos) < gs.expandRadius * 2) {
            val ptShape = Circle(pt.pos, gs.expandRadius).shape
            val npShape = newPattern.dilate(gs.expandRadius).contour.shape
            val newTotalArea = npShape.intersection(ptShape).area
            val pShape = p.dilate(gs.expandRadius).contour.shape
            val qShape = q.dilate(gs.expandRadius).contour.shape
            val oldArea = pShape.union(qShape).intersection(ptShape).area
            intersectionArea += newTotalArea - oldArea
        }
    }
    return sqrt(max(intersectionArea / PI, 0.0))
}

fun topoGrow(points: List<Point>, gs: GeneralSettings, tgs: GrowSettings, maxTime: Double,
             nUpdates: Int = 8, progressUpdate: ((Double) -> Unit)? = null): List<Pair<Double, Partition>> {
    // 1. Add SinglePoint -- SinglePoint merges
    // 2. Repeat the following
    // 3. Pick next event
    // 4. Check if merge is actually possible
    // 5. If so, do the merge and compute new possible merge events for this new pattern

    // Step 1. takes O(n^2 log(n)) time

    // O(n^2) events
    // Step 3. takes O(log(n)) time
    // Step 4.
    // - O(n) to check if patterns still exist
    // - O(n^2) to check for intersections
    // Step 5.
    // - O(n) to create the new pattern
    // - O(n^2) probably to create new merge events
    // O(n^4) in total

    var progressUpdates = 0

    val partition = Partition(points)

    val history = mutableListOf(0.0 to partition.copy())

    val events = PriorityQueue<PossibleMergeEvent>(compareBy { it.time })

    // 1. Add SinglePoint -- SinglePoint merges
    for (i in partition.patterns.indices) {
        val p = partition.patterns[i] as SinglePoint
        for (j in i + 1 until partition.patterns.size) {
            val q = partition.patterns[j] as SinglePoint
            if (p.type != q.type) continue
            if (p.point.pos.distanceTo(q.point.pos) > 2 * maxTime) continue

            val newPattern = Matching(p.point, q.point)

            val tooClose: Boolean = run {
                for (pt in partition.points) {
                    if (pt !in newPattern.points && newPattern.contour.distanceTo(pt.pos) < tgs.forbidTooClose * gs.expandRadius) {
                        return@run true
                    }
                }
                return@run false
            }

            if (tooClose) continue

            val ev = PossibleMergeEvent(newPattern.coverRadius, p, q, newPattern, final = false)
            events.add(ev)
        }
    }

    while (events.isNotEmpty()) {
        // 3. Pick next event
        val ev = events.poll()

        if (ev.time > progressUpdates / nUpdates.toDouble() * maxTime) {
            progressUpdate?.let { it(progressUpdates / nUpdates.toDouble()) }
            progressUpdates++
        }

        if (ev.time > maxTime) break

        if (!ev.final) {
            ev.time += intersectionDelay(partition.points, ev.p1, ev.p2, ev.mergeResult, gs, tgs)
            ev.final = true
            events.add(ev)
            continue
        }
        // 4. Check if merge is actually possible
        // - Check if patterns still exist
        if (ev.p1 !in partition.patterns || ev.p2 !in partition.patterns) continue

        // - Check for intersections
        val newPattern = ev.mergeResult

        val intersects = partition.patterns.any {
            it != ev.p1 && it != ev.p2 && (it is SinglePoint && it.point.pos in newPattern.contour || it !is SinglePoint && it.contour.overlaps(newPattern.contour))
        }

        if (intersects) continue

        val tooClose: Boolean = run {
            for (pt in partition.points) {
                if (pt !in newPattern.points && newPattern.contour.distanceTo(pt.pos) < tgs.forbidTooClose * gs.expandRadius) {
                    return@run true
                }
            }
            return@run false
        }

        if (tooClose) continue

        // 5. If so, do the merge and compute new possible merge events for this new pattern
        partition.merge(ev.p1, ev.p2, newPattern)

        history.add(ev.time to partition.copy())

        for (p in partition.patterns) {
            if (p == newPattern || p.type != newPattern.type) continue

            if (tgs.islands) {
                // TODO: Reduce number of coverRadius calls

                val tooFar = p is SinglePoint && newPattern.contour.distanceTo(p.point.pos) > 2 * maxTime ||
                        p !is SinglePoint && p.points.zip(newPattern.points) { a, b -> a.pos.distanceTo(b.pos) }.all { it > 2 * maxTime }

                if (!tooFar) {
                    val pts = p.points + newPattern.points
                    val freshPattern = Island(pts)
                    val intersectsFresh = partition.patterns.any {
                        it != newPattern && it != p && (it is SinglePoint && it.point.pos in newPattern.contour || it !is SinglePoint && it.contour.overlaps(
                            newPattern.contour
                        ))
                    }

                    if (!intersectsFresh) {
                        val coverRadius = coverRadius(pts.map { it.pos })
                        val delay = if (!tgs.postponeCoverRadiusIncrease) 0.0
                        else coverRadius - max(p.coverRadius, newPattern.coverRadius)
                        val t =
                            coverRadius + delay

                        events.add(PossibleMergeEvent(t, newPattern, p, freshPattern))
                    }
                }
            }

            if (tgs.banks && newPattern is Matching && p is SinglePoint) {
                val result = newPattern.extension(p.point, gs)

                if (result != null) {
                    val delay = if (!tgs.postponeCoverRadiusIncrease) 0.0
                        else result.second.coverRadius -
                            newPattern.coverRadius
                    val t = result.first + delay
                    events.add(PossibleMergeEvent(t, newPattern, p, result.second))
                }
            }

//            if (tgs.banks && newPattern is Bank && p is SinglePoint) {
//                val result = newPattern.extension(p.point, cps)
//                if (result != null) {
//                    val delay = if (!tgs.postponeCoverRadiusIncrease) 0.0
//                    else result.second.coverRadius -
//                            newPattern.coverRadius
//                    val t = result.first + delay + intersectionDelay(partition, p, newPattern, result.second, cps, tgs)
//                    events.add(PossibleMergeEvent(t, newPattern, p, result.second))
//                }
//            }
//
//            if (tgs.banks && newPattern is Bank && p is Bank) {
//                val result = newPattern.extension(p, cps)
//                if (result != null) {
//                    val delay = if (!tgs.postponeCoverRadiusIncrease) 0.0
//                    else result.second.coverRadius -
//                            max(newPattern.coverRadius, p.coverRadius)
//                    val t = result.first + delay + intersectionDelay(partition, p, newPattern, result.second, cps, tgs)
//                    events.add(PossibleMergeEvent(t, newPattern, p, result.second))
//                }
//            }

            if (tgs.banks && newPattern is Matching && p is Matching) {
//                val result = newPattern.toBank().extension(p, cps)
                val b1 = Bank(newPattern.points + p.points)
                val b2 = Bank(newPattern.points.reversed() + p.points)
                val b3 = Bank(p.points + newPattern.points)
                val b4 = Bank(p.points + newPattern.points.reversed())
                val b = listOf(b1, b2, b3, b4).filter { it.isValid(gs) }.minByOrNull { it.coverRadius }

                if (b != null) {
                    val delay = if (!tgs.postponeCoverRadiusIncrease) 0.0
                    else b.coverRadius -
                            max(newPattern.coverRadius, p.coverRadius)
                    val t = b.coverRadius + delay
                    events.add(PossibleMergeEvent(t, newPattern, p, b))
                }
            }

            if (tgs.banks && newPattern is Matching && p is Bank) {
                val result = p.extension(newPattern, gs)

                if (result != null) {
                    val delay = if (!tgs.postponeCoverRadiusIncrease) 0.0
                    else result.second.coverRadius -
                            max(newPattern.coverRadius, p.coverRadius)
                    val t = result.first + delay
                    events.add(PossibleMergeEvent(t, newPattern, p, result.second))
                }
            }

            if (tgs.banks && newPattern is Bank && p !is Island) {
                val result = when(p) {
                    is Bank -> newPattern.extension(p, gs)
                    is Matching -> newPattern.extension(p, gs)
                    is SinglePoint -> newPattern.extension(p.point, gs)
                    else -> error("Impossible")
                }

                if (result != null) {
                    val delay = if (!tgs.postponeCoverRadiusIncrease) 0.0
                    else result.second.coverRadius -
                            max(newPattern.coverRadius, p.coverRadius)
                    val t = result.first + delay
                    events.add(PossibleMergeEvent(t, newPattern, p, result.second))
                }
            }
        }
    }

    return history
}