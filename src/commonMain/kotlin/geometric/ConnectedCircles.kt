package geometric

import org.openrndr.shape.Circle

private fun Circle.overlapsWith(circles: List<Circle>): Boolean {
    return circles.any {
        it.contour.overlaps(contour)
    }
}

fun connectedCircles(circles: List<Circle>): List<List<Circle>> {
    val partition = mutableListOf<MutableList<Circle>>()

    fun merge(components: List<MutableList<Circle>>): MutableList<Circle> {
        val newComponent = mutableListOf<Circle>()
        for (comp in components) {
            newComponent.addAll(comp)
            comp.clear()
        }
        partition.removeAll { it.isEmpty() }
        return newComponent
    }

    for (c in circles) {
        val overlaps = partition.filter { c.overlapsWith(it) }
        val merged = merge(overlaps)
        merged.add(c)
        partition.add(merged)
    }

    return partition
}