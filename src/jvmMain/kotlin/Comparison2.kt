import dilated.ContourHighlight
import dilated.ShapeHighlight
import geometric.*
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.*
import org.openrndr.svg.loadSVG
import org.openrndr.svg.saveToFile
import patterns.Pattern
import patterns.Point
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {
        val gs = GeneralSettings(pSize = 1.55 * 1.33)
        val ds = DrawSettings()

//        val file = File("Vihrovs-mills.svg")
//        val file = File("Vihrovs-mills-thinner.svg")
        val file = File("Vihrovs-nyc.svg")
//        val file = File("Vihrovs-nyc-thinner.svg")
//        val file = File("Vihrovs-diseasome.svg")
//        val file = File("Vihrovs-diseasome-thinner.svg")
//        val file = File("ClusterSets-mills.svg")
//        val file = File("ClusterSets-nyc.svg")
//        val file = File("ClusterSets-diseasome.svg")
//        val file = File("SimpleSets-mills.svg")
//        val file = File("SimpleSets-nyc.svg")
//        val file = File("SimpleSets-diseasome.svg")

        val (pts, highlights) = parseSvg(file.readText(), gs.pSize, ds.colors, clusterSets=false, simpleSets=false, checkHoles=true)

        val comp = drawComposition {
        }

        fun ShapeContour.fix(): ShapeContour {
            val newSegments = mutableListOf<Segment>()
            for (s in segments) {
                if (s.length >= 0.05) {
                    if (newSegments.size > 0) {
                        if (newSegments.last().end != s.start) {
                            newSegments.add(Segment(newSegments.last().end, s.start))
                        }
                    }
                    newSegments.add(s)
                }
            }
            return ShapeContour.fromSegments(newSegments, closed, polarity)
        }

        fun inflectionPoints(h: Pattern): Int {
            val shape = if (h is ShapeHighlight) h.shape else h.contour.shape
            var inflections = 0
            for (cDirty in shape.contours) {
                val c = cDirty.fix()
                if (c.segments.size <= 1) continue
                val ors =
                    c.equidistantPositionsWithT((c.length).toInt() / 2).windowedCyclic(2) { (a, b) ->
//                        if (a.first == b.first) return@windowedCyclic c.position(a.second) to Orientation.STRAIGHT
                        val v1 = c.normal(a.second)
                        val v2 = c.normal(b.second)
                        val dot = v1.x * v2.x + v1.y * v2.y
                        val det = v1.x * v2.y - v1.y * v2.x
                        val angle = atan2(det, dot)
                        comp.draw {
                            strokeWeight = 0.25
                            lineSegment(a.first, a.first + v1 * 2.5)
                        }
                        c.position(if (abs(a.second - b.second) < 0.1) (a.second + b.second) / 2.0 else a.second) to if (abs(
                                angle
                            ) < 1E-2
                        ) Orientation.STRAIGHT else if (angle < 0) Orientation.LEFT else Orientation.RIGHT
                    }
                var start: Orientation? = null
                var current: Orientation? = null

                for ((p, o) in ors) {
                    comp.draw {
                        fill =
                            (if (o == Orientation.LEFT) ColorRGBa.RED else if (o == Orientation.RIGHT) ColorRGBa.BLUE else ColorRGBa.GRAY).opacify(
                                0.25
                            )
                        stroke = null
                        strokeWeight = 0.5
                        circle(p, 1.0)
                    }

                    if (current == null && o != Orientation.STRAIGHT) {
                        start = o
                        current = o
                    }
                    if (current != null && o == current.opposite()) {
                        current = o
                        inflections++
                        comp.draw {
                            fill =
                                (if (o == Orientation.LEFT) ColorRGBa.RED else if (o == Orientation.RIGHT) ColorRGBa.BLUE else ColorRGBa.GRAY).opacify(
                                    0.5
                                )
                            stroke = null
                            strokeWeight = 0.5
                            circle(p, 1.0)
                        }
                    }
                }
                if (current != start) inflections++
            }
            return inflections / 2
        }

        fun totalAbsoluteCurvature(h: Pattern): Double {
            val shape = if (h is ShapeHighlight) h.shape else h.contour.shape
            return shape.contours.sumOf { dirtyC ->
                val c = dirtyC.fix()
                if (c.segments.size <= 1) return@sumOf 0.0
                c.equidistantPositionsWithT((c.length).toInt() / 2).windowedCyclic(2) { (a, b) ->
//                    if (a.first == b.first) return@windowedCyclic 0.0
                    val v1 = c.normal(a.second)
                    val v2 = c.normal(b.second)
                    val dot = v1.x * v2.x + v1.y * v2.y
                    val det = v1.x * v2.y - v1.y * v2.x
                    val angle = atan2(det, dot)
//                    comp.draw {
//                        strokeWeight = 0.25
//                        lineSegment(a.first, a.first + v1 * 2.5)
//                    }
//                    println(a.first)
//                    println(v1)
//                    println(b.first)
//                    println(v2)
//                    println(angle)
                    abs(angle)
                }.sum()
            }
        }

        print("${highlights.sumOf { inflectionPoints(it) }} & ")
        print("${highlights.avgOf { perimeterRatio(it) }} / ")
        print("${highlights.maxOf { perimeterRatio(it) }} & ")
        print("${highlights.avgOf { areaRatio(it) }} / ")
        print("${highlights.maxOf { areaRatio(it) }} & ")
        print("${highlights.avgOf { totalAbsoluteCurvature(it) - 2 * PI }} / ")
        print("${highlights.maxOf { totalAbsoluteCurvature(it) - 2 * PI }} & ")
        print("${highlights.size} & ")
        // use areaCoveredCustom and densityDistortionCustom for diseasome SimpleSets
        print("${areaCovered(highlights) / pts.map { it.pos }.bounds.area} & ")
        val dd = densityDistortion(highlights, pts)
        print("${dd.first} / ${dd.second} & ")
        print("${avgCoverRadius(highlights)} / ")
        print("${maxCoverRadius(highlights)}")


//        println("#inflection points: ${highlights.sumOf { inflectionPoints(it) }}")
//        println("#shapes: ${highlights.size}")
//        println("Area covered: ${areaCovered(highlights) / pts.map { it.pos }.bounds.area}")
//        println("Density distortion: ${densityDistortion(highlights, pts) }")
//        println("Avg cover radius: ${avgCoverRadius(highlights)}")
//        println("Max cover radius: ${maxCoverRadius(highlights)}")
//        println("Avg perimeter ratio: ${highlights.avgOf { perimeterRatio(it) }}")
//        println("Max perimeter ratio: ${highlights.maxOf { perimeterRatio(it) }}")
//        println("Sum perimeter ratios - 1: ${highlights.sumOf { perimeterRatio(it) - 1 }}")
//        println("Avg area ratio: ${highlights.avgOf { areaRatio(it) }}")
//        println("Max area ratio: ${highlights.maxOf { areaRatio(it) }}")
//        println("Sum area ratios - 1: ${highlights.sumOf { areaRatio(it) - 1 }}")
//        println("Sum total absolute curvature: ${highlights.sumOf { totalAbsoluteCurvature(it) - 2 * PI }}")
//        println("Avg total absolute curvature: ${highlights.avgOf { totalAbsoluteCurvature(it) - 2 * PI }}")
//        println("Max total absolute curvature: ${highlights.maxOf { totalAbsoluteCurvature(it) - 2 * PI }}")
//        println("Max total absolute curvature: ${highlights.withIndex().map { it to (totalAbsoluteCurvature(it.value) - 2 * PI) }.maxBy { it.second }}")
//        println(totalAbsoluteCurvature(highlights[10]))
//        println(totalAbsoluteCurvature(highlights[8]))
//        inflectionPoints(highlights[15])
//        println(highlights[15].contour)
//        println(highlights[15].contour.fix())
        extend(Camera2D())

        val comp2 = drawComposition {}
        comp2.draw {
//                scale(-1.0, -1.0)
//                clear(ColorRGBa.WHITE)
            stroke = ColorRGBa.BLACK
            fill = ColorRGBa.GRAY

            for (h in highlights) {
                if (h is ShapeHighlight) {
                    fill = ds.colors[h.type].whiten(ds.whiten)
                    shape(h.shape)
//                        val vecs = h.points.map { it.pos }
//                        val shape = h.shape
//                        val delaunay = vecs.delaunayTriangulation()
//                        val voronoi = delaunay.voronoiDiagram(shape.bounds)
//                        val cells = voronoi.cellPolygons().map {
//                            var result = it.shape.intersection(shape.outline.reversed.shape)
//                            shape.contours.subList(1, shape.contours.size).forEach { hole ->
//                                result = difference(result, hole.clockwise)
//                            }
//                            result.contours.firstOrNull() ?: ShapeContour.EMPTY
//                        }
//                        contours(cells)
                } else {
                    patternContour(h, gs, ds)
                }
            }
            composition(comp)
            coloredPoints(pts, gs, ds)
//                patternContour(highlights[(seconds.toInt() % highlights.size)], gs, ds)
//                patternContour(highlights[10], gs, ds)
        }

        comp2.saveToFile(File("Vihrovs-amoeba.svg"))
        extend {
//            print("${(seconds.toInt() % highlights.size)}\r")
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(comp2)
        }
    }
}

