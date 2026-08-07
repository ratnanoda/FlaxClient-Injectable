/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Player.Scaffold;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class Scaffold_h
extends ModuleHeader {
    public NumberSetting places = new NumberSetting("Places", "Blocks placed per tick", 1.0, 1.0, 5.0, 1.0);
    public NumberSetting extend = new NumberSetting("Extend", "Horizontal block reach", 1.0, -5.0, 5.0, 0.1);
    public BoolSetting extendOnly = new BoolSetting("Extend Only", "Only place blocks at the exact extend boundary, do not fill in-between", false);
    public BoolSetting lockTimeBefore = new BoolSetting("Lock Time Before", "Locks aim on block before placing", false);
    public NumberSetting beforeTime = new NumberSetting("Before Time", "Duration to lock aim before placing (ms)", 100.0, 0.0, 1000.0, 10.0);
    public EnumSetting<SafeWalkMode> safeWalkMode = new EnumSetting<SafeWalkMode>("SafeWalk Mode", "SafeWalk behavior on block edges", SafeWalkMode.None, "None", "Normal", "Sneak");
    public NumberSetting edgeDistanceForward = new NumberSetting("Edge Distance Forward", "Threshold distance from forward edge to start sneak", 0.15, 0.01, 0.5, 0.01);
    public NumberSetting edgeDistanceSideways = new NumberSetting("Edge Distance Sideways", "Threshold distance from sideways edge to start sneak", 0.15, 0.01, 0.5, 0.01);
    public NumberSetting sneakReleaseDelay = new NumberSetting("Sneak Release Delay", "Delay ticks to release sneak key after leaving edge", 2.0, 0.0, 10.0, 1.0);
    public BoolSetting beforeTimeOnly = new BoolSetting("Before Time Only", "Apply SafeWalk only during the before time duration", false);
    public NumberSetting placeDelay = new NumberSetting("Place Delay", "Delay between placements (ms)", 0.0, 0.0, 1000.0, 10.0);
    public NumberSetting lookTime = new NumberSetting("Look Time", "Rotation retention duration (ms)", 100.0, 0.0, 1000.0, 50.0);
    public EnumSetting<SwitchMode> switchMode = new EnumSetting<SwitchMode>("Switch Mode", "Block hotbar auto-switch", SwitchMode.Spoof, "None", "Full", "Spoof", "Fake", "FullReverse");
    public BoolSetting switchTime = new BoolSetting("Switch Time", "Delay slot reverting after placing block", false);
    public NumberSetting switchTimeValue = new NumberSetting("Switch Time Value", "Delay duration to revert slot (ms)", 200.0, 0.0, 2000.0, 10.0);
    public EnumSetting<RotMode> rotMode = new EnumSetting<RotMode>("Rot Mode", "Aim rotation mode", RotMode.BACK, "None", "Normal", "Down", "Back", "Backwards", "Hive", "HypixelBack", "HypixelSideways");
    public EnumSetting<SprintMode> sprintMode = new EnumSetting<SprintMode>("Sprint Mode", "Sprint override behavior", SprintMode.NONE, "None", "Vanilla");
    public EnumSetting<TowerMode> towerMode = new EnumSetting<TowerMode>("Tower Mode", "Auto tower jump mode", TowerMode.VELOCITY, "None", "Vanilla", "Velocity");
    public EnumSetting<SwingMode> swingMode = new EnumSetting<SwingMode>("Swing Mode", "Hand swinging style", SwingMode.NORMAL, "None", "Normal", "Silent");
    public EnumSetting<MoveFix> moveFix = new EnumSetting<MoveFix>("Move Fix", "Silent movement angle correction", MoveFix.None, "None", "Simple", "Silent", "Test");
    public BoolSetting fakeBack = new BoolSetting("Fake Back", "Force look back visually", false);
    public BoolSetting lockY = new BoolSetting("Lock Y", "Lock blocks on activation height", true);
    public NumberSetting diagonalRange = new NumberSetting("Diagonal Range", "Diagonal block search range", 1.0, 1.0, 10.0, 1.0);

    public Scaffold_h() {
        super("Scaffold", "Automatically places blocks under you", Category.PLAYER, 0, false);
        this.lookTime.visibility = () -> this.rotMode.value == RotMode.DOWN || this.rotMode.value == RotMode.BACKWARDS;
        this.fakeBack.visibility = () -> this.rotMode.value != RotMode.NONE;
        this.moveFix.visibility = () -> this.rotMode.value != RotMode.NONE;
        this.extendOnly.visibility = () -> this.extend.value != 0.0;
        this.lockTimeBefore.visibility = () -> this.rotMode.value != RotMode.NONE;
        this.beforeTime.visibility = () -> this.rotMode.value != RotMode.NONE && this.lockTimeBefore.value;
        this.edgeDistanceForward.visibility = () -> this.safeWalkMode.value == SafeWalkMode.Sneak;
        this.edgeDistanceSideways.visibility = () -> this.safeWalkMode.value == SafeWalkMode.Sneak;
        this.sneakReleaseDelay.visibility = () -> this.safeWalkMode.value == SafeWalkMode.Sneak;
        this.beforeTimeOnly.visibility = () -> this.rotMode.value != RotMode.NONE && this.lockTimeBefore.value && this.safeWalkMode.value != SafeWalkMode.None;
        this.switchTime.visibility = () -> this.switchMode.value != SwitchMode.None;
        this.switchTimeValue.visibility = () -> this.switchMode.value != SwitchMode.None && this.switchTime.value;
        this.addSettings(this.places, this.extend, this.extendOnly, this.lockTimeBefore, this.beforeTime, this.safeWalkMode, this.edgeDistanceForward, this.edgeDistanceSideways, this.sneakReleaseDelay, this.beforeTimeOnly, this.placeDelay, this.lookTime, this.switchMode, this.switchTime, this.switchTimeValue, this.rotMode, this.sprintMode, this.towerMode, this.swingMode, this.moveFix, this.fakeBack, this.lockY, this.diagonalRange);
    }

    public static enum SafeWalkMode {
        None,
        Normal,
        Sneak;

    }

    public static enum SwitchMode {
        None,
        Full,
        Spoof,
        Fake,
        FullReverse;

    }

    public static enum RotMode {
        NONE,
        NORMAL,
        DOWN,
        BACK,
        BACKWARDS,
        HIVE,
        HYPIXEL_BACK,
        HYPIXEL_SIDEWAYS;

    }

    public static enum SprintMode {
        NONE,
        VANILLA;

    }

    public static enum TowerMode {
        NONE,
        VANILLA,
        VELOCITY;

    }

    public static enum SwingMode {
        NONE,
        NORMAL,
        SILENT;

    }

    public static enum MoveFix {
        None,
        Simple,
        Silent,
        Test;

    }
}

