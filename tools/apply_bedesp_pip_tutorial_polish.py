from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def replace_once(text, old, new, label):
    if old not in text:
        raise RuntimeError("Missing replacement target: " + label)
    return text.replace(old, new, 1)


# Rebuild BedESP defense icons as a screen-space overlay. This avoids RenderItem
# depth/z-level state leaking into a 3D billboard and gives every camera mode the
# same stable anchor above the bed.
bed_path = ROOT / "src/main/java/me/eldodebug/soar/management/mods/impl/BedESPMod.java"
bed = bed_path.read_text(encoding="utf-8")
bed = replace_once(
    bed,
    "import me.eldodebug.soar.management.event.impl.EventRender3D;\n",
    "import me.eldodebug.soar.management.event.impl.EventRender2D;\n"
    "import me.eldodebug.soar.management.event.impl.EventRender3D;\n",
    "BedESP EventRender2D import",
)
bed = replace_once(
    bed,
    "import me.eldodebug.soar.utils.render.RenderUtils;\n",
    "import me.eldodebug.soar.utils.render.RenderUtils;\n"
    "import me.eldodebug.soar.utils.render.WorldToScreen;\n",
    "BedESP WorldToScreen import",
)
bed = replace_once(
    bed,
    "import net.minecraft.client.renderer.GlStateManager;\n",
    "import net.minecraft.client.gui.ScaledResolution;\n"
    "import net.minecraft.client.renderer.GlStateManager;\n",
    "BedESP ScaledResolution import",
)
bed = replace_once(
    bed,
    "\tpublic void onRender3D(EventRender3D event) {\n\t\tif(mc.theWorld == null || mc.thePlayer == null || beds.isEmpty()) return;\n\n\t\tRenderManager rm = mc.getRenderManager();",
    "\tpublic void onRender3D(EventRender3D event) {\n\t\tif(mc.theWorld == null || mc.thePlayer == null || beds.isEmpty()) return;\n\n"
    "\t\t// Capture the real world camera matrices before changing GL state. Icon\n"
    "\t\t// panels are projected from these matrices during EventRender2D.\n"
    "\t\tWorldToScreen.capture();\n\n"
    "\t\tRenderManager rm = mc.getRenderManager();",
    "BedESP matrix capture",
)
bed = replace_once(
    bed,
    "\n\t\tif(showBedColorSetting.isToggled() || checkDefBlockSetting.isToggled()) {\n"
    "\t\t\tfor(Bed bed : beds) {\n"
    "\t\t\t\tList<ItemStack> icons = buildDisplayIcons(bed);\n"
    "\t\t\t\tif(!icons.isEmpty()) renderIconsAboveBed(bed, icons, rm);\n"
    "\t\t\t}\n"
    "\t\t}\n",
    "\n",
    "remove 3D BedESP icon rendering",
)

new_renderer = r'''
	@EventTarget
	public void onRender2D(EventRender2D event) {
		if(mc.theWorld == null || mc.thePlayer == null || beds.isEmpty()) return;
		if(!showBedColorSetting.isToggled() && !checkDefBlockSetting.isToggled()) return;

		ScaledResolution resolution = new ScaledResolution(mc);
		for(Bed bed : beds) {
			List<ItemStack> icons = buildDisplayIcons(bed);
			if(icons.isEmpty()) continue;

			double centerX = (bed.box.minX + bed.box.maxX) / 2.0D;
			double centerY = bed.box.maxY + 0.72D;
			double centerZ = (bed.box.minZ + bed.box.maxZ) / 2.0D;
			float[] screen = WorldToScreen.project(centerX, centerY, centerZ);
			if(screen == null) continue;

			renderIconPanel2D(screen[0], screen[1], icons, resolution);
		}
	}

	private void renderIconPanel2D(float anchorX, float anchorY, List<ItemStack> icons,
			ScaledResolution resolution) {
		int columns = Math.min(MAX_ICONS_PER_ROW, icons.size());
		int rows = (icons.size() + MAX_ICONS_PER_ROW - 1) / MAX_ICONS_PER_ROW;
		int widestRow = Math.min(MAX_ICONS_PER_ROW, icons.size());
		int contentWidth = widestRow * ICON_SIZE + Math.max(0, widestRow - 1) * ICON_GAP;
		int panelWidth = contentWidth + PANEL_PADDING * 2;
		int panelHeight = rows * ICON_SIZE + Math.max(0, rows - 1) * ICON_GAP + PANEL_PADDING * 2;

		// The panel's bottom edge is the anchor. This keeps one or two rows moving
		// upward instead of growing through the bed marker.
		float panelX = anchorX - panelWidth / 2.0F;
		float panelY = anchorY - panelHeight - 9.0F;
		panelX = Math.max(3.0F, Math.min(resolution.getScaledWidth() - panelWidth - 3.0F, panelX));
		panelY = Math.max(3.0F, Math.min(resolution.getScaledHeight() - panelHeight - 3.0F, panelY));

		GlStateManager.pushMatrix();
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.enableAlpha();
		GlStateManager.enableTexture2D();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		Color accent = colorSetting.getColor();
		RenderUtils.drawRoundedRect(panelX - 1.5F, panelY - 1.5F,
				panelWidth + 3.0F, panelHeight + 3.0F, 5.0F, new Color(0, 0, 0, 105));
		RenderUtils.drawRoundedRect(panelX, panelY,
				panelWidth, panelHeight, 4.0F, new Color(0, 0, 0, 191));
		RenderUtils.drawRoundedOutline(panelX, panelY,
				panelWidth, panelHeight, 4.0F, 0.8F,
				new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 205));

		GlStateManager.enableRescaleNormal();
		GlStateManager.enableColorMaterial();
		RenderHelper.enableGUIStandardItemLighting();
		float oldZLevel = mc.getRenderItem().zLevel;
		mc.getRenderItem().zLevel = 200.0F;
		try {
			for(int i = 0; i < icons.size(); i++) {
				int row = i / MAX_ICONS_PER_ROW;
				int column = i % MAX_ICONS_PER_ROW;
				int itemsInRow = Math.min(MAX_ICONS_PER_ROW, icons.size() - row * MAX_ICONS_PER_ROW);
				int rowWidth = itemsInRow * ICON_SIZE + Math.max(0, itemsInRow - 1) * ICON_GAP;
				int itemX = Math.round(panelX + (panelWidth - rowWidth) / 2.0F
						+ column * (ICON_SIZE + ICON_GAP));
				int itemY = Math.round(panelY + PANEL_PADDING + row * (ICON_SIZE + ICON_GAP));
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				mc.getRenderItem().renderItemAndEffectIntoGUI(icons.get(i), itemX, itemY);
			}
		} finally {
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
	}
'''
pattern = re.compile(r"\n\tprivate void renderIconsAboveBed\(Bed bed, List<ItemStack> icons, RenderManager rm\) \{.*?\n\t\}\n\n\tprivate static class DefenseIcon", re.S)
if not pattern.search(bed):
    raise RuntimeError("Missing BedESP 3D renderer method")
