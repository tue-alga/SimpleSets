package geometric

import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.*
import patterns.v
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

fun ShapeContour.overlaps(other: ShapeContour) =
    intersections(other).isNotEmpty() || position(0.0) in other || other.position(0.0) in this

val ShapeContour.start get() = segments.first().start

val ShapeContour.end get() = segments.last().end

fun ShapeContour.direction(ut: Double): Vector2 = normal(ut).perpendicular(polarity.opposite)

fun ShapeContour.distanceTo(v: Vector2) = nearest(v).position.distanceTo(v)

fun ShapeContour.squaredDistanceTo(v: Vector2) = nearest(v).position.squaredDistanceTo(v)

operator fun ShapeContour.contains(other: ShapeContour) =
    intersections(other).isEmpty() &&
            other.position(0.0) in this

operator fun Shape.contains(other: ShapeContour) =
    intersections(other).isEmpty() &&
            other.position(0.0) in this &&
            contours.count { it.overlaps(other) } == 1

fun ShapeContour.extend(amnt: Double): ShapeContour {
    val startD = normal(0.0).perpendicular(polarity.opposite) * amnt
    val startS = LineSegment(start - startD, start).segment
    val endD = normal(1.0).perpendicular(polarity.opposite) * amnt
    val endS = LineSegment(end, end + endD).segment
    return copy(segments = listOf(startS) + segments + listOf(endS))
}

val YPolarity.opposite get() =
    when(this) {
        YPolarity.CCW_POSITIVE_Y -> YPolarity.CW_NEGATIVE_Y
        YPolarity.CW_NEGATIVE_Y -> YPolarity.CCW_POSITIVE_Y
    }

fun ShapeContour.subV(start: Vector2, end: Vector2): ShapeContour {
    val t1 = nearest(start).contourT
    val t2 = nearest(end).contourT
    return sub(t1, t2)
}

fun ShapeContour.subVC(start: Vector2, end: Vector2): ShapeContour {
    val t1 = nearest(start).contourT
    val t2 = nearest(end).contourT
    val c = if (t1 < t2) sub(t1, t2) else sub(t1,  1.0) + sub(0.0, t2)
    return if (c.segments.isNotEmpty() && c.end.squaredDistanceTo(start) < c.start.squaredDistanceTo(start)) {
        c.reversed
    } else {
        c
    }
}

fun ShapeContour.subsVC(start: Vector2, end: Vector2): Pair<ShapeContour, ShapeContour> {
    val t1 = nearest(start).contourT
    val t2 = nearest(end).contourT
    return subsC(t1, t2).map { c ->
        if (c.segments.isNotEmpty() && c.end.squaredDistanceTo(start) < c.start.squaredDistanceTo(start)) {
            c.reversed
        } else {
            c
        }
    }
}

fun ShapeContour.subsC(t1: Double, t2: Double): Pair<ShapeContour, ShapeContour> {
    val a = min(t1, t2)
    val b = max(t1, t2)
    return sub(a, b) to (sub(b, 1.0) + sub(0.0, a))
}

// sub the smaller part of a closed ShapeContour
fun ShapeContour.subC(t1: Double, t2: Double): ShapeContour =
//    if (t2 > t1 || t1 - t2 < 0.5) sub(t1, t2) else (sub(t1, 1.0) + sub(0.0, t2))
    subsC(t1, t2).toList().minBy { it.length }

fun Circle.sub(t1: Double, t2: Double): ShapeContour = contour.subC(t1, t2)
fun Circle.subs(t1: Double, t2: Double): Pair<ShapeContour, ShapeContour> =
    contour.subsC(t1, t2)

fun Circle.subs(start: Vector2, end: Vector2): Pair<ShapeContour, ShapeContour> {
    val t1 = contour.nearest(start).contourT
    val t2 = contour.nearest(end).contourT
    return subs(t1, t2).map {
        if (it.segments.isEmpty() || it.start.squaredDistanceTo(start) < it.end.squaredDistanceTo(start))
            it
        else
            it.reversed
    }
}

private fun <A, B> Pair<A, A>.map(f: (A) -> B): Pair<B, B> = f(first) to f(second)

fun Circle.subV(start: Vector2, end: Vector2): ShapeContour = contour.subVC(start, end)

fun Circle.subVO(start: Vector2, end: Vector2, orient: Orientation): ShapeContour =
    subs(start, end).toList().first {
        orientation(it.contour.start, it.contour.position(0.5), it.contour.end) != orient.opposite()
    }

fun List<ShapeContour>.merge(start: Vector2 = first().start): ShapeContour {
    require(isNotEmpty()) {
        "Cannot merge an empty list of contours"
    }
    val contours = toMutableList()
    val startC = contours
        .map { it to listOf(it, it.reversed).minBy { it.start.squaredDistanceTo(start) } }
        .minBy {
            it.second.start.squaredDistanceTo(start)
        }
    var c = startC.second
    contours.remove(startC.first)

    while(contours.isNotEmpty()) {
        val next = contours
            .map { it to listOf(it, it.reversed).minBy { (it.start - c.end).squaredLength } }
            .minBy { (it.second.start - c.end).squaredLength }
        c += next.second
        contours.remove(next.first)
    }

    return c
}

