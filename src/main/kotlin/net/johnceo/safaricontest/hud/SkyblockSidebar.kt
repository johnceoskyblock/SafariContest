package net.johnceo.safaricontest.hud

import net.johnceo.safaricontest.contest.MiriaContestFilter
import net.johnceo.safaricontest.safari.SafariRules
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.PlayerTeam

object SkyblockSidebar {

    fun title(client: Minecraft): String =
        sidebar(client)
            ?.let { stripFormatting(it.displayName.string).trim() }
            ?: ""

    fun lines(client: Minecraft): List<String> = rawLines(client).map { stripFormatting(it).trim() }.filter(String::isNotEmpty)

    /** Preserve Hypixel's colored area row before private glyphs/formatting are stripped. */
    fun areaName(client: Minecraft): String? {
        val areaPattern = Regex("\\s*§.(.) §.(.*)")
        rawLines(client).forEach { row ->
            val match = areaPattern.matchEntire(row) ?: return@forEach
            if (match.groupValues[1] !in listOf("♲", "☀")) {
                return SafariRules.strip(match.groupValues[2])
            }
        }
        return client.connection?.onlinePlayers?.mapNotNull { it.tabListDisplayName?.string }
            ?.map(SafariRules::strip)
            ?.firstOrNull { it.startsWith("Area: ") }?.substringAfter("Area: ")
    }

    private fun rawLines(client: Minecraft): List<String> {
        val objective = sidebar(client) ?: return emptyList()
        val level = client.level ?: return emptyList()
        val scoreboard = level.scoreboard

        return MiriaContestFilter.sortScores(scoreboard.listPlayerScores(objective))
            .filterNot { it.isHidden }
            .map { entry ->
                val team = scoreboard.getPlayersTeam(entry.owner())

                val rendered: Component = if (team != null) {
                    PlayerTeam.formatNameForTeam(team, entry.ownerName())
                } else {
                    entry.ownerName()
                }

                rendered.string
            }
            .filter(String::isNotEmpty)
    }

    fun inSkyblock(client: Minecraft): Boolean =
        title(client).uppercase().contains("SKYBLOCK")

    private fun sidebar(client: Minecraft): Objective? =
        client.level
            ?.scoreboard
            ?.getDisplayObjective(DisplaySlot.SIDEBAR)

    private fun stripFormatting(text: String): String {
        if (!text.contains('§')) return text

        val out = StringBuilder(text.length)
        var i = 0

        while (i < text.length) {
            if (text[i] == '§' && i + 1 < text.length) {
                i += 2
            } else {
                out.append(text[i])
                i++
            }
        }

        return out.toString()
    }
}
