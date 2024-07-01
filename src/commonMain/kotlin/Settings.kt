@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

import kotlinx.serialization.Serializable
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.DoubleParameter

@Serializable
data class Settings(
    val gs: GeneralSettings = GeneralSettings(),
    val tgs: GrowSettings = GrowSettings(),
    val cds: ComputeDrawingSettings = ComputeDrawingSettings(),
    val ds: DrawSettings = DrawSettings(),
)

@Serializable
data class GeneralSettings(
    @BooleanParameter("Inflection", order=2000)
    var bendInflection: Boolean = true,

    @DoubleParameter("Max bend angle", 0.0, 360.0, order=3000)
    var maxBendAngle: Double = 180.0,

    @DoubleParameter("Max turning angle", 0.0, 180.0, order=4000)
    var maxTurningAngle: Double = 70.0,

    @DoubleParameter("Point size", 0.1, 10.0, order = 0)
    var pSize: Double = 10.0,
) {
    val expandRadius get() = pSize * 3
}

@Serializable
data class GrowSettings(
    @BooleanParameter("Banks")
    var banks: Boolean = true,

    @BooleanParameter("Islands")
    var islands: Boolean = true,

    @BooleanParameter("Postpone cover radius increase")
    var postponeCoverRadiusIncrease: Boolean = true,

    @BooleanParameter("Postpone intersections")
    var postponeIntersections: Boolean = true,

    @DoubleParameter("Forbid too close", 0.0, 1.0)
    var forbidTooClose: Double = 0.1
)

@Serializable
data class ComputeDrawingSettings(
    @DoubleParameter("Point clearance", 0.0, 1.0, order = 5)
    var pointClearance: Double = 0.625,
)

val blue = rgb(0.651, 0.807, 0.89)
//val blue = rgb(179/255.0, 205/255.0, 228/255.0) // for NYC
val red = rgb(0.984, 0.603, 0.6)
//val red = rgb(248/255.0, 179/255.0, 174/255.0) // for NYC
val green = rgb(0.698, 0.874, 0.541)
//val green = rgb(204/255.0, 230/255.0, 196/255.0) // for NYC
val orange = rgb(0.992, 0.749, 0.435)
val purple = rgb(0.792, 0.698, 0.839)
val yellow = rgb(251 / 255.0, 240 / 255.0, 116 / 255.0)
val brown = rgb(234 / 255.0, 189 / 255.0, 162 / 255.0)
val indigo = rgb(191 / 255.0, 197 / 255.0, 255 / 255.0)
val pink = rgb(239 / 255.0, 186 / 255.0, 235 / 255.0)
val sea = rgb(175 / 255.0, 228 / 255.0, 194 / 255.0)
val cyan = rgb(188 / 255.0, 235 / 255.0, 237 / 255.0)
val gray = rgb(206 / 255.0, 206 / 255.0, 206 / 255.0)

val cbColors = listOf(blue, red, green, orange, purple, yellow, brown, indigo, pink, sea, cyan, gray)

val diseasome = listOf(ColorRGBa.WHITE,
    rgb("#99CC00"),
    rgb("#6699CC"),
    rgb("#EE4444"),
//    rgb("#EE4444"),
    rgb("#999999"),
    rgb("#B3ECF7"),
    rgb("#FFCC00"),
    rgb("#CC6600"),
    rgb("#C0BB56"),
    rgb("#CC0099"),
    rgb("#FF0099"),
    rgb("#FF9999"),
    rgb("#CC9900"),
).map { it.whiten(0.35) }

val newDiseasome = listOf(
    rgb("#B9E1EE"),
    rgb("#9AC019"),
    rgb("#CD6814"),
    rgb("#E53389"),
    rgb("#C1BC56"),
    rgb("#923B8B"),
    rgb("#FBD2AA"),
    rgb("#999999"),
    rgb("#FECD0F"),
    rgb("#CB9A03"),
    rgb("#F3983B"),
    rgb("#4B8EC7"),
    rgb("#2E9A67"),
    rgb("#E95937"),
    rgb("#F8EE82"),
    rgb("#E74646"),
    rgb("#CBBC9D"),
    rgb("#6699CD"),
    rgb("#6FC4C6"),
    rgb("#F1979A"),
    rgb("#8F5A9C"),
    rgb("#BB3087"),
)

@Serializable
data class DrawSettings(
    @DoubleParameter("Whiten", 0.0, 1.0, order = 1000)
    var whiten: Double = 0.7,

    @BooleanParameter("Show points", order = 8980)
    var showPoints: Boolean = true,

    @BooleanParameter("Show islands", order = 8990)
    var showIslands: Boolean = true,

    @BooleanParameter("Show visibility contours", order = 9000)
    var showVisibilityContours: Boolean = true,

    @BooleanParameter("Show bridges", order = 9010)
    var showBridges: Boolean = true,

    @BooleanParameter("Show cluster circles", order = 10000)
    var showClusterCircles: Boolean = false,

    @BooleanParameter("Show island Voronoi", order = 10003)
    var showVoronoiCells: Boolean = false,

    @BooleanParameter("Show bend distance", order = 10005)
    var showBendDistance: Boolean = false,

    @BooleanParameter("Show visibility graph", order=10010)
    var showVisibilityGraph: Boolean = false,

    @BooleanParameter("Show voronoi", order=10020)
    var showVoronoi: Boolean = false,

    @DoubleParameter("Show subset based on computation", 0.0, 1.0, order=10000000)
    var subset: Double = 1.0,

    @BooleanParameter("Shadows", order = 1)
    var shadows: Boolean = false,

    var colors: List<ColorRGBa> = cbColors
//    var colors: List<ColorRGBa> = newDiseasome
) {
    fun pointStrokeWeight(gs: GeneralSettings) = gs.pSize / 2.5
    fun contourStrokeWeight(gs: GeneralSettings) = gs.pSize / 3.5
}