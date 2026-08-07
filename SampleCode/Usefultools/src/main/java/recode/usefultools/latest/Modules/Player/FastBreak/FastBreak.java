/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Player.FastBreak;

import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Player.FastBreak.FastBreak_h;
import recode.usefultools.latest.mixin.MultiPlayerGameModeAccessor;

public class FastBreak
extends BaseModule<FastBreak_h> {
    public FastBreak() {
        super(new FastBreak_h());
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onUpdate() {
        if (FastBreak.mc.gameMode != null && ((FastBreak_h)this.h).enabled && ((FastBreak_h)this.h).noBreakCooldown.value) {
            ((MultiPlayerGameModeAccessor)FastBreak.mc.gameMode).setDestroyDelay(0);
        }
    }
}

