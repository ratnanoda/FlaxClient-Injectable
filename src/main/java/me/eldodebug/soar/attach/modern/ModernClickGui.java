package me.eldodebug.soar.attach.modern;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import me.eldodebug.soar.attach.modern.ModernSetting.BooleanSetting;
import me.eldodebug.soar.attach.modern.ModernSetting.ColorSetting;
import me.eldodebug.soar.attach.modern.ModernSetting.ComboSetting;
import me.eldodebug.soar.attach.modern.ModernSetting.KeybindSetting;
import me.eldodebug.soar.attach.modern.ModernSetting.NumberSetting;

/**
 * Lunar renderer for the 1.8.9 Flax floating-glass GuiModMenu.
 *
 * <p>The original NanoVG screen cannot be loaded into Minecraft 26.1.2 because
 * it directly extends the 1.8.9 GuiScreen and LWJGL 2 APIs. This class keeps
 * its layout contract and interaction model while drawing through ImGui's
 * version-independent draw list.
 */
public final class ModernClickGui {

    private static final Page[] NAVIGATION = {
            Page.HOME, Page.MODULES, Page.YOUTUBE, Page.COSMETICS,
            Page.GAMES, Page.PROFILE, Page.SCREENSHOTS
    };

    private final ModernModuleManager modules;
    private final Set<String> expanded = new HashSet<String>();
    private final Panel ghostPanel = new Panel("Ghost");
    private final Panel blatantPanel = new Panel("Blatant");
    private final Panel otherPanel = new Panel("Other");
    private final float[] snowX = new float[36];
    private final float[] snowY = new float[36];
    private final float[] snowSpeed = new float[36];
    private final float[] snowPhase = new float[36];
    private final Map<String, Float> animations = new HashMap<String, Float>();

    private Page page = Page.MODULES;
    private ModernModule bindingModule;
    private KeybindSetting bindingSetting;
    private boolean escapeCaptured;
    private ImFont regular;
    private ImFont medium;
    private ImFont semibold;
    private ImFont icons;
    private float sidebarX = Float.NaN;
    private float sidebarY;
    private float scale = 1.0f;
    private long lastFrame = System.nanoTime();
    private float frameDelta;
    private float timeSeconds;
    private float menuReveal;

    public ModernClickGui(ModernModuleManager modules) {
        this.modules = modules;
        Random random = new Random(0xF1A8C11EL);
        for (int i = 0; i < snowX.length; i++) {
            snowX[i] = random.nextFloat();
            snowY[i] = random.nextFloat();
            snowSpeed[i] = 9.0f + random.nextFloat() * 20.0f;
            snowPhase[i] = random.nextFloat() * 6.28318f;
        }
    }

    public void setFonts(ImFont regular, ImFont medium, ImFont semibold, ImFont icons) {
        this.regular = regular;
        this.medium = medium;
        this.semibold = semibold;
        this.icons = icons;
    }

    public void onVisibilityChanged(boolean visible) {
        if (visible) {
            menuReveal = 0.0f;
            lastFrame = System.nanoTime();
        } else {
            bindingModule = null;
            bindingSetting = null;
        }
    }

    public void draw() {
        long now = System.nanoTime();
        frameDelta = Math.min(0.05f, Math.max(0.0f, (now - lastFrame) / 1_000_000_000.0f));
        lastFrame = now;
        timeSeconds = now / 1_000_000_000.0f;
        menuReveal = approach(menuReveal, 1.0f, 12.0f);
        float displayWidth = ImGui.getIO().getDisplaySizeX();
        float displayHeight = ImGui.getIO().getDisplaySizeY();
        scale = Math.max(0.85f, Math.min(2.0f, Math.min(displayWidth / 960.0f, displayHeight / 540.0f)));

        ImGui.setNextWindowPos(0.0f, 0.0f);
        ImGui.setNextWindowSize(displayWidth, displayHeight);
        int flags = ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoBackground
                | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoSavedSettings
                | ImGuiWindowFlags.NoBringToFrontOnFocus;
        if (!ImGui.begin("##flax-legacy-workspace", flags)) {
            ImGui.end();
            return;
        }

        ImDrawList draw = ImGui.getWindowDrawList();
        drawAtmosphere(draw, displayWidth, displayHeight);

        float logicalWidth = displayWidth / scale;
        float logicalHeight = displayHeight / scale;
        float contentWidth = Math.min(798.0f, Math.max(418.0f, logicalWidth - 92.0f)) * scale;
        float contentHeight = Math.min(360.0f, Math.max(250.0f, logicalHeight - 76.0f)) * scale;
        float contentX = (displayWidth - contentWidth) / 2.0f + 20.0f * scale;
        float contentY = (displayHeight - contentHeight) / 2.0f
                + (1.0f - easeOut(menuReveal)) * 12.0f * scale;

        drawSidebar(draw, displayWidth, displayHeight, contentX, contentY);
        if (page == Page.MODULES) {
            drawTextCentered(draw, semibold, 12.5f * scale, displayWidth / 2.0f,
                    5.0f * scale, rgba(235, 239, 251, 230), "Module");
            drawModuleWorkspace(draw, displayWidth, displayHeight, contentX, contentY, contentWidth);
        } else {
            drawGlass(draw, contentX, contentY, contentWidth, contentHeight, 15.0f * scale, false);
            drawContentPage(draw, contentX, contentY, contentWidth, contentHeight);
        }

        drawBindingPrompt(draw, displayWidth, displayHeight);

        ImGui.end();
    }

