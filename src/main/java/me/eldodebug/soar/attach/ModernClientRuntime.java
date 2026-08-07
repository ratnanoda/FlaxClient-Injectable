package me.eldodebug.soar.attach;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import me.eldodebug.soar.attach.modern.ModernImGuiEngine;
import me.eldodebug.soar.attach.modern.ModernMinecraftAccess;
import me.eldodebug.soar.attach.modern.ModernModuleManager;
import me.eldodebug.soar.logger.GlideLogger;

/**
 * Java-8-compatible bridge for Lunar Client 26.1.2 (Java 25).
 *
 * <p>This class intentionally uses reflection so the same embedded jar remains
 * loadable by both the legacy 1.8.9 runtime and the unobfuscated 26.1.2
 * runtime. Version-specific game classes must not leak into its descriptors.
 */
public final class ModernClientRuntime {

    private static final int GLFW_PRESS = 1;
    private static final int GLFW_KEY_ESCAPE = 256;
    private static final int GLFW_KEY_RIGHT_SHIFT = 344;
    private static final AtomicBoolean STARTED = new AtomicBoolean();

    private static volatile Object minecraft;
    private static volatile boolean rightShiftDown;
    private static volatile boolean escapeDown;
    private static volatile boolean welcomePending;
    private static volatile boolean keyInputFailureLogged;
    private static volatile ModernModuleManager moduleManager;
    private static volatile ModernImGuiEngine imGuiEngine;
    private static volatile boolean firstTickLogged;
    private static final Map<Object, Object> XRAY_RENDER_TYPES = Collections.synchronizedMap(
            new WeakHashMap<Object, Object>());

    private ModernClientRuntime() {
    }

    public static void start(Object minecraftInstance) {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        minecraft = minecraftInstance;
        diagnostic("Modern bootstrap entered", null);
        verifyTargetVersion(minecraftInstance);
        diagnostic("Minecraft version verified as 26.1.2", null);
        ModernModuleManager manager = new ModernModuleManager();
        manager.initialize(minecraftInstance);
        moduleManager = manager;
        imGuiEngine = new ModernImGuiEngine(minecraftInstance, manager);
        diagnostic("Module manager and ImGui bridge created", null);
        welcomePending = true;
        GlideLogger.info("FlaxClient attached to Lunar Client 26.1.2");
    }

    public static void onClientTick() {
        Object client = minecraft;
        if (!STARTED.get() || client == null) {
            return;
        }
        if (!firstTickLogged) {
            firstTickLogged = true;
            diagnostic("First transformed client tick received", null);
        }

        if (welcomePending && sendClientMessage(client, "FlaxClient 26.1.2 loaded")) {
            welcomePending = false;
        }

        ModernModuleManager manager = moduleManager;
        if (manager != null && !shouldBlockGameInput()) {
            manager.onTick();
        }

        boolean pressed = isKeyPressed(client, GLFW_KEY_RIGHT_SHIFT);
        if (pressed && !rightShiftDown) {
            ModernImGuiEngine engine = imGuiEngine;
            if (engine != null) {
                setMenuVisible(client, engine, !engine.isMenuVisible());
                diagnostic(
                        "Right Shift toggled ClickGUI to " + engine.isMenuVisible(),
                        null);
            }
        }
        rightShiftDown = pressed;

        boolean escapePressed = isKeyPressed(client, GLFW_KEY_ESCAPE);
        ModernImGuiEngine engine = imGuiEngine;
        if (escapePressed && !escapeDown && engine != null && engine.isMenuVisible()) {
            if (!engine.consumeEscapeCaptured()) {
                setMenuVisible(client, engine, false);
            }
        }
        escapeDown = escapePressed;
    }

    public static void onRenderFrame() {
        ModernModuleManager manager = moduleManager;
        if (manager != null && !shouldBlockGameInput()) {
            manager.onFrame();
        }
        ModernImGuiEngine engine = imGuiEngine;
        if (engine != null) {
            engine.render();
        }
    }

    public static boolean isClientEnabled() {
        return STARTED.get();
    }

    public static boolean onMouseTurn(Object mouseHandler) {
        ModernModuleManager manager = moduleManager;
        return manager != null && manager.onMouseTurn(mouseHandler);
    }

    public static void onCameraUpdate(Object camera) {
        ModernModuleManager manager = moduleManager;
        if (manager != null) manager.onCameraUpdate(camera);
    }

    public static boolean shouldBlockGameInput() {
        ModernImGuiEngine engine = imGuiEngine;
        return engine != null && engine.isMenuVisible();
    }

