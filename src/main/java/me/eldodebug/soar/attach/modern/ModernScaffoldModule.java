package me.eldodebug.soar.attach.modern;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import me.eldodebug.soar.attach.modern.ModernSetting.BooleanSetting;
import me.eldodebug.soar.attach.modern.ModernSetting.ComboSetting;
import me.eldodebug.soar.attach.modern.ModernSetting.NumberSetting;

/** Minecraft 26.1.2 reflection port of UsefulTools' Scaffold module. */
public final class ModernScaffoldModule extends ModernModule {

    private final NumberSetting places = setting(new NumberSetting("places", "Places", 1.0, 1.0, 5.0));
    private final NumberSetting extend = setting(new NumberSetting("extend", "Extend", 1.0, -5.0, 5.0));
    private final BooleanSetting extendOnly = setting(new BooleanSetting("extend_only", "Extend Only", false));
    private final BooleanSetting lockTimeBefore = setting(new BooleanSetting("lock_time_before", "Lock Time Before", false));
    private final NumberSetting beforeTime = setting(new NumberSetting("before_time", "Before Time", 100.0, 0.0, 1000.0));
    private final ComboSetting safeWalkMode = setting(new ComboSetting(
            "safe_walk_mode", "SafeWalk Mode", "None", "None", "Normal", "Sneak"));
    private final NumberSetting edgeDistanceForward = setting(new NumberSetting(
            "edge_distance_forward", "Edge Distance Forward", 0.15, 0.01, 0.5));
    private final NumberSetting edgeDistanceSideways = setting(new NumberSetting(
            "edge_distance_sideways", "Edge Distance Sideways", 0.15, 0.01, 0.5));
    private final NumberSetting sneakReleaseDelay = setting(new NumberSetting(
            "sneak_release_delay", "Sneak Release Delay", 2.0, 0.0, 10.0));
    private final BooleanSetting beforeTimeOnly = setting(new BooleanSetting(
            "before_time_only", "Before Time Only", false));
    private final NumberSetting placeDelay = setting(new NumberSetting(
            "place_delay", "Place Delay", 0.0, 0.0, 1000.0));
    private final NumberSetting lookTime = setting(new NumberSetting(
            "look_time", "Look Time", 100.0, 0.0, 1000.0));
    private final ComboSetting switchMode = setting(new ComboSetting(
            "switch_mode", "Switch Mode", "Spoof", "None", "Full", "Spoof", "Fake", "FullReverse"));
    private final BooleanSetting switchTime = setting(new BooleanSetting(
            "switch_time", "Switch Time", false));
    private final NumberSetting switchTimeValue = setting(new NumberSetting(
            "switch_time_value", "Switch Time Value", 200.0, 0.0, 2000.0));
    private final ComboSetting rotMode = setting(new ComboSetting(
            "rot_mode", "Rot Mode", "Back", "None", "Normal", "Down", "Back", "Backwards",
            "Hive", "HypixelBack", "HypixelSideways"));
    private final ComboSetting sprintMode = setting(new ComboSetting(
            "sprint_mode", "Sprint Mode", "None", "None", "Vanilla"));
    private final ComboSetting towerMode = setting(new ComboSetting(
            "tower_mode", "Tower Mode", "Velocity", "None", "Vanilla", "Velocity"));
    private final ComboSetting swingMode = setting(new ComboSetting(
            "swing_mode", "Swing Mode", "Normal", "None", "Normal", "Silent"));
    private final ComboSetting moveFix = setting(new ComboSetting(
            "move_fix", "Move Fix", "None", "None", "Simple", "Silent", "Test"));
    private final BooleanSetting fakeBack = setting(new BooleanSetting("fake_back", "Fake Back", false));
    private final BooleanSetting lockY = setting(new BooleanSetting("lock_y", "Lock Y", true));
    private final NumberSetting diagonalRange = setting(new NumberSetting(
            "diagonal_range", "Diagonal Range", 1.0, 1.0, 10.0));

    private Object minecraft;
    private double startY;
    private double placeAccumulator;
    private long lastPlaceTime;
    private int lastProcessedTick = -1;
    private float serverYaw;
    private float serverPitch;
    private float requestedYaw;
    private float requestedPitch;
    private float lastLookYaw;
    private float lastLookPitch;
    private long lookTimer;
    private boolean rotating;
    private int originalSlot = -1;
    private int lastSentSlot = -1;
    private int lastClientSelectedSlot = -1;
    private boolean spoofingActive;
    private long targetLockStartTime;
    private boolean currentlyInBeforeTime;
    private int sneakReleaseTicks;
    private boolean forcedSneak;
    private float actualYaw;
    private float actualPitch;
    private boolean rotationSpoofing;
    private boolean keysSpoofed;
    private boolean realUp;
    private boolean realDown;
    private boolean realLeft;
    private boolean realRight;
    private float lastServerYaw;
    private int rotationUpdateTick = -1;
    private boolean cameraReady;
    private float cameraYaw;
    private float cameraPitch;