bed = pattern.sub("\n" + new_renderer + "\n\tprivate static class DefenseIcon", bed, count=1)
bed_path.write_text(bed, encoding="utf-8")


# Cap PiP output to the source frame rate or 60 FPS, whichever is lower. The
# old fps=60 filter duplicated low-FPS material up to 60 instead of merely
# enforcing a ceiling.
yt_path = ROOT / "src/main/java/me/eldodebug/soar/management/youtube/YouTubeManager.java"
yt = yt_path.read_text(encoding="utf-8")
yt = replace_once(
    yt,
    '"fps=60,scale=" + videoWidth + ":" + videoHeight',
    '"fps=fps=\'min(source_fps,60)\',scale=" + videoWidth + ":" + videoHeight',
    "YouTube PiP 60 FPS ceiling",
)
yt_path.write_text(yt, encoding="utf-8")


# Polish the resize affordance so it traces the existing rounded corner and fix
# the tutorial arrow text to plain ASCII for every Windows/JVM locale.
module_path = ROOT / "src/main/java/me/eldodebug/soar/gui/modmenu/category/impl/ModuleCategory.java"
module = module_path.read_text(encoding="utf-8")
handle_pattern = re.compile(
    r"\tprivate void drawResizeHandles\(NanoVGManager nvg, float x, float y, float width, float height,\n"
    r"\t\t\tint mouseX, int mouseY\) \{.*?\n\t\}\n\n\tprivate int getResizeEdges",
    re.S,
)
new_handle = r'''	private void drawResizeHandles(NanoVGManager nvg, float x, float y, float width, float height,
			int mouseX, int mouseY) {
		int edges = getResizeEdges(mouseX, mouseY, x, y, width, height);
		int alpha = edges == 0 ? 52 : 132;
		Color edge = new Color(255, 255, 255, alpha);

		// Three dotted quarter-arcs follow the panel's 8px rounded corner instead
		// of adding a square or detached resize glyph.
		float cornerX = x + width - 1.5F;
		float cornerY = y + height - 1.5F;
		for(int ring = 0; ring < 3; ring++) {
			float radius = 4.0F + ring * 3.2F;
			for(int point = 1; point <= 4; point++) {
				float angle = (float) (Math.PI * 0.5D * point / 5.0D);
				float dotX = cornerX - (float) Math.cos(angle) * radius;
				float dotY = cornerY - (float) Math.sin(angle) * radius;
				nvg.drawCircle(dotX, dotY, edges == 0 ? 0.72F : 0.9F, edge);
			}
		}
	}

	private int getResizeEdges'''
if not handle_pattern.search(module):
    raise RuntimeError("Missing resize handle method")
module = handle_pattern.sub(new_handle, module, count=1)
module = module.replace('"Ã¢â‚¬Âº"', '">"')
module = module.replace('"â€º"', '">"')
module_path.write_text(module, encoding="utf-8")

print("Applied BedESP screen-space redesign, PiP FPS ceiling, and tutorial polish")
