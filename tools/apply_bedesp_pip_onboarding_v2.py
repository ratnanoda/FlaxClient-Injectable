from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


def write(path, value):
    (ROOT / path).write_text(value, encoding="utf-8")


def replace_once(text, old, new, label):
    if old not in text:
        raise RuntimeError("Missing source marker: " + label)
    return text.replace(old, new, 1)


# PiP decoder output is capped independently of the Minecraft render rate.
youtube_path = "src/main/java/me/eldodebug/soar/management/youtube/YouTubeManager.java"
youtube = read(youtube_path)
youtube = replace_once(
    youtube,
    '                        "scale=" + videoWidth + ":" + videoHeight + ":force_original_aspect_ratio=decrease,pad="',
    '                        "fps=60,scale=" + videoWidth + ":" + videoHeight + ":force_original_aspect_ratio=decrease,pad="',
    "YouTube FFmpeg filter")
write(youtube_path, youtube)


# Rebuild BedESP item labels as screen-space panels projected from each bed.
bed_path = "src/main/java/me/eldodebug/soar/management/mods/impl/BedESPMod.java"
bed = read(bed_path)
bed = replace_once(bed,
    "import me.eldodebug.soar.management.event.impl.EventRender3D;\n",
    "import me.eldodebug.soar.management.event.impl.EventRender2D;\nimport me.eldodebug.soar.management.event.impl.EventRender3D;\n",
    "BedESP EventRender2D import")
bed = replace_once(bed,
    "import me.eldodebug.soar.utils.render.RenderUtils;\n",
    "import me.eldodebug.soar.utils.render.RenderUtils;\nimport me.eldodebug.soar.utils.render.WorldToScreen;\n",
    "BedESP WorldToScreen import")
bed = replace_once(bed,
    "import net.minecraft.client.renderer.GlStateManager;\n",
    "import net.minecraft.client.gui.ScaledResolution;\nimport net.minecraft.client.renderer.GlStateManager;\n",
    "BedESP ScaledResolution import")
bed = replace_once(bed,
    "\tpublic void onRender3D(EventRender3D event) {\n\t\tif(mc.theWorld == null || mc.thePlayer == null || beds.isEmpty()) return;\n",
    "\tpublic void onRender3D(EventRender3D event) {\n\t\tif(mc.theWorld == null || mc.thePlayer == null || beds.isEmpty()) return;\n\n\t\t// Capture the active camera matrices once. Item labels are rendered later\n\t\t// as flat screen-space panels, avoiding billboard pitch and third-person flips.\n\t\tWorldToScreen.capture();\n",
    "BedESP matrix capture")
old_tail = """
\t\tif(showBedColorSetting.isToggled() || checkDefBlockSetting.isToggled()) {
\t\t\tfor(Bed bed : beds) {
\t\t\t\tList<ItemStack> icons = buildDisplayIcons(bed);
\t\t\t\tif(!icons.isEmpty()) renderIconsAboveBed(bed, icons, rm);
\t\t\t}
\t\t}
\t}
"""
new_tail = """
\t}

\t@EventTarget
\tpublic void onRender2D(EventRender2D event) {
\t\tif(mc.theWorld == null || mc.thePlayer == null || beds.isEmpty()) return;
\t\tif(!showBedColorSetting.isToggled() && !checkDefBlockSetting.isToggled()) return;

\t\tScaledResolution resolution = new ScaledResolution(mc);
\t\tList<ProjectedPanel> panels = new ArrayList<ProjectedPanel>();
\t\tfor(Bed bed : beds) {
\t\t\tList<ItemStack> icons = buildDisplayIcons(bed);
\t\t\tif(icons.isEmpty()) continue;

\t\t\tdouble centerX = (bed.box.minX + bed.box.maxX) / 2.0D;
\t\t\tdouble centerY = bed.box.maxY + 0.82D;
\t\t\tdouble centerZ = (bed.box.minZ + bed.box.maxZ) / 2.0D;
\t\t\tfloat[] screen = WorldToScreen.project(centerX, centerY, centerZ);
\t\t\tif(screen == null) continue;

\t\t\tdouble distance = mc.thePlayer.getDistance(centerX, centerY, centerZ);
\t\t\tif(distance > MAX_SCAN_CHUNKS * 16.0D + 16.0D) continue;
\t\t\tpanels.add(new ProjectedPanel(screen[0], screen[1], distance, icons));
\t\t}

\t\t// Far labels first, so closer beds remain readable when projected panels overlap.
\t\tCollections.sort(panels, (first, second) -> Double.compare(second.distance, first.distance));
\t\tfor(ProjectedPanel panel : panels) {
\t\t\trenderProjectedIcons(panel, resolution);
\t\t}
\t}
"""
bed = replace_once(bed, old_tail, new_tail, "BedESP 3D icon loop")

