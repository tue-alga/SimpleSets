package components

import emotion.react.css
import react.FC
import react.Props
import react.dom.html.InputHTMLAttributes
import react.dom.html.ReactHTML.input
import react.useState
import web.cssom.px
import web.html.HTMLInputElement
import web.html.InputType

external interface IntInputProps: Props {
    var inputProps: InputHTMLAttributes<HTMLInputElement>
    var startValue: Int
    var onParse: (Int) -> Unit
}

val IntInput = FC<IntInputProps> { props ->
    var currentValue: String by useState(props.startValue.toString())
    input {
        css {
            width = 48.px
        }
        +props.inputProps
        value = currentValue
        type = InputType.number
        min = 0
        max = 11
        onChange = {
            currentValue = it.target.value
            try {
                val v = it.target.value.toInt()
                if (v >= min as Int && v <= max as Int)
                    props.onParse(v)
            } catch (_: Exception) {

            }
        }
    }
}