fun List<ShapeContour>.mergeAdjacent(): List<ShapeContour> {
    if (size < 2) return this
    val new = mutableListOf<ShapeContour>()
    var current: ShapeContour = first()

    for (i in indices) {
        val next = if (i + 1 < size) this[(i+1)] else new[0]
        if (current.end.squaredDistanceTo(next.start) < 1E-6) {
            current += next
        } else {
            new.add(current)
            current = next
        }
    }
    if (current != first()) {
        new.add(current)
        new.removeFirst()
    }

    return new
}

fun ShapeContour.isStraight(): Boolean {
    val normals = equidistantPositionsWithT(50).map {
        normal(it.second)
    }

    for (i in normals.indices) {
        for (j in i + 1 until normals.size) {
            if (normals[i].distanceTo(normals[j]) > 0.05) return false
        }
    }

    return true
}

fun ShapeContour.offsetFix(distance: Double, joinType: SegmentJoin = SegmentJoin.ROUND): ShapeContour {
    val offsets =
        segments.map { it.offset(distance, yPolarity = polarity, stepSize = 0.01) }
            .filter { it.isNotEmpty() }
    val tempContours = offsets.map {
        ShapeContour.fromSegments(it.filter { it.length > 1E-4 }, closed = false, distanceTolerance = 0.1)
    }
    val offsetContours = tempContours.map { it }.filter { it.length > 1E-4 }.toMutableList()

    for (i in 0 until offsetContours.size) {
        offsetContours[i] = offsetContours[i].removeLoops()
    }

    for (i0 in 0 until if (this.closed) offsetContours.size else offsetContours.size - 1) {
        val i1 = (i0 + 1) % (offsetContours.size)
        val its = intersections(offsetContours[i0], offsetContours[i1])
        if (its.size == 1) {
            offsetContours[i0] = offsetContours[i0].sub(0.0, its[0].a.contourT)
            offsetContours[i1] = offsetContours[i1].sub(its[0].b.contourT, 1.0)
        }
    }

    if (offsets.isEmpty() || offsetContours.isEmpty()) {
        return ShapeContour(emptyList(), false)
    }


    val startPoint = if (closed) offsets.last().last().end else offsets.first().first().start

    val candidateContour = contour {
        moveTo(startPoint)
        for (offsetContour in offsetContours) {
            val delta = (offsetContour.position(0.0) - cursor)
            val joinDistance = delta.length
            if (joinDistance > 10e-6) {
                when (joinType) {
                    SegmentJoin.BEVEL -> lineTo(offsetContour.position(0.0))
                    SegmentJoin.ROUND -> arcTo(
                        crx = joinDistance * 0.5 * sqrt(2.0),
                        cry = joinDistance * 0.5 * sqrt(2.0),
                        angle = 90.0,
                        largeArcFlag = false,
                        sweepFlag = true,
                        end = offsetContour.position(0.0)
                    )
                    SegmentJoin.MITER -> {
                        val ls = lastSegment ?: offsetContours.last().segments.last()
                        val fs = offsetContour.segments.first()
                        val i = intersection(
                            ls.end,
                            ls.end + ls.direction(1.0),
                            fs.start,
                            fs.start - fs.direction(0.0),
                            eps = 10E8
                        )
                        if (i !== Vector2.INFINITY) {
                            lineTo(i)
                            lineTo(fs.start)
                        } else {
                            lineTo(fs.start)
                        }
                    }
                }
            }
            for (offsetSegment in offsetContour.segments) {
                segment(offsetSegment)
            }

        }
        if (this@offsetFix.closed) {
            close()
        }
    }

    val postProc = false

    var final = candidateContour.removeLoops()

    if (postProc && !final.empty) {
        val head = Segment(
            segments[0].start + segments[0].normal(0.0)
                .perpendicular(polarity) * 1000.0, segments[0].start
        ).offset(distance).firstOrNull()?.copy(end = final.segments[0].start)?.contour

        val tail = Segment(
            segments.last().end,
            segments.last().end - segments.last().normal(1.0)
                .perpendicular(polarity) * 1000.0
        ).offset(distance).firstOrNull()?.copy(start = final.segments.last().end)?.contour

        if (head != null) {
            val headInts = intersections(final, head)
            if (headInts.size == 1) {
                final = final.sub(headInts[0].a.contourT, 1.0)
            }
            if (headInts.size > 1) {
                val sInts = headInts.sortedByDescending { it.a.contourT }
                final = final.sub(sInts[0].a.contourT, 1.0)
            }
        }
//            final = head + final
//
        if (tail != null) {
            val tailInts = intersections(final, tail)
            if (tailInts.size == 1) {
                final = final.sub(0.0, tailInts[0].a.contourT)
            }
            if (tailInts.size > 1) {
                val sInts = tailInts.sortedBy { it.a.contourT }
                final = final.sub(0.0, sInts[0].a.contourT)
            }
        }

//            final = final + tail

    }

    return final
}

