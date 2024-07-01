package contexts

import react.createContext

interface PointSettings {
    var pointSize: Double
}

val PointSettingsContext = createContext<PointSettings>()