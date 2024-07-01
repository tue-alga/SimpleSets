package components

import web.cssom.*
import web.cssom.None.Companion.none
import emotion.react.css
import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span

external interface IconNonButtonProps : PropsWithChildren

val IconNonButton = FC<IconNonButtonProps> { props ->
    div {
        css {
            width = 32.px
            height = 32.px
            border = none
            backgroundColor = NamedColor.white
        }

        span {
            css {
                alignItems = AlignItems.center
                justifyContent = JustifyContent.center
                display = Display.flex
                flexWrap = FlexWrap.nowrap
                height = 100.pct
            }
            +props.children
        }
    }
}