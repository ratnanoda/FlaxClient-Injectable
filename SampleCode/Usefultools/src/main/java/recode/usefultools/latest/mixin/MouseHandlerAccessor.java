/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.MouseHandler
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={MouseHandler.class})
public interface MouseHandlerAccessor {
    @Accessor(value="accumulatedDX")
    public void setAccumulatedDX(double var1);

    @Accessor(value="accumulatedDY")
    public void setAccumulatedDY(double var1);

    @Accessor(value="xpos")
    public void setXpos(double var1);

    @Accessor(value="ypos")
    public void setYpos(double var1);

    @Accessor(value="accumulatedDX")
    public double getAccumulatedDX();

    @Accessor(value="accumulatedDY")
    public double getAccumulatedDY();
}

