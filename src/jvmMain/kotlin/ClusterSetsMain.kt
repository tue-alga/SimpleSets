import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.svg.saveToFile
import java.io.File

fun main() = application {
    configure {
        width = 800
        height = 800
        windowResizable = true
    }

    program {
//        val pts = getExampleInput(ExampleInput.Mills)
        val pts = parsePoints(File("input-output/diseasome.txt").readText())
//        val pts = ipeToPoints(File("example-pts.ipe").readText())
        val s = ClusterSetsSettings()
        val gs = GeneralSettings(pSize = 5.2)
        val ds = DrawSettings()

        val gui = GUI()
        gui.add(s, "ClusterSets")
        gui.add(gs, "General settings")
        gui.add(ds, "Draw settings")

        var skeleton = betaSkeleton(pts, s.beta)
        var forest = greedy(pts, skeleton)
        var final = augment(pts, forest, skeleton)

//        gui.onChange { _, _ ->
//            skeleton = betaSkeleton(pts, s.beta)
//            forest = greedy(pts, skeleton)
//            final = augment(pts, forest, skeleton)
//        }

        val comp = drawComposition {
//            scale(1.0, -1.0)
            stroke = ColorRGBa.BLACK.opacify(0.5)
            strokeWeight = 0.5
//            lineSegments(skeleton)

            for (pt in pts) {
                stroke = ds.colors[pt.type]
                fill = stroke!!.opacify(ds.whiten)
                strokeWeight = ds.contourStrokeWeight(gs) * 12.835
                lineStrip(listOf(pt.pos, pt.pos))
            }

            stroke = ColorRGBa.BLACK
            strokeWeight = 1.0
            lineSegments(final)
            coloredPoints(pts, gs, ds)
        }

        comp.saveToFile(File("ClusterSets-diseasome.svg"))
        "py svgtoipe.py ClusterSets-output.svg".runCommand(File("."))

        extend(gui)
        extend(Camera2D())
        extend {
            drawer.apply {
                clear(ColorRGBa.WHITE)
                composition(comp)
            }
        }
    }
}