package net.johnceo.safaricontest

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.johnceo.safaricontest.compat.*
import net.johnceo.safaricontest.config.*
import net.johnceo.safaricontest.contest.*
import net.johnceo.safaricontest.honeyhives.HoneyHives
import net.johnceo.safaricontest.party.PartyManager
import net.johnceo.safaricontest.safari.SafariIsland
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier

object SafariContest : ClientModInitializer {
    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath("safari-contest", path)
    private var openSettings = false
    private var smokeTicks = if (java.lang.Boolean.getBoolean("safari-contest.smoke")) 80 else -1
    override fun onInitializeClient() {
        ContestConfig.load()
        PartyManager.register()
        SafariIsland.register()
        HoneyHives.register()
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            for (name in listOf("safaricontest", "sc")) dispatcher.register(ClientCommands.literal(name)
                .executes { openSettings = true; 1 }
                .then(ClientCommands.literal("debug").executes { HoneyHives.debug(Minecraft.getInstance()); 1 }))
        }
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (smokeTicks > 0 && --smokeTicks == 0) openSettings = true
            if (openSettings) { openSettings = false; client.smSetScreen(ContestSettingsScreen()) }
            PartyManager.tick(client)
            HoneyHives.tick(client)
            ContestTracker.onClientTick(client)
        }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            PartyManager.reset(); SafariIsland.reset(); HoneyHives.disconnect(); ContestTracker.selectScope(null)
        }
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("contest")) { g, _ -> ContestHud.render(g) }
    }
}
