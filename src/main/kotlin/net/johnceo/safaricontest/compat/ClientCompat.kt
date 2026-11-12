package net.johnceo.safaricontest.compat

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.Options
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Blocks
//? if >=26.3 {
/*import org.lwjgl.sdl.SDLMouse
*///? } else {
import org.lwjgl.glfw.GLFW
//? }

//? if >=26.2 {
/*val Minecraft.smScreen get() = gui.screen()
val Minecraft.smOverlay get() = gui.overlay()
fun Minecraft.smSetScreen(screen: Screen?) = gui.setScreen(screen)
val Options.smHideGui get() = Minecraft.getInstance().gui.hud.isHidden
val GameRenderer.smCamera get() = mainCamera()
fun Gui.smSetTitle(title: Component) = hud.setTitle(title)
fun Gui.smSetSubtitle(title: Component) = hud.setSubtitle(title)
fun Gui.smSetTimes(fadeIn: Int, stay: Int, fadeOut: Int) = hud.setTimes(fadeIn, stay, fadeOut)
val redCandle get() = Blocks.DYED_CANDLE.red()
*///? } else {
val Minecraft.smScreen get() = screen
val Minecraft.smOverlay get() = overlay
fun Minecraft.smSetScreen(screen: Screen?) = setScreen(screen)
val Options.smHideGui get() = hideGui
val GameRenderer.smCamera get() = mainCamera
fun Gui.smSetTitle(title: Component) = setTitle(title)
fun Gui.smSetSubtitle(title: Component) = setSubtitle(title)
fun Gui.smSetTimes(fadeIn: Int, stay: Int, fadeOut: Int) = setTimes(fadeIn, stay, fadeOut)
val redCandle get() = Blocks.RED_CANDLE
//? }

fun physicalLeftMouseDown(client: Minecraft): Boolean {
    //? if >=26.3 {
    /*return SDLMouse.nSDL_GetMouseState(0L, 0L) and SDLMouse.SDL_BUTTON_LMASK != 0
    *///? } else {
    return GLFW.glfwGetMouseButton(client.window.handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
    //? }
}

fun shiftDown(client: Minecraft): Boolean {
    //? if >=26.3 {
    /*return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT)
    *///? } else {
    return InputConstants.isKeyDown(client.window, InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(client.window, InputConstants.KEY_RSHIFT)
    //? }
}
