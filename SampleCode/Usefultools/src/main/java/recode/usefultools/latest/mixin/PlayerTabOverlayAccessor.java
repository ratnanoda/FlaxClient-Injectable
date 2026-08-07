/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.components.PlayerTabOverlay
 *  net.minecraft.network.chat.Component
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={PlayerTabOverlay.class})
public interface PlayerTabOverlayAccessor {
    @Accessor(value="header")
    public Component getHeader();

    @Accessor(value="footer")
    public Component getFooter();
}

