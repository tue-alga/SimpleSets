import org.openrndr.math.Vector2
import patterns.Point

enum class ExampleInput {
    Mills,
    NYC,
    Hotels,
    SimpleScatterPlot,
    OverlapExample1,
    OverlapExample2,
    HexaSwirl,
    Bonn,
    Diseasome,
    Intertwined
}

fun getFileName(e: ExampleInput): String = when(e) {
    ExampleInput.Mills -> "mills"
    ExampleInput.NYC -> "nyc"
    ExampleInput.Hotels -> "hotels"
    ExampleInput.SimpleScatterPlot -> "simple-scatter-plot"
    ExampleInput.OverlapExample1 -> "overlap-example"
    ExampleInput.OverlapExample2 -> "overlap-example-2"
    ExampleInput.HexaSwirl -> "hexa-swirl"
    ExampleInput.Bonn -> "Bonn"
    ExampleInput.Diseasome -> "diseasome"
    ExampleInput.Intertwined -> "intertwined"
}

fun getExtension(e: ExampleInput): String = when(e) {
    ExampleInput.Diseasome -> "txt"
    else -> "ipe"
}

fun goodSettings(e: ExampleInput): Pair<Settings, Double> {
    when(e) {
        ExampleInput.Mills -> {
            val gs = GeneralSettings(
                pSize = 1.514,
                bendInflection = true,
                maxBendAngle = 180.0,
                maxTurningAngle = 70.0,
            )

            val tgs = GrowSettings(
                forbidTooClose = 0.5,
                postponeIntersections = false,
            )

            val settings = Settings(gs, tgs)

            return settings to 4.6
        }

        ExampleInput.NYC -> {
            val gs = GeneralSettings(
                pSize = 2.068,
                bendInflection = true,
                maxBendAngle = 180.0,
                maxTurningAngle = 70.0,
            )

            val tgs = GrowSettings(
                forbidTooClose = 0.1,
                postponeIntersections = true,
            )

            val settings = Settings(gs, tgs)

            return settings to 4.87
        }

        ExampleInput.Hotels -> {
            val gs = GeneralSettings(
                pSize = 1.514,
                bendInflection = true,
                maxBendAngle = 180.0,
                maxTurningAngle = 70.0,
            )

            val tgs = GrowSettings(
                forbidTooClose = 0.5,
                postponeIntersections = false,
            )

            val settings = Settings(gs, tgs)

            return settings to 4.5
        }

        ExampleInput.SimpleScatterPlot -> {
            val gs = GeneralSettings(
                pSize = 1.391,
                bendInflection = false,
                maxBendAngle = 180.0,
                maxTurningAngle = 75.0,
            )

            val tgs = GrowSettings(
                forbidTooClose = 0.4,
                postponeIntersections = false,
            )

            val settings = Settings(gs, tgs)

            return settings to 5.0
        }

        ExampleInput.OverlapExample1 -> {
            val gs = GeneralSettings(
                pSize = 5.142,
            )

            val tgs = GrowSettings(
                forbidTooClose = 0.2,
                postponeIntersections = false,
            )

            val settings = Settings(gs, tgs)

            return settings to 5.0
        }

        ExampleInput.OverlapExample2 -> {
            val gs = GeneralSettings(
                pSize = 2.375,
                maxBendAngle = 225.0,
                maxTurningAngle = 90.0,
            )

            val tgs = GrowSettings(
                forbidTooClose = 0.2,
                postponeIntersections = false,
            )

            val settings = Settings(gs, tgs)

            return settings to 4.0
        }

        ExampleInput.HexaSwirl -> {
            val gs = GeneralSettings(
                pSize = 1.76,
                maxBendAngle = 180.0,
                maxTurningAngle = 70.0,
            )

            val tgs = GrowSettings(
                forbidTooClose = 0.3,
                postponeIntersections = false,
            )

            val settings = Settings(gs, tgs)

            return settings to 4.0
        }

        ExampleInput.Bonn -> {
            val gs = GeneralSettings(
                pSize = 0.653,
                maxBendAngle = 180.0,
                maxTurningAngle = 70.0,
            )

            val tgs = GrowSettings(
                forbidTooClose = 0.4,
                postponeIntersections = false,
            )

            val settings = Settings(gs, tgs)

            return settings to 6.0
        }

        ExampleInput.Diseasome -> {
            val gs = GeneralSettings(
                pSize = 5.204,
                maxBendAngle = 180.0,
                maxTurningAngle = 70.0,
            )

            val tgs = GrowSettings(
                forbidTooClose = 0.4,
                postponeIntersections = false,
            )

            val ds = DrawSettings(colors = newDiseasome)

            val settings = Settings(gs=gs, tgs=tgs, ds=ds)

            return settings to 5.913
        }

        ExampleInput.Intertwined -> {
            val gs = GeneralSettings(
                pSize = 2.0,
                maxBendAngle = 180.0,
                maxTurningAngle = 70.0,
            )

            val tgs = GrowSettings(
                postponeIntersections = false,
            )

            val settings = Settings(gs=gs, tgs=tgs)

            return settings to 4.0
        }
    }
}

fun nodesToPoints(nodes: List<Map<String, String>>): List<Point> {
    val colorMap = mutableMapOf<String, Int>(
        "CB light blue" to 0,
        "CB light red" to 1,
        "CB light green" to 2,
        "CB light orange" to 3,
        "CB light purple" to 4,
        "CB yellow" to 5,
        "CB brown" to 6,
        "CART 1" to 0,
        "CART 2" to 1,
        "CART 3" to 2,
        "CART 4" to 3,
        "CART 5" to 4,
        "CART 6" to 5,
        "CART 7" to 6,
        "CART 8" to 7,
        "CART 9" to 8,
        "CART 10" to 9,
        "CART 11" to 10,
        "CART 12" to 11,
    )
    var last = 7
    return nodes.map { m ->
        val posString = m["pos"]!!.split(' ')
        val x = posString[0].toDouble()
        val y = posString[1].toDouble()
        val matrixString = m["matrix"]?.split(' ')
        val a = matrixString?.get(0)?.toDouble() ?: 1.0
        val b = matrixString?.get(1)?.toDouble() ?: 0.0
        val c = matrixString?.get(2)?.toDouble() ?: 0.0
        val d = matrixString?.get(3)?.toDouble() ?: 1.0
        val e = matrixString?.get(4)?.toDouble() ?: 0.0
        val f = matrixString?.get(5)?.toDouble() ?: 0.0
        val pos = Vector2(a * x + c * y + e, b * x + d * y + f)

        val fill = m["fill"]!!
        val type = colorMap[fill] ?: run {
            colorMap[fill] = last
            last++
            last - 1
        }
        Point(pos, type)
    }
}