/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$PosRot
 *  net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
 *  net.minecraft.network.protocol.game.ServerboundPlayerActionPacket$Action
 *  net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
 *  net.minecraft.network.protocol.game.ServerboundSwingPacket
 *  net.minecraft.util.Mth
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package recode.usefultools.latest.Modules.Combat.KillAura;

import java.lang.reflect.Field;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Combat.AntiBot.AntiBot;
import recode.usefultools.latest.Modules.Combat.KillAura.KillAura_h;
import recode.usefultools.latest.Modules.Misc.RotationManager.RotationManager;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Player.CivBreak.CivBreak;
import recode.usefultools.latest.Modules.Player.CivBreak.CivBreak_h;

public class KillAura
extends BaseModule<KillAura_h> {
    public static KillAura instance;
    private long lastAttackTime = 0L;
    private boolean shouldResetFlick = false;
    private double currentCpsThreshold = 10.0;
    private double currentCooldownThreshold = 1.0;
    public boolean isSwapping = false;
    public boolean isAboutToSwap = false;
    public boolean ignoreNextSwingPacket = false;

    public KillAura() {
        super(new KillAura_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        this.lastAttackTime = 0L;
        this.shouldResetFlick = false;
        this.isSwapping = false;
        this.isAboutToSwap = false;
        this.ignoreNextSwingPacket = false;
        this.randomizeThresholds();
    }

    @Override
    public void onDisable() {
        if (RotationManager.instance != null) {
            RotationManager.instance.reset("KillAura");
        }
        this.isSwapping = false;
        this.isAboutToSwap = false;
        this.ignoreNextSwingPacket = false;
    }

    private boolean shouldRotate() {
        BaseModule<?> civBreak;
        if (!((KillAura_h)this.h).rotate.value) {
            return false;
        }
        return !((KillAura_h)this.h).disableCivbreak.value || (civBreak = ModuleManager.INSTANCE.getModuleByName("CivBreak")) == null || !((ModuleHeader)civBreak.h).enabled;
    }

    private boolean isLookingAtHitbox(LivingEntity target) {
        if (KillAura.mc.player == null) {
            return false;
        }
        Vec3 eyePos = KillAura.mc.player.getEyePosition(1.0f);
        float yaw = RotationManager.instance != null && RotationManager.instance.rotating ? RotationManager.instance.serverYaw : KillAura.mc.player.getYRot();
        float pitch = RotationManager.instance != null && RotationManager.instance.rotating ? RotationManager.instance.serverPitch : KillAura.mc.player.getXRot();
        float f = pitch * ((float)Math.PI / 180);
        float f1 = -yaw * ((float)Math.PI / 180);
        float cosYaw = Mth.cos((double)f1);
        float sinYaw = Mth.sin((double)f1);
        float cosPitch = Mth.cos((double)f);
        float sinPitch = Mth.sin((double)f);
        Vec3 lookVec = new Vec3((double)(sinYaw * cosPitch), (double)(-sinPitch), (double)(cosYaw * cosPitch));
        double reach = ((KillAura_h)this.h).range.value + 0.5;
        Vec3 endPos = eyePos.add(lookVec.scale(reach));
        Optional clipResult = target.getBoundingBox().clip(eyePos, endPos);
        return clipResult.isPresent();
    }

    @Override
    public void onUpdate() {
        LivingEntity target;
        if (KillAura.mc.player == null || KillAura.mc.level == null) {
            return;
        }
        this.isAboutToSwap = false;
        if (this.shouldResetFlick) {
            if (RotationManager.instance != null) {
                RotationManager.instance.reset("KillAura");
            }
            this.shouldResetFlick = false;
        }
        if ((target = this.getClosestTarget()) != null) {
            boolean rotateActive;
            long now = System.currentTimeMillis();
            boolean isAboutToAttackNextTick = false;
            if (((KillAura_h)this.h).attackDelayMode.value == KillAura_h.AttackDelayMode.Ver_1_8) {
                long delay = (long)(1000.0 / this.currentCpsThreshold);
                isAboutToAttackNextTick = now + 50L - this.lastAttackTime >= delay;
            } else if (((KillAura_h)this.h).attackDelayMode.value == KillAura_h.AttackDelayMode.Ver_1_9) {
                float currentStrength = KillAura.mc.player.getAttackStrengthScale(0.5f);
                boolean bl = isAboutToAttackNextTick = currentStrength >= (float)this.currentCooldownThreshold - 0.05f;
            }
            if (isAboutToAttackNextTick) {
                int swordSlot = this.findSwordSlot();
                int originalSlot = this.getSelectedSlot();
                if (((KillAura_h)this.h).switchMode.value != KillAura_h.SwitchMode.NONE && swordSlot != -1 && originalSlot != swordSlot) {
                    this.isAboutToSwap = true;
                }
            }
            boolean isReadyToAttack = false;
            if (((KillAura_h)this.h).attackDelayMode.value == KillAura_h.AttackDelayMode.Ver_1_8) {
                long delay = (long)(1000.0 / this.currentCpsThreshold);
                isReadyToAttack = now - this.lastAttackTime >= delay;
            } else if (((KillAura_h)this.h).attackDelayMode.value == KillAura_h.AttackDelayMode.Ver_1_9) {
                float currentStrength = KillAura.mc.player.getAttackStrengthScale(0.0f);
                boolean bl = isReadyToAttack = currentStrength >= (float)this.currentCooldownThreshold;
            }
            if (isReadyToAttack && ((KillAura_h)this.h).delayOnCivbreak.value && CivBreak.instance != null && ((CivBreak_h)CivBreak.instance.h).enabled && CivBreak.instance.isAboutToBreak) {
                isReadyToAttack = false;
            }
            if (isReadyToAttack && ((KillAura_h)this.h).hitboxCheck.value && !this.isLookingAtHitbox(target)) {
                isReadyToAttack = false;
            }
            if (!(rotateActive = this.shouldRotate()) && RotationManager.instance != null) {
                RotationManager.instance.reset("KillAura");
            }
            float[] perfectRotations = this.getPerfectRotations(target);
            float[] rotations = this.applyRotationLimits(perfectRotations[0], perfectRotations[1]);
            switch ((KillAura_h.RotationMode)((Object)((KillAura_h)this.h).rotationMode.value)) {
                case Normal: 
                case Old: {
                    if (rotateActive) {
                        this.applyRotation(rotations[0], rotations[1]);
                    }
                    if (!isReadyToAttack) break;
                    this.attackEntity(target);
                    this.lastAttackTime = now;
                    this.randomizeThresholds();
                    break;
                }
                case Flick: {
                    if (!isReadyToAttack) break;
                    if (rotateActive) {
                        this.applyRotation(rotations[0], rotations[1]);
                    }
                    this.attackEntity(target);
                    this.lastAttackTime = now;
                    this.shouldResetFlick = true;
                    this.randomizeThresholds();
                    break;
                }
                case Flick2: {
                    if (isReadyToAttack) {
                        if (rotateActive) {
                            this.applyRotation(rotations[0], rotations[1]);
                        }
                        this.attackEntity(target);
                        this.lastAttackTime = now;
                        this.randomizeThresholds();
                        break;
                    }
                    if (now - this.lastAttackTime < (long)((KillAura_h)this.h).flickDuration.value) {
                        if (!rotateActive) break;
                        this.applyRotation(rotations[0], rotations[1]);
                        break;
                    }
                    if (RotationManager.instance == null) break;
                    RotationManager.instance.reset("KillAura");
                }
            }
        } else if (RotationManager.instance != null) {
            RotationManager.instance.reset("KillAura");
        }
    }

    private void randomizeThresholds() {
        double minC = ((KillAura_h)this.h).minCps.value;
        double maxC = ((KillAura_h)this.h).maxCps.value;
        if (minC > maxC) {
            double tmp = minC;
            minC = maxC;
            maxC = tmp;
        }
        this.currentCpsThreshold = minC + Math.random() * (maxC - minC);
        double minG = ((KillAura_h)this.h).cooldownMin.value;
        double maxG = ((KillAura_h)this.h).cooldownMax.value;
        if (minG > maxG) {
            double tmp = minG;
            minG = maxG;
            maxG = tmp;
        }
        this.currentCooldownThreshold = minG + Math.random() * (maxG - minG);
    }

    private float[] applyRotationLimits(float targetYaw, float targetPitch) {
        float currentYaw = RotationManager.instance != null && RotationManager.instance.rotating ? RotationManager.instance.serverYaw : KillAura.mc.player.getYRot();
        float currentPitch = RotationManager.instance != null && RotationManager.instance.rotating ? RotationManager.instance.serverPitch : KillAura.mc.player.getXRot();
        float diffYaw = Mth.wrapDegrees((float)(targetYaw - currentYaw));
        float diffPitch = targetPitch - currentPitch;
        float stepYaw = diffYaw * (float)((KillAura_h)this.h).horizontalFollowRate.value;
        float stepPitch = diffPitch * (float)((KillAura_h)this.h).verticalFollowRate.value;
        float maxYawSpeed = (float)((KillAura_h)this.h).horizontalSpeed.value;
        float maxPitchSpeed = (float)((KillAura_h)this.h).verticalSpeed.value;
        stepYaw = Mth.clamp((float)stepYaw, (float)(-maxYawSpeed), (float)maxYawSpeed);
        stepPitch = Mth.clamp((float)stepPitch, (float)(-maxPitchSpeed), (float)maxPitchSpeed);
        float nextYaw = currentYaw + stepYaw;
        float nextPitch = currentPitch + stepPitch;
        float clampLimit = (float)((KillAura_h)this.h).clampThreshold.value;
        if (Math.abs(Mth.wrapDegrees((float)(targetYaw - nextYaw))) <= clampLimit) {
            nextYaw = targetYaw;
        }
        if (Math.abs(targetPitch - nextPitch) <= clampLimit) {
            nextPitch = targetPitch;
        }
        return new float[]{nextYaw, Mth.clamp((float)nextPitch, -90.0f, 90.0f)};
    }

    private void applyRotation(float yaw, float pitch) {
        if (RotationManager.instance != null) {
            if (((KillAura_h)this.h).silent.value) {
                RotationManager.instance.setRotations(yaw, pitch, "KillAura");
            } else {
                RotationManager.instance.setRotations(yaw, pitch, "KillAura");
                if (RotationManager.instance.currentModuleName.equalsIgnoreCase("KillAura")) {
                    KillAura.mc.player.setYRot(yaw);
                    KillAura.mc.player.setXRot(pitch);
                }
            }
        }
    }

    private void attackEntity(LivingEntity target) {
        boolean canCrit;
        if (KillAura.mc.player == null || KillAura.mc.gameMode == null) {
            return;
        }
        int swordSlot = this.findSwordSlot();
        int originalSlot = this.getSelectedSlot();
        boolean swapped = false;
        if (((KillAura_h)this.h).switchMode.value != KillAura_h.SwitchMode.NONE && swordSlot != -1 && originalSlot != swordSlot) {
            this.isSwapping = true;
            if (((KillAura_h)this.h).switchMode.value == KillAura_h.SwitchMode.SPOOF) {
                this.setSelectedSlot(swordSlot);
                this.logSwitch("SPOOF", originalSlot, swordSlot);
            } else if (((KillAura_h)this.h).switchMode.value == KillAura_h.SwitchMode.FAKE) {
                KillAura.mc.player.connection.send((Packet)new ServerboundSetCarriedItemPacket(swordSlot));
                this.logSwitch("FAKE", originalSlot, swordSlot);
            }
            swapped = true;
        }
        boolean bl = canCrit = KillAura.mc.player.onGround() && !KillAura.mc.player.isInWater() && !KillAura.mc.player.isInLava() && !KillAura.mc.player.onClimbable() && !KillAura.mc.player.isPassenger() && !KillAura.mc.player.getAbilities().flying;
        if (canCrit && ((KillAura_h)this.h).critMode.value != KillAura_h.CritMode.NONE) {
            double x = KillAura.mc.player.getX();
            double y = KillAura.mc.player.getY();
            double z = KillAura.mc.player.getZ();
            float yaw = RotationManager.instance != null && RotationManager.instance.rotating ? RotationManager.instance.serverYaw : KillAura.mc.player.getYRot();
            float pitch = RotationManager.instance != null && RotationManager.instance.rotating ? RotationManager.instance.serverPitch : KillAura.mc.player.getXRot();
            switch ((KillAura_h.CritMode)((Object)((KillAura_h)this.h).critMode.value)) {
                case NCP: {
                    this.sendPosRot(x, y + 0.11, z, yaw, pitch);
                    this.sendPosRot(x, y + 0.1100013579, z, yaw, pitch);
                    this.sendPosRot(x, y + 1.3579E-6, z, yaw, pitch);
                    break;
                }
                case FALLING: {
                    this.sendPosRot(x, y + 0.0625, z, yaw, pitch);
                    this.sendPosRot(x, y + 0.0625013579, z, yaw, pitch);
                    this.sendPosRot(x, y + 1.3579E-6, z, yaw, pitch);
                    break;
                }
                case LOW: {
                    this.sendPosRot(x, y + 1.0E-9, z, yaw, pitch);
                    this.sendPosRot(x, y + 0.0, z, yaw, pitch);
                    break;
                }
                case DOWN: {
                    this.sendPosRot(x, y - 1.0E-9, z, yaw, pitch);
                }
            }
            KillAura.mc.player.crit((Entity)target);
            KillAura.mc.player.magicCrit((Entity)target);
        }
        if (((KillAura_h)this.h).attackMode.value == KillAura_h.AttackMode.CooldownBypass) {
            KillAura.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
            KillAura.mc.gameMode.attack((Player)KillAura.mc.player, (Entity)target);
            KillAura.mc.player.connection.send((Packet)new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        } else {
            KillAura.mc.gameMode.attack((Player)KillAura.mc.player, (Entity)target);
        }
        this.handleSwing();
        if (swapped) {
            if (((KillAura_h)this.h).switchMode.value == KillAura_h.SwitchMode.SPOOF) {
                this.setSelectedSlot(originalSlot);
                this.logRevert("SPOOF", swordSlot, originalSlot);
            } else if (((KillAura_h)this.h).switchMode.value == KillAura_h.SwitchMode.FAKE) {
                KillAura.mc.player.connection.send((Packet)new ServerboundSetCarriedItemPacket(originalSlot));
                this.logRevert("FAKE", swordSlot, originalSlot);
            }
            this.isSwapping = false;
        }
    }

    private void handleSwing() {
        if (KillAura.mc.player == null) {
            return;
        }
        switch ((KillAura_h.SwingMode)((Object)((KillAura_h)this.h).swingMode.value)) {
            case Normal: {
                KillAura.mc.player.swing(InteractionHand.MAIN_HAND);
                break;
            }
            case ClientOnly: {
                this.ignoreNextSwingPacket = true;
                KillAura.mc.player.swing(InteractionHand.MAIN_HAND);
                this.ignoreNextSwingPacket = false;
                break;
            }
            case ServerOnly: {
                KillAura.mc.player.connection.send((Packet)new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                break;
            }
        }
    }

    private void logSwitch(String modeName, int from, int to) {
        if (KillAura.mc.player != null) {
            KillAura.mc.player.sendSystemMessage((Component)Component.literal((String)("§7[§bUT§7] §c[KillAura] " + modeName + " Switch: Slot " + from + " -> " + to)));
        }
    }

    private void logRevert(String modeName, int from, int to) {
        if (KillAura.mc.player != null) {
            KillAura.mc.player.sendSystemMessage((Component)Component.literal((String)("§7[§bUT§7] §a[KillAura] " + modeName + " Revert: Slot " + from + " -> " + to)));
        }
    }

    private void sendPosRot(double px, double py, double pz, float pyaw, float ppitch) {
        if (KillAura.mc.player == null) {
            return;
        }
        KillAura.mc.player.connection.send((Packet)new ServerboundMovePlayerPacket.PosRot(px, py, pz, pyaw, ppitch, false, KillAura.mc.player.horizontalCollision));
    }

    private float[] getPerfectRotations(LivingEntity target) {
        Vec3 playerEyes = KillAura.mc.player.getEyePosition(1.0f);
        double targetX = target.getX();
        double targetZ = target.getZ();
        double targetY = target.getY();
        if (((KillAura_h)this.h).rotationMode.value == KillAura_h.RotationMode.Old) {
            targetY = target.getY() + (double)target.getEyeHeight() - 0.15;
        } else {
            switch ((KillAura_h.TargetPointMode)((Object)((KillAura_h)this.h).targetPointMode.value)) {
                case Head: {
                    targetY = target.getEyeY();
                    break;
                }
                case Body: {
                    targetY = target.getY() + (double)target.getBbHeight() / 2.0;
                    break;
                }
                case Feet: {
                    targetY = target.getY();
                    break;
                }
                case Actor: {
                    AABB box = target.getBoundingBox();
                    double closestX = Mth.clamp((double)playerEyes.x, (double)box.minX, (double)box.maxX);
                    double closestY = Mth.clamp((double)playerEyes.y, (double)box.minY, (double)box.maxY);
                    double closestZ = Mth.clamp((double)playerEyes.z, (double)box.minZ, (double)box.maxZ);
                    return this.calculateRotations(playerEyes, new Vec3(closestX, closestY, closestZ));
                }
                case Simple: {
                    AABB box = target.getBoundingBox();
                    targetY = playerEyes.y >= box.minY && playerEyes.y <= box.maxY ? playerEyes.y : (playerEyes.y > box.maxY ? box.maxY : box.minY);
                }
            }
        }
        return this.calculateRotations(playerEyes, new Vec3(targetX, targetY, targetZ));
    }

    private float[] calculateRotations(Vec3 from, Vec3 to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float)(Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float)(-(Math.atan2(diffY, dist) * 180.0 / Math.PI));
        return new float[]{yaw, Mth.clamp((float)pitch, -90.0f, 90.0f)};
    }

    private int getSelectedSlot() {
        if (KillAura.mc.player == null) {
            return 0;
        }
        try {
            Field field = Inventory.class.getDeclaredField("selected");
            field.setAccessible(true);
            return field.getInt(KillAura.mc.player.getInventory());
        } catch (Exception e) {
            try {
                for (Field f : Inventory.class.getDeclaredFields()) {
                    if (f.getType() != Integer.TYPE || !f.getName().equals("selected") && !f.getName().equals("selectedSlot")) continue;
                    f.setAccessible(true);
                    return f.getInt(KillAura.mc.player.getInventory());
                }
            } catch (Exception exception) {
                // empty catch block
            }
            return 0;
        }
    }

    private void setSelectedSlot(int slot) {
        if (KillAura.mc.player == null) {
            return;
        }
        try {
            Field field = Inventory.class.getDeclaredField("selected");
            field.setAccessible(true);
            field.setInt(KillAura.mc.player.getInventory(), slot);
        } catch (Exception e) {
            try {
                for (Field f : Inventory.class.getDeclaredFields()) {
                    if (f.getType() != Integer.TYPE || !f.getName().equals("selected") && !f.getName().equals("selectedSlot")) continue;
                    f.setAccessible(true);
                    f.setInt(KillAura.mc.player.getInventory(), slot);
                    return;
                }
            } catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private int findSwordSlot() {
        if (KillAura.mc.player == null) {
            return -1;
        }
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = KillAura.mc.player.getInventory().getItem(i);
            String itemId = BuiltInRegistries.ITEM.getKey((Object)stack.getItem()).getPath().toLowerCase();
            if (!itemId.contains("sword")) continue;
            return i;
        }
        return -1;
    }

    private LivingEntity getClosestTarget() {
        LivingEntity closest = null;
        double closestDist = ((KillAura_h)this.h).range.value;
        for (Entity entity : KillAura.mc.level.entitiesForRendering()) {
            double dist;
            LivingEntity living;
            if (!(entity instanceof LivingEntity) || !(living = (LivingEntity)entity).isAlive() || living == KillAura.mc.player || AntiBot.isBot((Entity)living) || !((dist = (double)KillAura.mc.player.distanceTo((Entity)living)) <= closestDist)) continue;
            closest = living;
            closestDist = dist;
        }
        return closest;
    }
}

