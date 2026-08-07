/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Visual.LevelInfo;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class LevelInfo_h
extends ModuleHeader {
    public BoolSetting sprint = new BoolSetting("Sprint", "Displays player sprinting status", true);
    public BoolSetting onGround = new BoolSetting("OnGround", "Displays player ground connection status", true);
    public BoolSetting fps = new BoolSetting("FPS", "Displays game frames per second", true);
    public BoolSetting xyz = new BoolSetting("XYZ", "Displays current coordinate location", true);
    public BoolSetting bps = new BoolSetting("BPS", "Displays horizontal speed blocks-per-second", true);
    public BoolSetting shadow = new BoolSetting("Shadow", "Draws a nice drop shadow behind text", true);
    public NumberSetting fontSize = new NumberSetting("Font Size", "Size of the HUD display text", 19.0, 10.0, 40.0, 1.0);

    public LevelInfo_h() {
        super("LevelInfo", "Displays debug info on the bottom-left corner", Category.VISUAL, 0, false);
        this.addSettings(this.sprint, this.onGround, this.fps, this.xyz, this.bps, this.shadow, this.fontSize);
    }
}

