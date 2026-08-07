/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.ModifyVariable
 */
package recode.usefultools.latest.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Misc.AngleFix.AngleFix_h;
import recode.usefultools.latest.Modules.Misc.RotationManager.RotationManager;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Player.Scaffold.Scaffold_h;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.Setting;

@Mixin(value={LivingEntity.class})
public class LivingEntityMixin {
    @ModifyVariable(method={"travel"}, at=@At(value="HEAD"), argsOnly=true)
    private Vec3 onTravel(Vec3 travelVector) {
        LivingEntityMixin livingEntityMixin = this;
        if (livingEntityMixin instanceof LocalPlayer) {
            LocalPlayer player = (LocalPlayer)livingEntityMixin;
            if (RotationManager.instance != null && RotationManager.instance.rotating) {
                BaseModule<?> angleFix;
                boolean afEnabled;
                BaseModule<?> fucker;
                Scaffold_h.RotMode rMode;
                BaseModule<?> scaffold;
                double xxa = travelVector.x;
                double zza = travelVector.z;
                if (xxa == 0.0 && zza == 0.0) {
                    return travelVector;
                }
                int activeFixType = 0;
                BaseModule<?> aura = ModuleManager.INSTANCE.getModuleByName("KillAura");
                if (aura != null && ((ModuleHeader)aura.h).enabled) {
                    activeFixType = Math.max(activeFixType, this.getMoveFixOrdinalSafe(aura));
                }
                if ((scaffold = ModuleManager.INSTANCE.getModuleByName("Scaffold")) != null && ((ModuleHeader)scaffold.h).enabled && (rMode = (Scaffold_h.RotMode)((Object)((Scaffold_h)scaffold.h).rotMode.value)) != Scaffold_h.RotMode.NORMAL && rMode != Scaffold_h.RotMode.BACK) {
                    activeFixType = Math.max(activeFixType, this.getMoveFixOrdinalSafe(scaffold));
                }
                if ((fucker = ModuleManager.INSTANCE.getModuleByName("Fucker")) != null && ((ModuleHeader)fucker.h).enabled) {
                    activeFixType = Math.max(activeFixType, this.getMoveFixOrdinalSafe(fucker));
                }
                boolean bl = afEnabled = (angleFix = ModuleManager.INSTANCE.getModuleByName("AngleFix")) != null && ((ModuleHeader)angleFix.h).enabled;
                if (afEnabled && ((AngleFix_h)angleFix.h).forceSilent.value) {
                    activeFixType = 2;
                }
                if (activeFixType > 0) {
                    float clientYaw = player.getYRot();
                    float serverYaw = RotationManager.instance.serverYaw;
                    if (afEnabled) {
                        if (((AngleFix_h)angleFix.h).invertClientYaw.value) {
                            clientYaw = -clientYaw;
                        }
                        if (((AngleFix_h)angleFix.h).invertServerYaw.value) {
                            serverYaw = -serverYaw;
                        }
                    }
                    float rawDiffDeg = Mth.wrapDegrees((float)(serverYaw - clientYaw));
                    if (afEnabled && ((AngleFix_h)angleFix.h).invertSimple.value) {
                        rawDiffDeg = -rawDiffDeg;
                    }
                    double diffRad = Math.toRadians(rawDiffDeg);
                    double cos = Math.cos(diffRad);
                    double sin = Math.sin(diffRad);
                    double newX = xxa * cos - zza * sin;
                    double newZ = zza * cos + xxa * sin;
                    return new Vec3(newX, travelVector.y, newZ);
                }
            }
        }
        return travelVector;
    }

    @Unique
    private int getMoveFixOrdinalSafe(BaseModule<?> module) {
        if (module == null || module.h == null) {
            return 0;
        }
        for (Setting s : ((ModuleHeader)module.h).settings) {
            if (!s.name.equalsIgnoreCase("Move Fix") || !(s instanceof EnumSetting)) continue;
            return ((Enum)((EnumSetting)s).value).ordinal();
        }
        return 0;
    }
}

