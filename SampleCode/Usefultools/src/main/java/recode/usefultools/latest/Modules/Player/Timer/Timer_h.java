/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Player.Timer;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.NumberSetting;

public class Timer_h
extends ModuleHeader {
    public NumberSetting speed = new NumberSetting("Speed", "Game speed multiplier", 1.0, 0.1, 10.0, 0.05);

    public Timer_h() {
        super("Timer", "Changes the overall game speed", Category.PLAYER, 0, false);
        this.addSettings(this.speed);
    }
}

