/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Player.CivBreak;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.Player.Fucker.Fucker_h;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.ListSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class CivBreak_h
extends ModuleHeader {
    public NumberSetting range = new NumberSetting("Range", "Block breaker range", 4.2, 1.0, 6.0, 0.1);
    public EnumSetting<OnGroundMode> onGroundMode = new EnumSetting<OnGroundMode>("OnGround Mode", "Air break behavior", OnGroundMode.Normal, "Normal", "MiningStop", "Cancel");
    public EnumSetting<Mode> mode = new EnumSetting<Mode>("Mode", "Mining protocol mode", Mode.Auto, "Normal", "FastBreak", "Auto", "Shotbow");
    public EnumSetting<SwitchMode> switchMode = new EnumSetting<SwitchMode>("Switch Mode", "Block break hotbar auto-switch", SwitchMode.None, "None", "Spoof", "Fake");
    public EnumSetting<BypassMode> bypassMode = new EnumSetting<BypassMode>("Bypass Mode", "Bypass protocols", BypassMode.None, "None", "AntiCiv2.2", "AntiCiv2.8", "AntiCiv2.11");
    public BoolSetting oneTickPackets = new BoolSetting("1-Tick Packets", "Send all packet sequences in 1 tick", true);
    public EnumSetting<BreakDelayMode> delayMode = new EnumSetting<BreakDelayMode>("Delay Mode", "Delay mechanism", BreakDelayMode.Normal, "Normal", "AirDelay", "Simulation");
    public NumberSetting packetInterval = new NumberSetting("Packet Interval", "Ticks interval between periodic packets", 5.0, 1.0, 40.0, 1.0);
    public EnumSetting<OverlapBehavior> overlapBehavior = new EnumSetting<OverlapBehavior>("Overlap Mode", "Conflict resolution strategy", OverlapBehavior.Normal, "Normal", "Skip", "NextTick");
    public EnumSetting<BypassPacketType> bypassPacketType = new EnumSetting<BypassPacketType>("Bypass Packet", "Periodic packet type to send", BypassPacketType.Abort, "Abort", "Release", "Twoswap");
    public BoolSetting fastestMode = new BoolSetting("Fastest Mode", "Spoofs server statistics", false);
    public NumberSetting bypassCount = new NumberSetting("Bypass Count", "Number of breaks for statistical spoofing", 75.0, 1.0, 500.0, 1.0);
    public NumberSetting minDelay = new NumberSetting("Min Delay", "Minimum tick delay", 1.0, 0.0, 40.0, 1.0);
    public NumberSetting maxDelay = new NumberSetting("Max Delay", "Maximum tick delay", 5.0, 0.0, 40.0, 1.0);
    public NumberSetting retryDelay = new NumberSetting("Retry Delay", "Delay between retries if break fails", 5.0, 1.0, 40.0, 1.0);
    public BoolSetting delayOnAura = new BoolSetting("Delay On Aura", "Delays breaking when KillAura is about to swap/attack", false);
    public EnumSetting<BlockSelectMode> blockSelectMode = new EnumSetting<BlockSelectMode>("Block Select", "Target block selection", BlockSelectMode.CivBreak, "CivBreak", "Exert", "Fucker");
    public ListSetting targetBlocks = new ListSetting("Target Blocks", "Fucker-mode target block names");
    public NumberSetting breakSpeed = new NumberSetting("Break Speed", "Mining speed multiplier", 1.0, 0.0, 10.0, 0.1);
    public BoolSetting rotate = new BoolSetting("Rotate", "Rotates towards the block", true);
    public EnumSetting<RotationMode> rotationMode = new EnumSetting<RotationMode>("Rotation Mode", "Aiming style", RotationMode.Always, "Always", "StopOnly");
    public NumberSetting stopRotateDuration = new NumberSetting("Stop Duration", "Ticks to hold aim after Stop", 3.0, 0.0, 20.0, 1.0);
    public NumberSetting startRotateBefore = new NumberSetting("Start Rotate Before", "Ticks to start rotating before STOP", 2.0, 0.0, 10.0, 1.0);
    public EnumSetting<DebugLogMode> debugLogMode = new EnumSetting<DebugLogMode>("Debug Log Mode", "Packet logging options", DebugLogMode.NONE, "None", "All", "Stop", "AllExceptStop", "StartOnly", "StartStop");
    public BoolSetting esp = new BoolSetting("ESP", "Draw 3D Box around target", true);
    public EnumSetting<ColorMode> colorMode = new EnumSetting<ColorMode>("Color Mode", "Visual color profile", ColorMode.Theme, "Theme", "Custom");
    public BoolSetting easing = new BoolSetting("Easing", "Smoothly slides the ESP box", true);
    public NumberSetting easingSpeed = new NumberSetting("Easing Speed", "Speed of the ESP box slide", 10.0, 0.1, 20.0, 1.0);
    public EnumSetting<Fucker_h.ProgressBarMode> barMode = new EnumSetting<Fucker_h.ProgressBarMode>("Bar Mode", "Progress bar style", Fucker_h.ProgressBarMode.Old, "Old", "New");
    public EnumSetting<Fucker_h.FontMode> fontMode = new EnumSetting<Fucker_h.FontMode>("FontMode", "Font Mode", Fucker_h.FontMode.InterfaceF, "Interface", "Mojangles", "Product Sans");
    public BoolSetting bold = new BoolSetting("Bold", "Use bold font for text", true);
    public BoolSetting shadow = new BoolSetting("Shadow", "Draws a nice drop shadow", true);
    public EnumSetting<SwingMode> swingMode = new EnumSetting<SwingMode>("Swing Mode", "Hand swing protocol", SwingMode.Normal, "Normal", "Silent", "Old", "OldPacket", "NoSwing");
    public NumberSetting swingDelay = new NumberSetting("Swing Delay", "Ticks interval between swings", 5.0, 1.0, 20.0, 1.0);

    public CivBreak_h() {
        super("CivBreak", "Automated custom block breaker", Category.PLAYER, 0, false);
        this.bypassMode.visibility = () -> this.mode.value == Mode.Normal || this.mode.value == Mode.Auto;
        this.minDelay.visibility = () -> this.delayMode.value == BreakDelayMode.Normal || this.delayMode.value == BreakDelayMode.AirDelay || this.delayMode.value == BreakDelayMode.Simulation;
        this.maxDelay.visibility = () -> this.delayMode.value == BreakDelayMode.Normal || this.delayMode.value == BreakDelayMode.AirDelay || this.delayMode.value == BreakDelayMode.Simulation;
        this.retryDelay.visibility = () -> this.delayMode.value == BreakDelayMode.AirDelay;
        this.packetInterval.visibility = () -> this.mode.value == Mode.Shotbow;
        this.overlapBehavior.visibility = () -> this.mode.value == Mode.Shotbow;
        this.bypassPacketType.visibility = () -> this.mode.value == Mode.Shotbow;
        this.fastestMode.visibility = () -> this.delayMode.value == BreakDelayMode.Simulation;
        this.bypassCount.visibility = () -> this.fastestMode.value;
        this.targetBlocks.visibility = () -> this.blockSelectMode.value == BlockSelectMode.Fucker;
        this.stopRotateDuration.visibility = () -> this.rotate.value && this.rotationMode.value == RotationMode.StopOnly;
        this.startRotateBefore.visibility = () -> this.rotate.value && this.rotationMode.value == RotationMode.StopOnly;
        this.rotationMode.visibility = () -> this.rotate.value;
        this.colorMode.visibility = () -> this.esp.value;
        this.easing.visibility = () -> this.esp.value;
        this.easingSpeed.visibility = () -> this.esp.value && this.easing.value;
        this.fontMode.visibility = () -> this.barMode.value == Fucker_h.ProgressBarMode.New;
        this.bold.visibility = () -> this.barMode.value == Fucker_h.ProgressBarMode.New;
        this.shadow.visibility = () -> this.barMode.value == Fucker_h.ProgressBarMode.New;
        this.swingDelay.visibility = () -> this.swingMode.value == SwingMode.Normal || this.swingMode.value == SwingMode.Silent;
        this.addSettings(this.range, this.onGroundMode, this.mode, this.switchMode, this.bypassMode, this.oneTickPackets, this.delayMode, this.packetInterval, this.overlapBehavior, this.bypassPacketType, this.fastestMode, this.bypassCount, this.minDelay, this.maxDelay, this.retryDelay, this.blockSelectMode, this.targetBlocks, this.breakSpeed, this.rotate, this.rotationMode, this.stopRotateDuration, this.startRotateBefore, this.delayOnAura, this.debugLogMode, this.esp, this.colorMode, this.easing, this.easingSpeed, this.barMode, this.fontMode, this.bold, this.shadow, this.swingMode, this.swingDelay);
    }

    public static enum OnGroundMode {
        Normal,
        MiningStop,
        Cancel;

    }

    public static enum Mode {
        Normal,
        FastBreak,
        Auto,
        Shotbow;

    }

    public static enum SwitchMode {
        None,
        Spoof,
        Fake;

    }

    public static enum BypassMode {
        None,
        AntiCiv2_2,
        AntiCiv2_8,
        AntiCiv2_11;

    }

    public static enum BreakDelayMode {
        Normal,
        AirDelay,
        Simulation;

    }

    public static enum OverlapBehavior {
        Normal,
        Skip,
        NextTick;

    }

    public static enum BypassPacketType {
        Abort,
        Release,
        Twoswap;

    }

    public static enum BlockSelectMode {
        CivBreak,
        Exert,
        Fucker;

    }

    public static enum RotationMode {
        Always,
        StopOnly;

    }

    public static enum DebugLogMode {
        NONE,
        ALL,
        STOP,
        ALL_EXCEPT_STOP,
        START_ONLY,
        START_STOP;

    }

    public static enum ColorMode {
        Theme,
        Custom;

    }

    public static enum SwingMode {
        Normal,
        Silent,
        Old,
        OldPacket,
        NoSwing;

    }
}

