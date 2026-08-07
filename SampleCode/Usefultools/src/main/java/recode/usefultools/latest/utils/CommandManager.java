/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants
 */
package recode.usefultools.latest.utils;

import com.mojang.blaze3d.platform.InputConstants;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.BindBlocker.BindBlocker;
import recode.usefultools.latest.Modules.Misc.BindBlocker.BindBlocker_h;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Player.CivBreak.CivBreak;
import recode.usefultools.latest.Modules.Player.Fucker.Fucker;
import recode.usefultools.latest.Modules.Player.Fucker.Fucker_h;
import recode.usefultools.latest.Modules.Visual.BlockESP.BlockESP;
import recode.usefultools.latest.Modules.Visual.BlockESP.BlockESP_h;
import recode.usefultools.latest.utils.ConfigManager;

public class CommandManager {
    public final static CommandManager INSTANCE = new CommandManager();

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean handleCommand(String message) {
        String cmd;
        String[] args = message.substring(1).split(" ");
        switch (cmd = args[0].toLowerCase()) {
            case "help": {
                this.send("§b§lUsefulTools Commands");
                this.send("§e.toggle (.t) §7- Toggle modules");
                this.send("§e.bind (.b) §7- Set keybinds");
                this.send("§e.config (.c) §7- Manage configs");
                this.send("§e.bindblocker §7- Manage blocked GUI screens");
                this.send("§e.blockesp §7- Manage BlockESP custom targets");
                return true;
            }
            case "toggle": 
            case "t": {
                if (args.length < 2) {
                    this.send("§cUsage: .t <module>");
                    return true;
                }
                BaseModule<?> m = ModuleManager.INSTANCE.getModuleByName(args[1]);
                if (m != null) {
                    m.toggle();
                    this.send("§b" + ((ModuleHeader)m.h).name + "§f: " + (((ModuleHeader)m.h).enabled ? "§aON" : "§cOFF"));
                    return true;
                } else {
                    this.send("§cModule not found.");
                }
                return true;
            }
            case "bind": 
            case "b": {
                if (args.length < 3) {
                    this.send("§cUsage: .b <module> <key>");
                    return true;
                }
                BaseModule<?> m = ModuleManager.INSTANCE.getModuleByName(args[1]);
                if (m != null) {
                    try {
                        String keyName = args[2].toLowerCase();
                        if (keyName.equals("none")) {
                            ((ModuleHeader)m.h).key = 0;
                            this.send("Unbound §b" + ((ModuleHeader)m.h).name);
                            return true;
                        }
                        int keyCode = InputConstants.getKey((String)("key.keyboard." + keyName)).getValue();
                        if (keyCode != -1) {
                            ((ModuleHeader)m.h).key = keyCode;
                            this.send("Bound §b" + ((ModuleHeader)m.h).name + "§f to §e" + keyName.toUpperCase());
                            return true;
                        }
                        this.send("§cInvalid key.");
                        return true;
                    } catch (Exception e) {
                        this.send("§cBind error.");
                    }
                    return true;
                }
                this.send("§cModule not found.");
                return true;
            }
            case "config": 
            case "c": {
                this.handleConfig(args);
                return true;
            }
            case "fucker": {
                this.handleFucker(args);
                return true;
            }
            case "civbreak": {
                this.handleCivBreak(args);
                return true;
            }
            case "bindblocker": {
                this.handleBindBlocker(args);
                return true;
            }
            case "blockesp": {
                this.handleBlockESP(args);
                return true;
            }
        }
        this.send("§cUnknown command. Type §b.help");
        return true;
    }

    private void handleConfig(String[] args) {
        if (args.length < 2) {
            this.send("§cUsage: .c <s/l/li/confirm>");
            return;
        }
        String sub = args[1].toLowerCase();
        ConfigManager cm = ConfigManager.INSTANCE;
        switch (sub) {
            case "save": 
            case "s": {
                String name;
                String string = name = args.length > 2 ? args[2] : cm.lastLoadedConfig;
                if (name.equalsIgnoreCase("none")) {
                    this.send("§cUsage: .c s <name>");
                    break;
                }
                cm.save(name, false);
                break;
            }
            case "load": 
            case "l": {
                if (args.length < 3) {
                    this.send("§cUsage: .c l <name>");
                    break;
                }
                cm.load(args[2]);
                break;
            }
            case "list": 
            case "li": {
                cm.list();
                break;
            }
            case "confirm": {
                if (!cm.pendingConfigName.isEmpty()) {
                    cm.save(cm.pendingConfigName, true);
                    break;
                }
                this.send("§cNo pending overwrite.");
            }
        }
    }

