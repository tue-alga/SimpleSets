import dilated.ContourHighlight
import geometric.*
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.*
import patterns.Pattern

data class XEdge(val hIndex: Int, val source: XVertex, val target: XVertex, val contour: ShapeContour) {
    val start get() = contour.start
    val end get() = contour.end

    var halfEdges: Pair<XHalfEdge, XHalfEdge>? = null

    fun split(): Pair<XHalfEdge, XHalfEdge> {
        val hes = XHalfEdge(source, target, contour, this) to XHalfEdge(target, source, contour.reversed, this)
        hes.first.twin = hes.second
        hes.second.twin = hes.first
        halfEdges = hes
        return hes
    }

    fun splitAndAdd(): Pair<XHalfEdge, XHalfEdge> {
        split()
        source.outgoing.add(halfEdges!!.first)
        target.outgoing.add(halfEdges!!.second)
        source.incoming.add(halfEdges!!.second)
        target.incoming.add(halfEdges!!.first)
        return halfEdges!!
    }
}

data class XVertex(val x: ContourIntersection, val h1: Pattern, val h2: Pattern) {
    val outgoing = mutableListOf<XHalfEdge>()
    val incoming = mutableListOf<XHalfEdge>()
}

data class XHalfEdge(val source: XVertex, val target: XVertex, val contour: ShapeContour, val original: XEdge) {
    val start get() = contour.start
    val end get() = contour.end

    val contourE by lazy {
        contour.extend(0.001)
    }

    lateinit var twin: XHalfEdge

    lateinit var face: XFace

    val next by lazy {
        target.outgoing.map {
            val y = target.x.position
            val z = y + it.contour.direction(0.0)
            val x = y - contour.direction(1.0)
            it to orientationD(x, y, z)
        }.maxBy { it.second }.first
    }

    val prev by lazy {
        source.incoming.map {
            val y = source.x.position
            val x = y - it.contour.direction(1.0)
            val z = y + contour.direction(0.0)
            it to orientationD(x, y, z)
        }.maxBy { it.second }.first
    }
}

enum class Ordering {
    LT,
    EQ,
    GT
}

data class Relation(val left: Int, val right: Int, val preference: Ordering, var order: Ordering = preference) {
    val top get() = if (order != Ordering.LT) left else right
    val bottom get() = if (order != Ordering.LT) right else left

    val hyperedges = mutableListOf<Hyperedge>()
}

fun computeTotalOrder(origins: List<Int>, relations: List<Relation>): List<Int>? {
    data class Vertex(val i: Int) {
        val neighbors = mutableListOf<Vertex>()
        var mark = 0
    }

    val vertices = origins.associateWith { Vertex(it) }

    for (p in relations) {
        if (p.order == Ordering.EQ) continue
        val u = vertices[p.left]!!
        val v = vertices[p.right]!!

        if (p.order == Ordering.LT) {
            v.neighbors.add(u)
        } else {
            u.neighbors.add(v)
        }
    }

    val ordering = mutableListOf<Int>()

    fun visit(u: Vertex): Boolean {
        if (u.mark == 2) return true
        if (u.mark == 1) return false

        u.mark = 1

        for (v in u.neighbors) {
            val success = visit(v)
            if (!success) return false
        }

        u.mark = 2
        ordering.add(u.i)
        return true
    }

    for (v in vertices.values) {
        val success = visit(v)
        if (!success) return null
    }

    return ordering
}

data class XFace(val edge: XHalfEdge, val origins: List<Int>, val contour: ShapeContour?) {
    val relations = mutableListOf<Relation>()

    val morphedContours = mutableMapOf<Int, ShapeContour>()
    val morphedEdge = mutableMapOf<Int, MutableList<Shape>>()
    var wasMorphed = mutableMapOf<Int, Boolean>()

    val edges: List<XHalfEdge> by lazy {
        buildList {
            var current = edge
            do {
                add(current)
                current = current.next
            } while (current != edge)
        }
    }

    // Access only when ordering is final!
    val ordering: List<Int> by lazy {
        computeTotalOrder(origins, relations) ?: error("No total order in face")
    }

    val top get() =
        ordering.last()

