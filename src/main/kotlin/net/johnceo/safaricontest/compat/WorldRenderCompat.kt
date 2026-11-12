package net.johnceo.safaricontest.compat

//? if >=26.3 {
/*import com.mojang.renderpearl.api.pipeline.DepthStencilState
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import com.mojang.renderpearl.api.pipeline.CompareOp
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.BlendFunction
*///? } else {
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
//? }
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.johnceo.safaricontest.SafariContest
import net.johnceo.safaricontest.compat.EspBoxLines
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.*
import net.minecraft.network.chat.Component
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f

/** API changes only: all callers retain their world coordinates, colors and visibility rules. */
enum class WorldDepthMode { THROUGH_TERRAIN, OCCLUDED }

object WorldRenderCompat {
    fun renderType(name: String, filled: Boolean = false, cull: Boolean? = false, depth: WorldDepthMode = WorldDepthMode.THROUGH_TERRAIN, afterShaders: Boolean = false): RenderType {
        val builder = RenderPipeline.builder(
            if (filled) RenderPipelines.DEBUG_FILLED_SNIPPET else RenderPipelines.LINES_SNIPPET)
            .withLocation(SafariContest.id("pipeline/$name"))
            .withDepthStencilState(DepthStencilState(if(depth==WorldDepthMode.OCCLUDED) DepthStencilState.DEFAULT.depthTest() else CompareOp.ALWAYS_PASS, false))
        // 26.3's line snippet leaves color targets to its caller; filled geometry already supplies one.
        //? if >=26.3 {
        /*if (!filled) builder.withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        *///? }
        cull?.let { builder.withCull(it) }
        val pipeline = RenderPipelines.register(builder.build())
        val setup = RenderSetup.builder(pipeline).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
        // 26.3 routes feature geometry through the frame's render passes; output targets no longer exist.
        //? if <26.3 {
        setup.setOutputTarget(if (afterShaders) OutputTarget.MAIN_TARGET else OutputTarget.ITEM_ENTITY_TARGET)
        //? }
        return RenderType.create("safari-contest:$name", setup.createRenderSetup())
    }

    fun register(callback: (LevelRenderContext) -> Unit) {
        //? if >=26.2 {
        /*LevelRenderEvents.COLLECT_SUBMITS.register { callback(it) }
        *///? } else {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register { callback(it) }
        //? }
    }

    private fun geometry(context: LevelRenderContext, type: RenderType, draw: (PoseStack.Pose, VertexConsumer) -> Unit) {
        //? if >=26.2 {
        /*context.submitNodeCollector().submitCustomGeometry(context.poseStack(), type) { pose, vertices -> draw(pose, vertices) }
        *///? } else {
        val buffers = context.bufferSource()
        draw(context.poseStack().last(), buffers.getBuffer(type))
        //? }
    }

    fun flush(context: LevelRenderContext, type: RenderType) {
        //? if <26.2 {
        context.bufferSource().endBatch(type)
        //? }
    }

    fun box(context: LevelRenderContext, type: RenderType, worldBox: AABB, color: Int, width: Float = 2f) {
        val camera = Minecraft.getInstance().gameRenderer.smCamera.position()
        val b = worldBox.move(-camera.x, -camera.y, -camera.z)
        geometry(context, type) { pose, vertices ->
            EspBoxLines.edges(b.minX.toFloat(), b.minY.toFloat(), b.minZ.toFloat(), b.maxX.toFloat(), b.maxY.toFloat(), b.maxZ.toFloat()) { x, y, z, ex, ey, ez, axis ->
                val nx = if (axis == 0) 1f else 0f; val ny = if (axis == 1) 1f else 0f; val nz = if (axis == 2) 1f else 0f
                vertices.addVertex(pose, x, y, z).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width)
                vertices.addVertex(pose, ex, ey, ez).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width)
            }
        }
    }

}
