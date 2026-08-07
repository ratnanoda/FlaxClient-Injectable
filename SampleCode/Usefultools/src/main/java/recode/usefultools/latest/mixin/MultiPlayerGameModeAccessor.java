/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.MultiPlayerGameMode
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={MultiPlayerGameMode.class})
public interface MultiPlayerGameModeAccessor {
    @Accessor(value="destroyDelay")
    public void setDestroyDelay(int var1);

    @Accessor(value="destroyDelay")
    public int getDestroyDelay();
}

