package me.eldodebug.soar.forge.render;

import java.awt.Color;

import me.eldodebug.soar.management.mods.impl.DepthFogPostProcessor;
import me.eldodebug.soar.management.mods.impl.ShaderMod;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Forge client listener for ShaderMod's depth-based post-processing fog.
 *
 * Vanilla fog is disabled on land so the post-process is the only fog pass.
 * Water and lava intentionally keep Minecraft's own fog behaviour.
 */
@SideOnly(Side.CLIENT)
public final class ForgeLinearFogHandler {

    private static boolean registered;

    private ForgeLinearFogHandler() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        MinecraftForge.EVENT_BUS.register(new ForgeLinearFogHandler());
        registered = true;
    }

    @SubscribeEvent
    public void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
        ShaderMod shaderMod = ShaderMod.getInstance();
        if (!shouldOverrideFog(shaderMod, event)) {
            return;
        }

        GlStateManager.disableFog();
    }

    @SubscribeEvent
    public void onFogColors(EntityViewRenderEvent.FogColors event) {
        ShaderMod shaderMod = ShaderMod.getInstance();
        if (!shouldOverrideFog(shaderMod, event)) {
            return;
        }

        Color color = shaderMod.getFogColor();
        event.red = color.getRed() / 255.0F;
        event.green = color.getGreen() / 255.0F;
        event.blue = color.getBlue() / 255.0F;
    }

    /**
     * RenderWorldLast runs after the world has populated the color/depth
     * buffers. The shared renderer copies those buffers and executes one
     * fullscreen post-process pass; it never creates world-space fog planes.
     */
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        ShaderMod shaderMod = ShaderMod.getInstance();
        if (shaderMod != null) {
            shaderMod.renderFogAndObjects(event.partialTicks);
        }
    }

    private boolean shouldOverrideFog(ShaderMod shaderMod, EntityViewRenderEvent event) {
        if (shaderMod == null || !shaderMod.isForgeFogEnabled()) {
            return false;
        }

        Object material = getEventBlockMaterial(event);
        return material != Material.water && material != Material.lava
                && DepthFogPostProcessor.isPostProcessAvailable();
    }

    /**
     * This is the equivalent of {@code event.block.getMaterial()}. The
     * tweaker build compiles against Forge's 1.8.9 universal API, whose public
     * event field references its Notch-named Block class. Reading it through
     * reflection keeps this client source compatible with both that runtime
     * and the MCP-named development classpath.
     */
    private Object getEventBlockMaterial(EntityViewRenderEvent event) {
        try {
            Object block = EntityViewRenderEvent.class.getField("block").get(event);
            if (block == null) {
                return null;
            }

            for (String methodName : new String[] { "getMaterial", "func_149688_o", "t" }) {
                try {
                    return block.getClass().getMethod(methodName).invoke(block);
                } catch (NoSuchMethodException ignored) {
                    // Try the next MCP/SRG/Notch method name.
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // A missing foreign block falls back to the land post-process.
        }

        return null;
    }
}
