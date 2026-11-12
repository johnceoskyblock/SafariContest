package net.johnceo.safaricontest.party

/** Unknown membership is distinct from a server-confirmed solo player. */
class PartyRosterState {
    var members = emptyList<String>()
        private set
    var known = false
        private set
    private var leader: String? = null
    var continuity = 0L
        private set
    fun update(ids: List<String>, leaderId: String? = null) {
        val next = ids.map(String::lowercase).distinct()
        val nextLeader = leaderId?.lowercase()?.takeIf { it in next }
        if (!known || next.toSet() != members.toSet() || nextLeader != leader) continuity++
        members = next
        leader = nextLeader
        known = true
    }
    fun isLeader(uuid: String) = known && leader == uuid.lowercase()
    fun invalidate() { if (known) continuity++; known = false; leader = null }
    fun withLocal(local: String): Set<String>? = if (known) (members + local.lowercase()).toSet() else null
    fun reset() { members = emptyList(); invalidate() }
}

/** Membership messages only: ordinary chat and invitations do not change the roster. */
object PartyRosterSignals {
    private val name = "(?:\\[[^]\\r\\n]+]\\s*)*[A-Za-z0-9_]{1,16}"
    private val messages = listOf(
        Regex("^$name has promoted $name to Party Leader[.!]?$"),
        Regex("^The party was transferred to $name(?: by $name)?[.!]?$"),
        Regex("^$name was removed from your party because they disconnected[.!]?$"),
        Regex("^Kicked $name because they were offline[.!]?$"),
        Regex("^$name (?:joined the party[.!]|has left the party[.!]|has been removed from the party[.!]|has disbanded the party[.!])$"),
        Regex("^You have joined $name's party!$"),
        Regex("^You (?:left the party[.!]|have been kicked from the party(?: by $name[.!]?)?)$"),
        Regex("^The party was disbanded(?:[.!]| because .+)$"),
        Regex("^You are not (?:currently )?in a party[.!]?$"))
    fun changed(text: String) = messages.any { it.matches(text.replace(Regex("§."), "").trim()) }
}
