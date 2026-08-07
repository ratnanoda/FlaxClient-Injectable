/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.DeltaTracker
 *  net.minecraft.client.renderer.GameRenderer
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import recode.usefultools.latest.Modules.Visual.ClickGui.ClickGui;
import recode.usefultools.latest.Modules.Visual.ClickGui.ClickGui_h;

@Mixin(value={GameRenderer.class})
public class BlurMixin {
    @Inject(method={"render"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V", shift=At.Shift.AFTER)})
    private void forceBlur(DeltaTracker delta, boolean tick, CallbackInfo ci) {
        if (ClickGui.isVisible() && ((ClickGui_h)ClickGui.instance.h).bgType.value == ClickGui_h.Background.New) {
            ((GameRenderer)this).processBlurEffect();
        }
    }
}

