/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  imgui.ImDrawList
 *  imgui.ImFont
 *  imgui.ImGui
 *  net.minecraft.client.Minecraft
 */
package recode.usefultools.latest.Modules.Visual.LevelInfo;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Visual.Interface.Interface;
import recode.usefultools.latest.Modules.Visual.Interface.Interface_h;
import recode.usefultools.latest.Modules.Visual.LevelInfo.LevelInfo_h;
import recode.usefultools.latest.utils.ImGuiEngine;

public class LevelInfo
extends BaseModule<LevelInfo_h> {
    public static LevelInfo instance;

    public LevelInfo() {
        super(new LevelInfo_h());
        instance = this;
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
        if (LevelInfo.mc.player == null || LevelInfo.mc.level == null || !((LevelInfo_h)this.h).enabled) {
            return;
        }
        Interface ui = (Interface)ModuleManager.INSTANCE.getModuleByName("Interface");
        if (ui == null) {
            return;
        }
        boolean isMc = ((Interface_h)ui.h).font.value == Interface_h.FontType.Mojangles;
        ImFont font = ImGuiEngine.INSTANCE.fonts.getOrDefault(isMc ? "minecraft" : "main", ImGuiEngine.INSTANCE.fonts.get("main"));
        float fSize = (float)((LevelInfo_h)this.h).fontSize.value;
        float scale = fSize / font.getFontSize();
        float lineHeight = fSize + 4.0f;
        float sh = ImGui.getIO().getDisplaySizeY();
        float startX = 10.0f;
        ArrayList<Object> lines = new ArrayList<Object>();
        if (((LevelInfo_h)this.h).sprint.value) {
            lines.add("Sprint: " + (LevelInfo.mc.player.isSprinting() ? "ON" : "OFF"));
        }
        if (((LevelInfo_h)this.h).onGround.value) {
            lines.add("OnGround: " + (LevelInfo.mc.player.onGround() ? "true" : "false"));
        }
        if (((LevelInfo_h)this.h).fps.value) {
            lines.add("FPS: " + Minecraft.getInstance().getFps());
        }
        if (((LevelInfo_h)this.h).xyz.value) {
            lines.add(String.format("XYZ: %.2f, %.2f, %.2f", LevelInfo.mc.player.getX(), LevelInfo.mc.player.getY(), LevelInfo.mc.player.getZ()));
        }
        if (((LevelInfo_h)this.h).bps.value) {
            double dx = LevelInfo.mc.player.getX() - LevelInfo.mc.player.xo;
            double dz = LevelInfo.mc.player.getZ() - LevelInfo.mc.player.zo;
            double bpsVal = Math.sqrt(dx * dx + dz * dz) * 20.0;
            lines.add(String.format("BPS: %.2f bps", bpsVal));
        }
        float startY = sh - (float)lines.size() * lineHeight - 10.0f;
        ImGui.pushFont((ImFont)font);
        ImDrawList dl = ImGui.getForegroundDrawList();
        for (int i = 0; i < lines.size(); ++i) {
            String text = (String)lines.get(i);
            float cx = isMc ? (float)Math.round(startX) : startX;
            float cy = isMc ? (float)Math.round(startY) : startY;
            int textColor = ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f);
            if (((LevelInfo_h)this.h).shadow.value) {
                float offset = 1.0f * scale;
                int shadowColor = ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.9f);
                dl.addText(font, (int)fSize, cx + offset, cy + offset, shadowColor, text);
            }
            dl.addText(font, (int)fSize, cx, cy, textColor, text);
            startY += lineHeight;
        }
        ImGui.popFont();
    }
}

