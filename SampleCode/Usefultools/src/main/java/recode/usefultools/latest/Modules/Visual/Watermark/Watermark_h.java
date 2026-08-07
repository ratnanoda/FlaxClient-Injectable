/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Visual.Watermark;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class Watermark_h
extends ModuleHeader {
    public EnumSetting<Mode> mode = new EnumSetting<Mode>("Mode", "Watermark style", Mode.Usefultools, "Useful-tools");
    public EnumSetting<FontMode> fontMode = new EnumSetting<FontMode>("FontMode", "Font Mode", FontMode.InterfaceF, "Interface", "Mojangles", "Product Sans");
    public NumberSetting fontSize = new NumberSetting("font size", "font size", 45.0, 0.0, 100.0, 5.0);
    public BoolSetting bold = new BoolSetting("Bold", "Use bold font", true);
    public BoolSetting shadow = new BoolSetting("Shadow", "Draws a nice drop shadow", true);
    public BoolSetting glow = new BoolSetting("Glow", "Draws a glowing aura around text", false);

    public Watermark_h() {
        super("Watermark", "Displays the client name", Category.VISUAL, 0, true);
        this.addSettings(this.mode, this.fontMode, this.fontSize, this.bold, this.shadow, this.glow);
    }

    public static enum Mode {
        Usefultools;

    }

    public static enum FontMode {
        InterfaceF,
        Mojangles,
        ProductSans;

    }
}

