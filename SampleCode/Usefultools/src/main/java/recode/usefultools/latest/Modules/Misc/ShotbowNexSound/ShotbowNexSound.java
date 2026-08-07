/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  org.spongepowered.asm.mixin.Unique
 */
package recode.usefultools.latest.Modules.Misc.ShotbowNexSound;

import java.util.Random;
import org.spongepowered.asm.mixin.Unique;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.ShotbowNexSound.ShotbowNexSound_h;

public class ShotbowNexSound
extends BaseModule<ShotbowNexSound_h> {
    public static ShotbowNexSound instance;
    @Unique
    private final Random random = new Random();

    public ShotbowNexSound() {
        super(new ShotbowNexSound_h());
        instance = this;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onUpdate() {
    }

    @Unique
    public float getRandomPitch() {
        double min = ((ShotbowNexSound_h)this.h).minPitch.value;
        double max = ((ShotbowNexSound_h)this.h).maxPitch.value;
        double step = ((ShotbowNexSound_h)this.h).pitchStep.value;
        double range = max - min;
        if (range <= 0.0) {
            return (float)min;
        }
        int steps = (int)Math.round(range / step);
        if (steps <= 0) {
            return (float)min;
        }
        int randomStep = this.random.nextInt(steps + 1);
        float pitch = (float)(min + (double)randomStep * step);
        return Math.max((float)min, Math.min((float)max, pitch));
    }
}