method_pattern = re.compile(r"\n\tprivate void renderIconsAboveBed\(Bed bed, List<ItemStack> icons, RenderManager rm\) \{.*?\n\t\}\n\n\tprivate static class DefenseIcon", re.S)
method_replacement = r'''
	private void renderProjectedIcons(ProjectedPanel panel, ScaledResolution resolution) {
		List<ItemStack> icons = panel.icons;
		int columns = Math.min(MAX_ICONS_PER_ROW, icons.size());
		int rows = (icons.size() + MAX_ICONS_PER_ROW - 1) / MAX_ICONS_PER_ROW;
		int contentWidth = columns * ICON_SIZE + Math.max(0, columns - 1) * ICON_GAP;
		int panelWidth = contentWidth + PANEL_PADDING * 2;
		int panelHeight = rows * ICON_SIZE + Math.max(0, rows - 1) * ICON_GAP + PANEL_PADDING * 2;

		// Keep labels a consistent GUI size. A small distance reduction prevents
		// far-away panels from dominating the screen without causing perspective drift.
		float scale = (float) Math.max(0.78D, Math.min(1.0D, 1.04D - panel.distance / 520.0D));
		float scaledWidth = panelWidth * scale;
		float scaledHeight = panelHeight * scale;
		float centerX = Math.max(scaledWidth / 2.0F + 3.0F,
				Math.min(resolution.getScaledWidth() - scaledWidth / 2.0F - 3.0F, panel.screenX));
		float bottomY = Math.max(scaledHeight + 3.0F,
				Math.min(resolution.getScaledHeight() - 3.0F, panel.screenY));

		GlStateManager.pushMatrix();
		GlStateManager.translate(centerX, bottomY - scaledHeight, 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.enableAlpha();
		GlStateManager.enableTexture2D();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		Color accent = colorSetting.getColor();
		RenderUtils.drawRoundedRect(-panelWidth / 2.0F - 1.5F, -1.5F,
				panelWidth + 3.0F, panelHeight + 3.0F, 5.0F, new Color(0, 0, 0, 105));
		RenderUtils.drawRoundedRect(-panelWidth / 2.0F, 0.0F,
				panelWidth, panelHeight, 4.0F, new Color(0, 0, 0, 191));
		RenderUtils.drawRoundedOutline(-panelWidth / 2.0F, 0.0F,
				panelWidth, panelHeight, 4.0F, 0.8F,
				new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 205));

		GlStateManager.enableRescaleNormal();
		GlStateManager.enableColorMaterial();
		RenderHelper.enableGUIStandardItemLighting();
		float oldZLevel = mc.getRenderItem().zLevel;
		mc.getRenderItem().zLevel = 0.0F;
		for(int i = 0; i < icons.size(); i++) {
			int row = i / MAX_ICONS_PER_ROW;
			int column = i % MAX_ICONS_PER_ROW;
			int itemsInRow = Math.min(MAX_ICONS_PER_ROW, icons.size() - row * MAX_ICONS_PER_ROW);
			int rowWidth = itemsInRow * ICON_SIZE + Math.max(0, itemsInRow - 1) * ICON_GAP;
			int itemX = -rowWidth / 2 + column * (ICON_SIZE + ICON_GAP);
			int itemY = PANEL_PADDING + row * (ICON_SIZE + ICON_GAP);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			mc.getRenderItem().renderItemAndEffectIntoGUI(icons.get(i), itemX, itemY);
		}
		mc.getRenderItem().zLevel = oldZLevel;

		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableColorMaterial();
		GlStateManager.depthMask(true);
		GlStateManager.enableDepth();
		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}

	private static class ProjectedPanel {
		private final float screenX, screenY;
		private final double distance;
		private final List<ItemStack> icons;

		private ProjectedPanel(float screenX, float screenY, double distance, List<ItemStack> icons) {
			this.screenX = screenX;
			this.screenY = screenY;
			this.distance = distance;
			this.icons = icons;
		}
	}

	private static class DefenseIcon'''
bed, count = method_pattern.subn(method_replacement, bed, count=1)
if count != 1:
    raise RuntimeError("Missing source marker: BedESP billboard method")
