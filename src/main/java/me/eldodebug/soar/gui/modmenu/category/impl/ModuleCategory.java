package me.eldodebug.soar.gui.modmenu.category.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.modmenu.GuiModMenu;
import me.eldodebug.soar.gui.modmenu.category.Category;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.color.ColorManager;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.ModManager;
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
import me.eldodebug.soar.management.nanovg.font.Font;
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
import me.eldodebug.soar.utils.animation.simple.SimpleAnimation;
import me.eldodebug.soar.utils.mouse.MouseUtils;

/**
 * Module browser presented as five independent alphabetical dropdowns.
 * Ghost modules are classified exclusively by ModCategory.GHOST and can never
 * leak into one of the regular alphabetical sections.
 */
public class ModuleCategory extends Category {

	private static final float PANEL_GAP = 7.0F;
	private static final float PANEL_MARGIN = 11.0F;
	private static final float ORIGINAL_SINGLE_PANEL_WIDTH = 132.0F;
	private static final float HEADER_HEIGHT = 28.0F;
	private static final float ROW_HEIGHT = 25.0F;
	private static final float INLINE_SETTINGS_PADDING = 5.0F;
	private static final float DEFAULT_SECTION_OFFSET_Y = -62.0F;
	private static final float SCREEN_EDGE_MARGIN = 8.0F;
	private static final float MIN_RESIZABLE_WIDTH = 132.0F;
	private static final float MIN_RESIZABLE_BODY_HEIGHT = 54.0F;
	private static final float RESIZE_EDGE = 5.0F;
	private static boolean resizeTutorialClaimed;

	private final ModCategory defaultCategory;
	private final boolean showAlphabetSections;
	private final ArrayList<DropdownSection> sections = new ArrayList<DropdownSection>();
	private final ArrayList<ModuleSetting> comps = new ArrayList<ModuleSetting>();
	private final Map<DropdownSection, List<Mod>> moduleCache = new HashMap<DropdownSection, List<Mod>>();
	private String cachedSearch = null;

	private boolean openSetting;
	private Mod currentMod;
	private Mod bindingMod;
	private DropdownSection draggingSection;
	private float sectionDragX, sectionDragY;
	private float sectionPressX, sectionPressY;
	private boolean sectionPositionsInitialized;
	private DropdownSection settingsSection;
	private DropdownSection resizingSection;
	private int resizeEdges;
	private float resizeStartMouseX, resizeStartMouseY;
	private float resizeStartOffsetX, resizeStartOffsetY;
	private float resizeStartWidth, resizeStartBodyHeight;
	private final SimpleAnimation resizeTutorialAnimation = new SimpleAnimation();
	private boolean resizeTutorialActive;

	public ModuleCategory(GuiModMenu parent) {
		this(parent, TranslateText.MODULE, LegacyIcon.ARCHIVE, Fonts.LEGACYICON, ModCategory.ALL, true);
	}

	protected ModuleCategory(GuiModMenu parent, TranslateText name, String icon, Font iconFont,
			ModCategory defaultCategory, boolean showAlphabetSections) {
		super(parent, name, icon, iconFont, true, true);
		this.defaultCategory = defaultCategory;
		this.showAlphabetSections = showAlphabetSections;
		createSections();
	}

	private void createSections() {
		sections.clear();
		if(showAlphabetSections) {
			sections.add(new DropdownSection("A ~ G", 'A', 'G', false));
			sections.add(new DropdownSection("H ~ N", 'H', 'N', false));
			sections.add(new DropdownSection("O ~ U", 'O', 'U', false));
			sections.add(new DropdownSection("V ~ Z", 'V', 'Z', false));
			sections.add(new DropdownSection("Ghost", 'A', 'Z', true));
		} else {
			sections.add(new DropdownSection(defaultCategory == ModCategory.GHOST ? "Ghost" : defaultCategory.getName(), 'A', 'Z', defaultCategory == ModCategory.GHOST));
		}
	}

	@Override
	public void initGui() {
		resetScene();
	}

	@Override
	public void initCategory() {
		resetScene();
		resizeTutorialActive = isResizableLayout()
				&& Glide.getInstance().getFileManager().isFirstInstallation()
				&& !resizeTutorialClaimed;
		if(resizeTutorialActive) {
			resizeTutorialClaimed = true;
			resizeTutorialAnimation.setValue(0.0F);
		}
	}

	private void resetScene() {
		scroll.resetAll();
		cachedSearch = null;
		moduleCache.clear();
		openSetting = false;
		currentMod = null;
		settingsSection = null;
		bindingMod = null;
		for(DropdownSection section : sections) {
			section.open = true;
			section.openAnimation.setValue(0.0F);
			section.scrollTarget = 0.0F;
			section.scrollAnimation.setValue(0.0F);
		}
		draggingSection = null;
		resizingSection = null;
		resizeEdges = 0;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		Glide instance = Glide.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ColorManager colorManager = instance.getColorManager();
		ColorPalette palette = colorManager.getPalette();
		AccentColor accentColor = colorManager.getCurrentColor();

		if(!getSearchBox().getText().isEmpty()) {
			nvg.drawCenteredText(getSearchBox().getText(), getScreenWidth() / 2.0F,
					getScreenHeight() / 2.0F - 18.0F, new Color(255, 255, 255, 28), 42.0F, Fonts.SEMIBOLD);
		}
		drawDropdowns(nvg, palette, accentColor, mouseX, mouseY, partialTicks);
		drawResizeTutorial(nvg, palette, accentColor);

		if(bindingMod != null) {
			drawBindingPrompt(nvg, palette, accentColor);
		}
	}

