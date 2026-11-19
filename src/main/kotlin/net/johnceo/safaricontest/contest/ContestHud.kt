package net.johnceo.safaricontest.contest

import net.johnceo.safaricontest.compat.*
import net.johnceo.safaricontest.config.ContestConfig
import net.johnceo.safaricontest.hud.SkyblockSidebar
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

/** Compact row with SparklingMutuals' pixel check/X sprites; independent of font glyph support. */
object ContestHud {
    const val TITLE = "Miria Contest"
    const val HEIGHT = 17
    val width get() = Minecraft.getInstance().font.width(TITLE) + 25
    fun render(g: GuiGraphicsExtractor) {
        val c = Minecraft.getInstance()
        if (c.player == null || c.options.smHideGui || c.smScreen is ContestGui || !ContestConfig.showContestTimer ||
            !ContestConfig.trackContest || !SkyblockSidebar.inSkyblock(c)) return
        val layout = ContestConfig.layoutFor(c.window.guiScaledWidth, c.window.guiScaledHeight)
        val box = layout.bounds(c.window.guiScaledWidth, c.window.guiScaledHeight, width, HEIGHT)
        renderAt(g, box.x, box.y, ContestTracker.isComplete())
    }
    fun renderAt(g: GuiGraphicsExtractor, x: Int, y: Int, earned: Boolean) {
        val c = Minecraft.getInstance()
        val scale = ContestConfig.layoutFor(c.window.guiScaledWidth, c.window.guiScaledHeight)
            .effectiveScale(c.window.guiScaledWidth, c.window.guiScaledHeight, width, HEIGHT)
        g.pose().pushMatrix(); g.pose().translate(x.toFloat(), y.toFloat()); g.pose().scale(scale, scale)
        val alpha = ((100 - ContestConfig.hudTransparency.coerceIn(0,100)) * 255 / 100) shl 24
        g.fill(0, 0, width, HEIGHT, alpha or ContestConfig.hudBackground)
        if (ContestConfig.hudBorder) {
            val color = 0xFF000000.toInt() or ContestConfig.hudBorderColor
            g.fill(0,0,width,1,color); g.fill(0,HEIGHT-1,width,HEIGHT,color)
            g.fill(0,0,1,HEIGHT,color); g.fill(width-1,0,width,HEIGHT,color)
        }
        g.text(c.font, TITLE, 5, 5, 0xFF000000.toInt() or ContestConfig.hudTitleColor, true)
        val sprite = if (earned) arrayOf("........#", ".......##", "......##.", "#....##..", "##..##...", ".####....", "..##.....")
            else arrayOf("##.....##", ".##...##.", "..##.##..", "...###...", "..##.##..", ".##...##.", "##.....##")
        sprite.forEachIndexed { row, line -> line.forEachIndexed { col, pixel ->
            if (pixel == '#') g.fill(width-14+col, 5+row, width-13+col, 6+row,
                if (earned) 0xFF55FF55.toInt() else 0xFFFF5555.toInt())
        } }
        g.pose().popMatrix()
    }
}
