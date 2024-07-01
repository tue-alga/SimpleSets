package contexts

import react.createContext

interface BendSettings {
    var bendInflection: Boolean
    var maxBendAngle: Double
    var maxTurningAngle: Double
}

val BendSettingsContext = createContext<BendSettings>()