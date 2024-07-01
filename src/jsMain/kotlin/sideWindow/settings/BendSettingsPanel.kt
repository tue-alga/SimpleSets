package sideWindow.settings

import components.Checkbox
import components.Slider
import contexts.BendSettingsContext
import drawComposition
import emotion.react.css
import dilated.dilate
import js.objects.jso
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.intersections
import patterns.Bank
import patterns.Point
import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span
import react.dom.svg.ReactSVG.svg
import react.useContext
import toSVG
import web.cssom.*
import kotlin.math.max
import kotlin.math.min

external interface BankSettingsPanelProps: PropsWithChildren {
    var ptSize: Double
    var ptStrokeWeight: Double
    var lineStrokeWeight: Double
    var expandRadius: Double
    var color: String
}

val BankSettingsPanel = FC<BankSettingsPanelProps> { props ->
    with(useContext(BendSettingsContext)!!) {
        div {
            css {
                display = Display.flex
                flexDirection = FlexDirection.column
            }
            div {
                css {
                    display = Display.flex
                    flexWrap = FlexWrap.wrap
                }
                div {
                    css {
                        flex = Flex.maxContent
                        flexGrow = number(0.0)
                    }
                    Checkbox {
                        title = "Allow bank inflection"
                        checked = bendInflection
                        label = "Bank inflection"
                        onChange = {
                            bendInflection = !bendInflection
                        }
                    }
                    Slider {
                        title = "Change max bend angle"
                        step = "any".unsafeCast<Double>()
                        min = 10.0
                        max = 180.0
                        value = maxBendAngle
                        unit = "°"
                        label = "Max bend angle"
                        onChange = {
                            maxBendAngle = it.currentTarget.valueAsNumber
                        }
                    }
                    Slider {
                        title = "Change max turning angle"
                        step = "any".unsafeCast<Double>()
                        min = 10.0
                        max = 160.0
                        value = maxTurningAngle
                        unit = "°"
                        label = "Max turning angle"
                        onChange = {
                            maxTurningAngle = it.currentTarget.valueAsNumber
                        }
                    }
                    +props.children
                }
                div {
                    css {
                        flexGrow = number(1.0)
                        maxWidth = 100.px
                    }
                }
                div {
                    css {
                        display = Display.flex
                        alignSelf = AlignSelf.center
                        alignItems = AlignItems.center
                        justifyContent = JustifyContent.center
                        flexDirection = FlexDirection.column
                    }
                    svg {
                        val (svg, bbox) = bendPreview(
                            maxTurningAngle, maxBendAngle, props.ptSize,
                            props.ptStrokeWeight, props.lineStrokeWeight, props.expandRadius, props.color
                        )
                        width = 200.0
                        height = 100.0
                        val scale = max(bbox.width / width!!, bbox.height / height!!)
                        val box = if (scale > 1.0) bbox else Rectangle.fromCenter(bbox.center, width!!, height!!)
                        viewBox = "${box.x} ${box.y} ${box.width} ${box.height}"
                        dangerouslySetInnerHTML = jso {
                            __html = svg
                        }
                    }
                    span {
                        css {
                            margin = Margin(10.px, 0.px)
                        }
                        +"Extreme bank"
                    }
                }
            }
        }
    }
}

fun extremeBend(maxDistance: Double, maxTurningAngle: Double, maxTotalAngle: Double): List<Vector2> {
    val points = mutableListOf(Vector2.ZERO, Vector2(0.0, maxDistance).rotate((180.0 - maxTotalAngle) / 2))
    var totalAngle = 0.0
    fun next(a: Vector2, b: Vector2): Vector2 {
        val dir = (b - a).normalized * maxDistance
        val angle = min(maxTurningAngle, maxTotalAngle - totalAngle)
        totalAngle += angle
        val v = b + dir.rotate(angle)
        return if (angle >= maxTurningAngle) v else {
            val ls = LineSegment(b, v)
            val hori = LineSegment(Vector2(-100000.0, 0.0), Vector2(100000.0, 0.0))
            ls.contour.intersections(hori.contour)[0].position
        }
    }
    while (totalAngle < maxTotalAngle - 1.0) {
        val n = next(points[points.lastIndex - 1], points.last())
        points.add(n)
    }
    return points.map { (x, y) -> Vector2(-x, -y) }
}

fun bendPreview(maxTurningAngle: Double, maxTotalAngle: Double,
                pointSize: Double, pointStrokeWeight: Double, lineStrokeWeight: Double,
                expandRadius: Double, color: String): Pair<String, Rectangle> {
    val pts = extremeBend(50.0, maxTurningAngle, maxTotalAngle)
    return drawComposition {
        stroke = ColorRGBa.BLACK
        strokeWeight = lineStrokeWeight
        fill = rgb(color).opacify(0.3)
        if (!pts.zipWithNext().all { (a, b) -> a.distanceTo(b) < expandRadius }
            && pts.first().distanceTo(pts.last()) > 2 * expandRadius) {
            val c = Bank(pts.map { Point(it, 0) }).dilate(expandRadius).contour
            contour(c)
        }
        strokeWeight = pointStrokeWeight
        fill = rgb(color)
        circles(pts, pointSize)
    }.toSVG() to pts.bounds.offsetEdges(1.0 + pointStrokeWeight + pointSize + expandRadius)
}