	private void drawDropdowns(NanoVGManager nvg, ColorPalette palette, AccentColor accentColor,
			int mouseX, int mouseY, float partialTicks) {
		float defaultPanelWidth = calculatePanelWidth();
		initializeSectionPositions(defaultPanelWidth);

		if(resizingSection != null) {
			updateResize(mouseX, mouseY);
		} else if(draggingSection != null) {
			draggingSection.offsetX = mouseX - getX() - sectionDragX;
			draggingSection.offsetY = mouseY - getY() - sectionDragY;
			draggingSection.moved = draggingSection.moved
					|| Math.abs(mouseX - sectionPressX) > 3 || Math.abs(mouseY - sectionPressY) > 3;
		}

		int wheel = 0;

		for(int i = sections.size() - 1; i >= 0; i--) {
			DropdownSection section = sections.get(i);
			float panelWidth = getPanelWidth(section, defaultPanelWidth);
			float panelX = getX() + section.offsetX;
			float panelY = getY() + section.offsetY;
			if(MouseUtils.isInside(mouseX, mouseY, panelX, panelY, panelWidth,
					HEADER_HEIGHT + Math.max(42.0F, section.visibleBodyHeight))) {
				wheel = Mouse.getDWheel();
				break;
			}
		}

		for(int i = 0; i < sections.size(); i++) {
			DropdownSection section = sections.get(i);
			List<Mod> modules = getSectionModules(section);
			float panelWidth = getPanelWidth(section, defaultPanelWidth);
			clampSectionWindow(section, panelWidth);
			float panelX = getX() + section.offsetX;
			float panelY = getY() + section.offsetY;
			float maxBodyHeight = Math.max(42.0F,
					getScreenHeight() - panelY - SCREEN_EDGE_MARGIN - HEADER_HEIGHT);
			boolean headerHovered = MouseUtils.isInside(mouseX, mouseY, panelX, panelY, panelWidth, HEADER_HEIGHT);

			section.openAnimation.setAnimation(section.open ? 1.0F : 0.0F, 18);
			section.hoverAnimation.setAnimation(headerHovered ? 1.0F : 0.0F, 18);

			float contentHeight = getSectionContentHeight(modules);
			float expandedHeight = section.bodyHeightOverride > 0.0F
					? Math.max(MIN_RESIZABLE_BODY_HEIGHT, Math.min(maxBodyHeight, section.bodyHeightOverride))
					: Math.min(maxBodyHeight, Math.max(42.0F, contentHeight));
			float bodyHeight = expandedHeight * section.openAnimation.getValue();
			section.visibleBodyHeight = bodyHeight;
			section.maxScroll = Math.max(0.0F, contentHeight - expandedHeight);

			if(wheel != 0 && section.open && MouseUtils.isInside(mouseX, mouseY, panelX, panelY + HEADER_HEIGHT, panelWidth, maxBodyHeight)) {
				section.scrollTarget += wheel / 2.4F;
			}
			section.scrollTarget = Math.max(-section.maxScroll, Math.min(0.0F, section.scrollTarget));
			section.scrollAnimation.setAnimation(section.scrollTarget, 18);

			float totalHeight = HEADER_HEIGHT + bodyHeight;
			nvg.drawShadow(panelX, panelY, panelWidth, totalHeight, 9, 3);
			nvg.drawRoundedRect(panelX, panelY, panelWidth, totalHeight, 8,
					translucent(palette.getBackgroundColor(ColorType.DARK), 62));
			nvg.drawOutlineRoundedRect(panelX + 0.5F, panelY + 0.5F, panelWidth - 1, totalHeight - 1,
					8, 0.55F, new Color(255, 255, 255, headerHovered ? 30 : 18));
				if(headerHovered) {
				nvg.drawRoundedRect(panelX + 3, panelY + 3, panelWidth - 6, HEADER_HEIGHT - 5, 6,
						translucent(palette.getBackgroundColor(ColorType.NORMAL), 34));
			}

			nvg.drawText(section.title, panelX + 10, panelY + 8.5F, Color.WHITE, 10.5F, Fonts.SEMIBOLD);
			String count = String.valueOf(modules.size());
			float countWidth = Math.max(17.0F, nvg.getTextWidth(count, 7.5F, Fonts.SEMIBOLD) + 9.0F);
			section.lastCountWidth = countWidth;
			float arrowX = panelX + panelWidth - 13.0F;
			nvg.drawRoundedRect(arrowX - countWidth - 7.0F, panelY + 7.0F, countWidth, 14.0F, 7.0F, new Color(9, 13, 24, 55));
			nvg.drawCenteredText(count, arrowX - 7.0F - countWidth / 2.0F, panelY + 11.0F, Color.WHITE, 7.5F, Fonts.SEMIBOLD);
			if(isResizableLayout()) {
				float resetWidth = 34.0F;
				float resetX = arrowX - countWidth - resetWidth - 12.0F;
				boolean resetHovered = MouseUtils.isInside(mouseX, mouseY, resetX, panelY + 6.0F, resetWidth, 16.0F);
				nvg.drawRoundedRect(resetX, panelY + 6.0F, resetWidth, 16.0F, 6.0F,
						new Color(255, 255, 255, resetHovered ? 40 : 22));
				nvg.drawCenteredText("Reset", resetX + resetWidth / 2.0F, panelY + 10.5F,
						palette.getFontColor(ColorType.NORMAL), 7.0F, Fonts.MEDIUM);
			}
			nvg.drawCenteredText(section.open ? LegacyIcon.CHEVRON_UP : LegacyIcon.CHEVRON_DOWN,
					arrowX, panelY + 9.0F, Color.WHITE, 8.0F, Fonts.LEGACYICON);

			if(isResizableLayout() && section.open) {
				drawResizeHandles(nvg, panelX, panelY, panelWidth, totalHeight, mouseX, mouseY);
			}

			if(bodyHeight <= 0.5F) continue;
			float bodyY = panelY + HEADER_HEIGHT;
			nvg.save();
			nvg.scissor(panelX, bodyY, panelWidth, bodyHeight);
			nvg.translate(0, section.scrollAnimation.getValue());

			if(modules.isEmpty()) {
				nvg.drawCenteredText("No modules", panelX + panelWidth / 2.0F, bodyY + 12.0F,
						palette.getFontColor(ColorType.NORMAL), 8.0F, Fonts.REGULAR);
			} else {
				float rowY = bodyY;
				for(int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
					Mod mod = modules.get(moduleIndex);
					drawModuleRow(nvg, palette, accentColor, mod, panelX, rowY, panelWidth, mouseX, mouseY, section);
					rowY += ROW_HEIGHT;
					if(openSetting && currentMod == mod) {
						drawInlineSettings(nvg, palette, panelX, rowY, panelWidth,
								mouseX, mouseY, partialTicks, section);
						rowY += getInlineSettingsHeight();
					}
				}
			}

			nvg.restore();
		}
	}

