package contexts

import react.createContext

interface Grow {
    var postponeIntersections: Boolean
    var forbidTooClose: Double
}

val GrowSettingsContext = createContext<Grow>()