/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Misc.ServerRotation;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class ServerRotation_h
extends ModuleHeader {
    public EnumSetting<BodyMode> bodyMode = new EnumSetting<BodyMode>("Body Mode", "Body rotation alignment", BodyMode.Threshold, "Sync", "Static", "Threshold");
    public EnumSetting<LerpMode> lerpMode = new EnumSetting<LerpMode>("Lerp Mode", "Visual interpolation mode", LerpMode.Normal, "None", "Normal", "Direct", "DirectLerp");
    public EnumSetting<Dependency> speedMode = new EnumSetting<Dependency>("Speed Mode", "Lerp Speed Dependency", Dependency.Tick, "Tick", "FPS");
    public NumberSetting lerpSpeed = new NumberSetting("Lerp Speed", "Rotation smooth speed", 10.0, 1.0, 20.0, 1.0);
    public NumberSetting threshold = new NumberSetting("Threshold", "Body rotation threshold", 45.0, 0.0, 180.0, 1.0);
    public BoolSetting smoothCamera = new BoolSetting("Smooth Camera", "Smooths head even when inactive", true);
    public BoolSetting fixWinding = new BoolSetting("Fix Winding", "Prevents 360-degree spins across 180/-180", true);

    public ServerRotation_h() {
        super("ServerRotation", "Handles player visual rotations", Category.MISC, 0, true);
        this.threshold.visibility = () -> this.bodyMode.value == BodyMode.Threshold;
        this.speedMode.visibility = () -> this.lerpMode.value == LerpMode.Normal || this.lerpMode.value == LerpMode.DirectLerp;
        this.lerpSpeed.visibility = () -> this.lerpMode.value == LerpMode.Normal || this.lerpMode.value == LerpMode.DirectLerp;
        this.addSettings(this.bodyMode, this.lerpMode, this.speedMode, this.lerpSpeed, this.threshold, this.smoothCamera, this.fixWinding);
    }

    public static enum BodyMode {
        Sync,
        Static,
        Threshold;

    }

    public static enum LerpMode {
        None,
        Normal,
        Direct,
        DirectLerp;

    }

    public static enum Dependency {
        Tick,
        FPS;

    }
}

