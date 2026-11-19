package net.johnceo.safaricontest.contest

import com.mojang.blaze3d.platform.InputConstants
import net.johnceo.safaricontest.compat.*
import net.johnceo.safaricontest.config.*
import net.johnceo.safaricontest.config.ThemedButton as Button
import net.johnceo.safaricontest.hud.HudSnapping
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component

/** Same placement, resize, snap and keyboard interactions as SparklingMutuals' HUD editor. */
class ContestGui(private val parent: Screen? = null) : Screen(Component.literal("SafariContest HUDs")) {
    private val layout get() = ContestConfig.layout
    private var dragging = false
    private var selected = false
    private var offsetX = 0.0
    private var offsetY = 0.0
    private var guideX: Int? = null
    private var guideY: Int? = null
    private var previewEarned = true
    override fun init() {
        ContestConfig.editLayout(width, height)
        dragging = false
        val w = minOf(110, (width-30)/3)
        val start = (width-w*3-12)/2
        addRenderableWidget(Button.builder(Component.literal("Reset position")) {
            ContestConfig.resetLayout(); save()
        }.bounds(start,height-24,w,20).build())
        addRenderableWidget(Button.builder(Component.literal(snapLabel())) {
            ContestConfig.hudSnapping = !ContestConfig.hudSnapping; it.message = Component.literal(snapLabel()); save()
        }.bounds(start+w+6,height-24,w,20).build())
        addRenderableWidget(Button.builder(Component.literal("Done")) { onClose() }
            .bounds(start+(w+6)*2,height-24,w,20).build())
        addRenderableWidget(Button.builder(Component.literal("Preview: ticket earned / incomplete")) {
            previewEarned = !previewEarned
        }.bounds((width-210)/2,8,210,20).build())
    }
    private fun snapLabel() = "Snap: " + if (ContestConfig.hudSnapping) "On" else "Off"
    private fun bounds() = layout.bounds(width,height,ContestHud.width,ContestHud.HEIGHT)
    override fun extractRenderState(g: GuiGraphicsExtractor, mx: Int, my: Int, delta: Float) {
        g.fill(0,0,width,height,0x88000000.toInt() or SafariTheme.BACKGROUND)
        val b = bounds()
        val hover = b.contains(mx.toDouble(),my.toDouble())
        if (hover) selected = true
        ContestHud.renderAt(g,b.x,b.y,previewEarned)
        if (hover || dragging) {
            val color = SettingsPalette.gold
            g.fill(b.x-1,b.y-1,b.x+b.width+1,b.y,color); g.fill(b.x-1,b.y+b.height,b.x+b.width+1,b.y+b.height+1,color)
            g.fill(b.x-1,b.y,b.x,b.y+b.height,color); g.fill(b.x+b.width,b.y,b.x+b.width+1,b.y+b.height,color)
            g.text(font,"Miria contest · ${(layout.scale*100).toInt()}%",b.x.coerceAtLeast(4),(b.y-11).coerceAtLeast(0),color,true)
        }
        if (dragging && ContestConfig.hudSnapping) {
            guideX?.let { g.fill(it,0,it+1,height-52,SettingsPalette.accent) }
            guideY?.let { g.fill(0,it,width,it+1,SettingsPalette.accent) }
        }
        val lines = font.split(Component.literal("Drag to move · Arrows to nudge · Scroll to resize · Right-click for settings · Esc to save"),width-16)
        lines.forEachIndexed { i,line -> g.text(font,line,(width-font.width(line))/2,height-30-lines.size*10+i*10,-1,true) }
        super.extractRenderState(g,mx,my,delta)
    }
    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (super.mouseClicked(event,doubleClick)) return true
        val b = bounds()
        if (!b.contains(event.x(),event.y())) return false
        selected = true
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            dragging = false; save(); minecraft.smSetScreen(ContestSettingsScreen(2,this)); return true
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false
        dragging = true; offsetX = event.x()-b.x; offsetY = event.y()-b.y
        return true
    }
    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        if (!dragging || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseDragged(event,dx,dy)
        val b = bounds(); val x = event.x()-offsetX; val y = event.y()-offsetY
        val snap = if (ContestConfig.hudSnapping) HudSnapping.position(x,y,width,height,b.width,b.height) else null
        guideX = snap?.guideX ?: (width/2).takeIf { snap?.centeredX == true }
        guideY = snap?.guideY ?: (height/2).takeIf { snap?.centeredY == true }
        layout.move(snap?.x?.toDouble() ?: x,snap?.y?.toDouble() ?: y,width,height,ContestHud.width,ContestHud.HEIGHT)
        return true
    }
    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (dragging && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            dragging = false; guideX = null; guideY = null; save(); return true
        }
        return super.mouseReleased(event)
    }
    override fun mouseScrolled(mx: Double, my: Double, horizontal: Double, vertical: Double): Boolean {
        if (!bounds().contains(mx,my) || vertical == 0.0) return super.mouseScrolled(mx,my,horizontal,vertical)
        layout.markPositioned(); layout.scale += if (vertical > 0) .1f else -.1f; save(); return true
    }
    override fun keyPressed(event: KeyEvent): Boolean {
        if (!selected) return super.keyPressed(event)
        val (dx,dy) = when(event.key()) {
            InputConstants.KEY_LEFT -> -1 to 0; InputConstants.KEY_RIGHT -> 1 to 0
            InputConstants.KEY_UP -> 0 to -1; InputConstants.KEY_DOWN -> 0 to 1
            else -> return super.keyPressed(event)
        }
        val b = bounds(); layout.move((b.x+dx).toDouble(),(b.y+dy).toDouble(),width,height,ContestHud.width,ContestHud.HEIGHT)
        save(); return true
    }
    private fun save() { ContestConfig.save() }
    override fun onClose() { save(); minecraft.smSetScreen(parent) }
    override fun isPauseScreen() = false
}