    public boolean onKeyInput(int key, int action) {
        if (action != 1) return true;
        if (bindingModule == null && bindingSetting == null) return true;
        if (key == 256) {
            bindingModule = null;
            bindingSetting = null;
            escapeCaptured = true;
            return true;
        }
        int value = key == 259 || key == 261 ? -1 : key;
        if (bindingModule != null) bindingModule.setKeybind(value);
        if (bindingSetting != null) bindingSetting.setValue(value);
        bindingModule = null;
        bindingSetting = null;
        modules.save();
        return true;
    }

    public boolean consumeEscapeCaptured() {
        boolean captured = escapeCaptured;
        escapeCaptured = false;
        return captured;
    }

    private void drawAtmosphere(ImDrawList draw, float width, float height) {
        int backdropAlpha = Math.round(66.0f * easeOut(menuReveal));
        draw.addRectFilled(0.0f, 0.0f, width, height, rgba(6, 9, 18, backdropAlpha));
        for (int i = 0; i < snowX.length; i++) {
            snowY[i] += snowSpeed[i] * scale * frameDelta / height;
            if (snowY[i] > 1.02f) snowY[i] = -0.02f;
            float x = snowX[i] * width + (float) Math.sin(timeSeconds * 0.75f + snowPhase[i]) * 4.0f * scale;
            float y = snowY[i] * height;
            float radius = (0.55f + (i % 7) * 0.12f) * scale;
            draw.addCircleFilled(x, y, radius, rgba(236, 244, 255, 72));
        }
    }

    private void drawSidebar(ImDrawList draw, float displayWidth, float displayHeight,
            float contentX, float contentY) {
        float width = 42.0f * scale;
        float gripHeight = 13.0f * scale;
        float height = (13.0f + 34.0f + NAVIGATION.length * 30.0f + 39.0f) * scale;
        if (Float.isNaN(sidebarX)) {
            sidebarX = Math.max(10.0f * scale, contentX - width - 14.0f * scale);
            sidebarY = (displayHeight - height) / 2.0f;
        }

        drawGlass(draw, sidebarX, sidebarY, width, height, 13.0f * scale, true);
        for (int index = 0; index < 3; index++) {
            draw.addCircleFilled(sidebarX + (17.0f + index * 4.0f) * scale,
                    sidebarY + 6.5f * scale, 0.9f * scale, rgba(255, 255, 255, 100));
        }
        hitbox("sidebar-grip", sidebarX, sidebarY, width, gripHeight);
        if (ImGui.isItemActive() && ImGui.isMouseDragging(ImGuiMouseButton.Left)) {
            sidebarX += ImGui.getIO().getMouseDeltaX();
            sidebarY += ImGui.getIO().getMouseDeltaY();
            sidebarX = clamp(sidebarX, 4.0f * scale, displayWidth - width - 4.0f * scale);
            sidebarY = clamp(sidebarY, 4.0f * scale, displayHeight - height - 4.0f * scale);
        }

        float logoX = sidebarX + 8.0f * scale;
        float logoY = sidebarY + 16.0f * scale;
        drawNavSelection(draw, logoX, logoY, page == Page.SETTINGS);
        drawGradientRect(draw, logoX + scale, logoY + scale, 24.0f * scale, 24.0f * scale,
                8.0f * scale, accent(205), accent2(205));
        drawTextCentered(draw, icons, 12.0f * scale, logoX + 13.0f * scale,
                logoY + 6.0f * scale, rgba(255, 255, 255, 255), "g");
        if (hitbox("nav-settings", logoX, logoY, 26.0f * scale, 24.0f * scale)) page = Page.SETTINGS;

        for (int index = 0; index < NAVIGATION.length; index++) {
            Page candidate = NAVIGATION[index];
            float itemX = sidebarX + 8.0f * scale;
            float itemY = sidebarY + (51.0f + index * 30.0f) * scale;
            drawNavSelection(draw, itemX, itemY, page == candidate);
            drawTextCentered(draw, icons, 12.0f * scale, itemX + 13.0f * scale,
                    itemY + 6.0f * scale,
                    page == candidate ? rgba(255, 255, 255, 255) : rgba(203, 210, 226, 205),
                    candidate.icon);
            if (hitbox("nav-" + candidate.name(), itemX, itemY, 26.0f * scale, 24.0f * scale)) {
                page = candidate;
            }
        }

        float hudX = sidebarX + 8.0f * scale;
        float hudY = sidebarY + height - 32.0f * scale;
        drawGradientRect(draw, hudX, hudY, 26.0f * scale, 24.0f * scale,
                7.0f * scale, accent(165), accent2(165));
        drawTextCentered(draw, icons, 12.0f * scale, hudX + 13.0f * scale,
                hudY + 6.0f * scale, rgba(255, 255, 255, 255), "?");
        if (hitbox("nav-hud", hudX, hudY, 26.0f * scale, 24.0f * scale)) page = Page.HUD_EDITOR;
    }

