package net.johnceo.safaricontest.config

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

object ContestConfig {
    var showContestTimer = true
    var trackContest = true
    var autoContestDowntime = true
    var honeyHives = true
    var ticketMode = 0
    val firstClassTicket get() = ticketMode == 1
    var ticketChoice = "r"
    var customTicket = ""
    var startChoice = "!dt contest"
    var customStart = ""
    var contestWarnEnabled = true
    var contestWarnTitle = true
    var contestSound = "minecraft:block.bell.use"
    var contestSoundVolume = 100
    var hudX = 10
    var hudY = 10
    val warnings = linkedSetOf(5, 3, 1)
    val ticketAnnouncements = mutableMapOf<String, Long>()
    val startAnnouncements = mutableMapOf<String, Long>()
    val ticketChoices = listOf("r", "!m7", "!t5", "other")
    val startChoices = listOf("!dt contest", "other")
    fun ticketMessage() = PartyMessage.resolve(ticketChoice, customTicket, ticketChoices, "r")
    fun startMessage() = PartyMessage.resolve(startChoice, customStart, startChoices, "!dt contest")
    fun warningMinutes() = if (contestWarnEnabled) warnings.sortedDescending() else emptyList()
    private val file get() = ConfigStorage.directory.resolve("contest.properties")

    fun load() {
        val p = Properties()
        if (Files.exists(file)) Files.newInputStream(file).use(p::load)
        fun bool(key: String, default: Boolean) = p.getProperty(key)?.toBooleanStrictOrNull() ?: default
        showContestTimer = bool("showContestTimer", true)
        trackContest = bool("trackContest", true)
        autoContestDowntime = bool("autoContestDowntime", true)
        honeyHives = bool("honeyHives", true)
        contestWarnEnabled = bool("contestWarnEnabled", true)
        contestWarnTitle = bool("contestWarnTitle", true)
        ticketMode = p.getProperty("ticketMode")?.toIntOrNull()?.coerceIn(0, 1) ?: 0
        ticketChoice = p.getProperty("ticketChoice")?.takeIf { it in ticketChoices } ?: "r"
        startChoice = p.getProperty("startChoice")?.takeIf { it in startChoices } ?: "!dt contest"
        customTicket = p.getProperty("customTicket", "")
        customStart = p.getProperty("customStart", "")
        hudX = p.getProperty("hudX")?.toIntOrNull()?.coerceAtLeast(0) ?: 10
        hudY = p.getProperty("hudY")?.toIntOrNull()?.coerceAtLeast(0) ?: 10
        contestSoundVolume = p.getProperty("contestSoundVolume")?.toIntOrNull()?.coerceIn(0, 200) ?: 100
        contestSound = p.getProperty("contestSound", "minecraft:block.bell.use")
        warnings.clear()
        warnings.addAll(p.getProperty("warnings", "5,3,1").split(',').mapNotNull(String::toIntOrNull).filter { it in setOf(5, 3, 1) })
        ticketAnnouncements.clear(); startAnnouncements.clear()
        p.stringPropertyNames().forEach { key ->
            p.getProperty(key).toLongOrNull()?.let { value ->
                if (key.startsWith("ticketAnnounced.")) ticketAnnouncements[key.removePrefix("ticketAnnounced.")] = value
                if (key.startsWith("startAnnounced.")) startAnnouncements[key.removePrefix("startAnnounced.")] = value
            }
        }
    }

    fun save() {
        val p = Properties()
        mapOf("showContestTimer" to showContestTimer, "trackContest" to trackContest,
            "autoContestDowntime" to autoContestDowntime, "honeyHives" to honeyHives,
            "contestWarnEnabled" to contestWarnEnabled, "contestWarnTitle" to contestWarnTitle,
            "ticketMode" to ticketMode, "ticketChoice" to ticketChoice, "customTicket" to customTicket,
            "startChoice" to startChoice, "customStart" to customStart, "warnings" to warnings.joinToString(","),
            "hudX" to hudX, "hudY" to hudY, "contestSoundVolume" to contestSoundVolume,
            "contestSound" to contestSound).forEach { (k,v) -> p.setProperty(k, v.toString()) }
        val window = net.johnceo.safaricontest.contest.ContestTracker.absoluteWindow(java.time.LocalDateTime.now())
        ticketAnnouncements.entries.removeIf { it.value < window - 2 }
        startAnnouncements.entries.removeIf { it.value < window - 2 }
        ticketAnnouncements.forEach { (k,v) -> p.setProperty("ticketAnnounced.$k", v.toString()) }
        startAnnouncements.forEach { (k,v) -> p.setProperty("startAnnounced.$k", v.toString()) }
        Files.createDirectories(file.parent)
        val temp = Files.createTempFile(file.parent, "contest-", ".tmp")
        try {
            Files.newOutputStream(temp).use { p.store(it, "SafariContest settings") }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
        } finally { Files.deleteIfExists(temp) }
    }
}

/** All user input remains the body of /pc, including a leading slash. */
object PartyMessage {
    const val MAX_LENGTH = 240
    fun sanitize(value: String) = value.filter { !it.isISOControl() && it != '§' }.trim().take(MAX_LENGTH)
    fun resolve(choice: String, custom: String, choices: List<String>, default: String): String =
        sanitize(if (choice == "other") custom else choice.takeIf { it in choices } ?: default).ifBlank { default }
}
