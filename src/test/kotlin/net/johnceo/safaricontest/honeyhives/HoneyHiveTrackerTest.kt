package net.johnceo.safaricontest.honeyhives

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HoneyHiveTrackerTest {
    @Test fun `reported entrance hive countdown overrides expired loot and survives relog`() {
        val hive = HoneyHiveRules.catalog.single { it.key == "-724,93,211" }
        val status = HoneyHiveLabels.forHive(hive, HoneyHiveRules.catalog, listOf(
            HiveLabel(-723.5, 92.28, 211.5, "Refill in: 27m 19s"),
            HiveLabel(-723.5, 91.28, 204.5, "Refill in: 27m 14s"),
            HiveLabel(-720.5, 91.28, 207.5, "Refill in: 27m 14s")))
        assertEquals("Refill in: 27m 19s", status)
        val times = mutableMapOf<String, Long>()
        val observed = mutableMapOf<String, Long>()
        val loots = mutableMapOf(hive.key to 100L)
        val tracker = HoneyHiveTracker(times, lootedAt = loots, observedRefills = observed)
        val now = 8_000_000L
        assertEquals(HiveState.FULL, tracker.state(hive.key, now))
        assertTrue(tracker.observe(hive.key, status, now = now))
        assertEquals(HiveState.EMPTY, tracker.state(hive.key, now))
        assertEquals(now + 1_639_000L, times[hive.key])
        val saved = java.util.Properties().apply {
            setProperty("tracking.version", "2")
            setProperty("looted.player.profile.${hive.key}", "100")
            setProperty("observedRefill.player.profile.${hive.key}", observed.getValue(hive.key).toString())
        }
        val restored = HoneyHiveRules.savedRefills(saved, now).getValue("player.profile").toMutableMap()
        val restarted = HoneyHiveTracker(restored, lootedAt = loots, observedRefills = observed.toMutableMap())
        assertEquals(HiveState.EMPTY, restarted.state(hive.key, now + 1000))
        assertEquals(HiveState.FULL, restarted.state(hive.key, times.getValue(hive.key)))
        assertTrue(restarted.observe(hive.key, "Click to loot!", now = now + 2000))
        assertEquals(HiveState.FULL, restarted.state(hive.key, now + 2000))
        assertFalse(loots.containsKey(hive.key))
    }
    @Test fun `Queen Bee can precede the loot response and a delayed response cannot empty it`() {
        val times = mutableMapOf("other" to 9000000L)
        val full = mutableSetOf<String>()
        val loots = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times, full, lootedAt = loots)
        tracker.click("a", 100)
        assertTrue(tracker.chat("QUEEN BEE! The Honeyhive instantly refilled with loot!", 200))
        assertTrue("a" in full)
        assertTrue(tracker.chat("HIVE! Honey!", 300))
        assertNull(times["a"])
        assertEquals(300L, loots["a"])
        tracker.observe("a", null, honeyLevel = 5, now = 4000)
        assertNull(times["a"])
        tracker.click("a", 4100)
        tracker.chat("HIVE! Honey!", 4200)
        assertEquals(4200L, loots["a"])
        assertEquals(4200 + HoneyHiveRules.REFILL_MILLIS, times["a"])
        assertEquals(9000000L, times["other"])
    }
    @Test fun `restart restores the same players last profile and original loot deadline`() {
        assertEquals("-705,92,225", HoneyHiveRules.parse("-704 92 225 hive_7").single().key)
        assertEquals("-705,92,225", HoneyHiveRules.parseLoot("refill.player.profile-a.-704,92,225", "1234", "refill")!!.second)
        val owner = "player.profile-a"
        assertEquals(owner, HoneyHiveRules.resumeScope("player", null, owner))
        assertNull(HoneyHiveRules.resumeScope("other", null, owner))
        assertEquals("player.profileb", HoneyHiveRules.resumeScope("player", "profile-b", owner))
        val times = mutableMapOf<String, Long>()
        val loots = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times, lootedAt = loots)
        tracker.click("a", 100)
        tracker.chat("HIVE! Honey!", 200)
        val originalDeadline = 200 + HoneyHiveRules.REFILL_MILLIS
        assertFalse(tracker.observe("a", "Click to loot!", 5, 500))
        assertFalse(tracker.observe("a", "Refill in: 12m 34s", 0, 500))
        assertEquals(originalDeadline, times["a"])
        val restarted = HoneyHiveTracker(times, lootedAt = loots)
        assertEquals(HiveState.EMPTY, restarted.state("a", originalDeadline - 1))
        assertEquals(HiveState.FULL, restarted.state("a", originalDeadline))
        assertTrue(restarted.observe("a", "Refill in: 12m 34s", 0, originalDeadline + 1))
        assertEquals(HiveState.EMPTY, restarted.state("a", originalDeadline + 1))
        val saved = java.util.Properties().apply {
            setProperty("tracking.version", "2")
            setProperty("refill.$owner.a", (times.getValue("a") + 5000).toString())
            setProperty("looted.$owner.a", loots.getValue("a").toString())
        }
        val bytes = java.io.ByteArrayOutputStream().also { saved.store(it, "test") }.toByteArray()
        val restored = java.util.Properties().apply { load(java.io.ByteArrayInputStream(bytes)) }
        val deadline = HoneyHiveRules.savedRefills(restored, 500).getValue(owner).getValue("a")
        assertEquals(200 + HoneyHiveRules.REFILL_MILLIS, deadline)
        assertEquals(HiveState.EMPTY, HoneyHiveRules.state(500, deadline))
        assertEquals(HiveState.FULL, HoneyHiveRules.state(deadline, deadline))
        assertTrue(HoneyHiveRules.savedRefills(restored, deadline).isEmpty())
        restored.setProperty("observedRefill.$owner.a", "100000")
        assertEquals(100000L, HoneyHiveRules.savedRefills(restored, 500).getValue(owner).getValue("a"))
        restored.setProperty("full.$owner.a", "1")
        assertEquals(100000L, HoneyHiveRules.savedRefills(restored, 500).getValue(owner).getValue("a"), "legacy full flags do not override a hologram correction")
        restored.setProperty("queenFull.$owner.a", "1")
        assertTrue(HoneyHiveRules.savedRefills(restored, 500).isEmpty(), "only a confirmed Queen Bee proc overrides the timer")
    }
    @Test fun `a fresh click at the refill boundary upgrades an earlier empty click`() {
        val times = mutableMapOf("a" to 200L)
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100); tracker.click("a", 300)
        assertTrue(tracker.chat("HIVE! Honey!", 400))
        assertEquals(400 + HoneyHiveRules.REFILL_MILLIS, times["a"])
    }
    @Test fun `empty clicks cannot steal a later full hives successful loot`() {
        val times = mutableMapOf("a" to 9000L)
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100); tracker.click("b", 200)
        tracker.chat("HIVE! Honey!", 300)
        assertEquals(9000L, times["a"])
        assertEquals(300 + HoneyHiveRules.REFILL_MILLIS, times["b"])
    }
    @Test fun `new provisional loot overrides older saved Queen Bee readiness when profile arrives`() {
        val times = mutableMapOf<String, Long>()
        val full = mutableSetOf("a")
        val tracker = HoneyHiveTracker(mutableMapOf())
        tracker.click("a", 100); tracker.chat("HIVE! Honey!", 200)
        tracker.promote(times, full)
        assertEquals(200 + HoneyHiveRules.REFILL_MILLIS, times["a"])
        assertFalse("a" in full)
    }
    @Test fun `pre-fix timers are cleared once and subsequent restarts retain independent profile deadlines`() {
        val saved = java.util.Properties().apply {
            setProperty("refill.player.profile-a.a", "100000")
            setProperty("refill.player.profile-a.b", "200000")
            setProperty("refill.player.profile-b.a", "300000")
            setProperty("refill.player.profile-a.-730,132,184", "400000")
        }
        assertTrue(HoneyHiveRules.savedRefills(saved, 0).isEmpty())
        saved.setProperty("tracking.version", "2")
        saved.setProperty("queenFull.player.profile-a.c", "1")
        saved.setProperty("queenFull.player.unknown.a", "1")
        val restored = HoneyHiveRules.savedRefills(saved, 150000)
        assertEquals(mapOf("b" to 200000L), restored["player.profile-a"])
        assertEquals(mapOf("a" to 300000L), restored["player.profile-b"])
        assertEquals(mapOf("player.profile-a" to setOf("c")), HoneyHiveRules.savedQueenFull(saved))
        assertTrue(HoneyHiveRules.parse("-730 132 184 -").isEmpty())
    }
    @Test fun `Queen Bee allows another loot immediately and that deadline survives tracker recreation`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100); tracker.chat("HIVE! Honey!", 200)
        tracker.chat("QUEEN BEE! The Honeyhive instantly refilled", 300)
        tracker.click("a", 400); tracker.chat("HIVE! Honey!", 500)
        val restarted = HoneyHiveTracker(times)
        assertFalse(restarted.chat("HIVE! Honey!", 700))
        assertEquals(500 + HoneyHiveRules.REFILL_MILLIS, times["a"])
    }
    @Test fun `a repeat click before delayed Queen Bee chat is retained for its later loot`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100); tracker.chat("HIVE! Honey!", 300)
        tracker.click("a", 350); tracker.chat("QUEEN BEE! The Honeyhive instantly refilled", 400)
        tracker.chat("HIVE! Honey!", 500)
        assertEquals(500 + HoneyHiveRules.REFILL_MILLIS, times["a"])
    }
    @Test fun `Queen Bee ignores immediate stale holograms but later server labels correct readiness`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100); tracker.chat("HIVE! Honey!", 200)
        tracker.chat("QUEEN BEE! The Honeyhive instantly refilled", 400)
        assertFalse(tracker.observe("a", "Refill in: 59m 59s", 0, 600))
        tracker.observe("a", null, 0, 3000)
        assertEquals(HiveState.FULL, tracker.state("a", 3000))
        tracker.observe("a", "Refill in: 59m 59s", 5, 6000)
        assertEquals(HiveState.EMPTY, tracker.state("a", 6000))
        assertEquals(3605000L, times["a"])
    }
    @Test fun `profile promotion retains queued click and Queen Bee correlation`() {
        val provisional = mutableMapOf<String, Long>()
        val actual = mutableMapOf("older" to 9000L)
        val queen = mutableSetOf<String>()
        val tracker = HoneyHiveTracker(provisional, queen)
        tracker.click("a", 100)
        tracker.promote(actual, queen)
        tracker.chat("HIVE! Honey!", 200)
        assertEquals(200 + HoneyHiveRules.REFILL_MILLIS, actual["a"])
        val newActual = mutableMapOf<String, Long>()
        tracker.promote(newActual, queen)
        tracker.chat("QUEEN BEE! The Honeyhive instantly refilled", 300)
        assertNull(newActual["a"])
        tracker.resetSession()
        HoneyHiveTracker(newActual, queen).observe("a", null, 0, 9000)
        assertNull(newActual["a"], "Queen Bee readiness must survive reconnects")
    }
    @Test fun `rapid clicks consume distinct hives in order and Queen Bee targets confirmed loot`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100)
        tracker.click("b", 200)
        assertTrue(tracker.chat("HIVE! You found honey!", 300))
        assertEquals(300 + HoneyHiveRules.REFILL_MILLIS, times["a"])
        assertNull(times["b"])
        assertTrue(tracker.chat("QUEEN BEE! The Honeyhive instantly refilled", 400))
        assertNull(times["a"])
        tracker.chat("HIVE! More honey!", 500)
        assertEquals(500 + HoneyHiveRules.REFILL_MILLIS, times["b"])
        assertNull(times["a"])
    }
    @Test fun `duplicate clicks cannot consume another hives message and party text cannot loot`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100); tracker.click("a", 110); tracker.click("b", 200)
        assertFalse(tracker.chat("Party > Foo: HIVE! Honey!", 250))
        tracker.chat("HIVE! Honey!", 300); tracker.chat("HIVE! Honey!", 400)
        assertEquals(setOf("a", "b"), times.keys)
    }
    @Test fun `unmatched or expired chat never guesses a nearby hive`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        assertFalse(tracker.chat("HIVE! Honey!", 0))
        tracker.click("a", 10)
        assertFalse(tracker.chat("HIVE! Honey!", 10_000))
        assertTrue(times.isEmpty())
    }
    @Test fun `block levels preserve timers but current loot holograms override them`() {
        val times = mutableMapOf("a" to 9000000L)
        val tracker = HoneyHiveTracker(times)
        for (level in listOf(0, 3, 5, null)) {
            assertFalse(tracker.observe("a", null, level, 1000))
            assertEquals(9000000L, times["a"])
            assertEquals(HiveState.EMPTY, tracker.state("a", 1000))
            assertEquals(HiveState.FULL, tracker.state("a", 9000001))
        }
        tracker.observe("a", "Click to loot!", 0, 2000)
        assertEquals(HiveState.FULL, tracker.state("a", 2000))
        assertNull(times["a"])
        tracker.observe("a", "Click to loot!", 0, 9000001)
        assertEquals(HiveState.FULL, tracker.state("a", 9000001))
        assertNull(times["a"])
        tracker.observe("a", "Refill in: 12m 34s", 5, 10000000)
        assertEquals(10754000L, times["a"])
        assertEquals(HiveState.EMPTY, tracker.state("a", 10000000))
    }
    @Test fun `confirmed loot and Queen Bee are immediate despite inconsistent blocks`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        tracker.click("a", 100); tracker.chat("HIVE! Honey!", 200)
        tracker.observe("a", null, 5, 300)
        assertEquals(200 + HoneyHiveRules.REFILL_MILLIS, times["a"])
        assertEquals(HiveState.EMPTY, tracker.state("a", 300))
        tracker.chat("QUEEN BEE! The Honeyhive instantly refilled", 400)
        tracker.observe("a", null, 0, 500)
        assertEquals(HiveState.FULL, tracker.state("a", 500))
        assertNull(times["a"])
    }
    @Test fun `untracked block observations cannot invent a personal loot deadline`() {
        val times = mutableMapOf<String, Long>()
        val tracker = HoneyHiveTracker(times)
        tracker.observe("a", null, 0, 100)
        assertNull(times["a"])
        assertEquals(HiveState.FULL, tracker.state("a", 100))
    }
    @Test fun `server countdown understands seconds and minutes without accepting unrelated text`() {
        assertEquals(34_000L, HoneyHiveTracker.countdownMillis("Refill in: 34s"))
        assertEquals(120_000L, HoneyHiveTracker.countdownMillis("Refill in: 2m"))
        assertNull(HoneyHiveTracker.countdownMillis("Other hive refills in 2m"))
    }
}