	private void drawModuleRow(NanoVGManager nvg, ColorPalette palette, AccentColor accentColor, Mod mod,
			float x, float y, float width, int mouseX, int mouseY, DropdownSection section) {
		float screenY = y + section.scrollAnimation.getValue();
		float visibleTop = getY() + section.offsetY + HEADER_HEIGHT;
		float visibleBottom = visibleTop + section.visibleBodyHeight;
		if(screenY + ROW_HEIGHT <= visibleTop || screenY >= visibleBottom) return;
		boolean hovered = MouseUtils.isInside(mouseX, mouseY, x, screenY, width, ROW_HEIGHT);
		if(hovered) {
			nvg.drawRoundedRect(x + 3, y + 2, width - 6, ROW_HEIGHT - 4, 5,
					translucent(palette.getBackgroundColor(ColorType.NORMAL), 82));
		}
		nvg.drawRect(x + 7, y + ROW_HEIGHT - 0.6F, width - 14, 0.6F, new Color(127, 135, 155, 22));

		mod.getAnimation().setAnimation(mod.isToggled() ? 1.0F : 0.0F, 18);
		float active = mod.getAnimation().getValue();
		nvg.drawGradientRoundedRect(x + 4, y + 5, 3.0F, 15.0F, 1.5F,
				ColorUtils.applyAlpha(accentColor.getColor1(), (int) (active * 255)),
				ColorUtils.applyAlpha(accentColor.getColor2(), (int) (active * 255)));

		String bindName = mod.getKeyCode() == Keyboard.KEY_NONE ? "" : Keyboard.getKeyName(mod.getKeyCode());
		if(bindName == null) bindName = "";
		float bindWidth = bindName.isEmpty() ? 0.0F : nvg.getTextWidth(bindName, 7.5F, Fonts.MEDIUM);
		float reserved = bindName.isEmpty() ? 28.0F : 37.0F + bindWidth;
		String name = nvg.getLimitText(mod.getName(), 9.0F, Fonts.MEDIUM, Math.max(28.0F, width - reserved));
		Color nameColor = mod.isToggled() ? palette.getFontColor(ColorType.DARK) : palette.getFontColor(ColorType.NORMAL);
		nvg.drawText(name, x + 12, y + 8.0F, nameColor, 9.0F, Fonts.MEDIUM);

		float bindX = x + width - 7.0F - bindWidth;
		if(!bindName.isEmpty()) {
			nvg.drawText(bindName, bindX, y + 8.8F, palette.getFontColor(ColorType.NORMAL, 180), 7.5F, Fonts.MEDIUM);
		}
		float stateX = bindName.isEmpty() ? x + width - 11.0F : bindX - 9.0F;
		nvg.drawCircle(stateX, y + 12.5F, 3.2F,
				translucent(palette.getBackgroundColor(ColorType.NORMAL), 140));
		nvg.drawGradientCircle(stateX, y + 12.5F, 3.2F,
				ColorUtils.applyAlpha(accentColor.getColor1(), (int) (active * 255)),
				ColorUtils.applyAlpha(accentColor.getColor2(), (int) (active * 255)));
	}

	private float getSectionContentHeight(List<Mod> modules) {
		float height = modules.isEmpty() ? 34.0F : modules.size() * ROW_HEIGHT;
		if(openSetting && currentMod != null && modules.contains(currentMod)) height += getInlineSettingsHeight();
		return height;
	}

