import patterns.Point

fun getExampleInput(e: ExampleInput): List<Point> {
    val ext = getExtension(e)
    val path = "/example-input/${getFileName(e)}.${ext}"
    val text = getResourceAsText(path)
    if (ext == "ipe")
        return ipeToPoints(text)
    else if (ext == "txt") {
        return parsePoints(text)
    } else {
        error("Unknown extesnion")
    }
}

fun getResourceAsText(path: String): String =
    object {}.javaClass.getResource(path)?.readText() ?: error("Unknown resource: '$path'")
