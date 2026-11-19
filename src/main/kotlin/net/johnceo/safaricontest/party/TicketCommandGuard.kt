package net.johnceo.safaricontest.party

/** Per-sender readiness. Only anchored party envelopes change requests, including the local user's echo. */
object TicketCommandGuard {
    enum class Decision { SEND, READY, WAIT }
    private val envelope = Regex("""^Party\s*>\s*(?:\[[^\]]+]\s*)*([A-Za-z0-9_]{1,16}):\s*(.*)$""")
    private val downtime = Regex("""^!dt(?:\s+.*)?$""", RegexOption.IGNORE_CASE)
    fun isOwnContestRequest(text: String, self: String): Boolean {
        if (self.isBlank()) return false
        val match = envelope.matchEntire(text.replace(Regex("§."), "").trim()) ?: return false
        return match.groupValues[1].equals(self,true) &&
            match.groupValues[2].trim().matches(Regex("^!dt\\s+contest$", RegexOption.IGNORE_CASE))
    }
    fun observe(text: String, requests: MutableSet<String>): Boolean {
        val match = envelope.matchEntire(text.replace(Regex("§."), "").trim()) ?: return false
        val sender = match.groupValues[1].lowercase()
        val body = match.groupValues[2].trim()
        if (downtime.matches(body) || body.equals("1s", true) || body.equals("sec", true)) return requests.add(sender)
        if (body.matches(Regex("^(?:r|ready)[.!]*$", RegexOption.IGNORE_CASE))) return requests.remove(sender)
        return false
    }
    fun decide(message: String, self: String, leader: Boolean?, requests: Set<String>): Decision {
        if (!message.trimStart().startsWith("!")) return Decision.SEND
        if (leader == null) return Decision.WAIT
        return if (leader) {
            if (requests.any { !it.equals(self,true) }) Decision.WAIT else Decision.SEND
        } else if (requests.isNotEmpty()) Decision.READY else Decision.SEND
    }
    fun observeDeparture(text: String, requests: MutableSet<String>): Boolean {
        val clean = text.replace(Regex("§."), "").trim()
        if (clean.matches(Regex("^You (?:left the party[.!]|have been kicked from the party.*|have joined .+'s party!)$")) ||
            clean.matches(Regex("^The party was disbanded(?:[.!]| because .+)$"))) {
            val changed = requests.isNotEmpty(); requests.clear(); return changed
        }
        val match = Regex("""^(?:\[[^\]]+]\s*)*([A-Za-z0-9_]{1,16}) (?:has left the party[.!]|has been removed from the party[.!]|was removed from your party because they disconnected[.!]?)$""")
            .matchEntire(clean) ?: Regex("""^Kicked (?:\[[^\]]+]\s*)*([A-Za-z0-9_]{1,16}) because they were offline[.!]?$""")
                .matchEntire(clean) ?: return false
        return requests.remove(match.groupValues[1].lowercase())
    }
}
