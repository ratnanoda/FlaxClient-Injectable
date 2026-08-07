/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  imgui.ImGui
 *  net.minecraft.util.Mth
 */
package recode.usefultools.latest.Modules.Misc.ServerRotation;

import imgui.ImGui;
import net.minecraft.util.Mth;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.RotationManager.RotationManager;
import recode.usefultools.latest.Modules.Misc.ServerRotation.ServerRotation_h;
import recode.usefultools.latest.utils.MathUtils;

public class ServerRotation
extends BaseModule<ServerRotation_h> {
    public static ServerRotation instance;
    public float yaw;
    public float pitch;
    public float bodyYaw;
    public float prevYaw;
    public float prevPitch;
    public float prevBodyYaw;

    public ServerRotation() {
        super(new ServerRotation_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        if (ServerRotation.mc.player != null) {
            this.yaw = this.prevYaw = ServerRotation.mc.player.getYRot();
            this.pitch = this.prevPitch = ServerRotation.mc.player.getXRot();
            this.bodyYaw = this.prevBodyYaw = ServerRotation.mc.player.yBodyRot;
        }
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onUpdate() {
        if (ServerRotation.mc.player == null) {
            return;
        }
        if (((ServerRotation_h)this.h).speedMode.value == ServerRotation_h.Dependency.Tick) {
            this.prevYaw = this.yaw;
            this.prevPitch = this.pitch;
            this.prevBodyYaw = this.bodyYaw;
            this.getRotations(1.0f, false);
        }
    }

    public float[] getRotations(float tickDelta, boolean isFrame) {
        float bodyFactor;
        boolean lerpEnabled;
        if (ServerRotation.mc.player == null) {
            return new float[]{0.0f, 0.0f, 0.0f};
        }
        boolean isRotating = RotationManager.instance != null && RotationManager.instance.rotating;
        float targetYaw = isRotating ? RotationManager.instance.serverYaw : ServerRotation.mc.player.getYRot();
        float targetPitch = isRotating ? RotationManager.instance.serverPitch : ServerRotation.mc.player.getXRot();
        float rYaw = this.yaw;
        float rPitch = this.pitch;
        boolean bl = lerpEnabled = ((ServerRotation_h)this.h).lerpMode.value == ServerRotation_h.LerpMode.Normal || ((ServerRotation_h)this.h).lerpMode.value == ServerRotation_h.LerpMode.DirectLerp;
        if (isRotating || ((ServerRotation_h)this.h).smoothCamera.value) {
            if (((ServerRotation_h)this.h).speedMode.value == ServerRotation_h.Dependency.FPS && isFrame) {
                if (lerpEnabled) {
                    float dt = ImGui.getIO().getDeltaTime();
                    float factor = (float)(1.0 - Math.pow(0.001, (double)dt * ((ServerRotation_h)this.h).lerpSpeed.value));
                    rYaw = this.interpolateAngle(this.yaw, targetYaw, factor);
                    rPitch = MathUtils.lerp(this.pitch, targetPitch, factor);
                } else {
                    rYaw = targetYaw;
                    rPitch = targetPitch;
                }
                this.yaw = Mth.wrapDegrees((float)rYaw);
                this.pitch = Mth.wrapDegrees((float)rPitch);
            } else if (((ServerRotation_h)this.h).speedMode.value == ServerRotation_h.Dependency.Tick && !isFrame) {
                if (lerpEnabled) {
                    float factor = (float)((ServerRotation_h)this.h).lerpSpeed.value * 0.05f;
                    rYaw = this.interpolateAngle(this.yaw, targetYaw, factor);
                    rPitch = MathUtils.lerp(this.pitch, targetPitch, factor);
                } else {
                    rYaw = targetYaw;
                    rPitch = targetPitch;
                }
                this.yaw = Mth.wrapDegrees((float)rYaw);
                this.pitch = Mth.wrapDegrees((float)rPitch);
            }
        } else {
            this.yaw = Mth.wrapDegrees((float)targetYaw);
            this.pitch = Mth.wrapDegrees((float)targetPitch);
            this.bodyYaw = ServerRotation.mc.player.yBodyRot;
            return new float[]{this.yaw, this.pitch, this.bodyYaw};
        }
        if (isRotating) {
            float bodyFactor2;
            float currentHeadYaw = isFrame ? rYaw : this.yaw;
            float targetBodyYaw = switch ((ServerRotation_h.BodyMode)((Object)((ServerRotation_h)this.h).bodyMode.value)) {
                case ServerRotation_h.BodyMode.Sync -> currentHeadYaw;
                case ServerRotation_h.BodyMode.Static -> ServerRotation.mc.player.yBodyRot;
                case ServerRotation_h.BodyMode.Threshold -> {
                    float limit = (float)((ServerRotation_h)this.h).threshold.value;
                    float diff = Mth.wrapDegrees((float)(currentHeadYaw - this.bodyYaw));
                    if (Math.abs(diff) > limit) {
                        yield Mth.wrapDegrees((float)(currentHeadYaw - Math.copySign(limit, diff)));
                    }
                    yield this.bodyYaw;
                }
                default -> currentHeadYaw;
            };
            if (((ServerRotation_h)this.h).speedMode.value == ServerRotation_h.Dependency.FPS && isFrame) {
                float dt = ImGui.getIO().getDeltaTime();
                bodyFactor2 = (float)(1.0 - Math.pow(0.001, (double)dt * ((ServerRotation_h)this.h).lerpSpeed.value));
                this.bodyYaw = this.interpolateAngle(this.bodyYaw, targetBodyYaw, bodyFactor2);
            } else if (((ServerRotation_h)this.h).speedMode.value == ServerRotation_h.Dependency.Tick && !isFrame) {
                bodyFactor2 = (float)((ServerRotation_h)this.h).lerpSpeed.value * 0.05f;
                this.bodyYaw = this.interpolateAngle(this.bodyYaw, targetBodyYaw, bodyFactor2);
            }
        } else if (((ServerRotation_h)this.h).speedMode.value == ServerRotation_h.Dependency.FPS && isFrame) {
            float dt = ImGui.getIO().getDeltaTime();
            bodyFactor = (float)(1.0 - Math.pow(0.001, (double)dt * ((ServerRotation_h)this.h).lerpSpeed.value));
            this.bodyYaw = this.interpolateAngle(this.bodyYaw, ServerRotation.mc.player.yBodyRot, bodyFactor);
        } else if (((ServerRotation_h)this.h).speedMode.value == ServerRotation_h.Dependency.Tick && !isFrame) {
            bodyFactor = (float)((ServerRotation_h)this.h).lerpSpeed.value * 0.05f;
            this.bodyYaw = this.interpolateAngle(this.bodyYaw, ServerRotation.mc.player.yBodyRot, bodyFactor);
        } else {
            this.bodyYaw = ServerRotation.mc.player.yBodyRot;
        }
        if (isFrame) {
            if (((ServerRotation_h)this.h).speedMode.value == ServerRotation_h.Dependency.Tick) {
                if (((ServerRotation_h)this.h).lerpMode.value == ServerRotation_h.LerpMode.None || ((ServerRotation_h)this.h).lerpMode.value == ServerRotation_h.LerpMode.Direct) {
                    return new float[]{targetYaw, targetPitch, this.bodyYaw};
                }
                float renderedYaw = Mth.rotLerp((float)tickDelta, (float)this.prevYaw, (float)this.yaw);
                float renderedPitch = MathUtils.lerp(this.prevPitch, this.pitch, tickDelta);
                float renderedBody = Mth.rotLerp((float)tickDelta, (float)this.prevBodyYaw, (float)this.bodyYaw);
                return new float[]{renderedYaw, renderedPitch, renderedBody};
            }
            return new float[]{this.yaw, this.pitch, this.bodyYaw};
        }
        return new float[]{this.yaw, this.pitch, this.bodyYaw};
    }

    public float interpolateAngle(float current, float target, float factor) {
        if (((ServerRotation_h)this.h).fixWinding.value) {
            float diff = Mth.wrapDegrees((float)(target - current));
            float result = current + diff * factor;
            return Mth.wrapDegrees((float)result);
        }
        return current + (target - current) * factor;
    }
}

