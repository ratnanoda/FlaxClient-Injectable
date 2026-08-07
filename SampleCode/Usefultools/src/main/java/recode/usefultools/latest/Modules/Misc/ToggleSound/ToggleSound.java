/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 */
package recode.usefultools.latest.Modules.Misc.ToggleSound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.ToggleSound.ToggleSound_h;

public class ToggleSound
extends BaseModule<ToggleSound_h> {
    public static ToggleSound instance;
    private static int pendingOnSounds;
    private static int pendingOffSounds;

    public ToggleSound() {
        super(new ToggleSound_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        pendingOnSounds = 0;
        pendingOffSounds = 0;
    }

    @Override
    public void onDisable() {
        pendingOnSounds = 0;
        pendingOffSounds = 0;
    }

    @Override
    public void onUpdate() {
        if (ToggleSound.mc.player == null) {
            return;
        }
        if (pendingOnSounds > 0) {
            this.playOnSound();
            --pendingOnSounds;
        }
        if (pendingOffSounds > 0) {
            this.playOffSound();
            --pendingOffSounds;
        }
    }

    public static void playToggleSound(boolean enabled) {
        if (instance == null || !((ToggleSound_h)ToggleSound.instance.h).enabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (enabled) {
            instance.playOnSound();
            if (((ToggleSound_h)ToggleSound.instance.h).mode.value == ToggleSound_h.Mode.FlowerV3) {
                pendingOnSounds = 1;
            }
        } else {
            instance.playOffSound();
            if (((ToggleSound_h)ToggleSound.instance.h).mode.value == ToggleSound_h.Mode.FlowerV3) {
                pendingOffSounds = 1;
            }
        }
    }

    private void playOnSound() {
        SoundEvent sound = ((ToggleSound_h)this.h).mode.value == ToggleSound_h.Mode.Exert ? (SoundEvent)SoundEvents.UI_BUTTON_CLICK.value() : SoundEvents.EXPERIENCE_ORB_PICKUP;
        mc.getSoundManager().play((SoundInstance)SimpleSoundInstance.forUI((SoundEvent)sound, 1.0f, (float)((float)((ToggleSound_h)this.h).onVolume.value)));
    }

    private void playOffSound() {
        SoundEvent sound = SoundEvents.ITEM_PICKUP;
        float pitch = ((ToggleSound_h)this.h).mode.value == ToggleSound_h.Mode.Exert ? 1.0f : 0.9f;
        mc.getSoundManager().play((SoundInstance)SimpleSoundInstance.forUI((SoundEvent)sound, (float)pitch, (float)((float)((ToggleSound_h)this.h).offVolume.value)));
    }

    static {
        pendingOnSounds = 0;
        pendingOffSounds = 0;
    }
}