    private void drawModuleWorkspace(ImDrawList draw, float displayWidth, float displayHeight,
            float contentX, float contentY, float contentWidth) {
        float panelWidth = 132.0f * scale;
        float gap = 7.0f * scale;
        float startX = displayWidth / 2.0f - (panelWidth * 3.0f + gap * 2.0f) / 2.0f;
        float startY = Math.max(8.0f * scale, contentY - 62.0f * scale);
        if (!ghostPanel.initialized) ghostPanel.set(startX, startY, panelWidth);
        if (!blatantPanel.initialized) blatantPanel.set(startX + panelWidth + gap, startY, panelWidth);
        if (!otherPanel.initialized) otherPanel.set(startX + (panelWidth + gap) * 2.0f, startY, panelWidth);

        drawModulePanel(draw, ghostPanel, modules.getModules(ModernCategory.GHOST), displayWidth, displayHeight);
        drawModulePanel(draw, blatantPanel, modules.getModules(ModernCategory.BLATANT), displayWidth, displayHeight);
        List<ModernModule> others = new ArrayList<ModernModule>();
        for (ModernModule module : modules.getModules()) {
            if (module.getCategory() != ModernCategory.GHOST
                    && module.getCategory() != ModernCategory.BLATANT) others.add(module);
        }
        drawModulePanel(draw, otherPanel, others, displayWidth, displayHeight);
    }

