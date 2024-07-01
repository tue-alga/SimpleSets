import org.openrndr.math.IntVector2
import react.useEffect
import react.useState
import web.events.EventType
import web.events.addEventListener
import web.events.removeEventListener
import web.window.window

fun useWindowSize(): IntVector2 {
    var size: IntVector2 by useState(IntVector2(window.innerWidth, window.innerHeight))
    useEffect {
        val listener = { _: Any -> size = IntVector2(window.innerWidth, window.innerHeight) }
        window.addEventListener(EventType("resize"), listener)
        cleanup {
            window.removeEventListener(EventType("resize"), listener)
        }
    }
    return size
}
