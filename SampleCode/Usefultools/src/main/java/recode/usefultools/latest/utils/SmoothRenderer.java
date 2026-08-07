/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package recode.usefultools.latest.utils;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class SmoothRenderer {
    public static void drawSmoothRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height, float radius, int color) {
        if (radius <= 0.0f) {
            graphics.fillGradient((int)x, (int)y, (int)(x + width), (int)(y + height), color, color);
            return;
        }
        graphics.fillGradient((int)(x + radius), (int)y, (int)(x + width - radius), (int)(y + height), color, color);
        for (int i = 0; i < (int)radius; ++i) {
            float offset = i;
            float h = radius - (float)Math.sqrt((double)(radius * radius) - Math.pow(radius - offset, 2.0));
            graphics.fillGradient((int)(x + offset), (int)(y + h), (int)(x + offset + 1.0f), (int)(y + height - h), color, color);
            graphics.fillGradient((int)(x + width - offset - 1.0f), (int)(y + h), (int)(x + width - offset), (int)(y + height - h), color, color);
        }
    }
}

