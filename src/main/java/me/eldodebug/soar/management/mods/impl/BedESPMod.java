package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.event.impl.EventRender3D;
import me.eldodebug.soar.management.event.impl.EventUpdate;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.BooleanSetting;
import me.eldodebug.soar.management.mods.settings.impl.ColorSetting;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.NumberSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import me.eldodebug.soar.management.nanovg.NanoVGManager;
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.Render3DUtils;
import me.eldodebug.soar.utils.animation.simple.SimpleAnimation;
import me.eldodebug.soar.utils.render.WorldToScreen;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class BedESPMod extends Mod {

	// Beds are scanned periodically rather than every frame, and the result is
	// cached for rendering. The scan walks loaded chunks and skips empty chunk
	// sections, so cost scales with actual terrain rather than the full cuboid.
	private static final int MAX_SCAN_CHUNKS = 8;
	private static final int VERTICAL_RANGE = 64;
	private static final int SCAN_INTERVAL_TICKS = 20;

	private static final float ICON_SIZE = 16.0F;
	private static final float ICON_GAP = 2.0F;
	private static final float PANEL_PADDING = 4.0F;

	private final ColorSetting colorSetting = new ColorSetting(TranslateText.COLOR, this, new Color(255, 64, 64), false);
	private final ColorSetting backgroundColorSetting = new ColorSetting(TranslateText.BACKGROUND, this, new Color(45, 48, 58, 205), true);
	private final NumberSetting alphaSetting = new NumberSetting(TranslateText.ALPHA, this, 0.85, 0.05, 1.0, false);
	private final NumberSetting lineWidthSetting = new NumberSetting(TranslateText.LINE_WIDTH, this, 2, 1, 5, true);
	private final ComboSetting modeSetting = new ComboSetting(TranslateText.MODE, this, TranslateText.BOX, new ArrayList<Option>(Arrays.asList(
			new Option(TranslateText.OUTLINE), new Option(TranslateText.BOX), new Option(TranslateText.GLOW))));
	private final BooleanSetting showBedColorSetting = new BooleanSetting(TranslateText.SHOW_BED_COLOR, this, false);
	private final BooleanSetting checkDefBlockSetting = new BooleanSetting(TranslateText.CHECK_DEF_BLOCK, this, false);

	private final List<Bed> beds = new ArrayList<Bed>();

	// Per-bed appear/scale animations, keyed by foot position so they persist
	// across scans (grow in on appear, fade out when the bed leaves range).
	private final Map<BlockPos, SimpleAnimation> animations = new HashMap<BlockPos, SimpleAnimation>();

	private int scanTimer;

	public BedESPMod() {
		super(TranslateText.BED_ESP, TranslateText.BED_ESP_DESCRIPTION, ModCategory.GHOST);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		beds.clear();
		animations.clear();
		scanTimer = 0;
	}

	@Override
	public void onDisable() {
		super.onDisable();
		beds.clear();
		animations.clear();
	}

	@EventTarget
	public void onUpdate(EventUpdate event) {
		if(mc.theWorld == null || mc.thePlayer == null) {
			beds.clear();
			return;
		}

		if(scanTimer > 0) {
			scanTimer--;
			return;
		}

		scanTimer = SCAN_INTERVAL_TICKS;
		scanBeds();
	}

	private void scanBeds() {

		EntityPlayer player = mc.thePlayer;
		int viewChunks = Math.min(mc.gameSettings.renderDistanceChunks, MAX_SCAN_CHUNKS);

		int px = MathHelper.floor_double(player.posX);
		int py = MathHelper.floor_double(player.posY);
		int pz = MathHelper.floor_double(player.posZ);

		int pcx = px >> 4;
		int pcz = pz >> 4;

		int minSection = Math.max(0, (py - VERTICAL_RANGE) >> 4);
		int maxSection = Math.min(15, (py + VERTICAL_RANGE) >> 4);

		// Keyed by the bed's foot position so the head and foot halves of the
		// same bed collapse into a single highlight.
		Map<BlockPos, Bed> found = new LinkedHashMap<BlockPos, Bed>();

		for(int cx = pcx - viewChunks; cx <= pcx + viewChunks; cx++) {
			for(int cz = pcz - viewChunks; cz <= pcz + viewChunks; cz++) {

				Chunk chunk = mc.theWorld.getChunkFromChunkCoords(cx, cz);

				if(chunk.isEmpty()) {
					continue;
				}

				ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
				int baseX = cx << 4;
				int baseZ = cz << 4;

				for(int s = minSection; s <= maxSection && s < sections.length; s++) {

					ExtendedBlockStorage section = sections[s];

					if(section == null || section.isEmpty()) {
						continue;
					}

					int baseY = s << 4;

					for(int ly = 0; ly < 16; ly++) {
						for(int lx = 0; lx < 16; lx++) {
							for(int lz = 0; lz < 16; lz++) {

								IBlockState state = section.get(lx, ly, lz);

								if(state.getBlock() != Blocks.bed) {
									continue;
								}

								int wx = baseX + lx;
								int wy = baseY + ly;
								int wz = baseZ + lz;

								EnumFacing facing = (EnumFacing) state.getValue(BlockDirectional.FACING);
								boolean head = state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD;

								// FACING points from the foot towards the head, so
								// derive both halves no matter which one we hit first.
								BlockPos here = new BlockPos(wx, wy, wz);
								BlockPos footPos = head ? here.offset(facing.getOpposite()) : here;
								BlockPos headPos = head ? here : here.offset(facing);

								if(found.containsKey(footPos)) {
									continue;
								}

								found.put(footPos, new Bed(footPos, headPos, computeDefenseBlocks(footPos, headPos)));
							}
						}
					}
				}
			}
		}

		beds.clear();
		beds.addAll(found.values());

		// Drop animations for beds that are no longer in range.
		animations.keySet().retainAll(found.keySet());
	}

	// Collects the unique block item icons surrounding the bed - the blocks
	// adjacent to either half (sides and above), excluding the bed itself and
	// the block underneath.
	private List<ItemStack> computeDefenseBlocks(BlockPos footPos, BlockPos headPos) {

		List<ItemStack> result = new ArrayList<ItemStack>();
		BlockPos[] cells = { footPos, headPos };

		for(BlockPos cell : cells) {
			for(EnumFacing facing : EnumFacing.values()) {

				if(facing == EnumFacing.DOWN) {
					continue;
				}

				BlockPos neighbor = cell.offset(facing);

				if(neighbor.equals(footPos) || neighbor.equals(headPos)) {
					continue;
				}

				IBlockState state = mc.theWorld.getBlockState(neighbor);
				Block block = state.getBlock();

				if(block == Blocks.air || block == Blocks.bed) {
					continue;
				}

				Item item = Item.getItemFromBlock(block);
				if(item == null) {
					continue;
				}

				int meta = block.damageDropped(state);

				boolean duplicate = false;
				for(ItemStack existing : result) {
					if(existing.getItem() == item && existing.getMetadata() == meta) {
						duplicate = true;
						break;
					}
				}

				if(!duplicate) {
					result.add(new ItemStack(item, 1, meta));
				}
			}
		}

		return result;
	}

	@EventTarget
	public void onRender3D(EventRender3D event) {

		if(mc.theWorld == null || mc.thePlayer == null) {
			return;
		}

		// Capture the camera matrices so the 2D panels can be projected later.
		WorldToScreen.capture();

		if(beds.isEmpty()) {
			return;
		}

		RenderManager rm = mc.getRenderManager();
		double viewX = rm.viewerPosX;
		double viewY = rm.viewerPosY;
		double viewZ = rm.viewerPosZ;

		Color color = colorSetting.getColor();
		float alpha = alphaSetting.getValueFloat();
		int alphaInt = (int) (alpha * 255);

		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		GL11.glLineWidth(lineWidthSetting.getValueFloat());

		boolean outlineMode = modeSetting.getOption().getTranslate().equals(TranslateText.OUTLINE);
		boolean glowMode = modeSetting.getOption().getTranslate().equals(TranslateText.GLOW);

		for(Bed bed : beds) {

			AxisAlignedBB box = new AxisAlignedBB(
					bed.box.minX - viewX, bed.box.minY - viewY, bed.box.minZ - viewZ,
					bed.box.maxX - viewX, bed.box.maxY - viewY, bed.box.maxZ - viewZ);

			if(glowMode) {
				for(int i = 1; i <= 3; i++) {
					double grow = 0.04D * i;
					ColorUtils.setColor(color.getRGB(), alpha * 0.12F);
					Render3DUtils.drawFillBox(box.expand(grow, grow, grow));
				}
			} else if(!outlineMode) {
				ColorUtils.setColor(color.getRGB(), alpha * 0.22F);
				Render3DUtils.drawFillBox(box);
			}

			RenderGlobal.drawOutlinedBoundingBox(box, color.getRed(), color.getGreen(), color.getBlue(), alphaInt);
		}

		GlStateManager.depthMask(true);
		GlStateManager.enableDepth();
		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		ColorUtils.resetColor();
		GlStateManager.popMatrix();
	}

	@EventTarget
	public void onRender2D(EventRender2D event) {

		if(mc.theWorld == null || mc.thePlayer == null || beds.isEmpty()) {
			return;
		}

		if(!showBedColorSetting.isToggled() && !checkDefBlockSetting.isToggled()) {
			return;
		}

		List<Panel> panels = new ArrayList<Panel>();

		for(Bed bed : beds) {

			List<ItemStack> icons = new ArrayList<ItemStack>();
			if(showBedColorSetting.isToggled()) {
				icons.add(bed.bedStack);
			}
			if(checkDefBlockSetting.isToggled()) {
				icons.addAll(bed.defenseStacks);
			}

			SimpleAnimation animation = animations.get(bed.footPos);
			if(animation == null) {
				animation = new SimpleAnimation();
				animations.put(bed.footPos, animation);
			}

			double centerX = (bed.box.minX + bed.box.maxX) / 2.0D;
			double centerZ = (bed.box.minZ + bed.box.maxZ) / 2.0D;
			float[] screen = icons.isEmpty() ? null : WorldToScreen.project(centerX, bed.box.maxY + 0.55D, centerZ);

			animation.setAnimation(screen != null ? 1.0F : 0.0F, 14);
			float appear = animation.getValue();

			if(screen == null || appear <= 0.02F) {
				continue;
			}

			panels.add(new Panel(screen[0], screen[1], appear, icons));
		}

		if(panels.isEmpty()) {
			return;
		}

		NanoVGManager nvg = Glide.getInstance().getNanoVGManager();
		nvg.setupAndDraw(() -> {
			for(Panel panel : panels) {
				drawPanelBackground(nvg, panel);
			}
		});

		for(Panel panel : panels) {
			drawPanelItems(panel);
		}
	}

	private void drawPanelBackground(NanoVGManager nvg, Panel panel) {

		int count = panel.icons.size();
		float contentWidth = count * ICON_SIZE + (count - 1) * ICON_GAP;
		float panelWidth = contentWidth + PANEL_PADDING * 2.0F;
		float panelHeight = ICON_SIZE + PANEL_PADDING * 2.0F;

		float scale = 0.65F + 0.35F * panel.appear;
		float slide = (1.0F - panel.appear) * 5.0F;

		float w = panelWidth * scale;
		float h = panelHeight * scale;
		float cx = panel.x;
		float cy = panel.y - (h / 2.0F) - 6.0F + slide;

		float x = cx - w / 2.0F;
		float y = cy - h / 2.0F;
		float radius = h * 0.32F;

		Color background = backgroundColorSetting.getColor();
		int shadowAlpha = (int) (panel.appear * 90);
		int baseAlpha = (int) (panel.appear * background.getAlpha());
		int glossAlpha = (int) (panel.appear * 14);

		nvg.drawRoundedRect(x - 1.5F, y - 1.5F, w + 3.0F, h + 3.0F, radius + 1.5F, new Color(0, 0, 0, shadowAlpha));
		nvg.drawRoundedRect(x, y, w, h, radius, new Color(background.getRed(), background.getGreen(), background.getBlue(), baseAlpha));
		nvg.drawRoundedRect(x, y, w, h / 2.0F, radius, new Color(255, 255, 255, glossAlpha));
	}

	private void drawPanelItems(Panel panel) {

		int count = panel.icons.size();
		float contentWidth = count * ICON_SIZE + (count - 1) * ICON_GAP;
		float panelHeight = ICON_SIZE + PANEL_PADDING * 2.0F;

		float scale = 0.65F + 0.35F * panel.appear;
		float slide = (1.0F - panel.appear) * 5.0F;

		float h = panelHeight * scale;
		float cx = panel.x;
		float cy = panel.y - (h / 2.0F) - 6.0F + slide;

		float left = cx - contentWidth / 2.0F;
		float top = cy - ICON_SIZE / 2.0F;

		GlStateManager.pushMatrix();
		GlStateManager.translate(cx, cy, 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		GlStateManager.translate(-cx, -cy, 0.0F);

		RenderHelper.enableGUIStandardItemLighting();
		GlStateManager.enableColorMaterial();
		GlStateManager.enableDepth();

		for(int i = 0; i < count; i++) {
			float iconX = left + i * (ICON_SIZE + ICON_GAP);
			mc.getRenderItem().renderItemIntoGUI(panel.icons.get(i), Math.round(iconX), Math.round(top));
		}

		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableLighting();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}

	private static class Panel {

		private final float x;
		private final float y;
		private final float appear;
		private final List<ItemStack> icons;

		private Panel(float x, float y, float appear, List<ItemStack> icons) {
			this.x = x;
			this.y = y;
			this.appear = appear;
			this.icons = icons;
		}
	}

	private static class Bed {

		private final BlockPos footPos;
		private final AxisAlignedBB box;
		private final ItemStack bedStack;
		private final List<ItemStack> defenseStacks;

		private Bed(BlockPos footPos, BlockPos headPos, List<ItemStack> defenseStacks) {

			int minX = Math.min(footPos.getX(), headPos.getX());
			int minZ = Math.min(footPos.getZ(), headPos.getZ());
			int maxX = Math.max(footPos.getX(), headPos.getX());
			int maxZ = Math.max(footPos.getZ(), headPos.getZ());

			this.footPos = footPos;
			this.box = new AxisAlignedBB(minX, footPos.getY(), minZ, maxX + 1, footPos.getY() + 0.5625D, maxZ + 1);
			this.bedStack = new ItemStack(Items.bed);
			this.defenseStacks = defenseStacks;
		}
	}
}
