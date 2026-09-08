package me.eldodebug.soar.attach.modern;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Small reflection boundary for Minecraft 26.2.
 *
 * <p>Keeping modern Minecraft types out of method descriptors lets the same
 * FlaxClient jar remain loadable on the established Java 8 / Minecraft 1.8.9
 * path. The original managers and mixins are not initialized in this runtime.
 */
public final class ModernLateHooks {

    private static final String[] MODULES = {
            "Aim Assist", "Auto Clicker", "Bed ESP", "Break Progress",
            "ESP", "Fast Place", "Ghost Freelook", "Ghost Nametags",
            "Healthbar", "Jump Reset", "Safe Walk", "YouTube PiP"
    };
    private static final boolean[] DEFAULTS = {
            false, false, false, false, false, false,
            false, false, false, false, false, false
    };
    private static final boolean[] ENABLED = DEFAULTS.clone();
    private static final Map<Integer, Boolean> KEY_STATES =
            new HashMap<Integer, Boolean>();
    private static final ArrayDeque<Long> LEFT_CLICKS = new ArrayDeque<Long>();
    private static final ArrayDeque<Long> RIGHT_CLICKS = new ArrayDeque<Long>();
    private static final DecimalFormat COORDINATE = new DecimalFormat(
            "0.0", DecimalFormatSymbols.getInstance(Locale.ROOT));
    private static final File CONFIG_FILE = new File(
            new File(System.getProperty("user.home"), ".flaxclient"),
            "dawn-26.2.properties");

    private static Object minecraft;
    private static Method glfwGetKey;
    private static Method glfwGetMouseButton;
    private static long windowHandle;
    private static boolean menuOpen;
    private static int selectedModule;
    private static boolean previousLeftMouse;
    private static boolean previousRightMouse;
    private static Object originalGamma;
    private static Object originalFov;
    private static boolean gammaApplied;
    private static boolean zoomApplied;
    private static boolean reportedFailure;
    private static boolean settingsLoaded;
    private static long nextAutoClickAt;
    private static long nextAimScanAt;
    private static Object aimTarget;
    private static int previousHurtTime;
    private static int jumpDelay;
    private static int jumpCooldown;
    private static boolean jumpArmed;
    private static boolean forcedSneak;
    private static final Set<Object> FLAX_GLOWING = Collections.newSetFromMap(
            new IdentityHashMap<Object, Boolean>());
    private static final Set<Object> FLAX_NAMETAGS = Collections.newSetFromMap(
            new IdentityHashMap<Object, Boolean>());
    private static final List<Object> BEDS = new ArrayList<Object>();
    private static int bedScanTicks;
    private static boolean freelookActive;
    private static float freelookBaseYaw;
    private static float freelookBasePitch;
    private static float freelookYaw;
    private static float freelookPitch;
    private static float lastObservedYaw;
    private static float lastObservedPitch;
    private static Object previousCameraType;

    private ModernLateHooks() {
    }

    public static void onAttached() {
        loadSettings();
        settingsLoaded = true;
        System.out.println("FlaxClient: Dawn/Minecraft 26.2 compatibility runtime attached");
    }

    public static void onClientTick() {
        try {
            if (!settingsLoaded) {
                loadSettings();
                settingsLoaded = true;
            }
            ensureRuntime();
            updateInput();
            runLegacyModules();
        } catch (Throwable error) {
            reportOnce("tick", error);
        }
    }

    public static void onHudRender(Object gui) {
        try {
            ensureRuntime();
            Object state = readField(gui, "guiRenderState");
            Object font = readField(minecraft, "font");
            if (state == null || font == null) {
                return;
            }

            renderLegacyHud(state, font);

            if (menuOpen) {
                renderMenu(state, font);
            }
        } catch (Throwable error) {
            reportOnce("HUD", error);
        }
    }

    public static void onCameraBeforeExtract(Object camera) {
        if (!freelookActive || camera == null) {
            return;
        }
        try {
            invokeDeclared(camera, "setRotation",
                    new Class<?>[] { float.class, float.class },
                    new Object[] { Float.valueOf(freelookYaw), Float.valueOf(freelookPitch) });
        } catch (Throwable error) {
            reportOnce("freelook camera", error);
        }
    }