write(bed_path, bed)


# Ghost window: rounded-corner grip and first-session resize spotlight.
module_path = "src/main/java/me/eldodebug/soar/gui/modmenu/category/impl/ModuleCategory.java"
module = read(module_path)
module = replace_once(module,
    "\tprivate static final float RESIZE_EDGE = 5.0F;\n",
    "\tprivate static final float RESIZE_EDGE = 5.0F;\n\tprivate static boolean resizeTutorialClaimed;\n",
    "ModuleCategory tutorial session flag")
module = replace_once(module,
    "\tprivate float resizeStartWidth, resizeStartBodyHeight;\n",
    "\tprivate float resizeStartWidth, resizeStartBodyHeight;\n\tprivate final SimpleAnimation resizeTutorialAnimation = new SimpleAnimation();\n\tprivate boolean resizeTutorialActive;\n",
    "ModuleCategory tutorial fields")
module = replace_once(module,
    "\t@Override\n\tpublic void initCategory() {\n\t\tresetScene();\n\t}\n",
    "\t@Override\n\tpublic void initCategory() {\n\t\tresetScene();\n\t\tresizeTutorialActive = isResizableLayout()\n\t\t\t\t&& Glide.getInstance().getFileManager().isFirstInstallation()\n\t\t\t\t&& !resizeTutorialClaimed;\n\t\tif(resizeTutorialActive) {\n\t\t\tresizeTutorialClaimed = true;\n\t\t\tresizeTutorialAnimation.setValue(0.0F);\n\t\t}\n\t}\n",
    "ModuleCategory tutorial initialization")
module = replace_once(module,
    "\t\tdrawDropdowns(nvg, palette, accentColor, mouseX, mouseY, partialTicks);\n\n\t\tif(bindingMod != null) {",
    "\t\tdrawDropdowns(nvg, palette, accentColor, mouseX, mouseY, partialTicks);\n\t\tdrawResizeTutorial(nvg, palette, accentColor);\n\n\t\tif(bindingMod != null) {",
    "ModuleCategory tutorial draw call")
