package me.eldodebug.soar.gui.modmenu.category.impl;

import java.awt.Color;

import org.lwjgl.input.Keyboard;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.modmenu.GuiModMenu;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.impl.SettingsMod;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.SearchUtils;
import me.eldodebug.soar.utils.mouse.MouseUtils;

/**
 * Main module workspace. The historical Ghost navigation entry now presents
 * itself as the regular module list, with a separate Other panel beside the
 * Ghost panel for global client settings.
 */
public class GhostCategory extends ModuleCategory {

	private static final float PANEL_WIDTH = 132.0F;
	private static final float PANEL_GAP = 7.0F;
	private static final float HEADER_HEIGHT = 28.0F;
	private static final float ROW_HEIGHT = 25.0F;
	private static final float OPTION_HEIGHT = 31.0F;
	private static final float DEFAULT_SECTION_OFFSET_Y = -62.0F;
	private static final float SCREEN_EDGE_MARGIN = 8.0F;

	private boolean otherOpen = true;
	private boolean settingsOpen;

	public GhostCategory(GuiModMenu parent) {
		super(parent, TranslateText.MODULE, LegacyIcon.ARCHIVE, Fonts.LEGACYICON, ModCategory.GHOST, false);
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
		otherOpen = true;
		settingsOpen = false;
		setCanClose(true);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		drawOtherPanel(mouseX, mouseY);
	}

	private void drawOtherPanel(int mouseX, int mouseY) {
		SettingsMod settingsMod = SettingsMod.getInstance();
		if(settingsMod == null) {
			return;
		}

		Glide instance = Glide.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ColorPalette palette = instance.getColorManager().getPalette();
		AccentColor accent = instance.getColorManager().getCurrentColor();
		boolean visible = isSettingsVisible(settingsMod);
		if(!visible && settingsOpen) {
			settingsOpen = false;
			setCanClose(true);
		}

		float panelX = getOtherPanelX();
		float panelY = getPanelY();
		float bodyHeight = otherOpen ? (visible ? ROW_HEIGHT + (settingsOpen ? OPTION_HEIGHT : 0.0F) : 34.0F) : 0.0F;
		float totalHeight = HEADER_HEIGHT + bodyHeight;
		boolean headerHovered = MouseUtils.isInside(mouseX, mouseY, panelX, panelY, PANEL_WIDTH, HEADER_HEIGHT);

		nvg.drawShadow(panelX, panelY, PANEL_WIDTH, totalHeight, 9, 3);
		nvg.drawRoundedRect(panelX, panelY, PANEL_WIDTH, totalHeight, 8,
				translucent(palette.getBackgroundColor(ColorType.DARK), 62));
		nvg.drawOutlineRoundedRect(panelX + 0.5F, panelY + 0.5F, PANEL_WIDTH - 1.0F, totalHeight - 1.0F,
				8, 0.55F, new Color(255, 255, 255, headerHovered ? 30 : 18));
		if(headerHovered) {
			nvg.drawRoundedRect(panelX + 3.0F, panelY + 3.0F, PANEL_WIDTH - 6.0F, HEADER_HEIGHT - 5.0F, 6,
					translucent(palette.getBackgroundColor(ColorType.NORMAL), 34));
		}

		nvg.drawText("Other", panelX + 10.0F, panelY + 8.5F, Color.WHITE, 10.5F, Fonts.SEMIBOLD);
		String count = visible ? "1" : "0";
		nvg.drawRoundedRect(panelX + PANEL_WIDTH - 47.0F, panelY + 7.0F, 17.0F, 14.0F, 7.0F,
				new Color(9, 13, 24, 55));
		nvg.drawCenteredText(count, panelX + PANEL_WIDTH - 38.5F, panelY + 11.0F,
				Color.WHITE, 7.5F, Fonts.SEMIBOLD);
		nvg.drawCenteredText(otherOpen ? LegacyIcon.CHEVRON_UP : LegacyIcon.CHEVRON_DOWN,
				panelX + PANEL_WIDTH - 13.0F, panelY + 9.0F, Color.WHITE, 8.0F, Fonts.LEGACYICON);

		if(!otherOpen) {
			return;
		}

		float rowY = panelY + HEADER_HEIGHT;
		if(!visible) {
			nvg.drawCenteredText("No modules", panelX + PANEL_WIDTH / 2.0F, rowY + 12.0F,
					palette.getFontColor(ColorType.NORMAL), 8.0F, Fonts.REGULAR);
			return;
		}

		boolean rowHovered = MouseUtils.isInside(mouseX, mouseY, panelX, rowY, PANEL_WIDTH, ROW_HEIGHT);
		if(rowHovered) {
			nvg.drawRoundedRect(panelX + 3.0F, rowY + 2.0F, PANEL_WIDTH - 6.0F, ROW_HEIGHT - 4.0F, 5,
					translucent(palette.getBackgroundColor(ColorType.NORMAL), 82));
		}
		nvg.drawRect(panelX + 7.0F, rowY + ROW_HEIGHT - 0.6F, PANEL_WIDTH - 14.0F, 0.6F,
				new Color(127, 135, 155, 22));
		nvg.drawGradientRoundedRect(panelX + 4.0F, rowY + 5.0F, 3.0F, 15.0F, 1.5F,
				accent.getColor1(), accent.getColor2());
		nvg.drawText(settingsMod.getName(), panelX + 12.0F, rowY + 8.0F,
				palette.getFontColor(ColorType.DARK), 9.0F, Fonts.MEDIUM);
		nvg.drawCircle(panelX + PANEL_WIDTH - 11.0F, rowY + 12.5F, 3.2F,
				translucent(palette.getBackgroundColor(ColorType.NORMAL), 140));
		nvg.drawGradientCircle(panelX + PANEL_WIDTH - 11.0F, rowY + 12.5F, 3.2F,
				accent.getColor1(), accent.getColor2());

		if(settingsOpen) {
			drawMoveFixOption(nvg, palette, accent, settingsMod.getMoveFixSetting(), panelX, rowY + ROW_HEIGHT,
					mouseX, mouseY);
		}
	}

