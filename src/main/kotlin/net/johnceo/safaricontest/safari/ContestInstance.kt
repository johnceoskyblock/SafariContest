package net.johnceo.safaricontest.safari

/** Exact Mod API modes distinguish instances from dungeon hub, Crimson Isle and other lobbies. */
object ContestInstance {
    fun eligible(mode: String?) = mode?.lowercase() in setOf("dungeon", "kuudra")
}
