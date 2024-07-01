import components.IconButton
import js.objects.jso
import react.FC
import react.Props
import react.dom.events.MouseEventHandler
import react.dom.svg.ReactSVG.line
import react.dom.svg.ReactSVG.svg
import web.html.HTMLButtonElement

external interface GridProps: Props {
    var showGrid: Boolean
    var onClick: MouseEventHandler<HTMLButtonElement>?
}

val Grid = FC<GridProps> { props ->
    IconButton {
        buttonProps = jso {
            title = "Turn ${if (props.showGrid) "off" else "on"} grid"
            onClick = props.onClick
        }
        isPressed = props.showGrid
        svg {
            width = 16.0
            height = 16.0
            stroke = "black"
            strokeWidth = 1.0
            for (i in 3..12 step 3) {
                line {
                    x1 = i.toDouble() + 0.5
                    x2 = i.toDouble() + 0.5
                    y1 = 0.5
                    y2 = 15.5
                }
                line {
                    x1 = 0.5
                    x2 = 15.5
                    y1 = i.toDouble() + 0.5
                    y2 = i.toDouble() + 0.5
                }
            }
        }
    }
}