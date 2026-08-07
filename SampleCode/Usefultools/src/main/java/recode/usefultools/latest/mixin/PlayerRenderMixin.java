/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.entity.LivingEntityRenderer
 *  net.minecraft.client.renderer.entity.state.LivingEntityRenderState
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.LivingEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.ServerRotation.ServerRotation;
import recode.usefultools.latest.Modules.Misc.ServerRotation.ServerRotation_h;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;

@Mixin(value={LivingEntityRenderer.class})
public abstract class PlayerRenderMixin {
    @Inject(method={"extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V"}, at={@At(value="RETURN")})
    private void onExtractRenderStateReturn(LivingEntity entity, LivingEntityRenderState state, float f, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (entity != mc.player) {
            return;
        }
        ServerRotation srm = ServerRotation.instance;
        if (srm == null || !((ServerRotation_h)srm.h).enabled) {
            return;
        }
        if (((ServerRotation_h)srm.h).lerpMode.value == ServerRotation_h.LerpMode.Direct || ((ServerRotation_h)srm.h).lerpMode.value == ServerRotation_h.LerpMode.DirectLerp) {
            float rBody;
            float[] rots = srm.getRotations(f, true);
            float rYaw = rots[0];
            float rPitch = rots[1];
            state.bodyRot = rBody = rots[2];
            state.yRot = Mth.wrapDegrees((float)(rYaw - state.bodyRot));
            state.xRot = rPitch;
        }
    }

    @Inject(method={"shouldShowName"}, at={@At(value="HEAD")}, cancellable=true)
    private void onShouldShowName(LivingEntity entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> cir) {
        BaseModule<?> nametags = ModuleManager.INSTANCE.getModuleByName("Nametags");
        if (nametags != null && ((ModuleHeader)nametags.h).enabled) {
            cir.setReturnValue((Object)false);
        }
    }
}

