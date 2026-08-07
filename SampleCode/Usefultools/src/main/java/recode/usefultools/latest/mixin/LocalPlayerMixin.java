/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.util.Mth
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.AngleFix.AngleFix;
import recode.usefultools.latest.Modules.Misc.AngleFix.AngleFix_h;
import recode.usefultools.latest.Modules.Misc.Disabler.Disabler;
import recode.usefultools.latest.Modules.Misc.Disabler.Disabler_h;
import recode.usefultools.latest.Modules.Misc.RotationManager.RotationManager;
import recode.usefultools.latest.Modules.Misc.RotationManager.RotationManager_h;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Movement.Fly.Fly;
import recode.usefultools.latest.Modules.Movement.Fly.Fly_h;
import recode.usefultools.latest.Modules.Movement.Speed.Speed;
import recode.usefultools.latest.Modules.Movement.Speed.Speed_h;
import recode.usefultools.latest.Modules.Movement.Sprint.Sprint_h;
import recode.usefultools.latest.Modules.Player.Scaffold.Scaffold_h;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.Setting;

@Mixin(value={LocalPlayer.class})
public class LocalPlayerMixin {
    @Unique
    private float actualYaw;
    @Unique
    private float actualPitch;
    @Unique
    private boolean isSpoofingActive = false;
    @Unique
    private boolean realOnGround;
    @Unique
    private boolean isSpoofingOnGround = false;
    @Unique
    private long lastNetskipSentTime = 0L;
    @Unique
    private boolean realUp;
    @Unique
    private boolean realDown;
    @Unique
    private boolean realLeft;
    @Unique
    private boolean realRight;
    @Unique
    private boolean keysSpoofed = false;
    @Unique
    private float lastServerYaw = 0.0f;

    @Inject(method={"tick"}, at={@At(value="HEAD")})
    private void onTick(CallbackInfo ci) {
        ModuleManager.INSTANCE.onUpdate();
    }

