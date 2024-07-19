import geometric.end
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.shapes.hobbyCurve
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.simplify
import org.openrndr.svg.saveToFile
import patterns.bounds
import java.io.File

fun main() = application {
    configure {
        width = 800
        height = 800
        windowResizable = true
    }

    program {
        val pts = getExampleInput(ExampleInput.Mills)
//        val s = VihrovsSettings(1.0, 11.5, 23.0) // +
        val s = VihrovsSettings(1.0, 8.625, 17.25) // -
//        val pts = getExampleInput(ExampleInput.NYC)
//        val s = VihrovsSettings(1.0, 17.0, 34.0) // +
//        val s = VihrovsSettings(1.0, 12.75, 25.5) // -
//        val pts = parsePoints(File("input-output/diseasome.txt").readText())
//        val s = VihrovsSettings(2.5, 40.0, 80.0) // +
//        val s = VihrovsSettings(2.5, 30.0, 60.0) // -
//        val pts = ipeToPoints(File("example-pts.ipe").readText())


//        val gs = GeneralSettings(pSize = 5.2) // diseasome
        val gs = GeneralSettings(pSize = 2.068) // nyc & mills
        val ds = DrawSettings(whiten = 0.1)
        var hs = vihrovs(pts, s)
        var contours = hs.associateWith { h ->
                hobbyCurve(
                        simplify(h.contour.segments.map { it.start } , 0.175)
                    , closed=true)
        }
        var r = pts.bounds.offsetEdges(s.vertexRadius)
        var comp = drawComposition {
            translate(0.0, height.toDouble())
            scale(1.0, -1.0)
            for (h in hs) {
                if (h.contour.shape.area < 0.85 * r.area) {
                    fill = if (h.points.isNotEmpty())
                        ds.colors[h.type].whiten(0.7)
                    else
                        ColorRGBa.WHITE
                    stroke = ColorRGBa.BLACK
                    strokeWeight = ds.contourStrokeWeight(gs)
                    contour(contours[h]!!)
                }
            }
            coloredPoints(pts, gs, ds)
        }
        comp.saveToFile(File("vihrovs.svg"))
        "py svgtoipe.py vihrovs.svg".runCommand(File("."))

        extend(Camera2D())
        extend {
            drawer.apply {
                clear(ColorRGBa.WHITE)
                composition(comp)
            }
        }
    }
}