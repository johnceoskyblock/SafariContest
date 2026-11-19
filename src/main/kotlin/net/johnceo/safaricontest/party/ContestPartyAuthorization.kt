package net.johnceo.safaricontest.party

/** An earned ticket alone never authorizes a party message. Require the local user's confirmed party echo. */
class ContestPartyAuthorization {
    private var player: String? = null
    private var window: Long? = null
    private var rosterPlayer: String? = null
    private var peers: Set<String>? = null
    fun observe(text: String, selfId: String, selfName: String, currentWindow: Long) {
        if (TicketCommandGuard.isOwnContestRequest(text,selfName)) {
            player = selfId; window = currentWindow
        }
    }
    fun allows(selfId: String, currentWindow: Long) = player == selfId && window == currentWindow
    fun observeRoster(selfId: String, members: Set<String>) {
        val nextPeers = members.filterNot { it == selfId }.toSet()
        if (rosterPlayer != null && rosterPlayer != selfId || nextPeers.isEmpty() ||
            peers?.let { it.isNotEmpty() && it.intersect(nextPeers).isEmpty() } == true) clearPermission()
        rosterPlayer = selfId; peers = nextPeers
    }
    fun observePartySignal(text: String): Boolean {
        if (!PartyRosterSignals.replacesParty(text)) return false
        reset()
        return true
    }
    private fun clearPermission() { player = null; window = null }
    fun reset() { clearPermission(); rosterPlayer = null; peers = null }
}