    private void handleFucker(String[] args) {
        if (args.length < 2) {
            this.send("§cUsage: .fucker <add/del/list> [block_name]");
            return;
        }
        String sub = args[1].toLowerCase();
        Fucker fMod = (Fucker)ModuleManager.INSTANCE.getModuleByName("Fucker");
        if (fMod == null) {
            this.send("§cModule Fucker not found.");
            return;
        }
        switch (sub) {
            case "add": {
                if (args.length < 3) {
                    this.send("§cUsage: .fucker add [block_name]");
                    return;
                }
                String blockName = args[2].toLowerCase();
                if (!((Fucker_h)fMod.h).targetBlocks.value.contains(blockName)) {
                    ((Fucker_h)fMod.h).targetBlocks.value.add(blockName);
                    this.send("§aAdded§f block: §e" + blockName);
                    break;
                }
                this.send("§cBlock already registered.");
                break;
            }
            case "del": 
            case "remove": {
                if (args.length < 3) {
                    this.send("§cUsage: .fucker del [block_name]");
                    return;
                }
                String blockName = args[2].toLowerCase();
                if (((Fucker_h)fMod.h).targetBlocks.value.remove(blockName)) {
                    this.send("§cRemoved§f block: §e" + blockName);
                    break;
                }
                this.send("§cBlock not found in target list.");
                break;
            }
            case "list": {
                if (((Fucker_h)fMod.h).targetBlocks.value.isEmpty()) {
                    this.send("§7No targeted blocks registered.");
                    break;
                }
                this.send("§bTargeted blocks: §f" + String.join((CharSequence)"§7, §f", ((Fucker_h)fMod.h).targetBlocks.value));
                break;
            }
            default: {
                this.send("§cUnknown subcommand. Use add, del, or list.");
            }
        }
    }

    private void handleCivBreak(String[] args) {
        if (args.length < 2) {
            this.send("§cUsage: .civbreak <add/del/list> [block_name]");
            return;
        }
        String sub = args[1].toLowerCase();
        CivBreak cMod = (CivBreak)ModuleManager.INSTANCE.getModuleByName("CivBreak");
        if (cMod == null) {
            this.send("§cModule CivBreak not found.");
            return;
        }
        switch (sub) {
            case "add": {
                if (args.length < 3) {
                    this.send("§cUsage: .civbreak add [block_name]");
                    return;
                }
                String blockName = args[2].toLowerCase();
                if (!cMod.targetBlockNames.contains(blockName)) {
                    cMod.targetBlockNames.add(blockName);
                    this.send("§aAdded§f block: §e" + blockName);
                    break;
                }
                this.send("§cBlock already registered.");
                break;
            }
            case "del": 
            case "remove": {
                if (args.length < 3) {
                    this.send("§cUsage: .civbreak del [block_name]");
                    return;
                }
                String blockName = args[2].toLowerCase();
                if (cMod.targetBlockNames.remove(blockName)) {
                    this.send("§cRemoved§f block: §e" + blockName);
                    break;
                }
                this.send("§cBlock not found in target list.");
                break;
            }
            case "list": {
                if (cMod.targetBlockNames.isEmpty()) {
                    this.send("§7No targeted blocks registered.");
                    break;
                }
                this.send("§bTargeted blocks: §f" + String.join((CharSequence)"§7, §f", cMod.targetBlockNames));
                break;
            }
            default: {
                this.send("§cUnknown subcommand. Use add, del, or list.");
            }
        }
    }

    private void handleBindBlocker(String[] args) {
        if (args.length < 2) {
            this.send("§cUsage: .bindblocker <add/del/list> [screen_address]");
            return;
        }
        String sub = args[1].toLowerCase();
        BindBlocker bMod = (BindBlocker)ModuleManager.INSTANCE.getModuleByName("BindBlocker");
        if (bMod == null) {
            this.send("§cModule BindBlocker not found.");
            return;
        }
        switch (sub) {
            case "add": {
                if (args.length < 3) {
                    this.send("§cUsage: .bindblocker add [screen_address]");
                    return;
                }
                String address = args[2];
                if (!((BindBlocker_h)bMod.h).blockedScreens.value.contains(address)) {
                    ((BindBlocker_h)bMod.h).blockedScreens.value.add(address);
                    this.send("§aAdded§f blocked screen: §e" + address);
                    break;
                }
                this.send("§cScreen address already registered.");
                break;
            }
            case "del": 
            case "remove": {
                if (args.length < 3) {
                    this.send("§cUsage: .bindblocker del [screen_address]");
                    return;
                }
                String address = args[2];
                if (((BindBlocker_h)bMod.h).blockedScreens.value.remove(address)) {
                    this.send("§cRemoved§f blocked screen: §e" + address);
                    break;
                }
                this.send("§cScreen address not found in list.");
                break;
            }
            case "list": {
                if (((BindBlocker_h)bMod.h).blockedScreens.value.isEmpty()) {
                    this.send("§7No custom blocked screens registered.");
                    break;
                }
                this.send("§bBlocked screens: §f" + String.join((CharSequence)"§7, §f", ((BindBlocker_h)bMod.h).blockedScreens.value));
                break;
            }
            default: {
                this.send("§cUnknown subcommand. Use add, del, or list.");
            }
        }
    }