    public static boolean onKeyboardInput(Object event, int action) {
        ModernImGuiEngine engine = imGuiEngine;
        if (engine == null || !engine.isMenuVisible()) return false;
        try {
            int key = ((Number) ModernMinecraftAccess.invoke(event, "key")).intValue();
            return engine.onKeyInput(key, action);
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    public static Object onPlayerRenderType(Object original, Object renderState) {
        ModernModuleManager manager = moduleManager;
        if (original == null || renderState == null || manager == null
                || !manager.isRealEspEnabled()
                || !renderState.getClass().getName().endsWith(".AvatarRenderState")) {
            return original;
        }
        Object cached = XRAY_RENDER_TYPES.get(original);
        if (cached != null) return cached;
        try {
            Object created = createXrayRenderType(original);
            XRAY_RENDER_TYPES.put(original, created);
            return created;
        } catch (Throwable error) {
            diagnostic("Could not create ESP Real render type", error);
            return original;
        }
    }

    public static void onLocalPlayerTick(Object player) {
        try {
            ModernModuleManager manager = moduleManager;
            if (manager != null && !shouldBlockGameInput()) manager.onLocalPlayerTick(player);
        } catch (Throwable error) {
            diagnostic("LocalPlayer tick hook failed", error);
        }
    }

    public static void onLocalPlayerAiStepHead(Object player) {
        try {
            ModernModuleManager manager = moduleManager;
            if (manager != null) manager.onLocalPlayerAiStepHead(player);
        } catch (Throwable error) {
            diagnostic("LocalPlayer aiStep head hook failed", error);
        }
    }

    public static void onLocalPlayerAiStepTail(Object player) {
        try {
            ModernModuleManager manager = moduleManager;
            if (manager != null) manager.onLocalPlayerAiStepTail(player);
        } catch (Throwable error) {
            diagnostic("LocalPlayer aiStep tail hook failed", error);
        }
    }

    public static void onLocalPlayerSendPositionHead(Object player) {
        try {
            ModernModuleManager manager = moduleManager;
            if (manager != null) manager.onLocalPlayerSendPositionHead(player);
        } catch (Throwable error) {
            diagnostic("LocalPlayer sendPosition head hook failed", error);
        }
    }

    public static void onLocalPlayerSendPositionTail(Object player) {
        try {
            ModernModuleManager manager = moduleManager;
            if (manager != null) manager.onLocalPlayerSendPositionTail(player);
        } catch (Throwable error) {
            diagnostic("LocalPlayer sendPosition tail hook failed", error);
        }
    }

    public static Object onLivingEntityTravel(Object entity, Object movement) {
        try {
            ModernModuleManager manager = moduleManager;
            return manager == null ? movement : manager.onLivingEntityTravel(entity, movement);
        } catch (Throwable error) {
            diagnostic("LivingEntity travel hook failed", error);
            return movement;
        }
    }

    public static synchronized void diagnostic(String message, Throwable error) {
        File directory = new File(System.getProperty("java.io.tmpdir"), "FlaxClient");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File log = new File(directory, "modern.log");
        try (PrintWriter writer = new PrintWriter(new FileWriter(log, true))) {
            writer.println("[" + new java.util.Date() + "] " + message);
            if (error != null) {
                error.printStackTrace(writer);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void verifyTargetVersion(Object client) {
        try {
            ClassLoader loader = client.getClass().getClassLoader();
            Class<?> sharedConstants =
                    Class.forName("net.minecraft.SharedConstants", true, loader);
            Method getCurrentVersion = sharedConstants.getMethod("getCurrentVersion");
            Object worldVersion = getCurrentVersion.invoke(null);
            Method id = worldVersion.getClass().getMethod("id");
            String version = String.valueOf(id.invoke(worldVersion));
            if (!"26.1.2".equals(version)) {
                throw new IllegalStateException(
                        "Expected Lunar Minecraft 26.1.2 but found " + version);
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not verify the Minecraft version", error);
        }
    }

    private static boolean isKeyPressed(Object client, int key) {
        try {
            Object window = ModernMinecraftAccess.invoke(client, "getWindow");
            long handle = ModernMinecraftAccess.windowHandle(window);
            Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW", true, client.getClass().getClassLoader());
            Method getKey = glfw.getMethod("glfwGetKey", long.class, int.class);
            return ((Number) getKey.invoke(null, handle, key)).intValue()
                    == GLFW_PRESS;
        } catch (Throwable unavailable) {
            if (!keyInputFailureLogged) {
                keyInputFailureLogged = true;
                diagnostic("GLFW key input bridge failed", unavailable);
            }
            return false;
        }
    }

    private static void setMenuVisible(
            Object client,
            ModernImGuiEngine engine,
            boolean visible) {
        engine.setMenuVisible(visible);
        try {
            Object mouseHandler = ModernMinecraftAccess.field(client, "mouseHandler");
            if (visible) {
                releaseGameInput(client, mouseHandler);
            }
            ModernMinecraftAccess.invoke(
                    mouseHandler,
                    visible ? "releaseMouse" : "grabMouse");
            setNativeCursorMode(client, visible);
        } catch (ReflectiveOperationException error) {
            GlideLogger.warn("Could not update Lunar mouse capture");
        }
    }

    private static void setNativeCursorMode(Object client, boolean visible) {
        try {
            Object window = ModernMinecraftAccess.invoke(client, "getWindow");
            long handle = ModernMinecraftAccess.windowHandle(window);
            Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW", true,
                    client.getClass().getClassLoader());
            // GLFW_CURSOR / GLFW_CURSOR_NORMAL. Closing is left to
            // MouseHandler.grabMouse(), which also performs Lunar's bookkeeping.
            if (visible) {
                glfw.getMethod("glfwSetInputMode", long.class, int.class, int.class)
                        .invoke(null, handle, 0x00033001, 0x00034001);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object createXrayRenderType(Object original) throws Exception {
        Object setup = ModernMinecraftAccess.field(original, "state");
        Object pipeline = ModernMinecraftAccess.field(setup, "pipeline");
        ClassLoader loader = original.getClass().getClassLoader();

        Class<?> compareOp = Class.forName("com.mojang.blaze3d.platform.CompareOp", true, loader);
        @SuppressWarnings({"rawtypes", "unchecked"})
        Object always = Enum.valueOf((Class<? extends Enum>) compareOp.asSubclass(Enum.class), "ALWAYS_PASS");
        Class<?> depthState = Class.forName("com.mojang.blaze3d.pipeline.DepthStencilState", true, loader);
        Object noDepth = depthState.getConstructor(compareOp, boolean.class).newInstance(always, false);

        Class<?> pipelineClass = pipeline.getClass();
        Constructor<?> pipelineConstructor = pipelineClass.getConstructors()[0];
        for (Constructor<?> candidate : pipelineClass.getConstructors()) {
            if (candidate.getParameterTypes().length == 13) pipelineConstructor = candidate;
        }
        Object xrayPipeline = pipelineConstructor.newInstance(
                ModernMinecraftAccess.field(pipeline, "location"),
                ModernMinecraftAccess.field(pipeline, "vertexShader"),
                ModernMinecraftAccess.field(pipeline, "fragmentShader"),
                ModernMinecraftAccess.field(pipeline, "shaderDefines"),
                ModernMinecraftAccess.field(pipeline, "samplers"),
                ModernMinecraftAccess.field(pipeline, "uniforms"),
                ModernMinecraftAccess.field(pipeline, "colorTargetState"),
                noDepth,
                ModernMinecraftAccess.field(pipeline, "polygonMode"),
                ModernMinecraftAccess.field(pipeline, "cull"),
                ModernMinecraftAccess.field(pipeline, "vertexFormat"),
                ModernMinecraftAccess.field(pipeline, "vertexFormatMode"),
                ModernMinecraftAccess.field(pipeline, "sortKey"));

        Class<?> setupClass = setup.getClass();
        Constructor<?> setupConstructor = null;
        for (Constructor<?> candidate : setupClass.getConstructors()) {
            if (candidate.getParameterTypes().length == 11) setupConstructor = candidate;
        }
        if (setupConstructor == null) throw new NoSuchMethodException("RenderSetup constructor");
        Object xraySetup = setupConstructor.newInstance(
                xrayPipeline,
                ModernMinecraftAccess.field(setup, "textures"),
                ModernMinecraftAccess.field(setup, "useLightmap"),
                ModernMinecraftAccess.field(setup, "useOverlay"),
                ModernMinecraftAccess.field(setup, "layeringTransform"),
                ModernMinecraftAccess.field(setup, "outputTarget"),
                ModernMinecraftAccess.field(setup, "textureTransform"),
                ModernMinecraftAccess.field(setup, "outlineProperty"),
                ModernMinecraftAccess.field(setup, "affectsCrumbling"),
                ModernMinecraftAccess.field(setup, "sortOnUpload"),
                ModernMinecraftAccess.field(setup, "bufferSize"));

        return original.getClass().getConstructor(String.class, setupClass)
                .newInstance("flax_real_esp", xraySetup);
    }

    private static void releaseGameInput(Object client, Object mouseHandler) {
        try {
            Class<?> keyMapping = Class.forName(
                    "net.minecraft.client.KeyMapping", true, client.getClass().getClassLoader());
            keyMapping.getMethod("releaseAll").invoke(null);
        } catch (Throwable ignored) {
        }
        for (String fieldName : new String[] {"isLeftPressed", "isMiddlePressed", "isRightPressed"}) {
            try {
                findField(mouseHandler.getClass(), fieldName).setBoolean(mouseHandler, false);
            } catch (Throwable ignored) {
            }
        }
    }

    private static boolean sendClientMessage(Object client, String text) {
        try {
            Field playerField = findField(client.getClass(), "player");
            Object player = playerField.get(client);
            if (player == null) {
                return false;
            }

            ClassLoader loader = client.getClass().getClassLoader();
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component", true, loader);
            Method literal = componentClass.getMethod("literal", String.class);
            Object component = literal.invoke(null, "\u00a77[\u00a7bFlax\u00a77] \u00a7f" + text);

            Method display = findMethod(
                    player.getClass(),
                    "displayClientMessage",
                    componentClass,
                    boolean.class);
            display.invoke(player, component, false);
            return true;
        } catch (Throwable unavailable) {
            return false;
        }
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // Lunar may subclass Minecraft; continue into the vanilla base.
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameters)
            throws NoSuchMethodException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, parameters);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                // Continue into the vanilla base class.
            }
        }
        throw new NoSuchMethodException(name);
    }
}
