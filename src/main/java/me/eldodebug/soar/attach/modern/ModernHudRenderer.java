package me.eldodebug.soar.attach.modern;

import imgui.ImGui;

public final class ModernHudRenderer {

    private static float nextY;
    private static boolean background = true;

    private ModernHudRenderer() {
    }

    public static void begin() {
        nextY = 12.0f;
    }

    public static void setBackground(boolean enabled) {
        background = enabled;
    }

    public static void line(String text, int color) {
        if (background) {
            ImGui.getForegroundDrawList().addRectFilled(
                    8.0f,
                    nextY - 2.0f,
                    18.0f + ImGui.calcTextSize(text).x,
                    nextY + 18.0f,
                    0x990D101A,
                    5.0f);
        }
        ImGui.getForegroundDrawList().addText(13.0f, nextY, color, text);
        nextY += 23.0f;
    }
}
