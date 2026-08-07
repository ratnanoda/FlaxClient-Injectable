/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.block.state.BlockState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Visual.BlockESP.BlockESP;

@Mixin(value={ClientLevel.class})
public class ClientLevelMixin {
    @Inject(method={"setBlock"}, at={@At(value="HEAD")})
    private void onSetBlock(BlockPos pos, BlockState state, int flags, int maxDepth, CallbackInfoReturnable<Boolean> cir) {
        BlockESP esp = (BlockESP)ModuleManager.INSTANCE.getModuleByName("BlockESP");
        if (esp != null) {
            esp.onBlockUpdate(pos, state);
        }
    }
}