    fun setMorphedEdge(i: Int, morphed: ShapeContour) {
        wasMorphed[i] = true
        val result = intersection(morphed, contour!!.offsetFix(0.01))
        val ml = morphedEdge[i]
        if (ml != null)
            ml.add(result)
        else
            morphedEdge[i] = mutableListOf(result)
    }

    fun setMorphedFace(i: Int, morphed: ShapeContour) {
        val morphedFaceContour = intersection(morphed.fix(0.1), contour!!.offsetFix(0.01).fix(0.1)).contours.firstOrNull() ?: ShapeContour.EMPTY
        morphedContours[i] = morphedFaceContour
    }

    val seamDrawing: ((List<Pattern>, GeneralSettings, DrawSettings) -> Composition)? by lazy {
        if (contour == null) return@lazy null

        val toDraw = buildList {
            for (i in ordering.asReversed()) {
                add( 0, i)
                if (morphedContours[last()] == null) break
            }
        }

        fun draw(hs: List<Pattern>, gs: GeneralSettings, ds: DrawSettings): Composition {
            return drawComposition {
                for (i in toDraw) {
                    val color = (ds.colors.getOrNull(hs[i].type) ?: ColorRGBa.WHITE).mix(ColorRGBa.WHITE, ds.whiten)
                    stroke = color
                    strokeWeight = ds.contourStrokeWeight(gs) / 2
                    fill = null
                    contour(morphedContours[i] ?: contour)
                }
            }
        }

        ::draw
    }

    val fillDrawing: ((List<Pattern>, DrawSettings) -> Composition)? by lazy {
        if (contour == null) return@lazy null

        val toDraw = buildList {
            for (i in ordering.asReversed()) {
                add( 0, i)
                if (morphedContours[last()] == null) break
            }
        }

        fun draw(hs: List<Pattern>, ds: DrawSettings): Composition {
            return drawComposition {
                for (i in toDraw) {
                    val color = (ds.colors.getOrNull(hs[i].type) ?: ColorRGBa.WHITE).mix(ColorRGBa.WHITE, ds.whiten)
                    stroke = null
                    fill = color
                    contour(morphedContours[i] ?: contour)
                }
            }
        }

        ::draw
    }

    val strokeDrawing: ((GeneralSettings, DrawSettings) -> Composition)? by lazy {
        if (contour == null) return@lazy null

        val toDraw = buildList {
            for (i in ordering.asReversed()) {
                add( 0, i)
                if (morphedContours[first()] == null) break
            }
        }

        val edgeContours = buildList {
            for ((iIndex, i) in toDraw.withIndex()) {
                val conts = mutableListOf<ShapeContour>()
                conts.addAll(morphedEdge[i]?.flatMap { it.contours } ?: emptyList())

                if (!wasMorphed.getOrElse(i) { false }) {
                    var current = edge
                    var iters = 0
                    do {
                        iters++
                        if (current.original.hIndex == i) {
                            conts.add(current.contour)
                        }
                        current = current.next
                    } while (current != edge && iters < 1000)
                    if (iters >= 1000) {
                        error("Problem")
                    }
                }

                for (cont in conts) {
                    var modified = Shape(listOf(cont))
                    for (jIndex in iIndex + 1 until toDraw.size) {
                        if (!morphedContours[toDraw[jIndex]]!!.empty)
                            modified = difference(modified, morphedContours[toDraw[jIndex]]!!.offsetFix(0.1))
                    }
                    addAll(modified.contours)
                }
            }
        }

        fun draw(gs: GeneralSettings, ds: DrawSettings): Composition {
            return drawComposition {
                stroke = ColorRGBa.BLACK
                strokeWeight = ds.contourStrokeWeight(gs)
                fill = null
                for (ec in edgeContours) {
                    contour(ec)
                }
            }
        }

        ::draw
    }
}

typealias Morpher = (ShapeContour, ShapeContour, Orientation, List<Circle>, List<Circle>, GeneralSettings, ComputeDrawingSettings, Debug) -> ShapeContour

data class XGraph(val hs: List<Pattern>, val gs: GeneralSettings, val cds: ComputeDrawingSettings, val debug: Debug = noDebug, val morph: Morpher = ::erodeDilate) {
    val hVertsMap = List(hs.size) {
        mutableListOf<Pair<XVertex, Double>>()
    }