old_grip = """
\tprivate void drawResizeHandles(NanoVGManager nvg, float x, float y, float width, float height,
\t\t\tint mouseX, int mouseY) {
\t\tint edges = getResizeEdges(mouseX, mouseY, x, y, width, height);
\t\tColor edge = new Color(255, 255, 255, edges == 0 ? 28 : 82);
\t\tnvg.drawRect(x + width - 10.0F, y + height - 2.0F, 8.0F, 1.0F, edge);
\t\tnvg.drawRect(x + width - 2.0F, y + height - 10.0F, 1.0F, 8.0F, edge);
\t}
"""
new_grip = """
\tprivate void drawResizeHandles(NanoVGManager nvg, float x, float y, float width, float height,
\t\t\tint mouseX, int mouseY) {
\t\tint edges = getResizeEdges(mouseX, mouseY, x, y, width, height);
\t\tColor edge = new Color(255, 255, 255, edges == 0 ? 34 : 105);
\t\t// Clip two concentric rounded outlines to the corner. The grip now follows
\t\t// the panel radius instead of drawing a square L over a rounded window.
\t\tnvg.save();
\t\tnvg.scissor(x + width - 15.0F, y + height - 15.0F, 15.0F, 15.0F);
\t\tnvg.drawOutlineRoundedRect(x + width - 14.0F, y + height - 14.0F,
\t\t\t\t13.0F, 13.0F, 7.0F, 1.0F, edge);
\t\tnvg.drawOutlineRoundedRect(x + width - 10.0F, y + height - 10.0F,
\t\t\t\t9.0F, 9.0F, 5.0F, 0.8F, edge);
\t\tnvg.restore();
\t}
"""
module = replace_once(module, old_grip, new_grip, "ModuleCategory rounded resize grip")
module = replace_once(module,
    "\tprivate Color translucent(Color color, int alpha) {\n",
    """\tpublic boolean isResizeTutorialVisible() {
\t\treturn resizeTutorialActive || resizeTutorialAnimation.getValue() > 0.02F;
\t}

\tprivate void dismissResizeTutorial() {
\t\tresizeTutorialActive = false;
\t}

\tprivate void drawResizeTutorial(NanoVGManager nvg, ColorPalette palette, AccentColor accentColor) {
\t\tif(!isResizableLayout()) return;
\t\tresizeTutorialAnimation.setAnimation(resizeTutorialActive ? 1.0F : 0.0F, 18);
\t\tfloat alpha = resizeTutorialAnimation.getValue();
\t\tif(alpha <= 0.02F || sections.isEmpty()) return;

\t\tDropdownSection section = sections.get(0);
\t\tfloat defaultWidth = calculatePanelWidth();
\t\tinitializeSectionPositions(defaultWidth);
\t\tfloat panelWidth = getPanelWidth(section, defaultWidth);
\t\tfloat panelX = getX() + section.offsetX;
\t\tfloat panelY = getY() + section.offsetY;
\t\tfloat panelHeight = HEADER_HEIGHT + Math.max(MIN_RESIZABLE_BODY_HEIGHT, section.visibleBodyHeight);
\t\tint shade = Math.min(205, Math.round(178.0F * alpha));

\t\t// Four rectangles form a cut-out, leaving only the Ghost list undimmed.
\t\tnvg.drawRect(0, 0, getScreenWidth(), Math.max(0.0F, panelY - 5.0F), new Color(3, 5, 10, shade));
\t\tnvg.drawRect(0, panelY - 5.0F, Math.max(0.0F, panelX - 5.0F), panelHeight + 10.0F, new Color(3, 5, 10, shade));
\t\tnvg.drawRect(panelX + panelWidth + 5.0F, panelY - 5.0F,
\t\t\t\tMath.max(0.0F, getScreenWidth() - panelX - panelWidth - 5.0F), panelHeight + 10.0F,
\t\t\t\tnew Color(3, 5, 10, shade));
\t\tnvg.drawRect(0, panelY + panelHeight + 5.0F, getScreenWidth(),
\t\t\t\tMath.max(0.0F, getScreenHeight() - panelY - panelHeight - 5.0F), new Color(3, 5, 10, shade));

\t\tColor glow = ColorUtils.applyAlpha(accentColor.getColor1(), Math.round(235.0F * alpha));
\t\tnvg.drawRoundedGlow(panelX - 3.0F, panelY - 3.0F, panelWidth + 6.0F, panelHeight + 6.0F, 11.0F, glow, 9.0F);
\t\tnvg.drawGradientOutlineRoundedRect(panelX - 2.0F, panelY - 2.0F,
\t\t\t\tpanelWidth + 4.0F, panelHeight + 4.0F, 10.0F, 1.4F,
\t\t\t\tColorUtils.applyAlpha(accentColor.getColor1(), Math.round(255.0F * alpha)),
\t\t\t\tColorUtils.applyAlpha(accentColor.getColor2(), Math.round(255.0F * alpha)));

\t\tfloat bubbleWidth = Math.min(285.0F, getScreenWidth() - 20.0F);
\t\tfloat bubbleHeight = 74.0F;
\t\tfloat bubbleX = Math.max(10.0F, Math.min(getScreenWidth() - bubbleWidth - 10.0F,
\t\t\t\tpanelX + panelWidth + 28.0F));
\t\tif(bubbleX < panelX + panelWidth + 12.0F) bubbleX = Math.max(10.0F, panelX - bubbleWidth - 28.0F);
\t\tfloat bubbleY = Math.max(10.0F, Math.min(getScreenHeight() - bubbleHeight - 10.0F,
\t\t\t\tpanelY + panelHeight * 0.5F - bubbleHeight * 0.5F));
\t\tfloat scale = 0.94F + alpha * 0.06F;
\t\tnvg.save();
\t\tnvg.translate(bubbleX + bubbleWidth / 2.0F, bubbleY + bubbleHeight / 2.0F);
\t\tnvg.scale(scale, scale);
\t\tnvg.translate(-bubbleX - bubbleWidth / 2.0F, -bubbleY - bubbleHeight / 2.0F);
\t\tnvg.drawShadow(bubbleX, bubbleY, bubbleWidth, bubbleHeight, 12.0F, 7.0F);
\t\tnvg.drawRoundedRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight, 12.0F,
\t\t\t\ttranslucent(palette.getBackgroundColor(ColorType.DARK), Math.round(245.0F * alpha)));
\t\tnvg.drawOutlineRoundedRect(bubbleX + 0.5F, bubbleY + 0.5F, bubbleWidth - 1.0F, bubbleHeight - 1.0F,
\t\t\t\t12.0F, 0.8F, new Color(255, 255, 255, Math.round(70.0F * alpha)));
\t\tnvg.drawText("Resize the Ghost module list", bubbleX + 15.0F, bubbleY + 12.0F,
\t\t\t\tnew Color(255, 255, 255, Math.round(255.0F * alpha)), 12.0F, Fonts.SEMIBOLD);
\t\tnvg.drawText("Drag any edge or corner to change its size.", bubbleX + 15.0F, bubbleY + 33.0F,
\t\t\t\tnew Color(225, 230, 242, Math.round(225.0F * alpha)), 8.8F, Fonts.REGULAR);
\t\tnvg.drawText("Use Reset to restore it. Click anywhere to continue.", bubbleX + 15.0F, bubbleY + 49.0F,
\t\t\t\tnew Color(225, 230, 242, Math.round(200.0F * alpha)), 8.0F, Fonts.REGULAR);
\t\tnvg.restore();

\t\tfloat startX = bubbleX < panelX ? bubbleX + bubbleWidth : bubbleX;
\t\tfloat startY = bubbleY + bubbleHeight / 2.0F;
\t\tfloat endX = panelX + panelWidth - 5.0F;
\t\tfloat endY = panelY + panelHeight - 5.0F;
\t\tint dots = 13;
\t\tfor(int i = 0; i < dots; i++) {
\t\t\tfloat t = i / (float) (dots - 1);
\t\t\tfloat curve = (float) Math.sin(t * Math.PI) * 22.0F;
\t\t\tfloat dotX = startX + (endX - startX) * t;
\t\t\tfloat dotY = startY + (endY - startY) * t - curve;
\t\t\tnvg.drawCircle(dotX, dotY, 1.15F + t * 0.55F,
\t\t\t\t\tColorUtils.applyAlpha(accentColor.getColor2(), Math.round(alpha * (100.0F + t * 145.0F))));
\t\t}
\t\tnvg.drawCenteredText("›", endX - 1.0F, endY - 8.0F,
\t\t\t\tColorUtils.applyAlpha(accentColor.getColor2(), Math.round(255.0F * alpha)), 16.0F, Fonts.SEMIBOLD);
\t}

\tprivate Color translucent(Color color, int alpha) {\n""",
    "ModuleCategory tutorial methods")
