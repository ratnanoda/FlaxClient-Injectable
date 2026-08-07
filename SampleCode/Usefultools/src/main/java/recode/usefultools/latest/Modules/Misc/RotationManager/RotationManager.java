/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 */
package recode.usefultools.latest.Modules.Misc.RotationManager;

import net.minecraft.util.Mth;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.RotationManager.RotationManager_h;

public class RotationManager
extends BaseModule<RotationManager_h> {
    public static RotationManager instance;
    public boolean rotating = false;
    public float serverYaw = 0.0f;
    public float serverPitch = 0.0f;
    private int lastUpdateTick = -1;
    private int currentPriority = Integer.MAX_VALUE;
    public String currentModuleName = "";
    private final String[] priorityList = new String[]{"CivBreak", "Regen", "Fucker", "KillAura", "Scaffold", "BowAimbot"};

    public RotationManager() {
        super(new RotationManager_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        this.bypassReset();
    }

    @Override
    public void onDisable() {
        this.bypassReset();
    }

    @Override
    public void onUpdate() {
    }

    private void bypassReset() {
        this.rotating = false;
        this.serverYaw = RotationManager.mc.player != null ? RotationManager.mc.player.getYRot() : 0.0f;
        this.serverPitch = RotationManager.mc.player != null ? RotationManager.mc.player.getXRot() : 0.0f;
        this.lastUpdateTick = -1;
        this.currentPriority = Integer.MAX_VALUE;
        this.currentModuleName = "";
    }

    public void setRotations(float yaw, float pitch, String moduleName) {
        this.setRotations(yaw, pitch, moduleName, true);
    }

    public void setRotations(float yaw, float pitch, String moduleName, boolean activeSpoof) {
        int priority;
        if (RotationManager.mc.player == null) {
            this.serverYaw = yaw;
            this.serverPitch = pitch;
            return;
        }
        int tick = RotationManager.mc.player.tickCount;
        if (tick != this.lastUpdateTick) {
            this.lastUpdateTick = tick;
            this.currentPriority = Integer.MAX_VALUE;
        }
        if ((priority = this.getModulePriority(moduleName)) > this.currentPriority) {
            return;
        }
        this.currentPriority = priority;
        this.currentModuleName = moduleName;
        if (activeSpoof) {
            this.rotating = true;
        }
        float currentYaw = activeSpoof && this.rotating ? this.serverYaw : RotationManager.mc.player.getYRot();
        float currentPitch = activeSpoof && this.rotating ? this.serverPitch : RotationManager.mc.player.getXRot();
        float diffYaw = Mth.wrapDegrees((float)(yaw - currentYaw));
        float diffPitch = pitch - currentPitch;
        float roundedYaw = currentYaw + diffYaw;
        float roundedPitch = currentPitch + diffPitch;
        if (((RotationManager_h)this.h).enabled) {
            float step;
            if (((RotationManager_h)this.h).gcdBypass.value) {
                double sens = (Double)RotationManager.mc.options.sensitivity().get();
                float f = (float)sens * 0.6f + 0.2f;
                float f1 = f * f * f * 8.0f;
                step = (float)((double)f1 * 0.15);
            } else {
                step = (float)((RotationManager_h)this.h).step.value;
            }
            if (step > 0.0f) {
                int stepsYaw = Math.round(diffYaw / step);
                int stepsPitch = Math.round(diffPitch / step);
                float finalDiffYaw = (float)stepsYaw * step;
                float finalDiffPitch = (float)stepsPitch * step;
                roundedYaw = currentYaw + finalDiffYaw;
                roundedPitch = currentPitch + finalDiffPitch;
                if (((RotationManager_h)this.h).smoothDelta.value) {
                    float microDiffYaw = currentYaw + diffYaw - roundedYaw;
                    float microDiffPitch = currentPitch + diffPitch - roundedPitch;
                    float maxVal = (float)((RotationManager_h)this.h).maxDelta.value;
                    roundedYaw += Mth.clamp((float)microDiffYaw, (float)(-maxVal), (float)maxVal);
                    roundedPitch += Mth.clamp((float)microDiffPitch, (float)(-maxVal), (float)maxVal);
                }
                if (((RotationManager_h)this.h).formatDecimals.value) {
                    roundedYaw = (float)((double)Math.round((double)roundedYaw * 10000.0) / 10000.0);
                    roundedPitch = (float)((double)Math.round((double)roundedPitch * 10000.0) / 10000.0);
                }
            }
        }
        this.serverYaw = roundedYaw;
        this.serverPitch = roundedPitch;
    }

    public void reset(String moduleName) {
        if (moduleName.equalsIgnoreCase(this.currentModuleName)) {
            this.rotating = false;
            this.currentPriority = Integer.MAX_VALUE;
            this.currentModuleName = "";
        }
    }

    public void reset() {
        this.rotating = false;
        this.currentPriority = Integer.MAX_VALUE;
        this.currentModuleName = "";
    }

    private int getModulePriority(String name) {
        for (int i = 0; i < this.priorityList.length; ++i) {
            if (!this.priorityList[i].equalsIgnoreCase(name)) continue;
            return i;
        }
        return Integer.MAX_VALUE;
    }
}

