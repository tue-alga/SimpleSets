package components

import web.cssom.*
import web.cssom.None.Companion.none
import emotion.react.css
import react.FC
import react.PropsWithChildren
import react.dom.html.ButtonHTMLAttributes
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.span
import web.html.HTMLButtonElement

external interface IconButtonProps : PropsWithChildren {
    var isPressed: Boolean
    var buttonProps: ButtonHTMLAttributes<HTMLButtonElement>
    var highlight: Boolean
}

val IconButton = FC<IconButtonProps> { props ->
    button {
        +props.buttonProps

        css {
            minWidth = 32.px
            width = 32.px
            height = 32.px
            border = none

            cursor = Cursor.pointer

            padding = 0.px

            backgroundPosition = GeometryPosition.center
            transition = Transition(PropertyName.background, duration=0.4.s, delay=0.s)
            if (props.isPressed) {
                backgroundColor = Color("#ECEBEB")
                hover {
                    transition = Transition(PropertyName.background, duration=0.2.s, delay=0.s)
                    backgroundColor = Color("#DFDEDE")
                }
            } else {
                backgroundColor = NamedColor.white
                hover {
                    transition = Transition(PropertyName.background, duration=0.2.s, delay=0.s)
                    backgroundColor = NamedColor.whitesmoke
                    backgroundImage = "radial-gradient(circle, transparent 1%, whitesmoke 1%)".unsafeCast<Gradient>()
                    backgroundSize = 15000.pct
                }
                active {
                    backgroundColor = Color("#DFDEDE")
                    backgroundSize = 100.pct
                    transition = Transition(PropertyName.background, duration=0.s, delay=0.s)
                }
            }

            if (props.highlight) {
                border = Border(1.px, LineStyle.solid, rgb(0, 96, 223))
                borderRadius = 2.px
            }
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