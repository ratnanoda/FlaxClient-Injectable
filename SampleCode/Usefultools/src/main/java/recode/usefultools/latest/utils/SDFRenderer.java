/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package recode.usefultools.latest.utils;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class SDFRenderer {
    public static void drawRoundedRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, int color) {
        if (radius <= 0.0f) {
            graphics.fillGradient((int)x, (int)y, (int)(x + width), (int)(y + height), color, color);
            return;
        }
        graphics.fillGradient((int)(x + radius), (int)y, (int)(x + width - radius), (int)(y + height), color, color);
        graphics.fillGradient((int)x, (int)(y + radius), (int)(x + radius), (int)(y + height - radius), color, color);
        graphics.fillGradient((int)(x + width - radius), (int)(y + radius), (int)(x + width), (int)(y + height - radius), color, color);
        int steps = 8;
        for (int i = 0; i < steps; ++i) {
            float angle1 = (float)Math.toRadians((float)i / (float)steps * 90.0f);
            float angle2 = (float)Math.toRadians((float)(i + 1) / (float)steps * 90.0f);
            float offset1 = radius * (1.0f - (float)Math.cos(angle1));
            float offset2 = radius * (1.0f - (float)Math.cos(angle2));
            float thickness1 = radius * (float)Math.sin(angle1);
            float thickness2 = radius * (float)Math.sin(angle2);
            graphics.fillGradient((int)(x + offset1), (int)(y + radius - thickness2), (int)(x + offset2), (int)(y + radius - thickness1), color, color);
            graphics.fillGradient((int)(x + width - offset2), (int)(y + radius - thickness2), (int)(x + width - offset1), (int)(y + radius - thickness1), color, color);
            graphics.fillGradient((int)(x + offset1), (int)(y + height - radius + thickness1), (int)(x + offset2), (int)(y + height - radius + thickness2), color, color);
            graphics.fillGradient((int)(x + width - offset2), (int)(y + height - radius + thickness1), (int)(x + width - offset1), (int)(y + height - radius + thickness2), color, color);
            graphics.fillGradient((int)(x + radius - thickness2), (int)(y + offset1), (int)(x + radius - thickness1), (int)(y + offset2), color, color);
            graphics.fillGradient((int)(x + width - radius + thickness1), (int)(y + offset1), (int)(x + width - radius + thickness2), (int)(y + offset2), color, color);
        }
    }
}

