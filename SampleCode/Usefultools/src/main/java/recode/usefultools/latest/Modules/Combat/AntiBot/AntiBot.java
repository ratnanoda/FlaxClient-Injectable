/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.network.chat.TextColor
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.component.DyedItemColor
 *  net.minecraft.world.scores.PlayerTeam
 *  net.minecraft.world.scores.Team
 */
package recode.usefultools.latest.Modules.Combat.AntiBot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Combat.AntiBot.AntiBot_h;

public class AntiBot
extends BaseModule<AntiBot_h> {
    public static AntiBot instance;

    public AntiBot() {
        super(new AntiBot_h());
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

    public static boolean isBot(Entity entity) {
        if (instance == null || !((AntiBot_h)AntiBot.instance.h).enabled) {
            return false;
        }
        if (((AntiBot_h)AntiBot.instance.h).playerCheck.value && !(entity instanceof Player)) {
            return true;
        }
        if (entity instanceof Player player) {
            String name;
            Minecraft mc = Minecraft.getInstance();
            if (((AntiBot_h)AntiBot.instance.h).shotbow.value && !AntiBot.hasShotbowTeamColor(player)) {
                return true;
            }
            if (((AntiBot_h)AntiBot.instance.h).teamsMode.value != AntiBot_h.TeamsMode.NONE && mc.player != null && AntiBot.checkTeammate(player)) {
                return true;
            }
            if (((AntiBot_h)AntiBot.instance.h).playerListCheck.value && mc.getConnection() != null && mc.getConnection().getPlayerInfo(player.getUUID()) == null) {
                return true;
            }
            if (((AntiBot_h)AntiBot.instance.h).hitBoxCheck.value && (player.getBbWidth() == 0.0f || player.getBbHeight() == 0.0f)) {
                return true;
            }
            if (((AntiBot_h)AntiBot.instance.h).nameCheck.value && ((name = player.getGameProfile().name()).isEmpty() || name.contains(" ") || name.contains("§"))) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasShotbowTeamColor(Player player) {
        TextColor color = player.getDisplayName().getStyle().getColor();
        if (color == null) {
            color = player.getName().getStyle().getColor();
        }
        if (color != null) {
            String colorName = color.serialize().toLowerCase();
            return colorName.contains("red") || colorName.contains("blue") || colorName.contains("green") || colorName.contains("yellow") || colorName.contains("gold");
        }
        return false;
    }

    public static boolean checkTeammate(Player target) {
        if (instance == null || !((AntiBot_h)AntiBot.instance.h).enabled) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        AntiBot_h.TeamsMode mode = (AntiBot_h.TeamsMode)((Object)((AntiBot_h)AntiBot.instance.h).teamsMode.value);
        if (mode == AntiBot_h.TeamsMode.NAME_COLOR || mode == AntiBot_h.TeamsMode.ANY) {
            TextColor targetColor = target.getDisplayName().getStyle().getColor();
            TextColor ourColor = mc.player.getDisplayName().getStyle().getColor();
            if (targetColor != null && ourColor != null && targetColor.equals((Object)ourColor)) {
                return true;
            }
        }
        if ((mode == AntiBot_h.TeamsMode.SCOREBOARD || mode == AntiBot_h.TeamsMode.ANY) && mc.level != null && mc.level.getScoreboard() != null) {
            PlayerTeam ourTeam = mc.level.getScoreboard().getPlayersTeam(mc.player.getScoreboardName());
            PlayerTeam targetTeam = mc.level.getScoreboard().getPlayersTeam(target.getScoreboardName());
            if (ourTeam != null && targetTeam != null && ourTeam.isAlliedTo((Team)targetTeam)) {
                return true;
            }
        }
        if (mode == AntiBot_h.TeamsMode.ARMOR_COLOR || mode == AntiBot_h.TeamsMode.ANY) {
            int ourColor = AntiBot.getDyedArmorColor((Player)mc.player);
            int targetColor = AntiBot.getDyedArmorColor(target);
            if (ourColor != -1 && targetColor != -1 && ourColor == targetColor) {
                return true;
            }
        }
        return false;
    }

    private static int getDyedArmorColor(Player player) {
        EquipmentSlot[] slots;
        for (EquipmentSlot slot : slots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            DyedItemColor dyedColor;
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.has(DataComponents.DYED_COLOR) || (dyedColor = (DyedItemColor)stack.get(DataComponents.DYED_COLOR)) == null) continue;
            return dyedColor.rgb();
        }
        return -1;
    }
}

