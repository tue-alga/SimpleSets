import js.array.asList
import patterns.Point
import web.dom.NamedNodeMap
import web.dom.parsing.DOMParser
import web.dom.parsing.DOMParserSupportedType

fun ipeToPoints(ipe: String): List<Point> {
    val parser = DOMParser()
    val doc = parser.parseFromString(ipe, DOMParserSupportedType.applicationXml)
    val parseError = doc.querySelector("parsererror")
    if (parseError != null) {
        error(parseError)
    }
    val nodes = doc.getElementsByTagName("use").asList().map { it.attributes.asMap() }

    return nodesToPoints(nodes)
}

internal fun NamedNodeMap.asMap(): Map<String, String> = buildMap(length) {
    for (i in 0 until length){
        put(item(i)!!.name, item(i)!!.value)
    }
}