    private void drawModulePanel(ImDrawList draw, Panel panel, List<ModernModule> entries,
            float displayWidth, float displayHeight) {
        float headerHeight = 28.0f * scale;
        float rowHeight = 25.0f * scale;
        float settingsHeight = 31.0f * scale;
        float naturalBodyHeight = Math.max(42.0f * scale,
                entries.size() * rowHeight + expandedSettingsHeight(entries, settingsHeight));
        float openProgress = animation("panel-open-" + panel.title, panel.open ? 1.0f : 0.0f, 14.0f);
        float bodyHeight = naturalBodyHeight * easeOut(openProgress);
        bodyHeight = Math.min(bodyHeight, displayHeight - panel.y - headerHeight - 8.0f * scale);
        float totalHeight = headerHeight + bodyHeight;

        drawShadow(draw, panel.x, panel.y, panel.width, totalHeight, 9.0f * scale);
        draw.addRectFilled(panel.x, panel.y, panel.x + panel.width, panel.y + totalHeight,
                rgba(9, 13, 24, 62), 8.0f * scale);
        draw.addRect(panel.x + 0.5f, panel.y + 0.5f, panel.x + panel.width - 0.5f,
                panel.y + totalHeight - 0.5f, rgba(255, 255, 255, 22), 8.0f * scale, 0, 0.55f * scale);
        drawText(draw, semibold, 10.5f * scale, panel.x + 10.0f * scale,
                panel.y + 8.0f * scale, rgba(255, 255, 255, 255), panel.title);

        String count = String.valueOf(entries.size());
        float badgeWidth = Math.max(17.0f * scale, textWidth(semibold, 7.5f * scale, count) + 9.0f * scale);
        float arrowX = panel.x + panel.width - 13.0f * scale;
        draw.addRectFilled(arrowX - badgeWidth - 7.0f * scale, panel.y + 7.0f * scale,
                arrowX - 7.0f * scale, panel.y + 21.0f * scale, rgba(9, 13, 24, 90), 7.0f * scale);
        drawTextCentered(draw, semibold, 7.5f * scale,
                arrowX - 7.0f * scale - badgeWidth / 2.0f, panel.y + 10.0f * scale,
                rgba(255, 255, 255, 255), count);
        drawTextCentered(draw, icons, 8.0f * scale, arrowX, panel.y + 9.0f * scale,
                rgba(255, 255, 255, 255), panel.open ? "K" : "J");

        hitbox("panel-header-" + panel.title, panel.x, panel.y, panel.width, headerHeight);
        if (ImGui.isItemActive() && ImGui.isMouseDragging(ImGuiMouseButton.Left)) {
            panel.x += ImGui.getIO().getMouseDeltaX();
            panel.y += ImGui.getIO().getMouseDeltaY();
            panel.x = clamp(panel.x, 8.0f * scale, displayWidth - panel.width - 8.0f * scale);
            panel.y = clamp(panel.y, 8.0f * scale, displayHeight - headerHeight - 8.0f * scale);
            panel.dragged = true;
        }
        if (ImGui.isItemDeactivated()) {
            if (!panel.dragged) panel.open = !panel.open;
            panel.dragged = false;
        }
        if (bodyHeight <= 0.5f) return;

        float maxScroll = Math.max(0.0f, naturalBodyHeight - bodyHeight);
        if (ImGui.isMouseHoveringRect(panel.x, panel.y + headerHeight,
                panel.x + panel.width, panel.y + totalHeight)) {
            panel.scroll = clamp(panel.scroll - ImGui.getIO().getMouseWheel() * 34.0f * scale,
                    0.0f, maxScroll);
        } else {
            panel.scroll = clamp(panel.scroll, 0.0f, maxScroll);
        }

        draw.pushClipRect(panel.x, panel.y + headerHeight, panel.x + panel.width,
                panel.y + totalHeight, true);
        float clipTop = panel.y + headerHeight;
        float clipBottom = panel.y + totalHeight;
        float rowY = clipTop - panel.scroll;
        for (ModernModule module : entries) {
            boolean rowVisible = rowY + rowHeight >= clipTop && rowY <= clipBottom;
            if (rowVisible) {
            boolean hovered = ImGui.isMouseHoveringRect(panel.x, rowY, panel.x + panel.width, rowY + rowHeight);
            float hover = animation("hover-" + panel.title + "-" + module.getId(), hovered ? 1.0f : 0.0f, 18.0f);
            if (hover > 0.01f) {
                draw.addRectFilled(panel.x + 3.0f * scale, rowY + 2.0f * scale,
                        panel.x + panel.width - 3.0f * scale, rowY + rowHeight - 2.0f * scale,
                        rgba(45, 52, 70, Math.round(82.0f * hover)), 5.0f * scale);
            }
            draw.addLine(panel.x + 7.0f * scale, rowY + rowHeight - 0.6f * scale,
                    panel.x + panel.width - 7.0f * scale, rowY + rowHeight - 0.6f * scale,
                    rgba(127, 135, 155, 22), 0.6f * scale);
            float enabled = animation("enabled-" + module.getId(), module.isEnabled() ? 1.0f : 0.0f, 16.0f);
            if (enabled > 0.01f) {
                drawGradientRect(draw, panel.x + 4.0f * scale, rowY + 5.0f * scale,
                        3.0f * scale, 15.0f * scale, 1.5f * scale,
                        accent(Math.round(255.0f * enabled)), accent2(Math.round(255.0f * enabled)));
            }
            drawText(draw, medium, 9.0f * scale, panel.x + 12.0f * scale, rowY + 8.0f * scale,
                    module.isEnabled() ? rgba(239, 242, 250, 255) : rgba(183, 190, 207, 220),
                    module.getName());
            float stateX = panel.x + panel.width - 11.0f * scale;
            String bindName = keyName(module.getKeybind());
            if (!bindName.isEmpty()) {
                float bindWidth = textWidth(medium, 7.2f * scale, bindName);
                drawText(draw, medium, 7.2f * scale, stateX - bindWidth - 8.0f * scale,
                        rowY + 8.8f * scale, rgba(183, 190, 207, 190), bindName);
            }
            draw.addCircleFilled(stateX, rowY + 12.5f * scale, 3.2f * scale,
                    blend(rgba(80, 88, 108, 140), accent(255), enabled));

            hitbox("module-" + panel.title + "-" + module.getId(), panel.x, rowY, panel.width, rowHeight);
            if (ImGui.isItemClicked(ImGuiMouseButton.Left)) modules.toggle(module);
            if (ImGui.isItemClicked(ImGuiMouseButton.Right) && !module.getSettings().isEmpty()) {
                if (!expanded.add(module.getId())) expanded.remove(module.getId());
            }
            if (ImGui.isItemClicked(ImGuiMouseButton.Middle)) {
                bindingSetting = null;
                bindingModule = module;
            }
            }
            rowY += rowHeight;
            if (expanded.contains(module.getId())) {
                for (ModernSetting<?> setting : module.getSettings()) {
                    if (rowY + settingsHeight >= clipTop && rowY <= clipBottom) {
                        drawSetting(draw, panel, module, setting, rowY, settingsHeight);
                    }
                    rowY += settingsHeight;
                }
            }
        }
        if (maxScroll > 0.5f) {
            float trackTop = clipTop + 4.0f * scale;
            float trackHeight = Math.max(8.0f * scale, bodyHeight - 8.0f * scale);
            float thumbHeight = Math.max(18.0f * scale, trackHeight * bodyHeight / naturalBodyHeight);
            float thumbY = trackTop + (trackHeight - thumbHeight) * panel.scroll / maxScroll;
            draw.addRectFilled(panel.x + panel.width - 3.0f * scale, thumbY,
                    panel.x + panel.width - 1.2f * scale, thumbY + thumbHeight,
                    accent(155), scale);
        }
        draw.popClipRect();
    }

