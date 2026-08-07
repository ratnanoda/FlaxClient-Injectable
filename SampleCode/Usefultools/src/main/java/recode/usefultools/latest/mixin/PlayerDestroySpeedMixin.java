/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.block.state.BlockState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Player.FastBreak.FastBreak_h;

@Mixin(value={Player.class})
public class PlayerDestroySpeedMixin {
    @Inject(method={"getDestroySpeed"}, at={@At(value="RETURN")}, cancellable=true)
    private void onGetDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        BaseModule<?> fastBreak;
        Player player = (Player)this;
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player && (fastBreak = ModuleManager.INSTANCE.getModuleByName("FastBreak")) != null && ((ModuleHeader)fastBreak.h).enabled) {
            FastBreak_h h = (FastBreak_h)fastBreak.h;
            float divisor = (float)h.breakSpeed.value;
            float newSpeed = ((Float)cir.getReturnValue()).floatValue() / divisor;
            cir.setReturnValue((Object)Float.valueOf(newSpeed));
        }
    }
}

