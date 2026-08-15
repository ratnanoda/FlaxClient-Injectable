package me.eldodebug.soar.gui.modmenu.category.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.lwjgl.input.Keyboard;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.modmenu.GuiModMenu;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.Setting;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.ColorSetting;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.ImageSetting;
import me.eldodebug.soar.management.mods.settings.impl.KeybindSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.mods.settings.impl.SoundSetting;
import me.eldodebug.soar.management.mods.settings.impl.TextSetting;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.ui.comp.Comp;
import me.eldodebug.soar.ui.comp.impl.CompColorPicker;
import me.eldodebug.soar.ui.comp.impl.CompComboBox;
import me.eldodebug.soar.ui.comp.impl.CompImageSelect;
import me.eldodebug.soar.ui.comp.impl.CompKeybind;
import me.eldodebug.soar.ui.comp.impl.CompSlider;
import me.eldodebug.soar.ui.comp.impl.CompSoundSelect;
import me.eldodebug.soar.ui.comp.impl.CompToggleButton;
import me.eldodebug.soar.ui.comp.impl.field.CompModTextBox;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.SearchUtils;
import me.eldodebug.soar.utils.mouse.MouseUtils;

/**
 * Main module workspace. Ghost uses the native ModuleCategory panel while
 * Other is an independent draggable and resizable module list.
 */
public class GhostCategory extends ModuleCategory {

    private static final float DEFAULT_PANEL_WIDTH = 132.0F;
    private static final float PANEL_GAP = 7.0F;
    private static final float HEADER_HEIGHT = 28.0F;
    private static final float ROW_HEIGHT = 25.0F;
    private static final float OPTION_HEIGHT = 31.0F;
    private static final float DEFAULT_SECTION_OFFSET_Y = -62.0F;
    private static final float SCREEN_EDGE_MARGIN = 8.0F;
    private static final float MIN_RESIZABLE_WIDTH = 132.0F;
    private static final float MIN_RESIZABLE_BODY_HEIGHT = ROW_HEIGHT + OPTION_HEIGHT;
    private static final float RESIZE_EDGE = 5.0F;

    private final AuxiliaryPanel renderPanel;
    private final AuxiliaryPanel otherPanel;
    private AuxiliaryPanel settingsPanel;
    private Mod openSettingsMod;
    private final ArrayList<ModuleSetting> settings = new ArrayList<ModuleSetting>();
    private Mod bindingMod;

    public GhostCategory(GuiModMenu parent) {
        super(parent, TranslateText.MODULE, LegacyIcon.ARCHIVE, Fonts.LEGACYICON, ModCategory.GHOST, false);
        renderPanel = new AuxiliaryPanel("Render", ModCategory.RENDER, -1);
        otherPanel = new AuxiliaryPanel("Other", ModCategory.OTHER, 1);
    }

    @Override
    public void initGui() {
        super.initGui();
        resetOtherPanel();
    }

    @Override
    public void initCategory() {
        super.initCategory();
        resetOtherPanel();
    }