    @Inject(method={"aiStep"}, at={@At(value="HEAD")})
    private void onAiStepHead(CallbackInfo ci) {
        Fly flyMod;
        LocalPlayer player = (LocalPlayer)this;
        Minecraft mc = Minecraft.getInstance();
        Speed speedMod = (Speed)ModuleManager.INSTANCE.getModuleByName("Speed");
        if (speedMod != null && ((Speed_h)speedMod.h).enabled) {
            speedMod.onTravel(player);
        }
        if ((flyMod = (Fly)ModuleManager.INSTANCE.getModuleByName("Fly")) != null && ((Fly_h)flyMod.h).enabled) {
            flyMod.onTravel(player);
        }
        this.keysSpoofed = false;
        if (RotationManager.instance != null && RotationManager.instance.rotating) {
            BaseModule<?> angleFix;
            boolean afEnabled;
            BaseModule<?> fucker;
            Scaffold_h.RotMode rMode;
            BaseModule<?> scaffold;
            int activeFixType = 0;
            BaseModule<?> aura = ModuleManager.INSTANCE.getModuleByName("KillAura");
            if (aura != null && ((ModuleHeader)aura.h).enabled) {
                activeFixType = Math.max(activeFixType, this.getMoveFixOrdinalSafe(aura));
            }
            if ((scaffold = ModuleManager.INSTANCE.getModuleByName("Scaffold")) != null && ((ModuleHeader)scaffold.h).enabled && (rMode = (Scaffold_h.RotMode)((Object)((Scaffold_h)scaffold.h).rotMode.value)) != Scaffold_h.RotMode.NORMAL && rMode != Scaffold_h.RotMode.BACK) {
                activeFixType = Math.max(activeFixType, this.getMoveFixOrdinalSafe(scaffold));
            }
            if ((fucker = ModuleManager.INSTANCE.getModuleByName("Fucker")) != null && ((ModuleHeader)fucker.h).enabled) {
                activeFixType = Math.max(activeFixType, this.getMoveFixOrdinalSafe(fucker));
            }
            boolean bl = afEnabled = (angleFix = ModuleManager.INSTANCE.getModuleByName("AngleFix")) != null && ((ModuleHeader)angleFix.h).enabled;
            if (afEnabled && ((AngleFix_h)angleFix.h).forceSilent.value) {
                activeFixType = 2;
            }
            if (activeFixType == 2) {
                boolean targetRight;
                float yawChange;
                this.realUp = mc.options.keyUp.isDown();
                this.realDown = mc.options.keyDown.isDown();
                this.realLeft = mc.options.keyLeft.isDown();
                this.realRight = mc.options.keyRight.isDown();
                if (!(this.realUp || this.realDown || this.realLeft || this.realRight)) {
                    this.lastServerYaw = RotationManager.instance.serverYaw;
                    return;
                }
                float z = 0.0f;
                float x = 0.0f;
                if (this.realUp) {
                    z += 1.0f;
                }
                if (this.realDown) {
                    z -= 1.0f;
                }
                if (this.realLeft) {
                    x += 1.0f;
                }
                if (this.realRight) {
                    x -= 1.0f;
                }
                float clientYaw = player.getYRot();
                float serverYaw = RotationManager.instance.serverYaw;
                if (afEnabled) {
                    if (((AngleFix_h)angleFix.h).invertClientYaw.value) {
                        clientYaw = -clientYaw;
                    }
                    if (((AngleFix_h)angleFix.h).invertServerYaw.value) {
                        serverYaw = -serverYaw;
                    }
                }
                if ((yawChange = Math.abs(Mth.wrapDegrees((float)(serverYaw - this.lastServerYaw)))) > 45.0f && !player.onGround()) {
                    serverYaw = Mth.rotLerp(0.5f, (float)this.lastServerYaw, (float)serverYaw);
                }
                this.lastServerYaw = serverYaw;
                float deltaYaw = Mth.wrapDegrees((float)(clientYaw - serverYaw));
                float rad = deltaYaw * ((float)Math.PI / 180);
                float newX = x * Mth.cos((double)rad) - z * Mth.sin((double)rad);
                float newZ = z * Mth.cos((double)rad) + x * Mth.sin((double)rad);
                int movementSideways = Math.round(newX);
                int movementForward = Math.round(newZ);
                boolean targetUp = movementForward > 0;
                boolean targetDown = movementForward < 0;
                boolean targetLeft = movementSideways > 0;
                boolean bl2 = targetRight = movementSideways < 0;
                if (afEnabled) {
                    boolean tmp;
                    if (((AngleFix_h)angleFix.h).swapSilentSides.value) {
                        tmp = targetLeft;
                        targetLeft = targetRight;
                        targetRight = tmp;
                    }
                    if (((AngleFix_h)angleFix.h).swapSilentUpDown.value) {
                        tmp = targetUp;
                        targetUp = targetDown;
                        targetDown = tmp;
                    }
                }
                if (!targetUp || targetDown) {
                    player.setSprinting(false);
                    mc.options.keySprint.setDown(false);
                }
                mc.options.keyUp.setDown(targetUp);
                mc.options.keyDown.setDown(targetDown);
                mc.options.keyLeft.setDown(targetLeft);
                mc.options.keyRight.setDown(targetRight);
                this.keysSpoofed = true;
                if (afEnabled && ((AngleFix_h)angleFix.h).debug.value) {
                    AngleFix.debugClientYaw = clientYaw;
                    AngleFix.debugServerYaw = serverYaw;
                    AngleFix.debugMoveAngle = deltaYaw;
                    AngleFix.debugRelDeg = deltaYaw;
                    AngleFix.debugKeysStr = this.getPressedKeysStringSafe(targetUp, targetDown, targetLeft, targetRight);
                }
            } else {
                this.lastServerYaw = player.getYRot();
            }
        } else {
            this.lastServerYaw = player.getYRot();
        }
    }

