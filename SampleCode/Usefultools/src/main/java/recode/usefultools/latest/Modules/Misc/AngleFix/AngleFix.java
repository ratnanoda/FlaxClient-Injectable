/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  imgui.ImDrawList
 *  imgui.ImGui
 */
package recode.usefultools.latest.Modules.Misc.AngleFix;

import imgui.ImDrawList;
import imgui.ImGui;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.AngleFix.AngleFix_h;

public class AngleFix
extends BaseModule<AngleFix_h> {
    public static AngleFix instance;
    public static float debugClientYaw;
    public static float debugServerYaw;
    public static double debugMoveAngle;
    public static double debugIntendedAngle;
    public static double debugRelDeg;
    public static String debugKeysStr;

    public AngleFix() {
        super(new AngleFix_h());
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
        String[] logs;
        if (!((AngleFix_h)this.h).enabled || !((AngleFix_h)this.h).debug.value) {
            return;
        }
        ImDrawList dl = ImGui.getForegroundDrawList();
        float sh = ImGui.getIO().getDisplaySizeY();
        float x = 10.0f;
        float y = sh * 0.45f;
        float spacing = 15.0f;
        for (String text : logs = new String[]{String.format("Client Yaw: %.2f", Float.valueOf(debugClientYaw)), String.format("Server Yaw: %.2f", Float.valueOf(debugServerYaw)), String.format("Input MoveAngle: %.2f", debugMoveAngle), String.format("Intended Angle: %.2f", debugIntendedAngle), String.format("Relative Angle (relDeg): %.2f", debugRelDeg), "Simulated Keys: " + debugKeysStr}) {
            dl.addText(x + 1.0f, y + 1.0f, ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.8f), text);
            dl.addText(x, y, ImGui.getColorU32(0.0f, 1.0f, 1.0f, 1.0f), text);
            y += spacing;
        }
    }

    static {
        debugClientYaw = 0.0f;
        debugServerYaw = 0.0f;
        debugMoveAngle = 0.0;
        debugIntendedAngle = 0.0;
        debugRelDeg = 0.0;
        debugKeysStr = "None";
    }
}