    private void drawSetting(ImDrawList draw, Panel panel, ModernModule module,
            ModernSetting<?> setting, float y, float height) {
        draw.addRectFilled(panel.x + 6.0f * scale, y + scale, panel.x + 7.2f * scale,
                y + height - scale, rgba(255, 255, 255, 145));
        drawText(draw, medium, 8.2f * scale, panel.x + 12.0f * scale,
                y + 10.0f * scale, rgba(190, 198, 216, 235), setting.getName());
        if (setting instanceof BooleanSetting) {
            BooleanSetting value = (BooleanSetting) setting;
            float toggleX = panel.x + panel.width - 36.0f * scale;
            float toggleY = y + 8.0f * scale;
            float toggle = animation("setting-toggle-" + module.getId() + "-" + setting.getKey(),
                    value.getValue() ? 1.0f : 0.0f, 18.0f);
            draw.addRectFilled(toggleX, toggleY, toggleX + 26.0f * scale, toggleY + 14.0f * scale,
                    blend(rgba(69, 77, 96, 170), accent(220), toggle), 7.0f * scale);
            draw.addCircleFilled(toggleX + (7.0f + toggle * 12.0f) * scale,
                    toggleY + 7.0f * scale, 4.5f * scale, rgba(255, 255, 255, 255));
            if (hitbox("setting-" + module.getId() + "-" + setting.getKey(), toggleX, toggleY,
                    26.0f * scale, 14.0f * scale)) {
                value.setValue(!value.getValue());
                modules.save();
            }
        } else if (setting instanceof NumberSetting) {
            NumberSetting value = (NumberSetting) setting;
            float barX = panel.x + panel.width - 57.0f * scale;
            float barY = y + 14.0f * scale;
            float barWidth = 47.0f * scale;
            float ratio = (float) ((value.getValue() - value.getMinimum())
                    / (value.getMaximum() - value.getMinimum()));
            draw.addRectFilled(barX, barY, barX + barWidth, barY + 3.0f * scale,
                    rgba(61, 69, 88, 190), 1.5f * scale);
            draw.addRectFilled(barX, barY, barX + barWidth * ratio, barY + 3.0f * scale,
                    accent(240), 1.5f * scale);
            draw.addCircleFilled(barX + barWidth * ratio, barY + 1.5f * scale,
                    3.4f * scale, rgba(255, 255, 255, 255));
            hitbox("setting-" + module.getId() + "-" + setting.getKey(), barX - 3.0f * scale,
                    barY - 6.0f * scale, barWidth + 6.0f * scale, 15.0f * scale);
            if (ImGui.isItemActive() && ImGui.isMouseDown(ImGuiMouseButton.Left)) {
                float nextRatio = clamp((ImGui.getMousePosX() - barX) / barWidth, 0.0f, 1.0f);
                value.setValue(value.getMinimum() + (value.getMaximum() - value.getMinimum()) * nextRatio);
                modules.save();
            }
        } else if (setting instanceof ComboSetting) {
            ComboSetting value = (ComboSetting) setting;
            float valueWidth = textWidth(medium, 7.8f * scale, value.getValue());
            float valueX = panel.x + panel.width - valueWidth - 12.0f * scale;
            drawText(draw, medium, 7.8f * scale, valueX, y + 10.0f * scale,
                    rgba(226, 231, 243, 240), value.getValue());
            if (hitbox("setting-" + module.getId() + "-" + setting.getKey(),
                    panel.x + panel.width - 68.0f * scale, y + 5.0f * scale,
                    58.0f * scale, 21.0f * scale)) {
                value.next();
                modules.save();
            }
        } else if (setting instanceof ColorSetting) {
            ColorSetting value = (ColorSetting) setting;
            int rgb = value.getValue();
            float swatchX = panel.x + panel.width - 34.0f * scale;
            float swatchY = y + 7.0f * scale;
            draw.addRectFilled(swatchX, swatchY, swatchX + 24.0f * scale,
                    swatchY + 17.0f * scale,
                    rgba((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255), 5.0f * scale);
            draw.addRect(swatchX, swatchY, swatchX + 24.0f * scale,
                    swatchY + 17.0f * scale, rgba(255, 255, 255, 65), 5.0f * scale);
        } else if (setting instanceof KeybindSetting) {
            KeybindSetting value = (KeybindSetting) setting;
            String label = bindingSetting == value ? "Press key..." : keyName(value.getValue());
            if (label.isEmpty()) label = "None";
            float labelWidth = textWidth(medium, 7.6f * scale, label);
            float boxWidth = Math.max(42.0f * scale, labelWidth + 12.0f * scale);
            float boxX = panel.x + panel.width - boxWidth - 10.0f * scale;
            float boxY = y + 6.0f * scale;
            draw.addRectFilled(boxX, boxY, boxX + boxWidth, boxY + 19.0f * scale,
                    rgba(47, 55, 74, 185), 5.0f * scale);
            drawTextCentered(draw, medium, 7.6f * scale, boxX + boxWidth / 2.0f,
                    boxY + 5.0f * scale, rgba(235, 239, 248, 245), label);
            if (hitbox("setting-" + module.getId() + "-" + setting.getKey(),
                    boxX, boxY, boxWidth, 19.0f * scale)) {
                bindingModule = null;
                bindingSetting = value;
            }
        }
    }

    private void drawBindingPrompt(ImDrawList draw, float displayWidth, float displayHeight) {
        String target = bindingModule != null ? bindingModule.getName()
                : bindingSetting != null ? bindingSetting.getName() : null;
        if (target == null) return;
        String message = "Press a key for " + target + "  -  Delete to clear";
        float width = Math.max(250.0f * scale, textWidth(medium, 9.0f * scale, message) + 28.0f * scale);
        float height = 30.0f * scale;
        float x = (displayWidth - width) / 2.0f;
        float y = displayHeight - 49.0f * scale;
        drawShadow(draw, x, y, width, height, 8.0f * scale);
        draw.addRectFilled(x, y, x + width, y + height, rgba(9, 13, 24, 188), 8.0f * scale);
        draw.addRect(x, y, x + width, y + height, accent(205), 8.0f * scale, 0, 0.8f * scale);
        drawTextCentered(draw, medium, 9.0f * scale, x + width / 2.0f,
                y + 10.0f * scale, rgba(239, 242, 250, 255), message);
    }

    private String keyName(int key) {
        if (key < 0) return "";
        if (key >= 65 && key <= 90) return String.valueOf((char) key);
        if (key >= 48 && key <= 57) return String.valueOf((char) key);
        if (key >= 290 && key <= 301) return "F" + (key - 289);
        switch (key) {
            case 32: return "SPACE";
            case 258: return "TAB";
            case 259: return "BACK";
            case 260: return "INS";
            case 261: return "DEL";
            case 262: return "RIGHT";
            case 263: return "LEFT";
            case 264: return "DOWN";
            case 265: return "UP";
            case 340: return "LSHIFT";
            case 341: return "LCTRL";
            case 342: return "LALT";
            case 344: return "RSHIFT";
            case 345: return "RCTRL";
            case 346: return "RALT";
            default: return "KEY " + key;
        }
    }

    private void drawContentPage(ImDrawList draw, float x, float y, float width, float height) {
        if (page != Page.HOME) {
            drawTextCentered(draw, semibold, 12.5f * scale, x + width / 2.0f,
                    y + 7.0f * scale, rgba(235, 239, 251, 230), page.title);
        }
        if (page == Page.HOME) {
            drawHome(draw, x, y);
        } else if (page == Page.SETTINGS) {
            drawSettings(draw, x, y, width);
        } else if (page == Page.HUD_EDITOR) {
            drawEmptyPage(draw, x, y, width, height, "HUD Editor", "HUD element positioning is available from the module workspace.");
        } else {
            drawEmptyPage(draw, x, y, width, height, page.title,
                    "This FlaxClient section is being connected to Minecraft 26.1.2.");
        }
    }

    private void drawHome(ImDrawList draw, float x, float y) {
        float padding = 15.0f * scale;
        float newsWidth = 200.0f * scale;
        float panelHeight = 250.0f * scale;
        float devX = x + 230.0f * scale;
        draw.addRectFilled(x + padding, y + padding, x + padding + newsWidth,
                y + padding + panelHeight, rgba(10, 14, 25, 165), 8.0f * scale);
        draw.addRectFilled(devX, y + padding, devX + 174.0f * scale,
                y + padding + panelHeight, rgba(10, 14, 25, 165), 8.0f * scale);
        drawText(draw, semibold, 11.0f * scale, x + padding + 8.0f * scale,
                y + 23.0f * scale, rgba(239, 242, 250, 255), "News");
        drawText(draw, semibold, 11.0f * scale, devX + 8.0f * scale,
                y + 23.0f * scale, rgba(239, 242, 250, 255), "Devlog");
        drawText(draw, semibold, 10.0f * scale, x + padding + 8.0f * scale,
                y + 45.0f * scale, rgba(230, 234, 245, 255), "FlaxClient on Lunar 26.1.2");
        drawText(draw, regular, 8.5f * scale, x + padding + 8.0f * scale,
                y + 64.0f * scale, rgba(177, 186, 205, 235), "The original floating workspace is now active.");
        drawText(draw, medium, 8.5f * scale, devX + 28.0f * scale,
                y + 45.0f * scale, rgba(190, 198, 216, 235), "Legacy ClickGUI layout restored");
        draw.addCircleFilled(devX + 14.5f * scale, y + 49.0f * scale,
                6.5f * scale, accent(220));
        drawTextCentered(draw, icons, 7.0f * scale, devX + 14.5f * scale,
                y + 45.0f * scale, rgba(255, 255, 255, 255), "I");
    }

    private void drawSettings(ImDrawList draw, float x, float y, float width) {
        String[] names = {"General", "Appearance", "Language"};
        String[] descriptions = {"Client behavior and controls", "Theme, blur and accent colors", "Client display language"};
        String[] glyphs = {":", "i", "-"};
        float cardWidth = 185.0f * scale;
        float total = names.length * cardWidth + (names.length - 1) * 10.0f * scale;
        float startX = x + (width - total) / 2.0f;
        for (int i = 0; i < names.length; i++) {
            float cardX = startX + i * (cardWidth + 10.0f * scale);
            float cardY = y + 54.0f * scale;
            draw.addRectFilled(cardX, cardY, cardX + cardWidth, cardY + 88.0f * scale,
                    rgba(10, 14, 25, 152), 9.0f * scale);
            draw.addRect(cardX, cardY, cardX + cardWidth, cardY + 88.0f * scale,
                    rgba(255, 255, 255, 28), 9.0f * scale);
            drawGradientRect(draw, cardX + 12.0f * scale, cardY + 12.0f * scale,
                    28.0f * scale, 28.0f * scale, 8.0f * scale, accent(205), accent2(205));
            drawTextCentered(draw, icons, 12.0f * scale, cardX + 26.0f * scale,
                    cardY + 18.0f * scale, rgba(255, 255, 255, 255), glyphs[i]);
            drawText(draw, semibold, 10.0f * scale, cardX + 12.0f * scale,
                    cardY + 49.0f * scale, rgba(239, 242, 250, 255), names[i]);
            drawText(draw, regular, 7.8f * scale, cardX + 12.0f * scale,
                    cardY + 67.0f * scale, rgba(174, 183, 202, 225), descriptions[i]);
        }
    }

    private void drawEmptyPage(ImDrawList draw, float x, float y, float width, float height,
            String title, String detail) {
        drawTextCentered(draw, semibold, 16.0f * scale, x + width / 2.0f,
                y + height / 2.0f - 18.0f * scale, rgba(239, 242, 250, 255), title);
        drawTextCentered(draw, regular, 8.5f * scale, x + width / 2.0f,
                y + height / 2.0f + 8.0f * scale, rgba(174, 183, 202, 225), detail);
    }

    private float expandedSettingsHeight(List<ModernModule> entries, float settingHeight) {
        float result = 0.0f;
        for (ModernModule module : entries) {
            if (expanded.contains(module.getId())) result += module.getSettings().size() * settingHeight;
        }
        return result;
    }

    private void drawNavSelection(ImDrawList draw, float x, float y, boolean selected) {
        float selectedProgress = animation("nav-" + Math.round(x) + "-" + Math.round(y),
                selected ? 1.0f : 0.0f, 16.0f);
        if (selectedProgress <= 0.01f) return;
        drawShadow(draw, x, y, 26.0f * scale, 24.0f * scale, 7.0f * scale);
        drawGradientRect(draw, x, y, 26.0f * scale, 24.0f * scale,
                7.0f * scale, accent(Math.round(190.0f * selectedProgress)),
                accent2(Math.round(190.0f * selectedProgress)));
    }

    private void drawGlass(ImDrawList draw, float x, float y, float width, float height,
            float radius, boolean stronger) {
        drawShadow(draw, x, y, width, height, radius);
        draw.addRectFilled(x, y, x + width, y + height,
                rgba(9, 13, 24, stronger ? 148 : 118), radius);
        draw.addRect(x + 0.5f, y + 0.5f, x + width - 0.5f, y + height - 0.5f,
                rgba(255, 255, 255, stronger ? 48 : 36), radius, 0, 0.8f * scale);
    }

    private void drawShadow(ImDrawList draw, float x, float y, float width, float height, float radius) {
        for (int index = 3; index >= 1; index--) {
            float spread = index * 2.0f * scale;
            draw.addRect(x - spread, y - spread, x + width + spread, y + height + spread,
                    rgba(0, 0, 0, 11), radius + spread, 0, scale);
        }
    }

    private void drawGradientRect(ImDrawList draw, float x, float y, float width, float height,
            float radius, int first, int second) {
        // addRectFilledMultiColor has no rounded-corner mask and used to leave
        // sharp blue corners on the right edge. Layer complete rounded shapes
        // under progressively narrower clips so every outer edge stays curved.
        draw.addRectFilled(x, y, x + width, y + height, first, radius);
        int secondAlpha = (second >>> 24) & 0xFF;
        for (int step = 1; step <= 4; step++) {
            float clipX = x + width * step / 5.0f;
            draw.pushClipRect(clipX, y, x + width, y + height, true);
            draw.addRectFilled(x, y, x + width, y + height,
                    withAlpha(second, Math.max(1, secondAlpha / 4)), radius);
            draw.popClipRect();
        }
    }

    private float animation(String key, float target, float speed) {
        Float stored = animations.get(key);
        float value = stored == null ? target : stored.floatValue();
        value += (target - value) * Math.min(1.0f, frameDelta * speed);
        animations.put(key, value);
        return value;
    }

    private float approach(float value, float target, float speed) {
        return value + (target - value) * Math.min(1.0f, frameDelta * speed);
    }

    private static float easeOut(float value) {
        float inverse = 1.0f - clamp(value, 0.0f, 1.0f);
        return 1.0f - inverse * inverse * inverse;
    }

    private static int blend(int from, int to, float amount) {
        amount = clamp(amount, 0.0f, 1.0f);
        int fr = from & 255;
        int fg = (from >> 8) & 255;
        int fb = (from >> 16) & 255;
        int fa = (from >>> 24) & 255;
        int tr = to & 255;
        int tg = (to >> 8) & 255;
        int tb = (to >> 16) & 255;
        int ta = (to >>> 24) & 255;
        return rgba(Math.round(fr + (tr - fr) * amount), Math.round(fg + (tg - fg) * amount),
                Math.round(fb + (tb - fb) * amount), Math.round(fa + (ta - fa) * amount));
    }

    private boolean hitbox(String id, float x, float y, float width, float height) {
        ImGui.setCursorScreenPos(x, y);
        return ImGui.invisibleButton("##" + id, width, height);
    }

    private void drawText(ImDrawList draw, ImFont font, float size, float x, float y, int color, String text) {
        if (font == null) draw.addText(x, y, color, text);
        else draw.addText(font, Math.max(1, Math.round(size)), x, y, color, text);
    }

    private void drawTextCentered(ImDrawList draw, ImFont font, float size, float centerX,
            float y, int color, String text) {
        drawText(draw, font, size, centerX - textWidth(font, size, text) / 2.0f, y, color, text);
    }

    private float textWidth(ImFont font, float size, String text) {
        return font == null ? text.length() * size * 0.5f
                : font.calcTextSizeAX(size, Float.MAX_VALUE, 0.0f, text);
    }

    private int accent(int alpha) {
        float[] color = modules.getAccentColor();
        return rgba(Math.round(color[0] * 255), Math.round(color[1] * 255),
                Math.round(color[2] * 255), alpha);
    }

    private int accent2(int alpha) {
        float[] color = modules.getAccentColor();
        return rgba(Math.round(Math.min(1.0f, color[0] + 0.12f) * 255),
                Math.round(Math.min(1.0f, color[1] + 0.08f) * 255),
                Math.round(Math.max(0.0f, color[2] - 0.12f) * 255), alpha);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private static int rgba(int red, int green, int blue, int alpha) {
        return ((alpha & 0xFF) << 24) | ((blue & 0xFF) << 16)
                | ((green & 0xFF) << 8) | (red & 0xFF);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private enum Page {
        HOME("Home", "a"),
        MODULES("Module", "C"),
        YOUTUBE("YouTube", "@"),
        COSMETICS("Cosmetics", "#"),
        GAMES("Games", "'"),
        PROFILE("Profile", "U"),
        SCREENSHOTS("Screenshot", "v"),
        SETTINGS("Settings", "3"),
        HUD_EDITOR("HUD Editor", "?");

        private final String title;
        private final String icon;

        Page(String title, String icon) {
            this.title = title;
            this.icon = icon;
        }
    }

    private static final class Panel {
        private final String title;
        private boolean initialized;
        private boolean open = true;
        private boolean dragged;
        private float x;
        private float y;
        private float width;
        private float scroll;

        private Panel(String title) {
            this.title = title;
        }

        private void set(float x, float y, float width) {
            this.x = x;
            this.y = y;
            this.width = width;
            initialized = true;
        }
    }
}