    val vertices = buildList {
        for (i in hs.indices) {
            val h1 = hs[i]
            for (j in i + 1 until hs.size) {
                val h2 = hs[j]
                val inters = h1.contour.intersections(h2.contour)
                for (inter in inters) {
                    val v = XVertex(inter, h1, h2)
                    add(v)
                    hVertsMap[i].add(v to inter.a.contourT)
                    hVertsMap[j].add(v to inter.b.contourT)
                }
            }
        }
    }

    val hEdgesMap = List(hs.size) {
        mutableListOf<XEdge>()
    }

    val edges by lazy { hEdgesMap.flatten() }

    val halfEdges by lazy {
        edges.flatMap {
            it.halfEdges?.toList() ?: emptyList()
        }
    }

    val faces = mutableListOf<XFace>()

    val hFacesMap = List(hs.size) {
        mutableListOf<XFace>()
    }

    val pairComponents = mutableMapOf<Pair<Int, Int>, List<Component>>()

    init {
        createEdges()
        createFaces()

        for (i in hs.indices) {
            for (j in i + 1 until hs.size) {
                val cs = intersectionComponents(i, j)
                pairComponents[i to j] = cs
                for (c in cs) {
                    val rel = computePreference(i, j, c)
                    for (f in c.faces) {
                        f.relations.add(rel)
                    }
                }
            }
        }

        val hEdges = hyperedges()
        for (e in hEdges) {
            // TODO: resolve conflicting preferences
            e.setOrdering()
        }

        for (i in hs.indices) {
            val cs = intersectionComponents(i)
            for (c in cs) {
                val avoidees = mutableSetOf<Int>()
                for (f in c.faces) {
                    avoidees.addAll(f.ordering.takeWhile { it != i })
                }
                if (avoidees.isEmpty()) continue

                val morpheds = c.boundaryPart(i).mapNotNull { cont ->
                    val (inclCircles, exclCircles) = relevantCircles(i, avoidees.toList(), c)

                    if (exclCircles.isEmpty()) null
                    else if (cont.empty) null
                    else {
                        val closePoint = hs[i].points.minBy { cont.squaredDistanceTo(it.pos) }
                        val near = cont.nearest(closePoint.pos)
                        val nearDelta = cont.position(near.contourT + 0.01)
                        val orient = orientation(near.position, nearDelta, closePoint.pos)
                        val morphed = morph(cont, c.contour, orient, inclCircles, exclCircles, gs, cds, debug)

                        for (f in c.faces) {
                            f.setMorphedEdge(i, morphed)
                        }

                        morphed
                    }
                }

                if (morpheds.isEmpty()) continue
                val restConts = ccFaces(hFacesMap[i] - c.faces).flatMap { it.boundaryPart(i) }
                val full = (morpheds + restConts).filterNot { it.empty }.merge().close()
                for (f in c.faces) {
                    f.setMorphedFace(i, full)
                }
            }
        }
    }

    private fun createEdges() {
        for (i in hs.indices) {
            val h = hs[i]
            if (hVertsMap[i].isEmpty()) continue
            val tValues = hVertsMap[i].sortedBy { it.second }
            val middleEdges = tValues.zipWithNext { (v1, t1), (v2, t2) ->
                val e = XEdge(i, v1, v2, h.contour.sub(t1, t2))
                e.splitAndAdd()
                e
            }
            val (lastV, lastT) = tValues.last()
            val (firstV, firstT) = tValues.first()
            val lastEdge = XEdge(i, lastV, firstV, h.contour.sub(lastT, 1.0) + h.contour.sub(0.0, firstT))
            lastEdge.splitAndAdd()
            hEdgesMap[i].addAll(middleEdges)
            hEdgesMap[i].add(lastEdge)
        }
    }

