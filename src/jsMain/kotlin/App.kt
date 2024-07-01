import components.*
import contexts.*
import emotion.react.css
import js.objects.jso
import kotlinx.browser.window
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.openrndr.color.ColorRGBa
import org.openrndr.math.IntVector2
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker
import patterns.Point
import patterns.bounds
import patterns.roundToDecimals
import patterns.v
import react.*
import react.dom.events.MouseEvent
import react.dom.events.NativeMouseEvent
import react.dom.events.PointerEvent
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.progress
import react.dom.html.ReactHTML.textarea
import react.dom.svg.ReactSVG.circle
import react.dom.svg.ReactSVG.svg
import sideWindow.*
import sideWindow.settings.*
import web.blob.Blob
import web.blob.BlobPart
import web.cssom.*
import web.cssom.Auto.Companion.auto
import web.cssom.Globals.Companion.initial
import web.cssom.None.Companion.none
import web.dom.Element
import web.dom.document
import web.html.HTMLAnchorElement
import web.html.HTMLDivElement
import web.html.InputType
import web.uievents.MouseButton
import web.url.URL

enum class Tool {
    None,
    PlacePoints,
    MovePoints,
    RemovePoints,
}

val worker = Worker("worker.js")

// Card border radius
val cardBr = 20.px