    private static void ensureRuntime() throws Exception {
        if (minecraft == null) {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            minecraft = minecraftClass.getMethod("getInstance").invoke(null);
        }
        if (windowHandle == 0L && minecraft != null) {
            Object window = invoke(minecraft, "getWindow");
            Object handle = invoke(window, "handle");
            windowHandle = ((Number) handle).longValue();
        }
        if (glfwGetKey == null) {
            Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
            glfwGetKey = glfw.getMethod("glfwGetKey", long.class, int.class);
            glfwGetMouseButton = glfw.getMethod(
                    "glfwGetMouseButton", long.class, int.class);
        }
    }

    private static void updateInput() throws Exception {
        if (pressed(344)) { // GLFW_KEY_RIGHT_SHIFT
            menuOpen = !menuOpen;
        }
        if (menuOpen) {
            if (pressed(265)) { // up
                selectedModule = (selectedModule + MODULES.length - 1) % MODULES.length;
            }
            if (pressed(264)) { // down
                selectedModule = (selectedModule + 1) % MODULES.length;
            }
            if (pressed(257) || pressed(32)) { // enter or space
                ENABLED[selectedModule] = !ENABLED[selectedModule];
                onModuleToggled(MODULES[selectedModule], ENABLED[selectedModule]);
                saveSettings();
            }
            if (pressed(256)) { // escape
                menuOpen = false;
            }
        }

        boolean left = mouseDown(0);
        boolean right = mouseDown(1);
        long now = System.currentTimeMillis();
        if (left && !previousLeftMouse) {
            LEFT_CLICKS.addLast(now);
        }
        if (right && !previousRightMouse) {
            RIGHT_CLICKS.addLast(now);
        }
        previousLeftMouse = left;
        previousRightMouse = right;
        trimClicks();
    }

    private static void runLegacyModules() throws Exception {
        Object player = readField(minecraft, "player");
        Object level = readField(minecraft, "level");
        if (player == null || level == null || readField(minecraft, "screen") != null) {
            releaseTransientState();
            return;
        }

        if (isEnabled("Aim Assist")) {
            try { runAimAssist(player, level); }
            catch (Throwable error) { disableFailedModule("Aim Assist", error); }
        }
        if (isEnabled("Auto Clicker")) {
            try { runAutoClicker(player); }
            catch (Throwable error) { disableFailedModule("Auto Clicker", error); }
        }
        if (isEnabled("Fast Place")) {
            try { runFastPlace(player); }
            catch (Throwable error) { disableFailedModule("Fast Place", error); }
        }
        if (isEnabled("Jump Reset")) {
            try { runJumpReset(player); }
            catch (Throwable error) { disableFailedModule("Jump Reset", error); }
        } else {
            previousHurtTime = intField(player, "hurtTime");
        }
        if (isEnabled("Safe Walk")) {
            try { runSafeWalk(player, level); }
            catch (Throwable error) { disableFailedModule("Safe Walk", error); }
        } else {
            releaseSneak();
        }
        try { runFreelook(player); }
        catch (Throwable error) { disableFailedModule("Ghost Freelook", error); }
        try { updatePlayerVisuals(player, level); }
        catch (Throwable error) { disableFailedModule("ESP", error); }
        if (isEnabled("Bed ESP") && --bedScanTicks <= 0) {
            try { scanBeds(player, level); }
            catch (Throwable error) { disableFailedModule("Bed ESP", error); }
            bedScanTicks = 20;
        } else if (!isEnabled("Bed ESP")) {
            BEDS.clear();
        }
    }

