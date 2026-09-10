package me.eldodebug.soar.gui.modmenu;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

import org.lwjgl.input.Keyboard;

import eu.shoroa.contrib.render.ShBlur;
import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.GuiEditHUD;
import me.eldodebug.soar.gui.modmenu.category.Category;
import me.eldodebug.soar.gui.modmenu.category.impl.CosmeticsCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.GamesCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.HomeCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.GhostCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.ModuleCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.ProfileCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.ScreenshotCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.SettingCategory;
import me.eldodebug.soar.gui.modmenu.category.impl.YouTubeCategory;
import me.eldodebug.soar.management.color.AccentColor;
import me.eldodebug.soar.management.color.ColorManager;
import me.eldodebug.soar.management.color.palette.ColorPalette;
import me.eldodebug.soar.management.color.palette.ColorType;
import me.eldodebug.soar.management.event.impl.EventRenderNotification;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.impl.InternalSettingsMod;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.management.nanovg.font.Fonts;
import me.eldodebug.soar.management.nanovg.font.LegacyIcon;
import me.eldodebug.soar.ui.comp.impl.field.CompSearchBox;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.animation.normal.Animation;
import me.eldodebug.soar.utils.animation.normal.Direction;
import me.eldodebug.soar.utils.animation.normal.easing.EaseBackIn;
import me.eldodebug.soar.utils.buffer.ScreenAnimation;
import me.eldodebug.soar.utils.file.FileUtils;
import me.eldodebug.soar.utils.mouse.MouseUtils;
import me.eldodebug.soar.utils.mouse.Scroll;
import me.eldodebug.soar.utils.render.BlurUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

/**
 * Floating glass workspace. The navigation rail and content surface are
 * intentionally independent; module dropdowns provide their own surfaces.
 */
public class GuiModMenu extends GuiScreen {

	private static final int SIDEBAR_WIDTH = 42;
	private static final int SIDEBAR_GRIP_HEIGHT = 13;
	private static final int NAV_ITEM_HEIGHT = 30;
	private static final float MIN_GUI_SCALE = 0.65F;
	private static final float MAX_GUI_SCALE = 1.30F;

	private Animation introAnimation;
	// x/width retain the legacy parent coordinate contract used by Category.
	private int x, y, width, height;
	private int contentX, contentY, contentWidth, contentHeight;
	private int sidebarX, sidebarY, sidebarHeight;
	// scaledWidth/Height are the logical GUI canvas. screenWidth/Height are the
	// actual Minecraft scaled resolution used for the background and animation.
	private int scaledWidth, scaledHeight;
	private int screenWidth, screenHeight;
	private float activeGuiScale = 1.0F;

	private final ArrayList<Category> categories = new ArrayList<Category>();
	private final ArrayList<Category> navigationCategories = new ArrayList<Category>();
	private Category currentCategory;
	private final ScreenAnimation screenAnimation = new ScreenAnimation();
	private final Scroll scroll = new Scroll();
	private final CompSearchBox searchBox = new CompSearchBox();
	private final BeginnerGuideOverlay beginnerGuide = new BeginnerGuideOverlay(this);

	private boolean toEditHUD, canClose;
	private boolean sidebarPositioned;
	private boolean draggingSidebar;
	private int sidebarDragX, sidebarDragY;

	private final ArrayList<Snowflake> snowflakes = new ArrayList<Snowflake>();
	private long lastSnowUpdate;

	public GuiModMenu() {
		categories.add(new HomeCategory(this));
		categories.add(new GhostCategory(this));
		categories.add(new YouTubeCategory(this));
		categories.add(new CosmeticsCategory(this));
		categories.add(new GamesCategory(this));
		categories.add(new ProfileCategory(this));
		categories.add(new ScreenshotCategory(this));
		// Settings remains a page, but the Flax button is its only navigation item.
		categories.add(new SettingCategory(this));
		for(Category category : categories) {
			if(!(category instanceof SettingCategory)) navigationCategories.add(category);
		}
		currentCategory = getCategoryByClass(GhostCategory.class);
	}

