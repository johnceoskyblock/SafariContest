package net.johnceo.safaricontest

import net.johnceo.safaricontest.config.SafariTheme
import net.johnceo.safaricontest.hud.SkyblockSidebar
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

internal class JoinWelcomeState {
    private var readyAt: Long? = null
    private var shown = false
    fun shouldShow(skyblock: Boolean, now: Long): Boolean {
        if (shown) return false
        if (!skyblock) { readyAt = null; return false }
        val start = readyAt ?: now.also { readyAt = it }
        if (now - start < 2000) return false
        shown = true
        return true
    }
}

object JoinWelcome {
    private var state = JoinWelcomeState()
    fun tick(client: Minecraft) {
        val player = client.player ?: return
        if (client.connection == null || client.level == null) return
        if (state.shouldShow(SkyblockSidebar.inSkyblock(client), System.currentTimeMillis())) player.sendSystemMessage(message())
    }
    fun message(): Component {
        fun text(value: String, color: Int) = Component.literal(value).withStyle { it.withColor(color) }
        val divider = "────────────────────────"
        return text("$divider\n  ", SafariTheme.ACCENT)
            .append(text("SafariContest", SafariTheme.CHAT_ACCENT).withStyle { it.withBold(true) })
            .append(text("\n  ", SafariTheme.ACCENT))
            .append(text("/s", SafariTheme.CHAT_ACCENT).withStyle {
                it.withClickEvent(ClickEvent.RunCommand("/s"))
                    .withHoverEvent(HoverEvent.ShowText(Component.literal("Open SafariContest settings")))
            })
            .append(text(" — Open settings\n", SafariTheme.CHAT_TEXT))
            .append(text(divider, SafariTheme.ACCENT))
    }
    fun reset() { state = JoinWelcomeState() }
}
