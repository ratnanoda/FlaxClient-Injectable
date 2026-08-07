/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.ChatScreen
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen
 *  net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
 *  net.minecraft.client.gui.screens.inventory.InventoryScreen
 *  net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen
 */
package recode.usefultools.latest.Modules.Misc.BindBlocker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.BindBlocker.BindBlocker_h;

public class BindBlocker
extends BaseModule<BindBlocker_h> {
    public static BindBlocker instance;

    public BindBlocker() {
        super(new BindBlocker_h());
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

    public static boolean shouldBlockKeybinds() {
        if (instance == null || !((BindBlocker_h)BindBlocker.instance.h).enabled) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            return false;
        }
        if (((BindBlocker_h)BindBlocker.instance.h).blockChat.value && mc.screen instanceof ChatScreen) {
            return true;
        }
        if (((BindBlocker_h)BindBlocker.instance.h).blockCmdBlock.value && mc.screen instanceof CommandBlockEditScreen) {
            return true;
        }
        if (((BindBlocker_h)BindBlocker.instance.h).blockStructure.value && mc.screen instanceof StructureBlockEditScreen) {
            return true;
        }
        if (((BindBlocker_h)BindBlocker.instance.h).blockInv.value && (mc.screen instanceof InventoryScreen || mc.screen instanceof CreativeModeInventoryScreen || mc.screen instanceof AbstractContainerScreen)) {
            return true;
        }
        String fullClassName = mc.screen.getClass().getName();
        String simpleClassName = mc.screen.getClass().getSimpleName();
        for (String address : ((BindBlocker_h)BindBlocker.instance.h).blockedScreens.value) {
            if (!fullClassName.equalsIgnoreCase(address) && !simpleClassName.equalsIgnoreCase(address)) continue;
            return true;
        }
        return false;
    }
}