    private fun createFaces() {
        val remainingHalfEdges = halfEdges.toMutableList()

        while(remainingHalfEdges.isNotEmpty()) {
            val heStart = remainingHalfEdges.first()
            val visited = mutableListOf(heStart)
            var current = heStart
            var faceContour = heStart.contour

            var iters = 0
            while (current.next != heStart && iters < 100) {
                iters++
                current = current.next
                visited.add(current)
                faceContour += current.contour
            }
            if (iters >= 100) {
                val comp = drawComposition {
                    for (e in visited) {
                        fill = null
                        stroke = ColorRGBa.BLACK.opacify(0.01)
                        contour(e.contour)
                    }
                }
                debug(comp, "debug-loop-edges")
                error("Problem with computing faces (a face seems to exist of more than 1000 half edges)")
            }

            remainingHalfEdges.removeAll(visited)

            val facePt = heStart.contour.position(0.5) + heStart.contour.normal(0.5) * -0.01
            val origins = hs.withIndex().filter { facePt in it.value.contour }.map { it.index }
            val finalFaceContour = if (origins.isNotEmpty())
                faceContour.close() else null

            val f = XFace(heStart, origins, finalFaceContour)
            faces.add(f)
            for (i in origins) {
                hFacesMap[i].add(f)
            }

            visited.forEach { e ->
                e.face = f
            }
        }
    }

    fun intersectionComponents(i: Int): List<Component> {
        val intersectedFaces = hFacesMap[i].filter { it.origins.size > 1 }.toMutableList()
        return ccFaces(intersectedFaces)
    }

    private fun ccFaces(subset: List<XFace>): List<Component> {
        val remainder = subset.toMutableList()
        val components = mutableListOf<Component>()

        var iters = 0
        while (remainder.isNotEmpty() && iters < 1000000 ) {
            iters++
            val component = mutableListOf<XFace>()
            val first = remainder.first()
            val q = mutableListOf(first)

            while (q.isNotEmpty() && iters < 1000000) {
                iters++
                val f = q.removeFirst()
                component.add(f)
                val startEdge = f.edge
                var currentEdge = startEdge

                do {
                    iters++
                    val candidate = currentEdge.twin.face
                    if (candidate !in component && candidate in remainder && candidate !in q) {
                        q.add(candidate)
                    }
                    currentEdge = currentEdge.next
                } while (currentEdge != startEdge && iters < 1000000)
            }

            components.add(Component(component))
            remainder.removeAll(component)
        }
        if (iters >= 1000000) error("Problem")

        return components.toList()
    }

    fun intersectionComponents(i: Int, j: Int): List<Component> {
        val commonFaces = faces.filter { i in it.origins && j in it.origins }
        return ccFaces(commonFaces)
    }

    fun relevantCircles(i: Int, j: Int, c: Component): Pair<List<Circle>, List<Circle>> {
        val r = gs.expandRadius
        val leftCircles = hs[i].points.map { Circle(it.pos, r) }
        val rightCircles = hs[j].points.map { Circle(it.pos, r) }

        val (includedCircles, excludedCircles) = growCircles(
            leftCircles.map { it.center },
            rightCircles.map { it.center },
            r,
            r * cds.pointClearance
        )

        // TODO: Make empty intersection check more efficient
        return includedCircles to //.filterNot { it.radius == 0.0 || intersection(c.contour, it.shape).empty } to
                excludedCircles.filterNot { it.radius == 0.0 ||
                        c.contour.nearest(it.center).position.distanceTo(it.center) > gs.expandRadius ||
                        intersection(c.contour, it.shape).empty
                }
    }

    fun relevantCircles(i: Int, avoidees: List<Int>, c: Component): Pair<List<Circle>, List<Circle>> {
        val r = gs.expandRadius
        val leftCircles = hs[i].points.map { Circle(it.pos, r) }
        val rightCircles = avoidees.flatMap { j -> hs[j].points.map { Circle(it.pos, r) } }

        val (includedCircles, excludedCircles) = growCircles(
            leftCircles.map { it.center },
            rightCircles.map { it.center },
            r,
            r * cds.pointClearance
        )

        return includedCircles to //.filterNot { it.radius == 0.0 || intersection(c.contour, it.shape).empty } to
                excludedCircles.filterNot { it.radius == 0.0 ||
                        c.contour.nearest(it.center).position.distanceTo(it.center) > gs.expandRadius ||
                        intersection(c.contour, it.shape).empty
                }
    }