    private static void runAimAssist(Object player, Object level) throws Exception {
        if (!mouseDown(0)) {
            aimTarget = null;
            return;
        }
        long now = System.nanoTime();
        if (aimTarget == null || now >= nextAimScanAt || !validTarget(player, aimTarget, 4.2D, 54.0F)) {
            aimTarget = findTarget(player, level);
            nextAimScanAt = now + 50000000L;
        }
        if (aimTarget == null) {
            return;
        }
        double dx = number(invoke(aimTarget, "getX")) - number(invoke(player, "getX"));
        double dz = number(invoke(aimTarget, "getZ")) - number(invoke(player, "getZ"));
        double targetEye = number(invoke(aimTarget, "getEyeY"));
        double ownEye = number(invoke(player, "getEyeY"));
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float wantedYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float wantedPitch = (float) -Math.toDegrees(Math.atan2(targetEye - ownEye, horizontal));
        float yaw = ((Number) invoke(player, "getYRot")).floatValue();
        float pitch = ((Number) invoke(player, "getXRot")).floatValue();
        float yawStep = clamp(wrap(wantedYaw - yaw) / 7.5F, -0.95F, 0.95F);
        float pitchStep = clamp(wrap(wantedPitch - pitch) / 8.4F, -0.80F, 0.80F);
        invokeWith(player, "setYRot", Float.valueOf(yaw + yawStep));
        invokeWith(player, "setXRot", Float.valueOf(clamp(pitch + pitchStep, -90.0F, 90.0F)));
    }

