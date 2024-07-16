import dilated.dilate
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import patterns.bounds
import java.io.File

fun main() {
    // Change this to generate a different example. See commonMain/kotlin/ExampleInputs.kt
    val example = ExampleInput.Mills
    val destination = File("${example.name}.svg")

    val pts = getExampleInput(example)
    val (gSettings, gCover) = goodSettings(example)
    val (gs, tgs, cds, ds) = gSettings

    val filtration = topoGrow(pts, gs, tgs, 8 * gs.expandRadius)
    val partition = filtration.takeWhile { it.first < gCover * gs.expandRadius }.lastOrNull()!!.second

    val dilatedPolies = partition.patterns.map { it.dilate(gs.expandRadius) }
    val xGraph = XGraph(dilatedPolies, gs, cds, morph=::erodeDilate)

    val rect = pts.bounds
        .offsetEdges(ds.contourStrokeWeight(gs) + 2 * gs.expandRadius)
        .contour
        .bounds

    val transformMatrix = transform {
        translate(0.0, rect.height)
        scale(1.0, -1.0)
    }

    val matrix = Matrix44.fit(rect, Rectangle(Vector2.ZERO, rect.width, rect.height))
    val comp = drawComposition {
        model *= transformMatrix * matrix.inversed
        xGraph.draw(this, ds)
        coloredPoints(partition.points, gs, ds)
    }

    val svgBody = comp.toSVG()

    val svg = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<svg version=\"1.2\" baseProfile=\"tiny\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"0 0 ${rect.width} ${rect.height}\">" +
            svgBody +
            "</svg>"

    destination.writeText(svg)
}