    fun computePreference(i: Int, j: Int, c: Component): Relation {
        var ord = Ordering.EQ

        val (_, rjcs) = relevantCircles(i, j, c)
        val (_, rics) = relevantCircles(j, i, c)

        // 3. Check if circular or straight part is covered. Prefer straight part being covered
        val es = c.faces.flatMap {
            it.edges
        }

        val iStraight = es.filter {
            it.original.hIndex == i
        }.all { it.contour.isStraight() }

        val jStraight = es.filter {
            it.original.hIndex == j
        }.all { it.contour.isStraight() }

        if (iStraight && !jStraight) {
            ord = Ordering.LT
        }

        if (jStraight && !iStraight) {
            ord = Ordering.GT
        }

        val ci = c.boundaryPart(i)
        val brokenI = ci.map { cont ->
            rjcs.mapNotNull { intersection(cont, it.shape).contours.firstOrNull() }
        }.flatten()
        val cj = c.boundaryPart(j)
        val brokenJ = cj.map { cont ->
            rics.mapNotNull { intersection(cont, it.shape).contours.firstOrNull() }
        }.flatten()

        // 2. Check if circular or straight part is indented. Highly prefer straight part being indented.
        if (brokenI.any { !it.isStraight() } && brokenJ.all { it.isStraight() }) {
            ord = Ordering.LT
        }

        if (brokenJ.any { !it.isStraight() } && brokenI.all { it.isStraight() }) {
            ord = Ordering.GT
        }

        // 1.
        if (rics.size < rjcs.size) {
            ord = Ordering.LT // Prefer i on top
        }

        if (rjcs.size < rics.size) {
            ord = Ordering.GT
        }

        return Relation(i, j, ord)
    }

    fun hyperedges(): List<Hyperedge> {
        val candidates = faces
            .filter { it.origins.size >= 3 }
            .groupBy { it.origins.size }
            .mapValues { it.value.map { Hyperedge(it.origins, it.relations.toList()) }.toMutableList() }

        val trashBin = mutableListOf<Pair<Int, Hyperedge>>()

        for ((i, edges) in candidates) {
            for (edge in edges) {
                for (larger in candidates[i+1] ?: break) {
                    if (larger.relations.containsAll(edge.relations)) {
                        trashBin.add(i to edge)
                    }
                }
            }
        }

        for ((i, e) in trashBin) {
            candidates[i]!!.remove(e)
        }

        val hEdges = candidates.values.flatten()

        for (e in hEdges) {
            for (r in e.relations) {
                r.hyperedges.add(e)
            }
        }

        return hEdges
    }

    fun totalOrder(): List<Int>? {
        // Get all relations. This is a bit inefficient currently
        val relations = mutableListOf<Relation>()
        for (i in hs.indices) {
            for (f in hFacesMap[i]) {
                for (r in f.relations) {
                    if (relations.find { it.left == r.left && it.right == r.right } == null) {
                        relations.add(r)
                    }
                }
            }
        }
        return computeTotalOrder(hs.indices.toList(), relations)
    }

    fun draw(drawer: CompositionDrawer, ds: DrawSettings) {
        val order = totalOrder()
        if (order != null) {
            for (i in order) {
                // draw hs[i]
                if (hVertsMap[i].isEmpty()) {
                    drawer.patternContour(hs[i], gs, ds)
                } else {
                    val boundaryPieces = mutableListOf<ShapeContour>()
                    for (f in hFacesMap[i]) {
                        boundaryPieces.addAll(f.morphedEdge[i]?.flatMap { it.contours } ?: emptyList())
                        if (!f.wasMorphed.getOrElse(i) { false }) {
                            var current = f.edge
                            var iters = 0
                            do {
                                iters++
                                if (current.original.hIndex == i) {
                                    boundaryPieces.add(current.contour)
                                }
                                current = current.next
                            } while (current != f.edge && iters < 1000)
                            if (iters >= 1000) {
                                error("Problem")
                            }
                        }
                    }
                    drawer.patternContour(ContourHighlight(boundaryPieces.merge().close(), hs[i].points), gs, ds)
                }
            }
            return
        }

        drawer.apply {
            for (f in faces) {
                composition((f.seamDrawing ?: continue)(hs, gs, ds))
            }

            for (f in faces) {
                composition((f.fillDrawing ?: continue)(hs, ds))
            }

            for (f in faces) {
                composition((f.strokeDrawing ?: continue)(gs, ds))
            }

            for (i in hs.indices) {
                if (hVertsMap[i].isEmpty()) {
                    patternContour(hs[i], gs, ds)
                }
            }
        }
    }
}

