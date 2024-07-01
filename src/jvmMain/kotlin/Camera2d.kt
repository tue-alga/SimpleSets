import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.events.Event
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.math.transforms.buildTransform

/**
 * The [Camera2D] extension enables panning, rotating and zooming the view
 * with the mouse:
 * - left click and drag to **pan**
 * - right click and drag to **rotate**
 * - use the mouse wheel to **zoom** in and out
 *
 * Usage: `extend(Camera2D())`
 */
class Camera2D : Extension {
    override var enabled = true

    var view = Matrix44.IDENTITY
    var rotationCenter = Vector2.ZERO

    val changed = Event<Unit>()

    fun setupMouseEvents(mouse: MouseEvents) {
        mouse.buttonDown.listen {
            if (!it.propagationCancelled) {
                rotationCenter = it.position
            }
        }
        mouse.dragged.listen {
            if (!it.propagationCancelled) {
                when (it.button) {
                    MouseButton.CENTER, MouseButton.RIGHT -> {
                        it.cancelPropagation()
                        view = buildTransform {
                            translate(it.dragDisplacement)
                        } * view
                    }

                    else -> Unit
                }
            }
        }
        mouse.scrolled.listen {
            if (!it.propagationCancelled) {
                val scaleFactor = clamp(1.0 + it.rotation.y * 0.2, 0.1, 2.0)
                view = buildTransform {
                    translate(it.position)
                    scale(scaleFactor)
                    translate(-it.position)
                } * view
                it.cancelPropagation()
            }
        }
    }

    override fun setup(program: Program) {
        setupMouseEvents(program.mouse)
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        drawer.pushTransforms()
        drawer.ortho(RenderTarget.active)
        drawer.view = view
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        drawer.popTransforms()
    }
}