    private static Object findTarget(Object player, Object level) throws Exception {
        Object players = invoke(level, "players");
        if (!(players instanceof Iterable)) {
            return null;
        }
        Object best = null;
        float bestScore = Float.MAX_VALUE;
        for (Object candidate : (Iterable<?>) players) {
            if (!validTarget(player, candidate, 4.2D, 48.0F)) {
                continue;
            }
            double dx = number(invoke(candidate, "getX")) - number(invoke(player, "getX"));
            double dz = number(invoke(candidate, "getZ")) - number(invoke(player, "getZ"));
            float wanted = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
            float yaw = ((Number) invoke(player, "getYRot")).floatValue();
            float score = Math.abs(wrap(wanted - yaw)) +
                    ((Number) invoke(player, "distanceTo", candidate)).floatValue() * 0.25F;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean validTarget(Object player, Object candidate, double range, float fov) {
        try {
            if (candidate == null || candidate == player ||
                    !Boolean.TRUE.equals(invoke(candidate, "isAlive")) ||
                    Boolean.TRUE.equals(invoke(candidate, "isInvisible"))) {
                return false;
            }
            float distance = ((Number) invoke(player, "distanceTo", candidate)).floatValue();
            if (distance > range || !Boolean.TRUE.equals(invoke(player, "hasLineOfSight", candidate))) {
                return false;
            }
            double dx = number(invoke(candidate, "getX")) - number(invoke(player, "getX"));
            double dz = number(invoke(candidate, "getZ")) - number(invoke(player, "getZ"));
            float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
            float yaw = ((Number) invoke(player, "getYRot")).floatValue();
            return Math.abs(wrap(targetYaw - yaw)) <= fov * 0.5F;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void runAutoClicker(Object player) throws Exception {
        if (!mouseDown(0) || Boolean.TRUE.equals(invoke(player, "isUsingItem"))) {
            nextAutoClickAt = 0L;
            return;
        }
        long now = System.currentTimeMillis();
        if (nextAutoClickAt == 0L) {
            nextAutoClickAt = now + randomClickDelay();
        } else if (now >= nextAutoClickAt) {
            writeField(minecraft, "missTime", Integer.valueOf(0));
            invokeDeclared(minecraft, "startAttack", new Class<?>[0], new Object[0]);
            nextAutoClickAt = now + randomClickDelay();
        }
    }

    private static long randomClickDelay() {
        double cps = ThreadLocalRandom.current().nextDouble(8.0D, 13.0001D);
        return Math.max(35L, Math.round((1000.0D / cps) *
                ThreadLocalRandom.current().nextDouble(0.90D, 1.10D)));
    }

    private static void runFastPlace(Object player) throws Exception {
        if (!mouseDown(1)) {
            return;
        }
        Object stack = invoke(player, "getMainHandItem");
        Object item = stack == null ? null : invoke(stack, "getItem");
        if (item != null && item.getClass().getName().endsWith("BlockItem")) {
            int delay = ThreadLocalRandom.current().nextDouble() < 0.75D ? 0 : 1;
            int current = intField(minecraft, "rightClickDelay");
            if (current > delay) {
                writeField(minecraft, "rightClickDelay", Integer.valueOf(delay));
            }
        }
    }

    private static void runJumpReset(Object player) throws Exception {
        if (jumpCooldown > 0) {
            --jumpCooldown;
        }
        int hurt = intField(player, "hurtTime");
        if (hurt > previousHurtTime && hurt > 0 && jumpCooldown == 0) {
            jumpDelay = 1;
            jumpArmed = true;
        }
        previousHurtTime = hurt;
        if (!jumpArmed) {
            return;
        } else if (jumpDelay > 0) {
            --jumpDelay;
        } else if (hurt > 0 && Boolean.TRUE.equals(invoke(player, "onGround")) && !keyDown(32)) {
            invokeDeclared(player, "jumpFromGround", new Class<?>[0], new Object[0]);
            jumpArmed = false;
            jumpCooldown = 6;
        } else if (hurt <= 0) {
            jumpArmed = false;
        }
    }

    private static void runSafeWalk(Object player, Object level) throws Exception {
        if (!Boolean.TRUE.equals(invoke(player, "onGround"))) {
            releaseSneak();
            return;
        }
        double forward = (keyDown(87) ? 1.0D : 0.0D) - (keyDown(83) ? 1.0D : 0.0D);
        double strafe = (keyDown(68) ? 1.0D : 0.0D) - (keyDown(65) ? 1.0D : 0.0D);
        if (forward == 0.0D && strafe == 0.0D) {
            releaseSneak();
            return;
        }
        double yaw = Math.toRadians(((Number) invoke(player, "getYRot")).doubleValue());
        double dx = strafe * Math.cos(yaw) - forward * Math.sin(yaw);
        double dz = forward * Math.cos(yaw) + strafe * Math.sin(yaw);
        double len = Math.sqrt(dx * dx + dz * dz);
        double x = number(invoke(player, "getX")) + dx / len * 0.42D;
        double y = number(invoke(player, "getY")) - 0.08D;
        double z = number(invoke(player, "getZ")) + dz / len * 0.42D;
        Class<?> blockPos = Class.forName("net.minecraft.core.BlockPos");
        Object pos = blockPos.getMethod("containing", double.class, double.class, double.class)
                .invoke(null, Double.valueOf(x), Double.valueOf(y), Double.valueOf(z));
        boolean gap = Boolean.TRUE.equals(invoke(level, "isEmptyBlock", pos));
        Object keyShift = readField(readField(minecraft, "options"), "keyShift");
        if (gap && !keyDown(340)) {
            invokeWith(keyShift, "setDown", Boolean.TRUE);
            forcedSneak = true;
        } else {
            releaseSneak();
        }
    }

    private static void releaseSneak() throws Exception {
        if (forcedSneak && minecraft != null) {
            Object keyShift = readField(readField(minecraft, "options"), "keyShift");
            invokeWith(keyShift, "setDown", Boolean.valueOf(keyDown(340)));
        }
        forcedSneak = false;
    }

    private static void runFreelook(Object player) throws Exception {
        boolean wanted = isEnabled("Ghost Freelook") && keyDown(86); // V
        Object options = readField(minecraft, "options");
        if (wanted && !freelookActive) {
            freelookActive = true;
            freelookBaseYaw = ((Number) invoke(player, "getYRot")).floatValue();
            freelookBasePitch = ((Number) invoke(player, "getXRot")).floatValue();
            freelookYaw = freelookBaseYaw;
            freelookPitch = freelookBasePitch;
            lastObservedYaw = freelookBaseYaw;
            lastObservedPitch = freelookBasePitch;
            previousCameraType = invoke(options, "getCameraType");
            Class<?> cameraType = Class.forName("net.minecraft.client.CameraType");
            Object thirdPerson = cameraType.getField("THIRD_PERSON_BACK").get(null);
            invokeWith(options, "setCameraType", thirdPerson);
        } else if (!wanted && freelookActive) {
            freelookActive = false;
            if (previousCameraType != null) {
                invokeWith(options, "setCameraType", previousCameraType);
            }
        }
        if (freelookActive) {
            float observedYaw = ((Number) invoke(player, "getYRot")).floatValue();
            float observedPitch = ((Number) invoke(player, "getXRot")).floatValue();
            freelookYaw += wrap(observedYaw - lastObservedYaw);
            freelookPitch = clamp(freelookPitch + observedPitch - lastObservedPitch, -90.0F, 90.0F);
            invokeWith(player, "setYRot", Float.valueOf(freelookBaseYaw));
            invokeWith(player, "setXRot", Float.valueOf(freelookBasePitch));
            lastObservedYaw = freelookBaseYaw;
            lastObservedPitch = freelookBasePitch;
        }
    }

    private static void updatePlayerVisuals(Object localPlayer, Object level) throws Exception {
        Object players = invoke(level, "players");
        if (!(players instanceof Iterable)) {
            return;
        }
        boolean esp = isEnabled("ESP");
        for (Object player : (Iterable<?>) players) {
            if (player == localPlayer) {
                continue;
            }
            if (esp) {
                if (!Boolean.TRUE.equals(invoke(player, "hasGlowingTag"))) {
                    invokeWith(player, "setGlowingTag", Boolean.TRUE);
                    FLAX_GLOWING.add(player);
                }
            } else if (FLAX_GLOWING.remove(player)) {
                invokeWith(player, "setGlowingTag", Boolean.FALSE);
            }
            if (isEnabled("Ghost Nametags")) {
                invokeWith(player, "setCustomNameVisible", Boolean.TRUE);
                FLAX_NAMETAGS.add(player);
            } else if (FLAX_NAMETAGS.remove(player)) {
                invokeWith(player, "setCustomNameVisible", Boolean.FALSE);
            }
        }
        if (!esp && !FLAX_GLOWING.isEmpty()) {
            for (Object entity : new ArrayList<Object>(FLAX_GLOWING)) {
                try {
                    invokeWith(entity, "setGlowingTag", Boolean.FALSE);
                } catch (Throwable ignored) {
                }
            }
            FLAX_GLOWING.clear();
        }
    }

    private static void scanBeds(Object player, Object level) throws Exception {
        BEDS.clear();
        int px = (int) Math.floor(number(invoke(player, "getX")));
        int py = (int) Math.floor(number(invoke(player, "getY")));
        int pz = (int) Math.floor(number(invoke(player, "getZ")));
        Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
        Constructor<?> constructor = blockPosClass.getConstructor(int.class, int.class, int.class);
        for (int x = px - 16; x <= px + 16; ++x) {
            for (int y = py - 8; y <= py + 8; ++y) {
                for (int z = pz - 16; z <= pz + 16; ++z) {
                    Object pos = constructor.newInstance(Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z));
                    Object state = invoke(level, "getBlockState", pos);
                    Object block = invoke(state, "getBlock");
                    if (block != null && block.getClass().getName().endsWith("BedBlock")) {
                        BEDS.add(pos);
                        if (BEDS.size() >= 64) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private static void renderLegacyHud(Object state, Object font) throws Exception {
        Object player = readField(minecraft, "player");
        Object level = readField(minecraft, "level");
        int y = 7;
        addText(state, font, "FlaxClient 26.2 port", 7, y, 0xFF71D6FF);
        y += 11;
        if (isEnabled("Break Progress")) {
            Object gameMode = readField(minecraft, "gameMode");
            float progress = gameMode == null ? 0.0F : floatField(gameMode, "destroyProgress");
            if (progress > 0.0F) {
                int filled = Math.max(0, Math.min(20, Math.round(progress * 20.0F)));
                StringBuilder bar = new StringBuilder("[");
                for (int i = 0; i < 20; ++i) bar.append(i < filled ? '=' : '-');
                bar.append("] ").append(Math.round(progress * 100.0F)).append('%');
                addText(state, font, bar.toString(), 7, y, 0xFF00FFAA);
                y += 10;
            }
        }
        if (isEnabled("Bed ESP") && !BEDS.isEmpty()) {
            addText(state, font, "Bed ESP: " + uniqueBedCount() + " nearby", 7, y, 0xFFFF5A5A);
            y += 10;
        }
        if (player != null && level != null &&
                (isEnabled("Healthbar") || isEnabled("Ghost Nametags"))) {
            Object players = invoke(level, "players");
            if (players instanceof Iterable) {
                int shown = 0;
                for (Object other : (Iterable<?>) players) {
                    if (other == player || shown >= 8) continue;
                    float distance = ((Number) invoke(player, "distanceTo", other)).floatValue();
                    if (distance > 42.0F) continue;
                    Object component = invoke(other, "getName");
                    String name = String.valueOf(invoke(component, "getString"));
                    String suffix = "  " + Math.round(distance) + "m";
                    if (isEnabled("Healthbar")) {
                        float health = ((Number) invoke(other, "getHealth")).floatValue();
                        suffix += "  " + Math.round(health) + " HP";
                    }
                    addText(state, font, name + suffix, 7, y,
                            isEnabled("Healthbar") ? healthColor(other) : 0xFFFFFFFF);
                    y += 10;
                    ++shown;
                }
            }
        }
        if (isEnabled("YouTube PiP")) {
            addText(state, font,
                    "YouTube PiP: media backend is being ported", 7, y, 0xFFFFC857);
        }
    }

    private static int healthColor(Object living) throws Exception {
        float health = ((Number) invoke(living, "getHealth")).floatValue();
        float max = ((Number) invoke(living, "getMaxHealth")).floatValue();
        float ratio = max <= 0.0F ? 0.0F : clamp(health / max, 0.0F, 1.0F);
        int red = (int) ((1.0F - ratio) * 255.0F);
        int green = (int) (ratio * 255.0F);
        return 0xFF000000 | red << 16 | green << 8 | 0x55;
    }

    private static int uniqueBedCount() {
        return (BEDS.size() + 1) / 2;
    }

    private static void onModuleToggled(String module, boolean enabled) {
        if (!enabled && "ESP".equals(module)) {
            for (Object entity : new ArrayList<Object>(FLAX_GLOWING)) {
                try { invokeWith(entity, "setGlowingTag", Boolean.FALSE); } catch (Throwable ignored) { }
            }
            FLAX_GLOWING.clear();
        }
        if (!enabled && "Safe Walk".equals(module)) {
            try { releaseSneak(); } catch (Throwable ignored) { }
        }
        if (!enabled && "Ghost Nametags".equals(module)) {
            for (Object entity : new ArrayList<Object>(FLAX_NAMETAGS)) {
                try { invokeWith(entity, "setCustomNameVisible", Boolean.FALSE); } catch (Throwable ignored) { }
            }
            FLAX_NAMETAGS.clear();
        }
    }

    private static void releaseTransientState() throws Exception {
        nextAutoClickAt = 0L;
        aimTarget = null;
        releaseSneak();
    }

    private static void disableFailedModule(String module, Throwable error) {
        for (int index = 0; index < MODULES.length; ++index) {
            if (MODULES[index].equals(module)) {
                ENABLED[index] = false;
                break;
            }
        }
        System.err.println("FlaxClient 26.2 disabled " + module + ": " + error);
    }

    private static void applyFullbright() throws Exception {
        if (isEnabled("Fullbright")) {
            if (!gammaApplied) {
                originalGamma = readOption("gamma");
                writeOption("gamma", Double.valueOf(1.0D));
                gammaApplied = true;
            }
        } else if (gammaApplied) {
            restoreOption("gamma", originalGamma);
            gammaApplied = false;
        }
    }

    private static void applySprint() throws Exception {
        if (!isEnabled("Toggle Sprint") || !keyDown(87)) { // W
            return;
        }
        Object player = readField(minecraft, "player");
        if (player != null) {
            Method setSprinting = player.getClass().getMethod("setSprinting", boolean.class);
            setSprinting.invoke(player, Boolean.TRUE);
        }
    }

    private static void applyZoom() throws Exception {
        boolean zoom = isEnabled("Zoom") && keyDown(67); // C
        if (zoom && !zoomApplied) {
            originalFov = readOption("fov");
            writeOption("fov", Integer.valueOf(30));
            zoomApplied = true;
        } else if (!zoom && zoomApplied) {
            restoreOption("fov", originalFov);
            zoomApplied = false;
        }
    }

    private static Object readOption(String getter) throws Exception {
        Object options = readField(minecraft, "options");
        Object option = invoke(options, getter);
        return invoke(option, "get");
    }

    private static void writeOption(String getter, Object value) throws Exception {
        Object options = readField(minecraft, "options");
        Object option = invoke(options, getter);
        Method set = option.getClass().getMethod("set", Object.class);
        set.invoke(option, value);
    }

    private static void restoreOption(String getter, Object value) throws Exception {
        if (value != null) {
            writeOption(getter, value);
        }
    }

    private static boolean pressed(int key) throws Exception {
        boolean down = keyDown(key);
        Boolean previous = KEY_STATES.put(Integer.valueOf(key), Boolean.valueOf(down));
        return down && (previous == null || !previous.booleanValue());
    }

    private static boolean keyDown(int key) throws Exception {
        return ((Number) glfwGetKey.invoke(null,
                Long.valueOf(windowHandle), Integer.valueOf(key))).intValue() == 1;
    }

    private static boolean mouseDown(int button) throws Exception {
        return ((Number) glfwGetMouseButton.invoke(null,
                Long.valueOf(windowHandle), Integer.valueOf(button))).intValue() == 1;
    }

    private static String keystrokes() throws Exception {
        return keyLabel(87, "W") + " " + keyLabel(65, "A") + " " +
                keyLabel(83, "S") + " " + keyLabel(68, "D") + "  " +
                (mouseDown(0) ? "[LMB]" : "LMB") + " " +
                (mouseDown(1) ? "[RMB]" : "RMB");
    }

    private static String keyLabel(int key, String label) throws Exception {
        return keyDown(key) ? "[" + label + "]" : label;
    }

    private static void trimClicks() {
        long threshold = System.currentTimeMillis() - 1000L;
        while (!LEFT_CLICKS.isEmpty() && LEFT_CLICKS.peekFirst() < threshold) {
            LEFT_CLICKS.removeFirst();
        }
        while (!RIGHT_CLICKS.isEmpty() && RIGHT_CLICKS.peekFirst() < threshold) {
            RIGHT_CLICKS.removeFirst();
        }
    }

    private static void renderMenu(Object state, Object font) throws Exception {
        int x = 18;
        int y = 65;
        addText(state, font, "FlaxClient Modules", x, y, 0xFF71D6FF);
        y += 13;
        for (int index = 0; index < MODULES.length; ++index) {
            String prefix = index == selectedModule ? "> " : "  ";
            String enabled = ENABLED[index] ? "[ON] " : "[OFF] ";
            int color = index == selectedModule ? 0xFFFFFF74 : 0xFFFFFFFF;
            addText(state, font, prefix + enabled + MODULES[index], x, y, color);
            y += 11;
        }
        addText(state, font, "RShift: close  Up/Down: select  Enter: toggle",
                x, y + 3, 0xFFB8B8B8);
    }

    private static void addText(
            Object state,
            Object font,
            String text,
            int x,
            int y,
            int color) throws Exception {
        Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
        Object component = componentClass.getMethod("literal", String.class)
                .invoke(null, text);
        Object sequence = component.getClass().getMethod("getVisualOrderText")
                .invoke(component);
        Object matrix = Class.forName("org.joml.Matrix3x2f").newInstance();
        Class<?> textStateClass = Class.forName(
                "net.minecraft.client.renderer.state.gui.GuiTextRenderState");
        Constructor<?> constructor = null;
        for (Constructor<?> candidate : textStateClass.getDeclaredConstructors()) {
            if (candidate.getParameterTypes().length == 10) {
                constructor = candidate;
                break;
            }
        }
        if (constructor == null) {
            throw new NoSuchMethodException("GuiTextRenderState constructor");
        }
        constructor.setAccessible(true);
        Object textState = constructor.newInstance(
                font, sequence, matrix,
                Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(color),
                Integer.valueOf(0), Boolean.TRUE, Boolean.FALSE, null);
        Method addText = state.getClass().getMethod("addText", textStateClass);
        addText.invoke(state, textState);
    }

    private static String formatCoordinate(Object player, String method) throws Exception {
        return COORDINATE.format(((Number) invoke(player, method)).doubleValue());
    }

    private static Object invoke(Object target, String method) throws Exception {
        if (target == null) {
            return null;
        }
        return target.getClass().getMethod(method).invoke(target);
    }

    private static Object invoke(Object target, String method, Object argument) throws Exception {
        return invokeWith(target, method, argument);
    }

    private static Object invokeWith(Object target, String method, Object argument) throws Exception {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        while (type != null) {
            for (Method candidate : type.getDeclaredMethods()) {
                if (candidate.getName().equals(method) && candidate.getParameterTypes().length == 1 &&
                        compatible(candidate.getParameterTypes()[0], argument)) {
                    candidate.setAccessible(true);
                    return candidate.invoke(target, argument);
                }
            }
            type = type.getSuperclass();
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + method);
    }

    private static Object invokeDeclared(Object target, String method,
            Class<?>[] parameterTypes, Object[] arguments) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Method found = type.getDeclaredMethod(method, parameterTypes);
                found.setAccessible(true);
                return found.invoke(target, arguments);
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + method);
    }

    private static boolean compatible(Class<?> parameter, Object value) {
        if (value == null) return !parameter.isPrimitive();
        if (parameter.isInstance(value)) return true;
        return (parameter == boolean.class && value instanceof Boolean) ||
                (parameter == int.class && value instanceof Integer) ||
                (parameter == float.class && value instanceof Float) ||
                (parameter == double.class && value instanceof Double) ||
                (parameter == long.class && value instanceof Long);
    }

    private static Object readField(Object target, String name) throws Exception {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static void writeField(Object target, String name, Object value) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static int intField(Object target, String name) throws Exception {
        Object value = readField(target, name);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static float floatField(Object target, String name) throws Exception {
        Object value = readField(target, name);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    private static double number(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0D;
    }

    private static float wrap(float angle) {
        angle %= 360.0F;
        if (angle >= 180.0F) angle -= 360.0F;
        if (angle < -180.0F) angle += 360.0F;
        return angle;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static boolean isEnabled(String name) {
        for (int index = 0; index < MODULES.length; ++index) {
            if (MODULES[index].equals(name)) {
                return ENABLED[index];
            }
        }
        return false;
    }

    private static void loadSettings() {
        Properties properties = new Properties();
        if (CONFIG_FILE.isFile()) {
            try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
                properties.load(input);
            } catch (Exception error) {
                reportOnce("settings load", error);
            }
        }
        for (int index = 0; index < MODULES.length; ++index) {
            ENABLED[index] = Boolean.parseBoolean(properties.getProperty(
                    key(MODULES[index]), Boolean.toString(DEFAULTS[index])));
        }
    }

    private static void saveSettings() {
        Properties properties = new Properties();
        for (int index = 0; index < MODULES.length; ++index) {
            properties.setProperty(key(MODULES[index]),
                    Boolean.toString(ENABLED[index]));
        }
        File parent = CONFIG_FILE.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            return;
        }
        try (FileOutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "FlaxClient Dawn/Minecraft 26.2 settings");
        } catch (Exception error) {
            reportOnce("settings save", error);
        }
    }

    private static String key(String module) {
        return module.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static void reportOnce(String stage, Throwable error) {
        if (!reportedFailure) {
            reportedFailure = true;
            System.err.println("FlaxClient 26.2 " + stage + " failed: " + error);
            error.printStackTrace(System.err);
        }
    }
}
