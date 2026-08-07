/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  imgui.ImDrawList
 *  imgui.ImFont
 *  imgui.ImGui
 */
package recode.usefultools.latest.Modules.Visual.Watermark;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Visual.Interface.Interface;
import recode.usefultools.latest.Modules.Visual.Interface.Interface_h;
import recode.usefultools.latest.Modules.Visual.Watermark.Watermark_h;
import recode.usefultools.latest.utils.ImGuiEngine;

public class Watermark
extends BaseModule<Watermark_h> {
    public final static Watermark instance = new Watermark();

    public Watermark() {
        super(new Watermark_h());
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public void onRenderHUD() {
        boolean isMc;
        Interface ui = (Interface)ModuleManager.INSTANCE.getModuleByName("Interface");
        if (ui == null) {
            return;
        }
        boolean bl = ((Watermark_h)this.h).fontMode.value == Watermark_h.FontMode.InterfaceF ? ((Interface_h)ui.h).font.value == Interface_h.FontType.Mojangles : (isMc = ((Watermark_h)this.h).fontMode.value == Watermark_h.FontMode.Mojangles);
        String fontKey = isMc ? (((Watermark_h)this.h).bold.value ? "minecraft_bold_large" : "minecraft_large") : (((Watermark_h)this.h).bold.value ? "watermark" : "main");
        ImFont font = ImGuiEngine.INSTANCE.fonts.getOrDefault(fontKey, ImGuiEngine.INSTANCE.fonts.get("main"));
        float fSize = (float)((Watermark_h)this.h).fontSize.value;
        float scale = fSize / font.getFontSize();
        String text = "Useful-tools";
        float startX = 5.0f;
        float startY = 5.0f;
        ImGui.pushFont((ImFont)font);
        for (int j = 0; j < text.length(); ++j) {
            float cy;
            String charStr = String.valueOf(text.charAt(j));
            int charColor = ui.getCurrentColor(j * 3);
            float cr = (float)(charColor >> 16 & 0xFF) / 255.0f;
            float cg = (float)(charColor >> 8 & 0xFF) / 255.0f;
            float cb = (float)(charColor & 0xFF) / 255.0f;
            float charWidth = ImGui.calcTextSize((String)charStr).x * scale;
            float charHeight = ImGui.calcTextSize((String)charStr).y * scale;
            float cx = isMc ? (float)Math.round(startX) : startX;
            float f = cy = isMc ? (float)Math.round(startY) : startY;
            if (((Watermark_h)this.h).glow.value) {
                float centerX = cx + charWidth / 2.0f;
                float centerY = cy + charHeight / 2.0f;
                float glowRadius = fSize / 3.0f;
                int glowColor = ImGui.getColorU32((float)cr, (float)cg, (float)cb, 0.75f);
                this.drawGlowCircle(ImGui.getForegroundDrawList(), centerX, centerY, glowRadius, glowColor);
            }
            if (((Watermark_h)this.h).shadow.value) {
                int shadowColor = ImGui.getColorU32((float)(cr * 0.25f), (float)(cg * 0.25f), (float)(cb * 0.25f), 0.925f);
                float offset = 1.0f * scale;
                ImGui.getForegroundDrawList().addText(font, (int)fSize, isMc ? (float)Math.round(cx + offset) : cx + offset, isMc ? (float)Math.round(cy + offset) : cy + offset, shadowColor, charStr);
            }
            ImGui.getForegroundDrawList().addText(font, (int)fSize, cx, cy, charColor, charStr);
            startX += charWidth;
        }
        ImGui.popFont();
    }

    private void drawGlowCircle(ImDrawList dl, float cx, float cy, float radius, int color) {
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        float a = (float)(color >> 24 & 0xFF) / 255.0f;
        int steps = 12;
        for (int i = 0; i < steps; ++i) {
            float ratio = (float)i / (float)steps;
            float alpha = a * (1.0f - ratio) * (1.0f - ratio) * 0.15f;
            int col = ImGui.getColorU32((float)r, (float)g, (float)b, (float)alpha);
            float currentRadius = radius * (1.0f + ratio * 1.5f);
            dl.addCircleFilled(cx, cy, currentRadius, col, 16);
        }
    }
}

