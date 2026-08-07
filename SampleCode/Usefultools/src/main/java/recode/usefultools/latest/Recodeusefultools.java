/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 */
package recode.usefultools.latest;

import net.fabricmc.api.ClientModInitializer;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.utils.ConfigManager;

public class Recodeusefultools
implements ClientModInitializer {
    public final static String MOD_ID = "recode-useful-tools";

    public void onInitializeClient() {
        ConfigManager.INSTANCE.init();
        ModuleManager.INSTANCE.init();
    }
}

