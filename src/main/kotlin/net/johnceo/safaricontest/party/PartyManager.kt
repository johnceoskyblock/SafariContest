package net.johnceo.safaricontest.party

import net.azureaaron.hmapi.events.HypixelPacketEvents
import net.azureaaron.hmapi.network.HypixelNetworking
import net.azureaaron.hmapi.network.packet.v2.s2c.PartyInfoS2CPacket
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.Minecraft
import net.johnceo.safaricontest.config.PartyMessage

/** Party membership gates ticket messages only. Starts are sent by every local user regardless of role. */
object PartyManager {
    private var members = 0
    private var known = false
    private var requestedAt = 0L
    fun register() {
        HypixelPacketEvents.HELLO.register { Minecraft.getInstance().execute { request() } }
        HypixelPacketEvents.PARTY_INFO.register { packet -> Minecraft.getInstance().execute {
            if (packet !is PartyInfoS2CPacket) return@execute
            known = true
            members = if (packet.inParty) packet.members?.size ?: 0 else 0
            requestedAt = 0
        } }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay && PartyRosterSignals.changed(message.string)) Minecraft.getInstance().execute {
                known = false; members = 0; request()
            }
        }
    }
    private fun request() {
        requestedAt = System.currentTimeMillis()
        HypixelNetworking.sendPartyInfoC2SPacket(2)
    }
    fun tick(client: Minecraft) {
        if (client.connection == null) { reset(); return }
        if (!known && client.player != null && System.currentTimeMillis() - requestedAt > 10_000) request()
    }
    fun reset() { known = false; members = 0; requestedAt = 0 }
    fun sendPartyChat(client: Minecraft, message: String, requireParty: Boolean = false): Boolean {
        val connection = client.connection ?: return false
        if (requireParty && (!known || members <= 1)) return false
        val body = PartyMessage.sanitize(message)
        if (body.isBlank()) return false
        connection.sendCommand("pc $body")
        return true
    }
}
