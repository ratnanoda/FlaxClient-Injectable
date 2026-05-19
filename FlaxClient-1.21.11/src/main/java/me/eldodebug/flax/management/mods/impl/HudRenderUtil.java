package me.eldodebug.flax.management.mods.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class HudRenderUtil {

	private HudRenderUtil() {
	}

	public static void drawHudLine(DrawContext context, MinecraftClient mc, int x, int y, String text, int accentColor, int textColor) {
		int width = mc.textRenderer.getWidth(text) + 8;
		context.fill(x - 3, y - 2, x + width, y + 10, 0x720A0A0A);
		context.fill(x - 3, y - 2, x - 1, y + 10, accentColor);
		context.drawTextWithShadow(mc.textRenderer, Text.literal(text), x + 1, y, textColor);
	}
}