	@Override
	public void initGui() {
		ScaledResolution sr = new ScaledResolution(mc);
		screenWidth = sr.getScaledWidth();
		screenHeight = sr.getScaledHeight();

		float previousScale = activeGuiScale;
		double configuredScale = InternalSettingsMod.getInstance() == null
				? 1.0 : InternalSettingsMod.getInstance().getGuiScaleSetting().getValue();
		activeGuiScale = (float) Math.max(MIN_GUI_SCALE, Math.min(MAX_GUI_SCALE, configuredScale));

		// Use a virtual canvas so every component (including module dropdowns)
		// scales together and still lays itself out inside the visible screen.
		scaledWidth = Math.max(1, Math.round(screenWidth / activeGuiScale));
		scaledHeight = Math.max(1, Math.round(screenHeight / activeGuiScale));
		currentCategory = getCategoryByClass(GhostCategory.class);

		contentWidth = Math.min(798, Math.max(418, scaledWidth - 92));
		contentHeight = Math.min(360, Math.max(250, scaledHeight - 76));
		contentX = (scaledWidth - contentWidth) / 2 + 20;
		contentY = (scaledHeight - contentHeight) / 2;

		// Category historically adds 32px to the parent X coordinate.
		x = contentX - 32;
		y = contentY;
		width = contentWidth + 32;
		height = contentHeight;

		sidebarHeight = SIDEBAR_GRIP_HEIGHT + 34 + visibleCategories().size() * NAV_ITEM_HEIGHT + 39;
		boolean scaleChanged = Math.abs(previousScale - activeGuiScale) > 0.001F;
		if(!sidebarPositioned || scaleChanged) {
			sidebarX = Math.max(10, contentX - SIDEBAR_WIDTH - 14);
			sidebarY = (scaledHeight - sidebarHeight) / 2;
			sidebarPositioned = true;
		}
		clampSidebar();

		introAnimation = new EaseBackIn(320, 1.0F, 2.0F);
		introAnimation.setDirection(Direction.FORWARDS);
		for(Category category : categories) category.initGui();

		scroll.resetAll();
		toEditHUD = false;
		canClose = true;
		draggingSidebar = false;
		beginnerGuide.initGui();
		initSnow();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		NanoVGManager nvg = Glide.getInstance().getNanoVGManager();
		if(InternalSettingsMod.getInstance().getBlurSetting().isToggled()) {
			BlurUtils.drawBlurScreen((float) (Math.min(introAnimation.getValue(), 1) * 18) + 1F);
		}
		nvg.setupAndDraw(() -> drawAtmosphere(nvg));
		screenAnimation.wrap(() -> drawNanoVG(mouseX, mouseY, partialTicks), 0, 0, screenWidth, screenHeight,
				2 - introAnimation.getValueFloat(), Math.min(introAnimation.getValueFloat(), 1), false);
		new EventRenderNotification().call();
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private void drawNanoVG(int mouseX, int mouseY, float partialTicks) {
		Glide instance = Glide.getInstance();
		NanoVGManager nvg = instance.getNanoVGManager();
		ColorManager colorManager = instance.getColorManager();
		ColorPalette palette = colorManager.getPalette();
		AccentColor accent = colorManager.getCurrentColor();

		if(introAnimation.isDone(Direction.BACKWARDS)) {
			mc.displayGuiScreen(toEditHUD ? new GuiEditHUD(true) : null);
			return;
		}

		int guiMouseX = toGuiMouse(mouseX);
		int guiMouseY = toGuiMouse(mouseY);

		nvg.save();
		try {
			nvg.scale(0, 0, activeGuiScale);

			if(draggingSidebar) {
				sidebarX = guiMouseX - sidebarDragX;
				sidebarY = guiMouseY - sidebarDragY;
				clampSidebar();
			}

			drawSidebar(nvg, palette, accent, guiMouseX, guiMouseY);
			boolean modulePage = currentCategory instanceof ModuleCategory;
			if(!modulePage) drawContentGlass(nvg, palette, accent);
			drawFloatingToolbar(nvg, palette, accent, guiMouseX, guiMouseY, partialTicks, modulePage);
			drawCurrentCategory(nvg, guiMouseX, guiMouseY, partialTicks, modulePage);
			beginnerGuide.draw(guiMouseX, guiMouseY, partialTicks);

			if(!beginnerGuide.isOpen() && MouseUtils.isInside(guiMouseX, guiMouseY, contentX, contentY + 31, contentWidth, contentHeight - 31)) {
				scroll.onScroll();
			}
			scroll.onAnimation();
		} finally {
			nvg.restore();
		}
	}

	private int toGuiMouse(int coordinate) {
		return Math.round(coordinate / activeGuiScale);
	}

	private void drawSidebar(NanoVGManager nvg, ColorPalette palette, AccentColor accent, int mouseX, int mouseY) {
		drawGlass(nvg, sidebarX, sidebarY, SIDEBAR_WIDTH, sidebarHeight, 13, palette, true);
		// A small, unambiguous drag grip keeps navigation clicks from moving the rail.
		for(int i = 0; i < 3; i++) {
			nvg.drawCircle(sidebarX + 17 + i * 4, sidebarY + 6.5F, 0.9F, new Color(255, 255, 255, 100));
		}

		float logoX = sidebarX + 8;
		float logoY = sidebarY + SIDEBAR_GRIP_HEIGHT + 3;
		boolean settings = currentCategory instanceof SettingCategory;
		drawNavSelection(nvg, accent, logoX, logoY, settings);
		nvg.drawGradientRoundedRect(logoX + 1, logoY + 1, 24, 24, 8,
				ColorUtils.applyAlpha(accent.getColor1(), 205), ColorUtils.applyAlpha(accent.getColor2(), 205));
		nvg.drawRoundedImage(new ResourceLocation("soar/logo.png"), logoX + 5, logoY + 5, 16, 16, 7);

		int offset = 0;
		for(Category category : visibleCategories()) {
			float itemX = sidebarX + 8;
			float itemY = sidebarY + SIDEBAR_GRIP_HEIGHT + 38 + offset;
			boolean selected = category == currentCategory;
			drawNavSelection(nvg, accent, itemX, itemY, selected);
			Color iconColor = selected ? Color.WHITE : palette.getFontColor(ColorType.NORMAL, 205);
			nvg.drawCenteredGlyph(category.getIcon(), itemX + 13, itemY + 12, iconColor, 12, category.getIconFont());
			offset += NAV_ITEM_HEIGHT;
		}

		float hudX = sidebarX + 8;
		float hudY = sidebarY + sidebarHeight - 32;
		nvg.drawGradientRoundedRect(hudX, hudY, 26, 24, 7,
				ColorUtils.applyAlpha(accent.getColor1(), 165), ColorUtils.applyAlpha(accent.getColor2(), 165));
		nvg.drawCenteredGlyph(LegacyIcon.LAYOUT, hudX + 13, hudY + 12, Color.WHITE, 12, Fonts.LEGACYICON);
	}

	private void drawNavSelection(NanoVGManager nvg, AccentColor accent, float itemX, float itemY, boolean selected) {
		if(!selected) return;
		nvg.drawRoundedGlow(itemX, itemY, 26, 24, 7, ColorUtils.applyAlpha(accent.getColor1(), 105), 5);
		nvg.drawGradientRoundedRect(itemX, itemY, 26, 24, 7,
				ColorUtils.applyAlpha(accent.getColor1(), 190), ColorUtils.applyAlpha(accent.getColor2(), 190));
	}

	private void drawContentGlass(NanoVGManager nvg, ColorPalette palette, AccentColor accent) {
		drawGlass(nvg, contentX, contentY, contentWidth, contentHeight, 15, palette, false);
	}

	private void drawFloatingToolbar(NanoVGManager nvg, ColorPalette palette, AccentColor accent,
			int mouseX, int mouseY, float partialTicks, boolean modulePage) {
		if(currentCategory.isShowTitle()) {
			currentCategory.getTextAnimation().setAnimation(1.0F, 14);
			float titleX = modulePage ? scaledWidth / 2.0F : contentX + contentWidth / 2.0F;
			float titleY = modulePage ? 5.0F : contentY + 7.2F;
			nvg.drawCenteredText(currentCategory.getName(), titleX, titleY,
					palette.getFontColor(ColorType.DARK, 230), 12.5F, Fonts.SEMIBOLD);
		}

		if(currentCategory.isShowSearchBox() && !modulePage) {
			searchBox.setPosition(contentX + contentWidth - 175, contentY + 5, 160, 18);
			searchBox.draw(mouseX, mouseY, partialTicks);
		}

		if(Objects.equals(currentCategory.getNameKey(), TranslateText.COSMETICS.getKey())) {
			float folderX = contentX + contentWidth - 198;
			float folderY = contentY + 5;
			nvg.drawRoundedRect(folderX, folderY, 18, 18, 6, glassColor(palette, ColorType.DARK, 150));
			nvg.drawOutlineRoundedRect(folderX, folderY, 18, 18, 6, 0.7F, new Color(255, 255, 255, 42));
			nvg.drawCenteredText(LegacyIcon.FOLDER, folderX + 9, folderY + 4.5F,
					palette.getFontColor(ColorType.NORMAL), 9, Fonts.LEGACYICON);
		}
	}

	private void drawCurrentCategory(NanoVGManager nvg, int mouseX, int mouseY, float partialTicks, boolean modulePage) {
		for(Category category : categories) {
			category.getCategoryAnimation().setAnimation(category == currentCategory ? 1.0F : 0.0F, 16);
			if(category != currentCategory) {
				if(category.isInitialized()) category.setInitialized(false);
				continue;
			}

			if(!category.isInitialized()) {
				category.setInitialized(true);
				category.initCategory();
				searchBox.setText("");
				searchBox.setFocused(false);
				category.setCanClose(true);
			}

			nvg.save();
			int yOffset = category.isShowTitle() ? 31 : 0;
			if(!modulePage) nvg.scissor(contentX, contentY + yOffset, contentWidth, contentHeight - yOffset);
			nvg.translate(0, 34 - category.getCategoryAnimation().getValue() * 34);
			if(!modulePage) nvg.setAlpha(0.82F);
			category.drawScreen(mouseX, mouseY, partialTicks);
			nvg.restore();
		}
	}

	private void drawGlass(NanoVGManager nvg, float gx, float gy, float gw, float gh, float radius,
			ColorPalette palette, boolean stronger) {
		if(InternalSettingsMod.getInstance().getBlurSetting().isToggled()) {
			ShBlur.getInstance().drawBlur(() -> nvg.drawRoundedRect(gx, gy, gw, gh, radius, Color.WHITE));
		}
		nvg.drawShadow(gx, gy, gw, gh, radius, stronger ? 8 : 6);
		nvg.drawRoundedRect(gx, gy, gw, gh, radius,
				glassColor(palette, ColorType.DARK, stronger ? 148 : 118));
		nvg.drawOutlineRoundedRect(gx + 0.5F, gy + 0.5F, gw - 1, gh - 1, radius, 0.8F,
				new Color(255, 255, 255, stronger ? 48 : 36));
	}

	private Color glassColor(ColorPalette palette, ColorType type, int alpha) {
		Color base = palette.getBackgroundColor(type);
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		int guiMouseX = toGuiMouse(mouseX);
		int guiMouseY = toGuiMouse(mouseY);

		if(beginnerGuide.mouseClicked(guiMouseX, guiMouseY, mouseButton)) return;
		if(mouseButton == 0 && MouseUtils.isInside(guiMouseX, guiMouseY, sidebarX, sidebarY,
				SIDEBAR_WIDTH, SIDEBAR_GRIP_HEIGHT)) {
			draggingSidebar = true;
			sidebarDragX = guiMouseX - sidebarX;
			sidebarDragY = guiMouseY - sidebarY;
			return;
		}

		if(mouseButton == 0 && clickSidebarNavigation(guiMouseX, guiMouseY)) return;

		boolean insideContent = MouseUtils.isInside(guiMouseX, guiMouseY, contentX - 5, contentY - 5,
				contentWidth + 10, contentHeight + 10);
		boolean insideSidebar = MouseUtils.isInside(guiMouseX, guiMouseY, sidebarX - 4, sidebarY - 4,
				SIDEBAR_WIDTH + 8, sidebarHeight + 8);
		if(!(currentCategory instanceof ModuleCategory) && !insideContent && !insideSidebar && mouseButton == 0 && canClose) {
			introAnimation.setDirection(Direction.BACKWARDS);
		}

		currentCategory.mouseClicked(guiMouseX, guiMouseY, mouseButton);
		if(!(currentCategory instanceof ModuleCategory)) searchBox.mouseClicked(guiMouseX, guiMouseY, mouseButton);

		if(Objects.equals(currentCategory.getNameKey(), TranslateText.COSMETICS.getKey())) {
			float folderX = contentX + contentWidth - 198;
			float folderY = contentY + 5;
			if(MouseUtils.isInside(guiMouseX, guiMouseY, folderX, folderY, 18, 18)) {
				FileUtils.openFolderAtPath(Glide.getInstance().getFileManager().getCustomCapeDir());
			}
		}

		try {
			super.mouseClicked(guiMouseX, guiMouseY, mouseButton);
		} catch(IOException ignored) {}
	}

	private boolean clickSidebarNavigation(int mouseX, int mouseY) {
		float logoX = sidebarX + 8;
		float logoY = sidebarY + SIDEBAR_GRIP_HEIGHT + 3;
		if(MouseUtils.isInside(mouseX, mouseY, logoX, logoY, 26, 24)) {
			currentCategory = getCategoryByClass(SettingCategory.class);
			return true;
		}

		int offset = 0;
		for(Category category : visibleCategories()) {
			float itemX = sidebarX + 8;
			float itemY = sidebarY + SIDEBAR_GRIP_HEIGHT + 38 + offset;
			if(MouseUtils.isInside(mouseX, mouseY, itemX, itemY, 26, 24)) {
				currentCategory = category;
				return true;
			}
			offset += NAV_ITEM_HEIGHT;
		}

		float hudX = sidebarX + 8;
		float hudY = sidebarY + sidebarHeight - 32;
		if(MouseUtils.isInside(mouseX, mouseY, hudX, hudY, 26, 24)) {
			toEditHUD = true;
			introAnimation.setDirection(Direction.BACKWARDS);
			return true;
		}
		return false;
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
		int guiMouseX = toGuiMouse(mouseX);
		int guiMouseY = toGuiMouse(mouseY);
		if(mouseButton == 0) draggingSidebar = false;
		currentCategory.mouseReleased(guiMouseX, guiMouseY, mouseButton);
	}

	@Override
	public void keyTyped(char typedChar, int keyCode) {
		if(beginnerGuide.keyTyped(typedChar, keyCode)) return;
		boolean shiftDown = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
		if(keyCode == Keyboard.KEY_DELETE && shiftDown && Glide.getInstance().getMusicManager() != null
				&& Glide.getInstance().getMusicManager().isPlaying()) {
			Glide.getInstance().getMusicManager().disable();
			return;
		}
		boolean modulePage = currentCategory instanceof ModuleCategory;
		boolean controlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
		if(modulePage && canClose) {
			if(controlDown && keyCode == Keyboard.KEY_F) {
				searchBox.setFocused(true);
				return;
			}
			if(searchBox.isFocused()) {
				if(keyCode == Keyboard.KEY_ESCAPE) {
					searchBox.setText("");
					searchBox.setFocused(false);
					return;
				}
				searchBox.keyTyped(typedChar, keyCode);
				if(keyCode == Keyboard.KEY_BACK && searchBox.getText().isEmpty()) searchBox.setFocused(false);
				return;
			}
		}

		if(keyCode == Keyboard.KEY_ESCAPE && canClose && InternalSettingsMod.getInstance().getFastCloseEscSetting().isToggled()) {
			mc.displayGuiScreen(toEditHUD ? new GuiEditHUD(true) : null);
			return;
		}

		boolean couldCloseBeforeKey = canClose;
		currentCategory.keyTyped(typedChar, keyCode);
		if(keyCode == Keyboard.KEY_ESCAPE && !couldCloseBeforeKey && canClose) return;
		if(!modulePage) searchBox.keyTyped(typedChar, keyCode);
		if(!modulePage && currentCategory.isShowSearchBox() && canClose && keyCode == Keyboard.KEY_ESCAPE) {
			if(!searchBox.getText().isEmpty()) {
				searchBox.setText("");
				searchBox.setFocused(false);
				return;
			}
			if(searchBox.isFocused()) {
				searchBox.setFocused(false);
				return;
			}
		}
		if(keyCode == Keyboard.KEY_ESCAPE && canClose) introAnimation.setDirection(Direction.BACKWARDS);
	}

	private ArrayList<Category> visibleCategories() {
		return navigationCategories;
	}

	private void clampSidebar() {
		sidebarX = Math.max(4, Math.min(scaledWidth - SIDEBAR_WIDTH - 4, sidebarX));
		sidebarY = Math.max(4, Math.min(scaledHeight - sidebarHeight - 4, sidebarY));
	}

	private void initSnow() {
		snowflakes.clear();
		Random random = new Random(0xF1A8C11EL);
		int count = Math.max(55, Math.min(105, (screenWidth * screenHeight) / 5200));
		for(int i = 0; i < count; i++) {
			snowflakes.add(new Snowflake(random.nextFloat() * screenWidth, random.nextFloat() * screenHeight,
					0.55F + random.nextFloat() * 1.35F, 9.0F + random.nextFloat() * 20.0F,
					4.0F + random.nextFloat() * 9.0F, random.nextFloat() * 6.28318F));
		}
		lastSnowUpdate = System.nanoTime();
	}

	private void drawAtmosphere(NanoVGManager nvg) {
		long now = System.nanoTime();
		float delta = lastSnowUpdate == 0L ? 0 : Math.min(0.05F, (now - lastSnowUpdate) / 1_000_000_000.0F);
		lastSnowUpdate = now;
		float time = now / 1_000_000_000.0F;
		float alpha = Math.max(0, Math.min(introAnimation.getValueFloat(), 1));
		nvg.drawRect(0, 0, screenWidth, screenHeight, new Color(6, 9, 18, (int) (66 * alpha)));
		for(Snowflake flake : snowflakes) {
			flake.y += flake.speed * delta;
			flake.x += (float) Math.sin(time * 0.75F + flake.phase) * flake.drift * delta;
			if(flake.y > screenHeight + 4) {
				flake.y = -4;
				flake.x = (float) ((Math.sin(flake.phase * 13.37F + time) * 0.5F + 0.5F) * screenWidth);
			}
			if(flake.x < -4) flake.x = screenWidth + 4;
			if(flake.x > screenWidth + 4) flake.x = -4;
			int flakeAlpha = (int) ((65 + flake.radius * 52) * alpha);
			nvg.drawCircle(flake.x, flake.y, flake.radius,
					new Color(244, 249, 255, Math.min(170, flakeAlpha)));
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	@Override
	public void onGuiClosed() {
		Glide.getInstance().getProfileManager().save();
	}

	public int getX() { return x; }
	public int getY() { return y; }
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public int getScaledWidth() { return scaledWidth; }
	public int getScaledHeight() { return scaledHeight; }
	public ArrayList<Category> getCategories() { return categories; }
	public Scroll getScroll() { return scroll; }
	public CompSearchBox getSearchBox() { return searchBox; }
	public boolean isCanClose() { return canClose; }
	public void setCanClose(boolean canClose) { this.canClose = canClose; }

	public Category getCategoryByClass(Class<?> clazz) {
		for(Category category : categories) if(category.getClass().equals(clazz)) return category;
		return null;
	}

	private static final class Snowflake {
		private float x, y;
		private final float radius, speed, drift, phase;
		private Snowflake(float x, float y, float radius, float speed, float drift, float phase) {
			this.x = x;
			this.y = y;
			this.radius = radius;
			this.speed = speed;
			this.drift = drift;
			this.phase = phase;
		}
	}
}
