/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.floats.FloatUnaryOperator
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package recode.usefultools.latest.mixin;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Movement.Fly.Fly;
import recode.usefultools.latest.Modules.Movement.Fly.Fly_h;
import recode.usefultools.latest.Modules.Player.Timer.Timer_h;

@Mixin(targets={"net.minecraft.client.DeltaTracker$Timer"})
public class DeltaTrackerTimerMixin {
    @Redirect(method={"advanceGameTime"}, at=@At(value="INVOKE", target="Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F"))
    private float onApplyMspt(FloatUnaryOperator operator, float msPerTick) {
        BaseModule<?> flyMod;
        float vanillaMspt = operator.apply(msPerTick);
        BaseModule<?> timerMod = ModuleManager.INSTANCE.getModuleByName("Timer");
        if (timerMod != null && ((ModuleHeader)timerMod.h).enabled) {
            Timer_h h = (Timer_h)timerMod.h;
            double speed = h.speed.value;
            if (speed > 0.0) {
                return (float)((double)vanillaMspt / speed);
            }
        }
        if ((flyMod = ModuleManager.INSTANCE.getModuleByName("Fly")) != null && ((ModuleHeader)flyMod.h).enabled) {
            double boostVal;
            double speedFactor;
            Fly f = (Fly)flyMod;
            if (((Fly_h)f.h).timerBoost.value && (speedFactor = (boostVal = f.getCurrentTimerBoostValue()) / 20.0) > 0.0) {
                return (float)((double)vanillaMspt / speedFactor);
            }
        }
        return vanillaMspt;
    }
}

