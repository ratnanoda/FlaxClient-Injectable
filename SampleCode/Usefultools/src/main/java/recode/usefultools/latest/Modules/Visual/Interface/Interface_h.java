/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Visual.Interface;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.ColorSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class Interface_h
extends ModuleHeader {
    public EnumSetting<NamingStyle> namingStyle = new EnumSetting<NamingStyle>("Naming", "Style of module names", NamingStyle.Normal, "lowercase", "lower spaced", "Normal", "Spaced");
    public EnumSetting<ColorTheme> theme = new EnumSetting<ColorTheme>("Theme", "UI Color Theme", ColorTheme.Trans, "Trans", "Rainbow", "Bubblegum", "Watermelon", "Sunset", "Poison", "Custom");
    public EnumSetting<FontType> font = new EnumSetting<FontType>("Font", "UI Font", FontType.ProductSans, "Mojangles", "Product Sans");
    public BoolSetting fix = new BoolSetting("Fix", "Fix Freezes the gradient animation", false);
    public NumberSetting colorSpeed = new NumberSetting("Color Speed", "Theme animation speed", 3.0, 0.01, 20.0, 0.01);
    public NumberSetting saturation = new NumberSetting("Saturation", "Color intensity", 1.0, 0.0, 1.0, 0.01);
    public ColorSetting color1 = new ColorSetting("Color 1", "Custom color 1", -65536);
    public ColorSetting color2 = new ColorSetting("Color 2", "Custom color 2", -33024);

    public Interface_h() {
        super("Interface", "Customize the visuals!", Category.VISUAL, 0, true);
        this.colorSpeed.visibility = () -> !this.fix.value;
        this.addSettings(this.namingStyle, this.theme, this.font, this.fix, this.colorSpeed, this.saturation, this.color1, this.color2);
    }

    public static enum NamingStyle {
        Lowercase,
        LowerSpaced,
        Normal,
        Spaced;

    }

    public static enum ColorTheme {
        Trans,
        Rainbow,
        Bubblegum,
        Watermelon,
        Sunset,
        Poison,
        Custom;

    }

    public static enum FontType {
        Mojangles,
        ProductSans;

    }
}

