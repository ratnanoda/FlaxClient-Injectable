/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Visual.HurtCam;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.EnumSetting;

public class HurtCam_h
extends ModuleHeader {
    public EnumSetting<Mode> mode = new EnumSetting<Mode>("Mode", "Hurt camera mode", Mode.NoHurtCam, "No Hurt Cam", "Fixed Left", "None");

    public HurtCam_h() {
        super("HurtCam", "Modifies or disables damage camera tilt", Category.VISUAL, 0, false);
        this.addSettings(this.mode);
    }

    public static enum Mode {
        NoHurtCam,
        FixedLeft,
        None;

    }
}

