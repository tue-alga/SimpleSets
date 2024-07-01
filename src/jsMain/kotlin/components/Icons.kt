package components

import web.cssom.px
import emotion.react.css
import js.objects.jso
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import require

fun createIcon(rawSvg: String) = FC<Props> {
    div {
        css {
            width = 24.px
            height = 24.px
        }
        dangerouslySetInnerHTML = jso {
            __html = rawSvg
        }
    }
}

//val Clear = createIcon(require("clear.svg"))
val Clear = createIcon("""
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<svg
   viewBox="0 0 24 24"
   version="1.1"
   id="svg5"
   width="24"
   height="24"
   xmlns="http://www.w3.org/2000/svg"
   xmlns:svg="http://www.w3.org/2000/svg">
  <defs
     id="defs2" />
  <path
     style="fill:none;stroke:#000000;stroke-width:1.1;stroke-linejoin:miter;stroke-dasharray:none"
     id="path24621"
     d="m 6.6,6.6 v 10.8 a 3,3 0 0 0 3,3 h 5.8 a 3,3 0 0 0 3,-3 v -11" />
  <path
     style="fill:none;stroke:#000000;stroke-linecap:round;stroke-linejoin:round;stroke-dasharray:none"
     d="m 4.5,6.5 h 16"
     id="path24623" />
  <path
     style="fill:none;stroke:#000000;stroke-width:1.1;stroke-linejoin:round;stroke-dasharray:none"
     d="m 9.5,8.5 v 9"
     id="path24627" />
  <path
     style="fill:none;stroke:#000000;stroke-width:1.1;stroke-linejoin:round;stroke-dasharray:none"
     d="m 15.5,8.5 v 9"
     id="path24629" />
  <path
     style="fill:none;stroke:#000000;stroke-width:1.1;stroke-linejoin:round;stroke-dasharray:none"
     d="m 12.5,8.5 v 9"
     id="path24629-3" />
  <path
     style="fill:none;stroke:#000000;stroke-width:1.1;stroke-linejoin:round;stroke-dasharray:none"
     d="M 7.5,6.6 9.7928932,4.3071068 A 2.4142136,2.4142136 157.5 0 1 11.5,3.6 h 2 a 2.4142136,2.4142136 22.5 0 1 1.707107,0.7071068 L 17.5,6.6"
     id="path24625-6" />
</svg>
""".trimIndent())

//val Run = createIcon(require("run.svg"))
val Run = createIcon("""
    <?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <!-- Created with Inkscape (http://www.inkscape.org/) -->

    <svg
       width="24"
       height="24"
       viewBox="0 0 24 24"
       version="1.1"
       id="svg38041"
       xmlns="http://www.w3.org/2000/svg"
       xmlns:svg="http://www.w3.org/2000/svg">
      <defs
         id="defs38038" />
      <g
         id="layer1">
        <path
           style="fill:none;stroke:#000000;stroke-width:1.1;stroke-linecap:round;stroke-dasharray:none"
           d="M 6.1229747,4.7477232 19.177025,11.297237 a 0.28322782,0.28322782 88.486771 0 1 0.01319,0.499245 L 6.1097861,19.248478 A 0.30754718,0.30754718 30.164791 0 1 5.65,18.981254 l 0,-13.9416662 A 0.32653963,0.32653963 148.32198 0 1 6.1229747,4.7477232 Z"
           id="path38218" />
      </g>
    </svg>
""".trimIndent())

//val DownloadSvg = createIcon(require("download-svg.svg"))
val DownloadSvg = createIcon("""
    <?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <!-- Created with Inkscape (http://www.inkscape.org/) -->

    <svg
       width="24"
       height="24"
       viewBox="0 0 24 24.000001"
       version="1.1"
       id="svg5"
       xmlns="http://www.w3.org/2000/svg"
       xmlns:svg="http://www.w3.org/2000/svg">
      <defs
         id="defs2" />
      <g
         id="layer1">
        <path
           style="fill:none;stroke:#000000;stroke-width:1.1;stroke-linecap:round"
           d="m 11.6,4.45 v 12"
           id="path287" />
        <path
           style="fill:none;stroke:#000000;stroke-width:1.1;stroke-linecap:round"
           d="m 11.6,16.45 5,-5"
           id="path289" />
        <path
           style="fill:none;stroke:#000000;stroke-width:1.1;stroke-linecap:round"
           d="m 11.6,16.45 -5,-5"
           id="path495" />
        <path
           style="fill:none;stroke:#000000;stroke-width:1.1;stroke-linecap:round;stroke-dasharray:none"
           d="m 3.6,19.35 h 16"
           id="path497" />
      </g>
    </svg>
""".trimIndent())

val Fit = createIcon("""
<?xml version="2.0" encoding="UTF-8"?><svg id="svg5" xmlns="http://www.w3.org/2000/svg" viewBox="-4 -4 24 24" width="24" height="24"><defs><style>.cls-1{fill:none;stroke:#000;stroke-linecap:round;stroke-linejoin:round;stroke-width:1.2}</style></defs><polyline class="cls-1" points=".75 5.75 .75 .75 5.75 .75"/><polyline class="cls-1" points="5.75 15.75 .75 15.75 .75 10.75"/><polyline class="cls-1" points="10.75 .75 15.75 .75 15.75 5.75"/><polyline class="cls-1" points="15.75 10.75 15.75 15.75 10.75 15.75"/></svg
""".trimIndent())
