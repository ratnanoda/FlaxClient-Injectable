/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Pos
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 */
package recode.usefultools.latest.Modules.Movement.Fly;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Movement.Fly.Fly_h;

public class Fly
extends BaseModule<Fly_h> {
    public static Fly instance;
    private boolean isDamageActive = false;
    private long triggerFlyStartTime = 0L;
    private double currentSpeed = 0.0;
    private double lastDirX = 0.0;
    private double lastDirZ = 0.0;
    private boolean wasMoving = false;
    private boolean previousDamageState = false;
    private int prevHurtTime = 0;
    private int currentStep = 1;
    private long stepStartTime = 0L;
    private boolean vulcanTestWaiting = false;
    private boolean isVulcanTestFlying = false;
    private long vulcanFlyStartTime = 0L;
    private boolean receivedRespawnTrigger = false;

    public Fly() {
        super(new Fly_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        this.isDamageActive = false;
        this.triggerFlyStartTime = 0L;
        this.currentSpeed = ((Fly_h)this.h).speed.value / 10.0;
        this.lastDirX = 0.0;
        this.lastDirZ = 0.0;
        this.wasMoving = false;
        this.previousDamageState = false;
        this.prevHurtTime = 0;
        this.currentStep = 1;
        this.stepStartTime = System.currentTimeMillis();
        this.isVulcanTestFlying = false;
        this.vulcanFlyStartTime = 0L;
        this.receivedRespawnTrigger = false;
        if (Fly.mc.player == null) {
            return;
        }
        if (((Fly_h)this.h).triggerType.value == Fly_h.TriggerType.VulcanTest) {
            this.vulcanTestWaiting = false;
            if (Fly.mc.player.isDeadOrDying() || Fly.mc.player.getHealth() <= 0.0f) {
                this.receivedRespawnTrigger = true;
            }
        } else {
            this.vulcanTestWaiting = false;
        }
        this.triggerSelfDamage();
        if (((Fly_h)this.h).mode.value == Fly_h.Mode.Old) {
            Fly.mc.player.getAbilities().flying = true;
        }
    }

    @Override
    public void onDisable() {
        this.isDamageActive = false;
        this.triggerFlyStartTime = 0L;
        this.vulcanTestWaiting = false;
        this.isVulcanTestFlying = false;
        this.vulcanFlyStartTime = 0L;
        this.receivedRespawnTrigger = false;
        if (Fly.mc.player != null) {
            Fly.mc.player.getAbilities().flying = false;
            if (((Fly_h)this.h).mode.value != Fly_h.Mode.Old && ((Fly_h)this.h).haltMode.value != Fly_h.HaltMode.None) {
                double targetY = ((Fly_h)this.h).haltY.value ? 0.0 : Fly.mc.player.getDeltaMovement().y;
                Fly.mc.player.setDeltaMovement(0.0, targetY, 0.0);
            }
        }
    }

    public boolean isVulcanTestWaiting() {
        if (!((Fly_h)this.h).enabled || ((Fly_h)this.h).triggerType.value != Fly_h.TriggerType.VulcanTest) {
            return false;
        }
        return this.vulcanTestWaiting;
    }

    public double getCurrentTimerBoostValue() {
        if (!((Fly_h)this.h).enabled || !((Fly_h)this.h).timerBoost.value) {
            return 20.0;
        }
        if (((Fly_h)this.h).mode.value != Fly_h.Mode.SpeedStep) {
            return ((Fly_h)this.h).timerBoostValue.value;
        }
        return switch (this.currentStep) {
            case 2 -> ((Fly_h)this.h).timerStep2.value;
            case 3 -> ((Fly_h)this.h).timerStep3.value;
            case 4 -> ((Fly_h)this.h).timerStep4.value;
            case 5 -> ((Fly_h)this.h).timerStep5.value;
            default -> ((Fly_h)this.h).timerBoostValue.value;
        };
    }

    public double getCurrentNetskipDelay() {
        if (!((Fly_h)this.h).enabled || !((Fly_h)this.h).netskip.value) {
            return 0.0;
        }
        if (((Fly_h)this.h).mode.value != Fly_h.Mode.SpeedStep) {
            return ((Fly_h)this.h).netskipDelay.value;
        }
        return switch (this.currentStep) {
            case 2 -> ((Fly_h)this.h).netskipStep2.value;
            case 3 -> ((Fly_h)this.h).netskipStep3.value;
            case 4 -> ((Fly_h)this.h).netskipStep4.value;
            case 5 -> ((Fly_h)this.h).netskipStep5.value;
            default -> ((Fly_h)this.h).netskipDelay.value;
        };
    }

    private double getCurrentGlideSpeed() {
        if (((Fly_h)this.h).mode.value != Fly_h.Mode.SpeedStep) {
            return ((Fly_h)this.h).glideSpeed.value;
        }
        return switch (this.currentStep) {
            case 2 -> ((Fly_h)this.h).glideStep2.value;
            case 3 -> ((Fly_h)this.h).glideStep3.value;
            case 4 -> ((Fly_h)this.h).glideStep4.value;
            case 5 -> ((Fly_h)this.h).glideStep5.value;
            default -> ((Fly_h)this.h).glideSpeed.value;
        };
    }

    @Override
    public void onUpdate() {
        long targetDuration;
        long stepElapsed;
        int totalSteps;
        long elapsed;
        boolean isMovingInput;
        if (Fly.mc.player == null || Fly.mc.level == null) {
            return;
        }
        if (Fly.mc.player.isDeadOrDying() || Fly.mc.player.getHealth() <= 0.0f) {
            this.triggerFlyStartTime = System.currentTimeMillis();
            this.vulcanFlyStartTime = System.currentTimeMillis();
            this.stepStartTime = System.currentTimeMillis();
            if (((Fly_h)this.h).triggerType.value == Fly_h.TriggerType.VulcanTest) {
                this.receivedRespawnTrigger = true;
                this.vulcanTestWaiting = false;
                this.isVulcanTestFlying = false;
            }
            this.prevHurtTime = Fly.mc.player.hurtTime;
            return;
        }
        if (((Fly_h)this.h).mode.value == Fly_h.Mode.Old) {
            Fly.mc.player.getAbilities().flying = true;
            this.prevHurtTime = Fly.mc.player.hurtTime;
            return;
        }
        boolean bl = isMovingInput = Fly.mc.options.keyUp.isDown() || Fly.mc.options.keyDown.isDown() || Fly.mc.options.keyLeft.isDown() || Fly.mc.options.keyRight.isDown();
        if (((Fly_h)this.h).triggerType.value == Fly_h.TriggerType.VulcanTest) {
            if (this.receivedRespawnTrigger) {
                this.vulcanTestWaiting = true;
                this.isDamageActive = true;
                this.receivedRespawnTrigger = false;
            }
            if (this.vulcanTestWaiting && isMovingInput) {
                this.vulcanTestWaiting = false;
                this.isVulcanTestFlying = true;
                this.vulcanFlyStartTime = System.currentTimeMillis();
            }
        }
        boolean isNewDamageInstance = false;
        if (((Fly_h)this.h).triggerType.value == Fly_h.TriggerType.KnockBack) {
            if (Fly.mc.player.hurtTime > this.prevHurtTime && Fly.mc.player.hurtTime > 0) {
                isNewDamageInstance = true;
            }
            if (Fly.mc.player.hurtMarked) {
                isNewDamageInstance = true;
            }
        } else if (((Fly_h)this.h).triggerType.value == Fly_h.TriggerType.Damage && Fly.mc.player.hurtTime > this.prevHurtTime && Fly.mc.player.hurtTime > 0) {
            isNewDamageInstance = true;
        }
        if (isNewDamageInstance) {
            this.isDamageActive = true;
            this.triggerFlyStartTime = System.currentTimeMillis();
            this.currentStep = 1;
            this.stepStartTime = System.currentTimeMillis();
            this.currentSpeed = ((Fly_h)this.h).speed.value / 10.0;
        }
        if (((Fly_h)this.h).triggerType.value == Fly_h.TriggerType.VulcanTest) {
            if (this.vulcanTestWaiting) {
                this.isDamageActive = true;
            } else if (this.isVulcanTestFlying) {
                long maxFlyTimeMs;
                elapsed = System.currentTimeMillis() - this.vulcanFlyStartTime;
                if (elapsed > (maxFlyTimeMs = (long)(((Fly_h)this.h).vulcanFlyTime.value * 1000.0))) {
                    this.isDamageActive = false;
                    this.isVulcanTestFlying = false;
                } else {
                    this.isDamageActive = true;
                }
            } else {
                this.isDamageActive = false;
            }
        } else if (((Fly_h)this.h).triggerType.value != Fly_h.TriggerType.None) {
            if (this.isDamageActive && (elapsed = System.currentTimeMillis() - this.triggerFlyStartTime) > (long)((Fly_h)this.h).triggerFlyTime.value) {
                this.isDamageActive = false;
            }
        } else {
            this.isDamageActive = true;
        }
        if (((Fly_h)this.h).triggerType.value != Fly_h.TriggerType.None && ((Fly_h)this.h).triggerType.value != Fly_h.TriggerType.VulcanTest && ((Fly_h)this.h).haltMode.value == Fly_h.HaltMode.AndDamage && this.previousDamageState && !this.isDamageActive) {
            double targetY = ((Fly_h)this.h).haltY.value ? 0.0 : Fly.mc.player.getDeltaMovement().y;
            Fly.mc.player.setDeltaMovement(0.0, targetY, 0.0);
        }
        this.previousDamageState = this.isDamageActive;
        if (!this.isDamageActive) {
            this.prevHurtTime = Fly.mc.player.hurtTime;
            return;
        }
        if (((Fly_h)this.h).mode.value == Fly_h.Mode.SpeedStep && this.currentStep < (totalSteps = (int)((Fly_h)this.h).settingValue.value) && (stepElapsed = System.currentTimeMillis() - this.stepStartTime) >= (targetDuration = this.getSpeedTimeForStep(this.currentStep))) {
            ++this.currentStep;
            this.stepStartTime = System.currentTimeMillis();
            if (((Fly_h)this.h).frictionReset.value) {
                this.currentSpeed = this.getSpeedForStep(this.currentStep);
            }
        }
        double activeFriction = this.getCurrentFrictionValue();
        if (((Fly_h)this.h).mode.value == Fly_h.Mode.Jump) {
            this.currentSpeed = Fly.mc.player.onGround() ? ((Fly_h)this.h).speed.value / 10.0 : (this.currentSpeed *= activeFriction);
        } else if (!isNewDamageInstance) {
            this.currentSpeed *= activeFriction;
        }
        this.prevHurtTime = Fly.mc.player.hurtTime;
    }

    private double getCurrentFrictionValue() {
        if (((Fly_h)this.h).mode.value != Fly_h.Mode.SpeedStep) {
            return ((Fly_h)this.h).friction.value;
        }
        return switch (this.currentStep) {
            case 2 -> ((Fly_h)this.h).frictionStep2.value;
            case 3 -> ((Fly_h)this.h).frictionStep3.value;
            case 4 -> ((Fly_h)this.h).frictionStep4.value;
            case 5 -> ((Fly_h)this.h).frictionStep5.value;
            default -> ((Fly_h)this.h).friction.value;
        };
    }

    private long getSpeedTimeForStep(int step) {
        return (long)(switch (step) {
            case 2 -> ((Fly_h)this.h).speedTime2.value;
            case 3 -> ((Fly_h)this.h).speedTime3.value;
            case 4 -> ((Fly_h)this.h).speedTime4.value;
            case 5 -> ((Fly_h)this.h).speedTime5.value;
            default -> ((Fly_h)this.h).speedTime.value;
        });
    }

    private double getSpeedForStep(int step) {
        double rawVal = switch (step) {
            case 2 -> ((Fly_h)this.h).speedStep2.value;
            case 3 -> ((Fly_h)this.h).speedStep3.value;
            case 4 -> ((Fly_h)this.h).speedStep4.value;
            case 5 -> ((Fly_h)this.h).speedStep5.value;
            default -> ((Fly_h)this.h).speed.value;
        };
        return rawVal / 10.0;
    }

    private double getVerticalSpeedForStep(int step) {
        double rawVal = switch (step) {
            case 2 -> ((Fly_h)this.h).verticalStep2.value;
            case 3 -> ((Fly_h)this.h).verticalStep3.value;
            case 4 -> ((Fly_h)this.h).verticalStep4.value;
            case 5 -> ((Fly_h)this.h).verticalStep5.value;
            default -> ((Fly_h)this.h).verticalSpeed.value;
        };
        return rawVal / 10.0;
    }

    public void onTravel(LocalPlayer player) {
        double activeSpeed;
        if (player == null || !this.isDamageActive) {
            return;
        }
        float forward = 0.0f;
        float strafe = 0.0f;
        if (Fly.mc.options.keyUp.isDown()) {
            forward += 1.0f;
        }
        if (Fly.mc.options.keyDown.isDown()) {
            forward -= 1.0f;
        }
        if (Fly.mc.options.keyLeft.isDown()) {
            strafe += 1.0f;
        }
        if (Fly.mc.options.keyRight.isDown()) {
            strafe -= 1.0f;
        }
        boolean isMovingInput = forward != 0.0f || strafe != 0.0f;
        double length = Math.sqrt(forward * forward + strafe * strafe);
        double normF = length > 0.0 ? (double)forward / length : 0.0;
        double normS = length > 0.0 ? (double)strafe / length : 0.0;
        float yaw = Fly.mc.player.getYRot();
        double rad = Math.toRadians(yaw);
        double mx = 0.0;
        double mz = 0.0;
        double d = activeSpeed = ((Fly_h)this.h).mode.value == Fly_h.Mode.SpeedStep ? this.getSpeedForStep(this.currentStep) : this.currentSpeed;
        if (isMovingInput) {
            if (forward != 0.0f) {
                mx += -Math.sin(rad) * normF * activeSpeed;
                mz += Math.cos(rad) * normF * activeSpeed;
            }
            if (strafe != 0.0f) {
                mx += Math.cos(rad) * normS * activeSpeed;
                mz += Math.sin(rad) * normS * activeSpeed;
            }
            this.lastDirX = mx / activeSpeed;
            this.lastDirZ = mz / activeSpeed;
            this.wasMoving = true;
        } else if (((Fly_h)this.h).mode.value == Fly_h.Mode.Jump) {
            mx = this.lastDirX * activeSpeed;
            mz = this.lastDirZ * activeSpeed;
        } else {
            mx = 0.0;
            mz = 0.0;
        }
        double vSpeed = ((Fly_h)this.h).mode.value == Fly_h.Mode.SpeedStep ? (((Fly_h)this.h).speedMode.value == Fly_h.SpeedMode.Separation ? this.getVerticalSpeedForStep(this.currentStep) : this.getSpeedForStep(this.currentStep)) : (((Fly_h)this.h).speedMode.value == Fly_h.SpeedMode.Separation ? ((Fly_h)this.h).verticalSpeed.value / 10.0 : ((Fly_h)this.h).speed.value / 10.0);
        double my = 0.0;
        double activeGlideSpeed = this.getCurrentGlideSpeed();
        my = Fly.mc.options.keyJump.isDown() ? vSpeed : (Fly.mc.options.keyShift.isDown() ? -vSpeed : -activeGlideSpeed);
        if (!isMovingInput) {
            if (((Fly_h)this.h).fastStopMode.value == Fly_h.FastStopMode.Always) {
                mx = 0.0;
                mz = 0.0;
                if (((Fly_h)this.h).fastStopY.value) {
                    my = 0.0;
                }
            } else if (((Fly_h)this.h).fastStopMode.value == Fly_h.FastStopMode.Normal && this.wasMoving) {
                mx = 0.0;
                mz = 0.0;
                if (((Fly_h)this.h).fastStopY.value) {
                    my = 0.0;
                }
                this.wasMoving = false;
            }
        }
        if (((Fly_h)this.h).mode.value == Fly_h.Mode.Motion || ((Fly_h)this.h).mode.value == Fly_h.Mode.SpeedStep) {
            Fly.mc.player.setDeltaMovement(mx, my, mz);
        } else if (((Fly_h)this.h).mode.value == Fly_h.Mode.Jump) {
            my = Fly.mc.player.onGround() ? 0.42 : (Fly.mc.options.keyJump.isDown() ? vSpeed : (Fly.mc.options.keyShift.isDown() ? -vSpeed : -activeGlideSpeed));
            Fly.mc.player.setDeltaMovement(mx, my, mz);
        }
    }

    private void triggerSelfDamage() {
        if (Fly.mc.player == null) {
            return;
        }
        double x = Fly.mc.player.getX();
        double y = Fly.mc.player.getY();
        double z = Fly.mc.player.getZ();
        if (((Fly_h)this.h).selfDamageMode.value == Fly_h.SelfDamageMode.Fall) {
            Fly.mc.player.connection.send((Packet)new ServerboundMovePlayerPacket.Pos(x, y + 3.05, z, false, Fly.mc.player.horizontalCollision));
            Fly.mc.player.connection.send((Packet)new ServerboundMovePlayerPacket.Pos(x, y, z, true, Fly.mc.player.horizontalCollision));
        } else if (((Fly_h)this.h).selfDamageMode.value == Fly_h.SelfDamageMode.SelfEntityAttack && Fly.mc.gameMode != null) {
            Fly.mc.gameMode.attack((Player)Fly.mc.player, (Entity)Fly.mc.player);
        }
    }
}

