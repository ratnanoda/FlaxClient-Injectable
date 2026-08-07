/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 */
package recode.usefultools.latest.Modules.Movement.Sprint;

import net.minecraft.util.Mth;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.RotationManager.RotationManager;
import recode.usefultools.latest.Modules.Movement.Sprint.Sprint_h;

public class Sprint
extends BaseModule<Sprint_h> {
    public Sprint() {
        super(new Sprint_h());
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        if (Sprint.mc.player != null) {
            if (((Sprint_h)this.h).mode.value == Sprint_h.Mode.Legit) {
                Sprint.mc.options.keySprint.setDown(false);
            } else {
                Sprint.mc.player.setSprinting(false);
            }
        }
    }

    @Override
    public void onUpdate() {
        float moveYaw;
        float lookYaw;
        float angleDiff;
        if (Sprint.mc.player == null || Sprint.mc.level == null) {
            return;
        }
        boolean shouldCancelSprint = false;
        if (((Sprint_h)this.h).sprintCancel.value && (double)(angleDiff = Math.abs(Mth.wrapDegrees((float)((lookYaw = RotationManager.instance != null && RotationManager.instance.rotating ? RotationManager.instance.serverYaw : Sprint.mc.player.getYRot()) - (moveYaw = this.getMoveYaw()))))) > ((Sprint_h)this.h).cancelAngle.value) {
            shouldCancelSprint = true;
        }
        if (shouldCancelSprint) {
            if (((Sprint_h)this.h).mode.value == Sprint_h.Mode.Legit) {
                Sprint.mc.options.keySprint.setDown(false);
            }
            Sprint.mc.player.setSprinting(false);
            return;
        }
        if (((Sprint_h)this.h).mode.value == Sprint_h.Mode.Legit) {
            Sprint.mc.options.keySprint.setDown(true);
        } else if (((Sprint_h)this.h).mode.value == Sprint_h.Mode.Vulcan) {
            boolean hungerCondition;
            boolean isMovingForward = Sprint.mc.options.keyUp.isDown();
            boolean bl = hungerCondition = !((Sprint_h)this.h).checkHunger.value || Sprint.mc.player.getFoodData().getFoodLevel() > 6;
            if (isMovingForward && hungerCondition) {
                Sprint.mc.player.setSprinting(true);
            }
        }
    }

    private float getMoveYaw() {
        if (Sprint.mc.player == null) {
            return 0.0f;
        }
        float yaw = Sprint.mc.player.getYRot();
        float f = 0.0f;
        float s = 0.0f;
        if (Sprint.mc.options.keyUp.isDown()) {
            f += 1.0f;
        }
        if (Sprint.mc.options.keyDown.isDown()) {
            f -= 1.0f;
        }
        if (Sprint.mc.options.keyLeft.isDown()) {
            s += 1.0f;
        }
        if (Sprint.mc.options.keyRight.isDown()) {
            s -= 1.0f;
        }
        if (f == 0.0f && s == 0.0f) {
            return yaw;
        }
        boolean back = f < 0.0f;
        float moveYaw = yaw;
        if (f != 0.0f) {
            if (s > 0.0f) {
                moveYaw += back ? 45.0f : -45.0f;
            } else if (s < 0.0f) {
                moveYaw += back ? -45.0f : 45.0f;
            }
            if (back) {
                moveYaw += 180.0f;
            }
        } else if (s > 0.0f) {
            moveYaw -= 90.0f;
        } else if (s < 0.0f) {
            moveYaw += 90.0f;
        }
        return Mth.wrapDegrees((float)moveYaw);
    }
}

