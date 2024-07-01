import dilated.dilate
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.openrndr.KEY_BACKSPACE
import org.openrndr.KEY_SPACEBAR
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.color.presets.BLUE_STEEL
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.GUIAppearance
import org.openrndr.extra.parameters.*
import org.openrndr.launch
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Composition
import org.openrndr.shape.LineSegment
import org.openrndr.svg.toSVG
import patterns.Pattern
import patterns.bounds
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.round

@OptIn(DelicateCoroutinesApi::class)
fun main() = application {
    configure {
        width = 800
        height = 800
        windowResizable = true
        title = "SimpleSets"
    }

    program {
        var partition: Partition = Partition.EMPTY
        var filtration: List<Pair<Double, Partition>> = emptyList()
        var dilatedPolies = listOf<Pattern>()
        var composition: (Boolean) -> Composition = { _ -> drawComposition { } }
        var calculating = false

        fun asyncCompute(block: () -> Unit) {
            launch {
                GlobalScope.launch {
                    try {
                        calculating = true
                        block()
                    }
                    catch(e: Throwable) {
                        e.printStackTrace()
                        calculating = false
                    }
                    calculating = false
                }.join()
            }
        }

        val gui = GUI(GUIAppearance(ColorRGBa.BLUE_STEEL))

        val ds = DrawSettings()
        val gs = GeneralSettings()
        val cds = ComputeDrawingSettings()
        val tgs = GrowSettings()

        var xGraph = XGraph(emptyList(), gs, cds, morph = ::erodeDilate)

        fun clearData(clearPartition: Boolean = true){
            if (clearPartition) {
                partition = Partition.EMPTY
                filtration = emptyList()
            }
            dilatedPolies = emptyList()
            xGraph = XGraph(emptyList(), gs, cds, morph = ::erodeDilate)
            composition = { _ -> drawComposition { } }
        }

        val cs = object {
            @DoubleParameter("Cover", 1.0, 8.0)
            var cover: Double = 3.0
        }

        val camera = Camera2D()

        val transformMatrix = transform {
            translate(0.0, height.toDouble())
            scale(1.0, -1.0)
        }

        val uiSettings = object {
            @ActionParameter("Fit to screen")
            fun fitToScreen() {
                val rect = partition.points.bounds
                    .offsetEdges(ds.contourStrokeWeight(gs) + 2 * gs.expandRadius)
                    .contour
                    .transform(transformMatrix)
                    .bounds
                val matrix = Matrix44.fit(rect, drawer.bounds)// * transform { translate(0.0, drawer.bounds.height) }
                camera.view = matrix.inversed
            }

            @ActionParameter("Reset viewport")
            fun resetViewport() {
                camera.view = Matrix44.IDENTITY
            }
        }

        val ps = object {
            @BooleanParameter("Auto compute drawing")
            var computeDrawing = true

            @ActionParameter("Compute drawing")
            fun computeDrawing() {
                try {
                    dilatedPolies = partition.patterns.map { it.dilate(gs.expandRadius) }
                    xGraph = XGraph(dilatedPolies, gs, cds, morph=::erodeDilate, debug={
                            comp, fileName ->
//                        val timeStamp = ZonedDateTime
//                            .now( ZoneId.systemDefault() )
//                            .format( DateTimeFormatter.ofPattern( "uuuu.MM.dd.HH.mm.ss" ) )
                        val f = File("${fileName}.svg")

                        f.writeText(comp.toSVG())
                        "py svgtoipe.py ${fileName}.svg".runCommand(File("."))
                    })
                }
                catch(e: Throwable) {
                    e.printStackTrace()
                }
            }

            fun modifiedPartition() {
                clearData(false)

                if (computeDrawing) {
                    computeDrawing()
                }
            }

            fun computeFiltration() {
                asyncCompute {
                    println("#Points: ${partition.points.size}")
                    val start = System.currentTimeMillis()
                    filtration = topoGrow(partition.points, gs, tgs, 8 * gs.expandRadius)
                    val end = System.currentTimeMillis()
                    println("topoGrow took: ${end - start}ms")
                    val newPartition = filtration.takeWhile { it.first < cs.cover * gs.expandRadius }.lastOrNull()?.second
                    if (newPartition != null) {
                        partition = newPartition
                        modifiedPartition()
                    }
                    val realEnd = System.currentTimeMillis()
                    println("Total took: ${realEnd - start}ms")
                }
            }
        }

        val inputOutputSettings = object {
            @OptionParameter("Example input", order = 10)
            var exampleInput = ExampleInput.NYC

            @ActionParameter("Load example input", order = 11)
            fun loadExample() {
                clearData()
                partition = Partition(getExampleInput(exampleInput).toMutableList())

                val (gSettings, gCover) = goodSettings(exampleInput)
                cs.cover = gCover
                val (gGs, gTgs, _, gDs) = gSettings
                gs.pSize = gGs.pSize
                gs.bendInflection = gGs.bendInflection
                gs.maxTurningAngle = gGs.maxTurningAngle
                gs.maxBendAngle = gGs.maxBendAngle
                tgs.forbidTooClose = gTgs.forbidTooClose
                tgs.postponeIntersections = gTgs.postponeIntersections
                ds.colors = gDs.colors

                ps.modifiedPartition()

                uiSettings.fitToScreen()
            }

            @TextParameter("Input ipe file (with extension)", order = 14)
            var inputFileName = "nyc"

            @ActionParameter("Load input file", order = 15)
            fun loadInput() {
                clearData()
                try {
                    val file = File("input-output/$inputFileName")
                    val pts = when(file.extension) {
                        "ipe" -> {
                            ipeToPoints(file.readText())
                        }

                        else -> {
                            parsePoints(file.readText())
                        }
                    }

                    partition = Partition(pts.toMutableList())
                    ps.modifiedPartition()
                } catch (e: IOException) {
                    println("Could not read input file")
                    e.printStackTrace()
                }
            }

            @TextParameter("Output file (no extension)", order = 20)
            var outputFileName = "output"

            @ActionParameter("Save points", order = 23)
            fun savePoints() {
                writeToIpe(partition.points, "input-output/$outputFileName-points.ipe")
                val txt = pointsToText(partition.points)
                File("input-output/$outputFileName-points.txt").writeText(txt)
            }

            @ActionParameter("Save output", order = 25)
            fun saveOutput() {
                val svg = composition(false).toSVG()
                File("input-output/$outputFileName.svg").writeText(svg)
                gui.saveParameters(File("input-output/$outputFileName-parameters.json"))
                "py svgtoipe.py input-output/$outputFileName.svg".runCommand(File("."))
            }
        }

        gui.add(inputOutputSettings, "Input output settings")
        gui.add(uiSettings, "UI Settings")
        gui.add(gs, "General settings")
        gui.add(tgs, "Grow settings")
        gui.add(cs, "Cover settings")
        gui.add(cds, "Compute drawing settings")
        gui.add(ps, "Pipeline")

        extend(gui)
        gui.compartmentsCollapsedByDefault = false

        class Grid(val cellSize: Double, val center: Vector2){
            fun snap(p: Vector2): Vector2 = (p - center).mapComponents { round(it / cellSize) * cellSize } + center

            fun draw(compositionDrawer: CompositionDrawer){
                val r = drawer.bounds
                val vLines = buildList {
                    var x = r.corner.x + (center.x.mod(cellSize))
                    while (x <= r.corner.x + r.width){
                        add(LineSegment(x, r.corner.y, x, r.corner.y + r.height))
                        x += cellSize
                    }
                }
                val hLines = buildList {
                    var y = r.corner.y + (center.y.mod(cellSize))
                    while (y <= r.corner.y + r.height){
                        add(LineSegment(r.corner.x, y, r.corner.x + r.width, y))
                        y += cellSize
                    }
                }
                compositionDrawer.isolated {
                    lineSegments(vLines + hLines)
                }
            }
        }

        gui.onChange { varName, _ ->
            if (varName == "pSize") {
                if (ps.computeDrawing) ps.computeDrawing()
            }

            if (varName == "cover" || varName == "pSize") {
                val newPartition = filtration.takeWhile { it.first < cs.cover * gs.expandRadius }.lastOrNull()?.second
                if (newPartition != null) {
                    partition = newPartition
                    ps.modifiedPartition()
                }
            }

            if (varName == "smoothing") {
                ps.computeDrawing()
            }
        }

        keyboard.keyDown.listen { keyEvent ->
            if (!keyEvent.propagationCancelled) {
                if (keyEvent.key == KEY_SPACEBAR) {
                    keyEvent.cancelPropagation()
                    if (calculating) return@listen

                    clearData(clearPartition = false)
                    asyncCompute {
                        ps.computeFiltration()
                    }
                }

                if (keyEvent.name == "c") {
                    keyEvent.cancelPropagation()
                    clearData()
                }

                if (keyEvent.key == KEY_BACKSPACE) {
                    keyEvent.cancelPropagation()
                    if (partition.points.isNotEmpty()){
                        partition.removeLast()
                    }
                }
            }
        }

        val font = loadFont("data/fonts/default.otf", 16.0)
        extend(camera)
        extend {
            drawer.fontMap = font
            drawer.fill = ColorRGBa.BLACK
            drawer.clear(ColorRGBa.WHITE)

            composition = { showMouse -> drawComposition {
                model *= transformMatrix
                xGraph.draw(this, ds)
                coloredPoints(partition.points, gs, ds)
            }}
            drawer.composition(composition(true))
        }
    }
}

fun Vector2.mapComponents(f: (Double) -> Double) = Vector2(f(x), f(y))

fun String.runCommand(workingDir: File) {
    ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor(60, TimeUnit.MINUTES)
}