enum class Side {
    LEFT,
    BOTTOM,
    RIGHT,
    TOP
}

fun intToSide(i: Int): Side = when(i) {
    0 -> Side.LEFT
    1 -> Side.BOTTOM
    2 -> Side.RIGHT
    3 -> Side.TOP
    else -> error("Unknown side $i")
}

fun sideToInt(side: Side): Int = when(side) {
    Side.LEFT   -> 0
    Side.BOTTOM -> 1
    Side.RIGHT  -> 2
    Side.TOP    -> 3
}

val Rectangle.xmin get() = x
val Rectangle.xmax get() = x + width
val Rectangle.ymin get() = y
val Rectangle.ymax get() = y + height

fun closestSide(p: Vector2, r: Rectangle): Side =
    intToSide(
        listOf(p.x - r.xmin, p.y - r.ymin, r.xmax - p.x, r.ymax - p.y)
            .withIndex()
            .minBy { it.value }
            .index
    )

fun sideDirection(side: Side) = when(side) {
    Side.LEFT   -> -Vector2.UNIT_X
    Side.BOTTOM -> -Vector2.UNIT_Y
    Side.RIGHT  -> Vector2.UNIT_X
    Side.TOP    -> Vector2.UNIT_Y
}

fun projOnSide(p: Vector2, side: Side, r: Rectangle) = when(side) {
    Side.LEFT   -> r.xmin v p.y
    Side.BOTTOM -> p.x    v r.ymin
    Side.RIGHT  -> r.xmax v p.y
    Side.TOP    -> p.x    v r.ymax
}

fun Rectangle.vertex(i: Int): Vector2 = when(i) {
    0 -> xmin v ymin
    1 -> xmax v ymin
    2 -> xmax v ymax
    3 -> xmin v ymax
    else -> error("Unknown")
}

fun corner(side1: Side, side2: Side, r: Rectangle): Vector2 {
    val i1 = sideToInt(side1)
    val i2 = sideToInt(side2)
    if (i1 > i2) return corner(side2, side1, r)
    val dist = abs(i1 - i2)
    return if (dist == 1) {
        r.vertex(i1)
    } else {
        r.vertex(i2)
    }
}

fun nextSide(side: Side): Side = intToSide((sideToInt(side) + 1) % 4)

// Close open contour around its bounding box, ensuring that the interior lies on side orient.
fun ShapeContour.closeAroundBB(orient: Orientation, offset: Double = 1.0): ShapeContour {
    if (closed) return this

    val source = start
    val target = end

    val r = bounds
    val sourceSide = closestSide(source, r)
    val targetSide = closestSide(target, r)

    var dist = abs(sideToInt(sourceSide) - sideToInt(targetSide))
    if (dist > 2) {
        dist -= 2
    }

    val sourceDir = sideDirection(sourceSide)
    val targetDir = sideDirection(targetSide)

    val sourceOut = projOnSide(source, sourceSide, r) + sourceDir * offset
    val targetOut = projOnSide(target, targetSide, r) + targetDir * offset

    val pts = mutableListOf<Vector2>(target)
    pts.add(targetOut)

    when (dist) {
        0 -> {}
        1 -> {
            val opt1 = listOf(targetOut, corner(sourceSide, targetSide, r) + (sourceDir + targetDir) * offset, sourceOut)
            if (orientation(opt1[0], opt1[1], opt1[2]) == orient) {
                pts.add(opt1[1])
            } else {
                // 0 1 -> 1 2 3 0
                // 1 2 -> 2 3 0 1
                // 2 3 -> 3 0 1 2
                // 3 0 -> 0 1 2 3
                //     3
                //   -----
                // 0 |   | 2
                //   ----
                //    1

                val smaller = if (sideToInt(sourceSide) < sideToInt(targetSide)) sourceSide else targetSide
                val s1 = nextSide(smaller)
                val s2 = nextSide(s1)
                val s3 = nextSide(s2)
                val s4 = nextSide(s3)
                val opt2 = listOf(
                    corner(s1, s2, r) + (sideDirection(s1) + sideDirection(s2)) * offset,
                    corner(s2, s3, r) + (sideDirection(s2) + sideDirection(s3)) * offset,
                    corner(s3, s4, r) + (sideDirection(s3) + sideDirection(s4)) * offset,
                )
                if (smaller == sourceSide || sideToInt(targetSide) == 0) {
                    pts.addAll(opt2)
                } else {
                    pts.addAll(opt2.reversed())
                }
            }
        }
        2 -> {

            val betweenSide = if (orient == Orientation.RIGHT) nextSide(sourceSide) else nextSide(targetSide)
            val betweenDir = sideDirection(betweenSide)
            pts.add(corner(betweenSide, targetSide, r) + (betweenDir + targetDir) * offset)
            pts.add(corner(sourceSide, betweenSide, r) + (sourceDir + betweenDir) * offset)
        }
        else -> error("Impossible")
    }
    pts.add(sourceOut)
    pts.add(source)

    return ShapeContour.fromSegments(segments + pts.zipWithNext { a, b -> Segment(a, b) }, closed = true)
}