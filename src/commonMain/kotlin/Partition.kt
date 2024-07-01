import patterns.Pattern
import patterns.Point
import patterns.SinglePoint

data class Partition(val points: MutableList<Point>, val patterns: MutableList<Pattern>) {
    constructor(points: List<Point>): this(points.toMutableList(), points.map { SinglePoint(it) }.toMutableList())

    val pointToPattern: MutableMap<Point, Pattern> = buildMap {
        for (pattern in patterns) {
            for (pt in pattern.points) {
                put(pt, pattern)
            }
        }
    }.toMutableMap()

    fun add(p: Point) {
        points.add(p)
        val pattern = SinglePoint(p)
        patterns.add(pattern)
        pointToPattern[p] = pattern
    }

    fun removeAt(index: Int) {
        val pt = points.removeAt(index)
        pointToPattern.remove(pt)
        breakPatterns(pt)
    }

    fun removeLast() {
        val pt = points.removeLast()
        pointToPattern.remove(pt)
        breakPatterns(pt)
    }

    fun breakPatterns(pt: Point) {
        val pattern = patterns.find { pt in it.points }!!
        patterns.remove(pattern)
        val newPatterns = (pattern.points - pt).map { SinglePoint(it) }
        patterns.addAll(newPatterns)
        newPatterns.forEach {
            pointToPattern[it.point] = it
        }
    }

    // Assumes oldPattern1.points union oldPattern2.points = newPattern.points
    fun merge(oldPattern1: Pattern, oldPattern2: Pattern, newPattern: Pattern) {
        patterns.remove(oldPattern1)
        patterns.remove(oldPattern2)
        patterns.add(newPattern)
        for (p in newPattern.points) {
            pointToPattern[p] = newPattern
        }
    }

    fun copy(): Partition {
        return copy(points = points, patterns = patterns.toMutableList())
    }

    companion object {
        val EMPTY = Partition(mutableListOf(), mutableListOf())
    }
}