fun parseSvg(svg: String, ptSize: Double, colors: List<ColorRGBa>, clusterSets: Boolean, simpleSets: Boolean, checkHoles: Boolean): Pair<List<Point>, List<Pattern>> {
    val comp = loadSVG(svg)
    val (ptShapes, visShapes) = comp.findShapes().partition {
//        println(it.effectiveStrokeWeight)
//        it.effectiveStrokeWeight > 0.8
//        it.effectiveShape.bounds.area < ptSize * ptSize * 5 &&
//                it.effectiveFill != null &&
//                (if (!clusterSets) it.effectiveStroke != null else it.effectiveStroke == ColorRGBa.BLACK)
        if (clusterSets)
            it.effectiveShape.area > ptSize && it.effectiveStroke == ColorRGBa.BLACK && it.effectiveFill != null
        else it.effectiveShape.bounds.area < ptSize * ptSize * 5 &&
                it.effectiveFill != null &&
                it.effectiveStroke != null
    }
    val pts = ptShapes.map { sn ->
        val type = colors.withIndex().minBy { (_, c) -> sn.effectiveFill!!.toVector4().squaredDistanceTo(c.toVector4()) }.index
        Point(sn.effectiveShape.bounds.center, type)
    }

    var strokeWeight = 0.0

    val svgColors = ptShapes.map { it.effectiveFill!! }.toSet()
    val (holes, highlights) = visShapes.filter { if (clusterSets) it.effectiveStroke in svgColors else true }.mapNotNull { sn ->
        val hole = if (checkHoles) sn.effectiveFill == ColorRGBa.WHITE && sn.effectiveShape.area > 1E-6 //&& !sn.effectiveShape.contours[0].buffer(-sn.effectiveStrokeWeight / 2).empty
                        else false
        if (hole) println("Hole!")
        val c = sn.effectiveShape.contours[0]
        if (c.segments.isNotEmpty()) {
                val buffered = if (clusterSets && !hole) c.buffer(sn.strokeWeight / 2).contours[0] else c
                val contained = pts.filter { it.pos in buffered &&
                        (if (clusterSets) buffered.distanceTo(it.pos) > sn.strokeWeight / 4 - 1E-6 else true) &&
                        (if (simpleSets) c.distanceTo(it.pos) >= ptSize * 0.5 else true)
                }
                if (contained.isNotEmpty()) {
                    strokeWeight = sn.effectiveStrokeWeight
                    hole to ContourHighlight(c.clockwise, contained)
                }
                else null
        }
        else null
    }.partition { it.first }.toList().map { it.map { it.second } }

    val minimal = highlights.filter { h ->
        if (clusterSets)
            highlights.none { other ->
                h != other && h.points.all { p -> p in other.points }// &&
//                    h.contour.equidistantPositions(8).any { it in other.contour }
            } || holes.any { other -> h.points.all { p -> p in other.points  } }
        else true
    }

    val withHoles = minimal.map { highlight ->
        if (!checkHoles) return@map ContourHighlight(highlight.contour.buffer(strokeWeight / 2.0).contours[0], highlight.points)

        val cBuff = if (clusterSets) highlight.contour.buffer(strokeWeight / 2.0).contours[0] else highlight.contour

        val hHoles = if (highlight.contour.shape.area < 1E-6) emptyList() else holes.filter { hole ->
            val hc = ShapeContour.fromSegments(hole.contour.segments.filter { it.length > 1E-6 }, true)
            val hcs = ShapeContour.fromSegments(highlight.contour.segments.filter { it.length > 1E-6 }, true)

            if (clusterSets) hc.overlaps(hcs) else hc in hcs
        }

        if (hHoles.isNotEmpty()) {
            val shape = if (clusterSets)
                Shape(listOf(cBuff) + hHoles.map { it.contour.counterClockwise.buffer(-strokeWeight / 2.0).contours[0] })//buffer(-strokeWeight / 2.0).counterClockwise })
                else Shape(listOf(highlight.contour) + hHoles.map { it.contour.counterClockwise })

            if (shape.empty) highlight else {
                val contained = highlight.points.filter { p -> shape.contours.subList(1, shape.contours.size).none { it.contains(p.pos) } }
                if (contained.isNotEmpty())
                    ShapeHighlight(shape, highlight.contour, contained, 0.0)
                else {
                    println("Problem!")
                    ShapeHighlight(shape, highlight.contour, highlight.points, 0.0)
                }
            }
        } else {
            if (clusterSets) {
                ContourHighlight(cBuff, highlight.points)
            } else {
                highlight
            }
        }
    }

    withHoles.withIndex().forEach { (i, h) ->
        val b = h.points.map { it.type }.toSet().size != 1
        if (b) {
            println("Highlight does not contain points of only one type!")
            println(h.points)
            println(i)
        }
    }

    val extra = pts.withIndex().mapNotNull { (i, p) ->
        val b = withHoles.none { h ->
            h.points.contains(p)
        }
        if (b) {
            println("Point $i ($p) not part of a pattern!")
            if (clusterSets) {
                ContourHighlight(Circle(p.pos, strokeWeight / 2).contour, listOf(p))
            } else if (simpleSets) {
                ContourHighlight(Circle(p.pos, ptSize * 3).contour, listOf(p))
            } else {
                error("")
            }
        } else {
            null
        }
    }
    return pts to (withHoles + extra)
}
