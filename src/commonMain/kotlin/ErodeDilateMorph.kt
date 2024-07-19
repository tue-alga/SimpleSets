import geometric.*
import org.openrndr.shape.Circle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.difference
import org.openrndr.shape.intersection

fun erodeDilate(c: ShapeContour, compContour: ShapeContour, orient: Orientation, inclCircles: List<Circle>, exclCircles: List<Circle>,
                gs: GeneralSettings, cds: ComputeDrawingSettings, debug: Debug = noDebug
): ShapeContour {
    if (exclCircles.isEmpty()) {
        return c
    }
    if (c.empty) {
        return c
    }

    val smoothingRadius = gs.expandRadius / 5.0

    val (lineCovering, arcCovering) = exclCircles.partition {
        val inter = intersection(c, it.shape)
        inter.empty || inter.contours[0].isStraight()
    }
    val components = connectedCircles(lineCovering.map { it.copy(radius = it.radius + smoothingRadius) }) + arcCovering.map { listOf(it.copy(radius = it.radius + smoothingRadius)) }
    val hulls = components.map {
        tangentLoop(convexHull(it.map { it.copy(radius = it.radius - smoothingRadius) }))
    }

    val modifiedHulls = hulls.map {
        var result = it.shape
        for (inclCircle in inclCircles) {
            result = result.difference(inclCircle.shape)
        }
        result.contours[0]
    }

    var result = compContour.shape
    for (mHull in modifiedHulls) {
        result = difference(result, mHull.shape)
    }
    for (circle in exclCircles) {
        val n = c.contour.nearest(circle.center)
        val nn = (n.position - circle.center).perpendicular()
        val nnn = nn.normalized * (gs.pSize/5)
        val spike = ShapeContour.fromPoints(listOf(
            n.position - nnn,
            circle.center - nnn,
            circle.center + nnn,
            n.position + nnn,
        ), closed=true)
        result = difference(result, spike)
    }

    if (result.contours.size > 1) {
        println("Morph problem")
    }

    val hm = result.contours[0].subsVC(c.start, c.end).toList().minBy {
        sub -> sub.equidistantPositions(10).sumOf {
            p -> c.squaredDistanceTo(p)
        }
    }.extend(gs.expandRadius).closeAroundBB(orient, gs.pSize * 5.0)

    val final = hm
        .buffer(-smoothingRadius)
        .buffer(smoothingRadius)
        .buffer(smoothingRadius)
        .buffer(-smoothingRadius)
        .contours[0]

//    val comp = drawComposition {
//        stroke = ColorRGBa.BLACK
//        contour(hm)
//
//        stroke = ColorRGBa.BLUE
//        shape(hm.buffer(-smoothingRadius))
//
//        stroke = ColorRGBa.GREEN
//        shape(hm.buffer(smoothingRadius))
//
//        stroke = ColorRGBa.RED
//        contour(final)
//
//        stroke = ColorRGBa.PINK
//        val ffinal = final.subsVC(c.start, c.end).toList().minBy { sub ->
//            sub.equidistantPositions(10).sumOf { p ->
//                c.squaredDistanceTo(p)
//            }
//        }.fix(0.1).removeSpikes()
//        contour(ffinal)
//    }
//    debug(comp, "erodeDilate_${c.segments.size}")

    if (final.empty) {
        return c.fix()
    }
    return final.subsVC(c.start, c.end).toList().minBy {
        sub -> sub.equidistantPositions(10).sumOf {
            p -> c.squaredDistanceTo(p)
        }
    }.fix(0.1).removeSpikes() // the Clipper buffer used in jsCommon causes spikes sometimes; remove manually
}