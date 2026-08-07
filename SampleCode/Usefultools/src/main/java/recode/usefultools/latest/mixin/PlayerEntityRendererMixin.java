/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.entity.LivingEntityRenderer
 *  net.minecraft.client.renderer.entity.state.LivingEntityRenderState
 *  net.minecraft.world.entity.LivingEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import recode.usefultools.latest.Modules.Misc.ServerRotation.ServerRotation;
import recode.usefultools.latest.Modules.Misc.ServerRotation.ServerRotation_h;

@Mixin(value={LivingEntityRenderer.class})
public abstract class PlayerEntityRendererMixin {
    @Unique
    private float oY;
    @Unique
    private float oP;
    @Unique
    private float oH;
    @Unique
    private float oB;
    @Unique
    private float oPY;
    @Unique
    private float oPP;
    @Unique
    private float oPH;
    @Unique
    private float oPB;
    @Unique
    private boolean isSpoofed = false;

    @Inject(method={"extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V"}, at={@At(value="HEAD")})
    private void onExtractHead(LivingEntity entity, LivingEntityRenderState state, float f, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (entity != mc.player) {
            return;
        }
        ServerRotation srm = ServerRotation.instance;
        if (srm == null || !((ServerRotation_h)srm.h).enabled) {
            return;
        }
        if (((ServerRotation_h)srm.h).lerpMode.value == ServerRotation_h.LerpMode.None || ((ServerRotation_h)srm.h).lerpMode.value == ServerRotation_h.LerpMode.Normal) {
            this.oY = entity.getYRot();
            this.oP = entity.getXRot();
            this.oH = entity.yHeadRot;
            this.oB = entity.yBodyRot;
            this.oPY = entity.yRotO;
            this.oPP = entity.xRotO;
            this.oPH = entity.yHeadRotO;
            this.oPB = entity.yBodyRotO;
            if (((ServerRotation_h)srm.h).speedMode.value == ServerRotation_h.Dependency.Tick && (((ServerRotation_h)srm.h).lerpMode.value == ServerRotation_h.LerpMode.None || ((ServerRotation_h)srm.h).lerpMode.value == ServerRotation_h.LerpMode.Normal)) {
                entity.setYRot(srm.yaw);
                entity.yRotO = srm.prevYaw;
                entity.setXRot(srm.pitch);
                entity.xRotO = srm.prevPitch;
                entity.yHeadRot = srm.yaw;
                entity.yHeadRotO = srm.prevYaw;
                entity.yBodyRot = srm.bodyYaw;
                entity.yBodyRotO = srm.prevBodyYaw;
            } else {
                float[] rots = srm.getRotations(f, true);
                float rY = rots[0];
                float rP = rots[1];
                float rB = rots[2];
                entity.setYRot(rY);
                entity.yRotO = rY;
                entity.setXRot(rP);
                entity.xRotO = rP;
                entity.yHeadRot = rY;
                entity.yHeadRotO = rY;
                entity.yBodyRot = rB;
                entity.yBodyRotO = rB;
            }
            this.isSpoofed = true;
        }
    }

    @Inject(method={"extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V"}, at={@At(value="RETURN")})
    private void onExtractReturn(LivingEntity entity, LivingEntityRenderState state, float f, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (entity != mc.player || !this.isSpoofed) {
            return;
        }
        entity.setYRot(this.oY);
        entity.yRotO = this.oPY;
        entity.setXRot(this.oP);
        entity.xRotO = this.oPP;
        entity.yHeadRot = this.oH;
        entity.yHeadRotO = this.oPH;
        entity.yBodyRot = this.oB;
        entity.yBodyRotO = this.oPB;
        this.isSpoofed = false;
    }
}

