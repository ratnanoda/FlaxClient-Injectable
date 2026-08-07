/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package recode.usefultools.latest.Modules.Combat.AntiBot;

import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;

public class AntiBot_h
extends ModuleHeader {
    public BoolSetting playerCheck = new BoolSetting("Player Check", "Validates basic player entities", true);
    public BoolSetting hitBoxCheck = new BoolSetting("HitBox Check", "Detects bots with zero hitbox", true);
    public BoolSetting nameCheck = new BoolSetting("Name Check", "Detects invalid/bot names", true);
    public BoolSetting playerListCheck = new BoolSetting("PlayerList Check", "Checks if target is in Tab-List", true);
    public BoolSetting shotbow = new BoolSetting("Shotbow", "Excludes players without team colors from target", false);
    public EnumSetting<TeamsMode> teamsMode = new EnumSetting<TeamsMode>("Teams Mode", "Excludes teammates from combat", TeamsMode.NONE, "None", "Name Color", "Scoreboard", "Armor Color", "Any");

    public AntiBot_h() {
        super("AntiBot", "Filters out anticheat bots and teammates", Category.COMBAT, 0, false);
        this.addSettings(this.playerCheck, this.hitBoxCheck, this.nameCheck, this.playerListCheck, this.shotbow, this.teamsMode);
    }

    public static enum TeamsMode {
        NONE,
        NAME_COLOR,
        SCOREBOARD,
        ARMOR_COLOR,
        ANY;

    }
}

