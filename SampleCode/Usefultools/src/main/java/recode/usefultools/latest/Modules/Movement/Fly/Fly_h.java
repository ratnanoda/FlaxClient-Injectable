/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Movement.Fly;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class Fly_h
extends ModuleHeader {
    public EnumSetting<Mode> mode = new EnumSetting<Mode>("Mode", "Flight mode", Mode.Old, "Old", "Motion", "Jump", "SpeedStep");
    public NumberSetting settingValue = new NumberSetting("Setting Value", "Number of SpeedStep stages", 3.0, 1.0, 5.0, 1.0);
    public EnumSetting<SpeedMode> speedMode = new EnumSetting<SpeedMode>("Speed Mode", "Flight horizontal/vertical split", SpeedMode.Normal, "Normal", "Separation");
    public NumberSetting speed = new NumberSetting("Speed", "Step 1 speed multiplier", 5.0, 0.0, 20.0, 0.1);
    public NumberSetting speedStep2 = new NumberSetting("Speed Step 2", "Step 2 speed multiplier", 6.0, 0.0, 20.0, 0.1);
    public NumberSetting speedStep3 = new NumberSetting("Speed Step 3", "Step 3 speed multiplier", 4.0, 0.0, 20.0, 0.1);
    public NumberSetting speedStep4 = new NumberSetting("Speed Step 4", "Step 4 speed multiplier", 4.0, 0.0, 20.0, 0.1);
    public NumberSetting speedStep5 = new NumberSetting("Speed Step 5", "Step 5 speed multiplier", 4.0, 0.0, 20.0, 0.1);
    public NumberSetting verticalSpeed = new NumberSetting("Vertical Speed", "Step 1 vertical speed", 3.0, 0.0, 20.0, 0.1);
    public NumberSetting verticalStep2 = new NumberSetting("Vertical Step 2", "Step 2 vertical speed", 4.0, 0.0, 20.0, 0.1);
    public NumberSetting verticalStep3 = new NumberSetting("Vertical Step 3", "Step 3 vertical speed", 3.0, 0.0, 20.0, 0.1);
    public NumberSetting verticalStep4 = new NumberSetting("Vertical Step 4", "Step 4 vertical speed", 3.0, 0.0, 20.0, 0.1);
    public NumberSetting verticalStep5 = new NumberSetting("Vertical Step 5", "Step 5 vertical speed", 3.0, 0.0, 20.0, 0.1);
    public NumberSetting friction = new NumberSetting("Friction", "Step 1 friction multiplier", 0.8, 0.0, 1.0, 0.01);
    public NumberSetting frictionStep2 = new NumberSetting("Friction Step 2", "Step 2 friction multiplier", 0.8, 0.0, 1.0, 0.01);
    public NumberSetting frictionStep3 = new NumberSetting("Friction Step 3", "Step 3 friction multiplier", 0.8, 0.0, 1.0, 0.01);
    public NumberSetting frictionStep4 = new NumberSetting("Friction Step 4", "Step 4 friction multiplier", 0.8, 0.0, 1.0, 0.01);
    public NumberSetting frictionStep5 = new NumberSetting("Friction Step 5", "Step 5 friction multiplier", 0.8, 0.0, 1.0, 0.01);
    public NumberSetting speedTime = new NumberSetting("Speed Time (ms)", "Step 1 active duration", 500.0, 10.0, 10000.0, 10.0);
    public NumberSetting speedTime2 = new NumberSetting("Speed Time 2 (ms)", "Step 2 active duration", 1000.0, 10.0, 10000.0, 10.0);
    public NumberSetting speedTime3 = new NumberSetting("Speed Time 3 (ms)", "Step 3 active duration", 1000.0, 10.0, 10000.0, 10.0);
    public NumberSetting speedTime4 = new NumberSetting("Speed Time 4 (ms)", "Step 4 active duration", 1000.0, 10.0, 10000.0, 10.0);
    public NumberSetting speedTime5 = new NumberSetting("Speed Time 5 (ms)", "Step 5 active duration", 1000.0, 10.0, 10000.0, 10.0);
    public NumberSetting glideSpeed = new NumberSetting("Glide Speed", "Step 1 slow descending speed", 0.02, 0.0, 1.0, 0.01);
    public NumberSetting glideStep2 = new NumberSetting("Glide Step 2", "Step 2 slow descending speed", 0.02, 0.0, 1.0, 0.01);
    public NumberSetting glideStep3 = new NumberSetting("Glide Step 3", "Step 3 slow descending speed", 0.02, 0.0, 1.0, 0.01);
    public NumberSetting glideStep4 = new NumberSetting("Glide Step 4", "Step 4 slow descending speed", 0.02, 0.0, 1.0, 0.01);
    public NumberSetting glideStep5 = new NumberSetting("Glide Step 5", "Step 5 slow descending speed", 0.02, 0.0, 1.0, 0.01);
    public BoolSetting frictionReset = new BoolSetting("Friction Reset", "Resets decayed speed when transitioning steps", false);
    public EnumSetting<FastStopMode> fastStopMode = new EnumSetting<FastStopMode>("Fast Stop", "Halt velocity immediately on release key", FastStopMode.None, "None", "Normal", "Always");
    public BoolSetting fastStopY = new BoolSetting("Fast Stop Y", "Also halts vertical (Y) velocity on stop", false);
    public EnumSetting<HaltMode> haltMode = new EnumSetting<HaltMode>("Halt Mode", "Halt velocity when module or flight turns off", HaltMode.Normal, "None", "Normal", "AndDamage");
    public BoolSetting haltY = new BoolSetting("Halt Y", "Also halts vertical (Y) velocity on halt", false);
    public BoolSetting timerBoost = new BoolSetting("Timer Boost", "Enables game speed modification", false);
    public NumberSetting timerBoostValue = new NumberSetting("Timer Boost Value", "Step 1 game tickrate", 20.0, 0.0, 60.0, 0.5);
    public NumberSetting timerStep2 = new NumberSetting("Timer Step 2", "Step 2 game tickrate", 20.0, 0.0, 60.0, 0.5);
    public NumberSetting timerStep3 = new NumberSetting("Timer Step 3", "Step 3 game tickrate", 20.0, 0.0, 60.0, 0.5);
    public NumberSetting timerStep4 = new NumberSetting("Timer Step 4", "Step 4 game tickrate", 20.0, 0.0, 60.0, 0.5);
    public NumberSetting timerStep5 = new NumberSetting("Timer Step 5", "Step 5 game tickrate", 20.0, 0.0, 60.0, 0.5);
    public BoolSetting netskip = new BoolSetting("Netskip", "Drops player packet rates to stretch buffer", false);
    public NumberSetting netskipDelay = new NumberSetting("Netskip Delay (ms)", "Step 1 packet skip interval", 50.0, 0.0, 5000.0, 10.0);
    public NumberSetting netskipStep2 = new NumberSetting("Netskip Step 2 (ms)", "Step 2 packet skip interval", 50.0, 0.0, 5000.0, 10.0);
    public NumberSetting netskipStep3 = new NumberSetting("Netskip Step 3 (ms)", "Step 3 packet skip interval", 50.0, 0.0, 5000.0, 10.0);
    public NumberSetting netskipStep4 = new NumberSetting("Netskip Step 4 (ms)", "Step 4 packet skip interval", 50.0, 0.0, 5000.0, 10.0);
    public NumberSetting netskipStep5 = new NumberSetting("Netskip Step 5 (ms)", "Step 5 packet skip interval", 50.0, 0.0, 5000.0, 10.0);
    public EnumSetting<TriggerType> triggerType = new EnumSetting<TriggerType>("Trigger Type", "Bypass flight trigger mode", TriggerType.None, "None", "KnockBack", "Damage", "VulcanTest");
    public NumberSetting vulcanFlyTime = new NumberSetting("Vulcan Fly Time", "Flight duration in seconds for VulcanTest", 2.0, 0.5, 15.0, 0.1);
    public EnumSetting<DamageFlyTimeMode> damageFlyTimeMode = new EnumSetting<DamageFlyTimeMode>("Fly Time Mode", "Active flight duration style", DamageFlyTimeMode.UntilDisabled, "UntilDisabled", "Ticks");
    public NumberSetting triggerFlyTime = new NumberSetting("Trigger Fly Time (ms)", "Duration of allowed flight in milliseconds", 1500.0, 50.0, 10000.0, 50.0);
    public EnumSetting<SelfDamageMode> selfDamageMode = new EnumSetting<SelfDamageMode>("Self Damage", "Triggers self damage on toggle", SelfDamageMode.None, "None", "Fall", "SelfEntityAttack");

    public Fly_h() {
        super("Fly", "Allows you to fly freely", Category.MOVEMENT, 0, false);
        this.speedMode.visibility = () -> this.mode.value != Mode.Old;
        this.settingValue.visibility = () -> this.mode.value == Mode.SpeedStep;
        this.speed.visibility = () -> this.mode.value != Mode.Old;
        this.speedStep2.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 2.0;
        this.speedStep3.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 3.0;
        this.speedStep4.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 4.0;
        this.speedStep5.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 5.0;
        this.verticalSpeed.visibility = () -> this.mode.value != Mode.Old && this.speedMode.value == SpeedMode.Separation;
        this.verticalStep2.visibility = () -> this.mode.value == Mode.SpeedStep && this.speedMode.value == SpeedMode.Separation && this.settingValue.value >= 2.0;
        this.verticalStep3.visibility = () -> this.mode.value == Mode.SpeedStep && this.speedMode.value == SpeedMode.Separation && this.settingValue.value >= 3.0;
        this.verticalStep4.visibility = () -> this.mode.value == Mode.SpeedStep && this.speedMode.value == SpeedMode.Separation && this.settingValue.value >= 4.0;
        this.verticalStep5.visibility = () -> this.mode.value == Mode.SpeedStep && this.speedMode.value == SpeedMode.Separation && this.settingValue.value >= 5.0;
        this.friction.visibility = () -> this.mode.value != Mode.Old;
        this.frictionStep2.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 2.0;
        this.frictionStep3.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 3.0;
        this.frictionStep4.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 4.0;
        this.frictionStep5.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 5.0;
        this.speedTime.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 1.0;
        this.speedTime2.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 2.0;
        this.speedTime3.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 3.0;
        this.speedTime4.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 4.0;
        this.speedTime5.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 5.0;
        this.glideSpeed.visibility = () -> this.mode.value != Mode.Old;
        this.glideStep2.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 2.0;
        this.glideStep3.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 3.0;
        this.glideStep4.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 4.0;
        this.glideStep5.visibility = () -> this.mode.value == Mode.SpeedStep && this.settingValue.value >= 5.0;
        this.frictionReset.visibility = () -> this.mode.value == Mode.SpeedStep;
        this.timerBoost.visibility = () -> this.mode.value != Mode.Old;
        this.timerBoostValue.visibility = () -> this.mode.value != Mode.Old && this.timerBoost.value;
        this.timerStep2.visibility = () -> this.mode.value == Mode.SpeedStep && this.timerBoost.value && this.settingValue.value >= 2.0;
        this.timerStep3.visibility = () -> this.mode.value == Mode.SpeedStep && this.timerBoost.value && this.settingValue.value >= 3.0;
        this.timerStep4.visibility = () -> this.mode.value == Mode.SpeedStep && this.timerBoost.value && this.settingValue.value >= 4.0;
        this.timerStep5.visibility = () -> this.mode.value == Mode.SpeedStep && this.timerBoost.value && this.settingValue.value >= 5.0;
        this.netskip.visibility = () -> this.mode.value != Mode.Old;
        this.netskipDelay.visibility = () -> this.mode.value != Mode.Old && this.netskip.value;
        this.netskipStep2.visibility = () -> this.mode.value == Mode.SpeedStep && this.netskip.value && this.settingValue.value >= 2.0;
        this.netskipStep3.visibility = () -> this.mode.value == Mode.SpeedStep && this.netskip.value && this.settingValue.value >= 3.0;
        this.netskipStep4.visibility = () -> this.mode.value == Mode.SpeedStep && this.netskip.value && this.settingValue.value >= 4.0;
        this.netskipStep5.visibility = () -> this.mode.value == Mode.SpeedStep && this.netskip.value && this.settingValue.value >= 5.0;
        this.fastStopMode.visibility = () -> this.mode.value != Mode.Old;
        this.fastStopY.visibility = () -> this.mode.value != Mode.Old && this.fastStopMode.value != FastStopMode.None;
        this.haltMode.visibility = () -> this.mode.value != Mode.Old;
        this.haltY.visibility = () -> this.mode.value != Mode.Old;
        this.triggerType.visibility = () -> this.mode.value != Mode.Old;
        this.damageFlyTimeMode.visibility = () -> this.mode.value != Mode.Old && this.triggerType.value != TriggerType.None && this.triggerType.value != TriggerType.VulcanTest;
        this.triggerFlyTime.visibility = () -> this.mode.value != Mode.Old && this.triggerType.value != TriggerType.None && this.triggerType.value != TriggerType.VulcanTest && this.damageFlyTimeMode.value == DamageFlyTimeMode.Ticks;
        this.vulcanFlyTime.visibility = () -> this.mode.value != Mode.Old && this.triggerType.value == TriggerType.VulcanTest;
        this.selfDamageMode.visibility = () -> this.mode.value != Mode.Old;
        this.addSettings(this.mode, this.settingValue, this.speedMode, this.speed, this.speedStep2, this.speedStep3, this.speedStep4, this.speedStep5, this.verticalSpeed, this.verticalStep2, this.verticalStep3, this.verticalStep4, this.verticalStep5, this.friction, this.frictionStep2, this.frictionStep3, this.frictionStep4, this.frictionStep5, this.speedTime, this.speedTime2, this.speedTime3, this.speedTime4, this.speedTime5, this.glideSpeed, this.glideStep2, this.glideStep3, this.glideStep4, this.glideStep5, this.frictionReset, this.fastStopMode, this.fastStopY, this.haltMode, this.haltY, this.timerBoost, this.timerBoostValue, this.timerStep2, this.timerStep3, this.timerStep4, this.timerStep5, this.netskip, this.netskipDelay, this.netskipStep2, this.netskipStep3, this.netskipStep4, this.netskipStep5, this.triggerType, this.vulcanFlyTime, this.damageFlyTimeMode, this.triggerFlyTime, this.selfDamageMode);
    }

    public static enum Mode {
        Old,
        Motion,
        Jump,
        SpeedStep;

    }

    public static enum SpeedMode {
        Normal,
        Separation;

    }

    public static enum FastStopMode {
        None,
        Normal,
        Always;

    }

    public static enum HaltMode {
        None,
        Normal,
        AndDamage;

    }

    public static enum TriggerType {
        None,
        KnockBack,
        Damage,
        VulcanTest;

    }

    public static enum DamageFlyTimeMode {
        UntilDisabled,
        Ticks;

    }

    public static enum SelfDamageMode {
        None,
        Fall,
        SelfEntityAttack;

    }
}

