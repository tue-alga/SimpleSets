package components

import web.cssom.px
import emotion.react.css
import react.FC
import react.dom.html.ButtonHTMLAttributes
import react.dom.html.ReactHTML
import web.html.HTMLButtonElement

external interface ButtonProps : ButtonHTMLAttributes<HTMLButtonElement>

val Button = FC<ButtonProps> { props ->
    ReactHTML.button {
        css {
            padding = 4.px
        }

        +props
    }
}
