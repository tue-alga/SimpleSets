import dilated.dilate
import org.openrndr.shape.*
import patterns.Point

fun computeXGraph(gs: GeneralSettings, cds: ComputeDrawingSettings,
                  filtration: List<Pair<Double, Partition>>, cover: Double): XGraph? {
    val partition = filtration.takeWhile { it.first < cover * gs.expandRadius }.lastOrNull()?.second ?: return null
    val highlights = partition.patterns.map { it.dilate(gs.expandRadius) }
    return XGraph(highlights, gs, cds)
}

fun createSvg(points: List<Point>, xGraph: XGraph, gs: GeneralSettings, ds: DrawSettings): String =
    drawComposition(CompositionDimensions(0.0.pixels, 0.0.pixels, 800.0.pixels, 800.0.pixels)) {
        xGraph.draw(this, ds)
        coloredPoints(points, gs, ds)
    }.toSVG()