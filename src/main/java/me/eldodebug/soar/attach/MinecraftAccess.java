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
 * loaded class. Keeping these accesses here lets the same client code run both
 * as a normal Forge mod (MCP names) and after late DLL attachment (notch names).
 */
public final class MinecraftAccess {

    private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();
    private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();

    private MinecraftAccess() {
    }

    public static Timer getTimer(Minecraft minecraft) {
        return get(minecraft, "timer", "Y");
    }

    public static void setSession(Minecraft minecraft, Session session) {
        set(minecraft, session, "session", "ae");
    }

    public static void clickMouse(Minecraft minecraft) {
        invoke(minecraft, new Class<?>[0], new Object[0], "clickMouse", "aw");
    }

    public static void rightClickMouse(Minecraft minecraft) {
        invoke(minecraft, new Class<?>[0], new Object[0], "rightClickMouse", "ax");
    }

    public static int getLeftClickCounter(Minecraft minecraft) {
        return (Integer) get(minecraft, "leftClickCounter", "ag");
    }

    public static void setLeftClickCounter(Minecraft minecraft, int counter) {
        set(minecraft, counter, "leftClickCounter", "ag");
    }

    public static int getRightClickDelayTimer(Minecraft minecraft) {
        return (Integer) get(minecraft, "rightClickDelayTimer", "ap");
    }

    public static void setRightClickDelayTimer(Minecraft minecraft, int delay) {
        set(minecraft, delay, "rightClickDelayTimer", "ap");
    }

    public static DefaultResourcePack getDefaultResourcePack(Minecraft minecraft) {
        return get(minecraft, "mcDefaultResourcePack", "aB");
    }

    public static void resize(Minecraft minecraft, int width, int height) {
        invoke(minecraft, new Class<?>[]{int.class, int.class}, new Object[]{width, height}, "resize", "a");
    }

    public static Entity getRenderViewEntity(Minecraft minecraft) {
        return get(minecraft, "renderViewEntity", "ad");
    }

    public static double getRenderPosX(RenderManager renderManager) {
        return (Double) get(renderManager, "renderPosX", "o");
    }

    public static double getRenderPosY(RenderManager renderManager) {
        return (Double) get(renderManager, "renderPosY", "p");
    }

    public static double getRenderPosZ(RenderManager renderManager) {
        return (Double) get(renderManager, "renderPosZ", "q");
    }

    @SuppressWarnings("unchecked")
    public static List<Shader> getShaders(ShaderGroup shaderGroup) {
        return (List<Shader>) get(shaderGroup, "listShaders", "d");
    }

    public static int getUpdateCounter(GuiIngame guiIngame) {
        return (Integer) get(guiIngame, "updateCounter", "n");
    }

    public static int getRemainingHighlightTicks(GuiIngame guiIngame) {
        return (Integer) get(guiIngame, "remainingHighlightTicks", "r");
    }

    public static ItemStack getHighlightingItemStack(GuiIngame guiIngame) {
        return get(guiIngame, "highlightingItemStack", "s");
    }

    public static float getCurBlockDamage(PlayerControllerMP controller) {
        return (Float) get(controller, "curBlockDamageMP", "e");
    }

    public static int getPotionId(ItemFood food) {
        return (Integer) get(food, "potionId", "l");
    }

    public static boolean isChunkLoaded(World world, int x, int z, boolean allowEmpty) {
        return (Boolean) invoke(world, new Class<?>[]{int.class, int.class, boolean.class},
                new Object[]{x, z, allowEmpty}, "isChunkLoaded", "a");
    }

    public static int getEntityId(S14PacketEntity packet) {
        return (Integer) get(packet, "entityId", "a");
    }

    public static byte getPosX(S14PacketEntity packet) {
        return (Byte) get(packet, "posX", "b");
    }

    public static byte getPosY(S14PacketEntity packet) {
        return (Byte) get(packet, "posY", "c");
    }

    public static byte getPosZ(S14PacketEntity packet) {
        return (Byte) get(packet, "posZ", "d");
    }

    public static WorldClient getWorld(RenderGlobal renderGlobal) {
        return get(renderGlobal, "theWorld", "k");
    }

    public static boolean hasThinArms(RenderPlayer renderPlayer) {
        return (Boolean) get(renderPlayer, "smallArms", "a");
    }

    public static NetworkPlayerInfo getPlayerInfo(AbstractClientPlayer player) {
        return getFrom(
                AbstractClientPlayer.class,
                player,
                "playerInfo",
                "a");
    }

    public static RenderPlayer getDeadmau5PlayerRenderer(
            LayerDeadmau5Head layer) {
        return getFrom(
                LayerDeadmau5Head.class,
                layer,
                "playerRenderer",
                "a");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, RenderPlayer> getPlayerRenderers(RenderManager manager) {
        return (Map<String, RenderPlayer>) get(manager, "skinMap", "l");
    }

    @SuppressWarnings("unchecked")
    public static List<LayerRenderer<?>> getLayers(RendererLivingEntity<?> renderer) {
        return (List<LayerRenderer<?>>) get(renderer, "layerRenderers", "h");
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
