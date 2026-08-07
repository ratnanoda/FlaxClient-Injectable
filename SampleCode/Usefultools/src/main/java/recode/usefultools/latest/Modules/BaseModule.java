/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package recode.usefultools.latest.Modules;

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recode.usefultools.latest.Modules.Misc.ToggleSound.ToggleSound;
import recode.usefultools.latest.Modules.ModuleHeader;

public abstract class BaseModule<T extends ModuleHeader> {
    protected final static Minecraft mc = Minecraft.getInstance();
    protected final static Logger LOGGER = LoggerFactory.getLogger((String)"UsefulTools");
    public final T h;

    public BaseModule(T header) {
        this.h = header;
    }

    public void toggle() {
        this.setEnabled(!((ModuleHeader)this.h).enabled);
    }

    public void setEnabled(boolean enabled) {
        if (((ModuleHeader)this.h).enabled == enabled) {
            return;
        }
        ((ModuleHeader)this.h).enabled = enabled;
        ToggleSound.playToggleSound(enabled);
        if (enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }

    public abstract void onEnable();

    public abstract void onDisable();

    public abstract void onUpdate();

    public void onRenderHUD() {
    }
}