	private float getInlineSettingsHeight() {
		float greatestShift = 0.0F;
		for(ModuleSetting entry : comps) greatestShift = Math.max(greatestShift, entry.openY);
		return INLINE_SETTINGS_PADDING * 2.0F + comps.size() * 31.0F + greatestShift;
	}

	private void drawInlineSettings(NanoVGManager nvg, ColorPalette palette, float x, float y,
			float width, int mouseX, int mouseY, float partialTicks, DropdownSection section) {
		float height = getInlineSettingsHeight();
		nvg.drawRect(x + 6.0F, y + 1.0F, 1.2F, height - 2.0F, new Color(255, 255, 255, 145));

		float cellX = x + 12.0F;
		float cellWidth = width - 18.0F;
		float visibleTop = getY() + section.offsetY + HEADER_HEIGHT;
		float visibleBottom = visibleTop + section.visibleBodyHeight;
		for(int i = 0; i < comps.size(); i++) {
			ModuleSetting entry = comps.get(i);
			entry.openAnimation.setAnimation(entry.openY, 16);
			float rowY = y + INLINE_SETTINGS_PADDING + i * 31.0F + entry.openAnimation.getValue();
			float screenRowY = rowY + section.scrollAnimation.getValue();
			if(screenRowY + 31.0F <= visibleTop || screenRowY >= visibleBottom) continue;
			nvg.drawText(nvg.getLimitText(entry.setting.getName(), 8.5F, Fonts.MEDIUM,
					Math.max(24.0F, cellWidth - 88.0F)), cellX, rowY + 10.0F,
					palette.getFontColor(ColorType.NORMAL), 8.5F, Fonts.MEDIUM);
			positionSettingComponent(entry.comp, cellX, rowY, cellWidth);
			entry.comp.draw(mouseX, (int) (mouseY - section.scrollAnimation.getValue()), partialTicks);
		}
	}

	private List<Mod> getSectionModules(final DropdownSection section) {
		String search = getSearchBox().getText();
		if(cachedSearch == null || !cachedSearch.equals(search) || !moduleCache.containsKey(section)) {
			rebuildModuleCache(search);
		}
		List<Mod> result = moduleCache.get(section);
		return result == null ? Collections.<Mod>emptyList() : result;
	}

	private void rebuildModuleCache(String search) {
		moduleCache.clear();
		for(DropdownSection section : sections) moduleCache.put(section, new ArrayList<Mod>());

		ModManager manager = Glide.getInstance().getModManager();
		final Map<Mod, String> sortNames = new HashMap<Mod, String>();

		for(Mod mod : manager.getMods()) {
			if(mod.isHide() || !mod.getAllowed()) continue;
			if(!search.isEmpty() && !SearchUtils.isSimillar(manager.getWords(mod), search)) continue;

			boolean ghost = mod.getCategory() == ModCategory.GHOST;
			String sortName = getSortName(mod);
			sortNames.put(mod, sortName);
			char initial = firstAsciiLetter(sortName);

			for(DropdownSection section : sections) {
				if(section.ghost != ghost) continue;
				if(!showAlphabetSections && !section.ghost && defaultCategory != ModCategory.ALL
						&& mod.getCategory() != defaultCategory) continue;
				if(section.ghost || (initial >= section.start && initial <= section.end)) {
					moduleCache.get(section).add(mod);
					break;
				}
			}
		}

		Comparator<Mod> comparator = new Comparator<Mod>() {
			@Override public int compare(Mod first, Mod second) {
				return sortNames.get(first).compareToIgnoreCase(sortNames.get(second));
			}
		};
		for(List<Mod> modules : moduleCache.values()) Collections.sort(modules, comparator);
		cachedSearch = search;
	}

	private String getSortName(Mod mod) {
		return Glide.getInstance().getLanguageManager().getBaseTranslation(mod.getNameKey()).trim();
	}

	private char firstAsciiLetter(String text) {
		for(int i = 0; i < text.length(); i++) {
			char c = Character.toUpperCase(text.charAt(i));
			if(c >= 'A' && c <= 'Z') return c;
		}
		return 'Z';
	}

	private void initializeSectionPositions(float panelWidth) {
		if(sectionPositionsInitialized) return;
		for(int i = 0; i < sections.size(); i++) {
			DropdownSection section = sections.get(i);
			section.offsetX = sections.size() == 1
					? (getWidth() - panelWidth) / 2.0F
					: PANEL_MARGIN + i * (panelWidth + PANEL_GAP);
			section.offsetY = Math.max(DEFAULT_SECTION_OFFSET_Y, SCREEN_EDGE_MARGIN - getY());
		}
		sectionPositionsInitialized = true;
	}

	private float calculatePanelWidth() {
		float availableWidth = getWidth() - PANEL_MARGIN * 2.0F;
		if(!showAlphabetSections && sections.size() == 1) {
			return Math.min(ORIGINAL_SINGLE_PANEL_WIDTH, availableWidth);
		}
		return (availableWidth - PANEL_GAP * (sections.size() - 1)) / sections.size();
	}

	private boolean isResizableLayout() {
		return !showAlphabetSections && sections.size() == 1;
	}

