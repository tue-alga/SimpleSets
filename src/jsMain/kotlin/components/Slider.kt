package components

import patterns.roundToDecimals
import react.FC
import react.dom.html.InputHTMLAttributes
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.input
import react.useState
import web.html.HTMLInputElement
import web.html.InputType
import web.timers.Timeout
import web.timers.clearTimeout
import web.timers.setTimeout
import kotlin.time.Duration.Companion.milliseconds

external interface SliderProps: InputHTMLAttributes<HTMLInputElement> {
    var label: String
    var unit: String
}

val Slider = FC<SliderProps> { props ->
    div {
        label {
            val value = "${props.value.unsafeCast<Double>().roundToDecimals(1)}${props.unit}"
            div {
                +"${props.label}: $value"
            }
            input {
                +props
                type = InputType.range
            }
        }
    }
}

external interface ThrottledSliderProps: InputHTMLAttributes<HTMLInputElement> {
    var label: String
    var unit: String
    var onThrottledChange: (Double) -> Unit
    var labelValue: Double
}

val ThrottledSlider = FC<ThrottledSliderProps> { props ->
    var apiTimeout: Timeout? by useState(null)

    div {
        label {
            div {
                +"${props.label}: ${props.labelValue}"
            }
            input {
                +props
                type = InputType.range
                onChange = { ev ->
                    apiTimeout?.let { clearTimeout(it) }
                    val v = ev.currentTarget.valueAsNumber
                    apiTimeout = setTimeout(100.milliseconds) {
                        props.onThrottledChange(v)
                    }
                }
            }
        }
    }
}