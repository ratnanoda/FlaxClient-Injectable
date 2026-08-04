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

	private boolean otherOpen = true;
	private boolean settingsOpen;
	private boolean otherPositionInitialized;
	private boolean draggingOther;
	private boolean resizingOther;
	private boolean otherMoved;
	private float otherOffsetX;
	private float otherOffsetY;
	private float otherWidth = DEFAULT_PANEL_WIDTH;
	private float otherBodyHeightOverride = -1.0F;
	private float otherVisibleBodyHeight;
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
		otherPositionInitialized = false;
		draggingOther = false;
		resizingOther = false;
		resizeEdges = 0;
		otherWidth = DEFAULT_PANEL_WIDTH;
		otherBodyHeightOverride = -1.0F;
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

		initializeOtherPanel();

		if(resizingOther) {
			updateOtherResize(mouseX, mouseY);
		} else if(draggingOther) {
			otherOffsetX = mouseX - getX() - dragOffsetX;
			otherOffsetY = mouseY - getY() - dragOffsetY;
			otherMoved = otherMoved
					|| Math.abs(mouseX - pressMouseX) > 3.0F
					|| Math.abs(mouseY - pressMouseY) > 3.0F;
			clampOtherPanel();
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
		float panelY = getOtherPanelY();
		float contentHeight = visible ? ROW_HEIGHT + (settingsOpen ? OPTION_HEIGHT : 0.0F) : 34.0F;
		float maxBodyHeight = Math.max(MIN_RESIZABLE_BODY_HEIGHT,
				getScreenHeight() - panelY - SCREEN_EDGE_MARGIN - HEADER_HEIGHT);
		float expandedBodyHeight = otherBodyHeightOverride > 0.0F
				? Math.max(MIN_RESIZABLE_BODY_HEIGHT, Math.min(maxBodyHeight, otherBodyHeightOverride))
				: contentHeight;
		float bodyHeight = otherOpen ? expandedBodyHeight : 0.0F;
		otherVisibleBodyHeight = bodyHeight;
		float totalHeight = HEADER_HEIGHT + bodyHeight;
		boolean headerHovered = MouseUtils.isInside(mouseX, mouseY, panelX, panelY, otherWidth, HEADER_HEIGHT);

		nvg.drawShadow(panelX, panelY, otherWidth, totalHeight, 9, 3);
		nvg.drawRoundedRect(panelX, panelY, otherWidth, totalHeight, 8,
				translucent(palette.getBackgroundColor(ColorType.DARK), 62));
		nvg.drawOutlineRoundedRect(panelX + 0.5F, panelY + 0.5F, otherWidth - 1.0F, totalHeight - 1.0F,
				8, 0.55F, new Color(255, 255, 255, headerHovered ? 30 : 18));
		if(headerHovered) {
			nvg.drawRoundedRect(panelX + 3.0F, panelY + 3.0F, otherWidth - 6.0F, HEADER_HEIGHT - 5.0F, 6,
					translucent(palette.getBackgroundColor(ColorType.NORMAL), 34));
		}

		nvg.drawText("Other", panelX + 10.0F, panelY + 8.5F, Color.WHITE, 10.5F, Fonts.SEMIBOLD);
		String count = visible ? "1" : "0";
		float countWidth = 17.0F;
		float arrowX = panelX + otherWidth - 13.0F;
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
		nvg.drawCenteredText(otherOpen ? LegacyIcon.CHEVRON_UP : LegacyIcon.CHEVRON_DOWN,
				arrowX, panelY + 9.0F, Color.WHITE, 8.0F, Fonts.LEGACYICON);

		if(otherOpen) {
			drawOtherResizeHandles(nvg, panelX, panelY, totalHeight, mouseX, mouseY);
		}

		if(!otherOpen || bodyHeight <= 0.5F) {
			return;
		}

		float rowY = panelY + HEADER_HEIGHT;
		nvg.save();
		nvg.scissor(panelX, rowY, otherWidth, bodyHeight);

		if(!visible) {
			nvg.drawCenteredText("No modules", panelX + otherWidth / 2.0F, rowY + 12.0F,
					palette.getFontColor(ColorType.NORMAL), 8.0F, Fonts.REGULAR);
			nvg.restore();
			return;
		}

		boolean rowHovered = MouseUtils.isInside(mouseX, mouseY, panelX, rowY, otherWidth, ROW_HEIGHT);
		if(rowHovered) {
			nvg.drawRoundedRect(panelX + 3.0F, rowY + 2.0F, otherWidth - 6.0F, ROW_HEIGHT - 4.0F, 5,
					translucent(palette.getBackgroundColor(ColorType.NORMAL), 82));
		}
		nvg.drawRect(panelX + 7.0F, rowY + ROW_HEIGHT - 0.6F, otherWidth - 14.0F, 0.6F,
				new Color(127, 135, 155, 22));
		nvg.drawGradientRoundedRect(panelX + 4.0F, rowY + 5.0F, 3.0F, 15.0F, 1.5F,
				accent.getColor1(), accent.getColor2());
		nvg.drawText(settingsMod.getName(), panelX + 12.0F, rowY + 8.0F,
				palette.getFontColor(ColorType.DARK), 9.0F, Fonts.MEDIUM);
		nvg.drawCircle(panelX + otherWidth - 11.0F, rowY + 12.5F, 3.2F,
				translucent(palette.getBackgroundColor(ColorType.NORMAL), 140));
		nvg.drawGradientCircle(panelX + otherWidth - 11.0F, rowY + 12.5F, 3.2F,
				accent.getColor1(), accent.getColor2());

		if(settingsOpen) {
			drawMoveFixOption(nvg, palette, accent, settingsMod.getMoveFixSetting(), panelX,
					rowY + ROW_HEIGHT, mouseX, mouseY);
		}
		nvg.restore();
	}

	private void drawMoveFixOption(NanoVGManager nvg, ColorPalette palette, AccentColor accent,
			BooleanSetting setting, float panelX, float optionY, int mouseX, int mouseY) {
		nvg.drawRect(panelX + 6.0F, optionY + 1.0F, 1.2F, OPTION_HEIGHT - 2.0F,
				new Color(255, 255, 255, 145));
		nvg.drawText(setting.getName(), panelX + 12.0F, optionY + 10.0F,
				palette.getFontColor(ColorType.NORMAL), 8.5F, Fonts.MEDIUM);

		float toggleX = panelX + otherWidth - 36.0F;
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
		return search.isEmpty()
				|| SearchUtils.isSimillar(Glide.getInstance().getModManager().getWords(settingsMod), search);
	}

	private void initializeOtherPanel() {
		if(otherPositionInitialized) {
			return;
		}
		float ghostPanelX = getX() + (getWidth() - DEFAULT_PANEL_WIDTH) / 2.0F;
		otherOffsetX = ghostPanelX + DEFAULT_PANEL_WIDTH + PANEL_GAP - getX();
		otherOffsetY = Math.max(DEFAULT_SECTION_OFFSET_Y, SCREEN_EDGE_MARGIN - getY());
		otherWidth = DEFAULT_PANEL_WIDTH;
		otherBodyHeightOverride = -1.0F;
		otherPositionInitialized = true;
		clampOtherPanel();
	}

	private float getOtherPanelX() {
		return getX() + otherOffsetX;
	}

	private float getOtherPanelY() {
		return getY() + otherOffsetY;
	}

	private void resetOtherLayout() {
		otherPositionInitialized = false;
		initializeOtherPanel();
		otherOpen = true;
	}

	private void clampOtherPanel() {
		float maxWidth = Math.max(MIN_RESIZABLE_WIDTH, getScreenWidth() - SCREEN_EDGE_MARGIN * 2.0F);
		otherWidth = Math.max(MIN_RESIZABLE_WIDTH, Math.min(maxWidth, otherWidth));

		float minOffsetX = SCREEN_EDGE_MARGIN - getX();
		float maxOffsetX = getScreenWidth() - SCREEN_EDGE_MARGIN - otherWidth - getX();
		otherOffsetX = Math.max(minOffsetX, Math.min(maxOffsetX, otherOffsetX));

		float minOffsetY = SCREEN_EDGE_MARGIN - getY();
		float maxOffsetY = getScreenHeight() - SCREEN_EDGE_MARGIN - HEADER_HEIGHT
				- MIN_RESIZABLE_BODY_HEIGHT - getY();
		otherOffsetY = Math.max(minOffsetY, Math.min(maxOffsetY, otherOffsetY));

		if(otherBodyHeightOverride > 0.0F) {
			float absoluteY = getY() + otherOffsetY;
			float maxBody = Math.max(MIN_RESIZABLE_BODY_HEIGHT,
					getScreenHeight() - SCREEN_EDGE_MARGIN - absoluteY - HEADER_HEIGHT);
			otherBodyHeightOverride = Math.min(otherBodyHeightOverride, maxBody);
		}
	}

	private void drawOtherResizeHandles(NanoVGManager nvg, float x, float y, float totalHeight,
			int mouseX, int mouseY) {
		int edges = getOtherResizeEdges(mouseX, mouseY, x, y, otherWidth, totalHeight);
		Color edge = new Color(255, 255, 255, edges == 0 ? 28 : 82);
		nvg.drawRect(x + otherWidth - 10.0F, y + totalHeight - 2.0F, 8.0F, 1.0F, edge);
		nvg.drawRect(x + otherWidth - 2.0F, y + totalHeight - 10.0F, 1.0F, 8.0F, edge);
	}

	private int getOtherResizeEdges(int mouseX, int mouseY, float x, float y, float width, float height) {
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

	private void beginOtherResize(int edges, int mouseX, int mouseY) {
		resizingOther = true;
		resizeEdges = edges;
		resizeStartMouseX = mouseX;
		resizeStartMouseY = mouseY;
		resizeStartOffsetX = otherOffsetX;
		resizeStartOffsetY = otherOffsetY;
		resizeStartWidth = otherWidth;
		resizeStartBodyHeight = Math.max(MIN_RESIZABLE_BODY_HEIGHT, otherVisibleBodyHeight);
		otherBodyHeightOverride = resizeStartBodyHeight;
	}

	private void updateOtherResize(int mouseX, int mouseY) {
		float dx = mouseX - resizeStartMouseX;
		float dy = mouseY - resizeStartMouseY;
		float maxWidth = Math.max(MIN_RESIZABLE_WIDTH, getScreenWidth() - SCREEN_EDGE_MARGIN * 2.0F);
		float width = resizeStartWidth;
		float offsetX = resizeStartOffsetX;

		if((resizeEdges & 1) != 0) {
			width = Math.max(MIN_RESIZABLE_WIDTH, Math.min(maxWidth, resizeStartWidth - dx));
			offsetX = resizeStartOffsetX + (resizeStartWidth - width);
		} else if((resizeEdges & 2) != 0) {
			width = Math.max(MIN_RESIZABLE_WIDTH, Math.min(maxWidth, resizeStartWidth + dx));
		}

		float bodyHeight = resizeStartBodyHeight;
		float offsetY = resizeStartOffsetY;
		if((resizeEdges & 4) != 0) {
			bodyHeight = Math.max(MIN_RESIZABLE_BODY_HEIGHT, resizeStartBodyHeight - dy);
			offsetY = resizeStartOffsetY + (resizeStartBodyHeight - bodyHeight);
		} else if((resizeEdges & 8) != 0) {
			bodyHeight = Math.max(MIN_RESIZABLE_BODY_HEIGHT, resizeStartBodyHeight + dy);
		}

		otherWidth = width;
		otherBodyHeightOverride = bodyHeight;
		otherOffsetX = offsetX;
		otherOffsetY = offsetY;
		clampOtherPanel();
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		initializeOtherPanel();
		SettingsMod settingsMod = SettingsMod.getInstance();
		float panelX = getOtherPanelX();
		float panelY = getOtherPanelY();
		float totalHeight = HEADER_HEIGHT + Math.max(MIN_RESIZABLE_BODY_HEIGHT, otherVisibleBodyHeight);
		float countWidth = 17.0F;
		float arrowX = panelX + otherWidth - 13.0F;
		float resetWidth = 34.0F;
		float resetX = arrowX - countWidth - resetWidth - 12.0F;

		if(mouseButton == 0
				&& MouseUtils.isInside(mouseX, mouseY, resetX, panelY + 6.0F, resetWidth, 16.0F)) {
			resetOtherLayout();
			return;
		}

		if(mouseButton == 0 && otherOpen) {
			int edges = getOtherResizeEdges(mouseX, mouseY, panelX, panelY, otherWidth, totalHeight);
			if(edges != 0) {
				beginOtherResize(edges, mouseX, mouseY);
				return;
			}
		}

		if(MouseUtils.isInside(mouseX, mouseY, panelX, panelY, otherWidth, HEADER_HEIGHT)
				&& mouseButton == 0) {
			draggingOther = true;
			dragOffsetX = mouseX - panelX;
			dragOffsetY = mouseY - panelY;
			pressMouseX = mouseX;
			pressMouseY = mouseY;
			otherMoved = false;
			return;
		}

		if(otherOpen && settingsMod != null && isSettingsVisible(settingsMod)) {
			float rowY = panelY + HEADER_HEIGHT;
			if(MouseUtils.isInside(mouseX, mouseY, panelX, rowY, otherWidth, ROW_HEIGHT)) {
				if(mouseButton == 1) {
					settingsOpen = !settingsOpen;
					setCanClose(!settingsOpen);
				}
				return;
			}

			if(settingsOpen) {
				float optionY = rowY + ROW_HEIGHT;
				float toggleX = panelX + otherWidth - 36.0F;
				float toggleY = optionY + 8.0F;
				if(mouseButton == 0
						&& MouseUtils.isInside(mouseX, mouseY, toggleX, toggleY, 26.0F, 14.0F)) {
					BooleanSetting moveFix = settingsMod.getMoveFixSetting();
					moveFix.setToggled(!moveFix.isToggled());
					return;
				}
			}
		}

		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
		if(mouseButton == 0 && resizingOther) {
			resizingOther = false;
			resizeEdges = 0;
		}
		if(mouseButton == 0 && draggingOther) {
			if(!otherMoved) {
				otherOpen = !otherOpen;
				if(!otherOpen && settingsOpen) {
					settingsOpen = false;
					setCanClose(true);
				}
			}
			draggingOther = false;
		}
		super.mouseReleased(mouseX, mouseY, mouseButton);
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
