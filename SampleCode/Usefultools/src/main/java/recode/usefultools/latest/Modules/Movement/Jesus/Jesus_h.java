/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Movement.Jesus;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class Jesus_h
extends ModuleHeader {
    public EnumSetting<Mode> mode = new EnumSetting<Mode>("Mode", "Jesus movement protocol", Mode.Normal, "Normal", "OnGroundSpoof", "Dolphin");
    public NumberSetting speed = new NumberSetting("Speed", "Liquid movement multiplier", 1.0, 0.1, 5.0, 0.05);
    public NumberSetting dolphinDelay = new NumberSetting("Dolphin Delay", "Ticks delay inside water before jump", 10.0, 0.0, 40.0, 1.0);
    public NumberSetting dolphinMotion = new NumberSetting("Dolphin Motion", "Upward leap velocity value", 0.42, 0.1, 1.5, 0.01);

    public Jesus_h() {
        super("Jesus", "Allows you to walk on liquids", Category.MOVEMENT, 0, false);
        this.dolphinDelay.visibility = () -> this.mode.value == Mode.Dolphin;
        this.dolphinMotion.visibility = () -> this.mode.value == Mode.Dolphin;
        this.addSettings(this.mode, this.speed, this.dolphinDelay, this.dolphinMotion);
    }

    public static enum Mode {
        Normal,
        OnGroundSpoof,
        Dolphin;

    }
}

