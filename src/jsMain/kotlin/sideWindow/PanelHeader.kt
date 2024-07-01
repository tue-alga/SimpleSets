package sideWindow

import emotion.react.css
import react.FC
import react.Props
import react.dom.html.ReactHTML.h2
import web.cssom.px

external interface PanelHeaderProps: Props {
    var title: String
}

val PanelHeader = FC<PanelHeaderProps> { props ->
    h2 {
        css {
            marginTop = 0.px
        }
        +props.title
    }
}