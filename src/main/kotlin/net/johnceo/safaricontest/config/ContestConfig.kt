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
    val layout = net.johnceo.safaricontest.hud.HudLayout(.015f, .025f)
    private var legacyLayout = true
    fun layoutFor(width: Int, height: Int) = layout.also {
        if (legacyLayout) {
            it.x = hudX.toFloat() / width.coerceAtLeast(1)
            it.y = hudY.toFloat() / height.coerceAtLeast(1)
        }
    }
    fun editLayout(width: Int, height: Int) { layoutFor(width, height); legacyLayout = false }
    fun resetLayout() { layout.reset(); legacyLayout = false }
    var hudSnapping = true
    var settingsTheme = 0
    var hudBorder = true
    var hudBackground = 0x0D0D10
    var hudBorderColor = 0x605238
    var hudTitleColor = 0xE6C878
    var hudTransparency = 20
    val warnings = linkedSetOf(5, 3, 1)
    val ticketAnnouncements = mutableMapOf<String, Long>()
    val startAnnouncements = mutableMapOf<String, Long>()
    val ticketRequests = mutableMapOf<String, MutableSet<String>>()
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
        legacyLayout = !p.containsKey("hud.contest.x")
        layout.load(p, "contest")
        hudSnapping = bool("hudSnapping", true)
        settingsTheme = p.getProperty("settingsTheme")?.toIntOrNull()?.coerceIn(SettingsPalette.names.indices) ?: 0
        hudBorder = bool("hudBorder", true)
        hudBackground = readColor(p.getProperty("hudBackground"), 0x0D0D10)
        hudBorderColor = readColor(p.getProperty("hudBorderColor"), 0x605238)
        hudTitleColor = readColor(p.getProperty("hudTitleColor"), 0xE6C878)
        hudTransparency = p.getProperty("hudTransparency")?.toIntOrNull()?.coerceIn(0,100) ?: 20
        contestSoundVolume = p.getProperty("contestSoundVolume")?.toIntOrNull()?.coerceIn(0, 200) ?: 100
        contestSound = p.getProperty("contestSound", "minecraft:block.bell.use")
        warnings.clear()
        warnings.addAll(p.getProperty("warnings", "5,3,1").split(',').mapNotNull(String::toIntOrNull).filter { it in setOf(5, 3, 1) })
        ticketAnnouncements.clear(); startAnnouncements.clear(); ticketRequests.clear()
        p.stringPropertyNames().forEach { key ->
            if (key.startsWith("ticketRequest.")) {
                ticketRequests[key.removePrefix("ticketRequest.")] = p.getProperty(key).split(',')
                    .filter { it.matches(Regex("[A-Za-z0-9_]{1,16}")) }.mapTo(linkedSetOf()) { it.lowercase() }
            }
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
        mapOf("hudSnapping" to hudSnapping, "settingsTheme" to settingsTheme, "hudBorder" to hudBorder,
            "hudBackground" to hex(hudBackground), "hudBorderColor" to hex(hudBorderColor),
            "hudTitleColor" to hex(hudTitleColor), "hudTransparency" to hudTransparency)
            .forEach { (k,v) -> p.setProperty(k, v.toString()) }
        if (!legacyLayout) layout.save(p, "contest")
        val window = net.johnceo.safaricontest.contest.ContestTracker.absoluteWindow(java.time.LocalDateTime.now())
        ticketAnnouncements.entries.removeIf { it.value < window - 2 }
        startAnnouncements.entries.removeIf { it.value < window - 2 }
        ticketRequests.entries.removeIf { (it.key.substringAfterLast('.').toLongOrNull() ?: -1L) < window || it.value.isEmpty() }
        ticketAnnouncements.forEach { (k,v) -> p.setProperty("ticketAnnounced.$k", v.toString()) }
        startAnnouncements.forEach { (k,v) -> p.setProperty("startAnnounced.$k", v.toString()) }
        ticketRequests.forEach { (k,v) -> p.setProperty("ticketRequest.$k", v.joinToString(",")) }
        Files.createDirectories(file.parent)
        val temp = Files.createTempFile(file.parent, "contest-", ".tmp")
        try {
            Files.newOutputStream(temp).use { p.store(it, "SafariContest settings") }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
        } finally { Files.deleteIfExists(temp) }
    }
    fun parseColor(value: String): Int? = value.trim().removePrefix("#").takeIf { it.matches(Regex("[0-9a-fA-F]{6}")) }?.toIntOrNull(16)
    private fun readColor(value: String?, fallback: Int) = value?.let(::parseColor) ?: fallback
    fun hex(value: Int) = "#%06X".format(value and 0xFFFFFF)
}

/** All user input remains the body of /pc, including a leading slash. */
object PartyMessage {
    const val MAX_LENGTH = 240
    fun sanitize(value: String) = value.filter { !it.isISOControl() && it != '§' }.trim().take(MAX_LENGTH)
    fun resolve(choice: String, custom: String, choices: List<String>, default: String): String =
        sanitize(if (choice == "other") custom else choice.takeIf { it in choices } ?: default).ifBlank { default }
}
