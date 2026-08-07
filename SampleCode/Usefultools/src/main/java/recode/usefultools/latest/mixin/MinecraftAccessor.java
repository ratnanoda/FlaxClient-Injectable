/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.minecraft.UserApiService
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.User
 *  net.minecraft.client.multiplayer.ProfileKeyPairManager
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Mutable
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package recode.usefultools.latest.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={Minecraft.class})
public interface MinecraftAccessor {
    @Accessor(value="user")
    @Mutable
    public void setUser(User var1);

    @Accessor(value="profileKeyPairManager")
    @Mutable
    public void setProfileKeyPairManager(ProfileKeyPairManager var1);

    @Accessor(value="userApiService")
    public UserApiService getUserApiService();
}

