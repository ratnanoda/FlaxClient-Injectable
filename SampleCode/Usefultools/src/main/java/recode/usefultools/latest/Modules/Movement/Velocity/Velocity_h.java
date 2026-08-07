/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Movement.Velocity;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;

public class Velocity_h
extends ModuleHeader {
    public EnumSetting<Mode> mode = new EnumSetting<Mode>("Mode", "Velocity Mode", Mode.FullCancel, "FullCancel", "SetVel");
    public NumberSetting horizontal = new NumberSetting("Horizontal", "Horizontal knockback percentage", 0.0, 0.0, 100.0, 1.0);
    public NumberSetting vertical = new NumberSetting("Vertical", "Vertical knockback percentage", 0.0, 0.0, 100.0, 1.0);
    public BoolSetting resetXZ = new BoolSetting("Reset XZ", "Resets horizontal velocity first", false);
    public BoolSetting resetY = new BoolSetting("Reset Y", "Resets vertical velocity first", false);
    public BoolSetting separateOnGround = new BoolSetting("Separate OnGround", "Use separate values on ground", false);
    public NumberSetting horizontalOnGround = new NumberSetting("H-OnGround", "Horizontal knockback on ground", 0.0, 0.0, 100.0, 1.0);
    public NumberSetting verticalOnGround = new NumberSetting("V-OnGround", "Vertical knockback on ground", 0.0, 0.0, 100.0, 1.0);
    public BoolSetting resetXZOnGround = new BoolSetting("Reset XZ OnGround", "Resets horizontal velocity on ground", false);
    public BoolSetting resetYOnGround = new BoolSetting("Reset Y OnGround", "Resets vertical velocity on ground", false);
    public BoolSetting nekozoAnni = new BoolSetting("Nekozo Anni", "Bypasses for grappling hook", false);

    public Velocity_h() {
        super("Velocity", "Reduces or eliminates knockback", Category.MOVEMENT, 0, false);
        this.horizontal.visibility = () -> this.mode.value == Mode.SetVel;
        this.vertical.visibility = () -> this.mode.value == Mode.SetVel;
        this.resetXZ.visibility = () -> this.mode.value == Mode.SetVel;
        this.resetY.visibility = () -> this.mode.value == Mode.SetVel;
        this.separateOnGround.visibility = () -> this.mode.value == Mode.SetVel;
        this.horizontalOnGround.visibility = () -> this.mode.value == Mode.SetVel && this.separateOnGround.value;
        this.verticalOnGround.visibility = () -> this.mode.value == Mode.SetVel && this.separateOnGround.value;
        this.resetXZOnGround.visibility = () -> this.mode.value == Mode.SetVel && this.separateOnGround.value;
        this.resetYOnGround.visibility = () -> this.mode.value == Mode.SetVel && this.separateOnGround.value;
        this.addSettings(this.mode, this.horizontal, this.vertical, this.resetXZ, this.resetY, this.separateOnGround, this.horizontalOnGround, this.verticalOnGround, this.resetXZOnGround, this.resetYOnGround, this.nekozoAnni);
    }

    public static enum Mode {
        FullCancel,
        SetVel;

    }
}

