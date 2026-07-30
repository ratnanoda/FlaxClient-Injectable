package me.eldodebug.soar.attach;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import me.eldodebug.soar.management.mods.impl.WaveyCapesMod;
import me.eldodebug.soar.management.mods.impl.skin3d.render.CustomizableModelPart;
import me.eldodebug.soar.management.mods.impl.waveycapes.sim.StickSimulation;
import me.eldodebug.soar.management.mods.impl.waveycapes.sim.StickSimulation.Point;
import me.eldodebug.soar.management.mods.impl.waveycapes.sim.StickSimulation.Stick;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

/**
 * State formerly injected as new fields into EntityPlayer by Mixin.
 */
public final class PlayerState {

    private static final Map<EntityPlayer, Data> PLAYERS =
            Collections.synchronizedMap(new WeakHashMap<EntityPlayer, Data>());

    private PlayerState() {
    }

    private static Data data(EntityPlayer player) {
        Data state = PLAYERS.get(player);
        if (state == null) {
            state = new Data();
            PLAYERS.put(player, state);
        }
        return state;
    }

    public static CustomizableModelPart getHeadLayer(EntityPlayer player) {
        return data(player).headLayer;
    }

    public static void setHeadLayer(EntityPlayer player, CustomizableModelPart headLayer) {
        data(player).headLayer = headLayer;
    }

    public static CustomizableModelPart[] getSkinLayers(EntityPlayer player) {
        return data(player).skinLayers;
    }

    public static void setSkinLayers(EntityPlayer player, CustomizableModelPart[] skinLayers) {
        data(player).skinLayers = skinLayers;
    }

    public static StickSimulation getSimulation(EntityPlayer player) {
        return data(player).simulation;
    }

    public static void updateSimulation(EntityPlayer player, int partCount) {
        StickSimulation simulation = getSimulation(player);
        boolean dirty = false;

        if (simulation.points.size() != partCount) {
            simulation.points.clear();
            simulation.sticks.clear();

            for (int i = 0; i < partCount; i++) {
                Point point = new Point();
                point.position.y = -i;
                point.locked = i == 0;
                simulation.points.add(point);
                if (i > 0) {
                    simulation.sticks.add(new Stick(simulation.points.get(i - 1), point, 1.0F));
                }
            }
            dirty = true;
        }

        if (dirty) {
            for (int i = 0; i < 10; i++) {
                simulate(player);
            }
        }
    }

    public static void simulate(EntityPlayer player) {
        StickSimulation simulation = getSimulation(player);
        if (simulation.points.isEmpty()) {
            return;
        }

        simulation.points.get(0).prevPosition.copy(simulation.points.get(0).position);
        double d = player.chasingPosX - player.posX;
        double m = player.chasingPosZ - player.posZ;
        float n = player.prevRenderYawOffset + player.renderYawOffset - player.prevRenderYawOffset;
        double o = Math.sin(n * 0.017453292F);
        double p = -Math.cos(n * 0.017453292F);
        float heightMultiplier = WaveyCapesMod.getInstance().getHeightMultiplierSetting().getValueInt();
        double fallHack = MathHelper.clamp_double(
                simulation.points.get(0).position.y - player.posY * heightMultiplier, 0.0D, 1.0D);

        simulation.points.get(0).position.x += d * o + m * p + fallHack;
        simulation.points.get(0).position.y =
                (float) (player.posY * heightMultiplier + (player.isSneaking() ? -4 : 0));
        simulation.simulate();
    }

    private static final class Data {
        private CustomizableModelPart headLayer;
        private CustomizableModelPart[] skinLayers;
        private final StickSimulation simulation = new StickSimulation();
    }
}
