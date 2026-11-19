package net.johnceo.safaricontest.hud

import kotlin.math.abs
import kotlin.math.roundToInt

/** Screen-center/edge snapping adapted from SafariUtils HudEditorScreen (MIT). */
object HudSnapping {
    data class Position(
        val x: Int, val y: Int, val centeredX: Boolean, val centeredY: Boolean,
        val guideX: Int? = null, val guideY: Int? = null
    )

    fun position(
        x: Double, y: Double, width: Int, height: Int, boxWidth: Int, boxHeight: Int,
        peers: List<HudBounds> = emptyList()
    ): Position {
        val px = axis(x, width, boxWidth, peers.map { it.x to it.width })
        val py = axis(y, height, boxHeight, peers.map { it.y to it.height })
        return Position(px.pixel, py.pixel, px.centered, py.centered, px.guide, py.guide)
    }

    private data class Axis(val pixel: Int, val centered: Boolean = false, val guide: Int? = null)

    private fun axis(value: Double, screen: Int, size: Int, peers: List<Pair<Int, Int>>): Axis {
        val far = (screen - size).coerceAtLeast(0)
        val pixel = value.roundToInt().coerceIn(0, far)
        val center = far / 2
        var best = Axis(pixel)
        var distance = Int.MAX_VALUE

        fun consider(target: Int, radius: Int, centered: Boolean = false, guide: Int? = null) {
            val delta = abs(pixel - target)
            if (target in 0..far && delta <= radius && delta < distance) {
                best = Axis(target, centered, guide)
                distance = delta
            }
        }

        consider(center, 5, centered = true)
        consider(0, 3)
        consider(far, 3)
        for ((start, length) in peers) {
            val end = start + length
            consider(start, 5, guide = start)
            consider(end - size, 5, guide = end)
            val midpoint = start + length / 2
            consider(midpoint - size / 2, 5, guide = midpoint)
            consider(start - size, 5, guide = start)
            consider(end, 5, guide = end)
        }
        return best
    }
}
