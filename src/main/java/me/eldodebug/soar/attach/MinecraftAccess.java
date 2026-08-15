package me.eldodebug.soar.attach;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.layers.LayerDeadmau5Head;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.util.Session;
import net.minecraft.util.Timer;
import net.minecraft.world.World;
import net.minecraft.util.IChatComponent;

/**
 * Accesses vanilla 1.8.9 state without requiring Mixin-added interfaces.
 *
 * JVM retransformation cannot add fields, methods, or interfaces to an already
 * loaded class. Keeping these accesses here lets the same client code run as a
 * Forge mod (SRG names), in development (MCP names), and after late DLL
 * attachment (notch names).
 */
public final class MinecraftAccess {

    private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();
    private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();

    private MinecraftAccess() {
    }

    public static Timer getTimer(Minecraft minecraft) {
        return get(minecraft, "timer", "field_71428_T", "Y");
    }

    public static void setSession(Minecraft minecraft, Session session) {
        set(minecraft, session, "session", "field_71449_j", "ae");
    }

    public static void clickMouse(Minecraft minecraft) {
        invoke(minecraft, new Class<?>[0], new Object[0], "clickMouse", "func_147116_af", "aw");
    }

    public static void rightClickMouse(Minecraft minecraft) {
        invoke(minecraft, new Class<?>[0], new Object[0], "rightClickMouse", "func_147121_ag", "ax");
    }

    public static int getLeftClickCounter(Minecraft minecraft) {
        return (Integer) get(minecraft, "leftClickCounter", "field_71429_W", "ag");
    }

    public static void setLeftClickCounter(Minecraft minecraft, int counter) {
        set(minecraft, counter, "leftClickCounter", "field_71429_W", "ag");
    }

    public static int getRightClickDelayTimer(Minecraft minecraft) {
        return (Integer) get(minecraft, "rightClickDelayTimer", "field_71467_ac", "ap");
    }

    public static void setRightClickDelayTimer(Minecraft minecraft, int delay) {
        set(minecraft, delay, "rightClickDelayTimer", "field_71467_ac", "ap");
    }

    public static DefaultResourcePack getDefaultResourcePack(Minecraft minecraft) {
        return get(minecraft, "mcDefaultResourcePack", "field_110450_ap", "aB");
    }

    public static void resize(Minecraft minecraft, int width, int height) {
        invoke(minecraft, new Class<?>[]{int.class, int.class}, new Object[]{width, height},
                "resize", "func_71370_a", "a");
    }

    public static Entity getRenderViewEntity(Minecraft minecraft) {
        return get(minecraft, "renderViewEntity", "field_175622_Z", "ad");
    }

    public static double getRenderPosX(RenderManager renderManager) {
        return (Double) get(renderManager, "renderPosX", "field_78725_b", "o");
    }

    public static double getRenderPosY(RenderManager renderManager) {
        return (Double) get(renderManager, "renderPosY", "field_78726_c", "p");
    }

    public static double getRenderPosZ(RenderManager renderManager) {
        return (Double) get(renderManager, "renderPosZ", "field_78723_d", "q");
    }

    @SuppressWarnings("unchecked")
    public static List<Shader> getShaders(ShaderGroup shaderGroup) {
        return (List<Shader>) get(shaderGroup, "listShaders", "field_148031_d", "d");
    }

    public static int getUpdateCounter(GuiIngame guiIngame) {
        return (Integer) get(guiIngame, "updateCounter", "field_73837_f", "n");
    }

    public static int getRemainingHighlightTicks(GuiIngame guiIngame) {
        return (Integer) get(guiIngame, "remainingHighlightTicks", "field_92017_k", "r");
    }

    public static ItemStack getHighlightingItemStack(GuiIngame guiIngame) {
        return get(guiIngame, "highlightingItemStack", "field_92016_l", "s");
    }

    public static float getCurBlockDamage(PlayerControllerMP controller) {
        return (Float) get(controller, "curBlockDamageMP", "field_78770_f", "e");
    }

    public static int getPotionId(ItemFood food) {
        return (Integer) get(food, "potionId", "field_77851_ca", "l");
    }

    public static boolean isChunkLoaded(World world, int x, int z, boolean allowEmpty) {
        return (Boolean) invoke(world, new Class<?>[]{int.class, int.class, boolean.class},
                new Object[]{x, z, allowEmpty}, "isChunkLoaded", "func_175680_a", "a");
    }

    public static int getEntityId(S14PacketEntity packet) {
        return (Integer) get(packet, "entityId", "field_149074_a", "a");
    }

    public static byte getPosX(S14PacketEntity packet) {
        return (Byte) get(packet, "posX", "field_149072_b", "b");
    }

    public static byte getPosY(S14PacketEntity packet) {
        return (Byte) get(packet, "posY", "field_149073_c", "c");
    }

