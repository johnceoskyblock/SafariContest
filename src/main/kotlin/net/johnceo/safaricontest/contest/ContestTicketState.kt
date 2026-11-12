package net.johnceo.safaricontest.contest

/** One local announcement per player and absolute contest window, independent of profile/HUD refreshes. */
class ContestTicketState {
    private val announced = mutableSetOf<Triple<String, Long, Boolean>>()
    fun claim(player: String, window: Long, tier: String, secondsIntoWindow: Int, firstClass: Boolean = false, points: Int = -1,
              dispatch: () -> Boolean = { true }): Boolean {
        if (player.isBlank() || secondsIntoWindow < 10 || !earned(tier, firstClass, points)) return false
        announced.removeAll { it.second < window - 2 }
        val key = Triple(player.lowercase(), window, firstClass)
        return key !in announced && dispatch() && announced.add(key)
    }
    companion object {
        val earnedTiers = setOf("UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "DIVINE", "SPECIAL")
        fun earned(tier: String, firstClass: Boolean, points: Int = -1) = if (firstClass)
            tier.uppercase() in setOf("LEGENDARY", "MYTHIC", "DIVINE", "SPECIAL") || points >= 5000
        else tier.uppercase() in earnedTiers
    }
}
