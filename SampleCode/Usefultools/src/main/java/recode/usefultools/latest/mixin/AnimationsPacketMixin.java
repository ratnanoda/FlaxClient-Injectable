/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
 *  net.minecraft.network.protocol.game.ServerboundUseItemPacket
 *  net.minecraft.world.item.ItemStack
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Visual.Animations.Animations;
import recode.usefultools.latest.Modules.Visual.Animations.Animations_h;

@Mixin(value={ClientCommonPacketListenerImpl.class})
public class AnimationsPacketMixin {
    @Inject(method={"send"}, at={@At(value="HEAD")}, cancellable=true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        BaseModule<?> anims;
        if ((packet instanceof ServerboundUseItemPacket || packet instanceof ServerboundUseItemOnPacket) && (anims = ModuleManager.INSTANCE.getModuleByName("Animations")) != null && ((ModuleHeader)anims.h).enabled) {
            Animations animMod = (Animations)anims;
            Animations_h.BlockMode bMode = (Animations_h.BlockMode)((Object)((Animations_h)animMod.h).fakeBlockMode.value);
            if (bMode != Animations_h.BlockMode.NONE) {
                ItemStack stack;
                Minecraft mc = Minecraft.getInstance();
                boolean isSword = false;
                if (mc.player != null && !(stack = mc.player.getMainHandItem()).isEmpty()) {
                    String name = BuiltInRegistries.ITEM.getKey((Object)stack.getItem()).getPath().toLowerCase();
                    isSword = name.contains("sword");
                }
                if ((bMode == Animations_h.BlockMode.ALWAYS || bMode == Animations_h.BlockMode.SWORD_ONLY && isSword) && mc.options.keyUse.isDown()) {
                    if (animMod.wasRightClickDown) {
                        ci.cancel();
                        return;
                    }
                    animMod.wasRightClickDown = true;
                }
            }
        }
    }
}

