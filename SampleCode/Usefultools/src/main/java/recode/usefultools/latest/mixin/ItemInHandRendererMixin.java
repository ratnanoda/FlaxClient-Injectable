/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.AbstractClientPlayer
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.ItemInHandRenderer
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.HumanoidArm
 *  net.minecraft.world.item.ItemStack
 *  org.joml.Quaternionfc
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package recode.usefultools.latest.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import recode.usefultools.latest.Modules.Visual.Animations.Animations;
import recode.usefultools.latest.Modules.Visual.Animations.Animations_h;

@Mixin(value={ItemInHandRenderer.class})
public abstract class ItemInHandRendererMixin {
    @Shadow
    private void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float attackValue) {
    }

    @Redirect(method={"swingArm"}, at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void onSwingArmTranslate(PoseStack poseStack, float x, float y, float z) {
        if (this.shouldApplyCustomTransform()) {
            return;
        }
        if (Animations.instance != null && ((Animations_h)Animations.instance.h).enabled && ((Animations_h)Animations.instance.h).fluxSwing.value) {
            return;
        }
        poseStack.translate(x, y, z);
    }

    @Redirect(method={"swingArm"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmAttackTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"))
    private void onApplyItemArmAttackTransform(ItemInHandRenderer instance, PoseStack poseStack, HumanoidArm arm, float attackValue) {
        this.applyItemArmAttackTransform(poseStack, arm, attackValue);
    }

    @Unique
    private boolean isHoldingSword(AbstractClientPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }
        String name = BuiltInRegistries.ITEM.getKey((Object)stack.getItem()).getPath().toLowerCase();
        return name.contains("sword");
    }

    @Unique
    private boolean isGeneralBlocking() {
        if (Animations.instance == null || !((Animations_h)Animations.instance.h).enabled) {
            return false;
        }
        Animations_h.BlockMode bMode = (Animations_h.BlockMode)((Object)((Animations_h)Animations.instance.h).fakeBlockMode.value);
        if (bMode == Animations_h.BlockMode.NONE) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        boolean isSword = this.isHoldingSword((AbstractClientPlayer)mc.player);
        return mc.options.keyUse.isDown() && (bMode == Animations_h.BlockMode.ALWAYS || bMode == Animations_h.BlockMode.SWORD_ONLY && isSword);
    }

    @Unique
    private boolean shouldApplyCustomTransform() {
        if (Animations.instance == null || !((Animations_h)Animations.instance.h).enabled) {
            return false;
        }
        if (((Animations_h)Animations.instance.h).onlyOnBlock.value) {
            return this.isGeneralBlocking();
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && !mc.player.getMainHandItem().isEmpty();
    }

    @Inject(method={"applyItemArmTransform"}, at={@At(value="TAIL")})
    private void onApplyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float inverseArmHeight, CallbackInfo ci) {
        if (Animations.instance != null && ((Animations_h)Animations.instance.h).enabled) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && arm == mc.player.getMainArm() && this.shouldApplyCustomTransform()) {
                float armSide = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
                float baseTransX = 0.0f;
                float baseTransY = 0.0f;
                float baseTransZ = 0.0f;
                float baseRotX = 0.0f;
                float baseRotY = 0.0f;
                float baseRotZ = 0.0f;
                if (this.isGeneralBlocking()) {
                    if (((Animations_h)Animations.instance.h).blockStyleMode.value == Animations_h.BlockStyleMode.Java) {
                        baseTransX = -0.14142136f;
                        baseTransY = (float)((Animations_h)Animations.instance.h).blockY.value;
                        baseTransZ = 0.14142136f;
                        baseRotX = -102.25f;
                        baseRotY = 13.365f;
                        baseRotZ = 78.05f;
                    } else if (((Animations_h)Animations.instance.h).blockStyleMode.value == Animations_h.BlockStyleMode.Solstice) {
                        baseTransX = 0.06f / armSide;
                        baseTransY = 0.4f;
                        baseTransZ = -0.55f;
                        baseRotX = 74.48f;
                        baseRotY = -53.54f;
                        baseRotZ = 0.0f;
                    }
                }
                float finalTransX = baseTransX + (float)((Animations_h)Animations.instance.h).customX.value;
                float finalTransY = baseTransY + (float)((Animations_h)Animations.instance.h).customY.value;
                float finalTransZ = baseTransZ + (float)((Animations_h)Animations.instance.h).customZ.value;
                float finalRotX = baseRotX + (float)((Animations_h)Animations.instance.h).customRotX.value;
                float finalRotY = baseRotY + (float)((Animations_h)Animations.instance.h).customRotY.value;
                float finalRotZ = baseRotZ + (float)((Animations_h)Animations.instance.h).customRotZ.value;
                poseStack.translate(armSide * finalTransX, finalTransY, finalTransZ);
                poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(finalRotX));
                poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(armSide * finalRotY));
                poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(armSide * finalRotZ));
                float customScale = (float)((Animations_h)Animations.instance.h).customScale.value;
                poseStack.scale(customScale, customScale, customScale);
            }
        }
    }

    @Redirect(method={"renderHandsWithItems"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/player/LocalPlayer;getAttackAnim(F)F"))
    private float onGetAttackAnim(LocalPlayer player, float partialTicks) {
        return player.getAttackAnim(partialTicks);
    }

    @Redirect(method={"tick"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"))
    private float onGetItemSwapScale(LocalPlayer player, float partialTicks) {
        if (Animations.instance != null && ((Animations_h)Animations.instance.h).enabled && ((Animations_h)Animations.instance.h).noCooldown.value) {
            return 1.0f;
        }
        return player.getItemSwapScale(partialTicks);
    }

    @Inject(method={"itemUsed"}, at={@At(value="HEAD")}, cancellable=true)
    private void onItemUsed(InteractionHand hand, CallbackInfo ci) {
        if (Animations.instance != null && ((Animations_h)Animations.instance.h).enabled && ((Animations_h)Animations.instance.h).noCooldown.value) {
            ci.cancel();
        }
    }

    @Inject(method={"shouldInstantlyReplaceVisibleItem"}, at={@At(value="HEAD")}, cancellable=true)
    private void onShouldInstantlyReplaceVisibleItem(ItemStack currentlyVisibleItem, ItemStack expectedItem, CallbackInfoReturnable<Boolean> cir) {
        if (Animations.instance != null && ((Animations_h)Animations.instance.h).enabled && ((Animations_h)Animations.instance.h).oldSwap.value && !ItemStack.matches((ItemStack)currentlyVisibleItem, (ItemStack)expectedItem)) {
            cir.setReturnValue((Object)false);
        }
    }
}

