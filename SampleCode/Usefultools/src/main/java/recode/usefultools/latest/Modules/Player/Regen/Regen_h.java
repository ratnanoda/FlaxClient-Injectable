/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Player.Regen;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class Regen_h
extends ModuleHeader {
    public NumberSetting range = new NumberSetting("Range", "Block breaker range", 4.2, 1.0, 6.0, 0.1);
    public EnumSetting<Mode> mode = new EnumSetting<Mode>("Mode", "Mining protocol mode", Mode.Normal, "Normal", "Packet");
    public NumberSetting breakSpeed = new NumberSetting("Break Speed", "Mining speed multiplier", 1.0, 0.0, 10.0, 0.1);
    public BoolSetting rotate = new BoolSetting("Rotate", "Rotates towards the block", true);
    public BoolSetting esp = new BoolSetting("ESP", "Draw 3D Box around target", true);
    public EnumSetting<ColorMode> colorMode = new EnumSetting<ColorMode>("Color Mode", "Visual color profile", ColorMode.Theme, "Theme", "Custom");
    public BoolSetting easing = new BoolSetting("Easing", "Smoothly slides the ESP box", true);
    public NumberSetting easingSpeed = new NumberSetting("Easing Speed", "Speed of the ESP box slide", 10.0, 0.1, 20.0, 1.0);
    public EnumSetting<SwingMode> swingMode = new EnumSetting<SwingMode>("Swing Mode", "Hand swing protocol", SwingMode.Normal, "Normal", "Silent", "Old", "OldPacket", "NoSwing");
    public NumberSetting rotationPercentage = new NumberSetting("Rotation %", "Mining progress before rotate", 0.5, 0.0, 1.0, 0.01);
    public EnumSetting<OnGroundMode> onGroundMode = new EnumSetting<OnGroundMode>("OnGround Mode", "Air break behavior", OnGroundMode.Normal, "Normal", "MiningStop", "Cancel");
    public BoolSetting absorption = new BoolSetting("Absorption Mode", "Mines redstone ore until 5 absorption hearts", true);
    public EnumSetting<SwitchMode> switchMode = new EnumSetting<SwitchMode>("Switch Mode", "Auto tool switch mode", SwitchMode.None, "None", "Full", "Fake");
    public EnumSetting<ProgressBarMode> barMode = new EnumSetting<ProgressBarMode>("Bar Mode", "Progress bar style", ProgressBarMode.Old, "Old", "Solstice", "Astra");
    public EnumSetting<FontMode> fontMode = new EnumSetting<FontMode>("FontMode", "Font Mode", FontMode.InterfaceF, "Interface", "Mojangles", "Product Sans");
    public BoolSetting bold = new BoolSetting("Bold", "Use bold font for text", true);
    public BoolSetting shadow = new BoolSetting("Shadow", "Draws a nice drop shadow", true);
    public BoolSetting solsticeAnimation = new BoolSetting("Solstice Animation", "Enables smooth reset/re-aim transition animations", true);
    public BoolSetting progressLerp = new BoolSetting("Progress Lerp", "Smoothly interpolates mining progress", true);
    public NumberSetting progressLerpSpeed = new NumberSetting("Progress Lerp Speed", "Lerp interpolation speed", 30.0, 1.0, 50.0, 1.0);
    public NumberSetting swingDelay = new NumberSetting("Swing Delay", "Ticks interval between swings", 5.0, 1.0, 20.0, 1.0);

    public Regen_h() {
        super("Regen", "Automatically regenerates absorption hearts using redstone", Category.PLAYER, 0, false);
        this.colorMode.visibility = () -> this.esp.value;
        this.easing.visibility = () -> this.esp.value;
        this.easingSpeed.visibility = () -> this.esp.value && this.easing.value;
        this.rotationPercentage.visibility = () -> this.rotate.value;
        this.fontMode.visibility = () -> this.barMode.value == ProgressBarMode.Solstice || this.barMode.value == ProgressBarMode.Astra;
        this.bold.visibility = () -> this.barMode.value == ProgressBarMode.Solstice || this.barMode.value == ProgressBarMode.Astra;
        this.shadow.visibility = () -> this.barMode.value == ProgressBarMode.Solstice || this.barMode.value == ProgressBarMode.Astra;
        this.solsticeAnimation.visibility = () -> this.barMode.value == ProgressBarMode.Solstice || this.barMode.value == ProgressBarMode.Astra;
        this.progressLerp.visibility = () -> this.barMode.value == ProgressBarMode.Solstice || this.barMode.value == ProgressBarMode.Astra;
        this.progressLerpSpeed.visibility = () -> this.progressLerp.value && (this.barMode.value == ProgressBarMode.Solstice || this.barMode.value == ProgressBarMode.Astra);
        this.swingDelay.visibility = () -> this.swingMode.value == SwingMode.Normal || this.swingMode.value == SwingMode.Silent;
        this.addSettings(this.range, this.mode, this.breakSpeed, this.rotate, this.esp, this.colorMode, this.easing, this.easingSpeed, this.swingMode, this.rotationPercentage, this.onGroundMode, this.absorption, this.switchMode, this.barMode, this.fontMode, this.bold, this.shadow, this.solsticeAnimation, this.progressLerp, this.progressLerpSpeed, this.swingDelay);
    }

    public static enum Mode {
        Normal,
        Packet;

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

    public static enum SwitchMode {
        None,
        Full,
        Fake;

    }

    public static enum ProgressBarMode {
        Old,
        Solstice,
        Astra;

    }

    public static enum FontMode {
        InterfaceF,
        Mojangles,
        ProductSans;

    }
}

