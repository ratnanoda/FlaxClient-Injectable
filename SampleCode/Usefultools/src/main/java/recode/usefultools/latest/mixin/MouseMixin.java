/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.MouseHandler
 *  net.minecraft.client.input.MouseButtonInfo
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import recode.usefultools.latest.Modules.Visual.ClickGui.ClickGui;
import recode.usefultools.latest.Modules.Visual.ClickGui.ClickGui_h;
import recode.usefultools.latest.mixin.MouseHandlerAccessor;
import recode.usefultools.latest.utils.AccountManager;

@Mixin(value={MouseHandler.class})
public class MouseMixin {
    @Inject(method={"onButton"}, at={@At(value="HEAD")}, cancellable=true)
    private void onMouseButton(long handle, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (ClickGui.instance != null && ((ClickGui_h)ClickGui.instance.h).enabled || AccountManager.INSTANCE.showScreen) {
            ci.cancel();
        }
    }

    @Inject(method={"onMove"}, at={@At(value="HEAD")}, cancellable=true)
    private void onMouseMove(long handle, double x, double y, CallbackInfo ci) {
        if (AccountManager.INSTANCE.showScreen) {
            MouseHandlerAccessor mha = (MouseHandlerAccessor)((Object)this);
            mha.setAccumulatedDX(0.0);
            mha.setAccumulatedDY(0.0);
            mha.setXpos(-9999.0);
            mha.setYpos(-9999.0);
            ci.cancel();
            return;
        }
        if (ClickGui.instance != null && ((ClickGui_h)ClickGui.instance.h).enabled) {
            MouseHandlerAccessor mha = (MouseHandlerAccessor)((Object)this);
            mha.setAccumulatedDX(0.0);
            mha.setAccumulatedDY(0.0);
            mha.setXpos(x);
            mha.setYpos(y);
            ci.cancel();
        }
    }
}