	private void drawMoveFixOption(NanoVGManager nvg, ColorPalette palette, AccentColor accent,
			BooleanSetting setting, float panelX, float optionY, int mouseX, int mouseY) {
		nvg.drawRect(panelX + 6.0F, optionY + 1.0F, 1.2F, OPTION_HEIGHT - 2.0F,
				new Color(255, 255, 255, 145));
		nvg.drawText(setting.getName(), panelX + 12.0F, optionY + 10.0F,
				palette.getFontColor(ColorType.NORMAL), 8.5F, Fonts.MEDIUM);

		float toggleX = panelX + PANEL_WIDTH - 36.0F;
		float toggleY = optionY + 8.0F;
		boolean enabled = setting.isToggled();
		boolean hovered = MouseUtils.isInside(mouseX, mouseY, toggleX, toggleY, 26.0F, 14.0F);
		nvg.drawRoundedRect(toggleX, toggleY, 26.0F, 14.0F, 7.0F,
				enabled ? ColorUtils.applyAlpha(accent.getColor1(), hovered ? 235 : 205)
						: translucent(palette.getBackgroundColor(ColorType.NORMAL), hovered ? 170 : 130));
		if(enabled) {
			nvg.drawGradientRoundedRect(toggleX, toggleY, 26.0F, 14.0F, 7.0F,
					ColorUtils.applyAlpha(accent.getColor1(), hovered ? 245 : 220),
					ColorUtils.applyAlpha(accent.getColor2(), hovered ? 245 : 220));
		}
		nvg.drawCircle(enabled ? toggleX + 19.0F : toggleX + 7.0F, toggleY + 7.0F, 4.5F, Color.WHITE);
	}

	private boolean isSettingsVisible(SettingsMod settingsMod) {
		String search = getSearchBox().getText();
		return search.isEmpty() || SearchUtils.isSimillar(Glide.getInstance().getModManager().getWords(settingsMod), search);
	}

	private float getOtherPanelX() {
		float ghostPanelX = getX() + (getWidth() - PANEL_WIDTH) / 2.0F;
		return Math.min(getScreenWidth() - SCREEN_EDGE_MARGIN - PANEL_WIDTH,
				ghostPanelX + PANEL_WIDTH + PANEL_GAP);
	}

	private float getPanelY() {
		return getY() + Math.max(DEFAULT_SECTION_OFFSET_Y, SCREEN_EDGE_MARGIN - getY());
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		SettingsMod settingsMod = SettingsMod.getInstance();
		float panelX = getOtherPanelX();
		float panelY = getPanelY();

		if(MouseUtils.isInside(mouseX, mouseY, panelX, panelY, PANEL_WIDTH, HEADER_HEIGHT) && mouseButton == 0) {
			otherOpen = !otherOpen;
			if(!otherOpen && settingsOpen) {
				settingsOpen = false;
				setCanClose(true);
			}
			return;
		}

		if(otherOpen && settingsMod != null && isSettingsVisible(settingsMod)) {
			float rowY = panelY + HEADER_HEIGHT;
			if(MouseUtils.isInside(mouseX, mouseY, panelX, rowY, PANEL_WIDTH, ROW_HEIGHT)) {
				if(mouseButton == 1) {
					settingsOpen = !settingsOpen;
					setCanClose(!settingsOpen);
				}
				return;
			}

			if(settingsOpen) {
				float optionY = rowY + ROW_HEIGHT;
				float toggleX = panelX + PANEL_WIDTH - 36.0F;
				float toggleY = optionY + 8.0F;
				if(mouseButton == 0 && MouseUtils.isInside(mouseX, mouseY, toggleX, toggleY, 26.0F, 14.0F)) {
					BooleanSetting moveFix = settingsMod.getMoveFixSetting();
					moveFix.setToggled(!moveFix.isToggled());
					return;
				}
			}
		}

		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void keyTyped(char typedChar, int keyCode) {
		if(settingsOpen && keyCode == Keyboard.KEY_ESCAPE) {
			settingsOpen = false;
			setCanClose(true);
			return;
		}
		super.keyTyped(typedChar, keyCode);
	}

	private Color translucent(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}
}
