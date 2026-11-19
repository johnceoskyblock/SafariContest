package net.johnceo.safaricontest.hud

import java.util.Properties
import kotlin.math.ceil
import kotlin.math.roundToInt

data class HudBounds(val x: Int, val y: Int, val width: Int, val height: Int) {
    fun contains(mx: Double, my: Double) = mx >= x && mx < x + width && my >= y && my < y + height
}

enum class HudExpansion(val horizontal: Float,val vertical: Float) {
    DOWN_RIGHT(0f,0f), DOWN(.5f,0f), DOWN_LEFT(1f,0f), RIGHT(0f,.5f),
    CENTER(.5f,.5f), LEFT(1f,.5f), UP_RIGHT(0f,1f), UP(.5f,1f), UP_LEFT(1f,1f)
}

/** Normalized positions keep panels on screen when Minecraft's GUI resolution changes. */
class HudLayout(private val defaultX: Float, private val defaultY: Float, private val defaultScale: Float = 1f,
    private val defaultReferenceWidth: Float=0f, private val defaultReferenceHeight: Float=0f) {
    var hasSavedPosition = false
        private set
    var x = defaultX
    var y = defaultY
    var scale = defaultScale
        set(value) { field = if (value.isFinite()) value.coerceIn(.15f, 2f) else defaultScale }
    private var basePanelWidth=0
    private var basePanelHeight=0
    private var lastGeometry: IntArray?=null
    var expansion=HudExpansion.DOWN_RIGHT
        set(value) {
            if(field==value)return
            val geometry=lastGeometry
            val previous=geometry?.let {bounds(it[0],it[1],it[2],it[3])}
            field=value
            if(geometry!=null && previous!=null)move(previous.x.toDouble(),previous.y.toDouble(),geometry[0],geometry[1],geometry[2],geometry[3])
        }
    private var referenceWidth = defaultReferenceWidth
    private var referenceHeight = defaultReferenceHeight
    fun effectiveScale(screenWidth: Int, screenHeight: Int, panelWidth: Int, panelHeight: Int): Float {
        if (referenceWidth <= 0 || referenceHeight <= 0) return scale
        val adapted = scale * minOf(screenWidth / referenceWidth, screenHeight / referenceHeight)
        return minOf(adapted, screenWidth.coerceAtLeast(1).toFloat() / panelWidth.coerceAtLeast(1),
            screenHeight.coerceAtLeast(1).toFloat() / panelHeight.coerceAtLeast(1))
    }
    fun bounds(screenWidth: Int, screenHeight: Int, panelWidth: Int, panelHeight: Int): HudBounds {
        if(basePanelWidth<=0)basePanelWidth=panelWidth.coerceAtLeast(1)
        if(basePanelHeight<=0)basePanelHeight=panelHeight.coerceAtLeast(1)
        lastGeometry=intArrayOf(screenWidth,screenHeight,panelWidth,panelHeight)
        val fittedScale = effectiveScale(screenWidth, screenHeight, panelWidth, panelHeight)
        val w = ceil(panelWidth * fittedScale).toInt()
        val h = ceil(panelHeight * fittedScale).toInt()
        return HudBounds((x * screenWidth-(panelWidth-basePanelWidth)*fittedScale*expansion.horizontal).roundToInt().coerceIn(0, (screenWidth - w).coerceAtLeast(0)),
            (y * screenHeight-(panelHeight-basePanelHeight)*fittedScale*expansion.vertical).roundToInt().coerceIn(0, (screenHeight - h).coerceAtLeast(0)), w, h)
    }
    fun move(px: Double, py: Double, screenWidth: Int, screenHeight: Int, panelWidth: Int, panelHeight: Int) {
        hasSavedPosition = true
        val bounds = bounds(screenWidth, screenHeight, panelWidth, panelHeight)
        val fitted=effectiveScale(screenWidth,screenHeight,panelWidth,panelHeight)
        x = (px.coerceIn(0.0, (screenWidth - bounds.width).coerceAtLeast(0).toDouble()).toFloat()+(panelWidth-basePanelWidth)*fitted*expansion.horizontal) / screenWidth.coerceAtLeast(1)
        y = (py.coerceIn(0.0, (screenHeight - bounds.height).coerceAtLeast(0).toDouble()).toFloat()+(panelHeight-basePanelHeight)*fitted*expansion.vertical) / screenHeight.coerceAtLeast(1)
    }
    fun load(properties: Properties, id: String) {
        fun read(key: String, default: Float) = properties.getProperty("hud.$id.$key")?.toFloatOrNull()?.takeIf(Float::isFinite) ?: default
        lastGeometry=null
        expansion=runCatching {HudExpansion.valueOf(properties.getProperty("hud.$id.expansion",if(id=="banner") "DOWN" else "DOWN_RIGHT"))}.getOrDefault(HudExpansion.DOWN_RIGHT)
        basePanelWidth=properties.getProperty("hud.$id.basePanelWidth")?.toIntOrNull()?.coerceIn(0,4096) ?: 0
        basePanelHeight=properties.getProperty("hud.$id.basePanelHeight")?.toIntOrNull()?.coerceIn(0,4096) ?: 0
        hasSavedPosition = properties.containsKey("hud.$id.x")
        x = read("x", defaultX).coerceIn(-2f, 2f); y = read("y", defaultY).coerceIn(-2f, 2f); scale = read("scale", defaultScale)
        referenceWidth = read("referenceWidth", defaultReferenceWidth).coerceAtLeast(0f)
        referenceHeight = read("referenceHeight", defaultReferenceHeight).coerceAtLeast(0f)
    }
    fun markPositioned() { hasSavedPosition=true }
    fun save(properties: Properties, id: String) {
        if(id in setOf("shard_guidance","hunting_box_value") && !hasSavedPosition)return
        properties.setProperty("hud.$id.x", x.toString()); properties.setProperty("hud.$id.y", y.toString())
        properties.setProperty("hud.$id.scale", scale.toString())
        properties.setProperty("hud.$id.expansion",expansion.name)
        if(basePanelWidth>0 && basePanelHeight>0) {
            properties.setProperty("hud.$id.basePanelWidth",basePanelWidth.toString())
            properties.setProperty("hud.$id.basePanelHeight",basePanelHeight.toString())
        }
        if (referenceWidth > 0 && referenceHeight > 0) {
            properties.setProperty("hud.$id.referenceWidth", referenceWidth.toString())
            properties.setProperty("hud.$id.referenceHeight", referenceHeight.toString())
        }
    }
    fun reset() { lastGeometry=null; basePanelWidth=0;basePanelHeight=0;expansion=HudExpansion.DOWN_RIGHT;hasSavedPosition=false; x = defaultX; y = defaultY; scale = defaultScale }
}