    private void resetOtherPanel() {
        renderPanel.reset();
        otherPanel.reset();
        settingsPanel = null;
        openSettingsMod = null;
        bindingMod = null;
        setCanClose(true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawPanel(renderPanel, mouseX, mouseY);
        drawPanel(otherPanel, mouseX, mouseY);
        if(bindingMod != null) {
            drawBindingPrompt();
        }
    }

    private void drawPanel(AuxiliaryPanel panel, int mouseX, int mouseY) {
        initializePanel(panel);

        if(panel.resizing) {
            updateResize(panel, mouseX, mouseY);
        } else if(panel.dragging) {
            panel.offsetX = mouseX - getX() - panel.dragOffsetX;
            panel.offsetY = mouseY - getY() - panel.dragOffsetY;
            panel.moved = panel.moved
                    || Math.abs(mouseX - panel.pressMouseX) > 3.0F
                    || Math.abs(mouseY - panel.pressMouseY) > 3.0F;
            clampPanel(panel);
        }

        List<Mod> modules = getVisibleModules(panel.category);
        if(settingsPanel == panel && (openSettingsMod == null || !modules.contains(openSettingsMod))) {
            closeInlineSettings();
        }

        Glide instance = Glide.getInstance();
        NanoVGManager nvg = instance.getNanoVGManager();
        ColorPalette palette = instance.getColorManager().getPalette();
        AccentColor accent = instance.getColorManager().getCurrentColor();

        float panelX = getPanelX(panel);
        float panelY = getPanelY(panel);
        float contentHeight = modules.isEmpty() ? 34.0F : modules.size() * ROW_HEIGHT;
        if(settingsPanel == panel && openSettingsMod != null) {
            contentHeight += getInlineSettingsHeight();
        }

        float maxBodyHeight = Math.max(MIN_RESIZABLE_BODY_HEIGHT,
                getScreenHeight() - panelY - SCREEN_EDGE_MARGIN - HEADER_HEIGHT);
        float expandedBodyHeight = panel.bodyHeightOverride > 0.0F
                ? Math.max(MIN_RESIZABLE_BODY_HEIGHT, Math.min(maxBodyHeight, panel.bodyHeightOverride))
                : Math.max(MIN_RESIZABLE_BODY_HEIGHT, contentHeight);
        float bodyHeight = panel.open ? expandedBodyHeight : 0.0F;
        panel.visibleBodyHeight = bodyHeight;
        float totalHeight = HEADER_HEIGHT + bodyHeight;
        boolean headerHovered = MouseUtils.isInside(mouseX, mouseY, panelX, panelY, panel.width, HEADER_HEIGHT);

        nvg.drawShadow(panelX, panelY, panel.width, totalHeight, 9, 3);
        nvg.drawRoundedRect(panelX, panelY, panel.width, totalHeight, 8,
                translucent(palette.getBackgroundColor(ColorType.DARK), 62));
        nvg.drawOutlineRoundedRect(panelX + 0.5F, panelY + 0.5F, panel.width - 1.0F, totalHeight - 1.0F,
                8, 0.55F, new Color(255, 255, 255, headerHovered ? 30 : 18));
        if(headerHovered) {
            nvg.drawRoundedRect(panelX + 3.0F, panelY + 3.0F, panel.width - 6.0F, HEADER_HEIGHT - 5.0F, 6,
                    translucent(palette.getBackgroundColor(ColorType.NORMAL), 34));
        }

        nvg.drawText(panel.title, panelX + 10.0F, panelY + 8.5F, Color.WHITE, 10.5F, Fonts.SEMIBOLD);
        String count = String.valueOf(modules.size());
        float countWidth = Math.max(17.0F, nvg.getTextWidth(count, 7.5F, Fonts.SEMIBOLD) + 9.0F);
        panel.lastCountWidth = countWidth;
        float arrowX = panelX + panel.width - 13.0F;
        nvg.drawRoundedRect(arrowX - countWidth - 7.0F, panelY + 7.0F, countWidth, 14.0F, 7.0F,
                new Color(9, 13, 24, 55));
        nvg.drawCenteredText(count, arrowX - 7.0F - countWidth / 2.0F, panelY + 11.0F,
                Color.WHITE, 7.5F, Fonts.SEMIBOLD);

        float resetWidth = 34.0F;
        float resetX = arrowX - countWidth - resetWidth - 12.0F;
        boolean resetHovered = MouseUtils.isInside(mouseX, mouseY, resetX, panelY + 6.0F, resetWidth, 16.0F);
        nvg.drawRoundedRect(resetX, panelY + 6.0F, resetWidth, 16.0F, 6.0F,
                new Color(255, 255, 255, resetHovered ? 40 : 22));
        nvg.drawCenteredText("Reset", resetX + resetWidth / 2.0F, panelY + 10.5F,
                palette.getFontColor(ColorType.NORMAL), 7.0F, Fonts.MEDIUM);
        nvg.drawCenteredText(panel.open ? LegacyIcon.CHEVRON_UP : LegacyIcon.CHEVRON_DOWN,
                arrowX, panelY + 9.0F, Color.WHITE, 8.0F, Fonts.LEGACYICON);

        if(panel.open) {
            drawResizeHandles(nvg, panel, panelX, panelY, totalHeight, mouseX, mouseY);
        }

        if(!panel.open || bodyHeight <= 0.5F) {
            return;
        }

        float bodyY = panelY + HEADER_HEIGHT;
        nvg.save();
        nvg.scissor(panelX, bodyY, panel.width, bodyHeight);

        if(modules.isEmpty()) {
            nvg.drawCenteredText("No modules", panelX + panel.width / 2.0F, bodyY + 12.0F,
                    palette.getFontColor(ColorType.NORMAL), 8.0F, Fonts.REGULAR);
            nvg.restore();
            return;
        }

        float rowY = bodyY;
        for(Mod mod : modules) {
            drawModuleRow(nvg, palette, accent, panel, mod, panelX, rowY, mouseX, mouseY);
            rowY += ROW_HEIGHT;

            if(settingsPanel == panel && openSettingsMod == mod) {
                drawInlineSettings(nvg, palette, panel, panelX, rowY, mouseX, mouseY);
                rowY += getInlineSettingsHeight();
            }
        }

        nvg.restore();
    }

    private void drawModuleRow(NanoVGManager nvg, ColorPalette palette, AccentColor accent,
            AuxiliaryPanel panel, Mod mod, float panelX, float rowY, int mouseX, int mouseY) {
        boolean hovered = MouseUtils.isInside(mouseX, mouseY, panelX, rowY, panel.width, ROW_HEIGHT);
        if(hovered) {
            nvg.drawRoundedRect(panelX + 3.0F, rowY + 2.0F, panel.width - 6.0F, ROW_HEIGHT - 4.0F, 5,
                    translucent(palette.getBackgroundColor(ColorType.NORMAL), 82));
        }

        nvg.drawRect(panelX + 7.0F, rowY + ROW_HEIGHT - 0.6F, panel.width - 14.0F, 0.6F,
                new Color(127, 135, 155, 22));

        mod.getAnimation().setAnimation(mod.isToggled() ? 1.0F : 0.0F, 18);
        float active = mod.getAnimation().getValue();
        nvg.drawGradientRoundedRect(panelX + 4.0F, rowY + 5.0F, 3.0F, 15.0F, 1.5F,
                ColorUtils.applyAlpha(accent.getColor1(), (int) (active * 255.0F)),
                ColorUtils.applyAlpha(accent.getColor2(), (int) (active * 255.0F)));

        String bindName = mod.getKeyCode() == Keyboard.KEY_NONE ? "" : Keyboard.getKeyName(mod.getKeyCode());
        if(bindName == null) {
            bindName = "";
        }
        float bindWidth = bindName.isEmpty() ? 0.0F : nvg.getTextWidth(bindName, 7.5F, Fonts.MEDIUM);
        float reserved = bindName.isEmpty() ? 28.0F : 37.0F + bindWidth;
        String name = nvg.getLimitText(mod.getName(), 9.0F, Fonts.MEDIUM,
                Math.max(28.0F, panel.width - reserved));
        Color nameColor = mod.isToggled()
                ? palette.getFontColor(ColorType.DARK)
                : palette.getFontColor(ColorType.NORMAL);
        nvg.drawText(name, panelX + 12.0F, rowY + 8.0F, nameColor, 9.0F, Fonts.MEDIUM);

        float bindX = panelX + panel.width - 7.0F - bindWidth;
        if(!bindName.isEmpty()) {
            nvg.drawText(bindName, bindX, rowY + 8.8F,
                    palette.getFontColor(ColorType.NORMAL, 180), 7.5F, Fonts.MEDIUM);
        }

        float stateX = bindName.isEmpty() ? panelX + panel.width - 11.0F : bindX - 9.0F;
        nvg.drawCircle(stateX, rowY + 12.5F, 3.2F,
                translucent(palette.getBackgroundColor(ColorType.NORMAL), 140));
        nvg.drawGradientCircle(stateX, rowY + 12.5F, 3.2F,
                ColorUtils.applyAlpha(accent.getColor1(), (int) (active * 255.0F)),
                ColorUtils.applyAlpha(accent.getColor2(), (int) (active * 255.0F)));
    }

    private void drawInlineSettings(NanoVGManager nvg, ColorPalette palette, AuxiliaryPanel panel,
            float panelX, float optionY, int mouseX, int mouseY) {
        float height = getInlineSettingsHeight();
        float cellX = panelX + 12.0F;
        float cellWidth = panel.width - 18.0F;
        nvg.drawRect(panelX + 6.0F, optionY + 1.0F, 1.2F, height - 2.0F,
                new Color(255, 255, 255, 145));
        for(int i = 0; i < settings.size(); i++) {
            ModuleSetting entry = settings.get(i);
            entry.animation.setAnimation(entry.openY, 16);
            float rowY = optionY + 5.0F + i * 31.0F + entry.animation.getValue();
            nvg.drawText(nvg.getLimitText(entry.setting.getName(), 8.5F, Fonts.MEDIUM,
                    Math.max(24.0F, cellWidth - 88.0F)), cellX, rowY + 10.0F,
                    palette.getFontColor(ColorType.NORMAL), 8.5F, Fonts.MEDIUM);
            positionSettingComponent(entry.comp, cellX, rowY, cellWidth);
            entry.comp.draw(mouseX, mouseY, 0.0F);
        }
    }

    private float getInlineSettingsHeight() {
        float greatestShift = 0.0F;
        for(ModuleSetting entry : settings) greatestShift = Math.max(greatestShift, entry.openY);
        return 10.0F + settings.size() * 31.0F + greatestShift;
    }

    private void positionSettingComponent(Comp comp, float cellX, float rowY, float cellWidth) {
        float controlX = cellX + cellWidth - 84.0F;
        if(comp instanceof CompToggleButton) {
            CompToggleButton toggle = (CompToggleButton) comp;
            toggle.setX(cellX + cellWidth - 34.0F); toggle.setY(rowY + 7.0F); toggle.setScale(0.85F);
        } else if(comp instanceof CompSlider) {
            CompSlider slider = (CompSlider) comp;
            slider.setX(controlX); slider.setY(rowY + 13.0F); slider.setWidth(72);
        } else if(comp instanceof CompComboBox || comp instanceof CompKeybind) {
            comp.setX(controlX); comp.setY(rowY + 6.0F);
        } else if(comp instanceof CompModTextBox) {
            CompModTextBox textBox = (CompModTextBox) comp;
            textBox.setX(controlX); textBox.setY(rowY + 6.0F); textBox.setWidth(75); textBox.setHeight(16);
        } else if(comp instanceof CompColorPicker) {
            CompColorPicker picker = (CompColorPicker) comp;
            picker.setX(cellX + cellWidth - 102.0F); picker.setY(rowY + 6.0F); picker.setScale(0.8F);
        } else if(comp instanceof CompImageSelect || comp instanceof CompSoundSelect) {
            comp.setX(cellX + cellWidth - 18.0F); comp.setY(rowY + 6.0F);
        }
    }

    private void drawBindingPrompt() {
        Glide instance = Glide.getInstance();
        NanoVGManager nvg = instance.getNanoVGManager();
        ColorPalette palette = instance.getColorManager().getPalette();
        AccentColor accent = instance.getColorManager().getCurrentColor();
        float promptWidth = 250.0F;
        float promptHeight = 30.0F;
        float promptX = getX() + getWidth() / 2.0F - promptWidth / 2.0F;
        float promptY = getY() + getHeight() - 39.0F;
        String text = "Press a key for " + bindingMod.getName() + "  -  right click to clear";

        nvg.drawShadow(promptX, promptY, promptWidth, promptHeight, 8);
        nvg.drawRoundedRect(promptX, promptY, promptWidth, promptHeight, 8,
                translucent(palette.getBackgroundColor(ColorType.DARK), 145));
        nvg.drawGradientOutlineRoundedRect(promptX, promptY, promptWidth, promptHeight, 8, 0.8F,
                accent.getColor1(), accent.getColor2());
        nvg.drawCenteredText(text, promptX + promptWidth / 2.0F, promptY + 10.5F,
                palette.getFontColor(ColorType.DARK), 9.0F, Fonts.MEDIUM);
    }

    private List<Mod> getVisibleModules(ModCategory category) {
        ArrayList<Mod> result = new ArrayList<Mod>();
        String search = getSearchBox().getText();

        for(Mod mod : Glide.getInstance().getModManager().getMods()) {
            if(mod.isHide() || !mod.getAllowed() || mod.getCategory() != category) {
                continue;
            }
            if(!search.isEmpty()
                    && !SearchUtils.isSimillar(Glide.getInstance().getModManager().getWords(mod), search)) {
                continue;
            }
            result.add(mod);
        }

        Collections.sort(result, new Comparator<Mod>() {
            @Override
            public int compare(Mod first, Mod second) {
                return first.getName().compareToIgnoreCase(second.getName());
            }
        });
        return result;
    }

    private void initializePanel(AuxiliaryPanel panel) {
        if(panel.initialized) {
            return;
        }

        float ghostPanelX = getX() + (getWidth() - DEFAULT_PANEL_WIDTH) / 2.0F;
        panel.offsetX = ghostPanelX
                + panel.position * (DEFAULT_PANEL_WIDTH + PANEL_GAP) - getX();
        panel.offsetY = Math.max(DEFAULT_SECTION_OFFSET_Y, SCREEN_EDGE_MARGIN - getY());
        panel.width = DEFAULT_PANEL_WIDTH;
        panel.bodyHeightOverride = -1.0F;
        panel.initialized = true;
        clampPanel(panel);
    }

    private float getPanelX(AuxiliaryPanel panel) {
        return getX() + panel.offsetX;
    }

    private float getPanelY(AuxiliaryPanel panel) {
        return getY() + panel.offsetY;
    }

    private void resetLayout(AuxiliaryPanel panel) {
        if(settingsPanel == panel) {
            closeInlineSettings();
        }
        panel.reset();
        initializePanel(panel);
    }

    private void clampPanel(AuxiliaryPanel panel) {
        float maxWidth = Math.max(MIN_RESIZABLE_WIDTH, getScreenWidth() - SCREEN_EDGE_MARGIN * 2.0F);
        panel.width = Math.max(MIN_RESIZABLE_WIDTH, Math.min(maxWidth, panel.width));

        float minOffsetX = SCREEN_EDGE_MARGIN - getX();
        float maxOffsetX = getScreenWidth() - SCREEN_EDGE_MARGIN - panel.width - getX();
        panel.offsetX = Math.max(minOffsetX, Math.min(maxOffsetX, panel.offsetX));

        float minOffsetY = SCREEN_EDGE_MARGIN - getY();
        float maxOffsetY = getScreenHeight() - SCREEN_EDGE_MARGIN - HEADER_HEIGHT
                - MIN_RESIZABLE_BODY_HEIGHT - getY();
        panel.offsetY = Math.max(minOffsetY, Math.min(maxOffsetY, panel.offsetY));

        if(panel.bodyHeightOverride > 0.0F) {
            float absoluteY = getY() + panel.offsetY;
            float maxBody = Math.max(MIN_RESIZABLE_BODY_HEIGHT,
                    getScreenHeight() - SCREEN_EDGE_MARGIN - absoluteY - HEADER_HEIGHT);
            panel.bodyHeightOverride = Math.min(panel.bodyHeightOverride, maxBody);
        }
    }

    private void drawResizeHandles(NanoVGManager nvg, AuxiliaryPanel panel,
            float x, float y, float totalHeight, int mouseX, int mouseY) {
        int edges = getResizeEdges(mouseX, mouseY, x, y, panel.width, totalHeight);
        Color edge = new Color(255, 255, 255, edges == 0 ? 28 : 82);
        nvg.drawRect(x + panel.width - 10.0F, y + totalHeight - 2.0F, 8.0F, 1.0F, edge);
        nvg.drawRect(x + panel.width - 2.0F, y + totalHeight - 10.0F, 1.0F, 8.0F, edge);
    }

    private int getResizeEdges(int mouseX, int mouseY, float x, float y, float width, float height) {
        int edges = 0;
        if(mouseX >= x - RESIZE_EDGE && mouseX <= x + RESIZE_EDGE
                && mouseY >= y - RESIZE_EDGE && mouseY <= y + height + RESIZE_EDGE) {
            edges |= 1;
        }
        if(mouseX >= x + width - RESIZE_EDGE && mouseX <= x + width + RESIZE_EDGE
                && mouseY >= y - RESIZE_EDGE && mouseY <= y + height + RESIZE_EDGE) {
            edges |= 2;
        }
        if(mouseY >= y - RESIZE_EDGE && mouseY <= y + RESIZE_EDGE
                && mouseX >= x - RESIZE_EDGE && mouseX <= x + width + RESIZE_EDGE) {
            edges |= 4;
        }
        if(mouseY >= y + height - RESIZE_EDGE && mouseY <= y + height + RESIZE_EDGE
                && mouseX >= x - RESIZE_EDGE && mouseX <= x + width + RESIZE_EDGE) {
            edges |= 8;
        }
        return edges;
    }

    private void beginResize(AuxiliaryPanel panel, int edges, int mouseX, int mouseY) {
        panel.resizing = true;
        panel.resizeEdges = edges;
        panel.resizeStartMouseX = mouseX;
        panel.resizeStartMouseY = mouseY;
        panel.resizeStartOffsetX = panel.offsetX;
        panel.resizeStartOffsetY = panel.offsetY;
        panel.resizeStartWidth = panel.width;
        panel.resizeStartBodyHeight = Math.max(MIN_RESIZABLE_BODY_HEIGHT, panel.visibleBodyHeight);
        panel.bodyHeightOverride = panel.resizeStartBodyHeight;
    }

    private void updateResize(AuxiliaryPanel panel, int mouseX, int mouseY) {
        float dx = mouseX - panel.resizeStartMouseX;
        float dy = mouseY - panel.resizeStartMouseY;
        float maxWidth = Math.max(MIN_RESIZABLE_WIDTH, getScreenWidth() - SCREEN_EDGE_MARGIN * 2.0F);
        float width = panel.resizeStartWidth;
        float offsetX = panel.resizeStartOffsetX;

        if((panel.resizeEdges & 1) != 0) {
            width = Math.max(MIN_RESIZABLE_WIDTH, Math.min(maxWidth, panel.resizeStartWidth - dx));
            offsetX = panel.resizeStartOffsetX + (panel.resizeStartWidth - width);
        } else if((panel.resizeEdges & 2) != 0) {
            width = Math.max(MIN_RESIZABLE_WIDTH, Math.min(maxWidth, panel.resizeStartWidth + dx));
        }

        float bodyHeight = panel.resizeStartBodyHeight;
        float offsetY = panel.resizeStartOffsetY;
        if((panel.resizeEdges & 4) != 0) {
            bodyHeight = Math.max(MIN_RESIZABLE_BODY_HEIGHT, panel.resizeStartBodyHeight - dy);
            offsetY = panel.resizeStartOffsetY + (panel.resizeStartBodyHeight - bodyHeight);
        } else if((panel.resizeEdges & 8) != 0) {
            bodyHeight = Math.max(MIN_RESIZABLE_BODY_HEIGHT, panel.resizeStartBodyHeight + dy);
        }

        panel.width = width;
        panel.bodyHeightOverride = bodyHeight;
        panel.offsetX = offsetX;
        panel.offsetY = offsetY;
        clampPanel(panel);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if(bindingMod != null) {
            if(mouseButton == 1) {
                bindingMod.setKeyCode(Keyboard.KEY_NONE);
                bindingMod = null;
                updateCanClose();
                return;
            }
            if(mouseButton == 2) {
                bindingMod = null;
                updateCanClose();
                return;
            }
        }

        if(handlePanelClick(renderPanel, mouseX, mouseY, mouseButton)
                || handlePanelClick(otherPanel, mouseX, mouseY, mouseButton)) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean handlePanelClick(AuxiliaryPanel panel, int mouseX, int mouseY, int mouseButton) {
        initializePanel(panel);
        List<Mod> modules = getVisibleModules(panel.category);
        float panelX = getPanelX(panel);
        float panelY = getPanelY(panel);
        float totalHeight = HEADER_HEIGHT + Math.max(MIN_RESIZABLE_BODY_HEIGHT, panel.visibleBodyHeight);
        float arrowX = panelX + panel.width - 13.0F;
        float resetWidth = 34.0F;
        float resetX = arrowX - panel.lastCountWidth - resetWidth - 12.0F;

        if(mouseButton == 0
                && MouseUtils.isInside(mouseX, mouseY, resetX, panelY + 6.0F, resetWidth, 16.0F)) {
            resetLayout(panel);
            return true;
        }

        if(mouseButton == 0 && panel.open) {
            int edges = getResizeEdges(mouseX, mouseY, panelX, panelY, panel.width, totalHeight);
            if(edges != 0) {
                beginResize(panel, edges, mouseX, mouseY);
                return true;
            }
        }

        if(mouseButton == 0 && MouseUtils.isInside(mouseX, mouseY, panelX, panelY, panel.width, HEADER_HEIGHT)) {
            panel.dragging = true;
            panel.dragOffsetX = mouseX - panelX;
            panel.dragOffsetY = mouseY - panelY;
            panel.pressMouseX = mouseX;
            panel.pressMouseY = mouseY;
            panel.moved = false;
            return true;
        }

        if(!panel.open) {
            return false;
        }

        float rowY = panelY + HEADER_HEIGHT;
        for(Mod mod : modules) {
            if(MouseUtils.isInside(mouseX, mouseY, panelX, rowY, panel.width, ROW_HEIGHT)) {
                if(mouseButton == 0) {
                    mod.toggle();
                } else if(mouseButton == 1 && hasSettings(mod)) {
                    if(settingsPanel == panel && openSettingsMod == mod) {
                        closeInlineSettings();
                    } else {
                        bindingMod = null;
                        openInlineSettings(panel, mod);
                    }
                } else if(mouseButton == 2) {
                    closeInlineSettings();
                    bindingMod = mod;
                    updateCanClose();
                }
                return true;
            }

            rowY += ROW_HEIGHT;
            if(settingsPanel == panel && openSettingsMod == mod) {
                float inlineHeight = getInlineSettingsHeight();
                if(mouseButton == 0 && MouseUtils.isInside(mouseX, mouseY, panelX, rowY,
                        panel.width, inlineHeight)) {
                    for(ModuleSetting entry : settings) {
                        entry.comp.mouseClicked(mouseX, mouseY, mouseButton);
                        if(entry.comp instanceof CompColorPicker) {
                            CompColorPicker picker = (CompColorPicker) entry.comp;
                            if(picker.isInsideOpen(mouseX, mouseY)) {
                                shiftRowsBelowColorPicker(entry, picker.isOpen()
                                        ? (picker.isShowAlpha() ? 100 : 85)
                                        : -(picker.isShowAlpha() ? 100 : 85));
                            }
                        }
                    }
                    return true;
                }
                rowY += inlineHeight;
            }
        }

        return MouseUtils.isInside(mouseX, mouseY, panelX, panelY,
                panel.width, HEADER_HEIGHT + panel.visibleBodyHeight);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if(openSettingsMod != null && mouseButton == 0) {
            for(ModuleSetting entry : settings) entry.comp.mouseReleased(mouseX, mouseY, mouseButton);
        }
        boolean handled = releasePanel(renderPanel, mouseButton);
        handled = releasePanel(otherPanel, mouseButton) || handled;
        if(!handled) {
            super.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    private boolean releasePanel(AuxiliaryPanel panel, int mouseButton) {
        boolean handled = false;

        if(mouseButton == 0 && panel.resizing) {
            panel.resizing = false;
            panel.resizeEdges = 0;
            handled = true;
        }

        if(mouseButton == 0 && panel.dragging) {
            if(!panel.moved) {
                panel.open = !panel.open;
                if(!panel.open && settingsPanel == panel) {
                    closeInlineSettings();
                }
            }
            panel.dragging = false;
            handled = true;
        }

        return handled;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if(bindingMod != null) {
            if(keyCode == Keyboard.KEY_ESCAPE) {
                bindingMod = null;
                updateCanClose();
                return;
            }
            if(keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK) {
                bindingMod.setKeyCode(Keyboard.KEY_NONE);
                bindingMod = null;
                updateCanClose();
                return;
            }
            if(keyCode != Keyboard.KEY_NONE) {
                bindingMod.setKeyCode(keyCode);
                bindingMod = null;
                updateCanClose();
            }
            return;
        }

        if(openSettingsMod != null && keyCode == Keyboard.KEY_ESCAPE) {
            closeInlineSettings();
            return;
        }
        if(openSettingsMod != null) {
            for(ModuleSetting entry : settings) entry.comp.keyTyped(typedChar, keyCode);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private boolean hasSettings(Mod mod) {
        return Glide.getInstance().getModManager().getSettingsByMod(mod) != null;
    }

    private void openInlineSettings(AuxiliaryPanel panel, Mod mod) {
        ArrayList<Setting> moduleSettings = Glide.getInstance().getModManager().getSettingsByMod(mod);
        if(moduleSettings == null) return;
        settings.clear();
        for(Setting setting : moduleSettings) {
            Comp comp = createComponent(setting);
            if(comp != null) settings.add(new ModuleSetting(setting, comp));
        }
        if(settings.isEmpty()) return;
        getSearchBox().setFocused(false);
        settingsPanel = panel;
        openSettingsMod = mod;
        updateCanClose();
    }

    private Comp createComponent(Setting setting) {
        if(setting instanceof BooleanSetting) return new CompToggleButton((BooleanSetting) setting);
        if(setting instanceof NumberSetting) return new CompSlider((NumberSetting) setting);
        if(setting instanceof ComboSetting) return new CompComboBox(75, (ComboSetting) setting);
        if(setting instanceof ImageSetting) return new CompImageSelect((ImageSetting) setting);
        if(setting instanceof SoundSetting) return new CompSoundSelect((SoundSetting) setting);
        if(setting instanceof KeybindSetting) return new CompKeybind(75, (KeybindSetting) setting);
        if(setting instanceof TextSetting) return new CompModTextBox((TextSetting) setting);
        if(setting instanceof ColorSetting) return new CompColorPicker((ColorSetting) setting);
        return null;
    }

    private void shiftRowsBelowColorPicker(ModuleSetting source, int amount) {
        int sourceIndex = settings.indexOf(source);
        for(int i = sourceIndex + 1; i < settings.size(); i++) settings.get(i).openY += amount;
    }

    private void closeInlineSettings() {
        settingsPanel = null;
        openSettingsMod = null;
        settings.clear();
        updateCanClose();
    }

    private void updateCanClose() {
        setCanClose(bindingMod == null && openSettingsMod == null);
    }

    private Color translucent(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static final class ModuleSetting {
        private final Setting setting;
        private final Comp comp;
        private final me.eldodebug.soar.utils.animation.simple.SimpleAnimation animation =
                new me.eldodebug.soar.utils.animation.simple.SimpleAnimation();
        private float openY;

        private ModuleSetting(Setting setting, Comp comp) {
            this.setting = setting;
            this.comp = comp;
        }
    }

    private static final class AuxiliaryPanel {
        private final String title;
        private final ModCategory category;
        private final int position;

        private boolean open = true;
        private boolean initialized;
        private boolean dragging;
        private boolean resizing;
        private boolean moved;
        private float offsetX;
        private float offsetY;
        private float width = DEFAULT_PANEL_WIDTH;
        private float bodyHeightOverride = -1.0F;
        private float visibleBodyHeight;
        private float lastCountWidth = 17.0F;
        private float dragOffsetX;
        private float dragOffsetY;
        private float pressMouseX;
        private float pressMouseY;
        private int resizeEdges;
        private float resizeStartMouseX;
        private float resizeStartMouseY;
        private float resizeStartOffsetX;
        private float resizeStartOffsetY;
        private float resizeStartWidth;
        private float resizeStartBodyHeight;

        private AuxiliaryPanel(String title, ModCategory category, int position) {
            this.title = title;
            this.category = category;
            this.position = position;
        }

        private void reset() {
            open = true;
            initialized = false;
            dragging = false;
            resizing = false;
            moved = false;
            width = DEFAULT_PANEL_WIDTH;
            bodyHeightOverride = -1.0F;
            visibleBodyHeight = 0.0F;
            lastCountWidth = 17.0F;
            resizeEdges = 0;
        }
    }
}
