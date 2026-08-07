/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Movement.Speed;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class Speed_h
extends ModuleHeader {
    public EnumSetting<Mode> mode = new EnumSetting<Mode>("Mode", "Speed movement style", Mode.None, "None", "Friction", "Legit", "Custom");
    public BoolSetting autoJump = new BoolSetting("Auto Jump", "Automatically jumps while moving", false);
    public NumberSetting frictionFactor = new NumberSetting("Friction", "Motion speed multiplier", 0.98, 0.0, 1.0, 0.01);
    public EnumSetting<FastFallMode> fastFallMode = new EnumSetting<FastFallMode>("FastFall Mode", "Downward velocity apply style", FastFallMode.None, "None", "All", "1ticks");
    public NumberSetting fastFallVelocity = new NumberSetting("FastFall Velocity", "Downward velocity force", 0.42, 0.0, 3.0, 0.01);
    public NumberSetting fastFallTicks = new NumberSetting("FastFall Ticks", "Air ticks to trigger FastFall", 3.0, 0.0, 40.0, 1.0);
    public BoolSetting fastFall2 = new BoolSetting("FastFall 2", "Enables secondary FastFall tick check", false);
    public NumberSetting fastFallTicks2 = new NumberSetting("FastFall Ticks 2", "Secondary air ticks to trigger FastFall", 6.0, 0.0, 40.0, 1.0);
    public NumberSetting fastFallVelocity2 = new NumberSetting("FastFall Velocity 2", "Secondary downward velocity force", 0.42, 0.0, 3.0, 0.01);
    public BoolSetting vulcanFastFall = new BoolSetting("Vulcan FastFall", "Enables dynamic speed and ticks shift after certain count", false);
    public NumberSetting vulcanLimit = new NumberSetting("Vulcan Limit", "Required FastFall count to trigger Vulcan", 5.0, 1.0, 50.0, 1.0);
    public NumberSetting vulcanVelocity = new NumberSetting("Vulcan Velocity", "Shifted downward velocity force", 0.35, 0.0, 3.0, 0.01);
    public NumberSetting vulcanTicks = new NumberSetting("Vulcan Ticks", "Shifted air ticks to trigger FastFall", 4.0, 0.0, 40.0, 1.0);
    public EnumSetting<StrafeModeOption> strafeMode = new EnumSetting<StrafeModeOption>("Strafe Mode", "Air maneuvering protocol", StrafeModeOption.Strafe, "None", "FullMotion", "Strafe");
    public BoolSetting friction = new BoolSetting("Friction", "Enables custom friction multiplier", false);
    public NumberSetting customFrictionValue = new NumberSetting("Friction Value", "Custom friction decay factor", 0.98, 0.0, 1.0, 0.01);
    public NumberSetting speed = new NumberSetting("Speed", "Forward movement speed multiplier", 0.35, 0.0, 2.0, 0.01);
    public EnumSetting<SpeedStep> speedStep = new EnumSetting<SpeedStep>("SpeedStep", "Speed stepping logic", SpeedStep.None, "None", "GroundCount", "LastSpeed", "CollideCS", "Vulcan");
    public NumberSetting firstSpeed = new NumberSetting("First Speed", "Initial speed step value", 0.35, 0.0, 2.0, 0.01);
    public NumberSetting secondSpeed = new NumberSetting("Second Speed", "Secondary speed step value", 0.45, 0.0, 2.0, 0.01);
    public NumberSetting transitionBPS = new NumberSetting("Transition BPS", "BPS threshold to transition to second speed", 7.0, 0.0, 30.0, 0.1);
    public NumberSetting collideBPS = new NumberSetting("Collide BPS", "BPS threshold to reset to first speed", 4.0, 0.0, 30.0, 0.1);
    public NumberSetting maxStrafeAngle = new NumberSetting("Max Strafe Angle", "Speed decay multiplier", 0.9, 0.0, 1.8, 0.01);
    public NumberSetting fixValue = new NumberSetting("Fix Value", "Subtracted speed value or angle threshold", 0.1, 0.0, 180.0, 0.1);
    public EnumSetting<DecayMethod> decayMethod = new EnumSetting<DecayMethod>("Decay Method", "Decay calculation logic", DecayMethod.FixedValue, "FixedValue", "LossPercent");
    public BoolSetting oneTickBoost = new BoolSetting("1-Tick Boost", "Enables high speed boost on the first ground tick", false);
    public NumberSetting oneTickBoostSpeed = new NumberSetting("1-Tick Speed", "Ground 1st tick boost speed value", 0.6, 0.0, 3.0, 0.01);
    public EnumSetting<StrafeSpeedMode> strafeSpeedMode = new EnumSetting<StrafeSpeedMode>("Strafe Speed Mode", "Air speed boost calculation", StrafeSpeedMode.None, "None", "Solstice", "ASDOnly", "Separation");
    public NumberSetting strafeSpeed = new NumberSetting("Strafe Speed", "Horizontal strafe speed in bps", 3.0, 0.0, 20.0, 0.1);
    public NumberSetting firstStrafeSpeed = new NumberSetting("First Strafe Speed", "Initial strafe speed step value", 0.3, 0.0, 2.0, 0.01);
    public NumberSetting secondStrafeSpeed = new NumberSetting("Second Strafe Speed", "Secondary strafe speed step value", 0.4, 0.0, 2.0, 0.01);
    public EnumSetting<StrafeTicksMode> strafeTicksMode = new EnumSetting<StrafeTicksMode>("Strafe Ticks Mode", "Strafe duration profile", StrafeTicksMode.Always, "None", "Always", "StrafeTicks");
    public NumberSetting strafeTicks = new NumberSetting("Strafe Ticks", "Ticks to allow strafe after leaving ground", 5.0, 0.0, 40.0, 1.0);
    public NumberSetting velocityTicks = new NumberSetting("Velocity Ticks", "Ticks to apply velocity boost in air", 5.0, -1.0, 40.0, 1.0);
    public EnumSetting<OnGroundMode> onGroundMode = new EnumSetting<OnGroundMode>("OnGround Mode", "Ground packet spoof mode", OnGroundMode.None, "None", "Normal", "Always", "ReverseAlways", "Test");
    public NumberSetting onGroundDelay = new NumberSetting("OnGround Delay", "Delay ticks before spoofing", 3.0, 0.0, 40.0, 1.0);
    public NumberSetting onGroundTime = new NumberSetting("OnGround Time", "Duration ticks to keep spoofing", 2.0, 0.0, 40.0, 1.0);
    public EnumSetting<JumpHeightMode> jumpHeightMode = new EnumSetting<JumpHeightMode>("Jump Height Mode", "Jump physics modification", JumpHeightMode.None, "None", "FullVanilla", "Velocity");
    public NumberSetting jumpHeight = new NumberSetting("Jump Height", "Jump upward velocity", 0.42, 0.0, 2.0, 0.01);
    public BoolSetting debugLog = new BoolSetting("Debug Log", "Displays active BPS calculation on screen using ImGui", false);

    public Speed_h() {
        super("Speed", "Allows you to run significantly faster", Category.MOVEMENT, 0, false);
        this.autoJump.visibility = () -> this.mode.value != Mode.None;
        this.frictionFactor.visibility = () -> this.mode.value == Mode.Friction;
        this.fastFall2.visibility = () -> this.mode.value != Mode.None && this.fastFallMode.value != FastFallMode.None;
        this.fastFallTicks2.visibility = () -> this.mode.value != Mode.None && this.fastFallMode.value == FastFallMode.OneTick && this.fastFall2.value;
        this.fastFallVelocity2.visibility = () -> this.mode.value != Mode.None && this.fastFallMode.value == FastFallMode.OneTick && this.fastFall2.value;
        this.vulcanFastFall.visibility = () -> this.mode.value != Mode.None && this.fastFallMode.value == FastFallMode.OneTick;
        this.vulcanLimit.visibility = () -> this.mode.value != Mode.None && this.fastFallMode.value == FastFallMode.OneTick && this.vulcanFastFall.value;
        this.vulcanVelocity.visibility = () -> this.mode.value != Mode.None && this.fastFallMode.value == FastFallMode.OneTick && this.vulcanFastFall.value;
        this.vulcanTicks.visibility = () -> this.mode.value != Mode.None && this.fastFallMode.value == FastFallMode.OneTick && this.vulcanFastFall.value;
        this.strafeMode.visibility = () -> this.mode.value == Mode.Custom;
        this.friction.visibility = () -> this.mode.value == Mode.Custom;
        this.customFrictionValue.visibility = () -> this.mode.value == Mode.Custom && this.friction.value;
        this.speed.visibility = () -> this.mode.value == Mode.Legit || this.mode.value == Mode.Custom && this.speedStep.value == SpeedStep.None;
        this.speedStep.visibility = () -> this.mode.value == Mode.Custom;
        this.firstSpeed.visibility = () -> this.mode.value == Mode.Custom && this.speedStep.value != SpeedStep.None;
        this.secondSpeed.visibility = () -> this.mode.value == Mode.Custom && this.speedStep.value != SpeedStep.None;
        this.transitionBPS.visibility = () -> this.mode.value == Mode.Custom && this.speedStep.value == SpeedStep.LastSpeed;
        this.collideBPS.visibility = () -> this.mode.value == Mode.Custom && (this.speedStep.value == SpeedStep.CollideCS || this.speedStep.value == SpeedStep.Vulcan);
        this.maxStrafeAngle.visibility = () -> this.mode.value == Mode.Custom && this.speedStep.value == SpeedStep.Vulcan;
        this.fixValue.visibility = () -> this.mode.value == Mode.Custom && this.speedStep.value == SpeedStep.Vulcan;
        this.decayMethod.visibility = () -> this.mode.value == Mode.Custom && this.speedStep.value == SpeedStep.Vulcan;
        this.oneTickBoost.visibility = () -> this.mode.value == Mode.Custom;
        this.oneTickBoostSpeed.visibility = () -> this.mode.value == Mode.Custom && this.oneTickBoost.value;
        this.strafeSpeedMode.visibility = () -> this.mode.value == Mode.Custom && this.strafeMode.value != StrafeModeOption.None;
        this.strafeSpeed.visibility = () -> this.mode.value == Mode.Custom && this.strafeMode.value != StrafeModeOption.None && this.strafeSpeedMode.value == StrafeSpeedMode.Separation && this.speedStep.value == SpeedStep.None;
        this.firstStrafeSpeed.visibility = () -> this.mode.value == Mode.Custom && this.strafeMode.value != StrafeModeOption.None && this.strafeSpeedMode.value == StrafeSpeedMode.Separation && this.speedStep.value != SpeedStep.None;
        this.secondStrafeSpeed.visibility = () -> this.mode.value == Mode.Custom && this.strafeMode.value != StrafeModeOption.None && this.strafeSpeedMode.value == StrafeSpeedMode.Separation && this.speedStep.value != SpeedStep.None;
        this.strafeTicksMode.visibility = () -> this.mode.value == Mode.Custom && this.strafeMode.value != StrafeModeOption.None;
        this.strafeTicks.visibility = () -> this.mode.value == Mode.Custom && this.strafeMode.value == StrafeModeOption.Strafe && this.strafeTicksMode.value == StrafeTicksMode.StrafeTicks;
        this.velocityTicks.visibility = () -> this.mode.value == Mode.Custom && this.strafeMode.value != StrafeModeOption.None && this.strafeTicksMode.value == StrafeTicksMode.StrafeTicks;
        this.onGroundMode.visibility = () -> this.mode.value == Mode.Custom;
        this.onGroundDelay.visibility = () -> this.mode.value == Mode.Custom && this.onGroundMode.value == OnGroundMode.Test;
        this.onGroundTime.visibility = () -> this.mode.value == Mode.Custom && this.onGroundMode.value == OnGroundMode.Test;
        this.jumpHeightMode.visibility = () -> this.mode.value == Mode.Custom;
        this.jumpHeight.visibility = () -> this.mode.value == Mode.Custom && this.jumpHeightMode.value == JumpHeightMode.Velocity;
        this.addSettings(this.mode, this.autoJump, this.frictionFactor, this.fastFallMode, this.fastFallVelocity, this.fastFallTicks, this.fastFall2, this.fastFallTicks2, this.fastFallVelocity2, this.vulcanFastFall, this.vulcanLimit, this.vulcanVelocity, this.vulcanTicks, this.strafeMode, this.friction, this.customFrictionValue, this.speed, this.speedStep, this.firstSpeed, this.secondSpeed, this.transitionBPS, this.collideBPS, this.maxStrafeAngle, this.fixValue, this.decayMethod, this.oneTickBoost, this.oneTickBoostSpeed, this.strafeSpeedMode, this.strafeSpeed, this.firstStrafeSpeed, this.secondStrafeSpeed, this.strafeTicksMode, this.strafeTicks, this.velocityTicks, this.onGroundMode, this.onGroundDelay, this.onGroundTime, this.jumpHeightMode, this.jumpHeight, this.debugLog);
    }

    public static enum Mode {
        None,
        Friction,
        Legit,
        Custom;

    }

    public static enum FastFallMode {
        None,
        All,
        OneTick;

    }

    public static enum StrafeModeOption {
        None,
        FullMotion,
        Strafe;

    }

    public static enum SpeedStep {
        None,
        GroundCount,
        LastSpeed,
        CollideCS,
        Vulcan;

    }

    public static enum DecayMethod {
        FixedValue,
        LossPercent;

    }

    public static enum StrafeSpeedMode {
        None,
        Solstice,
        ASDOnly,
        Separation;

    }

    public static enum StrafeTicksMode {
        None,
        Always,
        StrafeTicks;

    }

    public static enum OnGroundMode {
        None,
        Normal,
        Always,
        ReverseAlways,
        Test;

    }

    public static enum JumpHeightMode {
        None,
        FullVanilla,
        Velocity;

    }
}

