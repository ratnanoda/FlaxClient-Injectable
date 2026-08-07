/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Player.Fucker;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.ListSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class Fucker_h
extends ModuleHeader {
    public NumberSetting range = new NumberSetting("Range", "Block breaker range", 4.2, 1.0, 6.0, 0.1);
    public EnumSetting<Mode> mode = new EnumSetting<Mode>("Mode", "Mining protocol mode", Mode.Normal, "Normal", "Packet");
    public NumberSetting breakSpeed = new NumberSetting("Break Speed", "Mining speed multiplier", 1.0, 0.0, 10.0, 0.1);
    public BoolSetting rotate = new BoolSetting("Rotate", "Rotates towards the block", true);
    public EnumSetting<MoveFix> moveFix = new EnumSetting<MoveFix>("Move Fix", "Silent movement angle correction", MoveFix.None, "None", "Simple", "Silent", "Test");
    public BoolSetting esp = new BoolSetting("ESP", "Draw 3D Box around target", true);
    public EnumSetting<ColorMode> colorMode = new EnumSetting<ColorMode>("Color Mode", "Visual color profile", ColorMode.Theme, "Theme", "Custom");
    public BoolSetting easing = new BoolSetting("Easing", "Smoothly slides the ESP box", true);
    public NumberSetting easingSpeed = new NumberSetting("Easing Speed", "Speed of the ESP box slide", 10.0, 0.1, 20.0, 1.0);
    public EnumSetting<SwingMode> swingMode = new EnumSetting<SwingMode>("Swing Mode", "Hand swing protocol", SwingMode.Normal, "Normal", "Silent", "Old", "OldPacket", "NoSwing");
    public NumberSetting rotationPercentage = new NumberSetting("Rotation %", "Mining progress before rotate", 0.5, 0.0, 1.0, 0.01);
    public EnumSetting<OnGroundMode> onGroundMode = new EnumSetting<OnGroundMode>("OnGround Mode", "Air break behavior", OnGroundMode.Normal, "Normal", "MiningStop", "Cancel");
    public BoolSetting bed = new BoolSetting("Bed", "Automatically mines all colored beds", true);
    public EnumSetting<ExposedMode> exposedMode = new EnumSetting<ExposedMode>("Exposed Mode", "Mining exposure security protocol", ExposedMode.None, "None", "Legit", "Surround");
    public ListSetting targetBlocks = new ListSetting("Target Blocks", "Target block names to break");
    public EnumSetting<ProgressBarMode> barMode = new EnumSetting<ProgressBarMode>("Bar Mode", "Progress bar style", ProgressBarMode.Old, "Old", "New");
    public EnumSetting<FontMode> fontMode = new EnumSetting<FontMode>("FontMode", "Font Mode", FontMode.InterfaceF, "Interface", "Mojangles", "Product Sans");
    public BoolSetting bold = new BoolSetting("Bold", "Use bold font for text", true);
    public BoolSetting shadow = new BoolSetting("Shadow", "Draws a nice drop shadow", true);

    public Fucker_h() {
        super("Fucker", "Breaks specified blocks around you", Category.PLAYER, 0, false);
        this.moveFix.visibility = () -> this.rotate.value;
        this.colorMode.visibility = () -> this.esp.value;
        this.easing.visibility = () -> this.esp.value;
        this.easingSpeed.visibility = () -> this.esp.value && this.easing.value;
        this.rotationPercentage.visibility = () -> this.rotate.value;
        this.fontMode.visibility = () -> this.barMode.value == ProgressBarMode.New;
        this.bold.visibility = () -> this.barMode.value == ProgressBarMode.New;
        this.shadow.visibility = () -> this.barMode.value == ProgressBarMode.New;
        this.targetBlocks.value.add("white_bed");
        this.targetBlocks.value.add("orange_bed");
        this.targetBlocks.value.add("magenta_bed");
        this.targetBlocks.value.add("light_blue_bed");
        this.targetBlocks.value.add("yellow_bed");
        this.targetBlocks.value.add("lime_bed");
        this.targetBlocks.value.add("pink_bed");
        this.targetBlocks.value.add("gray_bed");
        this.targetBlocks.value.add("light_gray_bed");
        this.targetBlocks.value.add("cyan_bed");
        this.targetBlocks.value.add("purple_bed");
        this.targetBlocks.value.add("blue_bed");
        this.targetBlocks.value.add("brown_bed");
        this.targetBlocks.value.add("green_bed");
        this.targetBlocks.value.add("red_bed");
        this.targetBlocks.value.add("black_bed");
        this.addSettings(this.range, this.mode, this.breakSpeed, this.rotate, this.moveFix, this.esp, this.colorMode, this.easing, this.easingSpeed, this.swingMode, this.rotationPercentage, this.onGroundMode, this.bed, this.exposedMode, this.barMode, this.fontMode, this.bold, this.shadow, this.targetBlocks);
    }

    public static enum Mode {
        Normal,
        Packet;

    }

    public static enum MoveFix {
        None,
        Simple,
        Silent,
        Test;

    }

    public static enum ColorMode {
        Theme,
        Custom;

    }

    public static enum SwingMode {
        Normal,
        Silent,
        Old,
        OldPacket,
        NoSwing;

    }

    public static enum OnGroundMode {
        Normal,
        MiningStop,
        Cancel;

    }

    public static enum ExposedMode {
        None,
        Legit,
        Surround;

    }

    public static enum ProgressBarMode {
        Old,
        New;

    }

    public static enum FontMode {
        InterfaceF,
        Mojangles,
        ProductSans;

    }
}

