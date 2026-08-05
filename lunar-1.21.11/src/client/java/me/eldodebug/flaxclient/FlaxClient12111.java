package me.eldodebug.flaxclient;

import com.mojang.blaze3d.platform.InputConstants;
import me.eldodebug.flaxclient.config.ClientConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlaxClient12111 implements ClientModInitializer {
    public static final String MOD_ID = "flaxclient";

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "compatibility_status");
    private static final KeyMapping TOGGLE_HUD = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.flaxclient.toggle_hud",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyMapping.Category.MISC
    ));

    private ClientConfig config;

    @Override
    public void onInitializeClient() {
        config = ClientConfig.load();

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.BOSS_BAR,
                HUD_ID,
                this::renderCompatibilityHud
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_HUD.consumeClick()) {
                config.toggleHud();
                config.save();
                LOGGER.info("FlaxClient compatibility HUD: {}", config.hudEnabled() ? "enabled" : "disabled");
            }
        });

        LOGGER.info("FlaxClient compatibility layer loaded for Minecraft/Lunar 1.21.11");
    }

    private void renderCompatibilityHud(GuiGraphics graphics, DeltaTracker tickCounter) {
        if (!config.hudEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        graphics.drawString(client.font, "FlaxClient 1.21.11", 6, 6, 0xFFFFFFFF);
    }
}
