/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Visual.Nametags;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class Nametags_h
extends ModuleHeader {
    public BoolSetting antiBotFilter = new BoolSetting("AntiBot Filter", "Filter out bots using AntiBot", true);
    public BoolSetting showTeammates = new BoolSetting("Show Teammates", "Render custom nametags on teammates", true);
    public BoolSetting showDistance = new BoolSetting("Show Distance", "Appends player distance to nametag", true);
    public NumberSetting opacity = new NumberSetting("Opacity", "Background box opacity", 0.6, 0.0, 1.0, 0.05);
    public EnumSetting<FontMode> fontMode = new EnumSetting<FontMode>("FontMode", "Font Mode", FontMode.InterfaceF, "Interface", "Mojangles", "Product Sans");
    public NumberSetting size = new NumberSetting("Size", "Overall nametag size multiplier", 1.0, 0.5, 3.0, 0.1);

    public Nametags_h() {
        super("Nametags", "Renders customized overhead nametags", Category.VISUAL, 0, false);
        this.addSettings(this.antiBotFilter, this.showTeammates, this.showDistance, this.opacity, this.fontMode, this.size);
    }

    public static enum FontMode {
        InterfaceF,
        Mojangles,
        ProductSans;

    }
}

