package net.johnceo.safaricontest.honeyhives

enum class HiveState { FULL, EMPTY }

/** A honeyhive position and, once matched, the id Hypixel's API knows it by (`hive_8`). */
data class Hive(val x: Int, val y: Int, val z: Int, var id: String? = null) {
    val key get() = "$x,$y,$z"
}

/** The decisions behind the honeyhive beacons, free of the client so they can be tested. */
object HoneyHiveRules {
    val catalog: List<Hive> by lazy {
        checkNotNull(HoneyHiveRules::class.java.getResourceAsStream("/assets/safari-contest/honeyhives.txt"))
            .bufferedReader().use { parse(it.readText()) }
    }
    private val catalogKeys by lazy { catalog.mapTo(hashSetOf()) { it.key } }
    fun knownHive(key: String) = key in catalogKeys
    /** Unloaded known waypoints remain useful; a loaded missing block is never a hive waypoint. */
    fun waypointExists(key: String, loaded: Boolean, beeNest: Boolean) = knownHive(key) && (!loaded || beeNest)
    /** Old terrain scans and clicks could persist decorative/temporary nests as lootable honeyhives. */
    fun removeUnknownPositions(saved: java.util.Properties): Int {
        val prefixes = listOf("hive.", "refill.", "observedRefill.", "looted.", "state.", "full.", "queenFull.")
        val invalid = saved.stringPropertyNames().filter { name ->
            prefixes.any(name::startsWith) && !knownHive(correctedKey(name.substringAfterLast('.')))
        }
        invalid.forEach(saved::remove)
        return invalid.size
    }
    const val REMOVED_HIVE = "-730,132,184"
    fun correctedKey(key: String) = if (key == "-704,92,225") "-705,92,225" else key
    /** Discard pre-fix timers once; later launches retain each valid hive's personal deadline. */
    fun savedRefills(saved: java.util.Properties, now: Long): Map<String, Map<String, Long>> {
        if (saved.getProperty("tracking.version") != "2") return emptyMap()
        val result = mutableMapOf<String, MutableMap<String, Long>>()
        saved.stringPropertyNames().forEach { name ->
            parseLoot(name, saved.getProperty(name), "refill")
                ?.takeIf { (owner, key, at) -> !owner.endsWith(".unknown") && key != REMOVED_HIVE && now < at }
                ?.let { (owner, key, at) -> result.getOrPut(owner) { mutableMapOf() }.merge(key, at, ::maxOf) }
        }
        // Restore exact loot deadlines first, then apply any saved current-hologram correction below.
        saved.stringPropertyNames().forEach { name ->
            parseLoot(name, saved.getProperty(name), "looted")?.let { (owner, key, at) ->
                if (!owner.endsWith(".unknown") && key != REMOVED_HIVE && at >= 0 && at <= Long.MAX_VALUE - REFILL_MILLIS) {
                    val times = result.getOrPut(owner) { mutableMapOf() }
                    if (at + REFILL_MILLIS > now) times[key] = at + REFILL_MILLIS else times.remove(key)
                }
            }
        }
        saved.stringPropertyNames().forEach { name ->
            parseLoot(name, saved.getProperty(name), "observedRefill")?.let { (owner, key, at) ->
                if (!owner.endsWith(".unknown") && key != REMOVED_HIVE) {
                    val times = result.getOrPut(owner) { mutableMapOf() }
                    if (at > now) times[key] = at else times.remove(key)
                }
            }
        }
        savedQueenFull(saved).forEach { (owner, keys) -> keys.forEach { result[owner]?.remove(it) } }
        result.entries.removeIf { it.value.isEmpty() }
        return result
    }
    fun savedQueenFull(saved: java.util.Properties): Map<String, Set<String>> {
        if (saved.getProperty("tracking.version") != "2") return emptyMap()
        val result = mutableMapOf<String, MutableSet<String>>()
        saved.stringPropertyNames().forEach { name ->
            // Legacy `full` mixed hologram observations with real Queen Bee procs and cannot override a timer.
            parseLoot(name, saved.getProperty(name), "queenFull")
                ?.takeIf { (owner, key, value) -> !owner.endsWith(".unknown") && key != REMOVED_HIVE && value == 1L }
                ?.let { (owner, key, _) -> result.getOrPut(owner) { mutableSetOf() }.add(key) }
        }
        return result
    }
    /** Measured against the API: a looted hive refills an hour later, to the second. */
    const val REFILL_MILLIS = 3_600_000L
    /** How close a refill time has to be to loot + an hour to count as that loot. */
    const val MATCH_TOLERANCE = 15_000L
    /** How far from a hive the loot message can be and still be taken as that hive's. */
    const val LOOT_RADIUS = 8.0

