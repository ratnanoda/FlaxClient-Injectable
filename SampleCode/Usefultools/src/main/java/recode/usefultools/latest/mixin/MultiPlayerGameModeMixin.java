/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.MultiPlayerGameMode
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Player.CivBreak.CivBreak;
import recode.usefultools.latest.Modules.Player.CivBreak.CivBreak_h;

@Mixin(value={MultiPlayerGameMode.class})
public class MultiPlayerGameModeMixin {
    @Inject(method={"startDestroyBlock"}, at={@At(value="HEAD")})
    private void onStartDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BaseModule<?> civBreak = ModuleManager.INSTANCE.getModuleByName("CivBreak");
        if (civBreak != null && ((ModuleHeader)civBreak.h).enabled) {
            CivBreak cb = (CivBreak)civBreak;
            CivBreak_h h = (CivBreak_h)cb.h;
            if (h.blockSelectMode.value == CivBreak_h.BlockSelectMode.CivBreak || h.blockSelectMode.value == CivBreak_h.BlockSelectMode.Exert) {
                cb.targetBlock = pos.immutable();
            }
        }
    }
}

