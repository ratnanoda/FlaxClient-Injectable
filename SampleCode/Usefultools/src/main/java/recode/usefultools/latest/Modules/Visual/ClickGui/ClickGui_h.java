/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Visual.ClickGui;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class ClickGui_h
extends ModuleHeader {
    public EnumSetting<Mode> mode = new EnumSetting<Mode>("Mode", "UI Style", Mode.Solstice, "Useful-V3", "Solstice");
    public EnumSetting<Background> bgType = new EnumSetting<Background>("Background", "Background Style", Background.Old, "Old", "New");
    public NumberSetting fontSize = new NumberSetting("Font Size", "Size of the UI text", 18.0, 10.0, 40.0, 1.0);
    public NumberSetting inAnimSpeed = new NumberSetting("In Speed", "Opening speed", 0.12, 0.01, 0.5, 0.01);
    public NumberSetting outAnimSpeed = new NumberSetting("Out Speed", "Closing speed", 0.18, 0.01, 0.5, 0.01);
    public NumberSetting blurStrength = new NumberSetting("Blur Strength", "Blur intensity", 7.0, 1.0, 20.0, 0.5);
    public NumberSetting rounding = new NumberSetting("Rounding", "Corner radius", 12.0, 0.0, 25.0, 0.5);
    public NumberSetting maxBgHeight = new NumberSetting("Max BG Height", "Height for expanded boxes", 300.0, 50.0, 1000.0, 10.0);
    public NumberSetting xOffset = new NumberSetting("X Offset", "Global X offset", 0.0, -500.0, 500.0, 1.0);
    public NumberSetting yOffset = new NumberSetting("Y Offset", "Global Y offset", 0.0, -500.0, 500.0, 1.0);
    public BoolSetting guiOnMove = new BoolSetting("GUI on Move", "Allow movement while menu is open", true);

    public ClickGui_h() {
        super("ClickGui", "Main GUI manager", Category.VISUAL, 344, false);
        this.blurStrength.visibility = () -> this.bgType.value == Background.New;
        this.addSettings(this.mode, this.bgType, this.fontSize, this.inAnimSpeed, this.outAnimSpeed, this.rounding, this.maxBgHeight, this.xOffset, this.yOffset, this.guiOnMove);
    }

    public static enum Mode {
        Useful_V3,
        Solstice;

    }

    public static enum Background {
        Old,
        New;

    }
}

