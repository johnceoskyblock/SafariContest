package net.johnceo.safaricontest.honeyhives

import kotlin.math.abs
import net.johnceo.safaricontest.safari.SafariRules

data class HiveLabel(val x: Double, val y: Double, val z: Double, val text: String)

object HoneyHiveLabels {
    fun statusLines(raw: String): List<String> = SafariRules.strip(raw).replace('\u00a0', ' ').lines()
        .map(String::trim).filter { it.equals("Click to loot!", true) || HoneyHiveTracker.countdownMillis(it) != null }
    fun forHive(hive: Hive, hives: Collection<Hive>, labels: List<HiveLabel>): String? = labels.filter { label ->
        val nearest = hives.map { candidate ->
            val dx = candidate.x + .5 - label.x
            val dy = candidate.y + 1.5 - label.y
            val dz = candidate.z + .5 - label.z
            candidate to dx * dx + dy * dy + dz * dz
        }.sortedBy { it.second }
        val unambiguous = nearest.size < 2 || abs(nearest[0].second - nearest[1].second) > .01
        unambiguous && nearest.firstOrNull()?.first?.key == hive.key && abs(label.x - hive.x - .5) <= 1.5 &&
            abs(label.z - hive.z - .5) <= 1.5 && label.y in (hive.y - 1.0)..hive.y + 4.0
    }.sortedBy { if (HoneyHiveTracker.countdownMillis(it.text) != null) 0 else 1 }.firstOrNull()?.text
}