    private void handleBlockESP(String[] args) {
        if (args.length < 2) {
            this.send("§cUsage: .blockesp <add/del/color/list> [block_name] [color]");
            return;
        }
        String sub = args[1].toLowerCase();
        BlockESP bMod = (BlockESP)ModuleManager.INSTANCE.getModuleByName("BlockESP");
        if (bMod == null) {
            this.send("§cModule BlockESP not found.");
            return;
        }
        switch (sub) {
            case "add": {
                if (args.length < 3) {
                    this.send("§cUsage: .blockesp add [block_name]");
                    return;
                }
                String blockName = args[2].toLowerCase();
                if (!((BlockESP_h)bMod.h).targetBlocks.value.contains(blockName)) {
                    ((BlockESP_h)bMod.h).targetBlocks.value.add(blockName);
                    String colorEntryPrefix = blockName + ":";
                    boolean hasColor = false;
                    for (String entry2 : ((BlockESP_h)bMod.h).blockColors.value) {
                        if (!entry2.toLowerCase().startsWith(colorEntryPrefix)) continue;
                        hasColor = true;
                        break;
                    }
                    if (!hasColor) {
                        ((BlockESP_h)bMod.h).blockColors.value.add(blockName + ":themecolor");
                    }
                    this.send("§aAdded§f BlockESP target: §e" + blockName);
                    break;
                }
                this.send("§cBlock already registered.");
                break;
            }
            case "del": 
            case "remove": {
                if (args.length < 3) {
                    this.send("§cUsage: .blockesp del [block_name]");
                    return;
                }
                String blockName = args[2].toLowerCase();
                if (((BlockESP_h)bMod.h).targetBlocks.value.remove(blockName)) {
                    String colorEntryPrefix = blockName + ":";
                    ((BlockESP_h)bMod.h).blockColors.value.removeIf(entry -> entry.toLowerCase().startsWith(colorEntryPrefix));
                    this.send("§cRemoved§f BlockESP target: §e" + blockName);
                    break;
                }
                this.send("§cBlock not found in target list.");
                break;
            }
            case "list": {
                if (((BlockESP_h)bMod.h).targetBlocks.value.isEmpty()) {
                    this.send("§7No BlockESP targets registered.");
                    break;
                }
                this.send("§bRegistered targets: §f" + String.join((CharSequence)"§7, §f", ((BlockESP_h)bMod.h).targetBlocks.value));
                break;
            }
            case "color": {
                if (args.length < 4) {
                    this.printColorManual();
                    return;
                }
                String blockName = args[2].toLowerCase();
                String inputColor = args[3].toLowerCase();
                if (!((BlockESP_h)bMod.h).targetBlocks.value.contains(blockName)) {
                    this.send("§c[Error] §e" + blockName + " §cはターゲットリストに登録されていません。先に .blockesp add で追加してください。");
                    return;
                }
                boolean isHex = false;
                if (!inputColor.equals("themecolor")) {
                    String cleanHex = inputColor.startsWith("#") ? inputColor.substring(1) : inputColor;
                    boolean bl = isHex = cleanHex.matches("^[0-9a-fA-F]{6}$") || cleanHex.matches("^[0-9a-fA-F]{8}$");
                }
                if (!inputColor.equals("themecolor") && !isHex) {
                    this.printColorManual();
                    return;
                }
                String colorEntryPrefix = blockName + ":";
                ((BlockESP_h)bMod.h).blockColors.value.removeIf(entry -> entry.toLowerCase().startsWith(colorEntryPrefix));
                String newEntry = blockName + ":" + (String)(inputColor.startsWith("#") ? inputColor : "#" + inputColor);
                if (inputColor.equals("themecolor")) {
                    newEntry = blockName + ":themecolor";
                }
                ((BlockESP_h)bMod.h).blockColors.value.add(newEntry);
                this.send("§aSuccessfully updated! §b" + blockName + " §fcolor set to §e" + inputColor.toUpperCase());
                break;
            }
            default: {
                this.send("§cUnknown subcommand. Use add, del, color, or list.");
            }
        }
    }

    private void printColorManual() {
        this.send("§c[Error] 不正なカラー記述フォーマットです。");
        this.send("§e===============================================");
        this.send("§b【正しいBlockESPカラーコマンド使用方法】");
        this.send("§e  .blockesp color [ブロック名] [カラー指定]");
        this.send("§a 1. 16進数カラーコード（RRGGBB または AARRGGBB）で指定:");
        this.send("   §f.blockesp color chest FFC39B §7(チェストを黄土色に指定)");
        this.send("   §f.blockesp color end_stone FFE000 §7(エンドストーンを黄色に指定)");
        this.send("   §f.blockesp color furnace #808080 §7(ハッシュ記号付き指定も可能)");
        this.send("§a 2. クライアントの同期カラー（Theme Color）に指定:");
        this.send("   §f.blockesp color redstone_ore themecolor");
        this.send("§e===============================================");
    }

    private void send(String msg) {
        ConfigManager.INSTANCE.sendMessage(msg);
    }
}

