package me.eldodebug.soar.management.mods.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import me.eldodebug.soar.management.event.EventTarget;
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
import me.eldodebug.soar.utils.ColorUtils;
import me.eldodebug.soar.utils.Render3DUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.Gui;
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

	private static final int MAX_SCAN_CHUNKS = 8;
	private static final int VERTICAL_RANGE = 64;
	private static final int SCAN_INTERVAL_TICKS = 20;
	private static final int MAX_DEFENSE_ICONS = 8;

	private static final int ICON_SIZE = 16;
	private static final int ICON_GAP = 2;
	private static final int PANEL_PADDING = 4;

	private final ColorSetting colorSetting = new ColorSetting(TranslateText.COLOR, this, new Color(255, 64, 64), false);
	private final ColorSetting backgroundColorSetting = new ColorSetting(TranslateText.BACKGROUND, this, new Color(45, 48, 58, 205), true);
	private final NumberSetting alphaSetting = new NumberSetting(TranslateText.ALPHA, this, 0.85, 0.05, 1.0, false);
	private final NumberSetting lineWidthSetting = new NumberSetting(TranslateText.LINE_WIDTH, this, 2, 1, 5, true);
	private final ComboSetting modeSetting = new ComboSetting(TranslateText.MODE, this, TranslateText.BOX, new ArrayList<Option>(Arrays.asList(
			new Option(TranslateText.OUTLINE), new Option(TranslateText.BOX), new Option(TranslateText.GLOW))));
	private final BooleanSetting showBedColorSetting = new BooleanSetting(TranslateText.SHOW_BED_COLOR, this, false);
	private final BooleanSetting checkDefBlockSetting = new BooleanSetting(TranslateText.CHECK_DEF_BLOCK, this, false);

	private final List<Bed> beds = new ArrayList<Bed>();
	private int scanTimer;

	public BedESPMod() {
		super(TranslateText.BED_ESP, TranslateText.BED_ESP_DESCRIPTION, ModCategory.GHOST);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		beds.clear();
		scanTimer = 0;
	}

	@Override
	public void onDisable() {
		super.onDisable();
		beds.clear();
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

		Map<BlockPos, Bed> found = new LinkedHashMap<BlockPos, Bed>();

		for(int cx = pcx - viewChunks; cx <= pcx + viewChunks; cx++) {
			for(int cz = pcz - viewChunks; cz <= pcz + viewChunks; cz++) {
				Chunk chunk = mc.theWorld.getChunkFromChunkCoords(cx, cz);
				if(chunk.isEmpty()) continue;

				ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
				int baseX = cx << 4;
				int baseZ = cz << 4;

				for(int s = minSection; s <= maxSection && s < sections.length; s++) {
					ExtendedBlockStorage section = sections[s];
					if(section == null || section.isEmpty()) continue;

					int baseY = s << 4;
					for(int ly = 0; ly < 16; ly++) {
						for(int lx = 0; lx < 16; lx++) {
							for(int lz = 0; lz < 16; lz++) {
								IBlockState state = section.get(lx, ly, lz);
								if(state.getBlock() != Blocks.bed) continue;

								int wx = baseX + lx;
								int wy = baseY + ly;
								int wz = baseZ + lz;
								EnumFacing facing = (EnumFacing) state.getValue(BlockDirectional.FACING);
								boolean head = state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD;

								BlockPos here = new BlockPos(wx, wy, wz);
								BlockPos footPos = head ? here.offset(facing.getOpposite()) : here;
								BlockPos headPos = head ? here : here.offset(facing);
								if(found.containsKey(footPos)) continue;

								found.put(footPos, new Bed(footPos, headPos, computeDefenseBlocks(footPos, headPos)));
							}
						}
					}
				}
			}
		}

		beds.clear();
		beds.addAll(found.values());
	}

	private List<ItemStack> computeDefenseBlocks(BlockPos footPos, BlockPos headPos) {
		Map<String, DefenseIcon> nearestByItem = new LinkedHashMap<String, DefenseIcon>();
		int minX = Math.min(footPos.getX(), headPos.getX()) - 2;
		int maxX = Math.max(footPos.getX(), headPos.getX()) + 2;
		int minZ = Math.min(footPos.getZ(), headPos.getZ()) - 2;
		int maxZ = Math.max(footPos.getZ(), headPos.getZ()) + 2;
		int minY = Math.min(footPos.getY(), headPos.getY());
		int maxY = Math.max(footPos.getY(), headPos.getY()) + 2;

		for(int y = minY; y <= maxY; y++) {
			for(int x = minX; x <= maxX; x++) {
				for(int z = minZ; z <= maxZ; z++) {
					BlockPos position = new BlockPos(x, y, z);
					if(position.equals(footPos) || position.equals(headPos)) continue;

					int distance = Math.min(shellDistance(position, footPos), shellDistance(position, headPos));
					if(distance <= 0 || distance > 2) continue;

					IBlockState state = mc.theWorld.getBlockState(position);
					Block block = state.getBlock();
					if(block == Blocks.air || block == Blocks.bed) continue;

					Item item = Item.getItemFromBlock(block);
					if(item == null) continue;

					int meta;
					try {
						meta = block.damageDropped(state);
					} catch(Exception ignored) {
						meta = block.getMetaFromState(state);
					}

					String key = Item.getIdFromItem(item) + ":" + meta;
					DefenseIcon existing = nearestByItem.get(key);
					if(existing == null || distance < existing.distance) {
						nearestByItem.put(key, new DefenseIcon(new ItemStack(item, 1, meta), distance));
					}
				}
			}
		}

		List<DefenseIcon> sorted = new ArrayList<DefenseIcon>(nearestByItem.values());
		Collections.sort(sorted, (first, second) -> {
			int distanceCompare = Integer.compare(first.distance, second.distance);
			if(distanceCompare != 0) return distanceCompare;
			int itemCompare = Integer.compare(Item.getIdFromItem(first.stack.getItem()), Item.getIdFromItem(second.stack.getItem()));
			if(itemCompare != 0) return itemCompare;
			return Integer.compare(first.stack.getMetadata(), second.stack.getMetadata());
		});

		List<ItemStack> result = new ArrayList<ItemStack>();
		for(DefenseIcon icon : sorted) {
			result.add(icon.stack);
			if(result.size() >= MAX_DEFENSE_ICONS) break;
		}
		return result;
	}

	private int shellDistance(BlockPos first, BlockPos second) {
		return Math.max(Math.max(Math.abs(first.getX() - second.getX()), Math.abs(first.getY() - second.getY())),
				Math.abs(first.getZ() - second.getZ()));
	}

	@EventTarget
	public void onRender3D(EventRender3D event) {
		if(mc.theWorld == null || mc.thePlayer == null || beds.isEmpty()) return;

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

		if(showBedColorSetting.isToggled() || checkDefBlockSetting.isToggled()) {
			for(Bed bed : beds) {
				List<ItemStack> icons = buildDisplayIcons(bed);
				if(!icons.isEmpty()) renderIconsAboveBed(bed, icons, rm);
			}
		}
	}

	private List<ItemStack> buildDisplayIcons(Bed bed) {
		List<ItemStack> icons = new ArrayList<ItemStack>();
		if(showBedColorSetting.isToggled()) icons.add(bed.bedStack);
		if(checkDefBlockSetting.isToggled()) icons.addAll(bed.defenseStacks);
		return icons;
	}

	private void renderIconsAboveBed(Bed bed, List<ItemStack> icons, RenderManager rm) {
		double centerX = (bed.box.minX + bed.box.maxX) / 2.0D;
		double centerY = bed.box.maxY + 1.05D;
		double centerZ = (bed.box.minZ + bed.box.maxZ) / 2.0D;
		double distance = mc.thePlayer.getDistance(centerX, centerY, centerZ);
		float scale = (float) Math.max(0.025D, Math.min(0.12D, distance * 0.0025D));

		int contentWidth = icons.size() * ICON_SIZE + (icons.size() - 1) * ICON_GAP;
		int panelWidth = contentWidth + PANEL_PADDING * 2;
		int panelHeight = ICON_SIZE + PANEL_PADDING * 2;
		int left = -contentWidth / 2;
		int top = -ICON_SIZE / 2;

		GlStateManager.pushMatrix();
		GlStateManager.translate(centerX - rm.viewerPosX, centerY - rm.viewerPosY, centerZ - rm.viewerPosZ);
		GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
		GlStateManager.scale(-scale, -scale, scale);

		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.enableAlpha();
		GlStateManager.enableTexture2D();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		Color background = backgroundColorSetting.getColor();
		int backgroundArgb = (background.getAlpha() & 255) << 24
				| (background.getRed() & 255) << 16
				| (background.getGreen() & 255) << 8
				| background.getBlue() & 255;
		Gui.drawRect(-panelWidth / 2 - 1, -panelHeight / 2 - 1, panelWidth / 2 + 1, panelHeight / 2 + 1, 0x66000000);
		Gui.drawRect(-panelWidth / 2, -panelHeight / 2, panelWidth / 2, panelHeight / 2, backgroundArgb);

		GlStateManager.enableRescaleNormal();
		GlStateManager.enableColorMaterial();
		RenderHelper.enableGUIStandardItemLighting();

		float oldZLevel = mc.getRenderItem().zLevel;
		// RenderItem adds 100 to zLevel for GUI rendering. Cancel that translation
		// because this GUI is already positioned in the 3D world.
		mc.getRenderItem().zLevel = -100.0F;
		for(int i = 0; i < icons.size(); i++) {
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			mc.getRenderItem().renderItemAndEffectIntoGUI(icons.get(i), left + i * (ICON_SIZE + ICON_GAP), top);
		}
		mc.getRenderItem().zLevel = oldZLevel;

		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableColorMaterial();
		GlStateManager.enableDepth();
		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}

	private static class DefenseIcon {
		private final ItemStack stack;
		private final int distance;

		private DefenseIcon(ItemStack stack, int distance) {
			this.stack = stack;
			this.distance = distance;
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
