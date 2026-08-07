/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.state.level.CameraRenderState
 *  net.minecraft.util.Mth
 *  org.joml.Quaternionfc
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package recode.usefultools.latest.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Visual.HurtCam.HurtCam_h;

@Mixin(value={GameRenderer.class})
public class HurtCamMixin {
    @Inject(method={"bobHurt(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void onBobHurt(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        BaseModule<?> hurtCam = ModuleManager.INSTANCE.getModuleByName("HurtCam");
        if (hurtCam == null || !((ModuleHeader)hurtCam.h).enabled) {
            return;
        }
        HurtCam_h.Mode mode = (HurtCam_h.Mode)((Object)((HurtCam_h)hurtCam.h).mode.value);
        if (mode == HurtCam_h.Mode.None) {
            return;
        }
        if (mode == HurtCam_h.Mode.NoHurtCam) {
            ci.cancel();
            return;
        }
        if (mode == HurtCam_h.Mode.FixedLeft && cameraState.entityRenderState.isLiving) {
            float hurt = cameraState.entityRenderState.hurtTime;
            if (cameraState.entityRenderState.isDeadOrDying) {
                float duration = Math.min(cameraState.entityRenderState.deathTime, 20.0f);
                poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(40.0f - 8000.0f / (duration + 200.0f)));
            }
            if (hurt < 0.0f) {
                ci.cancel();
                return;
            }
            hurt /= (float)cameraState.entityRenderState.hurtDuration;
            hurt = Mth.sin((double)(hurt * hurt * hurt * hurt * (float)Math.PI));
            float rr = 0.0f;
            poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(-rr));
            GameRenderer renderer = (GameRenderer)this;
            double tiltStrength = renderer.getGameRenderState().optionsRenderState.damageTiltStrength;
            float tiltAmount = (float)((double)(-hurt) * 14.0 * tiltStrength);
            poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(tiltAmount));
            poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(rr));
            ci.cancel();
        }
    }
}

