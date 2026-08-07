/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Combat.KillAura;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class KillAura_h
extends ModuleHeader {
    public NumberSetting range = new NumberSetting("Range", "Attack range", 4.2, 1.0, 6.0, 0.1);
    public BoolSetting rotate = new BoolSetting("Rotate", "Aims at target", true);
    public BoolSetting silent = new BoolSetting("Silent", "Spoofs rotations silently to server", true);
    public BoolSetting hitboxCheck = new BoolSetting("Hitbox Check", "Only attack when your server-side look vector intersects target hitbox", false);
    public EnumSetting<MoveFix> moveFix = new EnumSetting<MoveFix>("Move Fix", "Silent movement angle correction", MoveFix.None, "None", "Simple", "Silent", "Test");
    public BoolSetting disableCivbreak = new BoolSetting("Disable Civbreak", "Forcibly disables rotations if CivBreak is enabled", false);
    public BoolSetting delayOnCivbreak = new BoolSetting("Delay On Civbreak", "Delays attacks when CivBreak is about to break", false);
    public EnumSetting<SwingMode> swingMode = new EnumSetting<SwingMode>("Swing Mode", "Customizes hand swing animation and packet behavior", SwingMode.Normal, "Normal", "ClientOnly", "ServerOnly", "NoSwing");
    public EnumSetting<AttackMode> attackMode = new EnumSetting<AttackMode>("Attack Mode", "Bypass attack send protocol", AttackMode.Normal, "Normal", "CooldownBypass");
    public EnumSetting<RotationMode> rotationMode = new EnumSetting<RotationMode>("Rotation Mode", "How KillAura rotates", RotationMode.Normal, "Normal", "Flick", "Flick2", "Old");
    public EnumSetting<TargetPointMode> targetPointMode = new EnumSetting<TargetPointMode>("Target Point", "Aims location on target", TargetPointMode.Body, "Head", "Body", "Feet", "Actor", "Simple");
    public NumberSetting flickDuration = new NumberSetting("Flick Duration", "Hold aim duration (ms)", 100.0, 10.0, 500.0, 10.0);
    public EnumSetting<AttackDelayMode> attackDelayMode = new EnumSetting<AttackDelayMode>("Attack Delay Mode", "Combat timing engine", AttackDelayMode.Ver_1_8, "1.8", "1.9+");
    public EnumSetting<SwitchMode> switchMode = new EnumSetting<SwitchMode>("Switch Mode", "Sword hotbar auto-switch", SwitchMode.NONE, "None", "Spoof", "Fake");
    public EnumSetting<CritMode> critMode = new EnumSetting<CritMode>("Criticals", "Forces critical hits on attack", CritMode.NONE, "None", "NCP", "Falling", "Low", "Down");
    public NumberSetting minCps = new NumberSetting("Min CPS", "Minimum attacks per second", 8.0, 1.0, 20.0, 0.5);
    public NumberSetting maxCps = new NumberSetting("Max CPS", "Maximum attacks per second", 12.0, 1.0, 20.0, 0.5);
    public NumberSetting cooldownMin = new NumberSetting("Cooldown Min", "Minimum attack progress", 1.0, 0.0, 1.0, 0.01);
    public NumberSetting cooldownMax = new NumberSetting("Cooldown Max", "Maximum attack progress", 1.0, 0.0, 1.0, 0.01);
    public NumberSetting horizontalFollowRate = new NumberSetting("H-Follow Rate", "Horizontal tracking percentage", 1.0, 0.0, 1.0, 0.05);
    public NumberSetting verticalFollowRate = new NumberSetting("V-Follow Rate", "Vertical tracking percentage", 1.0, 0.0, 1.0, 0.05);
    public NumberSetting clampThreshold = new NumberSetting("Clamp Threshold", "Instantly snap angle if diff is larger", 10.0, 0.0, 90.0, 0.5);
    public NumberSetting horizontalSpeed = new NumberSetting("H-Speed", "Max horizontal rotation speed", 180.0, 1.0, 180.0, 1.0);
    public NumberSetting verticalSpeed = new NumberSetting("V-Speed", "Max vertical rotation speed", 90.0, 1.0, 180.0, 1.0);

    public KillAura_h() {
        super("KillAura", "Automatically attacks entities", Category.COMBAT, 0, false);
        this.silent.visibility = () -> this.rotate.value;
        this.moveFix.visibility = () -> this.rotate.value;
        this.hitboxCheck.visibility = () -> this.rotate.value;
        this.disableCivbreak.visibility = () -> this.rotate.value;
        this.delayOnCivbreak.visibility = () -> this.rotate.value;
        this.targetPointMode.visibility = () -> this.rotate.value && this.rotationMode.value != RotationMode.Old;
        this.flickDuration.visibility = () -> this.rotate.value && this.rotationMode.value == RotationMode.Flick2;
        this.attackDelayMode.visibility = () -> true;
        this.minCps.visibility = () -> this.attackDelayMode.value == AttackDelayMode.Ver_1_8;
        this.maxCps.visibility = () -> this.attackDelayMode.value == AttackDelayMode.Ver_1_8;
        this.cooldownMin.visibility = () -> this.attackDelayMode.value == AttackDelayMode.Ver_1_9;
        this.cooldownMax.visibility = () -> this.attackDelayMode.value == AttackDelayMode.Ver_1_9;
        this.horizontalFollowRate.visibility = () -> this.rotate.value;
        this.verticalFollowRate.visibility = () -> this.rotate.value;
        this.clampThreshold.visibility = () -> this.rotate.value && (this.horizontalFollowRate.value < 1.0 || this.verticalFollowRate.value < 1.0);
        this.horizontalSpeed.visibility = () -> this.rotate.value;
        this.verticalSpeed.visibility = () -> this.rotate.value;
        this.addSettings(this.range, this.rotate, this.silent, this.moveFix, this.hitboxCheck, this.disableCivbreak, this.delayOnCivbreak, this.swingMode, this.attackMode, this.rotationMode, this.targetPointMode, this.flickDuration, this.attackDelayMode, this.switchMode, this.critMode, this.minCps, this.maxCps, this.cooldownMin, this.cooldownMax, this.horizontalFollowRate, this.verticalFollowRate, this.clampThreshold, this.horizontalSpeed, this.verticalSpeed);
    }

    public static enum MoveFix {
        None,
        Simple,
        Silent,
        Test;

    }

    public static enum SwingMode {
        Normal,
        ClientOnly,
        ServerOnly,
        NoSwing;

    }

    public static enum AttackMode {
        Normal,
        CooldownBypass;

    }

    public static enum RotationMode {
        Normal,
        Flick,
        Flick2,
        Old;

    }

    public static enum TargetPointMode {
        Head,
        Body,
        Feet,
        Actor,
        Simple;

    }

    public static enum AttackDelayMode {
        Ver_1_8,
        Ver_1_9;

    }

    public static enum SwitchMode {
        NONE,
        SPOOF,
        FAKE;

    }

    public static enum CritMode {
        NONE,
        NCP,
        FALLING,
        LOW,
        DOWN;

    }
}

