package sideWindow

import emotion.react.css
import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span
import web.cssom.*

external interface SideWindowProps: PropsWithChildren {
    var isHorizontal: Boolean
    var size: Length
    var onClickInfo: () -> Unit
}

val SideWindow = FC<SideWindowProps> { props ->
    div {
        css {
            overflow = Overflow.hidden
            if (props.isHorizontal) {
                height = 100.pct
                width = props.size
            } else {
                height = props.size
                width = 100.pct
            }
        }

        div {
            css {
                width = 100.pct
                height = 100.pct
                display = Display.flex
                flexDirection = FlexDirection.column
                boxSizing = BoxSizing.borderBox
                padding = Padding(20.px, 30.px)
                minWidth = 250.px
                if (props.isHorizontal) {
                    paddingRight = 5.px
                } else {
                    paddingBottom = 0.px
                }
            }
            ReactHTML.h1 {
                css {
                    marginTop = 0.px
                }
                span {
                    +"SimpleSets"
                }
                span {
                    css {
                        marginLeft = 5.px
                        color = NamedColor.gray
                        cursor = Cursor.pointer
                    }
                    onClick = {
                        props.onClickInfo()
                    }
                    +" ⓘ"
                }
            }
            div {
                css {
                    overflow = Auto.auto
                }
                +props.children
            }
        }
    }
}