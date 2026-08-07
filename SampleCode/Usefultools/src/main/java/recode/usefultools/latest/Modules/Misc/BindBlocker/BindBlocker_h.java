/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Misc.BindBlocker;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.ListSetting;

public class BindBlocker_h
extends ModuleHeader {
    public BoolSetting blockInv = new BoolSetting("Block Inv", "Block keybinds when inventory or container is open", true);
    public BoolSetting blockCmdBlock = new BoolSetting("Block CmdBlock", "Block keybinds when editing command block", true);
    public BoolSetting blockChat = new BoolSetting("Block Chat", "Block keybinds when typing in chat screen", true);
    public BoolSetting blockStructure = new BoolSetting("Block Stru", "Block keybinds when editing structure block", true);
    public ListSetting blockedScreens = new ListSetting("Blocked Screens", "List of dynamically blocked screen classes");

    public BindBlocker_h() {
        super("BindBlocker", "Prevents keybind triggers inside UI screens", Category.MISC, 0, true);
        this.addSettings(this.blockInv, this.blockCmdBlock, this.blockChat, this.blockStructure, this.blockedScreens);
    }
}

