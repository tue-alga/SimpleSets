package components

import web.cssom.Display
import emotion.react.css
import react.FC
import react.PropsWithChildren
import react.dom.html.HTMLAttributes
import react.dom.html.ReactHTML.div
import web.html.HTMLDivElement

external interface ToolbarProps: PropsWithChildren, HTMLAttributes<HTMLDivElement>

val Toolbar = FC<ToolbarProps> { props ->
    div {
        css {
            display = Display.flex
        }
        +props.children
    }
}