	private float getPanelWidth(DropdownSection section, float defaultWidth) {
		return section.widthOverride > 0.0F ? section.widthOverride : defaultWidth;
	}

	private void drawResizeHandles(NanoVGManager nvg, float x, float y, float width, float height,
			int mouseX, int mouseY) {
		int edges = getResizeEdges(mouseX, mouseY, x, y, width, height);
		Color edge = new Color(255, 255, 255, edges == 0 ? 34 : 105);
		// Clip two concentric rounded outlines to the corner. The grip now follows
		// the panel radius instead of drawing a square L over a rounded window.
		nvg.save();
		nvg.scissor(x + width - 15.0F, y + height - 15.0F, 15.0F, 15.0F);
		nvg.drawOutlineRoundedRect(x + width - 14.0F, y + height - 14.0F,
				13.0F, 13.0F, 7.0F, 1.0F, edge);
		nvg.drawOutlineRoundedRect(x + width - 10.0F, y + height - 10.0F,
				9.0F, 9.0F, 5.0F, 0.8F, edge);
		nvg.restore();
	}

	private int getResizeEdges(int mouseX, int mouseY, float x, float y, float width, float height) {
		if(!isResizableLayout()) return 0;
		int edges = 0;
		if(mouseX >= x - RESIZE_EDGE && mouseX <= x + RESIZE_EDGE
				&& mouseY >= y - RESIZE_EDGE && mouseY <= y + height + RESIZE_EDGE) edges |= 1;
		if(mouseX >= x + width - RESIZE_EDGE && mouseX <= x + width + RESIZE_EDGE
				&& mouseY >= y - RESIZE_EDGE && mouseY <= y + height + RESIZE_EDGE) edges |= 2;
		if(mouseY >= y - RESIZE_EDGE && mouseY <= y + RESIZE_EDGE
				&& mouseX >= x - RESIZE_EDGE && mouseX <= x + width + RESIZE_EDGE) edges |= 4;
		if(mouseY >= y + height - RESIZE_EDGE && mouseY <= y + height + RESIZE_EDGE
				&& mouseX >= x - RESIZE_EDGE && mouseX <= x + width + RESIZE_EDGE) edges |= 8;
		return edges;
	}

	private void beginResize(DropdownSection section, int edges, int mouseX, int mouseY, float width) {
		resizingSection = section;
		resizeEdges = edges;
		resizeStartMouseX = mouseX;
		resizeStartMouseY = mouseY;
		resizeStartOffsetX = section.offsetX;
		resizeStartOffsetY = section.offsetY;
		resizeStartWidth = width;
		resizeStartBodyHeight = Math.max(MIN_RESIZABLE_BODY_HEIGHT, section.visibleBodyHeight);
		section.widthOverride = width;
		section.bodyHeightOverride = resizeStartBodyHeight;
	}

	private void updateResize(int mouseX, int mouseY) {
		DropdownSection section = resizingSection;
		if(section == null) return;
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
		section.widthOverride = width;
		section.bodyHeightOverride = bodyHeight;
		section.offsetX = offsetX;
		section.offsetY = offsetY;
		clampSectionWindow(section, width);
	}

	private void clampSectionWindow(DropdownSection section, float width) {
		float minOffsetX = SCREEN_EDGE_MARGIN - getX();
		float maxOffsetX = getScreenWidth() - SCREEN_EDGE_MARGIN - width - getX();
		section.offsetX = Math.max(minOffsetX, Math.min(maxOffsetX, section.offsetX));
		float minOffsetY = SCREEN_EDGE_MARGIN - getY();
		float maxOffsetY = getScreenHeight() - SCREEN_EDGE_MARGIN - HEADER_HEIGHT - MIN_RESIZABLE_BODY_HEIGHT - getY();
		section.offsetY = Math.max(minOffsetY, Math.min(maxOffsetY, section.offsetY));
		if(section.bodyHeightOverride > 0.0F) {
			float absoluteY = getY() + section.offsetY;
			float maxBody = Math.max(MIN_RESIZABLE_BODY_HEIGHT,
					getScreenHeight() - SCREEN_EDGE_MARGIN - absoluteY - HEADER_HEIGHT);
			section.bodyHeightOverride = Math.min(section.bodyHeightOverride, maxBody);
		}
	}

	private void resetSectionLayout(DropdownSection section, float defaultWidth) {
		section.widthOverride = -1.0F;
		section.bodyHeightOverride = -1.0F;
		section.offsetX = (getWidth() - defaultWidth) / 2.0F;
		section.offsetY = Math.max(DEFAULT_SECTION_OFFSET_Y, SCREEN_EDGE_MARGIN - getY());
		section.scrollTarget = 0.0F;
		section.scrollAnimation.setValue(0.0F);
		section.open = true;
	}

	public boolean isResizeTutorialVisible() {
		return resizeTutorialActive || resizeTutorialAnimation.getValue() > 0.02F;
	}

	private void dismissResizeTutorial() {
		resizeTutorialActive = false;
	}

