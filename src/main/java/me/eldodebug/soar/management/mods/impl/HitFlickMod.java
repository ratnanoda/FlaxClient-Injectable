package me.eldodebug.soar.management.mods.impl;

import java.util.ArrayList;
import java.util.Arrays;

import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.event.impl.EventAttackEntity;
import me.eldodebug.soar.management.event.impl.EventRenderTick;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.Mod;
import me.eldodebug.soar.management.mods.ModCategory;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

/**
 * Briefly flicks the player's view 90 degrees after a real attack, restores
 * the original view, resumes sprinting and performs one follow-up attack.
 */
public class HitFlickMod extends Mod {

    private final ComboSetting directionSetting;
    private Entity pendingTarget;
    private float originalYaw;
    private int stage;
    private boolean internalAttack;

    public HitFlickMod() {
        // The visible strings are overridden below; MODE is only a constructor
        // placeholder so this module does not require new translation keys.
        super(TranslateText.MODE, TranslateText.MODE, ModCategory.GHOST);
        directionSetting = new ComboSetting(
                TranslateText.MODE,
                this,
                TranslateText.RIGHT,
                new ArrayList<Option>(Arrays.asList(
                        new Option(TranslateText.LEFT),
                        new Option(TranslateText.RIGHT))));
    }

    @Override
    public String getName() {
        return "HitFlick";
    }

    @Override
    public String getDescription() {
        return "Flicks 90 degrees on hit, returns, sprints and follows up with an attack.";
    }

    @Override
    public String getNameKey() {
        return "text.hitflick.name";
    }

    @EventTarget
    public void onAttack(EventAttackEntity event) {
        if(internalAttack || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        Entity entity = event.getEntity();
        if(!(entity instanceof EntityLivingBase) || entity == mc.thePlayer || entity.isDead) {
            return;
        }

        pendingTarget = entity;
        originalYaw = mc.thePlayer.rotationYaw;
        float direction = directionSetting.getOption().getNameKey().equals(TranslateText.LEFT.getKey())
                ? -90.0F
                : 90.0F;
        mc.thePlayer.rotationYaw = originalYaw + direction;
        stage = 1;
    }

    @EventTarget
    public void onRenderTick(EventRenderTick event) {
        if(stage == 0 || mc.thePlayer == null || mc.theWorld == null) {
            clearState(false);
            return;
        }

        if(stage == 1) {
            mc.thePlayer.rotationYaw = originalYaw;
            if(!mc.thePlayer.isSneaking() && !mc.thePlayer.isUsingItem()
                    && !mc.thePlayer.isCollidedHorizontally && mc.thePlayer.moveForward > 0.0F) {
                mc.thePlayer.setSprinting(true);
            }
            stage = 2;
            return;
        }

        Entity target = pendingTarget;
        clearState(false);
        if(target == null || target.isDead || target == mc.thePlayer) {
            return;
        }

        internalAttack = true;
        try {
            mc.playerController.attackEntity(mc.thePlayer, target);
            mc.thePlayer.swingItem();
        } finally {
            internalAttack = false;
        }
    }

    @Override
    public void onDisable() {
        clearState(true);
        super.onDisable();
    }

    private void clearState(boolean restoreYaw) {
        if(restoreYaw && stage != 0 && mc.thePlayer != null) {
            mc.thePlayer.rotationYaw = originalYaw;
        }
        pendingTarget = null;
        stage = 0;
    }
}
