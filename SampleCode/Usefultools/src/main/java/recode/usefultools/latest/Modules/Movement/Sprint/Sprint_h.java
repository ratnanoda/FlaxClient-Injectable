/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Movement.Sprint;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class Sprint_h
extends ModuleHeader {
    public EnumSetting<Mode> mode = new EnumSetting<Mode>("Mode", "Sprinting mode", Mode.Legit, "Legit", "Vulcan");
    public BoolSetting checkHunger = new BoolSetting("Check Hunger", "Sprints only when food level is high enough", true);
    public BoolSetting sprintCancel = new BoolSetting("Sprint Cancel", "Stops sprinting if looking too far from move direction", false);
    public NumberSetting cancelAngle = new NumberSetting("Cancel Angle", "Max look/move angle deviation before stopping sprint", 45.0, 0.0, 180.0, 5.0);

    public Sprint_h() {
        super("Sprint", "Automatically sprints for you", Category.MOVEMENT, 0, false);
        this.checkHunger.visibility = () -> this.mode.value == Mode.Vulcan;
        this.cancelAngle.visibility = () -> this.sprintCancel.value;
        this.addSettings(this.mode, this.checkHunger, this.sprintCancel, this.cancelAngle);
    }

    public static enum Mode {
        Legit,
        Vulcan;

    }
}

