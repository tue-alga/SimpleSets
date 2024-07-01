package sideWindow.settings

import components.Checkbox
import components.Slider
import contexts.GrowSettingsContext
import react.FC
import react.Props
import react.useContext

val GrowSettingsPanel = FC<Props> {
    with(useContext(GrowSettingsContext)!!) {
        Slider {
            label = "Avoid"
            title = "Avoid"
            unit = ""
            step = 0.01
            min = 0.0
            max = 1.0
            value = forbidTooClose
            onChange = {
                forbidTooClose = it.currentTarget.valueAsNumber
            }
        }
        Checkbox {
            title = "Delay merges that introduce intersections"
            checked = postponeIntersections
            label = "Intersection delay (slow!)"
            onChange = {
                postponeIntersections = !postponeIntersections
            }
        }
    }
}