package net.johnceo.safaricontest.party

import net.azureaaron.hmapi.events.HypixelPacketEvents
import net.azureaaron.hmapi.network.HypixelNetworking
import net.azureaaron.hmapi.network.packet.v2.s2c.PartyInfoS2CPacket
import net.azureaaron.hmapi.data.party.PartyRole
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.Minecraft
import net.johnceo.safaricontest.config.PartyMessage
import net.johnceo.safaricontest.config.ContestConfig
import net.johnceo.safaricontest.contest.ContestTracker
import net.johnceo.safaricontest.hud.SkyblockSidebar

/** Party membership gates ticket messages only. Starts are sent by every local user regardless of role. */
object PartyManager {
    private val ticketAuthorization = ContestPartyAuthorization()
    private var members = 0
    private var known = false
    private var leader: String? = null
    private var requestedAt = 0L
    private var refreshAgain = false
    fun register() {
        HypixelPacketEvents.HELLO.register { Minecraft.getInstance().execute { request() } }
        HypixelPacketEvents.PARTY_INFO.register { packet -> Minecraft.getInstance().execute {
            if (packet !is PartyInfoS2CPacket) return@execute
            requestedAt = 0
            if (refreshAgain) { refreshAgain = false; request(); return@execute }
            known = true
            members = if (packet.inParty) (packet.members?.keys?.map { it.toString() }.orEmpty() +
                listOfNotNull(Minecraft.getInstance().player?.uuid?.toString())).toSet().size else 0
            leader = if (packet.inParty) packet.members?.entries?.firstOrNull { it.value == PartyRole.LEADER }?.key?.toString() else null
            Minecraft.getInstance().player?.uuid?.toString()?.let { self ->
                ticketAuthorization.observeRoster(self,if (packet.inParty)
                    packet.members?.keys?.mapTo(hashSetOf()) { it.toString() }.orEmpty() + self else emptySet())
            }
        } }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { observeHold(message.string) }
            if (!overlay && PartyRosterSignals.changed(message.string)) Minecraft.getInstance().execute {
                known = false; members = 0; leader = null
                if (requestedAt != 0L) refreshAgain = true else request()
            }
        }
        ClientReceiveMessageEvents.CHAT.register { message, _, _, _, _ ->
            Minecraft.getInstance().execute { observeHold(message.string) }
        }
    }
    private fun observeHold(text: String) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        if (ticketAuthorization.observePartySignal(text)) {
            ContestConfig.ticketRequests.remove("${player.uuid}.${ContestTracker.absoluteWindow(java.time.LocalDateTime.now())}")
            ContestConfig.save()
        }
        if (!SkyblockSidebar.inSkyblock(client)) return
        ticketAuthorization.observe(text,player.uuid.toString(),player.name.string,
            ContestTracker.absoluteWindow(java.time.LocalDateTime.now()))
        val requests = requests(player.uuid.toString())
        if (TicketCommandGuard.observe(text, requests) || TicketCommandGuard.observeDeparture(text, requests)) ContestConfig.save()
    }
    private fun requests(player: String): MutableSet<String> {
        val window = ContestTracker.absoluteWindow(java.time.LocalDateTime.now())
        return ContestConfig.ticketRequests.getOrPut("$player.$window") { linkedSetOf() }
    }
    fun sendTicketMessage(client: Minecraft, message: String): Boolean {
        val player = client.player ?: return false
        if (!known || members <= 1) return false
        if (!ticketAuthorization.allows(player.uuid.toString(),ContestTracker.absoluteWindow(java.time.LocalDateTime.now()))) return false
        val pending = requests(player.uuid.toString())
        val role = leader?.let { it == player.uuid.toString() }
        val body = when (TicketCommandGuard.decide(message,player.name.string,role,pending)) {
            TicketCommandGuard.Decision.WAIT -> return false
            TicketCommandGuard.Decision.READY -> "r"
            TicketCommandGuard.Decision.SEND -> message
        }
        return sendPartyChat(client,body,requireParty = true)
    }
    private fun request() {
        requestedAt = System.currentTimeMillis()
        HypixelNetworking.sendPartyInfoC2SPacket(2)
    }
    fun tick(client: Minecraft) {
        if (client.connection == null) { reset(); return }
        if ((!known || members > 1 && leader == null) && client.player != null && System.currentTimeMillis() - requestedAt > 10_000) request()
    }
    fun reset() { known = false; members = 0; leader = null; requestedAt = 0; refreshAgain = false; ticketAuthorization.reset() }
    fun sendPartyChat(client: Minecraft, message: String, requireParty: Boolean = false): Boolean {
        val connection = client.connection ?: return false
        if (requireParty && (!known || members <= 1)) return false
        val body = PartyMessage.sanitize(message)
        if (body.isBlank()) return false
        connection.sendCommand("pc $body")
        return true
    }
}
