import org.openrndr.math.Vector2
import org.w3c.dom.Document
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import patterns.Point
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.text.DecimalFormat
import javax.xml.parsers.DocumentBuilderFactory


/**
 * This class helps creating ipe-files in Java by providing functions for most
 * common objects. Currently supports: Mark, Rectangle, Path, Edge, Text
 * label, Circle, Circular Arc, Spline, Splinegon
 *
 * Usage: The functions create a UML-String that corresponds to the specified
 * object in Ipe. Every file has to start with getIpePreamble(), followed by
 * getIpeConf(), and has to end with getIpeEnd().
 *
 * @author Martin Fink
 * @author Philipp Kindermann
 *
 * source: https://github.com/otfried/ipe-wiki/blob/master/ipedraw/io.IpeDraw.java
 * Adapted by Steven van den Broek
 */
object IpeDraw {
    /**
     * Draws a mark.
     *
     * @param x
     * x-coordinate
     * @param y
     * y-coordinate
     * @param shape
     * shape: disk, fdisk, circle, box, square, fsquare, cross
     * @param stroke
     * color of the boundary
     * @param fill
     * color of the interior
     * @param size
     * size: tiny, small, normal, large
     * @return
     */
    @JvmOverloads
    fun drawIpeMark(x: Double, y: Double, shape: String = "disk", stroke: String? = "black",
                    fill: String? = null, size: String = "normal"): String {
        return """<use name="mark/$shape(${if (shape.first() == 'f') "sfx" else "sx"})" pos="$x $y" size="$size"""" +
                (stroke?.let { """ stroke="$it"""" } ?: "") + (fill?.let { """ fill="$it"""" } ?: "") + "/>"
    }
    /**
     * Draws a rectangle.
     *
     * @param x1
     * left-most x-coordinate
     * @param y1
     * bottom-most y-coordinate
     * @param x2
     * right-most x-coordinate
     * @param y2
     * top-most y-coordinate
     * @param color
     * color
     * @param pen
     * pen width: normal, heavier, fat, ultrafat
     * @param dash
     * dash style: normal, dashed, dotted, dash dotted, dash dot dotted
     * @return
     */
    @JvmOverloads
    fun drawIpeBox(
        x1: Double, y1: Double, x2: Double, y2: Double,
        color: String = "black", pen: String = "normal", dash: String = "normal"
    ): String {
        return """<path stroke="$color" pen="$pen" dash="$dash">
 $x1 $y2 m
 $x1 $y1 l
 $x2 $y1 l
 $x2 $y2 l
 h
</path>
"""
    }
    /**
     * Draws a path between points.
     *
     * @param x
     * x-coordinates of the points
     * @param y
     * y-coordinates of the points
     * @param color
     * color
     * @param pen
     * pen width: normal, heavier, fat, ultrafat
     * @param dash
     * dash style: normal, dashed, dotted, dash dotted, dash dot dotted
     * @return
     */
    @JvmOverloads
    fun drawIpePath(
        x: List<Double>, y: List<Double>, color: String = "black",
        pen: String = "normal", dash: String = "normal"
    ): String {
        var s = """<path stroke="$color" pen="$pen" dash="$dash">
 ${x[0]} ${y[0]} m
 """
        for (i in 1 until x.size) {
            s += """${x[i]} ${y[i]} l
 """
        }
        s += "</path>\n"
        return s
    }

    /**
     * Draws a polygon.
     *
     * @param x
     * x-coordinates of the points
     * @param y
     * y-coordinates of the points
     * @param color
     * color
     * @param pen
     * pen width: normal, heavier, fat, ultrafat
     * @param dash
     * dash style: normal, dashed, dotted, dash dotted, dash dot dotted
     * @return
     */
    @JvmOverloads
    fun drawIpePolygon(
        x: List<Double>, y: List<Double>, stroke: String? = "black", fill: String? = null,
        pen: String = "normal", dash: String = "normal", opacity: String = "opaque", strokeOpacity: String = "opaque"
    ): String {
        var s = """<path pen="$pen" dash="$dash" opacity="$opacity" stroke-opacity="$strokeOpacity"""" + (stroke?.let { """ stroke="$it"""" } ?: "") +
                (fill?.let { """ fill="$it"""" } ?: "") + ">" + "\n  ${x[0]} ${y[0]} m\n"
        for (i in 1 until x.size) {
            s += "${x[i]} ${y[i]} l\n"
        }
        s += "h\n</path>\n"
        return s
    }

    /**
     * Draws an edge between two points.
     *
     * @param x1
     * x-coordinate of point 1
     * @param y1
     * y-coordinate of point 1
     * @param x2
     * x-coordinate of point 2
     * @param y2
     * y-coordinate of point 2
     * @param color
     * color
     * @param pen
     * pen width: normal, heavier, fat, ultrafat
     * @param dash
     * dash style: normal, dashed, dotted, dash dotted, dash dot dotted
     * @return
     */
    @JvmOverloads
    fun drawIpeEdge(
        x1: Double, y1: Double, x2: Double, y2: Double,
        color: String = "black", pen: String = "normal", dash: String = "normal"
    ): String {
        return drawIpePath(
            listOf(x1, x2), listOf(y1, y2), color,
            pen, dash
        )
    }

    /**
     * Places a text label at a specific point.
     *
     * @param text
     * The text
     * @param x
     * x-coordinate of the box
     * @param y
     * y-coordinate of the box
     * @param color
     * text-color
     * @param size
     * text-size
     * @return
     */
    @JvmOverloads
    fun writeIpeText(
        text: String, x: Double, y: Double, color: String = "black",
        size: String = "normal"
    ): String {
        return ("<text transformations=\"translations\" pos=\""
                + x
                + " "
                + y
                + "\" stroke=\""
                + color
                + "\" type=\"label\" width=\"190\" height=\"10\" depth=\"0\" valign=\"baseline\" size=\""
                + size + "\">" + text + "</text>")
    }
    /**
     * Draws a circle.
     *
     * @param x
     * x-coordinate of the center
     * @param y
     * y-coordinate of the center
     * @param radius
     * radius
     * @param stroke
     * color of the boundary
     * @param fill
     * color of the interior
     * @param pen
     * pen width: normal, heavier, fat, ultrafat
     * @param dash
     * dash style: normal, dashed, dotted, dash dotted, dash dot
     * dotted
     * @return
     */
    @JvmOverloads
    fun drawIpeCircle(
        x: Double, y: Double, radius: Double, stroke: String? = "black", fill: String? = null,
        pen: String = "normal", dash: String = "normal", opacity: String = "opaque", strokeOpacity: String = "opaque"
    ): String {
        val sf = DecimalFormat("####.000").format(radius)
        return "<path" + (stroke?.let { """ stroke="$it"""" } ?: "") + (fill?.let { """ fill="$it"""" } ?: "") +
                """ pen="$pen" dash="$dash" opacity="$opacity" stroke-opacity="$strokeOpacity">
 $sf 0 0 $sf $x $y e
</path>
"""
    }
    /**
     * Draws a circular arc in a mathematical positive sense.
     *
     * @param xCenter
     * x-coordinate of the center
     * @param yCenter
     * y-coordinate of the center
     * @param xStart
     * x-coordinate of the starting point on the circle
     * @param yStart
     * y-coordinate of the starting point on the circle
     * @param xEnd
     * x-coordinate of the end point on the circle
     * @param yEnd
     * y-coordinate of the end point on the circle
     * @param color
     * color
     * @param pen
     * pen width: normal, heavier, fat, ultrafat
     * @param dash
     * dash style: normal, dashed, dotted, dash dotted, dash dot
     * dotted
     * @return
     */
    @JvmOverloads
    fun drawIpeCircularArc(
        xCenter: Double, yCenter: Double,
        xStart: Double, yStart: Double, xEnd: Double, yEnd: Double, color: String =
            "black",
        pen: String = "normal", dash: String = "normal"
    ): String {
        val radius = Math.sqrt(
            Math.pow((xStart - xCenter), 2.0)
                    + Math.pow((yStart - yCenter), 2.0)
        )
        val sf = DecimalFormat("####.000").format(radius)
        return """<path stroke="$color" pen="$pen" dash="$dash">
 $xStart $yStart m
 $sf 0 0 $sf $xCenter $yCenter $xEnd $yEnd a
</path>
"""
    }

    @JvmOverloads
    fun drawIpeSemiCircle(xStart: Double, yStart: Double, xEnd: Double, yEnd: Double, color: String = "black"): String {
        val xCenter = (xStart + xEnd) / 2
        val yCenter = (yStart + yEnd) / 2
        return drawIpeCircularArc(
            xCenter, yCenter, xStart, yStart, xEnd, yEnd,
            color, "normal", "normal"
        )
    }

    /**
     * Draws a spline.
     *
     * @param x
     * x-coordinates of the control points.
     * @param y
     * y-coordinates of the control points.
     * @param color
     * color
     * @param pen
     * pen width: normal, heavier, fat, ultrafat
     * @param dash
     * dash style: normal, dashed, dotted, dash dotted, dash dot dotted
     * @return
     */
    @JvmOverloads
    fun drawIpeSpline(
        x: List<Double>, y: List<Double>, color: String = "black",
        pen: String = "black", dash: String = "black"
    ): String {
        var s = """<path stroke="$color" pen="$pen" dash="$dash">
 ${x[0]} ${y[0]} m"""
        for (i in 1 until x.size) {
            s += "\n ${x[i]} ${y[i]}"
        }
        s += " s\n</path>\n"
        return s
    }

    /**
     * Draws a splinegon.
     *
     * @param x
     * x-coordinates of the control points.
     * @param y
     * y-coordinates of the control points.
     * @param color
     * color
     * @param pen
     * pen width: normal, heavier, fat, ultrafat
     * @param dash
     * dash style: normal, dashed, dotted, dash dotted, dash dot dotted
     * @return
     */
    @JvmOverloads
    fun drawIpeSplinegon(
        x: List<Double>, y: List<Double>, color: String = "black",
        pen: String = "normal", dash: String = "normal"
    ): String {
        var s = """<path stroke="$color" pen="$pen" dash="$dash">
 ${x[0]} ${y[0]}"""
        for (i in 1 until x.size) {
            s += "\n ${x[i]} ${y[i]}"
        }
        s += " u\n</path>\n"
        return s
    }

    /**
     * Creates a new page.
     *
     * @return
     */
    fun newPage(): String {
        return "</page>\n<page>\n<layer name=\"alpha\"/>\n<view layers=\"alpha\" active=\"alpha\"/>\n"
    }

    /**
     * Closes the file.
     *
     * @return
     */
    const val ipeEnd: String
        = "</page>\n</ipe>\n"

    /**
     * The mandatory preamble for an ipe-file.
     *
     * @return
     */
    const val ipePreamble: String
        = "<?xml version=\"1.0\"?>\n    <!DOCTYPE ipe SYSTEM \"ipe.dtd\">\n    <ipe version=\"70005\" creator=\"Ipe 7.1.4\">\n    <info created=\"D:20131106154934\" modified=\"D:20131106160041\"/>\n    <preamble>\\usepackage[english]{babel}</preamble>\n"

    /**
     * Configuration of the standard objects in ipe.
     *
     * @return
     */
    const val ipeConf: String = """
    <ipestyle name="basic">
    <symbol name="arrow/arc(spx)">
    <path stroke="sym-stroke" fill="sym-stroke" pen="sym-pen">
    0 0 m
    -1 0.333 l
    -1 -0.333 l
    h
    </path>
    </symbol>
    <symbol name="arrow/farc(spx)">
    <path stroke="sym-stroke" fill="white" pen="sym-pen">
    0 0 m
    -1 0.333 l
    -1 -0.333 l
    h
    </path>
    </symbol>
    <symbol name="mark/circle(sx)" transformations="translations">
    <path fill="sym-stroke">
    0.6 0 0 0.6 0 0 e
    0.4 0 0 0.4 0 0 e
    </path>
    </symbol>
    <symbol name="mark/disk(sx)" transformations="translations">
    <path fill="sym-stroke">
    0.6 0 0 0.6 0 0 e
    </path>
    </symbol>
    <symbol name="mark/fdisk(sfx)" transformations="translations">
    <group>
    <path fill="sym-fill">
    0.5 0 0 0.5 0 0 e
    </path>
    <path fill="sym-stroke" fillrule="eofill">
    0.6 0 0 0.6 0 0 e
    0.4 0 0 0.4 0 0 e
    </path>
    </group>
    </symbol>
    <symbol name="mark/box(sx)" transformations="translations">
    <path fill="sym-stroke" fillrule="eofill">
    -0.6 -0.6 m
    0.6 -0.6 l
    0.6 0.6 l
    -0.6 0.6 l
    h
    -0.4 -0.4 m
    0.4 -0.4 l
    0.4 0.4 l
    -0.4 0.4 l
    h
    </path>
    </symbol>
    <symbol name="mark/square(sx)" transformations="translations">
    <path fill="sym-stroke">
    -0.6 -0.6 m
    0.6 -0.6 l
    0.6 0.6 l
    -0.6 0.6 l
    h
    </path>
    </symbol>
    <symbol name="mark/fsquare(sfx)" transformations="translations">
    <group>
    <path fill="sym-fill">
    -0.5 -0.5 m
    0.5 -0.5 l
    0.5 0.5 l
    -0.5 0.5 l
    h
    </path>
    <path fill="sym-stroke" fillrule="eofill">
    -0.6 -0.6 m
    0.6 -0.6 l
    0.6 0.6 l
    -0.6 0.6 l
    h
    -0.4 -0.4 m
    0.4 -0.4 l
    0.4 0.4 l
    -0.4 0.4 l
    h
    </path>
    </group>
    </symbol>
    <symbol name="mark/cross(sx)" transformations="translations">
    <group>
    <path fill="sym-stroke">
    -0.43 -0.57 m
    0.57 0.43 l
    0.43 0.57 l
    -0.57 -0.43 l
    h
    </path>
    <path fill="sym-stroke">
    -0.43 0.57 m
    0.57 -0.43 l
    0.43 -0.57 l
    -0.57 0.43 l
    h
    </path>
    </group>
    </symbol>
    <symbol name="arrow/fnormal(spx)">
    <path stroke="sym-stroke" fill="white" pen="sym-pen">
    0 0 m
    -1 0.333 l
    -1 -0.333 l
    h
    </path>
    </symbol>
    <symbol name="arrow/pointed(spx)">
    <path stroke="sym-stroke" fill="sym-stroke" pen="sym-pen">
    0 0 m
    -1 0.333 l
    -0.8 0 l
    -1 -0.333 l
    h
    </path>
    </symbol>
    <symbol name="arrow/fpointed(spx)">
    <path stroke="sym-stroke" fill="white" pen="sym-pen">
    0 0 m
    -1 0.333 l
    -0.8 0 l
    -1 -0.333 l
    h
    </path>
    </symbol>
    <symbol name="arrow/linear(spx)">
    <path stroke="sym-stroke" pen="sym-pen">
    -1 0.333 m
    0 0 l
    -1 -0.333 l
    </path>
    </symbol>
    <symbol name="arrow/fdouble(spx)">
    <path stroke="sym-stroke" fill="white" pen="sym-pen">
    0 0 m
    -1 0.333 l
    -1 -0.333 l
    h
    -1 0 m
    -2 0.333 l
    -2 -0.333 l
    h
    </path>
    </symbol>
    <symbol name="arrow/double(spx)">
    <path stroke="sym-stroke" fill="sym-stroke" pen="sym-pen">
    0 0 m
    -1 0.333 l
    -1 -0.333 l
    h
    -1 0 m
    -2 0.333 l
    -2 -0.333 l
    h
    </path>
    </symbol>
    <pen name="heavier" value="0.8"/>
    <pen name="fat" value="1.2"/>
    <pen name="ultrafat" value="2"/>
    <symbolsize name="large" value="5"/>
    <symbolsize name="small" value="2"/>
    <symbolsize name="tiny" value="1.1"/>
    <arrowsize name="large" value="10"/>
    <arrowsize name="small" value="5"/>
    <arrowsize name="tiny" value="3"/>
    <color name="red" value="1 0 0"/>
    <color name="green" value="0 1 0"/>
    <color name="blue" value="0 0 1"/>
    <color name="yellow" value="1 1 0"/>
    <color name="orange" value="1 0.647 0"/>
    <color name="gold" value="1 0.843 0"/>
    <color name="purple" value="0.627 0.125 0.941"/>
    <color name="gray" value="0.745"/>
    <color name="brown" value="0.647 0.165 0.165"/>
    <color name="navy" value="0 0 0.502"/>
    <color name="pink" value="1 0.753 0.796"/>
    <color name="seagreen" value="0.18 0.545 0.341"/>
    <color name="turquoise" value="0.251 0.878 0.816"/>
    <color name="violet" value="0.933 0.51 0.933"/>
    <color name="darkblue" value="0 0 0.545"/>
    <color name="darkcyan" value="0 0.545 0.545"/>
    <color name="darkgray" value="0.663"/>
    <color name="darkgreen" value="0 0.392 0"/>
    <color name="darkmagenta" value="0.545 0 0.545"/>
    <color name="darkorange" value="1 0.549 0"/>
    <color name="darkred" value="0.545 0 0"/>
    <color name="lightblue" value="0.678 0.847 0.902"/>
    <color name="lightcyan" value="0.878 1 1"/>
    <color name="lightgray" value="0.827"/>
    <color name="lightgreen" value="0.565 0.933 0.565"/>
    <color name="lightyellow" value="1 1 0.878"/>
    <dashstyle name="dashed" value="[4] 0"/>
    <dashstyle name="dotted" value="[1 3] 0"/>
    <dashstyle name="dash dotted" value="[4 2 1 2] 0"/>
    <dashstyle name="dash dot dotted" value="[4 2 1 2 1 2] 0"/>
    <textsize name="large" value="\large"/>
    <textsize name="Large" value="\Large"/>
    <textsize name="LARGE" value="\LARGE"/>
    <textsize name="huge" value="\huge"/>
    <textsize name="Huge" value="\Huge"/>
    <textsize name="small" value="\small"/>
    <textsize name="footnote" value="\u000cootnotesize"/>
    <textsize name="tiny" value="\tiny"/>
    <textstyle name="center" begin="\begin{center}" end="\end{center}"/>
    <textstyle name="itemize" begin="\begin{itemize}" end="\end{itemize}"/>
    <textstyle name="item" begin="\begin{itemize}\item{}" end="\end{itemize}"/>
    <gridsize name="4 pts" value="4"/>
    <gridsize name="8 pts (~3 mm)" value="8"/>
    <gridsize name="16 pts (~6 mm)" value="16"/>
    <gridsize name="32 pts (~12 mm)" value="32"/>
    <gridsize name="10 pts (~3.5 mm)" value="10"/>
    <gridsize name="20 pts (~7 mm)" value="20"/>
    <gridsize name="14 pts (~5 mm)" value="14"/>
    <gridsize name="28 pts (~10 mm)" value="28"/>
    <gridsize name="56 pts (~20 mm)" value="56"/>
    <anglesize name="90 deg" value="90"/>
    <anglesize name="60 deg" value="60"/>
    <anglesize name="45 deg" value="45"/>
    <anglesize name="30 deg" value="30"/>
    <anglesize name="22.5 deg" value="22.5"/>
    <tiling name="falling" angle="-60" step="4" width="1"/>
    <tiling name="rising" angle="30" step="4" width="1"/>
    <layout paper="1000 1000" origin="0 0" frame="1000 1000" skip="32" crop="yes"/>
    </ipestyle>        
    """

    const val ipeStart: String = """
    <page>
    <layer name="alpha"/>
    <view layers="alpha" active="alpha"/>
    """

    const val ipeExtendedColors: String = """
    <ipestyle name="extendedcolors">
    <color name="CART 1" value="0.145 0.737 0.612"/>
    <color name="CART 10" value="0.996 0.965 0.608"/>
    <color name="CART 11" value="0.996 0.859 0.706"/>
    <color name="CART 12" value="0.98 0.714 0.58"/>
    <color name="CART 13" value="1 0.8 0.302"/>
    <color name="CART 2" value="0.533 0.78 0.396"/>
    <color name="CART 3" value="0.561 0.737 0.757"/>
    <color name="CART 4" value="0.604 0.839 0.741"/>
    <color name="CART 5" value="0.706 0.592 0.506"/>
    <color name="CART 6" value="0.733 0.718 0.349"/>
    <color name="CART 7" value="0.831 0.878 0.353"/>
    <color name="CART 8" value="0.835 0.725 0.541"/>
    <color name="CART 9" value="0.867 0.529 0.475"/>
    <color name="CB brown" value="0.694 0.349 0.157"/>
    <color name="CB dark blue" value="0.121 0.47 0.705"/>
    <color name="CB dark green" value="0.2 0.627 0.172"/>
    <color name="CB dark orange" value="1 0.498 0"/>
    <color name="CB dark purple" value="0.415 0.239 0.603"/>
    <color name="CB dark red" value="0.89 0.102 0.109"/>
    <color name="CB light blue" value="0.651 0.807 0.89"/>
    <color name="CB light green" value="0.698 0.874 0.541"/>
    <color name="CB light orange" value="0.992 0.749 0.435"/>
    <color name="CB light purple" value="0.792 0.698 0.839"/>
    <color name="CB light red" value="0.984 0.603 0.6"/>
    <color name="CB yellow" value="1 1 0.6"/>
    <color name="Gray 0.0" value="0"/>
    <color name="Gray 0.1" value="0.1"/>
    <color name="Gray 0.2" value="0.2"/>
    <color name="Gray 0.3" value="0.3"/>
    <color name="Gray 0.4" value="0.4"/>
    <color name="Gray 0.5" value="0.5"/>
    <color name="Gray 0.6" value="0.6"/>
    <color name="Gray 0.7" value="0.7"/>
    <color name="Gray 0.8" value="0.8"/>
    <color name="Gray 0.9" value="0.9"/>
    <color name="Gray 1.0" value="1"/>
    <dashstyle name="W dashed fat" value="[3 5.1] 0"/>
    <dashstyle name="W dashed heavier" value="[2 3] 0"/>
    <dashstyle name="W dashed normal" value="[1 1.7] 0"/>
    <dashstyle name="W dashed ultrafat" value="[5 8.5] 0"/>
    <dashstyle name="W dot fat" value="[0.01 2.4] 0"/>
    <dashstyle name="W dot heavier" value="[0.01 1.6] 0"/>
    <dashstyle name="W dot normal" value="[0.01 0.8] 0"/>
    <dashstyle name="W dot ultrafat" value="[0.01 4] 0"/>
    </ipestyle>"""
}

class IpeDrawBuilder(private val colors: List<String>) {
    private val s = StringBuilder()

    init {
        s.append(IpeDraw.ipePreamble)
        s.append(IpeDraw.ipeConf)
        s.append(IpeDraw.ipeExtendedColors)
        s.append("""
    <ipestyle name="transparency">""")
        s.append("""
    <opacity name="island" value="0.3"/>""")
        s.append("""
    </ipestyle>""")
        s.append(IpeDraw.ipeStart)
    }

    fun point(p: Point) {
        val op = p.originalPoint ?: p
        s.append(
            IpeDraw.drawIpeMark(
                op.pos.x,
                op.pos.y,
                shape = "fdisk",
                stroke = "black",
                fill = colors[op.type],
                size = "large"
            )
        )
    }

    fun toIpeString(): String {
        s.append(IpeDraw.ipeEnd)
        return s.toString()
    }
}

fun ipeDraw(colors: List<String>, draw: IpeDrawBuilder.() -> Unit): String {
    val builder = IpeDrawBuilder(colors)
    builder.draw()
    return builder.toIpeString()
}

internal fun NodeList.asList(): List<Node> = List(length) { item(it) }
internal fun NamedNodeMap.asMap(): Map<String, String> = buildMap(length) {
    for (i in 0 until length){
        val attr = item(i).toString().split('=')
        put(attr[0], attr[1].dropWhile { it == '"' }.dropLastWhile { it == '"' })
    }
}

fun ipeToPoints(badIpeXML: String): List<Point> {
    val ipeXML = badIpeXML.lines().filterNot { it.contains("""<!DOCTYPE ipe SYSTEM "ipe.dtd">""") }.joinToString("\n")
    val doc = loadXMLFromString(ipeXML)
    val nodeList = doc.getElementsByTagName("use").asList().map { it.attributes.asMap() }

    return nodesToPoints(nodeList)
}

@Throws(Exception::class)
fun loadXMLFromString(xml: String): Document {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val inputSource = InputSource(StringReader(xml))
    return builder.parse(inputSource)
}

fun writeToIpe(points: List<Point>, fileName: String) {
    val colors = listOf("CB light blue", "CB light red", "CB light green", "CB light orange", "CB light purple")

    val file = File(fileName)

    try {
        val s = ipeDraw(colors) {
            for (p in points){
                point(p)
            }
        }
        file.writeText(s)
    } catch (e: IOException) {
        println("Could not write to output file!")
        e.printStackTrace()
    }
}
