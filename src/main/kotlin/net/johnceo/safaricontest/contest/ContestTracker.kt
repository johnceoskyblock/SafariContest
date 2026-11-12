package net.johnceo.safaricontest.contest

import net.johnceo.safaricontest.compat.*
import java.time.LocalTime
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import net.johnceo.safaricontest.config.ContestConfig
import net.johnceo.safaricontest.hud.SkyblockSidebar
import net.johnceo.safaricontest.party.PartyManager
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

object ContestTracker {

    private const val FIRST_START_MINUTE = 15
    private const val WINDOW_SECONDS = 1200
    private const val END_GAP_SECONDS = 30
    private const val STALE_GUARD_SECONDS = 10
    private const val MAX_SOUND_COPIES = 20

    private val TIER_ROW = Regex("""^\s*([A-Za-z]+)\s+with\s+(\S+)""")

    private var selectedTicketMode = ContestConfig.ticketMode
    private val ticketNotices = ContestTicketState()

    private val starts = ContestStartState()

    private var tickCounter = 0
    private var windowKey = -1L
    private var secondsIntoWindow = 0
    private var complete = false
    private var completedWindow = -1L
    private var points = -1
    private var tier = ""
    private var scope: String? = null

    private val warnedMinutes = linkedSetOf<Int>()
    private data class Progress(val window: Long, val seconds: Int, val complete: Boolean,
        val completedWindow: Long, val points: Int, val tier: String, val warnings: Set<Int>, val ticketMode: Int)
    private val retained = mutableMapOf<String, Progress>()

    /** Completion and sidebar evidence belong to one player/profile, including while tracking is disabled. */
    fun selectScope(owner: String?, now: java.time.LocalDateTime = java.time.LocalDateTime.now()) {
        if (scope == owner) return
        val currentWindow = absoluteWindow(now)
        scope?.takeIf { windowKey == currentWindow }?.let {
            retained[it] = Progress(windowKey, secondsIntoWindow, complete, completedWindow, points, tier,
                warnedMinutes.toSet(), selectedTicketMode)
        }
        retained.entries.removeIf { it.value.window != currentWindow }
        scope = owner
        complete = false; completedWindow = -1L; windowKey = -1L
        points = -1; tier = ""; tickCounter = 0; warnedMinutes.clear()
        owner?.let(retained::get)?.let { saved ->
            windowKey = saved.window; secondsIntoWindow = saved.seconds
            complete = saved.complete; completedWindow = saved.completedWindow
            points = saved.points; tier = saved.tier; warnedMinutes.addAll(saved.warnings)
            selectedTicketMode = saved.ticketMode
        }
    }

    fun onClientTick(client: Minecraft) {
        if (client.player == null || client.level == null) return

        if (++tickCounter < 10) return
        tickCounter = 0

        rollWindow()
        if (selectedTicketMode != ContestConfig.ticketMode) {
            selectedTicketMode = ContestConfig.ticketMode
            complete = false; completedWindow = -1L; warnedMinutes.clear()
        }
        val skyblock = SkyblockSidebar.inSkyblock(client)
        val player = client.player!!
        val startKey = player.uuid.toString()
        if (starts.poll(windowKey, secondsIntoWindow, ContestConfig.autoContestDowntime, skyblock) &&
            ContestConfig.startAnnouncements[startKey] != windowKey &&
            PartyManager.sendPartyChat(client, ContestConfig.startMessage())) {
            ContestConfig.startAnnouncements[startKey] = windowKey
            ContestConfig.save()
        }
        if (skyblock) {
            readSidebar(client)
            announceTicket(client)
        }
        if (ContestConfig.trackContest) maybeWarn(client)
    }

    /** The contest window a time of day falls in. */
    fun windowAt(time: LocalTime): Long =
        (Math.floorMod(time.toSecondOfDay() - FIRST_START_MINUTE * 60, 86400) / WINDOW_SECONDS).toLong()

    private fun rollWindow() {
        val time = java.time.LocalDateTime.now()
        val now = time.toLocalTime()
        val shifted = Math.floorMod(
            now.toSecondOfDay() - FIRST_START_MINUTE * 60,
            86400
        )
        val key = absoluteWindow(time)

        secondsIntoWindow = shifted % WINDOW_SECONDS

        if (key == windowKey) return

        val first = windowKey == -1L

        windowKey = key
        complete = false
        warnedMinutes.clear()
        points = -1
        tier = ""

        if (!first) {
            println("SafariContest: Contest window rolled at $now")
        }
    }

    fun secondsToEnd(): Int =
        max(0, WINDOW_SECONDS - END_GAP_SECONDS - secondsIntoWindow)

    fun secondsToNextStart(): Int =
        WINDOW_SECONDS - secondsIntoWindow

    fun betweenContests(): Boolean =
        secondsToEnd() == 0

