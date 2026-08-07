/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Misc.ToggleSound;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class ToggleSound_h
extends ModuleHeader {
    public EnumSetting<Mode> mode = new EnumSetting<Mode>("Mode", "Toggle sound mode style", Mode.Exert, "Exert", "FlowerV3?");
    public NumberSetting onVolume = new NumberSetting("On Volume", "Volume of module enabled sound", 0.5, 0.0, 1.0, 0.05);
    public NumberSetting offVolume = new NumberSetting("Off Volume", "Volume of module disabled sound", 0.5, 0.0, 1.0, 0.05);

    public ToggleSound_h() {
        super("ToggleSound", "Plays custom audio feedback when toggling modules", Category.MISC, 0, false);
        this.addSettings(this.mode, this.onVolume, this.offVolume);
    }

    public static enum Mode {
        Exert,
        FlowerV3;

    }
}