    public ModernScaffoldModule() {
        super("scaffold", "Scaffold", "Automatically places blocks under you.",
                ModernCategory.BLATANT, false);
    }

    @Override
    public void onEnable(Object client) {
        minecraft = client;
        try {
            Object player = player();
            if (player != null) startY = Math.floor(number(player, "getY"));
        } catch (Throwable ignored) {
        }
        placeAccumulator = 0.0;
        lastPlaceTime = 0L;
        lastProcessedTick = -1;
        rotating = false;
        originalSlot = -1;
        lastSentSlot = -1;
        lastClientSelectedSlot = -1;
        spoofingActive = false;
        targetLockStartTime = 0L;
        currentlyInBeforeTime = false;
        sneakReleaseTicks = 0;
        forcedSneak = false;
        rotationSpoofing = false;
        keysSpoofed = false;
        cameraReady = false;
        try {
            Object player = player();
            if (player != null) {
                serverYaw = (float) number(player, "getYRot");
                serverPitch = (float) number(player, "getXRot");
                lastServerYaw = serverYaw;
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onDisable(Object client) {
        minecraft = client;
        restoreHotbarSlot(true);
        restoreCameraRotation();
        rotating = false;
        rotationSpoofing = false;
        restoreMovementKeys();
        releaseSneak();
    }

    @Override
    public void onTick(Object client) {
        minecraft = client;
        try {
            Object player = player();
            Object level = level();
            if (player == null || level == null || screenOpen()) return;
            int tick = ((Number) ModernMinecraftAccess.field(player, "tickCount")).intValue();
            if (tick == lastProcessedTick) return;
            lastProcessedTick = tick;

            int clientSlot = getSelectedSlot();
            boolean clientSlotChanged = lastClientSelectedSlot != -1
                    && lastClientSelectedSlot != clientSlot;
            lastClientSelectedSlot = clientSlot;
            boolean moving = isMoving();
            boolean jumping = keyDown("keyJump");
            if (!lockY.getValue() || !moving
                    || (jumping && Boolean.TRUE.equals(ModernMinecraftAccess.invoke(player, "onGround")))) {
                startY = Math.floor(number(player, "getY"));
            }

            BlockData current = findIntelligentFoundation(moving);
            applyRotation(current);
            if (rotating) {
                ensureCameraRotation(player);
                setRotations(requestedYaw, requestedPitch);
                applyPlayerRotation(player);
            } else if (cameraReady) {
                restoreCameraRotation();
            }
            boolean allowPlacement = true;
            currentlyInBeforeTime = false;
            if (lockTimeBefore.getValue() && current != null) {
                boolean newCycle = System.currentTimeMillis() - lastPlaceTime > 500L;
                if (newCycle) {
                    if (targetLockStartTime == 0L) targetLockStartTime = System.currentTimeMillis();
                    if (System.currentTimeMillis() - targetLockStartTime < beforeTime.getValue().longValue()) {
                        allowPlacement = false;
                        currentlyInBeforeTime = true;
                    }
                } else {
                    targetLockStartTime = 0L;
                }
            } else if (current == null) {
                targetLockStartTime = 0L;
            }

            handleSafeWalkPhysics();
            long now = System.currentTimeMillis();
            if (allowPlacement && now - lastPlaceTime >= placeDelay.getValue().longValue()) {
                placeAccumulator += places.getValue();
                while (placeAccumulator >= 1.0) {
                    BlockData data = findIntelligentFoundation(moving);
                    if (data == null) {
                        placeAccumulator = 0.0;
                        break;
                    }
                    int slot = findBlockInHotbar();
                    if (slot < 0) {
                        placeAccumulator = 0.0;
                        break;
                    }
                    handleFakeSwitch(slot, clientSlotChanged);
                    if (!placeBlock(data, slot)) {
                        placeAccumulator = 0.0;
                        break;
                    }
                    lastPlaceTime = System.currentTimeMillis();
                    lookTimer = lastPlaceTime + lookTime.getValue().longValue();
                    targetLockStartTime = 0L;
                    currentlyInBeforeTime = false;
                    placeAccumulator -= 1.0;
                }
            }

            if (switchTime.getValue()
                    && System.currentTimeMillis() - lastPlaceTime >= switchTimeValue.getValue().longValue()) {
                restoreHotbarSlot(false);
            } else if (!switchTime.getValue() && placeAccumulator < 1.0) {
                restoreHotbarSlot(false);
            }
            if (jumping) handleTower();
            if ("None".equals(sprintMode.getValue())) {
                ModernMinecraftAccess.invoke(player, "setSprinting", false);
            }
        } catch (Throwable ignored) {
        }
    }

    private void handleSafeWalkPhysics() throws Exception {
        if ("None".equals(safeWalkMode.getValue())) {
            releaseSneak();
            return;
        }
        if (lockTimeBefore.getValue() && beforeTimeOnly.getValue() && !currentlyInBeforeTime) {
            releaseSneak();
            return;
        }
        Object player = player();
        if ("Normal".equals(safeWalkMode.getValue())) {
            Object velocity = ModernMinecraftAccess.invoke(player, "getDeltaMovement");
            double vx = fieldNumber(velocity, "x");
            double vy = fieldNumber(velocity, "y");
            double vz = fieldNumber(velocity, "z");
            Object nextBelow = blockPos(number(player, "getX") + vx,
                    number(player, "getY") - 1.0, number(player, "getZ") + vz);
            if (isReplaceable(nextBelow)) {
                ModernMinecraftAccess.invoke(player, "setDeltaMovement", 0.0, vy, 0.0);
            }
            return;
        }

        Object playerPos = ModernMinecraftAccess.invoke(player, "blockPosition");
        double px = number(player, "getX");
        double pz = number(player, "getZ");
        double fracX = px - Math.floor(px);
        double fracZ = pz - Math.floor(pz);
        float yaw = wrap(viewYaw());
        if (yaw < 0.0f) yaw += 360.0f;
        double thresholdX = edgeDistanceSideways.getValue();
        double thresholdZ = edgeDistanceForward.getValue();
        if (!((yaw >= 315.0f || yaw < 45.0f) || (yaw >= 135.0f && yaw < 225.0f))) {
            thresholdX = edgeDistanceForward.getValue();
            thresholdZ = edgeDistanceSideways.getValue();
        }
        boolean shouldSneak = false;
        if (fracX <= thresholdX && isReplaceable(relative(relative(playerPos, "WEST"), "DOWN"))) shouldSneak = true;
        if (1.0 - fracX <= thresholdX && isReplaceable(relative(relative(playerPos, "EAST"), "DOWN"))) shouldSneak = true;
        if (fracZ <= thresholdZ && isReplaceable(relative(relative(playerPos, "NORTH"), "DOWN"))) shouldSneak = true;
        if (1.0 - fracZ <= thresholdZ && isReplaceable(relative(relative(playerPos, "SOUTH"), "DOWN"))) shouldSneak = true;
        if (shouldSneak) {
            setShift(true);
            forcedSneak = true;
            sneakReleaseTicks = 0;
        } else if (forcedSneak) {
            sneakReleaseTicks++;
            if (sneakReleaseTicks >= sneakReleaseDelay.getValue().intValue()) releaseSneak();
        }
    }

    private boolean placeBlock(BlockData data, int slot) throws Exception {
        Object player = player();
        int oldSlot = getSelectedSlot();
        String mode = switchMode.getValue();
        if ("Full".equals(mode) || "FullReverse".equals(mode) || "Spoof".equals(mode)) {
            if (originalSlot == -1) originalSlot = oldSlot;
            setSelectedSlot(slot);
        }
        Object hit = createHitResult(data);
        Object gameMode = ModernMinecraftAccess.field(minecraft, "gameMode");
        Object hand = enumValue("net.minecraft.world.InteractionHand", "MAIN_HAND");
        Object result = ModernMinecraftAccess.invoke(gameMode, "useItemOn", player, hand, hit);
        boolean success = Boolean.TRUE.equals(ModernMinecraftAccess.invoke(result, "consumesAction"));
        if (success) {
            if ("Normal".equals(swingMode.getValue())) {
                ModernMinecraftAccess.invoke(player, "swing", hand);
            } else if ("Silent".equals(swingMode.getValue())) {
                sendPacket(newInstance("net.minecraft.network.protocol.game.ServerboundSwingPacket",
                        new Class<?>[] {hand.getClass()}, hand));
            }
        }
        if ("Spoof".equals(mode)) {
            setSelectedSlot(oldSlot);
        }
        return success;
    }

    private void handleFakeSwitch(int targetSlot, boolean clientSlotChanged) throws Exception {
        if (!"Fake".equals(switchMode.getValue())) return;
        int current = getSelectedSlot();
        if (current == targetSlot) {
            if (spoofingActive) {
                spoofingActive = false;
                lastSentSlot = targetSlot;
            }
            return;
        }
        if (lastSentSlot != targetSlot || clientSlotChanged || !spoofingActive) {
            sendSlot(targetSlot);
            lastSentSlot = targetSlot;
            spoofingActive = true;
        }
    }

    private void restoreHotbarSlot(boolean force) {
        try {
            if (player() == null) return;
            if (spoofingActive) {
                int selected = getSelectedSlot();
                sendSlot(selected);
                lastSentSlot = selected;
                spoofingActive = false;
            }
            if ("FullReverse".equals(switchMode.getValue()) || force) {
                if (originalSlot != -1) {
                    setSelectedSlot(originalSlot);
                    originalSlot = -1;
                }
            } else if (!"Full".equals(switchMode.getValue())) {
                originalSlot = -1;
            }
        } catch (Throwable ignored) {
        }
    }

    private int findBlockInHotbar() throws ReflectiveOperationException, ClassNotFoundException {
        Object inventory = inventory();
        Class<?> blockItem = Class.forName("net.minecraft.world.item.BlockItem", true, loader());
        for (int slot = 0; slot < 9; slot++) {
            Object stack = ModernMinecraftAccess.invoke(inventory, "getItem", slot);
            if (Boolean.TRUE.equals(ModernMinecraftAccess.invoke(stack, "isEmpty"))) continue;
            Object item = ModernMinecraftAccess.invoke(stack, "getItem");
            if (blockItem.isInstance(item)) return slot;
        }
        return -1;
    }

    private void applyRotation(BlockData data) throws Exception {
        if ("None".equals(rotMode.getValue())) {
            rotating = false;
            return;
        }
        Object player = player();
        float moveYaw = getMoveYaw();
        float targetYaw = (float) number(player, "getYRot");
        float targetPitch = (float) number(player, "getXRot");
        if (data != null) {
            Object hit = createHitVector(data);
            double dx = fieldNumber(hit, "x") - number(player, "getX");
            double dy = fieldNumber(hit, "y") - number(player, "getEyeY");
            double dz = fieldNumber(hit, "z") - number(player, "getZ");
            lastLookYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
            lastLookPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        }
        rotating = true;
        String mode = rotMode.getValue();
        if ("Normal".equals(mode)) {
            targetYaw = moveYaw;
            targetPitch = 82.0f;
        } else if ("Back".equals(mode)) {
            targetYaw = moveYaw + 180.0f;
            targetPitch = 82.0f;
        } else if ("Down".equals(mode)) {
            if (System.currentTimeMillis() < lookTimer) {
                targetYaw = lastLookYaw;
                targetPitch = lastLookPitch;
            } else rotating = false;
        } else if ("Backwards".equals(mode)) {
            if (System.currentTimeMillis() < lookTimer) {
                targetYaw = lastLookYaw + 180.0f;
                targetPitch = lastLookPitch;
            } else rotating = false;
        } else if ("Hive".equals(mode)) {
            if (System.currentTimeMillis() - lastPlaceTime < 100L) {
                targetYaw = moveYaw + 180.0f;
                targetPitch = 82.0f;
            } else rotating = false;
        } else if ("HypixelBack".equals(mode)) {
            targetYaw = moveYaw + 180.0f;
            targetPitch = lastLookPitch != 0.0f ? lastLookPitch : 82.0f;
        } else if ("HypixelSideways".equals(mode)) {
            float playerYaw = (float) number(player, "getYRot");
            float mod = Math.abs(wrap(playerYaw) % 90.0f);
            if (mod > 22.5f && mod < 67.5f) {
                targetYaw = moveYaw + 180.0f;
            } else {
                float back = moveYaw + 180.0f;
                float reference = lastLookYaw != 0.0f ? lastLookYaw : playerYaw;
                float one = back + 45.0f;
                float two = back - 45.0f;
                targetYaw = Math.abs(wrap(one - reference)) < Math.abs(wrap(two - reference)) ? one : two;
            }
            targetPitch = lastLookPitch != 0.0f ? lastLookPitch : 82.0f;
        }
        if (fakeBack.getValue()) targetYaw = moveYaw + 180.0f;
        if (!rotating) return;
        requestedYaw = wrap(targetYaw);
        requestedPitch = clamp(targetPitch, -90.0f, 90.0f);
    }

    /** UsefulTools RotationManager defaults: GCD bypass, smooth delta, four decimals. */
    private void setRotations(float yaw, float pitch) throws Exception {
        Object player = player();
        int tick = ((Number) ModernMinecraftAccess.field(player, "tickCount")).intValue();
        if (tick != rotationUpdateTick) rotationUpdateTick = tick;
        float currentYaw = serverYaw;
        float currentPitch = serverPitch;
        float diffYaw = wrap(yaw - currentYaw);
        float diffPitch = pitch - currentPitch;
        float roundedYaw = currentYaw + diffYaw;
        float roundedPitch = currentPitch + diffPitch;
        Object options = ModernMinecraftAccess.field(minecraft, "options");
        Object sensitivity = ModernMinecraftAccess.invoke(options, "sensitivity");
        double sensitivityValue = ((Number) ModernMinecraftAccess.invoke(sensitivity, "get")).doubleValue();
        float f = (float) sensitivityValue * 0.6f + 0.2f;
        float step = f * f * f * 8.0f * 0.15f;
        if (step > 0.0f) {
            roundedYaw = currentYaw + Math.round(diffYaw / step) * step;
            roundedPitch = currentPitch + Math.round(diffPitch / step) * step;
            roundedYaw += clamp(currentYaw + diffYaw - roundedYaw, -0.005f, 0.005f);
            roundedPitch += clamp(currentPitch + diffPitch - roundedPitch, -0.005f, 0.005f);
            roundedYaw = (float) (Math.round(roundedYaw * 10000.0) / 10000.0);
            roundedPitch = (float) (Math.round(roundedPitch * 10000.0) / 10000.0);
        }
        serverYaw = roundedYaw;
        serverPitch = roundedPitch;
    }

    @Override
    public boolean onMouseTurn(Object mouseHandler) {
        if (!rotating || minecraft == null) return false;
        try {
            if (screenOpen()) return false;
            Object player = player();
            if (player == null) return false;
            ensureCameraRotation(player);
            double deltaX = ((Number) ModernMinecraftAccess.field(
                    mouseHandler, "accumulatedDX")).doubleValue();
            double deltaY = ((Number) ModernMinecraftAccess.field(
                    mouseHandler, "accumulatedDY")).doubleValue();
            Object options = ModernMinecraftAccess.field(minecraft, "options");
            Object sensitivity = ModernMinecraftAccess.invoke(options, "sensitivity");
            double value = ((Number) ModernMinecraftAccess.invoke(sensitivity, "get")).doubleValue();
            double f = value * 0.6000000238418579 + 0.20000000298023224;
            double scale = f * f * f * 8.0 * 0.15;
            boolean invertX = optionBoolean(options, "invertMouseX");
            boolean invertY = optionBoolean(options, "invertMouseY");
            cameraYaw += (float) (deltaX * scale * (invertX ? -1.0 : 1.0));
            cameraPitch += (float) (deltaY * scale * (invertY ? -1.0 : 1.0));
            cameraPitch = clamp(cameraPitch, -90.0f, 90.0f);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void onCameraUpdate(Object camera) {
        if (!rotating || !cameraReady) return;
        try {
            ModernMinecraftAccess.invoke(camera, "setRotation", cameraYaw, cameraPitch);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onFrame(Object client) {
        if (!rotating) return;
        try {
            Object player = player();
            if (player != null) applyPlayerRotation(player);
        } catch (Throwable ignored) {
        }
    }

    private void ensureCameraRotation(Object player) throws ReflectiveOperationException {
        if (cameraReady) return;
        cameraYaw = (float) number(player, "getYRot");
        cameraPitch = (float) number(player, "getXRot");
        cameraReady = true;
    }

    private void applyPlayerRotation(Object player) throws ReflectiveOperationException {
        ModernMinecraftAccess.invoke(player, "setYRot", serverYaw);
        ModernMinecraftAccess.invoke(player, "setXRot", serverPitch);
        setFloatFieldIfPresent(player, "yHeadRot", serverYaw);
        setFloatFieldIfPresent(player, "yBodyRot", serverYaw);
    }

    private void restoreCameraRotation() {
        if (!cameraReady) return;
        try {
            Object player = player();
            if (player != null) {
                ModernMinecraftAccess.invoke(player, "setYRot", cameraYaw);
                ModernMinecraftAccess.invoke(player, "setXRot", cameraPitch);
                setFloatFieldIfPresent(player, "yHeadRot", cameraYaw);
                setFloatFieldIfPresent(player, "yBodyRot", cameraYaw);
            }
        } catch (Throwable ignored) {
        }
        cameraReady = false;
    }

    private void setFloatFieldIfPresent(Object owner, String name, float value) {
        try {
            ModernMinecraftAccess.findField(owner.getClass(), name).setFloat(owner, value);
        } catch (Throwable ignored) {
        }
    }

    private boolean optionBoolean(Object options, String methodName)
            throws ReflectiveOperationException {
        Object option = ModernMinecraftAccess.invoke(options, methodName);
        return Boolean.TRUE.equals(ModernMinecraftAccess.invoke(option, "get"));
    }

    private float viewYaw() throws ReflectiveOperationException {
        return cameraReady ? cameraYaw : (float) number(player(), "getYRot");
    }

    public void onSendPositionHead(Object player) {
        if (!rotating || rotationSpoofing) return;
        try {
            actualYaw = (float) number(player, "getYRot");
            actualPitch = (float) number(player, "getXRot");
            ModernMinecraftAccess.invoke(player, "setYRot", serverYaw);
            ModernMinecraftAccess.invoke(player, "setXRot", serverPitch);
            rotationSpoofing = true;
        } catch (Throwable ignored) {
        }
    }

    public void onSendPositionTail(Object player) {
        if (!rotationSpoofing) return;
        try {
            ModernMinecraftAccess.invoke(player, "setYRot", actualYaw);
            ModernMinecraftAccess.invoke(player, "setXRot", actualPitch);
        } catch (Throwable ignored) {
        } finally {
            rotationSpoofing = false;
        }
    }

    public void onAiStepHead(Object player) {
        keysSpoofed = false;
        if (!rotating || !"Silent".equals(moveFix.getValue())) {
            try {
                lastServerYaw = (float) number(player, "getYRot");
            } catch (Throwable ignored) {
            }
            return;
        }
        try {
            realUp = keyDown("keyUp");
            realDown = keyDown("keyDown");
            realLeft = keyDown("keyLeft");
            realRight = keyDown("keyRight");
            if (!(realUp || realDown || realLeft || realRight)) {
                lastServerYaw = serverYaw;
                return;
            }
            float z = (realUp ? 1.0f : 0.0f) - (realDown ? 1.0f : 0.0f);
            float x = (realLeft ? 1.0f : 0.0f) - (realRight ? 1.0f : 0.0f);
            float effectiveServerYaw = serverYaw;
            float yawChange = Math.abs(wrap(effectiveServerYaw - lastServerYaw));
            if (yawChange > 45.0f
                    && !Boolean.TRUE.equals(ModernMinecraftAccess.invoke(player, "onGround"))) {
                effectiveServerYaw = lastServerYaw
                        + wrap(effectiveServerYaw - lastServerYaw) * 0.5f;
            }
            lastServerYaw = effectiveServerYaw;
            float clientYaw = viewYaw();
            double radians = Math.toRadians(wrap(clientYaw - effectiveServerYaw));
            int sideways = Math.round((float) (x * Math.cos(radians) - z * Math.sin(radians)));
            int forward = Math.round((float) (z * Math.cos(radians) + x * Math.sin(radians)));
            boolean up = forward > 0;
            boolean down = forward < 0;
            setKey("keyUp", up);
            setKey("keyDown", down);
            setKey("keyLeft", sideways > 0);
            setKey("keyRight", sideways < 0);
            if (!up || down) {
                ModernMinecraftAccess.invoke(player, "setSprinting", false);
                setKey("keySprint", false);
            }
            keysSpoofed = true;
        } catch (Throwable ignored) {
            restoreMovementKeys();
        }
    }

    public void onAiStepTail(Object player) {
        restoreMovementKeys();
    }

    public Object onTravel(Object entity, Object movement) {
        if (!rotating) return movement;
        try {
            if (entity != player()) return movement;
            double x = fieldNumber(movement, "x");
            double z = fieldNumber(movement, "z");
            if (x == 0.0 && z == 0.0) return movement;
            float clientYaw = viewYaw();
            double radians = Math.toRadians(wrap(serverYaw - clientYaw));
            double newX = x * Math.cos(radians) - z * Math.sin(radians);
            double newZ = z * Math.cos(radians) + x * Math.sin(radians);
            Class<?> vec = Class.forName("net.minecraft.world.phys.Vec3", true, loader());
            return vec.getConstructor(double.class, double.class, double.class)
                    .newInstance(newX, fieldNumber(movement, "y"), newZ);
        } catch (Throwable ignored) {
            return movement;
        }
    }

    private void setKey(String name, boolean down) throws ReflectiveOperationException {
        Object options = ModernMinecraftAccess.field(minecraft, "options");
        ModernMinecraftAccess.invoke(ModernMinecraftAccess.field(options, name), "setDown", down);
    }

    private void restoreMovementKeys() {
        if (!keysSpoofed) return;
        try {
            setKey("keyUp", realUp);
            setKey("keyDown", realDown);
            setKey("keyLeft", realLeft);
            setKey("keyRight", realRight);
        } catch (Throwable ignored) {
        }
        keysSpoofed = false;
    }

    private BlockData findIntelligentFoundation(boolean moving) throws Exception {
        float yaw = getMoveYaw();
        double radians = Math.toRadians(yaw);
        double absoluteExtend = Math.abs(extend.getValue());
        double sign = Math.signum(extend.getValue());
        if (sign == 0.0) sign = 1.0;
        double start = extendOnly.getValue() ? absoluteExtend : 0.0;
        for (double distance = start; distance <= absoluteExtend + 0.0001; distance += 0.1) {
            double current = distance * sign;
            Object air = blockPos(
                    number(player(), "getX") - Math.sin(radians) * current,
                    lockY.getValue() && moving ? startY - 1.0 : number(player(), "getY") - 1.0,
                    number(player(), "getZ") + Math.cos(radians) * current);
            if (!isReplaceable(air)) continue;
            for (String direction : new String[] {"NORTH", "SOUTH", "EAST", "WEST", "DOWN"}) {
                Object neighbor = relative(air, direction);
                if (isSolid(neighbor)) {
                    return new BlockData(neighbor, opposite(enumValue("net.minecraft.core.Direction", direction)), air);
                }
            }
            Object nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            int search = diagonalRange.getValue().intValue();
            int ax = coordinate(air, "getX");
            int ay = coordinate(air, "getY");
            int az = coordinate(air, "getZ");
            for (int ox = -search; ox <= search; ox++) {
                for (int oy = -search; oy <= search; oy++) {
                    for (int oz = -search; oz <= search; oz++) {
                        double squared = ox * ox + oy * oy + oz * oz;
                        if (squared >= nearestDistance) continue;
                        Object candidate = offset(air, ox, oy, oz);
                        if (!isSolid(candidate)) continue;
                        nearestDistance = squared;
                        nearest = candidate;
                    }
                }
            }
            if (nearest != null) return new BlockData(nearest, closestFace(air, nearest), air);
        }
        return null;
    }

    private Object closestFace(Object air, Object solid) throws Exception {
        int dx = coordinate(air, "getX") - coordinate(solid, "getX");
        int dy = coordinate(air, "getY") - coordinate(solid, "getY");
        int dz = coordinate(air, "getZ") - coordinate(solid, "getZ");
        if (dy > 0) return enumValue("net.minecraft.core.Direction", "UP");
        if (dy < 0) return enumValue("net.minecraft.core.Direction", "DOWN");
        if (dx > 0) return enumValue("net.minecraft.core.Direction", "EAST");
        if (dx < 0) return enumValue("net.minecraft.core.Direction", "WEST");
        if (dz > 0) return enumValue("net.minecraft.core.Direction", "SOUTH");
        if (dz < 0) return enumValue("net.minecraft.core.Direction", "NORTH");
        return enumValue("net.minecraft.core.Direction", "UP");
    }

    private void handleTower() throws ReflectiveOperationException {
        if ("None".equals(towerMode.getValue())) return;
        Object player = player();
        Object velocity = ModernMinecraftAccess.invoke(player, "getDeltaMovement");
        double vx = fieldNumber(velocity, "x");
        double vy = fieldNumber(velocity, "y");
        double vz = fieldNumber(velocity, "z");
        boolean grounded = Boolean.TRUE.equals(ModernMinecraftAccess.invoke(player, "onGround"));
        if (("Vanilla".equals(towerMode.getValue()) && grounded)
                || ("Velocity".equals(towerMode.getValue()) && (grounded || vy < 0.15))) {
            ModernMinecraftAccess.invoke(player, "setDeltaMovement", vx, 0.42, vz);
        }
    }

    private float getMoveYaw() throws ReflectiveOperationException {
        float yaw = viewYaw();
        float forward = 0.0f;
        float sideways = 0.0f;
        if (keyDown("keyUp")) forward += 1.0f;
        if (keyDown("keyDown")) forward -= 1.0f;
        if (keyDown("keyLeft")) sideways += 1.0f;
        if (keyDown("keyRight")) sideways -= 1.0f;
        if (forward == 0.0f && sideways == 0.0f) return yaw;
        boolean back = forward < 0.0f;
        if (forward != 0.0f) {
            if (sideways > 0.0f) yaw += back ? 45.0f : -45.0f;
            else if (sideways < 0.0f) yaw += back ? -45.0f : 45.0f;
            if (back) yaw += 180.0f;
        } else if (sideways > 0.0f) yaw -= 90.0f;
        else yaw += 90.0f;
        return wrap(yaw);
    }

    private Object createHitVector(BlockData data) throws Exception {
        Object center = ModernMinecraftAccess.invoke(data.pos, "getCenter");
        Object normal = ModernMinecraftAccess.invoke(data.face, "getUnitVec3");
        Object half = ModernMinecraftAccess.invoke(normal, "scale", 0.5);
        return ModernMinecraftAccess.invoke(center, "add", half);
    }

    private Object createHitResult(BlockData data) throws Exception {
        Object vector = createHitVector(data);
        Class<?> result = Class.forName("net.minecraft.world.phys.BlockHitResult", true, loader());
        Class<?> vec = Class.forName("net.minecraft.world.phys.Vec3", true, loader());
        Class<?> direction = Class.forName("net.minecraft.core.Direction", true, loader());
        Class<?> blockPos = Class.forName("net.minecraft.core.BlockPos", true, loader());
        return result.getConstructor(vec, direction, blockPos, boolean.class)
                .newInstance(vector, data.face, data.pos, false);
    }

    private void sendSlot(int slot) throws Exception {
        sendPacket(newInstance("net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket",
                new Class<?>[] {int.class}, slot));
    }

    private void sendPacket(Object packet) throws ReflectiveOperationException {
        Object connection = ModernMinecraftAccess.field(player(), "connection");
        ModernMinecraftAccess.invoke(connection, "send", packet);
    }

    private int getSelectedSlot() throws ReflectiveOperationException {
        return ((Number) ModernMinecraftAccess.invoke(inventory(), "getSelectedSlot")).intValue();
    }

    private void setSelectedSlot(int slot) throws ReflectiveOperationException {
        ModernMinecraftAccess.invoke(inventory(), "setSelectedSlot", slot);
    }

    private Object inventory() throws ReflectiveOperationException {
        return ModernMinecraftAccess.invoke(player(), "getInventory");
    }

    private boolean isMoving() throws ReflectiveOperationException {
        return keyDown("keyUp") || keyDown("keyDown") || keyDown("keyLeft") || keyDown("keyRight");
    }

    private boolean keyDown(String name) throws ReflectiveOperationException {
        Object options = ModernMinecraftAccess.field(minecraft, "options");
        Object mapping = ModernMinecraftAccess.field(options, name);
        return Boolean.TRUE.equals(ModernMinecraftAccess.invoke(mapping, "isDown"));
    }

    private void setShift(boolean down) throws ReflectiveOperationException {
        Object options = ModernMinecraftAccess.field(minecraft, "options");
        ModernMinecraftAccess.invoke(ModernMinecraftAccess.field(options, "keyShift"), "setDown", down);
    }

    private void releaseSneak() {
        if (!forcedSneak) return;
        try {
            setShift(false);
        } catch (Throwable ignored) {
        }
        forcedSneak = false;
        sneakReleaseTicks = 0;
    }

    private boolean screenOpen() throws ReflectiveOperationException {
        return ModernMinecraftAccess.field(minecraft, "screen") != null;
    }

    private Object player() throws ReflectiveOperationException {
        return minecraft == null ? null : ModernMinecraftAccess.field(minecraft, "player");
    }

    private Object level() throws ReflectiveOperationException {
        return minecraft == null ? null : ModernMinecraftAccess.field(minecraft, "level");
    }

    private Object blockPos(double x, double y, double z) throws Exception {
        Class<?> type = Class.forName("net.minecraft.core.BlockPos", true, loader());
        return type.getMethod("containing", double.class, double.class, double.class)
                .invoke(null, x, y, z);
    }

    private Object offset(Object pos, int x, int y, int z) throws ReflectiveOperationException {
        return ModernMinecraftAccess.invoke(pos, "offset", x, y, z);
    }

    private Object relative(Object pos, String direction) throws Exception {
        return ModernMinecraftAccess.invoke(pos, "relative",
                enumValue("net.minecraft.core.Direction", direction));
    }

    private Object opposite(Object direction) throws ReflectiveOperationException {
        return ModernMinecraftAccess.invoke(direction, "getOpposite");
    }

    private boolean isReplaceable(Object pos) throws ReflectiveOperationException {
        Object state = ModernMinecraftAccess.invoke(level(), "getBlockState", pos);
        return Boolean.TRUE.equals(ModernMinecraftAccess.invoke(state, "canBeReplaced"));
    }

    private boolean isSolid(Object pos) throws ReflectiveOperationException {
        Object state = ModernMinecraftAccess.invoke(level(), "getBlockState", pos);
        return !Boolean.TRUE.equals(ModernMinecraftAccess.invoke(state, "isAir"))
                && Boolean.TRUE.equals(ModernMinecraftAccess.invoke(state, "canOcclude"));
    }

    private int coordinate(Object pos, String name) throws ReflectiveOperationException {
        return ((Number) ModernMinecraftAccess.invoke(pos, name)).intValue();
    }

    private Object enumValue(String className, String value) throws Exception {
        Class<?> type = Class.forName(className, true, loader());
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object result = Enum.valueOf((Class<? extends Enum>) type.asSubclass(Enum.class), value);
        return result;
    }

    private Object newInstance(String name, Class<?>[] types, Object... values) throws Exception {
        Constructor<?> constructor = Class.forName(name, true, loader()).getConstructor(types);
        return constructor.newInstance(values);
    }

    private ClassLoader loader() {
        return minecraft.getClass().getClassLoader();
    }

    private static double number(Object owner, String method) throws ReflectiveOperationException {
        return ModernMinecraftAccess.number(owner, method);
    }

    private static double fieldNumber(Object owner, String field) throws ReflectiveOperationException {
        return ((Number) ModernMinecraftAccess.field(owner, field)).doubleValue();
    }

    private static float wrap(float value) {
        value %= 360.0f;
        if (value >= 180.0f) value -= 360.0f;
        if (value < -180.0f) value += 360.0f;
        return value;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static final class BlockData {
        private final Object pos;
        private final Object face;
        @SuppressWarnings("unused")
        private final Object targetPos;

        private BlockData(Object pos, Object face, Object targetPos) {
            this.pos = pos;
            this.face = face;
            this.targetPos = targetPos;
        }
    }
}