	private void drawResizeTutorial(NanoVGManager nvg, ColorPalette palette, AccentColor accentColor) {
		if(!isResizableLayout()) return;
		resizeTutorialAnimation.setAnimation(resizeTutorialActive ? 1.0F : 0.0F, 18);
		float alpha = resizeTutorialAnimation.getValue();
		if(alpha <= 0.02F || sections.isEmpty()) return;

		DropdownSection section = sections.get(0);
		float defaultWidth = calculatePanelWidth();
		initializeSectionPositions(defaultWidth);
		float panelWidth = getPanelWidth(section, defaultWidth);
		float panelX = getX() + section.offsetX;
		float panelY = getY() + section.offsetY;
		float panelHeight = HEADER_HEIGHT + Math.max(MIN_RESIZABLE_BODY_HEIGHT, section.visibleBodyHeight);
		int shade = Math.min(205, Math.round(178.0F * alpha));

		// Four rectangles form a cut-out, leaving only the Ghost list undimmed.
		nvg.drawRect(0, 0, getScreenWidth(), Math.max(0.0F, panelY - 5.0F), new Color(3, 5, 10, shade));
		nvg.drawRect(0, panelY - 5.0F, Math.max(0.0F, panelX - 5.0F), panelHeight + 10.0F, new Color(3, 5, 10, shade));
		nvg.drawRect(panelX + panelWidth + 5.0F, panelY - 5.0F,
				Math.max(0.0F, getScreenWidth() - panelX - panelWidth - 5.0F), panelHeight + 10.0F,
				new Color(3, 5, 10, shade));
		nvg.drawRect(0, panelY + panelHeight + 5.0F, getScreenWidth(),
				Math.max(0.0F, getScreenHeight() - panelY - panelHeight - 5.0F), new Color(3, 5, 10, shade));

		Color glow = ColorUtils.applyAlpha(accentColor.getColor1(), Math.round(235.0F * alpha));
		nvg.drawRoundedGlow(panelX - 3.0F, panelY - 3.0F, panelWidth + 6.0F, panelHeight + 6.0F, 11.0F, glow, 9);
		nvg.drawGradientOutlineRoundedRect(panelX - 2.0F, panelY - 2.0F,
				panelWidth + 4.0F, panelHeight + 4.0F, 10.0F, 1.4F,
				ColorUtils.applyAlpha(accentColor.getColor1(), Math.round(255.0F * alpha)),
				ColorUtils.applyAlpha(accentColor.getColor2(), Math.round(255.0F * alpha)));

		float bubbleWidth = Math.min(285.0F, getScreenWidth() - 20.0F);
		float bubbleHeight = 74.0F;
		float bubbleX = Math.max(10.0F, Math.min(getScreenWidth() - bubbleWidth - 10.0F,
				panelX + panelWidth + 28.0F));
		if(bubbleX < panelX + panelWidth + 12.0F) bubbleX = Math.max(10.0F, panelX - bubbleWidth - 28.0F);
		float bubbleY = Math.max(10.0F, Math.min(getScreenHeight() - bubbleHeight - 10.0F,
				panelY + panelHeight * 0.5F - bubbleHeight * 0.5F));
		float scale = 0.94F + alpha * 0.06F;
		nvg.save();
		nvg.scale(bubbleX, bubbleY, bubbleWidth, bubbleHeight, scale);
		nvg.drawShadow(bubbleX, bubbleY, bubbleWidth, bubbleHeight, 12.0F, 7);
		nvg.drawRoundedRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight, 12.0F,
				translucent(palette.getBackgroundColor(ColorType.DARK), Math.round(245.0F * alpha)));
		nvg.drawOutlineRoundedRect(bubbleX + 0.5F, bubbleY + 0.5F, bubbleWidth - 1.0F, bubbleHeight - 1.0F,
				12.0F, 0.8F, new Color(255, 255, 255, Math.round(70.0F * alpha)));
		nvg.drawText("Resize the Ghost module list", bubbleX + 15.0F, bubbleY + 12.0F,
				new Color(255, 255, 255, Math.round(255.0F * alpha)), 12.0F, Fonts.SEMIBOLD);
		nvg.drawText("Drag any edge or corner to change its size.", bubbleX + 15.0F, bubbleY + 33.0F,
				new Color(225, 230, 242, Math.round(225.0F * alpha)), 8.8F, Fonts.REGULAR);
		nvg.drawText("Use Reset to restore it. Click anywhere to continue.", bubbleX + 15.0F, bubbleY + 49.0F,
				new Color(225, 230, 242, Math.round(200.0F * alpha)), 8.0F, Fonts.REGULAR);
		nvg.restore();

