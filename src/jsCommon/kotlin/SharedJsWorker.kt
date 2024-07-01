import kotlinx.serialization.Serializable
import patterns.Point

//This source is shared between client and worker

@Serializable
sealed class Assignment

@Serializable
data class Compute(
    val points: List<Point>,
    val gs: GeneralSettings,
    val tgs: GrowSettings,
    val cds: ComputeDrawingSettings,
    val ds: DrawSettings,
    val cover: Double,
): Assignment()

@Serializable
data class ChangeCover(val cover: Double): Assignment()

@Serializable
data class DrawSvg(val drawSettings: DrawSettings): Assignment()

@Serializable
sealed class Answer

@Serializable
data class CompletedWork(val svg: String): Answer()

@Serializable
data class Progress(val progress: Double): Answer()