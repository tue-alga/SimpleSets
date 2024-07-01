import patterns.Point
import patterns.roundToDecimals
import patterns.v

fun parsePoints(s: String): List<Point> =
    s.lines()
        .filter { it.isNotEmpty() }
        .map {
            val words = it.split(' ')
            val t = words[0].toInt()
            val x = words[1].toDouble()
            val y = words[2].toDouble()
            Point(x v y, t)
        }

fun pointsToText(pts: List<Point>): String =
    pts.joinToString("\n") {
        it.run {
            "$type ${pos.x.roundToDecimals(3)} ${pos.y.roundToDecimals(3)}"
        }
    }