		float startX = bubbleX < panelX ? bubbleX + bubbleWidth : bubbleX;
		float startY = bubbleY + bubbleHeight / 2.0F;
		float endX = panelX + panelWidth - 5.0F;
		float endY = panelY + panelHeight - 5.0F;
		int dots = 13;
		for(int i = 0; i < dots; i++) {
			float t = i / (float) (dots - 1);
			float curve = (float) Math.sin(t * Math.PI) * 22.0F;
			float dotX = startX + (endX - startX) * t;
			float dotY = startY + (endY - startY) * t - curve;
			nvg.drawCircle(dotX, dotY, 1.15F + t * 0.55F,
					ColorUtils.applyAlpha(accentColor.getColor2(), Math.round(alpha * (100.0F + t * 145.0F))));
		}
		nvg.drawCenteredText("Ã¢â‚¬Âº", endX - 1.0F, endY - 8.0F,
				ColorUtils.applyAlpha(accentColor.getColor2(), Math.round(255.0F * alpha)), 16.0F, Fonts.SEMIBOLD);
	}

	private Color translucent(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private void positionSettingComponent(Comp comp, float cellX, float rowY, float cellWidth) {
		float controlX = cellX + cellWidth - 84.0F;
		if(comp instanceof CompToggleButton) {
			CompToggleButton toggle = (CompToggleButton) comp;
			toggle.setX(cellX + cellWidth - 34.0F);
			toggle.setY(rowY + 7.0F);
			toggle.setScale(0.85F);
		} else if(comp instanceof CompSlider) {
			CompSlider slider = (CompSlider) comp;
			slider.setX(controlX);
			slider.setY(rowY + 13.0F);
			slider.setWidth(72);
		} else if(comp instanceof CompComboBox || comp instanceof CompKeybind) {
			comp.setX(controlX);
			comp.setY(rowY + 6.0F);
		} else if(comp instanceof CompModTextBox) {
			CompModTextBox textBox = (CompModTextBox) comp;
			textBox.setX(controlX);
			textBox.setY(rowY + 6.0F);
			textBox.setWidth(75);
			textBox.setHeight(16);
		} else if(comp instanceof CompColorPicker) {
			CompColorPicker picker = (CompColorPicker) comp;
			picker.setX(cellX + cellWidth - 102.0F);
			picker.setY(rowY + 6.0F);
			picker.setScale(0.8F);
		} else if(comp instanceof CompImageSelect || comp instanceof CompSoundSelect) {
			comp.setX(cellX + cellWidth - 18.0F);
			comp.setY(rowY + 6.0F);
		}
	}

	private void drawBindingPrompt(NanoVGManager nvg, ColorPalette palette, AccentColor accentColor) {
		float promptWidth = 250;
		float promptHeight = 30;
		float promptX = getX() + getWidth() / 2.0F - promptWidth / 2.0F;
		float promptY = getY() + getHeight() - 39;
		String bindText = "Press a key for " + bindingMod.getName() + "  Ã¢â‚¬Â¢  right click to clear";
		nvg.drawShadow(promptX, promptY, promptWidth, promptHeight, 8);
		nvg.drawRoundedRect(promptX, promptY, promptWidth, promptHeight, 8,
				translucent(palette.getBackgroundColor(ColorType.DARK), 145));
		nvg.drawGradientOutlineRoundedRect(promptX, promptY, promptWidth, promptHeight, 8, 0.8F,
				accentColor.getColor1(), accentColor.getColor2());
		nvg.drawCenteredText(bindText, promptX + promptWidth / 2.0F, promptY + 10.5F,
				palette.getFontColor(ColorType.DARK), 9, Fonts.MEDIUM);
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		if(mouseButton == 0 && isResizeTutorialVisible()) {
			dismissResizeTutorial();
			return;
		}
		if(bindingMod != null) {
			if(mouseButton == 1) {
				bindingMod.setKeyCode(Keyboard.KEY_NONE);
				bindingMod = null;
				setCanClose(true);
				return;
			}
			if(mouseButton == 2) {
				bindingMod = null;
				setCanClose(true);
				return;
			}
		}

		handleDropdownClick(mouseX, mouseY, mouseButton);
	}

	private boolean handleDropdownClick(int mouseX, int mouseY, int mouseButton) {
		float defaultPanelWidth = calculatePanelWidth();
		initializeSectionPositions(defaultPanelWidth);

		for(int i = sections.size() - 1; i >= 0; i--) {
			DropdownSection section = sections.get(i);
			float panelWidth = getPanelWidth(section, defaultPanelWidth);
			float panelX = getX() + section.offsetX;
			float panelY = getY() + section.offsetY;
			float bodyY = panelY + HEADER_HEIGHT;
			float totalHeight = HEADER_HEIGHT + Math.max(MIN_RESIZABLE_BODY_HEIGHT, section.visibleBodyHeight);

			if(mouseButton == 0 && isResizableLayout()) {
				float countWidth = Math.max(17.0F, section.lastCountWidth);
				float arrowX = panelX + panelWidth - 13.0F;
				float resetWidth = 34.0F;
				float resetX = arrowX - countWidth - resetWidth - 12.0F;
				if(MouseUtils.isInside(mouseX, mouseY, resetX, panelY + 6.0F, resetWidth, 16.0F)) {
					resetSectionLayout(section, defaultPanelWidth);
					return true;
				}
				if(section.open) {
					int edges = getResizeEdges(mouseX, mouseY, panelX, panelY, panelWidth, totalHeight);
					if(edges != 0) {
						beginResize(section, edges, mouseX, mouseY, panelWidth);
						return true;
					}
				}
			}

			if(MouseUtils.isInside(mouseX, mouseY, panelX, panelY, panelWidth, HEADER_HEIGHT) && mouseButton == 0) {
				draggingSection = section;
				sectionDragX = mouseX - panelX;
				sectionDragY = mouseY - panelY;
				sectionPressX = mouseX;
				sectionPressY = mouseY;
				section.moved = false;
				// The grabbed panel is rendered above overlapping panels.
				sections.remove(section);
				sections.add(section);
				return true;
			}

			if(!section.open || !MouseUtils.isInside(mouseX, mouseY, panelX, bodyY, panelWidth, section.visibleBodyHeight)) continue;
			List<Mod> modules = getSectionModules(section);
			float rowY = bodyY;
			for(Mod mod : modules) {
				float screenRowY = rowY + section.scrollAnimation.getValue();
				if(MouseUtils.isInside(mouseX, mouseY, panelX, screenRowY, panelWidth, ROW_HEIGHT)) {
					if(mouseButton == 0) {
						mod.toggle();
						return true;
					}
					if(mouseButton == 1 && Glide.getInstance().getModManager().getSettingsByMod(mod) != null) {
						if(openSetting && currentMod == mod) closeSettings();
						else openSettings(mod, section);
						return true;
					}
					if(mouseButton == 2) {
						bindingMod = mod;
						setCanClose(false);
						return true;
					}
				}

				rowY += ROW_HEIGHT;
				if(openSetting && currentMod == mod) {
					float inlineHeight = getInlineSettingsHeight();
					float screenInlineY = rowY + section.scrollAnimation.getValue();
					if(MouseUtils.isInside(mouseX, mouseY, panelX, screenInlineY, panelWidth, inlineHeight)) {
						if(mouseButton == 0) {
							int componentMouseY = (int) (mouseY - section.scrollAnimation.getValue());
							for(ModuleSetting entry : comps) {
								entry.comp.mouseClicked(mouseX, componentMouseY, mouseButton);
								if(entry.comp instanceof CompColorPicker) {
									CompColorPicker picker = (CompColorPicker) entry.comp;
									if(picker.isInsideOpen(mouseX, componentMouseY)) {
										shiftRowsBelowColorPicker(entry, picker.isOpen() ?
												(picker.isShowAlpha() ? 100 : 85) : -(picker.isShowAlpha() ? 100 : 85));
									}
								}
							}
						}
						return true;
					}
					rowY += inlineHeight;
				}
			}
		}
		return false;
	}

	private void openSettings(Mod mod, DropdownSection section) {
		ArrayList<Setting> settings = Glide.getInstance().getModManager().getSettingsByMod(mod);
		if(settings == null) return;
		comps.clear();

		for(Setting setting : settings) {
			Comp comp = createComponent(setting);
			if(comp != null) comps.add(new ModuleSetting(setting, comp));
		}

		getSearchBox().setFocused(false);
		currentMod = mod;
		settingsSection = section;
		openSetting = true;
		setCanClose(false);
	}

	private void closeSettings() {
		openSetting = false;
		currentMod = null;
		settingsSection = null;
		setCanClose(bindingMod == null);
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
		int sourceIndex = comps.indexOf(source);
		for(int i = sourceIndex + 1; i < comps.size(); i++) {
			comps.get(i).openY += amount;
		}
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
		if(mouseButton == 0 && resizingSection != null) {
			resizingSection = null;
			resizeEdges = 0;
		}
		if(mouseButton == 0 && draggingSection != null) {
			if(!draggingSection.moved) draggingSection.open = !draggingSection.open;
			draggingSection = null;
		}
		if(openSetting && settingsSection != null && mouseButton == 0) {
			for(ModuleSetting entry : comps) {
				entry.comp.mouseReleased(mouseX,
						(int) (mouseY - settingsSection.scrollAnimation.getValue()), mouseButton);
			}
		}
	}

	@Override
	public void keyTyped(char typedChar, int keyCode) {
		if(bindingMod != null) {
			if(keyCode == Keyboard.KEY_ESCAPE) {
				bindingMod = null;
				setCanClose(true);
				return;
			}
			if(keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK) {
				bindingMod.setKeyCode(Keyboard.KEY_NONE);
				bindingMod = null;
				setCanClose(true);
				return;
			}
			if(keyCode != Keyboard.KEY_NONE) {
				bindingMod.setKeyCode(keyCode);
				bindingMod = null;
				setCanClose(true);
			}
			return;
		}

		if(openSetting && keyCode == Keyboard.KEY_ESCAPE) {
			closeSettings();
			return;
		}

		if(openSetting) {
			for(ModuleSetting entry : comps) entry.comp.keyTyped(typedChar, keyCode);
			return;
		}
	}

	private static final class DropdownSection {
		private final String title;
		private final char start, end;
		private final boolean ghost;
		private final SimpleAnimation openAnimation = new SimpleAnimation();
		private final SimpleAnimation hoverAnimation = new SimpleAnimation();
		private final SimpleAnimation scrollAnimation = new SimpleAnimation();
		private boolean open = true;
		private float scrollTarget;
		private float maxScroll;
		private float visibleBodyHeight;
		private float offsetX, offsetY;
		private float widthOverride = -1.0F;
		private float bodyHeightOverride = -1.0F;
		private float lastCountWidth = 17.0F;
		private boolean moved;

		private DropdownSection(String title, char start, char end, boolean ghost) {
			this.title = title;
			this.start = start;
			this.end = end;
			this.ghost = ghost;
		}
	}

	private static final class ModuleSetting {
		private final SimpleAnimation openAnimation = new SimpleAnimation();
		private final Setting setting;
		private final Comp comp;
		private float openY;

		private ModuleSetting(Setting setting, Comp comp) {
			this.setting = setting;
			this.comp = comp;
		}
	}
}
