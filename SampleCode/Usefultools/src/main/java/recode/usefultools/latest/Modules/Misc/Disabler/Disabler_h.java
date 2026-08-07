/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Misc.Disabler;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class Disabler_h
extends ModuleHeader {
    public EnumSetting<DMode> mode = new EnumSetting<DMode>("Mode", "Disabler mode", DMode.Vulcan, "Vulcan");
    public BoolSetting sprintD = new BoolSetting("Sprint D", "Desyncs sprinting state", false);
    public BoolSetting onGroundOnly = new BoolSetting("On Ground Only", "Only desyncs when on the ground", true);
    public BoolSetting velocityD = new BoolSetting("Velocity D", "Fakes knockback receipt to server", false);
    public EnumSetting<VeloBypass> veloBypass = new EnumSetting<VeloBypass>("Velo Bypass", "Velocity bypass mode", VeloBypass.NormalFix, "NormalFix", "Old", "Semi Full", "Break Delay", "Break Semi");
    public EnumSetting<VelMode> velocityMode = new EnumSetting<VelMode>("Velocity Mode", "Which axis to spoof", VelMode.Both, "Horizontal Only", "Vertical Only", "Both");
    public NumberSetting horizontalPct = new NumberSetting("Horizontal %", "Horizontal multiplier", 0.0, 0.0, 100.0, 1.0);
    public NumberSetting velocityTicks = new NumberSetting("Velocity Ticks", "Ticks to distribute movement", 1.0, 1.0, 20.0, 1.0);
    public BoolSetting smoothReturn = new BoolSetting("Smooth Return", "Smoothly decays the offset back", false);
    public BoolSetting debugLog = new BoolSetting("Debug Log", "Displays packet details in chat", false);

    public Disabler_h() {
        super("Disabler", "Anti-cheat disabler utilities", Category.MISC, 0, false);
        this.onGroundOnly.visibility = () -> this.sprintD.value || this.velocityD.value;
        this.veloBypass.visibility = () -> this.velocityD.value;
        this.velocityMode.visibility = () -> this.velocityD.value;
        this.horizontalPct.visibility = () -> this.velocityD.value && this.velocityMode.value != VelMode.Vertical_Only;
        this.velocityTicks.visibility = () -> this.velocityD.value;
        this.smoothReturn.visibility = () -> this.velocityD.value;
        this.addSettings(this.mode, this.sprintD, this.onGroundOnly, this.velocityD, this.veloBypass, this.velocityMode, this.horizontalPct, this.velocityTicks, this.smoothReturn, this.debugLog);
    }

    public static enum DMode {
        Vulcan;

    }

    public static enum VeloBypass {
        NormalFix,
        Old,
        Semi_Full,
        Break_Delay,
        Break_Semi;

    }

    public static enum VelMode {
        Horizontal_Only,
        Vertical_Only,
        Both;

    }
}

