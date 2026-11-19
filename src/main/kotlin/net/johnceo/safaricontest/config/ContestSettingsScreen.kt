package net.johnceo.safaricontest.config

import net.johnceo.safaricontest.compat.*
import net.johnceo.safaricontest.contest.ContestGui
import net.johnceo.safaricontest.config.ThemedButton as Button
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/** SparklingMutuals workspace surfaces, search, sidebar navigation and compact setting cards. */
class ContestSettingsScreen(private var page: Int = 0, private val returnTo: Screen? = null) : Screen(Component.literal("SafariContest")) {
    private data class Row(val page: Int, val name: String, val description: String, val control: (Int,Int,Int) -> AbstractWidget)
    private data class Card(val row: Row, val y: Int, val height: Int, val textWidth: Int)
    private val rows = mutableListOf<Row>()
    private val cards = mutableListOf<Card>()
    private val rowWidgets = mutableListOf<AbstractWidget>()
    private lateinit var search: EditBox
    private var query = ""
    private var scroll = 0
    private var maxScroll = 0
    private val pages = listOf("Contest", "Party", "HUDs")
    private val nav get() = if (width < 450) 88 else 120
    private val left get() = nav + 12
    private val contentWidth get() = (width-left-12).coerceAtLeast(120)
    private val top get() = 86
    private val bottom get() = height-34
    private fun save() = ContestConfig.save()
    private fun button(label: String, x: Int, y: Int, w: Int, action: () -> Unit) =
        Button.builder(Component.literal(label)) { action() }.bounds(x,y,w,20).build()
    private fun refresh() { save(); rebuildRows() }
    private fun toggle(page: Int, name: String, desc: String, read: () -> Boolean, write: (Boolean) -> Unit) {
        rows += Row(page,name,desc) { x,y,w -> button(if (read()) "Enabled" else "Disabled",x,y,w) { write(!read()); refresh() } }
    }
    private fun choice(page: Int, name: String, desc: String, options: List<String>, read: () -> String, write: (String) -> Unit) {
        rows += Row(page,name,desc) { x,y,w -> button(read()+"  ▾",x,y,w) {
            minecraft.smSetScreen(ChoiceScreen(this,name,options,read()) { write(it); save() })
        } }
    }
    private fun textInput(page: Int, name: String, desc: String, read: () -> String, write: (String) -> Unit) {
        rows += Row(page,name,desc) { x,y,w -> EditBox(font,x,y,w,20,Component.literal(name)).also {
            it.setMaxLength(PartyMessage.MAX_LENGTH); it.value = read()
            it.setResponder { value -> write(value); save() }
        } }
    }
    private fun action(page: Int, name: String, desc: String, label: String, run: () -> Unit) {
        rows += Row(page,name,desc) { x,y,w -> button(label,x,y,w,run) }
    }
    private fun catalog() {
        rows.clear()
        choice(0,"Ticket target","Basic needs Uncommon. First-Class needs Legendary or 5,000 points.",listOf("Basic","First-Class"),
            { if (ContestConfig.firstClassTicket) "First-Class" else "Basic" }, { ContestConfig.ticketMode = if (it == "First-Class") 1 else 0 })
        toggle(0,"Honeyhive beacons","Show ready personal hives in Torrhus until your selected ticket is earned.",
            { ContestConfig.honeyHives }, { ContestConfig.honeyHives = it })
        toggle(0,"Contest warnings","Warn while your selected ticket is incomplete.",{ ContestConfig.contestWarnEnabled },{ ContestConfig.contestWarnEnabled = it })
        for (minute in listOf(5,3,1)) toggle(0,"$minute minute warning","Warn once at this remaining-time threshold.",
            { minute in ContestConfig.warnings }, { if (it) ContestConfig.warnings.add(minute) else ContestConfig.warnings.remove(minute) })
        toggle(0,"Warning title","Show an incomplete-contest title.",{ ContestConfig.contestWarnTitle },{ ContestConfig.contestWarnTitle = it })
        choice(0,"Warning volume","Set the local warning sound volume.",listOf("0%","25%","50%","75%","100%","150%","200%"),
            { "${ContestConfig.contestSoundVolume}%" },{ ContestConfig.contestSoundVolume = it.removeSuffix("%").toInt() })
        textInput(0,"Warning sound","Minecraft sound identifier.",{ ContestConfig.contestSound },{ ContestConfig.contestSound = it })
        choice(1,"Ticket earned message","For ! commands: members send r if anyone (including self) requested !dt, 1s or sec. Leaders wait for every other requester to say r or ready, then send the command. Plain text sends normally.",
            ContestConfig.ticketChoices,{ ContestConfig.ticketChoice },{ ContestConfig.ticketChoice = it })
        if (ContestConfig.ticketChoice == "other") textInput(1,"Custom ticket message","Sent as party chat. Empty text uses r. The same member r fallback and leader readiness wait apply to any ! command.",
            { ContestConfig.customTicket },{ ContestConfig.customTicket = it })
        toggle(1,"Auto contest-start message","Send at a new contest only inside a dungeon or Kuudra instance. Never in hubs, islands, Garden or Torrhus.",
            { ContestConfig.autoContestDowntime },{ ContestConfig.autoContestDowntime = it })
        choice(1,"Contest-start message","Only sends in dungeon or Kuudra instances, regardless of your party role.",ContestConfig.startChoices,
            { ContestConfig.startChoice },{ ContestConfig.startChoice = it })
        if (ContestConfig.startChoice == "other") textInput(1,"Custom start message","Empty text uses !dt contest. The instance restriction applies to custom text too.",
            { ContestConfig.customStart },{ ContestConfig.customStart = it })
        toggle(2,"Miria contest HUD","One condensed row: Miria Contest and a ticket checkmark or X. Ticket detection continues when hidden.",
            { ContestConfig.showContestTimer },{ ContestConfig.showContestTimer = it })
        action(2,"Move and resize HUD","Drag, scroll to resize, arrow keys to nudge, snapping and right-click settings — just like SparklingMutuals.","Edit layout") {
            minecraft.smSetScreen(ContestGui(this))
        }
        toggle(2,"HUD snapping","Snap to screen edges and center while dragging.",{ ContestConfig.hudSnapping },{ ContestConfig.hudSnapping = it })
        choice(2,"HUD scale","You can also resize by scrolling over the HUD in the layout editor.",listOf("25%","50%","75%","100%","125%","150%","200%"),
            { "${(ContestConfig.layout.scale*100).toInt()}%" },{ ContestConfig.editLayout(width,height); ContestConfig.layout.scale = it.removeSuffix("%").toFloat()/100 })
        toggle(2,"HUD border","Draw a thin border around the compact row.",{ ContestConfig.hudBorder },{ ContestConfig.hudBorder = it })
        choice(2,"Background transparency","100% makes the background fully transparent.",(0..10).map { "${it*10}%" },
            { "${ContestConfig.hudTransparency}%" },{ ContestConfig.hudTransparency = it.removeSuffix("%").toInt() })
        textInput(2,"Background color","Hex color, e.g. #0D0D10. Invalid colors leave the previous color in place.",
            { ContestConfig.hex(ContestConfig.hudBackground) },{ value -> ContestConfig.parseColor(value)?.let { ContestConfig.hudBackground = it } })
        textInput(2,"Border color","Hex color, e.g. #605238.",{ ContestConfig.hex(ContestConfig.hudBorderColor) },
            { value -> ContestConfig.parseColor(value)?.let { ContestConfig.hudBorderColor = it } })
        textInput(2,"Title color","Hex color, e.g. #E6C878. Ticket icons remain green/red.",{ ContestConfig.hex(ContestConfig.hudTitleColor) },
            { value -> ContestConfig.parseColor(value)?.let { ContestConfig.hudTitleColor = it } })
        choice(2,"Expansion direction","Keep this edge or center anchored when panel geometry changes.",net.johnceo.safaricontest.hud.HudExpansion.entries.map { it.name },
            { ContestConfig.layout.expansion.name },{ ContestConfig.editLayout(width,height); ContestConfig.layout.expansion = net.johnceo.safaricontest.hud.HudExpansion.valueOf(it) })
        action(2,"Reset HUD","Restore the default position, scale, border and colors.","Reset") {
            ContestConfig.resetLayout(); ContestConfig.hudBorder = true; ContestConfig.hudTransparency = 20
            ContestConfig.hudBackground = SafariTheme.BACKGROUND; ContestConfig.hudBorderColor = SafariTheme.BORDER
            ContestConfig.hudTitleColor = SafariTheme.CHAT_ACCENT; refresh()
        }
    }
    override fun init() {
        val themeWidth = minOf(122,contentWidth/2)
        search = EditBox(font,left+7,18,contentWidth-themeWidth-22,20,Component.literal("Search settings"))
        search.setBordered(false); search.setMaxLength(100); search.value = query
        search.setHint(Component.literal("Search…"))
        search.setResponder { query = it; scroll = 0; rebuildRows() }
        addRenderableWidget(search)
        addRenderableWidget(button(SettingsPalette.names[ContestConfig.settingsTheme],width-12-themeWidth,14,themeWidth) {
            minecraft.smSetScreen(ChoiceScreen(this,"Settings theme",SettingsPalette.names,SettingsPalette.names[ContestConfig.settingsTheme]) {
                ContestConfig.settingsTheme = SettingsPalette.names.indexOf(it); save()
            })
        })
        pages.forEachIndexed { index,label -> addRenderableWidget(button(label,8,59+index*34,nav-16) {
            page = index; scroll = 0; query = ""; search.value = ""; rebuildRows()
        }) }
        addRenderableWidget(button("Done",width-92,height-27,80) { onClose() })
        rowWidgets.clear(); rebuildRows()
    }
    private fun rebuildRows() {
        rowWidgets.forEach(::removeWidget); rowWidgets.clear(); cards.clear(); catalog()
        val filtered = rows.filter { if (query.isBlank()) it.page == page else (it.name+" "+it.description).contains(query,true) }
        val wide = contentWidth >= 330
        val controlWidth = if (wide) 125 else (contentWidth-18).coerceAtLeast(40)
        val textWidth = if (wide) contentWidth-controlWidth-30 else contentWidth-18
        fun rowHeight(row: Row): Int = font.split(Component.literal(row.name),textWidth).size*10 +
            font.split(Component.literal(row.description),textWidth).size*10 + if (wide) 22 else 48
        maxScroll = (filtered.sumOf { rowHeight(it)+7 } - (bottom-top)).coerceAtLeast(0)
        scroll = scroll.coerceIn(0,maxScroll)
        var y = top-scroll
        filtered.forEach { row ->
            val h = rowHeight(row)
            if (y+h > top && y < bottom) {
                cards += Card(row,y,h,textWidth)
                val cy = if (wide) y+(h-20)/2 else y+h-29
                if (cy >= top && cy+20 <= bottom) {
                    val cx = if (wide) width-12-controlWidth-9 else left+9
                    val widget = row.control(cx,cy,controlWidth)
                    rowWidgets += widget; addRenderableWidget(widget)
                }
            }
            y += h+7
        }
    }
    override fun extractRenderState(g: GuiGraphicsExtractor, mx: Int, my: Int, delta: Float) {
        g.fill(0,0,width,height,SettingsPalette.background)
        g.fill(0,0,nav,height,SettingsPalette.surface); g.fill(nav,0,width,48,SettingsPalette.surface)
        g.fill(nav-1,0,nav,height,SettingsPalette.border); g.fill(0,47,width,48,SettingsPalette.border)
        g.text(font,Component.literal("SAFARI").withStyle { it.withBold(true) },8,12,SettingsPalette.gold,false)
        g.text(font,Component.literal("CONTEST").withStyle { it.withBold(true) },8,26,SettingsPalette.gold,false)
        g.fill(left,12,search.x+search.width+7,38,SettingsPalette.card)
        g.text(font,if (query.isBlank()) pages[page] else "Search results",left,59,SettingsPalette.text,false)
        g.text(font,"/s",8,height-23,SettingsPalette.accent,false)
        g.enableScissor(left,top,width-10,bottom)
        cards.forEach { card ->
            g.fill(left,card.y,width-12,card.y+card.height,SettingsPalette.card)
            g.fill(left,card.y,left+2,card.y+card.height,SettingsPalette.border)
            var y = card.y+9
            font.split(Component.literal(card.row.name).withStyle { it.withBold(true) },card.textWidth).forEach {
                g.text(font,it,left+9,y,SettingsPalette.text,false); y += 10
            }
            y += 3
            font.split(Component.literal(card.row.description),card.textWidth).forEach {
                g.text(font,it,left+9,y,SettingsPalette.muted,false); y += 10
            }
        }
        if (maxScroll > 0) {
            val track = bottom-top
            val thumb = (track*track/(track+maxScroll)).coerceAtLeast(16)
            val y = top+(track-thumb)*scroll/maxScroll
            g.fill(width-7,top,width-4,bottom,SettingsPalette.card)
            g.fill(width-7,y,width-4,y+thumb,SettingsPalette.accent)
        }
        g.disableScissor()
        super.extractRenderState(g,mx,my,delta)
    }
    override fun mouseScrolled(mx: Double, my: Double, horizontal: Double, vertical: Double): Boolean {
        if (mx < left || my < top || my >= bottom) return super.mouseScrolled(mx,my,horizontal,vertical)
        scroll = (scroll-vertical*28).toInt().coerceIn(0,maxScroll); rebuildRows(); return true
    }
    override fun onClose() { save(); minecraft.smSetScreen(returnTo) }
    override fun isPauseScreen() = false
}

