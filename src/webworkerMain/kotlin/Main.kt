import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.DedicatedWorkerGlobalScope
import org.w3c.dom.MessageEvent

external val self: DedicatedWorkerGlobalScope

fun main() {
    var lastFiltration: List<Pair<Double, Partition>>? = null
    var lastComputeAssignment: Compute? = null
    var lastCover: Double? = null
    var lastDrawSettings: DrawSettings? = null
    var lastXGraph: XGraph? = null

    self.onmessage = { m: MessageEvent ->
        val assignment: Assignment = Json.decodeFromString(m.data as String)
        val svg = when (assignment) {
            is Compute -> {
                with(assignment) {
                    val filtration = topoGrow(points, gs, tgs, 8 * assignment.gs.expandRadius) {
                        val progress: Answer = Progress(it)
                        self.postMessage(Json.encodeToString(progress))
                    }
                    lastFiltration = filtration
                    lastComputeAssignment = assignment
                    lastCover = cover
                    lastDrawSettings = ds
                    lastXGraph = computeXGraph(gs, cds, filtration, cover)
                    lastXGraph?.let { createSvg(points, it, gs, ds) }
                }
            }

            is DrawSvg -> {
                if (lastFiltration == null || lastComputeAssignment == null || lastCover == null || lastXGraph == null) {
                    null
                } else {
                    lastDrawSettings = assignment.drawSettings
                    createSvg(lastComputeAssignment!!.points, lastXGraph!!, lastComputeAssignment!!.gs, assignment.drawSettings)
                }
            }

            is ChangeCover -> {
                if (lastComputeAssignment == null || lastFiltration == null) null
                else {
                    lastCover = assignment.cover
                    with(lastComputeAssignment!!) {
                        lastXGraph = computeXGraph(gs, cds, lastFiltration!!, lastCover!!)
                        createSvg(points, lastXGraph!!, lastComputeAssignment!!.gs, lastDrawSettings!!)
                    }
                }
            }
        }

        if (svg != null) {
            val completedWork: Answer = CompletedWork(svg)
            self.postMessage(Json.encodeToString(completedWork))
        }
    }
}