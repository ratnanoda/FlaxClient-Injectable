/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Player.FastBreak;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class FastBreak_h
extends ModuleHeader {
    public BoolSetting noBreakCooldown = new BoolSetting("NoBreakCooldown", "Removes delay between block breaks", true);
    public NumberSetting breakSpeed = new NumberSetting("BreakSpeed", "Mining speed divisor", 1.0, 0.0, 1.0, 0.01);

    public FastBreak_h() {
        super("FastBreak", "Breaks blocks faster", Category.PLAYER, 0, false);
        this.addSettings(this.noBreakCooldown, this.breakSpeed);
    }
}