    @Inject(method={"aiStep"}, at={@At(value="TAIL")})
    private void onAiStepTail(CallbackInfo ci) {
        BaseModule<?> sprint;
        LocalPlayer player = (LocalPlayer)this;
        if (this.keysSpoofed) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options != null) {
                mc.options.keyUp.setDown(this.realUp);
                mc.options.keyDown.setDown(this.realDown);
                mc.options.keyLeft.setDown(this.realLeft);
                mc.options.keyRight.setDown(this.realRight);
                this.keysSpoofed = false;
            }
        }
        if ((sprint = ModuleManager.INSTANCE.getModuleByName("Sprint")) != null && ((ModuleHeader)sprint.h).enabled) {
            sprint.onUpdate();
        }
    }

    @Inject(method={"isSprintingPossible"}, at={@At(value="HEAD")}, cancellable=true)
    private void onIsSprintingPossible(boolean allowedInShallowWater, CallbackInfoReturnable<Boolean> cir) {
        BaseModule<?> sprint = ModuleManager.INSTANCE.getModuleByName("Sprint");
        if (sprint != null && ((ModuleHeader)sprint.h).enabled) {
            Sprint_h sh = (Sprint_h)sprint.h;
            if (!sh.checkHunger.value) {
                cir.setReturnValue((Object)true);
            }
        }
    }

    @Inject(method={"sendPosition"}, at={@At(value="HEAD")}, cancellable=true)
    private void onSendPositionHead(CallbackInfo ci) {
        BaseModule<?> speedMod;
        LocalPlayer player = (LocalPlayer)this;
        Fly flyMod = (Fly)ModuleManager.INSTANCE.getModuleByName("Fly");
        if (flyMod != null && ((Fly_h)flyMod.h).enabled) {
            if (flyMod.isVulcanTestWaiting()) {
                ci.cancel();
                return;
            }
            if (((Fly_h)flyMod.h).netskip.value) {
                long delay;
                long now = System.currentTimeMillis();
                if (now - this.lastNetskipSentTime < (delay = (long)flyMod.getCurrentNetskipDelay())) {
                    ci.cancel();
                    return;
                }
                this.lastNetskipSentTime = now;
            }
        }
        if ((speedMod = ModuleManager.INSTANCE.getModuleByName("Speed")) != null && ((ModuleHeader)speedMod.h).enabled) {
            this.realOnGround = player.onGround();
            boolean spoofedOnGround = ((Speed)speedMod).getSpoofedOnGround(this.realOnGround);
            if (spoofedOnGround != this.realOnGround) {
                player.setOnGround(spoofedOnGround);
                this.isSpoofingOnGround = true;
            }
        }
        if (Disabler.instance != null && ((Disabler_h)Disabler.instance.h).enabled && ((Disabler_h)Disabler.instance.h).velocityD.value && Disabler.instance.ticksToSend > 0) {
            boolean shouldCancelVanilla = false;
            switch ((Disabler_h.VeloBypass)((Object)((Disabler_h)Disabler.instance.h).veloBypass.value)) {
                case NormalFix: {
                    shouldCancelVanilla = Disabler.instance.isMoving();
                    break;
                }
                case Semi_Full: {
                    shouldCancelVanilla = true;
                    break;
                }
                case Break_Delay: {
                    shouldCancelVanilla = Disabler.instance.isMoving();
                    break;
                }
                case Break_Semi: {
                    int currentTick = player.tickCount;
                    if (currentTick - Disabler.lastStopBreakTick <= 1) {
                        shouldCancelVanilla = true;
                        break;
                    }
                    shouldCancelVanilla = Disabler.instance.isMoving();
                    break;
                }
                case Old: {
                    shouldCancelVanilla = false;
                }
            }
            if (shouldCancelVanilla) {
                if (this.isSpoofingOnGround) {
                    player.setOnGround(this.realOnGround);
                    this.isSpoofingOnGround = false;
                }
                ci.cancel();
                return;
            }
        }
        if (RotationManager.instance != null) {
            boolean shouldSpoof;
            boolean bl = shouldSpoof = RotationManager.instance.rotating || ((RotationManager_h)RotationManager.instance.h).enabled && ((RotationManager_h)RotationManager.instance.h).alwaysClamp.value;
            if (shouldSpoof) {
                this.actualYaw = player.getYRot();
                this.actualPitch = player.getXRot();
                if (!RotationManager.instance.rotating && ((RotationManager_h)RotationManager.instance.h).alwaysClamp.value) {
                    RotationManager.instance.setRotations(player.getYRot(), player.getXRot(), "AlwaysClamp", false);
                }
                player.setYRot(RotationManager.instance.serverYaw);
                player.setXRot(RotationManager.instance.serverPitch);
                this.isSpoofingActive = true;
            }
        }
    }

    @Inject(method={"sendPosition"}, at={@At(value="TAIL")})
    private void onSendPositionTail(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer)this;
        if (this.isSpoofingActive && RotationManager.instance != null) {
            player.setYRot(this.actualYaw);
            player.setXRot(this.actualPitch);
            this.isSpoofingActive = false;
        }
        if (this.isSpoofingOnGround) {
            player.setOnGround(this.realOnGround);
            this.isSpoofingOnGround = false;
        }
    }

    @Unique
    private int getMoveFixOrdinalSafe(BaseModule<?> module) {
        if (module == null || module.h == null) {
            return 0;
        }
        for (Setting s : ((ModuleHeader)module.h).settings) {
            if (!s.name.equalsIgnoreCase("Move Fix") || !(s instanceof EnumSetting)) continue;
            return ((Enum)((EnumSetting)s).value).ordinal();
        }
        return 0;
    }

    @Unique
    private String getPressedKeysStringSafe(boolean up, boolean down, boolean left, boolean right) {
        String res;
        StringBuilder sb = new StringBuilder();
        if (up) {
            sb.append("W ");
        }
        if (down) {
            sb.append("S ");
        }
        if (left) {
            sb.append("A ");
        }
        if (right) {
            sb.append("D ");
        }
        return (res = sb.toString().trim()).isEmpty() ? "None" : res.replace(" ", " + ");
    }
}