    private fun readSidebar(client: Minecraft) {
        // A ticket requires a currently observed Miria tier, never a retained row from another area.
        val match = MiriaContestFilter.miriaLines(SkyblockSidebar.lines(client)).firstNotNullOfOrNull { line ->
            TIER_ROW.find(line)?.takeIf { isTierName(it.groupValues[1]) }
        } ?: return
        val observedTier = match.groupValues[1]
        val observedPoints = parseInt(match.groupValues[2])
        if (!observeScore(observedTier, observedPoints)) return

        if (
            tier.isNotEmpty() &&
            !complete &&
            secondsIntoWindow >= STALE_GUARD_SECONDS &&
            ContestTicketState.earned(tier, ContestConfig.firstClassTicket, points)
        ) {
            complete = true
            completedWindow = windowKey
            println("SafariContest: Contest complete — $tier with $points")
        }
    }
    private fun announceTicket(client: Minecraft) {
        if (!complete || completedWindow != windowKey || betweenContests()) return
        val player = client.player ?: return
        val firstClass = ContestConfig.firstClassTicket
        val key = "${player.uuid}.${ContestConfig.ticketMode}"
        val saved = ContestConfig.ticketAnnouncements
        if (saved[key] == windowKey) return
        if (ticketNotices.claim(player.uuid.toString(), windowKey, tier, secondsIntoWindow, firstClass, points) {
                PartyManager.sendPartyChat(client, ContestConfig.ticketMessage(), requireParty = true)
            }) {
            saved.entries.removeIf { it.value < windowKey - 2 }
            saved[key] = windowKey
            runCatching { ContestConfig.save() }.onFailure {
                org.slf4j.LoggerFactory.getLogger("safari-contest/contest").warn("Could not save ticket announcement", it)
            }
        }
    }
    /** Contest points increase within a profile/window; reconnect placeholders must not replace them. */
    internal fun observeScore(observedTier: String, observedPoints: Int): Boolean {
        if (observedPoints < 0 || observedPoints < points) return false
        tier = observedTier; points = observedPoints
        return true
    }

    private fun maybeWarn(client: Minecraft) {
        if (
            complete ||
            !ContestConfig.contestWarnEnabled ||
            betweenContests() ||
            !SkyblockSidebar.inSkyblock(client)
        ) {
            return
        }

        val remaining = secondsToEnd()
        val due = ContestConfig.warningMinutes().filter { remaining <= it * 60 && it !in warnedMinutes }
        if (due.isEmpty()) return
        warnedMinutes.addAll(due)
        if (ContestConfig.contestWarnTitle) showTitle(client, remaining)
        playSound(client)
    }

    private fun playSound(client: Minecraft) {
        val id = Identifier.tryParse(ContestConfig.contestSound.trim()) ?: return
        val sound = BuiltInRegistries.SOUND_EVENT
            .getOptional(id)
            .orElse(null)
            ?: run {
                println(
                    "SafariContest: Unknown contest sound ${ContestConfig.contestSound}"
                )
                return
            }

        val wanted = max(0f, ContestConfig.contestSoundVolume / 100f)
        val copies = min(MAX_SOUND_COPIES, ceil(wanted.toDouble()).toInt())

        for (i in 0 until copies) {
            val gain = min(1f, wanted - i)

            client.soundManager.play(
                SimpleSoundInstance.forUI(sound, 1.0f, gain)
            )
        }
    }

    private fun showTitle(client: Minecraft, remaining: Int) {
        client.gui.smSetTimes(5, 60, 15)
        client.gui.smSetTitle(Component.literal("§c§lINCOMPLETE!"))
        client.gui.smSetSubtitle(
            Component.literal(
                "§e${formatRemaining(remaining)} left to reach ${if (ContestConfig.firstClassTicket) "Legendary" else "Uncommon"}"
            )
        )
    }

    /**
     * Completion belongs to the observed profile and contest window; ticket detection continues with the HUD off.
     */
    /** Warp cycles require only the Basic ticket, independently of the announcer's ticket mode. */
    fun hasUncommon(): Boolean = windowKey == absoluteWindow(java.time.LocalDateTime.now()) &&
        secondsIntoWindow >= 10 && ContestTicketState.earned(tier, false, points)

    fun isComplete(): Boolean = selectedTicketMode == ContestConfig.ticketMode &&
        completeFor(complete, completedWindow, absoluteWindow(java.time.LocalDateTime.now()))
    fun completeFor(complete: Boolean, seenWindow: Long, currentWindow: Long) = complete && seenWindow == currentWindow

    /** The contest a completion belongs to, read together with it: unique across days, null while incomplete. */
    fun completedWindow(): Long? = if (isComplete()) completedWindow else null
    fun absoluteWindow(time: java.time.LocalDateTime): Long =
        Math.floorDiv(time.toEpochSecond(java.time.ZoneOffset.UTC) - FIRST_START_MINUTE * 60, WINDOW_SECONDS.toLong())

    fun currentTier(): String = tier

    fun currentPoints(): Int = points

    fun isActive(): Boolean =
        windowKey != -1L && !betweenContests()

    fun secondsIntoWindow(): Int = secondsIntoWindow

    fun formatRemaining(seconds: Int): String =
        String.format("%d:%02d", seconds / 60, seconds % 60)

    private fun isTierName(word: String): Boolean {
        val upper = word.uppercase()
        return upper == "COMMON" || upper in ContestTicketState.earnedTiers
    }

    private fun parseInt(token: String): Int {
        var text = token.trim().lowercase().replace(",", "")

        val scale = when {
            text.endsWith("k") -> {
                text = text.dropLast(1)
                1000.0
            }
            text.endsWith("m") -> {
                text = text.dropLast(1)
                1_000_000.0
            }
            else -> 1.0
        }

        return try {
            round(text.toDouble() * scale).toInt()
        } catch (_: NumberFormatException) {
            -1
        }
    }
}
