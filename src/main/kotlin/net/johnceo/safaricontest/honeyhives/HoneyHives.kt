package net.johnceo.safaricontest.honeyhives

import net.johnceo.safaricontest.compat.*
import java.nio.file.Files
import java.util.Properties
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.johnceo.safaricontest.config.ConfigStorage
import net.johnceo.safaricontest.config.ConfigManager
import net.johnceo.safaricontest.contest.ContestTracker
import net.johnceo.safaricontest.hud.SkyblockSidebar
import net.johnceo.safaricontest.safari.SafariRules
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import org.slf4j.LoggerFactory

/** Personal honeyhive beacons, driven by confirmed loot/refill chat and loaded server holograms. */
object HoneyHives {
    private val logger = LoggerFactory.getLogger("safari-contest/honeyhives")
    private const val FULL = 0xFF55FF55.toInt()
    private val hives = linkedMapOf<String, Hive>()
    /** Scope -> hive key -> when it refills: the one thing that decides whether a hive is full. */
    private val refillAt = mutableMapOf<String, MutableMap<String, Long>>()
    private val observedRefills = mutableMapOf<String, MutableMap<String, Long>>()
    private val lootedAt = mutableMapOf<String, MutableMap<String, Long>>()
    private val queenFull = mutableMapOf<String, MutableSet<String>>()
    /** The last scope seen, saved, so loots apply straight away after a restart or a server switch. */
    private val lastScopes = mutableMapOf<String, String>()
    private var profile: String? = null
    private var scope: String? = null
    private var tracker: HoneyHiveTracker? = null
    private var visible = emptyList<Pair<Hive, Int>>()
    private var ticks = 0
    private var level: Any? = null
    private var onTorrhus = false
    private var beamsBroken = false
    private val file get() = ConfigStorage.directory.resolve("safari-contest-honeyhives.properties")

    private val lines = WorldRenderCompat.renderType("honeyhive_lines")

