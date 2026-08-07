/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Visual.BlockESP;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.ListSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class BlockESP_h
extends ModuleHeader {
    public EnumSetting<OutlineMode> outlineMode = new EnumSetting<OutlineMode>("Outline Mode", "ESP wireframe style", OutlineMode.Line12, "12line", "6line");
    public NumberSetting lineWidth = new NumberSetting("Line Width", "Thickness of outline lines", 1.5, 0.5, 5.0, 0.1);
    public NumberSetting outlineOpacity = new NumberSetting("Outline Alpha", "Opacity of the 3D box frame", 1.0, 0.0, 1.0, 0.05);
    public BoolSetting filled = new BoolSetting("Filled", "Draws filled faces inside the 3D box", true);
    public NumberSetting filledOpacity = new NumberSetting("Filled Alpha", "Opacity of the filled face polygon", 0.5, 0.0, 1.0, 0.05);
    public NumberSetting renderChunksXZ = new NumberSetting("Render Chunks XZ", "Horizontal ESP detection chunk radius", 4.0, 1.0, 32.0, 1.0);
    public NumberSetting renderChunksY = new NumberSetting("Render Chunks Y", "Vertical ESP detection chunk height", 4.0, 1.0, 32.0, 1.0);
    public NumberSetting layersPerTick = new NumberSetting("Layers Per Tick", "Vertical Y-slices scanned per game tick", 3.0, 1.0, 10.0, 1.0);
    public BoolSetting exposedOnly = new BoolSetting("Exposed Only", "Show only ores that are exposed to air", false);
    public EnumSetting<RenderRangeMode> renderRangeMode = new EnumSetting<RenderRangeMode>("Render Range Mode", "ESP render distance behavior", RenderRangeMode.Old, "Old", "Sync", "Custom");
    public NumberSetting renderRangeBlocksXZ = new NumberSetting("Render Range XZ (Blocks)", "Horizontal stand-alone ESP render distance", 35.0, 5.0, 256.0, 1.0);
    public NumberSetting renderRangeBlocksY = new NumberSetting("Render Range Y (Blocks)", "Vertical stand-alone ESP render distance", 35.0, 5.0, 256.0, 1.0);
    public EnumSetting<CSRenderMode> csRenderMode = new EnumSetting<CSRenderMode>("CSRender Mode", "Adaptive projection rendering updates", CSRenderMode.Normal, "Normal", "Custom", "Simple", "Simple12line");
    public NumberSetting csRenderRange1 = new NumberSetting("CSRender Range 1", "Simplification / Normal update range limit (Blocks)", 15.0, 1.0, 100.0, 1.0);
    public NumberSetting csRenderDelay2 = new NumberSetting("CSRender Delay 2", "Update delay for medium range (Ticks)", 2.0, 1.0, 40.0, 1.0);
    public NumberSetting csRenderRange2 = new NumberSetting("CSRender Range 2", "Medium range limit (Blocks)", 35.0, 1.0, 150.0, 1.0);
    public NumberSetting csRenderDelay3 = new NumberSetting("CSRender Delay 3", "Update delay for long range (Ticks)", 5.0, 1.0, 100.0, 1.0);
    public NumberSetting csRenderRange3 = new NumberSetting("CSRender Range 3", "Long range limit (Blocks)", 64.0, 1.0, 256.0, 1.0);
    public BoolSetting renderCurrentChunk = new BoolSetting("Render Current Chunk", "Draws an ESP plane on the chunk currently being scanned", false);
    public BoolSetting bed = new BoolSetting("Bed", "Automatically renders all beds with ESP", true);
    public ListSetting targetBlocks = new ListSetting("Target Blocks", "Blocks registered to be searched");
    public ListSetting blockColors = new ListSetting("Block Colors", "Color code mappings for each block");

    public BlockESP_h() {
        super("BlockESP", "Draws a box around selected blocks", Category.VISUAL, 0, false);
        this.filledOpacity.visibility = () -> this.filled.value;
        this.renderRangeBlocksXZ.visibility = () -> this.renderRangeMode.value == RenderRangeMode.Custom;
        this.renderRangeBlocksY.visibility = () -> this.renderRangeMode.value == RenderRangeMode.Custom;
        this.csRenderRange1.visibility = () -> this.csRenderMode.value == CSRenderMode.Custom || this.csRenderMode.value == CSRenderMode.Simple || this.csRenderMode.value == CSRenderMode.Simple12line;
        this.csRenderDelay2.visibility = () -> this.csRenderMode.value == CSRenderMode.Custom;
        this.csRenderRange2.visibility = () -> this.csRenderMode.value == CSRenderMode.Custom;
        this.csRenderDelay3.visibility = () -> this.csRenderMode.value == CSRenderMode.Custom;
        this.csRenderRange3.visibility = () -> this.csRenderMode.value == CSRenderMode.Custom;
        this.targetBlocks.value.add("chest");
        this.targetBlocks.value.add("end_stone");
        this.targetBlocks.value.add("redstone_ore");
        this.targetBlocks.value.add("gold_ore");
        this.targetBlocks.value.add("iron_ore");
        this.targetBlocks.value.add("ender_chest");
        this.targetBlocks.value.add("furnace");
        this.targetBlocks.value.add("blast_furnace");
        this.targetBlocks.value.add("lapis_ore");
        this.blockColors.value.add("chest:#808080");
        this.blockColors.value.add("end_stone:#808080");
        this.blockColors.value.add("redstone_ore:#808080");
        this.blockColors.value.add("gold_ore:#808080");
        this.blockColors.value.add("iron_ore:#808080");
        this.blockColors.value.add("ender_chest:#808080");
        this.blockColors.value.add("furnace:#808080");
        this.blockColors.value.add("blast_furnace:#808080");
        this.blockColors.value.add("lapis_ore:#808080");
        this.blockColors.value.add("white_bed:#FFFFFF");
        this.blockColors.value.add("orange_bed:#FF8000");
        this.blockColors.value.add("magenta_bed:#FF00FF");
        this.blockColors.value.add("light_blue_bed:#80C0FF");
        this.blockColors.value.add("yellow_bed:#FFFF00");
        this.blockColors.value.add("lime_bed:#80FF00");
        this.blockColors.value.add("pink_bed:#FFC0CB");
        this.blockColors.value.add("gray_bed:#404040");
        this.blockColors.value.add("light_gray_bed:#C0C0C0");
        this.blockColors.value.add("cyan_bed:#00FFFF");
        this.blockColors.value.add("purple_bed:#800080");
        this.blockColors.value.add("blue_bed:#0000FF");
        this.blockColors.value.add("brown_bed:#804000");
        this.blockColors.value.add("green_bed:#008000");
        this.blockColors.value.add("red_bed:#FF0000");
        this.blockColors.value.add("black_bed:#101010");
        this.addSettings(this.outlineMode, this.lineWidth, this.outlineOpacity, this.filled, this.filledOpacity, this.renderChunksXZ, this.renderChunksY, this.layersPerTick, this.exposedOnly, this.renderRangeMode, this.renderRangeBlocksXZ, this.renderRangeBlocksY, this.csRenderMode, this.csRenderRange1, this.csRenderDelay2, this.csRenderRange2, this.csRenderDelay3, this.csRenderRange3, this.renderCurrentChunk, this.bed, this.targetBlocks, this.blockColors);
    }

    public static enum OutlineMode {
        Line12,
        Line6;

    }

    public static enum RenderRangeMode {
        Old,
        Sync,
        Custom;

    }

    public static enum CSRenderMode {
        Normal,
        Custom,
        Simple,
        Simple12line;

    }
}

