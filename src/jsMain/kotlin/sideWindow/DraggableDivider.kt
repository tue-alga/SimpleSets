package sideWindow

import emotion.react.css
import offset
import react.*
import react.dom.html.ReactHTML
import web.cssom.*
import web.html.HTMLDivElement
import web.html.HTMLElement
import kotlin.math.max

external interface DividerProps: Props {
    var isHorizontal: Boolean
    var windowContainer: RefObject<HTMLDivElement>
    var valueSetter: StateSetter<Double>
}

val DraggableDivider = FC<DividerProps> { props ->
    val horizontal = props.isHorizontal
    var sideWindowMovingStart: Double? by useState(null)
    val sideWindowMoving: Boolean = sideWindowMovingStart != null

    ReactHTML.div {
        css {
            display = Display.flex
            alignItems = AlignItems.center
            justifyContent = JustifyContent.center
            if (horizontal) {
                width = 20.px
                height = 100.pct
            } else {
                height = 20.px
                width = 100.pct
            }
            overscrollBehavior = OverscrollBehavior.contain
            cursor = if (horizontal) Cursor.colResize else Cursor.rowResize
            flexShrink = number(0.0)
        }

        onPointerDown = { ev ->
            val target = ev.target as HTMLElement
            sideWindowMovingStart = if (horizontal) {
                val diff = target.getBoundingClientRect().left - ev.currentTarget.getBoundingClientRect().left
                ev.offset.x + diff
            } else {
                val diff = target.getBoundingClientRect().top - ev.currentTarget.getBoundingClientRect().top
                ev.offset.y + diff
            }
            ev.currentTarget.setPointerCapture(ev.pointerId)
        }

        onPointerMove = { ev ->
            if (sideWindowMoving) {
                props.windowContainer.current?.let { container ->
                    val v = if (horizontal) {
                        val left = container.getBoundingClientRect().left
                        (ev.clientX - sideWindowMovingStart!! - left) / container.clientWidth
                    } else {
                        val top = container.getBoundingClientRect().top
                        (ev.clientY - sideWindowMovingStart!! - top) / container.clientHeight
                    }
                    props.valueSetter(max(0.0, v))
                }
            }
        }

        onPointerUp = { ev ->
            sideWindowMovingStart = null
            ev.stopPropagation()
        }

        ReactHTML.div {
            css {
                if (horizontal) {
                    height = 66.pct
                } else {
                    width = 66.pct
                }
                border = Border(1.px, LineStyle.solid, rgb(200, 200, 200))

            }
        }
    }
}