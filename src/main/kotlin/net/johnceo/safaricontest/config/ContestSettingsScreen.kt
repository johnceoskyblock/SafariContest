package net.johnceo.safaricontest.config

import net.johnceo.safaricontest.compat.*
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class ContestSettingsScreen : Screen(Component.literal("SafariContest")) {
    private var displayPage = false
    private var popup = false
    private val left get() = (width - panelWidth) / 2
    private val panelWidth get() = (width - 24).coerceIn(200, 400)
    private val top get() = ((height - 240) / 2).coerceAtLeast(0)
    private fun button(label: String, x: Int, y: Int, w: Int, action: () -> Unit): Button =
        addRenderableWidget(Button.builder(Component.literal(label)) { action() }.bounds(x, y, w, 20).build())
    private fun persist() { ContestConfig.save() }
    override fun init() {
        popup = false
        button(if (displayPage) "Party messages" else "Display & alerts", left, top+23, panelWidth) {
            displayPage = !displayPage; rebuildWidgets()
        }
        if (displayPage) displayControls() else messageControls()
        button("Done", left, top+216, panelWidth) { onClose() }
    }
    private fun dropdown(label: String, value: String, options: List<String>, y: Int, select: (String) -> Unit) {
        button("$label: $value  ▼", left, y, panelWidth) {
            popup = true
            children().filterIsInstance<AbstractWidget>().forEach { it.active = false; it.visible = false }
            val popupY = y.coerceAtMost(top+210 - (options.size+1)*22)
            options.forEachIndexed { i, option ->
                button(option, left+12, popupY+i*22, panelWidth-24) {
                    select(option); persist(); rebuildWidgets()
                }
            }
            button("Cancel", left+12, popupY+options.size*22, panelWidth-24) { rebuildWidgets() }
        }
    }
    private fun custom(value: String, enabled: Boolean, y: Int, update: (String) -> Unit) {
        val edit = EditBox(font, left, y, panelWidth, 20, Component.literal("Custom party message"))
        edit.setMaxLength(PartyMessage.MAX_LENGTH)
        edit.value = value
        edit.setHint(Component.literal("Type your custom party message"))
        edit.setEditable(enabled)
        edit.active = enabled
        edit.setResponder { update(it); persist() }
        addRenderableWidget(edit)
    }
    private fun messageControls() {
        dropdown("Ticket earned", ContestConfig.ticketChoice, ContestConfig.ticketChoices, top+55) { ContestConfig.ticketChoice = it }
        custom(ContestConfig.customTicket, ContestConfig.ticketChoice == "other", top+79) { ContestConfig.customTicket = it }
        dropdown("Contest start", ContestConfig.startChoice, ContestConfig.startChoices, top+120) { ContestConfig.startChoice = it }
        custom(ContestConfig.customStart, ContestConfig.startChoice == "other", top+144) { ContestConfig.customStart = it }
        button("Auto contest start: ${onOff(ContestConfig.autoContestDowntime)}", left, top+181, panelWidth) {
            ContestConfig.autoContestDowntime = !ContestConfig.autoContestDowntime; persist(); rebuildWidgets()
        }
    }
    private fun onOff(value: Boolean) = if (value) "ON" else "OFF"
    private fun displayControls() {
        val half = (panelWidth-6)/2
        button("Contest HUD: ${onOff(ContestConfig.showContestTimer)}", left, top+49, half) {
            ContestConfig.showContestTimer = !ContestConfig.showContestTimer; persist(); rebuildWidgets()
        }
        button("Hives: ${onOff(ContestConfig.honeyHives)}", left+half+6, top+49, half) {
            ContestConfig.honeyHives = !ContestConfig.honeyHives; persist(); rebuildWidgets()
        }
        dropdown("Ticket target", if (ContestConfig.firstClassTicket) "First-Class" else "Basic", listOf("Basic", "First-Class"), top+74) {
            ContestConfig.ticketMode = if (it == "First-Class") 1 else 0
        }
        for ((i, minute) in listOf(5,3,1).withIndex()) {
            button("${minute}m: ${onOff(minute in ContestConfig.warnings)}", left+i*(panelWidth/3), top+100, panelWidth/3-3) {
                if (!ContestConfig.warnings.add(minute)) ContestConfig.warnings.remove(minute)
                persist(); rebuildWidgets()
            }
        }
        button("Warning title: ${onOff(ContestConfig.contestWarnTitle)}", left, top+126, half) {
            ContestConfig.contestWarnTitle = !ContestConfig.contestWarnTitle; persist(); rebuildWidgets()
        }
        button("Sound: ${onOff(ContestConfig.contestSoundVolume > 0)}", left+half+6, top+126, half) {
            ContestConfig.contestSoundVolume = if (ContestConfig.contestSoundVolume > 0) 0 else 100; persist(); rebuildWidgets()
        }
        for ((i, axis) in listOf("X", "Y").withIndex()) {
            val x = left+i*(half+6)
            button("HUD $axis -", x, top+154, half/2-2) {
                if (i == 0) ContestConfig.hudX = (ContestConfig.hudX-10).coerceAtLeast(0)
                else ContestConfig.hudY = (ContestConfig.hudY-10).coerceAtLeast(0)
                persist()
            }
            button("HUD $axis +", x+half/2+2, top+154, half/2-2) {
                if (i == 0) ContestConfig.hudX = (ContestConfig.hudX+10).coerceAtMost((width-190).coerceAtLeast(0))
                else ContestConfig.hudY = (ContestConfig.hudY+10).coerceAtMost((height-68).coerceAtLeast(0))
                persist()
            }
        }
        button("Reset HUD position", left, top+181, panelWidth) {
            ContestConfig.hudX = 10; ContestConfig.hudY = 10; persist()
        }
    }
    override fun extractRenderState(g: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        g.fill(0, 0, width, height, 0xEE101A17.toInt())
        g.centeredText(font, title, width/2, top+7, 0xFFFFCC66.toInt())
        if (!displayPage) {
            g.text(font, "Sent to party chat when your ticket is earned", left, top+45, 0xFFBBBBBB.toInt(), false)
            g.text(font, "Every new contest, anywhere in SkyBlock", left, top+110, 0xFFBBBBBB.toInt(), false)
            g.text(font, "Empty custom messages use the default.", left, top+169, 0xFFBBBBBB.toInt(), false)
        }
        if (popup) g.fill(left, top+44, left+panelWidth, top+211, 0xE0101A17.toInt())
        super.extractRenderState(g, mouseX, mouseY, delta)
    }
    override fun onClose() { persist(); minecraft.smSetScreen(null) }
    override fun isPauseScreen() = false
}
