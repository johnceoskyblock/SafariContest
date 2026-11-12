package net.johnceo.safaricontest.contest

/** Observe continuously; no catch-up when joining, enabling, or entering SkyBlock mid-window. */
class ContestStartState {
    private var previous: Long? = null
    fun poll(window: Long, secondsIntoWindow: Int, enabled: Boolean, skyblock: Boolean): Boolean {
        val before = previous
        previous = window
        return before != null && before != window && secondsIntoWindow in 0..9 && enabled && skyblock
    }
}