data class Component(val faces: List<XFace>) {
    val origins = buildSet {
        for (f in faces)
            addAll(f.origins)
    }

    val XHalfEdge.nextBoundaryEdge: XHalfEdge
        get() {
        var current = next
        var iters = 0
        while (current.twin.face in faces && iters < 1000) {
            iters++
            current = current.twin.next
        }
        if (iters >= 1000) error("Problem")
        return current
    }

    val XHalfEdge.prevBoundaryEdge get() =
        if (prev.twin.face !in faces) prev else prev.twin.prev

    val contour: ShapeContour by lazy {
        val boundaryEdge = run {
            for (f in faces) {
                val startEdge = f.edge
                var currentEdge = startEdge

                var iters = 0
                do {
                    iters++
                    if (currentEdge.twin.face !in faces) return@run currentEdge
                    currentEdge = currentEdge.next
                } while (currentEdge != startEdge && iters < 1000)
                if (iters >= 1000) error("Problem")
            }
            error("Could not found a boundary edge of component $this")
        }

        var currentEdge = boundaryEdge
        var c = boundaryEdge.contour

        var iters = 0
        while (iters < 1000) {
            iters++
            currentEdge = currentEdge.nextBoundaryEdge
            if (currentEdge == boundaryEdge) break
            c += currentEdge.contour
        }
        if (iters > 1000) error("Problem")

        c.close()
    }

    fun boundaryPart(i: Int): List<ShapeContour> {
        val startEdge = run {
            var candidate: XHalfEdge? = null
            for (f in faces) {
                val startEdge = f.edge
                var currentEdge = startEdge

                var iters = 0
                do {
                    iters++
                    if (currentEdge.original.hIndex == i) {
                        candidate = currentEdge
                        if (currentEdge.prevBoundaryEdge.original.hIndex != i)
                            return@run currentEdge
                    }
                    currentEdge = currentEdge.next
                } while (currentEdge != startEdge && iters < 1000)
                if (iters >= 1000) error("Problem")
            }
            candidate ?: error("Could not found a boundary edge of component $this")
        }

        val contours = mutableListOf<ShapeContour>()

        var currentEdge = startEdge
        var c = startEdge.contour

        var iters = 0
        while (iters < 1000) {
            iters++
            currentEdge = currentEdge.nextBoundaryEdge
            if (currentEdge == startEdge) break
            else if (currentEdge.original.hIndex != i) {
                contours.add(c)
                c = ShapeContour.EMPTY
            }
            else
                c += currentEdge.contour
        }
        if (c != ShapeContour.EMPTY) contours.add(c)
        if (iters >= 1000) error("Problem")

        return contours
    }
}

data class Hyperedge(val origins: List<Int>, val relations: List<Relation>) {
    data class Vertex(val i: Int) {
        val neighbors = mutableListOf<Vertex>()
        var mark = 0
    }

    val vertices = origins.associateWith { Vertex(it) }

    init {
        for (p in relations) {
            if (p.order == Ordering.EQ) continue
            val u = vertices[p.left]!!
            val v = vertices[p.right]!!

            if (p.order == Ordering.LT) {
                v.neighbors.add(u)
            } else {
                u.neighbors.add(v)
            }
        }
    }

    fun ordering(): List<Int>? {
        val ordering = mutableListOf<Int>()

        fun visit(u: Vertex): Boolean {
            if (u.mark == 2) return true
            if (u.mark == 1) return false

            u.mark = 1

            for (v in u.neighbors) {
                val success = visit(v)
                if (!success) return false
            }

            u.mark = 2
            ordering.add(u.i)
            return true
        }

        for (v in vertices.values) {
            val success = visit(v)
            if (!success) return null
        }

        return ordering
    }

    fun setOrdering() {
        val ordering = ordering() ?: return
        for (r in relations) {
            val i = ordering.indexOf(r.left)
            val j = ordering.indexOf(r.right)
            r.order = if (i < j) Ordering.LT else Ordering.GT
        }
    }
}