module = replace_once(module,
    "\t@Override\n\tpublic void mouseClicked(int mouseX, int mouseY, int mouseButton) {\n\t\tif(bindingMod != null) {",
    "\t@Override\n\tpublic void mouseClicked(int mouseX, int mouseY, int mouseButton) {\n\t\tif(mouseButton == 0 && isResizeTutorialVisible()) {\n\t\t\tdismissResizeTutorial();\n\t\t\treturn;\n\t\t}\n\t\tif(bindingMod != null) {",
    "ModuleCategory tutorial click")
write(module_path, module)


# Do not place the beginner-guide prompt on top of the resize spotlight.
gui_path = "src/main/java/me/eldodebug/soar/gui/modmenu/GuiModMenu.java"
gui = read(gui_path)
gui = replace_once(gui,
    "\t\tdrawCurrentCategory(nvg, mouseX, mouseY, partialTicks, modulePage);\n\t\tbeginnerGuide.draw(mouseX, mouseY, partialTicks);\n",
    "\t\tdrawCurrentCategory(nvg, mouseX, mouseY, partialTicks, modulePage);\n\t\tboolean resizeTutorial = currentCategory instanceof ModuleCategory\n\t\t\t\t&& ((ModuleCategory) currentCategory).isResizeTutorialVisible();\n\t\tif(!resizeTutorial) beginnerGuide.draw(mouseX, mouseY, partialTicks);\n",
    "GuiModMenu tutorial draw ordering")
gui = replace_once(gui,
    "\tpublic void mouseClicked(int mouseX, int mouseY, int mouseButton) {\n\t\tif(beginnerGuide.mouseClicked(mouseX, mouseY, mouseButton)) return;\n",
    "\tpublic void mouseClicked(int mouseX, int mouseY, int mouseButton) {\n\t\tboolean resizeTutorial = currentCategory instanceof ModuleCategory\n\t\t\t\t&& ((ModuleCategory) currentCategory).isResizeTutorialVisible();\n\t\tif(!resizeTutorial && beginnerGuide.mouseClicked(mouseX, mouseY, mouseButton)) return;\n",
    "GuiModMenu tutorial click ordering")
write(gui_path, gui)

print("Applied BedESP screen projection, PiP 60 FPS cap, rounded Ghost grip, and first-run resize spotlight")
