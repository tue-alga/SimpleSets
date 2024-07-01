import geometric.distanceTo
import dilated.ContourHighlight
import dilated.DilatedPoint
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import org.openrndr.shape.contour
import org.openrndr.svg.toSVG
import patterns.Pattern
import patterns.Point
import patterns.SinglePoint
import java.io.File

fun main() {
    // Set this to one of "ClusterSets", "BubbleSets", or "LineSets" to create the corresponding visualizations.
    val vis = "ClusterSets"
    val ipe = File("nyc-$vis.ipe").readText()

    application {
        program {
            val gs = GeneralSettings(pSize = 1.25)
            val ds = DrawSettings()
            val cds = ComputeDrawingSettings()
            val (points, highlights) = ipeToContourHighlights(ipe, gs.expandRadius)
            val xGraph = XGraph(highlights, gs, cds)
//            { comp, fileName ->
//                val timeStamp = ZonedDateTime
//                    .now( ZoneId.systemDefault() )
//                    .format( DateTimeFormatter.ofPattern( "uuuu.MM.dd.HH.mm.ss" ) )
//                val f = File("${fileName}.svg")
//
//                f.writeText(comp.toSVG())
//                "py svgtoipe.py ${fileName}.svg".runCommand(File("."))
//            }
            val c = drawComposition {
                translate(0.0, 1.25 * height)
                scale(1.0, -1.0)
                xGraph.draw(this, ds)
                coloredPoints(points, gs, ds)
            }

            val outputFile = File("input-output/nyc-$vis-drawing.svg")
            outputFile.writeText(c.toSVG())
            "py svgtoipe.py input-output/nyc-$vis-drawing.svg".runCommand(File("."))

            extend {
                drawer.apply {
                    clear(ColorRGBa.WHITE)
                    composition(c)
                }
            }
        }
    }
}

fun ipeToContourHighlights(ipe: String, expandRadius: Double): Pair<List<Point>, List<Pattern>> {
    val ipeXML = ipe.lines().filterNot { it.contains("""<!DOCTYPE ipe SYSTEM "ipe.dtd">""") }.joinToString("\n")
    val doc = loadXMLFromString(ipeXML)
    val nodeList = doc.getElementsByTagName("path").asList()

    val points = nodesToPoints(doc.getElementsByTagName("use").asList().map { it.attributes.asMap() })

    val polygons = mutableMapOf<Int, MutableList<ShapeContour>>()

    for (m in nodeList) {
        val type = when(m.attributes.asMap()["fill"]) {
            "CB light blue"  -> 0
            "CB light red"   -> 1
            "CB light green" -> 2
            else -> when(m.attributes.asMap()["stroke"]) {
                "CB dark blue"  -> 0
                "CB dark red"   -> 1
                "CB dark green" -> 2
                else -> null
            }
        }
        if (type != null) {
            val list = polygons[type]
            if (list != null) {
                list.add(m.textContent.ipePathToContour())
            } else {
                polygons[type] = mutableListOf(m.textContent.ipePathToContour())
            }
        }
    }

    return points to buildList {
        val isolatedPoints = points.toMutableSet()
        for ((_, cs) in polygons) {
            for (c in cs) {
                val pts = points.filter { it.pos in c || c.distanceTo(it.pos) < 1E-3 }
                isolatedPoints.removeAll(pts)
                add(ContourHighlight(c.buffer(expandRadius).contours[0], pts))
            }
        }
        for (p in isolatedPoints) {
            add(DilatedPoint(SinglePoint(p), expandRadius))
        }
    }
}

fun String.ipePathToContour(): ShapeContour =
    contour {
        for (l in lines()) {
            if (l.isEmpty()) continue
            if (l == "h")
                close()
            else {
                val words = l.split(' ')
                val x = words[0].toDouble()
                val y = words[1].toDouble()
                if (words[2] == "m")
                    moveTo(x, y)
                else
                    lineTo(x, y)
            }
        }
    }