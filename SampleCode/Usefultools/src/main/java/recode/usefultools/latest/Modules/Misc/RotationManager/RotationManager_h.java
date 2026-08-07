/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Misc.RotationManager;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class RotationManager_h
extends ModuleHeader {
    public BoolSetting alwaysClamp = new BoolSetting("Always Clamp", "Always round rotations sent to server", false);
    public BoolSetting gcdBypass = new BoolSetting("GCD Bypass", "Match real Minecraft sensitivity grid", true);
    public NumberSetting step = new NumberSetting("Step", "Rounding interval step", 0.15, 0.01, 1.0, 0.01);
    public BoolSetting formatDecimals = new BoolSetting("Format Decimals", "Clean float precision noise", true);
    public BoolSetting smoothDelta = new BoolSetting("Smooth Delta", "Preserves micro-variations", true);
    public NumberSetting maxDelta = new NumberSetting("Max Variation", "Maximum allowed micro-delta", 0.005, 0.001, 0.05, 0.001);

    public RotationManager_h() {
        super("RotationManager", "Centralized rotation filtering", Category.MISC, 0, true);
        this.step.visibility = () -> !this.gcdBypass.value;
        this.smoothDelta.visibility = () -> this.step.value > 0.0 || this.gcdBypass.value;
        this.maxDelta.visibility = () -> this.smoothDelta.value && (this.step.value > 0.0 || this.gcdBypass.value);
        this.addSettings(this.alwaysClamp, this.gcdBypass, this.step, this.formatDecimals, this.smoothDelta, this.maxDelta);
    }
}