val App = FC<Props> {
    var useGrid: Boolean by useState(true)

    val (pointSize, pointSizeSetter) = useState(5.0)
    val pointSettings = object: PointSettings {
        override var pointSize: Double
            get() = pointSize
            set(v) = pointSizeSetter(v)
    }

    var cover: Double by useState(3.0)

    val (bendInflection, bendInflectionSetter) = useState(true)
    val (maxBendAngle, maxBendAngleSetter) = useState(120.0)
    val (maxTurningAngle, maxTurningAngleSetter) = useState(60.0)

    val bendSettings = object: BendSettings {
        override var bendInflection: Boolean
            get() = bendInflection
            set(v) = bendInflectionSetter(v)
        override var maxBendAngle: Double
            get() = maxBendAngle
            set(v) = maxBendAngleSetter(v)
        override var maxTurningAngle: Double
            get() = maxTurningAngle
            set(v) = maxTurningAngleSetter(v)
    }

    val generalSettings = GeneralSettings(
        bendInflection = bendInflection,
        maxBendAngle = maxBendAngle,
        maxTurningAngle = maxTurningAngle,
        pSize = pointSize
    )

    val computeDrawingSettings = ComputeDrawingSettings()

    val (postponeIntersections, postponeIntersectionsSetter) = useState(false)
    val (forbidTooClose, forbidTooCloseSetter) = useState(0.5)

    val grow = object: Grow {
        override var postponeIntersections: Boolean
            get() = postponeIntersections
            set(v) = postponeIntersectionsSetter(v)
        override var forbidTooClose: Double
            get() = forbidTooClose
            set(v) = forbidTooCloseSetter(v)
    }

    val growSettings = GrowSettings(
        postponeIntersections = grow.postponeIntersections,
        forbidTooClose = grow.forbidTooClose
    )

    val cbColorsRGB = cbColors
    val (colors, colorsSetter) = useState(cbColorsRGB)
    val colorsObj = object: Colors {
        override val defaultColors: List<ColorRGBa>
            get() = cbColorsRGB
        override var colors: List<ColorRGBa>
            get() = colors
            set(v) = colorsSetter(v)

    }
    val drawSettings = DrawSettings(colors = colors)

    var viewMatrix: Matrix44 by useState(Matrix44.IDENTITY)
    val svgContainerRef: MutableRefObject<HTMLDivElement> = useRef(null)
    var svgSize by useState(IntVector2(svgContainerRef.current?.clientWidth ?: 0, svgContainerRef.current?.clientHeight ?: 0))
    val bottomLeft = viewMatrix * Vector2.ZERO
    val topRight = viewMatrix * svgSize
    val viewBoxTransform = "${bottomLeft.x} ${bottomLeft.y} ${topRight.x - bottomLeft.x} ${topRight.y - bottomLeft.y}"

    val emptySvg = ""
    var svg: String by useState(emptySvg)

    var tool: Tool by useState(Tool.PlacePoints)
    var points: List<Point> by useState(emptyList())

    val pointBounds = run {
        points.bounds.offsetEdges(drawSettings.contourStrokeWeight(generalSettings) + 2 * generalSettings.expandRadius)
    }

    var fittingToScreen: Boolean by useState(false)

    fun fitToScreen() {
        viewMatrix = Matrix44.fit(pointBounds, Rectangle(0.0, 0.0,
            svgSize.x.toDouble(), svgSize.y.toDouble()
        ))
    }

    var computing: Boolean by useState(false)

    var lastPoints: List<Point> by useState(emptyList())
    var lastGeneralSettings: GeneralSettings by useState(generalSettings)
    var lastComputeDrawingSettings: ComputeDrawingSettings by useState(computeDrawingSettings)
    var lastGrowSettings: GrowSettings by useState(growSettings)
    var lastSentPoints: List<Point> by useState(emptyList())
    var lastSentGeneralSettings: GeneralSettings by useState(generalSettings)
    var lastSentDrawingSettings: ComputeDrawingSettings by useState(computeDrawingSettings)
    var lastSentGrowSettings: GrowSettings by useState(growSettings)
    val changedProblem = points != lastPoints
            || generalSettings != lastGeneralSettings
            || computeDrawingSettings != lastComputeDrawingSettings
            || growSettings != lastGrowSettings

    var currentType: Int by useState(0)
    val currentColor = colors.getOrElse(currentType) { colors[0] }

    var evCache: List<PointerEvent<HTMLDivElement>> by useState(emptyList())
    var downEvent: PointerEvent<HTMLDivElement>? by useState(null)
    var prevDiff: Double? = null

    val (sideWindowRatio, setSideWindowRatio) = useState(0.382)

    val sideWindowContainer: MutableRefObject<HTMLDivElement> = useRef(null)

    val windowSize = useWindowSize()
    val horizontal = windowSize.x > windowSize.y

    var movingPoint: Int? by useState(null)

    var areaText: String by useState("")

    fun matchText(newPoints: List<Point>) {
        val newText = pointsToText(newPoints)
        if (parsePoints(newText) != parsePoints(areaText)) {
            areaText = newText
        }
    }

    fun matchPoints(newText: String) {
        try {
            val pts = parsePoints(newText)
            if (pts != points) {
                points = pts
            }
        } catch (_: Exception) {

        }
    }

    var progress: Double by useState(0.0)

    useEffect(windowSize, sideWindowRatio) {
        val svgContainer = svgContainerRef.current ?: return@useEffect
        svgSize = IntVector2(svgContainer.clientWidth, svgContainer.clientHeight)
    }

    useEffect(points, pointSize, svgSize) {
        if (fittingToScreen && points.isNotEmpty()) fitToScreen()
    }

    var showInfo: Boolean by useState(false)

    worker.onmessage = { m: MessageEvent ->
        val answer: Answer = Json.decodeFromString(m.data as String)
        when (answer) {
            is CompletedWork -> {
                progress = 0.0
                svg = answer.svg
                computing = false
                lastPoints = lastSentPoints
                lastGeneralSettings = lastSentGeneralSettings
                lastComputeDrawingSettings = lastSentDrawingSettings
                lastGrowSettings = lastSentGrowSettings
            }

            is Progress -> {
                progress = answer.progress
            }
        }

        Unit
    }

    fun recomputeSvg() {
        val assignment: Assignment = DrawSvg(drawSettings)
        worker.postMessage(Json.encodeToString(assignment))
    }

    useEffect(colors) {
        recomputeSvg()
    }

    useEffect(cover) {
        val assignment: Assignment = ChangeCover(cover)
        worker.postMessage(Json.encodeToString(assignment))
    }

    div {
        css {
            display = Display.flex
            flexDirection = FlexDirection.column
            alignItems = AlignItems.center
            width = 100.pct
            height = 100.pct
        }

        div {
            ref = sideWindowContainer
            css {
                background = NamedColor.white
                boxShadow = ("0px 6px 3px -3px rgba(0,0,0,0.2),0px 3px 3px 0px rgba(0,0,0,0.14)," +
                        "0px 3px 9px 0px rgba(0,0,0,0.12),0px -2px 3px -3px rgba(0,0,0,0.2)").unsafeCast<BoxShadow>()
                borderRadius = cardBr
                margin = Margin(10.px, 10.px)
                boxSizing = BoxSizing.borderBox
                display = Display.flex
                width = 100.pct - 20.px
                height = 100.pct - 20.px
                flexDirection = if (horizontal) FlexDirection.row else FlexDirection.column

            }

            SideWindow {
                isHorizontal = horizontal
                size = (100 * sideWindowRatio).pct

                onClickInfo = {
                    showInfo = true
                }

                PanelHeader {
                    title = "Input and output"
                }
                SelectExample {
                    onLoadExample = {
                        val ext = getExtension(it)
                        window
                            .fetch("example-input/${getFileName(it)}.${ext}")
                            .then { it.text() }
                            .then { text ->
                                fittingToScreen = true
                                val parsedPoints = when(ext) {
                                    "ipe" -> ipeToPoints(text)
                                    "txt" -> parsePoints(text)
                                    else -> error("Unknown extension: $ext")
                                }
                                val newPoints = parsedPoints.map { p ->
                                    p.copy(pos = p.pos.copy(y = svgSize.y - p.pos.y))
                                }
                                points = newPoints
                                matchText(newPoints)
                                val (gSettings, gCover) = goodSettings(it)
                                val (gs, tgs, _, ds) = gSettings
                                maxBendAngleSetter(gs.maxBendAngle)
                                maxTurningAngleSetter(gs.maxTurningAngle)
                                bendInflectionSetter(gs.bendInflection)
                                pointSizeSetter(gs.pSize)
                                forbidTooCloseSetter(tgs.forbidTooClose)
                                postponeIntersectionsSetter(tgs.postponeIntersections)
                                colorsSetter(ds.colors)
                                cover = gCover
                            }
                    }
                }
                textarea {
                    value = areaText
                    onChange = {
                        areaText = it.target.value
                        matchPoints(it.target.value)
                    }
                    css {
                        padding = 10.px
                        lineHeight = number(1.5);
                        borderRadius = 1.px;
                        border = Border(1.px, LineStyle.solid, Color("#ccc"))
                        boxShadow = BoxShadow(1.px, 1.px, 1.px, Color("#999"))
                        width = 250.px
                        height = 100.px
                    }
                }
                Divider()
                PanelHeader {
                    title = "General settings"
                }
                BendSettingsContext.Provider {
                    value = bendSettings
                    BankSettingsPanel {
                        ptSize = pointSize
                        ptStrokeWeight = drawSettings.pointStrokeWeight(generalSettings)
                        lineStrokeWeight = drawSettings.contourStrokeWeight(generalSettings)
                        expandRadius = generalSettings.expandRadius
                        color = currentColor.toHex()

                        PointSettingsContext.Provider {
                            value = pointSettings
                            PointSettingsPanel {
                                strokeWeight = drawSettings.pointStrokeWeight(generalSettings)
                                fillColor = currentColor.toSvgString()
                            }
                        }
                    }
                }

                Divider()
                PanelHeader {
                    title = "Grow settings"
                }
                GrowSettingsContext.Provider {
                    value = grow
                    GrowSettingsPanel()
                }
                Divider()
                PanelHeader {
                    title = "Colors"
                }
                ColorsContext.Provider {
                    value = colorsObj
                    ColorSettingsPanel()
                }
            }

            DraggableDivider {
                isHorizontal = horizontal
                windowContainer = sideWindowContainer
                valueSetter = setSideWindowRatio
            }

            div {
                css {
                    position = Position.relative
                    display = Display.flex
                    flexDirection = FlexDirection.column
                    boxSizing = BoxSizing.borderBox
//                    fontFamily = FontFamily.sansSerif
                    fontSize = (13 + 1.0 / 3.0).px
                    flex = auto
                    margin = 10.px
                    if (horizontal) {
                        marginLeft = 0.px
                        width = (100 * (1 - sideWindowRatio)).pct - 20.px
                    } else {
                        marginTop = 0.px
                        height = (100 * (1 - sideWindowRatio)).pct - 20.px
                    }
                    overflow = Overflow.hidden
                }

                tabIndex = 0

                if (computing) {
                    div {
                        css {
                            position = Position.absolute
                            width = 100.pct
                            height = 100.pct
                            background = rgb(255, 255, 255, 0.9)
                            display = Display.flex
                            alignItems = AlignItems.center
                            justifyContent = JustifyContent.center
                            zIndex = integer(10)
                        }
                        div {
                            css {
                                padding = 20.px
                                border = Border(1.px, LineStyle.solid, NamedColor.black)
                                background = NamedColor.white
                            }
                            label {
                                div {
                                    +"Computing..."
                                }
                                progress {
                                    value = progress
                                }
                            }
                        }
                    }
                }

                div {
                    css {
                        zIndex = integer(20)
                        margin = Margin(0.px, 10.px)
                    }
                    Toolbar {
                        val whiteSpace = div.create {
                            css {
                                width = 16.px
                                flex = initial
                                flexShrink = number(10000.0)
                            }
                        }

                        +whiteSpace

                        IntInput {
                            startValue = currentType
                            inputProps = jso {
                                type = InputType.number
                                min = 0
                                max = 11
                                css {
                                    width = 48.px
                                }
                            }
                            onParse = {
                                currentType = it
                            }
                        }

                        IconButton {
                            buttonProps = jso {
                                title = "Place points with mouse"
                                onClick = {
                                    tool = if (tool == Tool.PlacePoints) Tool.None else Tool.PlacePoints
                                }
                            }
                            isPressed = tool == Tool.PlacePoints
                            +"+"
                        }

                        IconButton {
                            buttonProps = jso {
                                title = "Remove points with mouse"
                                onClick = {
                                    tool = if (tool == Tool.RemovePoints) Tool.None else Tool.RemovePoints
                                }
                            }
                            isPressed = tool == Tool.RemovePoints
                            +"×"
                        }

                        IconButton {
                            buttonProps = jso {
                                title = "Move points with mouse"
                                onClick = {
                                    tool = if (tool == Tool.MovePoints) Tool.None else Tool.MovePoints
                                }
                            }
                            isPressed = tool == Tool.MovePoints
                            +"→"
                        }

//                        Grid {
//                            showGrid = useGrid
//                            onClick = {
//                                useGrid = !useGrid
//                            }
//                        }

                        +whiteSpace

                        IconButton {
                            buttonProps = jso {
                                title = "Clear"
//                        ariaLabel = "clear"
                                onClick =
                                {
                                    svg = emptySvg
                                    points = emptyList()
                                    matchText(emptyList())
                                }
                            }
                            Clear()
//                            +"C"
                        }

                        IconButton {
                            buttonProps = jso {
                                title = "Run computations"
                                onClick = {
                                    if (changedProblem) {
                                        val assignment: Assignment = Compute(
                                            points, generalSettings, growSettings,
                                            computeDrawingSettings, drawSettings, cover
                                        )
                                        worker.postMessage(Json.encodeToString(assignment))
                                        computing = true
                                        lastSentPoints = points
                                        lastSentGeneralSettings = generalSettings
                                        lastSentDrawingSettings = computeDrawingSettings
                                        lastSentGrowSettings = growSettings
                                    }
                                }
                            }
                            highlight = changedProblem

                            Run()
                        }

                        +whiteSpace

                        IconButton {
                            buttonProps = jso {
                                title = "Fit drawing to screen"
                                onClick = {
                                    if (!fittingToScreen) {
                                        fittingToScreen = true
                                        fitToScreen()
                                    } else {
                                        fittingToScreen = false
                                    }
                                }
                            }
                            isPressed = fittingToScreen
                            Fit()
                        }

                        +whiteSpace

                        IconButton {
                            buttonProps = jso {
                                title = "Download output as an SVG file"
                                onClick = {
                                    // Adapted from: https://stackoverflow.com/a/38019175
                                    val downloadee: BlobPart =
                                        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                                "<svg version=\"1.2\" baseProfile=\"tiny\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"$viewBoxTransform\">" +
                                                svg +
                                                "</svg>"
                                    val svgBlob =
                                        Blob(arrayOf(downloadee), jso { type = "image/svg+xml;charset=utf-8" })
                                    val svgUrl = URL.createObjectURL(svgBlob)
                                    val downloadLink = document.createElement("a").unsafeCast<HTMLAnchorElement>()
                                    downloadLink.href = svgUrl
                                    downloadLink.download = "output.svg"
//                            downloadLink.style = jso { display = "none" }
                                    document.body.appendChild(downloadLink)
                                    downloadLink.click()
                                    document.body.removeChild(downloadLink)
                                }
                            }
                            DownloadSvg()
//                            +"D"
                        }

                        div {
                            css {
                                flex = auto
                            }
                        }
                    }
                }

                div {
                    css {
                        height = 100.pct
                        width = 100.pct
                        borderBottomLeftRadius = if (horizontal) 0.px else 20.px
                        borderBottomRightRadius = 20.px
                        border = Border(1.px, LineStyle.solid, rgb(200, 200, 200))
                        position = Position.relative
                        overflow = Overflow.hidden
                        boxSizing = BoxSizing.borderBox

                        if (tool == Tool.PlacePoints) {
                            cursor = Cursor.pointer
                        }
                        if (evCache.isNotEmpty() &&
                            (tool == Tool.None ||
                            downEvent?.button != MouseButton.MAIN)
                            ) {
                            cursor = Cursor.move
                        }
                        if (tool == Tool.MovePoints && downEvent?.button == MouseButton.MAIN && movingPoint != null) {
                            cursor = Cursor.grabbing
                        }
                    }
                    div {
                        css {
                            height = 100.pct
                            width = 100.pct
                            position = Position.relative
                            overscrollBehavior = OverscrollBehavior.contain
                            touchAction = none
                        }

                        ref = svgContainerRef

                        onContextMenu = { ev ->
                            ev.preventDefault()
                        }

                        onPointerDown = { ev ->
                            ev.preventDefault()
                            ev.stopPropagation()
                            ev.currentTarget.setPointerCapture(ev.pointerId)

                            if (tool == Tool.PlacePoints && ev.button == MouseButton.MAIN) {
                                val newPoints = points + Point(viewMatrix * ev.offset, currentType)
                                points = newPoints
                                matchText(newPoints)
                            } else {
                                evCache += ev
                                downEvent = ev
                            }
                        }

                        onPointerUp = { ev ->
                            ev.preventDefault()
                            ev.stopPropagation()
                            evCache = evCache.filterNot {
                                it.pointerId == ev.pointerId
                            }
                            if (movingPoint != null) {
                                movingPoint = null
                            }
                        }

                        onPointerMove = { ev ->
                            ev.preventDefault()
                            ev.stopPropagation()
                            val prevEv = evCache.find {
                                it.pointerId == ev.pointerId
                            }

                            if (evCache.size == 2) {
                                val other = (evCache - prevEv).first()!!
                                val curDiff = ev.offset.distanceTo(other.offset)
                                if (prevDiff != null) {
                                    val diff = curDiff - prevDiff!!
                                    val middle = (ev.offset + other.offset) * 0.5
                                    // Pinched diff amount
                                    viewMatrix *= transform {
                                        translate(middle)
                                        scale(1 + diff / 1000)
                                        translate(-middle)
                                    }
                                }
                                prevDiff = curDiff
                            }

                            if (evCache.size == 1 && prevEv != null) {
                                if (tool == Tool.None || downEvent?.button != MouseButton.MAIN) {
                                    viewMatrix *= transform {
                                        translate(-(ev.clientX - prevEv.clientX), -(ev.clientY - prevEv.clientY))
                                    }
                                    fittingToScreen = false
                                } else if (tool == Tool.MovePoints && movingPoint != null){
                                    val newPoints = points.replace(movingPoint!!) { it.copy(pos = (viewMatrix * ev.offset).roundToDecimals(3)) }
                                    points = newPoints
                                    matchText(newPoints)
                                }
                            }

                            if (prevEv != null) {
                                evCache = evCache - prevEv + ev
                            }
                        }

                        onWheel = { ev ->
                            ev.preventDefault()
                            val pos = Vector2(ev.nativeEvent.offsetX, ev.nativeEvent.offsetY)
                            viewMatrix *= transform {
                                translate(pos)
                                scale(1 + ev.deltaY / 1000)
                                translate(-pos)
                            }
                            fittingToScreen = false
                        }

                        div {
                            css {
                                height = 100.pct
                                width = 100.pct
                                position = Position.absolute
                                zIndex = integer(2)
                                borderBottomRightRadius = cardBr
                                if (changedProblem || tool == Tool.MovePoints || tool == Tool.RemovePoints)
                                    background = rgb(255, 255, 255, 0.9)
                            }
                        }

                        svg {
                            css {
                                height = 100.pct
                                width = 100.pct
                                position = Position.absolute
                                display = if (changedProblem || tool == Tool.MovePoints || tool == Tool.RemovePoints) Display.block else none
                                zIndex = integer(3)
                                borderBottomRightRadius = cardBr
                            }

                            viewBox = viewBoxTransform

                            for ((i, p) in points.withIndex()) {
                                circle {
                                    cx = p.pos.x
                                    cy = p.pos.y
                                    r = pointSize
                                    fill = colors.getOrElse(p.type) { ColorRGBa.WHITE }.toSvgString()
                                    stroke = "black"
                                    strokeWidth = drawSettings.pointStrokeWeight(generalSettings)
                                    onPointerDown = { e ->
                                        if (tool == Tool.MovePoints)
                                            movingPoint = i
                                        if (tool == Tool.RemovePoints) {
                                            val newPoints = points.remove(i)
                                            points = newPoints
                                            matchText(newPoints)
                                        }
                                    }
                                    if (tool == Tool.RemovePoints)
                                        cursor = Cursor.pointer.toString()
                                    if (tool == Tool.MovePoints)
                                        cursor = Cursor.grab.toString()
                                }
                            }
                        }

                        div {
                            css {
                                height = 100.pct
                                width = 100.pct
                                position = Position.absolute
                            }

                            svg {
                                css {
                                    height = 100.pct
                                    width = 100.pct
                                    borderBottomRightRadius = cardBr
                                }

                                viewBox = viewBoxTransform

                                dangerouslySetInnerHTML = jso {
                                    __html = svg
                                }
                            }
                        }
                    }

                    if (viewMatrix != Matrix44.IDENTITY) {
                        div {
                            css {
                                position = Position.absolute
                                zIndex = integer(100)
                                padding = 10.px
                                bottom = 0.px
                                right = 0.px
                                userSelect = none
                                cursor = Cursor.pointer
                                background = rgb(253, 253, 253, 0.975)
                                borderRadius = 10.px
                                fontSize = 1.rem
                                margin = 10.px
                                border = Border(1.px, LineStyle.solid, rgb(200, 200, 200))
                            }
                            onClick = {
                                viewMatrix = Matrix44.IDENTITY
                                fittingToScreen = false
                            }
                            +"Reset"
                        }
                    }

                    div {
                        css {
                            position = Position.absolute
                            zIndex = integer(100)
                            padding = 10.px
                            top = 0.px
                            left = 0.px
                            userSelect = none
                            cursor = Cursor.pointer
                            background = rgb(253, 253, 253, 0.975)
                            borderRadius = 10.px
                            fontSize = 1.rem
                            margin = 10.px
                            border = Border(1.px, LineStyle.solid, rgb(200, 200, 200))
                        }

                        ThrottledSlider {
                            label = "Cover"
                            title = "Cover"
                            unit = ""
//                            step = "any".unsafeCast<Double>()
                            step = 0.1
                            min = 1.0
                            max = 8.0
                            defaultValue = cover
                            labelValue = cover
                            onThrottledChange = { v ->
                                cover = v
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInfo) {
        InfoScreen {
            closeScreen = { showInfo = false }
        }
    }
}

private fun Vector2.roundToDecimals(decimals: Int): Vector2 = x.roundToDecimals(decimals) v y.roundToDecimals(decimals)

val <T: Element, E : NativeMouseEvent> MouseEvent<T, E>.offset: Vector2
    get() = Vector2(nativeEvent.offsetX, nativeEvent.offsetY)
