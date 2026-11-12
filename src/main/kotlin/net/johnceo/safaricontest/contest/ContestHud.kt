package net.johnceo.safaricontest.contest

import net.johnceo.safaricontest.compat.*
import net.johnceo.safaricontest.config.ContestConfig
import net.johnceo.safaricontest.hud.SkyblockSidebar
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

object ContestHud {
    fun render(g: GuiGraphicsExtractor) {
        val c = Minecraft.getInstance()
        if (c.player == null || c.options.smHideGui || !ContestConfig.showContestTimer ||
            !ContestConfig.trackContest || !SkyblockSidebar.inSkyblock(c)) return
        val x = ContestConfig.hudX.coerceIn(0, (c.window.guiScaledWidth - 190).coerceAtLeast(0))
        val y = ContestConfig.hudY.coerceIn(0, (c.window.guiScaledHeight - 68).coerceAtLeast(0))
        g.fill(x, y, x+190, y+68, 0xB0182018.toInt())
        g.text(c.font, "Miria's Contest", x+6, y+6, 0xFFFFCC66.toInt(), true)
        val gap = ContestTracker.betweenContests()
        val seconds = if (gap) ContestTracker.secondsToNextStart() else ContestTracker.secondsToEnd()
        val rows = listOf(
            "${if (gap) "Next contest" else "Time left"}: ${ContestTracker.formatRemaining(seconds)}",
            "Bracket: ${ContestTracker.currentTier().ifBlank { "—" }}",
            "Score: ${ContestTracker.currentPoints().takeIf { it >= 0 }?.let { "%,d".format(it) } ?: "—"}",
            "${if (ContestConfig.firstClassTicket) "First-Class" else "Basic"} ticket: ${if (ContestTracker.isComplete()) "Earned" else "Incomplete"}")
        rows.forEachIndexed { i, row -> g.text(c.font, row, x+6, y+18+i*11,
            if (i == 3 && ContestTracker.isComplete()) 0xFF55FF55.toInt() else 0xFFEEEEEE.toInt(), true) }
    }
}
