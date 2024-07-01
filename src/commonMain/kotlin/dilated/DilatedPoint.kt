package dilated

import org.openrndr.shape.ShapeContour
import patterns.SinglePoint

class DilatedPoint(point: SinglePoint, expandRadius: Double): DilatedPoly<SinglePoint>(point, expandRadius) {
    override val contour: ShapeContour = circles.first().contour
}

fun SinglePoint.dilate(expandRadius: Double) = DilatedPoint(this, expandRadius)
