package net.johnceo.safaricontest

import net.johnceo.safaricontest.config.SafariTheme
import net.johnceo.safaricontest.config.ModUpdates
import net.johnceo.safaricontest.config.ReleaseUpdateChecker
import java.net.URI
import net.johnceo.safaricontest.hud.SkyblockSidebar
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

internal class JoinWelcomeState {
    private var readyAt: Long? = null
    var shown = false
        private set
    fun shouldShow(skyblock: Boolean, now: Long, updateReady: Boolean = true): Boolean {
        if (shown) return false
        if (!skyblock) { readyAt = null; return false }
        val start = readyAt ?: now.also { readyAt = it }
        if (!updateReady || now - start < 2000) return false
        shown = true
        return true
    }
}

object JoinWelcome {
    private var state = JoinWelcomeState()
    fun tick(client: Minecraft) {
        if (state.shown) return
        val player = client.player ?: return
        if (client.connection == null || client.level == null) return
        val skyblock = SkyblockSidebar.inSkyblock(client)
        if (skyblock) ModUpdates.check()
        if (state.shouldShow(skyblock, System.currentTimeMillis(), ModUpdates.label != "Checking…"))
            player.sendSystemMessage(message(ModUpdates.release?.tag,ModUpdates.label))
    }
    fun message(releaseTag: String? = null, status: String = "Couldn't check"): Component {
        fun text(value: String, color: Int) = Component.literal(value).withStyle { it.withColor(color) }
        val divider = "────────────────────────"
        val block = text("$divider\n  ", SafariTheme.ACCENT)
            .append(text("SafariContest", SafariTheme.CHAT_ACCENT).withStyle { it.withBold(true) })
            .append(text("\n  ", SafariTheme.ACCENT))
            .append(text("/s", SafariTheme.CHAT_ACCENT).withStyle {
                it.withClickEvent(ClickEvent.RunCommand("/s"))
                    .withHoverEvent(HoverEvent.ShowText(Component.literal("Open SafariContest settings")))
            })
            .append(text(" — Open settings\n", SafariTheme.CHAT_TEXT))
        block.append(text("  Latest release: ",SafariTheme.CHAT_TEXT))
        val tag = releaseTag?.takeIf { ReleaseUpdateChecker.compareVersions(it,it) != null }
        if (tag != null) {
            block.append(text("v${tag.removePrefix("v")}",SafariTheme.CHAT_ACCENT).withStyle {
                it.withUnderlined(true).withClickEvent(ClickEvent.OpenUrl(URI.create("${ModUpdates.REPOSITORY}/releases/tag/$tag")))
                    .withHoverEvent(HoverEvent.ShowText(Component.literal("Open the SafariContest release")))
            }).append(text(" ($status)",SafariTheme.CHAT_TEXT))
        } else block.append(text(status,SafariTheme.MUTED))
        return block.append(text("\n$divider", SafariTheme.ACCENT))
    }
    fun reset() { state = JoinWelcomeState() }
}
