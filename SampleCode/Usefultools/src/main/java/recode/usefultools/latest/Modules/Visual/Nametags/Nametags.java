/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  imgui.ImFont
 *  imgui.ImGui
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.phys.Vec3
 */
package recode.usefultools.latest.Modules.Visual.Nametags;

import imgui.ImFont;
import imgui.ImGui;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Combat.AntiBot.AntiBot;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Visual.Interface.Interface;
import recode.usefultools.latest.Modules.Visual.Interface.Interface_h;
import recode.usefultools.latest.Modules.Visual.Nametags.Nametags_h;
import recode.usefultools.latest.utils.ImGuiEngine;

public class Nametags
extends BaseModule<Nametags_h> {
    public Nametags() {
        super(new Nametags_h());
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

    @Override
    public void onRenderHUD() {
        if (Nametags.mc.player == null || Nametags.mc.level == null) {
            return;
        }
        Interface ui = (Interface)ModuleManager.INSTANCE.getModuleByName("Interface");
        boolean isMc = ((Nametags_h)this.h).fontMode.value == Nametags_h.FontMode.InterfaceF ? ui != null && ((Interface_h)ui.h).font.value == Interface_h.FontType.Mojangles : ((Nametags_h)this.h).fontMode.value == Nametags_h.FontMode.Mojangles;
        ImFont font = ImGuiEngine.INSTANCE.fonts.getOrDefault(isMc ? "minecraft" : "main", ImGuiEngine.INSTANCE.fonts.get("main"));
        Vec3 camPos = Nametags.mc.gameRenderer.getMainCamera().position();
        float pitch = Nametags.mc.gameRenderer.getMainCamera().xRot();
        float yaw = Nametags.mc.gameRenderer.getMainCamera().yRot();
        float fRad = pitch * ((float)Math.PI / 180);
        float f1Rad = -yaw * ((float)Math.PI / 180);
        float cosYaw = Mth.cos((double)f1Rad);
        float sinYaw = Mth.sin((double)f1Rad);
        float cosPitch = Mth.cos((double)fRad);
        float sinPitch = Mth.sin((double)fRad);
        Vec3 lookVec = new Vec3((double)(sinYaw * cosPitch), (double)(-sinPitch), (double)(cosYaw * cosPitch));
        for (Entity entity : Nametags.mc.level.entitiesForRendering()) {
            LivingEntity living;
            if (!(entity instanceof LivingEntity) || (living = (LivingEntity)entity) == Nametags.mc.player || !living.isAlive()) continue;
            boolean isTeammate = false;
            if (living instanceof Player player) {
                isTeammate = AntiBot.checkTeammate(player);
            }
            if (!isTeammate ? ((Nametags_h)this.h).antiBotFilter.value && AntiBot.isBot((Entity)living) : !((Nametags_h)this.h).showTeammates.value) continue;
            Vec3 worldPos = living.position().add(0.0, (double)living.getBbHeight() + 0.5, 0.0);
            Vec3 toTarget = worldPos.subtract(camPos);
            if (toTarget.dot(lookVec) <= 0.0) continue;
            Vec3 ndc = Nametags.mc.gameRenderer.projectPointToScreen(worldPos);
            float sw = ImGui.getIO().getDisplaySizeX();
            float sh = ImGui.getIO().getDisplaySizeY();
            float x = (float)((ndc.x + 1.0) * 0.5 * (double)sw);
            float y = (float)((1.0 - ndc.y) * 0.5 * (double)sh);
            float finalSize = 16.0f * (float)((Nametags_h)this.h).size.value;
            String nameText = living.getDisplayName().getString();
            String hpText = String.format(" [%.1f]", Float.valueOf(living.getHealth()));
            String distText = ((Nametags_h)this.h).showDistance.value ? String.format(" [%.1fm]", Float.valueOf(Nametags.mc.player.distanceTo((Entity)living))) : "";
            int rawColor = -1;
            if (living.getDisplayName().getStyle().getColor() != null) {
                rawColor = living.getDisplayName().getStyle().getColor().getValue();
            }
            float r = (float)(rawColor >> 16 & 0xFF) / 255.0f;
            float g = (float)(rawColor >> 8 & 0xFF) / 255.0f;
            float b = (float)(rawColor & 0xFF) / 255.0f;
            int nameColor = ImGui.getColorU32((float)r, (float)g, (float)b, 1.0f);
            int hpColor = ImGui.getColorU32(0.0f, 1.0f, 0.0f, 1.0f);
            int distColor = ImGui.getColorU32(0.5f, 0.8f, 1.0f, 1.0f);
            ImGui.pushFont((ImFont)font);
            int fontTargetSize = (int)finalSize;
            float nameWidth = ImGui.calcTextSize((String)nameText).x * ((float)fontTargetSize / font.getFontSize());
            float hpWidth = ImGui.calcTextSize((String)hpText).x * ((float)fontTargetSize / font.getFontSize());
            float distWidth = ((Nametags_h)this.h).showDistance.value ? ImGui.calcTextSize((String)distText).x * ((float)fontTargetSize / font.getFontSize()) : 0.0f;
            float textHeight = ImGui.calcTextSize((String)nameText).y * ((float)fontTargetSize / font.getFontSize());
            ImGui.popFont();
            float totalWidth = nameWidth + hpWidth + distWidth;
            float padX = 6.0f * (float)((Nametags_h)this.h).size.value;
            float padY = 3.0f * (float)((Nametags_h)this.h).size.value;
            float minX = x - totalWidth / 2.0f - padX;
            float maxX = x + totalWidth / 2.0f + padX;
            float minY = y - textHeight / 2.0f - padY;
            float maxY = y + textHeight / 2.0f + padY;
            int bgColor = ImGui.getColorU32(0.0f, 0.0f, 0.0f, (float)((float)((Nametags_h)this.h).opacity.value));
            ImGui.getForegroundDrawList().addRectFilled(minX, minY, maxX, maxY, bgColor, 4.0f * (float)((Nametags_h)this.h).size.value);
            float startX = x - totalWidth / 2.0f;
            float startY = y - textHeight / 2.0f;
            ImGui.getForegroundDrawList().addText(font, fontTargetSize, startX, startY, nameColor, nameText);
            ImGui.getForegroundDrawList().addText(font, fontTargetSize, startX + nameWidth, startY, hpColor, hpText);
            if (!((Nametags_h)this.h).showDistance.value) continue;
            ImGui.getForegroundDrawList().addText(font, fontTargetSize, startX + nameWidth + hpWidth, startY, distColor, distText);
        }
    }
}

