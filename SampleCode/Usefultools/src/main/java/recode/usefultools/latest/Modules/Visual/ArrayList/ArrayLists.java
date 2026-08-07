/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  imgui.ImDrawList
 *  imgui.ImFont
 *  imgui.ImGui
 */
package recode.usefultools.latest.Modules.Visual.ArrayList;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import java.util.Comparator;
import java.util.List;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Visual.ArrayList.ArrayLists_h;
import recode.usefultools.latest.Modules.Visual.Interface.Interface;
import recode.usefultools.latest.Modules.Visual.Interface.Interface_h;
import recode.usefultools.latest.utils.ImGuiEngine;

public class ArrayLists
extends BaseModule<ArrayLists_h> {
    public final static ArrayLists instance = new ArrayLists();

    public ArrayLists() {
        super(new ArrayLists_h());
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
        boolean bl = ((ArrayLists_h)this.h).fontMode.value == ArrayLists_h.FontMode.InterfaceF ? ((Interface_h)ui.h).font.value == Interface_h.FontType.Mojangles : (isMc = ((ArrayLists_h)this.h).fontMode.value == ArrayLists_h.FontMode.Mojangles);
        String fontKey = isMc ? (((ArrayLists_h)this.h).bold.value ? "minecraft_bold" : "minecraft") : (((ArrayLists_h)this.h).bold.value ? "main_bold" : "main");
        ImFont font = ImGuiEngine.INSTANCE.fonts.getOrDefault(fontKey, ImGuiEngine.INSTANCE.fonts.get("main"));
        float fSize = (float)((ArrayLists_h)this.h).fontSize.value;
        float scale = fSize / font.getFontSize();
        List<BaseModule> modules = ModuleManager.INSTANCE.getModules().stream().filter(m -> ((ModuleHeader)m.h).enabled && !((ModuleHeader)m.h).name.matches("(?i)ClickGui|ArrayList")).sorted(Comparator.comparingDouble(m -> -ImGui.calcTextSize((String)((ModuleHeader)m.h).name).x)).toList();
        float y = 5.0f;
        float sw = ImGui.getIO().getDisplaySizeX();
        ImGui.pushFont((ImFont)font);
        for (int i = 0; i < modules.size(); ++i) {
            String name = ((ModuleHeader)modules.get((int)i).h).name;
            float tw = ImGui.calcTextSize((String)name).x * (fSize / font.getFontSize());
            float posX = sw - tw - 5.0f;
            float posY = y;
            int rawColor = ui.getCurrentColor(i * 2);
            float mr = (float)(rawColor >> 16 & 0xFF) / 255.0f;
            float mg = (float)(rawColor >> 8 & 0xFF) / 255.0f;
            float mb = (float)(rawColor & 0xFF) / 255.0f;
            if (((ArrayLists_h)this.h).glow.value) {
                float centerX = posX + tw / 2.0f;
                float centerY = posY + fSize / 2.0f;
                float glowRadius = fSize / 2.0f;
                int glowColor = ImGui.getColorU32((float)mr, (float)mg, (float)mb, 0.5f);
                this.drawGlowCircle(ImGui.getForegroundDrawList(), centerX, centerY, glowRadius, glowColor);
            }
            if (((ArrayLists_h)this.h).shadow.value) {
                int shadowColor = ImGui.getColorU32((float)(mr * 0.25f), (float)(mg * 0.25f), (float)(mb * 0.25f), 0.925f);
                float offset = 1.0f * scale;
                ImGui.getForegroundDrawList().addText(font, (int)fSize, isMc ? (float)Math.round(posX + offset) : posX + offset, isMc ? (float)Math.round(posY + offset) : posY + offset, shadowColor, name);
            }
            ImGui.getForegroundDrawList().addText(font, (int)fSize, isMc ? (float)Math.round(posX) : posX, isMc ? (float)Math.round(posY) : posY, ImGui.getColorU32((float)mr, (float)mg, (float)mb, 1.0f), name);
            y += fSize + 2.0f;
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

