package sideWindow

import ExampleInput
import web.cssom.Display
import emotion.react.css
import react.FC
import react.Props
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.option
import react.useState
import web.cssom.px

external interface SelectExampleProps: Props {
    var onLoadExample: (ExampleInput) -> Unit
}

val SelectExample = FC<SelectExampleProps> { props ->
    var selectedExampleInput: ExampleInput by useState(ExampleInput.Mills)
    div {
        css {
            marginBottom = 10.px
        }
        label {
            title = "Select example input"
            ReactHTML.select {
                css {
                    display = Display.block
                    marginBottom = 2.px
                }
                value = selectedExampleInput.name
                ExampleInput.values().forEach {
                    option {
                        value = it.name
                        +it.name
                    }
                }
                onChange = {
                    selectedExampleInput = ExampleInput.valueOf(it.target.value)
                }
            }
        }
        button {
            css {
                display = Display.block
            }
            +"Load example"
            onClick = {
                props.onLoadExample(selectedExampleInput)
            }
        }
    }
}
