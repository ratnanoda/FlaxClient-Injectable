/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Misc.ShotbowNexSound;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.NumberSetting;

public class ShotbowNexSound_h
extends ModuleHeader {
    public NumberSetting minPitch = new NumberSetting("Min Pitch", "Minimum random sound pitch value", 0.5, 0.0, 2.0, 0.01);
    public NumberSetting maxPitch = new NumberSetting("Max Pitch", "Maximum random sound pitch value", 1.0, 0.0, 2.0, 0.01);
    public NumberSetting pitchStep = new NumberSetting("Pitch Step", "Interval interval for sound pitch", 0.025, 0.001, 0.5, 0.001);

    public ShotbowNexSound_h() {
        super("ShotbowNexSound", "Modifies the pitch of Annihilation Nexus break sounds", Category.MISC, 0, false);
        this.addSettings(this.minPitch, this.maxPitch, this.pitchStep);
    }
}

