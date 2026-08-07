/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  imgui.ImDrawList
 *  imgui.ImFont
 *  imgui.ImGui
 *  net.minecraft.network.chat.Component
 *  org.spongepowered.asm.mixin.Unique
 */
package recode.usefultools.latest.Modules.Misc.StaffDetector;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Unique;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.StaffDetector.StaffDetector_h;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Visual.Interface.Interface;
import recode.usefultools.latest.Modules.Visual.Interface.Interface_h;
import recode.usefultools.latest.mixin.PlayerTabOverlayAccessor;
import recode.usefultools.latest.utils.ImGuiEngine;

public class StaffDetector
extends BaseModule<StaffDetector_h> {
    public static StaffDetector instance;
    public boolean staffDetected = false;
    public int lastDeclaredCount = 0;
    public int lastActualCount = 0;
    @Unique
    private final static Pattern COUNT_PATTERN;
    @Unique
    private long detectionStartTime = 0L;
    @Unique
    private boolean wasDetected = false;

    public StaffDetector() {
        super(new StaffDetector_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        this.staffDetected = false;
        this.lastDeclaredCount = 0;
        this.lastActualCount = 0;
        this.wasDetected = false;
        this.detectionStartTime = 0L;
    }

    @Override
    public void onDisable() {
        this.staffDetected = false;
        this.wasDetected = false;
        this.detectionStartTime = 0L;
    }

    @Override
    public void onUpdate() {
        if (StaffDetector.mc.player == null || StaffDetector.mc.level == null) {
            return;
        }
        if (mc.getConnection() == null || StaffDetector.mc.gui.getTabList() == null) {
            return;
        }
        PlayerTabOverlayAccessor tabAccessor = (PlayerTabOverlayAccessor)StaffDetector.mc.gui.getTabList();
        Component headerComp = tabAccessor.getHeader();
        Component footerComp = tabAccessor.getFooter();
        String header = headerComp != null ? headerComp.getString() : "";
        String footer = footerComp != null ? footerComp.getString() : "";
        String fullTab = header + "\n" + footer;
        Matcher matcher = COUNT_PATTERN.matcher(fullTab);
        int declaredCount = -1;
        while (matcher.find()) {
            try {
                declaredCount = Integer.parseInt(matcher.group(1));
            } catch (Exception exception) {}
        }
        if (declaredCount == -1) {
            return;
        }
        int actualCount = mc.getConnection().getOnlinePlayers().size();
        boolean prevStaffDetected = this.staffDetected;
        this.staffDetected = declaredCount > actualCount;
        this.lastDeclaredCount = declaredCount;
        this.lastActualCount = actualCount;
        if (this.staffDetected && !prevStaffDetected) {
            StaffDetector.mc.player.sendSystemMessage((Component)Component.literal((String)("§7[§cStaffDetector§7] §c§lWARNING: Vanished staff member detected! §7(Tab shows: " + declaredCount + ", Actual: " + actualCount + ")")));
        }
        if (this.staffDetected) {
            if (!this.wasDetected) {
                this.detectionStartTime = System.currentTimeMillis();
                this.wasDetected = true;
            }
        } else {
            this.wasDetected = false;
            this.detectionStartTime = 0L;
        }
    }

    @Override
    public void onRenderHUD() {
        if (!((StaffDetector_h)this.h).enabled || !this.staffDetected || this.detectionStartTime == 0L) {
            return;
        }
        long elapsed = System.currentTimeMillis() - this.detectionStartTime;
        float progress = (float)elapsed / 1200.0f;
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        float sw = ImGui.getIO().getDisplaySizeX();
        float sh = ImGui.getIO().getDisplaySizeY();
        ImDrawList dl = ImGui.getForegroundDrawList();
        Interface ui = (Interface)ModuleManager.INSTANCE.getModuleByName("Interface");
        boolean isMc = ui != null && ((Interface_h)ui.h).font.value == Interface_h.FontType.Mojangles;
        String fontKey = isMc ? "minecraft_bold" : "main_bold";
        ImFont font = ImGuiEngine.INSTANCE.fonts.getOrDefault(fontKey, ImGuiEngine.INSTANCE.fonts.get("main"));
        float fSize = (float)((StaffDetector_h)this.h).fontSize.value;
        float scale = fSize / font.getFontSize();
        if (progress < 1.0f) {
            float barWidth = 200.0f;
            float barHeight = 6.0f;
            float x = (sw - barWidth) / 2.0f;
            float y = sh / 2.0f + (float)((StaffDetector_h)this.h).yOffset.value;
            dl.addRectFilled(x, y, x + barWidth, y + barHeight, ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.5f), 3.0f);
            int themeColor = ui != null ? ui.getCurrentColor(0) : -22016;
            float fillWidth = barWidth * progress;
            if (fillWidth > 0.0f) {
                dl.addRectFilled(x, y, x + fillWidth, y + barHeight, themeColor, 3.0f);
            }
            int pct = (int)(progress * 100.0f);
            String pctText = "Analyzing... " + pct + "%";
            ImGui.pushFont((ImFont)font);
            float tw = ImGui.calcTextSize((String)pctText).x * scale;
            float th = ImGui.calcTextSize((String)pctText).y * scale;
            ImGui.popFont();
            float tx = (sw - tw) / 2.0f;
            float ty = y - th - 6.0f;
            ImGui.pushFont((ImFont)font);
            dl.addText(font, (int)fSize, tx + 1.0f * scale, ty + 1.0f * scale, ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.8f), pctText);
            dl.addText(font, (int)fSize, tx, ty, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), pctText);
            ImGui.popFont();
        } else {
            String alertText = "!!! Staff Detected !!!";
            ImGui.pushFont((ImFont)font);
            float tw = ImGui.calcTextSize((String)alertText).x * scale;
            ImGui.popFont();
            float tx = (sw - tw) / 2.0f;
            float ty = sh / 2.0f + (float)((StaffDetector_h)this.h).yOffset.value;
            int redColor = ImGui.getColorU32(1.0f, 0.2f, 0.2f, 1.0f);
            int shadowColor = ImGui.getColorU32(0.25f, 0.05f, 0.05f, 0.9f);
            ImGui.pushFont((ImFont)font);
            dl.addText(font, (int)fSize, tx + 1.5f * scale, ty + 1.5f * scale, shadowColor, alertText);
            dl.addText(font, (int)fSize, tx, ty, redColor, alertText);
            ImGui.popFont();
        }
    }

    static {
        COUNT_PATTERN = Pattern.compile(":\\s*(\\d+)(?!\\s*(ms|fps|\\.|\\d))");
    }
}