    fun register() {
        load()
        // Watching clicks only: neither callback changes or consumes the interaction.
        UseBlockCallback.EVENT.register { player, world, _, hit -> clicked(player, world, hit.blockPos); InteractionResult.PASS }
        AttackBlockCallback.EVENT.register { player, world, _, pos, _ -> clicked(player, world, pos); InteractionResult.PASS }
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { chat(SafariRules.strip(message.string)) }
        }
        LevelRenderEvents.COLLECT_SUBMITS.register(::beams)
        WorldRenderCompat.register(::outlines)
    }

    private fun chat(text: String) {
        HoneyHiveRules.profileId(text)?.let { profile = it; updateScope(Minecraft.getInstance()); return }
        val client = Minecraft.getInstance()
        if (!ConfigManager.honeyHives || !onTorrhus || !SkyblockSidebar.inSkyblock(client)) return
        updateScope(client)
        val now = System.currentTimeMillis()
        if (tracker?.chat(text.trim(), now) == true) {
            save()
            refreshVisible(client, now)
        } else if (text.startsWith("QUEEN BEE!")) logger.info("Unmatched Queen Bee refill: scope={} text={}", scope, text)
    }

    private fun clicked(player: Player, world: Level, pos: BlockPos) {
        val client = Minecraft.getInstance()
        if (!ConfigManager.honeyHives || !onTorrhus || !world.isClientSide || player !== client.player ||
            !world.getBlockState(pos).`is`(Blocks.BEE_NEST)) return
        updateScope(client)
        val key = "${pos.x},${pos.y},${pos.z}"
        if (key !in hives) return
        tracker?.click(key, System.currentTimeMillis())
    }

    /** Until the join's profile line arrives, track fresh loots under this player only. */
    private fun updateScope(client: Minecraft) {
        val player = client.player?.uuid?.toString()?.replace("-", "")
        val selected = HoneyHiveRules.resumeScope(player, profile, lastScopes[player])
        val next = selected ?: player?.let { "$it.unknown" }
        ContestTracker.selectScope(selected)
        if (next == scope) return
        val promote = selected != null && scope == "$player.unknown" && tracker != null
        if (!promote) tracker?.resetSession()
        scope = next
        tracker = next?.let { owner ->
            val times = refillAt.getOrPut(owner) { mutableMapOf() }
            val full = queenFull.getOrPut(owner) { mutableSetOf() }
            val loots = lootedAt.getOrPut(owner) { mutableMapOf() }
            val observations = observedRefills.getOrPut(owner) { mutableMapOf() }
            if (promote) {
                tracker!!.apply { promote(times, full, loots, observations) }.also {
                    refillAt.remove("$player.unknown"); queenFull.remove("$player.unknown"); lootedAt.remove("$player.unknown"); observedRefills.remove("$player.unknown")
                }
            } else HoneyHiveTracker(times, full, provisional = selected == null, lootedAt = loots, observedRefills = observations)
        }
        if (player != null && selected != null && lastScopes.put(player, selected) != selected) save()
    }

    fun disconnect() {
        profile = null
        tracker?.resetSession()
        scope = null
        tracker = null
        visible = emptyList()
        onTorrhus = false
    }

    fun tick(client: Minecraft) {
        // Only a disconnect forgets the profile: the player is briefly missing on every server switch too.
        if (client.connection == null) disconnect()
        if (client.level !== level) { level = client.level; onTorrhus = false; visible = emptyList(); tracker?.resetSession() }
        updateScope(client)
        if (++ticks % 20 != 0) return
        val now = System.currentTimeMillis()
        var expired = false
        refillAt.values.forEach { times -> if (times.entries.removeIf { now >= it.value }) expired = true }
        if (expired) save()
        if (client.level == null || client.player == null) return
        if (!ConfigManager.honeyHives || !SkyblockSidebar.inSkyblock(client)) { visible = emptyList(); return }
        // A decorative nest elsewhere must never establish this island's identity.
        val mode = net.johnceo.safaricontest.safari.SafariIsland.currentMode
        onTorrhus = if (mode != null) mode.equals("foraging_3", true)
            else SkyblockSidebar.areaName(client) in setOf("Torrhus Canyon", "Torrhus Springs")
        if (!onTorrhus || ContestTracker.isComplete()) {
            visible = emptyList(); return
        }
        verifyHolograms(client, now)
        refreshVisible(client, now)
    }

    private fun refreshVisible(client: Minecraft, now: Long) {
        if (client.player == null || scope?.endsWith(".unknown") != false ||
            !ConfigManager.honeyHives || !onTorrhus || ContestTracker.isComplete()) { visible = emptyList(); return }
        val refills = scope?.let { refillAt[it] }.orEmpty()
        val shown = mutableListOf<Pair<Hive, Int>>()
        for (hive in hives.values) {
            val refill = refills[hive.key]
            when (tracker?.state(hive.key, now) ?: HoneyHiveRules.state(now, refill)) {
                HiveState.FULL -> shown += hive to FULL
                HiveState.EMPTY -> Unit
            }
        }
        visible = shown
    }

    private fun verifyHolograms(client: Minecraft, now: Long) {
        val world = client.level ?: return
        val current = tracker ?: return
        val labels = world.entitiesForRendering().filter { !it.isRemoved && world.getEntity(it.id) === it }.flatMap { entity ->
            val text = if (entity is Display.TextDisplay) entity.text.string else entity.customName?.string
            text?.let { HoneyHiveLabels.statusLines(it).map { line -> HiveLabel(entity.x, entity.y, entity.z, line) } }.orEmpty()
        }
        var changed = false
        for (hive in hives.values) {
            if (current.observe(hive.key, HoneyHiveLabels.forHive(hive, hives.values, labels), now = now)) changed = true
        }
        if (changed) save()
    }

    /** Log raw label carriers as well as interpreted state to diagnose server data changes. */
    fun debug(client: Minecraft) {
        val world = client.level ?: return
        val player = client.player ?: return
        logger.info("Hive diagnostics: enabled={} onTorrhus={} profileKnown={} provisional={} visible={}",
            ConfigManager.honeyHives, onTorrhus, profile != null, profile == null, visible.size)
        hives.values.filter { player.distanceToSqr(it.x + .5, it.y + .5, it.z + .5) <= 96.0 * 96.0 }.forEach { hive ->
            val pos = BlockPos(hive.x, hive.y, hive.z)
            logger.info("Hive pos={} loaded={} block={} refillAt={} queenFull={} lootedAt={} observedRefill={} state={}", hive.key, world.hasChunkAt(pos),
                if (world.hasChunkAt(pos)) world.getBlockState(pos) else null,
                scope?.let { refillAt[it]?.get(hive.key) }, scope?.let { queenFull[it]?.contains(hive.key) },
                scope?.let { lootedAt[it]?.get(hive.key) }, scope?.let { observedRefills[it]?.get(hive.key) },
                tracker?.state(hive.key, System.currentTimeMillis()))
        }
        world.entitiesForRendering().filter { !it.isRemoved && world.getEntity(it.id) === it && player.distanceToSqr(it) <= 96.0 * 96.0 }.forEach { entity ->
            val raw = if (entity is Display.TextDisplay) entity.text.string else entity.customName?.string
            if (raw != null && (raw.contains("hive", true) || raw.contains("refill", true) || raw.contains("loot", true)))
                logger.info("Hive label carrier: id={} type={} pos={},{},{} raw={} status={}",
                    entity.id, entity.type.toShortString(), entity.x, entity.y, entity.z, raw, HoneyHiveLabels.statusLines(raw))
        }
    }

    // ------------------------------------------------------------------ drawing

    private fun beams(context: LevelRenderContext) {
        val client = Minecraft.getInstance()
        val world = client.level ?: return
        if (world !== level || !ConfigManager.honeyHives || visible.isEmpty() || beamsBroken) return
        try {
            val camera = client.gameRenderer.smCamera.position()
            val animation = Math.floorMod(world.gameTime, 40L) + client.deltaTracker.getGameTimeDeltaPartialTick(false)
            val poses = context.poseStack()
            for ((hive, color) in visible) {
                // Beams use RGB without alpha.
                val rgb = if (color ushr 24 < 0xFF) (color and 0xFEFEFE) shr 1 else color and 0xFFFFFF
                poses.pushPose()
                poses.translate(hive.x - camera.x, hive.y - camera.y, hive.z - camera.z)
                BeaconRenderer.submitBeaconBeam(poses, context.submitNodeCollector(), BeaconRenderer.BEAM_LOCATION, 1f, animation,
                    0, BeaconRenderer.MAX_RENDER_Y, rgb, BeaconRenderer.SOLID_BEAM_RADIUS, BeaconRenderer.BEAM_GLOW_RADIUS)
                poses.popPose()
            }
        } catch (e: Throwable) {
            // If a future version moves the beacon renderer, fall back to drawn columns rather than nothing.
            beamsBroken = true
            logger.warn("Beacon beams unavailable, drawing columns instead", e)
        }
    }

    private fun outlines(context: LevelRenderContext) {
        val client = Minecraft.getInstance()
        if (client.level == null || client.level !== level || !ConfigManager.honeyHives || client.options.smHideGui || visible.isEmpty()) return
        fun box(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double, color: Int) =
            WorldRenderCompat.box(context, lines, net.minecraft.world.phys.AABB(minX, minY, minZ, maxX, maxY, maxZ), color)
        for ((hive, color) in visible) {
            box(hive.x.toDouble(), hive.y.toDouble(), hive.z.toDouble(), hive.x + 1.0, hive.y + 1.0, hive.z + 1.0, color)
            if (beamsBroken) box(hive.x + .35, hive.y + 1.0, hive.z + .35, hive.x + .65, hive.y + 64.0, hive.z + .65, color)
        }
        WorldRenderCompat.flush(context, lines)
    }

    // ------------------------------------------------------------------ storage

    private fun load() {
        hives.clear()
        HoneyHiveRules.catalog.forEach { hives[it.key] = it.copy() }
        if (!Files.exists(file)) return
        val saved = Properties().apply { Files.newInputStream(file).use(::load) }
        if (saved.stringPropertyNames().any { it.startsWith("full.") && !saved.containsKey("queenFull." + it.removePrefix("full.")) }) {
            runCatching { Files.copy(file, file.resolveSibling(file.fileName.toString() + ".pre-timer-priority.bak"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
                .onFailure { logger.warn("Couldn't back up old hive readiness", it) }
        }
        if (HoneyHiveRules.removeUnknownPositions(saved) > 0) {
            runCatching { Files.copy(file, file.resolveSibling(file.fileName.toString() + ".pre-waypoint-cleanup.bak"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
                .onFailure { logger.warn("Couldn't back up old hive positions", it) }
        }
        val now = System.currentTimeMillis()
        // Preserve version-2 personal deadlines across upgrades and relogs.
        HoneyHiveRules.savedRefills(saved, now).forEach { (owner, times) -> refillAt[owner] = times.toMutableMap() }
        HoneyHiveRules.savedQueenFull(saved).forEach { (owner, keys) -> queenFull[owner] = keys.toMutableSet() }
        // Only the bundled catalogue defines locations; saved positions never expand it.
        saved.stringPropertyNames().forEach { name ->
            HoneyHiveRules.parseLoot(name, saved.getProperty(name), "observedRefill")?.let { (owner, key, at) ->
                observedRefills.getOrPut(owner) { mutableMapOf() }[key] = at
            }
            HoneyHiveRules.parseLoot(name, saved.getProperty(name), "looted")?.let { (owner, key, at) ->
                lootedAt.getOrPut(owner) { mutableMapOf() }[key] = at
            }
        }
        saved.getProperty("scope.last")?.let { lastScopes[it.substringBefore('.')] = it }
        saved.stringPropertyNames().filter { it.startsWith("scope.last.") }.forEach { key ->
            val player = key.removePrefix("scope.last.")
            HoneyHiveRules.resumeScope(player, null, saved.getProperty(key))?.let { lastScopes[player] = it }
        }
        // Legacy files stored only one last scope. A single saved profile for another
        // account is safe to resume while waiting for that account's profile line.
        (refillAt.keys + lootedAt.keys + observedRefills.keys + queenFull.keys)
            .filterNot { it.endsWith(".unknown") }.groupBy { it.substringBefore('.') }
            .forEach { (player, owners) -> owners.singleOrNull()?.let { lastScopes.putIfAbsent(player, it) } }
        save()
    }

    private fun save() {
        val out = Properties()
        out.setProperty("tracking.version", "2")
        hives.values.forEach { out.setProperty("hive.${it.key}", it.id ?: "-") }
        refillAt.forEach { (owner, times) -> times.forEach { (key, at) -> out.setProperty("refill.$owner.$key", at.toString()) } }
        observedRefills.forEach { (owner, times) -> times.forEach { (key, at) -> out.setProperty("observedRefill.$owner.$key", at.toString()) } }
        val now = System.currentTimeMillis()
        lootedAt.forEach { (owner, times) -> times.forEach { (key, at) ->
            out.setProperty("looted.$owner.$key", at.toString())
            val current = (if (owner == scope) tracker?.state(key, now) else null)
                ?: HoneyHiveRules.state(now, refillAt[owner]?.get(key))
            out.setProperty("state.$owner.$key", current.name.lowercase())
        } }
        queenFull.forEach { (owner, keys) -> keys.forEach { key -> out.setProperty("queenFull.$owner.$key", "1") } }
        lastScopes.forEach { (player, owner) -> out.setProperty("scope.last.$player", owner) }
        runCatching { Files.createDirectories(file.parent); Files.newOutputStream(file).use { out.store(it, "SafariContest honeyhives") } }
            .onFailure { logger.warn("Couldn't save honeyhives", it) }
    }
}
