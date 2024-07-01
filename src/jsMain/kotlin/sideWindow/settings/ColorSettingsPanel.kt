package sideWindow.settings

import contexts.ColorsContext
import emotion.react.css
import org.openrndr.color.ColorRGBa
import react.FC
import react.Props
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.useContext
import react.useState
import toHex
import web.cssom.*
import web.html.InputType
import web.timers.Timeout
import web.timers.clearTimeout
import web.timers.setTimeout
import kotlin.time.Duration.Companion.milliseconds

val ColorSettingsPanel = FC<Props> {
    div {
        css {
            display = Display.flex
            flexDirection = FlexDirection.row
            alignItems = AlignItems.center
            columnGap = 8.px
            rowGap = 10.px
            flexWrap = FlexWrap.wrap
            maxWidth = 500.px
        }

        with(useContext(ColorsContext)!!) {
            for (i in colors.indices) {
                ColorPicker {
                    this.i = i
                }
//                if (i == colors.size / 2 - 1) {
//                    div {
//                        css {
//                            flexBasis = 100.pct
//                            height = 0.px
//                        }
//                    }
//                }
            }
        }
    }
}

external interface ColorPickersProps: Props {
    var i: Int
}

val ColorPicker = FC<ColorPickersProps> { props ->
    val i = props.i

    var apiTimeout: Timeout? by useState(null)

    with(useContext(ColorsContext)!!) {
        div {
            css {
                display = Display.flex
                flexDirection = FlexDirection.column
                alignItems = AlignItems.center
                rowGap = 5.px
            }
            ReactHTML.label {
                css {
                    display = Display.flex
                    flexDirection = FlexDirection.column
                    alignItems = AlignItems.center
                }
                ReactHTML.div {
                    css {
                        width = Length.maxContent
                        marginBottom = 4.px
                    }
                    +"$i"
                }
                ReactHTML.input {
                    type = InputType.color
                    value = colors[i].toHex()
                    onChange = { ev ->
                        apiTimeout?.let { clearTimeout(it) }

                        val v = ev.currentTarget.value
                        apiTimeout = setTimeout(100.milliseconds) {
                            colors = colors.replace(i) { ColorRGBa.fromHex(v) }
                        }
                    }
                }
            }

            ReactHTML.button {
                onClick = {
                    colors = colors.replace(i, defaultColors[i])
                }
                +"Reset"
            }
        }
    }
}

fun <E> List<E>.replace(i: Int, function: (E) -> E): List<E> =
    withIndex().map {
        if (it.index == i) function(it.value) else it.value
    }

fun <E> List<E>.replace(i: Int, new: E): List<E> =
    withIndex().map {
        if (it.index == i) new else it.value
    }

fun <E> List<E>.remove(i: Int): List<E> =
    withIndex().filter {
        it.index != i
    }.map { it.value }