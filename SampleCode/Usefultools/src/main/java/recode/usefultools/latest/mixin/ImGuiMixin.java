/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import recode.usefultools.latest.utils.ImGuiEngine;

@Mixin(value={Minecraft.class})
public class ImGuiMixin {
    @Inject(method={"renderFrame"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/systems/RenderSystem;flipFrame(Lcom/mojang/blaze3d/TracyFrameCapture;)V")})
    private void onBeforeFlipFrame(boolean advanceGameTime, CallbackInfo ci) {
        ImGuiEngine.INSTANCE.onFrame();
    }
}