    /**
     * Every hive is tracked on its own by one number, when it refills: empty until then, full after. Nothing else
     * changes this timer unless a Queen Bee proc or loaded server state confirms an update. A hive never looted is full.
     */
    fun state(now: Long, refillAt: Long?): HiveState = if (refillAt != null && now < refillAt) HiveState.EMPTY else HiveState.FULL

    /**
     * Folds the API's refill time for a hive into its own: the later one wins, since the API can lag a loot just seen
     * here. A time from a loot before the hive's last Queen Bee proc is stale (the API's time runs a second or so past
     * the loot, hence the tolerance), and one already passed changes nothing.
     */
    fun merge(current: Long?, apiRefill: Long, queenAt: Long?, now: Long): Long? = when {
        apiRefill <= now -> current
        queenAt != null && apiRefill - REFILL_MILLIS <= queenAt + MATCH_TOLERANCE -> current
        else -> maxOf(current ?: apiRefill, apiRefill)
    }

    /** Hive loots belong to one player's SkyBlock profile; unknown until both are known. */
    fun scope(player: String?, profile: String?): String? =
        if (player.isNullOrBlank() || profile.isNullOrBlank()) null else "${player.replace("-", "")}.${profile.replace("-", "")}"
    /** Reuse only this player's most recently confirmed profile while its join message is pending. */
    fun resumeScope(player: String?, profile: String?, lastScope: String?): String? = scope(player, profile)
        ?: lastScope?.takeIf { player != null && it.startsWith("${player.replace("-", "")}.") && !it.endsWith(".unknown") }

    private val profileId = Regex("^Profile ID: ([0-9a-fA-F-]{32,36})$")
    /** Hypixel sends `Profile ID: <uuid>` whenever you join a SkyBlock server. */
    fun profileId(line: String): String? = profileId.matchEntire(line.trim())?.groupValues?.get(1)?.lowercase()

    /** `<prefix>.<scope>.<x,y,z>` entries back to (scope, key, time); entries without an owner are dropped. */
    fun parseLoot(name: String, value: String, prefix: String = "loot"): Triple<String, String, Long>? {
        val rest = name.removePrefix("$prefix.").takeIf { it != name } ?: return null
        val cut = rest.lastIndexOf('.').takeIf { it > 0 } ?: return null
        val time = value.toLongOrNull() ?: return null
        return Triple(rest.substring(0, cut), correctedKey(rest.substring(cut + 1)), time)
    }

    fun nearest(hives: Collection<Hive>, x: Double, y: Double, z: Double): Hive? = hives
        .map { it to (it.x + .5 - x).let { d -> d * d } + (it.y + .5 - y).let { d -> d * d } + (it.z + .5 - z).let { d -> d * d } }
        .filter { it.second <= LOOT_RADIUS * LOOT_RADIUS }.minByOrNull { it.second }?.first

    /** Bind only unambiguous pairs; close loots must never persist the wrong position-to-id mapping. */
    fun bind(pending: Map<String, Long>, refills: Map<String, Long>, taken: Set<String>): Map<String, String> {
        val candidates = pending.mapValues { (_, lootedAt) ->
            refills.filter { (id, at) -> id !in taken && kotlin.math.abs(at - (lootedAt + REFILL_MILLIS)) <= MATCH_TOLERANCE }.keys
        }
        return candidates.filterValues { it.size == 1 }
            .filter { (_, ids) -> candidates.values.count { ids.single() in it } == 1 }
            .mapValues { (_, ids) -> ids.single() }
    }

    /** Lines of `x y z id` (id `-` while unmatched); `#` starts a comment. */
    fun parse(text: String): List<Hive> = text.lines().mapNotNull { line ->
        val parts = line.substringBefore('#').trim().split(Regex("\\s+"))
        if (parts.size < 3) return@mapNotNull null
        val (x, y, z) = parts.take(3).map { it.toIntOrNull() ?: return@mapNotNull null }
        Hive(if (x == -704 && y == 92 && z == 225) -705 else x, y, z, parts.getOrNull(3)?.takeIf { it != "-" }).takeIf { it.key != REMOVED_HIVE }
    }

}
