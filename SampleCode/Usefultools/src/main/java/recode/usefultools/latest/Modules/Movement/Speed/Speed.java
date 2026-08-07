/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  imgui.ImDrawList
 *  imgui.ImGui
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 */
package recode.usefultools.latest.Modules.Movement.Speed;

import imgui.ImDrawList;
import imgui.ImGui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Movement.Speed.Speed_h;

public class Speed
extends BaseModule<Speed_h> {
    public static Speed instance;
    private int airTicks = 0;
    private int offGroundTicks = 0;
    private int groundCount = 1;
    private int groundTicks = 0;
    private int groundSpoofTimer = 0;
    private double currentSpeed = 0.0;
    private boolean wasOnGround = true;
    private double lastAirBPS = 0.0;
    private boolean useSecondSpeed = false;
    private double lastMovementYaw = 0.0;
    private double lastX = 0.0;
    private double lastZ = 0.0;
    private boolean hasFastFalledThisJump = false;
    private int fastFallCount = 0;

    public Speed() {
        super(new Speed_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        this.airTicks = 0;
        this.offGroundTicks = 0;
        this.groundTicks = 0;
        this.groundCount = 1;
        this.groundSpoofTimer = 0;
        this.currentSpeed = ((Speed_h)this.h).speed.value;
        this.wasOnGround = true;
        this.lastAirBPS = 0.0;
        this.useSecondSpeed = false;
        this.lastMovementYaw = 0.0;
        this.hasFastFalledThisJump = false;
        this.fastFallCount = 0;
        if (Speed.mc.player != null) {
            this.lastX = Speed.mc.player.getX();
            this.lastZ = Speed.mc.player.getZ();
        }
    }

    @Override
    public void onDisable() {
        this.airTicks = 0;
        this.offGroundTicks = 0;
        this.groundTicks = 0;
        this.groundCount = 1;
        this.groundSpoofTimer = 0;
        this.wasOnGround = true;
        this.lastAirBPS = 0.0;
        this.useSecondSpeed = false;
        this.lastMovementYaw = 0.0;
        this.hasFastFalledThisJump = false;
        this.fastFallCount = 0;
    }

    private double getBPS() {
        if (Speed.mc.player == null) {
            return 0.0;
        }
        double dx = Speed.mc.player.getX() - this.lastX;
        double dz = Speed.mc.player.getZ() - this.lastZ;
        return Math.sqrt(dx * dx + dz * dz) * 20.0;
    }

    @Override
    public void onUpdate() {
        boolean isMoving;
        if (Speed.mc.player == null || Speed.mc.level == null) {
            return;
        }
        double bps = this.getBPS();
        this.lastX = Speed.mc.player.getX();
        this.lastZ = Speed.mc.player.getZ();
        boolean onGround = Speed.mc.player.onGround();
        if (onGround) {
            this.airTicks = 0;
            this.offGroundTicks = 0;
            ++this.groundTicks;
            this.hasFastFalledThisJump = false;
            if (!this.wasOnGround) {
                ++this.groundCount;
                if (((Speed_h)this.h).speedStep.value == Speed_h.SpeedStep.GroundCount) {
                    this.useSecondSpeed = this.groundCount > 1;
                } else if (((Speed_h)this.h).speedStep.value == Speed_h.SpeedStep.LastSpeed) {
                    this.useSecondSpeed = this.lastAirBPS >= ((Speed_h)this.h).transitionBPS.value;
                } else if (((Speed_h)this.h).speedStep.value == Speed_h.SpeedStep.CollideCS || ((Speed_h)this.h).speedStep.value == Speed_h.SpeedStep.Vulcan) {
                    this.useSecondSpeed = true;
                }
            } else {
                if (((Speed_h)this.h).speedStep.value == Speed_h.SpeedStep.GroundCount && this.groundCount > 1) {
                    this.useSecondSpeed = true;
                }
                if (this.groundTicks > 4) {
                    this.groundCount = 1;
                    this.useSecondSpeed = false;
                }
            }
            if ((((Speed_h)this.h).speedStep.value == Speed_h.SpeedStep.CollideCS || ((Speed_h)this.h).speedStep.value == Speed_h.SpeedStep.Vulcan) && this.useSecondSpeed && bps <= ((Speed_h)this.h).collideBPS.value) {
                this.useSecondSpeed = false;
                this.groundCount = 1;
            }
            this.currentSpeed = !this.wasOnGround && ((Speed_h)this.h).oneTickBoost.value ? ((Speed_h)this.h).oneTickBoostSpeed.value : (((Speed_h)this.h).speedStep.value != Speed_h.SpeedStep.None ? (this.useSecondSpeed ? ((Speed_h)this.h).secondSpeed.value : ((Speed_h)this.h).firstSpeed.value) : ((Speed_h)this.h).speed.value);
            this.wasOnGround = true;
        } else {
            ++this.airTicks;
            ++this.offGroundTicks;
            this.groundTicks = 0;
            this.wasOnGround = false;
            if (this.airTicks > 1) {
                this.lastAirBPS = bps;
            }
            this.currentSpeed *= ((Speed_h)this.h).frictionFactor.value;
            if (((Speed_h)this.h).speedStep.value == Speed_h.SpeedStep.CollideCS || ((Speed_h)this.h).speedStep.value == Speed_h.SpeedStep.Vulcan) {
                this.useSecondSpeed = true;
            }
        }
        boolean bl = isMoving = Speed.mc.options.keyUp.isDown() || Speed.mc.options.keyDown.isDown() || Speed.mc.options.keyLeft.isDown() || Speed.mc.options.keyRight.isDown();
        if (((Speed_h)this.h).autoJump.value && Speed.mc.player.onGround() && isMoving) {
            Speed.mc.player.jumpFromGround();
        }
        this.handleFastFall();
    }

    public boolean getSpoofedOnGround(boolean realOnGround) {
        if (((Speed_h)this.h).mode.value != Speed_h.Mode.Custom) {
            return realOnGround;
        }
        switch ((Speed_h.OnGroundMode)((Object)((Speed_h)this.h).onGroundMode.value)) {
            case Always: {
                return true;
            }
            case ReverseAlways: {
                return !realOnGround;
            }
            case Test: {
                int delay = (int)((Speed_h)this.h).onGroundDelay.value;
                int duration = (int)((Speed_h)this.h).onGroundTime.value;
                return this.groundSpoofTimer >= delay && this.groundSpoofTimer < delay + duration;
            }
        }
        return realOnGround;
    }

    private void handleFastFall() {
        int activeTicks1;
        if (((Speed_h)this.h).fastFallMode.value == Speed_h.FastFallMode.None || Speed.mc.player == null) {
            return;
        }
        boolean shouldFastFall = false;
        boolean isVulcanActive = ((Speed_h)this.h).vulcanFastFall.value && this.fastFallCount >= (int)((Speed_h)this.h).vulcanLimit.value;
        double targetVelocity = isVulcanActive ? ((Speed_h)this.h).vulcanVelocity.value : ((Speed_h)this.h).fastFallVelocity.value;
        int n = activeTicks1 = isVulcanActive ? (int)((Speed_h)this.h).vulcanTicks.value : (int)((Speed_h)this.h).fastFallTicks.value;
        if (((Speed_h)this.h).fastFallMode.value == Speed_h.FastFallMode.All) {
            shouldFastFall = true;
        } else if (((Speed_h)this.h).fastFallMode.value == Speed_h.FastFallMode.OneTick) {
            if (this.airTicks == activeTicks1) {
                shouldFastFall = true;
                if (isVulcanActive) {
                    this.fastFallCount = 0;
                }
            }
            if (((Speed_h)this.h).fastFall2.value && this.airTicks == (int)((Speed_h)this.h).fastFallTicks2.value) {
                shouldFastFall = true;
                targetVelocity = ((Speed_h)this.h).fastFallVelocity2.value;
                if (isVulcanActive) {
                    this.fastFallCount = 0;
                }
            }
        }
        if (shouldFastFall && !Speed.mc.player.onGround()) {
            Vec3 vel = Speed.mc.player.getDeltaMovement();
            Speed.mc.player.setDeltaMovement(vel.x, -targetVelocity, vel.z);
            if (!this.hasFastFalledThisJump) {
                ++this.fastFallCount;
                this.hasFastFalledThisJump = true;
            }
        }
    }

    public void onTravel(LocalPlayer player) {
        boolean isMoving;
        if (player == null) {
            return;
        }
        boolean bl = isMoving = Speed.mc.options.keyUp.isDown() || Speed.mc.options.keyDown.isDown() || Speed.mc.options.keyLeft.isDown() || Speed.mc.options.keyRight.isDown();
        if (((Speed_h)this.h).mode.value == Speed_h.Mode.Friction) {
            if (!player.onGround() && isMoving) {
                this.applyStrafeDirection(player, this.currentSpeed);
            }
        } else if (((Speed_h)this.h).mode.value == Speed_h.Mode.Custom) {
            if (((Speed_h)this.h).strafeMode.value == Speed_h.StrafeModeOption.Strafe) {
                if (isMoving) {
                    boolean withinStrafeTicks;
                    boolean withinVelocityTicks = ((Speed_h)this.h).strafeTicksMode.value == Speed_h.StrafeTicksMode.StrafeTicks && ((Speed_h)this.h).velocityTicks.value >= 0.0 && this.offGroundTicks <= (int)((Speed_h)this.h).velocityTicks.value || ((Speed_h)this.h).strafeTicksMode.value == Speed_h.StrafeTicksMode.Always;
                    boolean bl2 = withinStrafeTicks = ((Speed_h)this.h).strafeTicksMode.value == Speed_h.StrafeTicksMode.StrafeTicks && ((Speed_h)this.h).strafeTicks.value >= 0.0 && this.offGroundTicks <= (int)((Speed_h)this.h).strafeTicks.value || ((Speed_h)this.h).strafeTicksMode.value == Speed_h.StrafeTicksMode.Always;
                    if (withinVelocityTicks) {
                        double activeSpeed;
                        double d = activeSpeed = ((Speed_h)this.h).speedStep.value != Speed_h.SpeedStep.None ? this.currentSpeed : ((Speed_h)this.h).speed.value;
                        double activeStrafeSpeed = ((Speed_h)this.h).speedStep.value != Speed_h.SpeedStep.None ? (this.useSecondSpeed ? ((Speed_h)this.h).secondStrafeSpeed.value : ((Speed_h)this.h).firstStrafeSpeed.value) : ((Speed_h)this.h).strafeSpeed.value;
                        if (((Speed_h)this.h).speedStep.value == Speed_h.SpeedStep.Vulcan) {
                            activeSpeed = this.applyVulcanDecay(activeSpeed);
                            activeStrafeSpeed = this.applyVulcanDecay(activeStrafeSpeed);
                        }
                        if (((Speed_h)this.h).strafeSpeedMode.value == Speed_h.StrafeSpeedMode.Separation) {
                            this.applySeparationStrafe(player, activeSpeed, activeStrafeSpeed);
                        } else {
                            this.applyStrafeDirection(player, activeSpeed);
                        }
                    } else if (withinStrafeTicks) {
                        Vec3 currentVel = player.getDeltaMovement();
                        double vanillaSpeed = Math.sqrt(currentVel.x * currentVel.x + currentVel.z * currentVel.z);
                        if (((Speed_h)this.h).speedStep.value == Speed_h.SpeedStep.Vulcan) {
                            vanillaSpeed = this.applyVulcanDecay(vanillaSpeed);
                        }
                        this.applyStrafeDirection(player, vanillaSpeed);
                    }
                }
            } else if (((Speed_h)this.h).strafeMode.value == Speed_h.StrafeModeOption.FullMotion && isMoving) {
                boolean withinVelocityTicks;
                boolean bl3 = withinVelocityTicks = ((Speed_h)this.h).strafeTicksMode.value == Speed_h.StrafeTicksMode.StrafeTicks && ((Speed_h)this.h).velocityTicks.value >= 0.0 && this.offGroundTicks <= (int)((Speed_h)this.h).velocityTicks.value || ((Speed_h)this.h).strafeTicksMode.value == Speed_h.StrafeTicksMode.Always;
                if (withinVelocityTicks) {
                    double activeSpeed;
                    double d = activeSpeed = ((Speed_h)this.h).speedStep.value != Speed_h.SpeedStep.None ? this.currentSpeed : ((Speed_h)this.h).speed.value;
                    if (((Speed_h)this.h).speedStep.value == Speed_h.SpeedStep.Vulcan) {
                        activeSpeed = this.applyVulcanDecay(activeSpeed);
                    }
                    this.applyStrafeDirection(player, activeSpeed);
                }
            }
        }
    }

    private double applyVulcanDecay(double baseSpeed) {
        float forward = 0.0f;
        float strafe = 0.0f;
        if (Speed.mc.options.keyUp.isDown()) {
            forward += 1.0f;
        }
        if (Speed.mc.options.keyDown.isDown()) {
            forward -= 1.0f;
        }
        if (Speed.mc.options.keyLeft.isDown()) {
            strafe += 1.0f;
        }
        if (Speed.mc.options.keyRight.isDown()) {
            strafe -= 1.0f;
        }
        float yaw = Speed.mc.player.getYRot();
        double moveAngle = Math.atan2(-strafe, forward);
        double currentMovementYaw = Mth.wrapDegrees((double)((double)yaw + Math.toDegrees(moveAngle)));
        double angleDiff = Math.abs(Mth.wrapDegrees((double)(currentMovementYaw - this.lastMovementYaw)));
        this.lastMovementYaw = currentMovementYaw;
        double finalSpeed = baseSpeed;
        if (((Speed_h)this.h).decayMethod.value == Speed_h.DecayMethod.FixedValue) {
            int turnSteps = (int)(angleDiff / 90.0);
            if (turnSteps > 0) {
                double subtraction = (double)turnSteps * ((Speed_h)this.h).fixValue.value * (((Speed_h)this.h).maxStrafeAngle.value / 0.9);
                finalSpeed = Math.max(0.0, finalSpeed - subtraction);
            }
        } else if (((Speed_h)this.h).decayMethod.value == Speed_h.DecayMethod.LossPercent && angleDiff >= ((Speed_h)this.h).fixValue.value) {
            finalSpeed *= 1.0 - ((Speed_h)this.h).maxStrafeAngle.value;
        }
        return finalSpeed;
    }

    private void applyStrafeDirection(LocalPlayer player, double speedVal) {
        float forward = 0.0f;
        float strafe = 0.0f;
        if (Speed.mc.options.keyUp.isDown()) {
            forward += 1.0f;
        }
        if (Speed.mc.options.keyDown.isDown()) {
            forward -= 1.0f;
        }
        if (Speed.mc.options.keyLeft.isDown()) {
            strafe += 1.0f;
        }
        if (Speed.mc.options.keyRight.isDown()) {
            strafe -= 1.0f;
        }
        float yaw = player.getYRot();
        double moveAngle = Math.atan2(-strafe, forward);
        double finalRad = Math.toRadians(yaw) + moveAngle;
        double mx = -Math.sin(finalRad) * speedVal;
        double mz = Math.cos(finalRad) * speedVal;
        player.setDeltaMovement(mx, player.getDeltaMovement().y, mz);
    }

    private void applySeparationStrafe(LocalPlayer player, double speedVal, double strafeSpeedVal) {
        float forward = 0.0f;
        float strafe = 0.0f;
        if (Speed.mc.options.keyUp.isDown()) {
            forward += 1.0f;
        }
        if (Speed.mc.options.keyDown.isDown()) {
            forward -= 1.0f;
        }
        if (Speed.mc.options.keyLeft.isDown()) {
            strafe += 1.0f;
        }
        if (Speed.mc.options.keyRight.isDown()) {
            strafe -= 1.0f;
        }
        float yaw = player.getYRot();
        double rad = Math.toRadians(yaw);
        double length = Math.sqrt(forward * forward + strafe * strafe);
        double normForward = (double)forward / length;
        double normStrafe = (double)strafe / length;
        double mx = 0.0;
        double mz = 0.0;
        if (forward != 0.0f) {
            mx += -Math.sin(rad) * normForward * speedVal;
            mz += Math.cos(rad) * normForward * speedVal;
        }
        if (strafe != 0.0f) {
            mx += Math.cos(rad) * normStrafe * strafeSpeedVal;
            mz += Math.sin(rad) * normStrafe * strafeSpeedVal;
        }
        player.setDeltaMovement(mx, player.getDeltaMovement().y, mz);
    }

    @Override
    public void onRenderHUD() {
        if (!((Speed_h)this.h).enabled || !((Speed_h)this.h).debugLog.value) {
            return;
        }
        ImDrawList dl = ImGui.getForegroundDrawList();
        float sh = ImGui.getIO().getDisplaySizeY();
        String text = String.format("Speed (BPS): %.2f", this.getBPS());
        float posX = 10.0f;
        float posY = sh * 0.4f;
        dl.addText(posX + 1.0f, posY + 1.0f, ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.85f), text);
        dl.addText(posX, posY, ImGui.getColorU32(0.0f, 1.0f, 1.0f, 1.0f), text);
    }
}

