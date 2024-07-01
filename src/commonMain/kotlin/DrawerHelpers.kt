import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.shape.Circle
import patterns.Pattern
import patterns.Point

fun Drawer.coloredPoints(points: List<Point>, gs: GeneralSettings, ds: DrawSettings) {
    for (p in points) {
        fill = ds.colors[p.type]
        stroke = ColorRGBa.BLACK
        strokeWeight = ds.pointStrokeWeight(gs)
        contour(Circle(p.pos, gs.pSize).contour)
    }
}

fun Drawer.patternContour(p: Pattern, gs: GeneralSettings, ds: DrawSettings) {
    if (p.contour.segments.size > 1) {
        fill = ds.colors[p.type].whiten(ds.whiten)
        stroke = ColorRGBa.BLACK
        strokeWeight = ds.contourStrokeWeight(gs)
        contour(p.contour)
    }
}

fun Drawer.pattern(p: Pattern, gs: GeneralSettings, ds: DrawSettings) {
    patternContour(p, gs, ds)
    coloredPoints(p.points, gs, ds)
}

fun CompositionDrawer.coloredPoints(points: List<Point>, gs: GeneralSettings, ds: DrawSettings) {
    for (p in points) {
        fill = ds.colors.getOrNull(p.type) ?: ColorRGBa.WHITE
        stroke = ColorRGBa.BLACK
        strokeWeight = ds.pointStrokeWeight(gs)
        contour(Circle(p.pos, gs.pSize).contour)
    }
}

fun CompositionDrawer.patternContour(p: Pattern, gs: GeneralSettings, ds: DrawSettings) {
    if (p.contour.segments.size > 1) {
        fill = ds.colors[p.type].whiten(ds.whiten)
        stroke = ColorRGBa.BLACK
        strokeWeight = ds.contourStrokeWeight(gs)
        contour(p.contour)
    }
}

fun CompositionDrawer.pattern(p: Pattern, gs: GeneralSettings, ds: DrawSettings) {
    patternContour(p, gs, ds)
    coloredPoints(p.points, gs, ds)
}

fun ColorRGBa.whiten(factor: Double) = mix(ColorRGBa.WHITE, factor)