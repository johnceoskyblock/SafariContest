package net.johnceo.safaricontest.config

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component

/** Native button interaction/narration, with the same surfaces as the settings workspace. */
class ThemedButton private constructor(x: Int,y: Int,w: Int,h: Int,label: Component,action: OnPress) : Button(x,y,w,h,label,action,DEFAULT_NARRATION) {
    override fun extractContents(g: GuiGraphicsExtractor,mx: Int,my: Int,delta: Float) {
        val highlighted=isHoveredOrFocused
        g.fill(x,y,x+width,y+height,if(highlighted && active) SettingsPalette.hover else SettingsPalette.card)
        val border=if(highlighted && active) SettingsPalette.accent else SettingsPalette.border
        g.fill(x,y,x+width,y+1,border);g.fill(x,y+height-1,x+width,y+height,border)
        g.fill(x,y,x+1,y+height,border);g.fill(x+width-1,y,x+width,y+height,border)
        val font=Minecraft.getInstance().font
        val text=font.plainSubstrByWidth(message.string,(width-8).coerceAtLeast(1))
        g.text(font,text,x+(width-font.width(text))/2,y+(height-8)/2,if(active) SettingsPalette.text else SettingsPalette.muted,false)
    }
    class Builder(private val message: Component,private val action: OnPress) {
        private var x=0;private var y=0;private var w=150;private var h=20
        fun bounds(x: Int,y: Int,w: Int,h: Int)=apply { this.x=x;this.y=y;this.w=w;this.h=h }
        fun width(w: Int)=apply { this.w=w }
        fun build()=ThemedButton(x,y,w,h,message,action)
    }
    companion object { fun builder(message: Component,action: OnPress)=Builder(message,action) }
}