private class ChoiceScreen(private val parent: Screen, label: String, private val options: List<String>,
    private val selected: String, private val choose: (String) -> Unit) : Screen(Component.literal(label)) {
    private var scroll = 0
    private val count get() = ((height-82)/24).coerceAtLeast(1)
    override fun init() {
        val w = minOf(300,width-32)
        val x = (width-w)/2
        options.drop(scroll).take(count).forEachIndexed { i,option ->
            addRenderableWidget(Button.builder(Component.literal(if (option == selected) "$option  ✓" else option)) {
                choose(option); minecraft.smSetScreen(parent)
            }.bounds(x,40+i*24,w,20).build())
        }
        addRenderableWidget(Button.builder(Component.literal("Cancel")) { onClose() }.bounds(x,height-28,w,20).build())
    }
    override fun extractRenderState(g: GuiGraphicsExtractor, mx: Int, my: Int, delta: Float) {
        g.fill(0,0,width,height,SettingsPalette.background)
        g.centeredText(font,title,width/2,15,SettingsPalette.gold)
        super.extractRenderState(g,mx,my,delta)
    }
    override fun mouseScrolled(mx: Double, my: Double, horizontal: Double, vertical: Double): Boolean {
        scroll = (scroll-vertical*3).toInt().coerceIn(0,(options.size-count).coerceAtLeast(0)); rebuildWidgets(); return true
    }
    override fun onClose() { minecraft.smSetScreen(parent) }
    override fun isPauseScreen() = false
}