    public static byte getPosZ(S14PacketEntity packet) {
        return (Byte) get(packet, "posZ", "field_149070_d", "d");
    }

    public static WorldClient getWorld(RenderGlobal renderGlobal) {
        return get(renderGlobal, "theWorld", "field_72769_h", "k");
    }

    public static boolean hasThinArms(RenderPlayer renderPlayer) {
        return (Boolean) get(renderPlayer, "smallArms", "field_177140_a", "a");
    }

    public static NetworkPlayerInfo getPlayerInfo(AbstractClientPlayer player) {
        return getFrom(
                AbstractClientPlayer.class,
                player,
                "playerInfo",
                "field_175157_a",
                "a");
    }

    public static RenderPlayer getDeadmau5PlayerRenderer(
            LayerDeadmau5Head layer) {
        return getFrom(
                LayerDeadmau5Head.class,
                layer,
                "playerRenderer",
                "field_177208_a",
                "a");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, RenderPlayer> getPlayerRenderers(RenderManager manager) {
        return (Map<String, RenderPlayer>) get(manager, "skinMap", "field_178636_l", "l");
    }

    @SuppressWarnings("unchecked")
    public static List<LayerRenderer<?>> getLayers(RendererLivingEntity<?> renderer) {
        return (List<LayerRenderer<?>>) get(renderer, "layerRenderers", "field_177097_h", "h");
    }

    public static NetworkPlayerInfo getChatPlayerInfo(ChatLine line) {
        NetHandlerPlayClient netHandler = Minecraft.getMinecraft().getNetHandler();
        if (netHandler == null || line == null) {
            return null;
        }

        IChatComponent component = line.getChatComponent();
        if (component == null) {
            return null;
        }

        Map<String, NetworkPlayerInfo> nicknames = new HashMap<>();
        try {
            for (String word : component.getFormattedText().split("(ﾂｧ.)|\\W")) {
                if (word.isEmpty()) {
                    continue;
                }

                NetworkPlayerInfo info = netHandler.getPlayerInfo(word);
                if (info != null) {
                    return info;
                }

                if (nicknames.isEmpty()) {
                    for (NetworkPlayerInfo candidate : netHandler.getPlayerInfoMap()) {
                        IChatComponent displayName = candidate.getDisplayName();
                        if (displayName != null) {
                            String nickname = displayName.getUnformattedTextForChat();
                            nicknames.put(nickname, candidate);
                        }
                    }
                }

                info = nicknames.get(word);
                if (info != null) {
                    return info;
                }
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T get(Object owner, String... names) {
        try {
            return (T) findField(owner.getClass(), names).get(owner);
        } catch (ReflectiveOperationException e) {
            throw accessFailure(owner, names, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getFrom(
            Class<?> declaringType,
            Object owner,
            String... names) {
        try {
            return (T) findField(declaringType, names).get(owner);
        } catch (ReflectiveOperationException e) {
            throw accessFailure(owner, names, e);
        }
    }

    private static void set(Object owner, Object value, String... names) {
        try {
            findField(owner.getClass(), names).set(owner, value);
        } catch (ReflectiveOperationException e) {
            throw accessFailure(owner, names, e);
        }
    }

    private static Object invoke(Object owner, Class<?>[] parameterTypes, Object[] args, String... names) {
        try {
            return findMethod(owner.getClass(), parameterTypes, names).invoke(owner, args);
        } catch (ReflectiveOperationException e) {
            throw accessFailure(owner, names, e);
        }
    }

    private static Field findField(Class<?> type, String... names) throws NoSuchFieldException {
        String key = type.getName() + "#F#" + join(names);
        Field cached = FIELDS.get(key);
        if (cached != null) {
            return cached;
        }

        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (String name : names) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    FIELDS.put(key, field);
                    return field;
                } catch (NoSuchFieldException ignored) {
                }
            }
        }
        throw new NoSuchFieldException(key);
    }

    private static Method findMethod(Class<?> type, Class<?>[] parameterTypes, String... names)
            throws NoSuchMethodException {
        String key = type.getName() + "#M#" + join(names) + "#" + parameterTypes.length;
        Method cached = METHODS.get(key);
        if (cached != null) {
            return cached;
        }

        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (String name : names) {
                try {
                    Method method = current.getDeclaredMethod(name, parameterTypes);
                    method.setAccessible(true);
                    METHODS.put(key, method);
                    return method;
                } catch (NoSuchMethodException ignored) {
                }
            }
        }
        throw new NoSuchMethodException(key);
    }

    private static IllegalStateException accessFailure(Object owner, String[] names, Exception cause) {
        return new IllegalStateException("Cannot access " + owner.getClass().getName() + " member " + join(names), cause);
    }

    private static String join(String[] names) {
        StringBuilder result = new StringBuilder();
        for (String name : names) {
            if (result.length() > 0) {
                result.append('/');
            }
            result.append(name);
        }
        return result.toString();
    }
}
