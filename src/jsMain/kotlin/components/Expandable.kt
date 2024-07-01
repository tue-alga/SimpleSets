package components

import web.cssom.*
import web.cssom.None.Companion.none
import emotion.react.css
import react.FC
import react.Props
import react.ReactNode
import react.dom.html.ReactHTML.div

external interface ExpandableProps: Props {
    var expander: ReactNode
    var expandee: ReactNode
}

val expandee = ClassName("expandee")

val Expandable = FC<ExpandableProps> { props ->
    div {
        css {
            position = Position.relative

            expandee {
                display = none
            }

            hover {
                expandee {
                    display = Display.block
                    position = Position.absolute
                    top = 100.pct
                    background = NamedColor.white
                    boxShadow = BoxShadow(0.px, 10.px, 10.px, 0.px, rgb(0, 0, 0, 0.25))
                    borderBottomLeftRadius = 10.px
                    borderBottomRightRadius = 10.px

                    zIndex = integer(10)
                }
            }
        }
        +props.expander
        div {
            className = expandee
            +props.expandee
        }
    }
}