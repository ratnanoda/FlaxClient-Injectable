package me.eldodebug.soar.attach.modern;

import imgui.ImGui;
import imgui.ImFont;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import me.eldodebug.soar.attach.modern.ModernSetting.BooleanSetting;
import me.eldodebug.soar.attach.modern.ModernSetting.ColorSetting;
import me.eldodebug.soar.attach.modern.ModernSetting.ComboSetting;
import me.eldodebug.soar.attach.modern.ModernSetting.KeybindSetting;
import me.eldodebug.soar.attach.modern.ModernSetting.NumberSetting;
import me.eldodebug.soar.logger.GlideLogger;

public final class ModernModuleManager {

    private final List<ModernModule> modules = new ArrayList<ModernModule>();
    private final Map<String, Boolean> bindDown = new HashMap<String, Boolean>();
    private final File configFile = new File(
            new File(System.getProperty("user.home"), ".flaxclient"),
            "lunar-26.1.2.properties");
    private Object minecraft;
    private NumberSetting accentHue;
    private BooleanSetting hudBackground;
    private ImFont nametagInterfaceFont;
    private ImFont nametagMinecraftFont;
    private ImFont nametagProductFont;
    private final List<Object> hudPlayers = new ArrayList<Object>();
    private boolean hudFrameActive;
    private Object hudProjectionMatrix;
    private Object hudProjectionVector;
    private double hudCameraX;
    private double hudCameraY;
    private double hudCameraZ;
    private volatile boolean realEspActive;
    private ModernScaffoldModule scaffold;

    public void initialize(Object minecraft) {
        this.minecraft = minecraft;
        registerModules();
        load();
        refreshFastState();
        for (ModernModule module : modules) {
            if (module.isEnabled()) {
                module.onEnable(minecraft);
            }
        }
    }

    private void registerModules() {
        modules.clear();
        registerSettings();
        registerAimAssist();
        registerAutoClicker();
        registerBedEsp();
        registerBreakProgress();
        registerEsp();
        registerFastPlace();
        registerGhostFreelook();
        registerGhostNametags();
        registerHealthbar();
        registerJumpReset();
        registerSafeWalk();
        scaffold = new ModernScaffoldModule();
        modules.add(scaffold);
        registerYouTubePip();
    }

    private void registerSettings() {
        modules.add(new ModernModule("settings", "Settings", "Global FlaxClient behaviour.",
                ModernCategory.OTHER, true) {
            {
                setting(new BooleanSetting("move_fix", "MoveFix", true));
                accentHue = setting(new NumberSetting("accent_hue", "Accent hue", 0.62, 0.0, 1.0));
                hudBackground = setting(new BooleanSetting("hud_background", "HUD background", true));
            }

            @Override
            public void setEnabled(boolean enabled, Object client) {
                super.setEnabled(true, client);
            }
        });
    }

    private void registerAimAssist() {
        modules.add(new ModernModule("aim_assist", "Aim Assist",
                "Smoothly micro-adjust aim toward nearby players while you aim manually.",
                ModernCategory.GHOST, false) {
            private final NumberSetting range = setting(new NumberSetting("range", "Range", 4.2, 2.0, 6.0));
            private final NumberSetting fov = setting(new NumberSetting("fov", "FOV", 48.0, 10.0, 140.0));
            private final NumberSetting smooth = setting(new NumberSetting("smooth", "Smooth Speed", 7.5, 1.5, 26.0));
            private final NumberSetting accuracy = setting(new NumberSetting("accuracy", "Accuracy", 84.0, 0.0, 100.0));
            private final NumberSetting strength = setting(new NumberSetting("strength", "Strength", 0.9, 0.05, 2.6));
            private final BooleanSetting requireClick = setting(new BooleanSetting(
                    "require_click", "Require Click", true));

            @Override
            public void onFrame(Object client) {
                if ((requireClick.getValue() && !mouseButtonDown(client, 0)) || hasScreen(client)) return;
                try {
                    Object player = ModernMinecraftAccess.field(client, "player");
                    Object level = ModernMinecraftAccess.field(client, "level");
                    if (player == null || level == null || Boolean.TRUE.equals(ModernMinecraftAccess.invoke(player, "isUsingItem"))) return;
                    Iterable<?> players = (Iterable<?>) ModernMinecraftAccess.invoke(level, "players");
                    double px = ModernMinecraftAccess.number(player, "getX");
                    double py = ModernMinecraftAccess.number(player, "getEyeY");
                    double pz = ModernMinecraftAccess.number(player, "getZ");
                    float currentYaw = ((Number) ModernMinecraftAccess.invoke(player, "getYRot")).floatValue();
                    float currentPitch = ((Number) ModernMinecraftAccess.invoke(player, "getXRot")).floatValue();
                    double yawRadians = Math.toRadians(currentYaw);
                    double pitchRadians = Math.toRadians(currentPitch);
                    double lookX = -Math.sin(yawRadians) * Math.cos(pitchRadians);
                    double lookY = -Math.sin(pitchRadians);
                    double lookZ = Math.cos(yawRadians) * Math.cos(pitchRadians);
                    Object best = null;
                    float bestError = Float.MAX_VALUE;
                    float bestYaw = 0.0f;
                    float bestPitch = 0.0f;
                    for (Object candidate : players) {
                        if (candidate == player || !Boolean.TRUE.equals(ModernMinecraftAccess.invoke(candidate, "isAlive"))) continue;
                        Object box = ModernMinecraftAccess.invoke(candidate, "getBoundingBox");
                        double minX = ((Number) ModernMinecraftAccess.field(box, "minX")).doubleValue();
                        double minY = ((Number) ModernMinecraftAccess.field(box, "minY")).doubleValue();
                        double minZ = ((Number) ModernMinecraftAccess.field(box, "minZ")).doubleValue();
                        double maxX = ((Number) ModernMinecraftAccess.field(box, "maxX")).doubleValue();
                        double maxY = ((Number) ModernMinecraftAccess.field(box, "maxY")).doubleValue();
                        double maxZ = ((Number) ModernMinecraftAccess.field(box, "maxZ")).doubleValue();
                        double centerX = (minX + maxX) * 0.5;
                        double centerY = (minY + maxY) * 0.5;
                        double centerZ = (minZ + maxZ) * 0.5;
                        double alongRay = Math.max(0.0, (centerX - px) * lookX
                                + (centerY - py) * lookY + (centerZ - pz) * lookZ);
                        // Clamp the current sight ray to the player's hitbox.
                        // This chooses the nearest visible body point instead
                        // of always pulling vertically toward the head.
                        double aimX = clampDouble(px + lookX * alongRay, minX, maxX);
                        double aimY = clampDouble(py + lookY * alongRay, minY + 0.05, maxY - 0.05);
                        double aimZ = clampDouble(pz + lookZ * alongRay, minZ, maxZ);
                        double dx = aimX - px;
                        double dy = aimY - py;
                        double dz = aimZ - pz;
                        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (distance > range.getValue() || distance < 0.01) continue;
                        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
                        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
                        float yawError = wrapDegrees(yaw - currentYaw);
                        float pitchError = wrapDegrees(pitch - currentPitch);
                        float total = (float) Math.sqrt(yawError * yawError + pitchError * pitchError);
                        if (Math.abs(yawError) <= fov.getValue() / 2.0 && total < bestError) {
                            best = candidate;
                            bestError = total;
                            bestYaw = yawError;
                            bestPitch = pitchError;
                        }
                    }
                    if (best == null) return;
                    float blend = (float) (strength.getValue() / smooth.getValue());
                    float precision = (float) (accuracy.getValue() / 100.0);
                    float yawStep = clamp(bestYaw * blend, -2.6f, 2.6f) * (0.67f + precision * 0.33f);
                    float pitchStep = clamp(bestPitch * blend * 0.88f, -2.0f, 2.0f) * (0.67f + precision * 0.33f);
                    ModernMinecraftAccess.invoke(player, "setYRot", currentYaw + yawStep);
                    ModernMinecraftAccess.invoke(player, "setXRot", clamp(currentPitch + pitchStep, -90.0f, 90.0f));
                } catch (ReflectiveOperationException ignored) {
                }
            }
        });
    }

    private void registerAutoClicker() {
        modules.add(new ModernModule("auto_clicker", "AutoClicker",
                "Hold left click to auto click with randomized CPS.", ModernCategory.GHOST, false) {
            private final NumberSetting minimum = setting(new NumberSetting("min_cps", "Min CPS", 8, 1, 24));
            private final NumberSetting maximum = setting(new NumberSetting("max_cps", "Max CPS", 13, 1, 24));
            private long nextClick;

            @Override
            public void onFrame(Object client) {
                if (!mouseButtonDown(client, 0) || hasScreen(client)) {
                    nextClick = 0L;
                    return;
                }
                long now = System.currentTimeMillis();
                if (nextClick == 0L) nextClick = now + nextDelay();
                if (now < nextClick) return;
                try {
                    ModernMinecraftAccess.setInt(client, "missTime", 0);
                    ModernMinecraftAccess.invoke(client, "startAttack");
                } catch (ReflectiveOperationException ignored) {
                }
                nextClick = now + nextDelay();
            }

            private long nextDelay() {
                double low = Math.min(minimum.getValue(), maximum.getValue());
                double high = Math.max(minimum.getValue(), maximum.getValue());
                double cps = low == high ? low : ThreadLocalRandom.current().nextDouble(low, high);
                return Math.max(8L, Math.round(1000.0 / cps));
            }
        });
    }

    private void registerBedEsp() {
        modules.add(new ModernModule("bed_esp", "BedESP",
                "Highlight nearby beds, including through walls.", ModernCategory.GHOST, false) {
            {
                setting(new ColorSetting("color", "Color", 255, 64, 64));
                setting(new NumberSetting("alpha", "Alpha", 0.85, 0.05, 1.0));
                setting(new NumberSetting("line_width", "Line Width", 2, 1, 5));
                setting(new ComboSetting("mode", "Mode", "Box", "Outline", "Box", "Glow"));
            }
        });
    }

    private void registerBreakProgress() {
        modules.add(new ModernModule("break_progress", "BreakProgress",
                "Show an animated bar of the block breaking progress.", ModernCategory.GHOST, false) {
            private final ComboSetting style = setting(new ComboSetting("style", "Style", "Bar", "Bar", "Circle", "Text"));
            private final ColorSetting color = setting(new ColorSetting("color", "Color", 0, 255, 170));
            private final NumberSetting width = setting(new NumberSetting("width", "Width", 80, 40, 160));
            private final NumberSetting height = setting(new NumberSetting("height", "Height", 5, 2, 12));
            private final NumberSetting offset = setting(new NumberSetting("offset", "Offset", 20, 5, 80));
            private final BooleanSetting text = setting(new BooleanSetting("text", "Text", true));

            @Override
            public void onHudRender() {
                try {
                    Object gameMode = ModernMinecraftAccess.field(minecraft, "gameMode");
                    if (gameMode == null) return;
                    int stage = ((Number) ModernMinecraftAccess.invoke(gameMode, "getDestroyStage")).intValue();
                    if (stage < 0) return;
                    float progress = Math.min(1.0f, (stage + 1) / 10.0f);
                    int rgb = color.getValue();
                    int foreground = abgr((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 235);
                    float centerX = ImGui.getIO().getDisplaySizeX() / 2.0f;
                    float centerY = ImGui.getIO().getDisplaySizeY() / 2.0f + offset.getValue().floatValue();
                    if ("Text".equals(style.getValue())) {
                        ImGui.getForegroundDrawList().addText(centerX - 13.0f, centerY, foreground,
                                Math.round(progress * 100.0f) + "%");
                    } else {
                        float barWidth = width.getValue().floatValue();
                        float barHeight = height.getValue().floatValue();
                        ImGui.getForegroundDrawList().addRectFilled(centerX - barWidth / 2.0f, centerY,
                                centerX + barWidth / 2.0f, centerY + barHeight, 0xA0100D0D, barHeight / 2.0f);
                        ImGui.getForegroundDrawList().addRectFilled(centerX - barWidth / 2.0f, centerY,
                                centerX - barWidth / 2.0f + barWidth * progress, centerY + barHeight,
                                foreground, barHeight / 2.0f);
                        if (text.getValue()) ImGui.getForegroundDrawList().addText(centerX - 13.0f,
                                centerY - 17.0f, 0xFFFFFFFF, Math.round(progress * 100.0f) + "%");
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
        });
    }

    private void registerEsp() {
        modules.add(new ModernModule("esp", "ESP",
                "Highlight player positions, including through walls.", ModernCategory.GHOST, false) {
            private final ComboSetting mode = setting(new ComboSetting("mode", "Mode", "Box", "Box", "Real"));
            private final ColorSetting color = setting(new ColorSetting("color", "Color", 0, 255, 170));
            private final NumberSetting alpha = setting(new NumberSetting("alpha", "Alpha", 0.85, 0.05, 1.0));
            private final NumberSetting lineWidth = setting(new NumberSetting("line_width", "Line Width", 2, 1, 5));
            private final BooleanSetting fill = setting(new BooleanSetting("fill", "Fill", true));
            private final BooleanSetting outline = setting(new BooleanSetting("outline", "Outline", true));

            @Override
            public void onHudRender() {
                // Real mode is handled in the world renderer by replacing the
                // player model RenderType with a depth-independent variant.
                if ("Real".equals(mode.getValue())) return;
                int rgb = color.getValue();
                int edge = abgr((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255,
                        (int) (alpha.getValue() * 255));
                for (Object player : otherPlayers()) {
                    try {
                        float[] feet = project(ModernMinecraftAccess.number(player, "getX"),
                                ModernMinecraftAccess.number(player, "getY"),
                                ModernMinecraftAccess.number(player, "getZ"));
                        float[] head = project(ModernMinecraftAccess.number(player, "getX"),
                                ModernMinecraftAccess.number(player, "getEyeY") + 0.35,
                                ModernMinecraftAccess.number(player, "getZ"));
                        if (feet == null || head == null) continue;
                        float height = Math.abs(feet[1] - head[1]);
                        if (height < 3.0f) continue;
                        float halfWidth = height * ("Real".equals(mode.getValue()) ? 0.22f : 0.30f);
                        if (fill.getValue()) {
                            ImGui.getForegroundDrawList().addRectFilled(head[0] - halfWidth, head[1],
                                    head[0] + halfWidth, feet[1], withAlpha(edge, 38));
                        }
                        if (outline.getValue()) {
                            ImGui.getForegroundDrawList().addRect(head[0] - halfWidth, head[1],
                                    head[0] + halfWidth, feet[1], edge, 0.0f, 0,
                                    lineWidth.getValue().floatValue());
                        }
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            }
        });
    }

    private void registerFastPlace() {
        modules.add(new ModernModule("fast_place", "FastPlace",
                "Hold right click to place blocks faster with randomized CPS.", ModernCategory.GHOST, false) {
            private final Random random = new Random();
            private int targetDelay;
            private long refreshAt;

            @Override
            public void onTick(Object client) {
                if (!mouseButtonDown(client, 1) || hasScreen(client)) return;
                long now = System.currentTimeMillis();
                if (now >= refreshAt) {
                    double roll = random.nextDouble();
                    targetDelay = roll < 0.72 ? 0 : roll < 0.96 ? 1 : 2;
                    refreshAt = now + 55L + random.nextInt(125);
                }
                try {
                    int delay = ((Number) ModernMinecraftAccess.field(client, "rightClickDelay")).intValue();
                    if (delay > targetDelay) ModernMinecraftAccess.setInt(client, "rightClickDelay", targetDelay);
                } catch (ReflectiveOperationException ignored) {
                }
            }
        });
    }

    private void registerGhostFreelook() {
        modules.add(new ModernModule("ghost_freelook", "Ghost Freelook",
                "Move the viewpoint freely (Ghost category).", ModernCategory.GHOST, false) {
            private final BooleanSetting invertYaw = setting(new BooleanSetting("invert_yaw", "Invert Yaw", false));
            private final BooleanSetting invertPitch = setting(new BooleanSetting("invert_pitch", "Invert Pitch", false));
            private final BooleanSetting lockPitch = setting(new BooleanSetting("lock_camera", "Lock Camera", true));
            private final BooleanSetting customFov = setting(new BooleanSetting("custom_fov", "Custom FOV", false));
            private final NumberSetting fov = setting(new NumberSetting("fov", "FOV", 90, 10, 150));
            private final ComboSetting mode = setting(new ComboSetting("mode", "Mode", "Key Down", "Key Down", "Toggle"));
            private final KeybindSetting keybind = setting(new KeybindSetting("keybind", "Keybind", 86));
            private boolean active;
            private boolean keyWasDown;
            private boolean rotationReady;
            private float cameraYaw;
            private float cameraPitch;

            @Override
            public void onTick(Object client) {
                boolean key = keyboardKeyDown(client, keybind.getValue().intValue());
                if ("Toggle".equals(mode.getValue())) {
                    if (key && !keyWasDown) active = !active;
                } else {
                    active = key;
                }
                keyWasDown = key;
                if (!active) rotationReady = false;
            }

            @Override
            public boolean onMouseTurn(Object mouseHandler) {
                if (!active || hasScreen(minecraft)) return false;
                try {
                    Object local = ModernMinecraftAccess.field(minecraft, "player");
                    if (local == null) return false;
                    if (!rotationReady) {
                        cameraYaw = ((Number) ModernMinecraftAccess.invoke(local, "getYRot")).floatValue();
                        cameraPitch = ((Number) ModernMinecraftAccess.invoke(local, "getXRot")).floatValue();
                        rotationReady = true;
                    }
                    double deltaX = ((Number) ModernMinecraftAccess.field(mouseHandler, "accumulatedDX")).doubleValue();
                    double deltaY = ((Number) ModernMinecraftAccess.field(mouseHandler, "accumulatedDY")).doubleValue();
                    cameraYaw += (float) (deltaX * 0.15 * (invertYaw.getValue() ? -1.0 : 1.0));
                    cameraPitch += (float) (deltaY * 0.15 * (invertPitch.getValue() ? -1.0 : 1.0));
                    if (lockPitch.getValue()) cameraPitch = clamp(cameraPitch, -90.0f, 90.0f);
                    return true;
                } catch (ReflectiveOperationException ignored) {
                    return false;
                }
            }

            @Override
            public void onCameraUpdate(Object camera) {
                if (!active || !rotationReady) return;
                try {
                    ModernMinecraftAccess.invoke(camera, "setRotation", cameraYaw, cameraPitch);
                } catch (ReflectiveOperationException ignored) {
                }
            }

            @Override
            public void onDisable(Object client) {
                active = false;
                rotationReady = false;
                keyWasDown = false;
            }
        });
    }

    private void registerGhostNametags() {
        modules.add(new ModernModule("ghost_nametags", "Nametags",
                "UsefulTools-style custom overhead nametags.", ModernCategory.GHOST, false) {
            private final BooleanSetting antiBot = setting(new BooleanSetting(
                    "anti_bot_filter", "AntiBot Filter", true));
            private final BooleanSetting teammates = setting(new BooleanSetting(
                    "show_teammates", "Show Teammates", true));
            private final BooleanSetting distance = setting(new BooleanSetting(
                    "show_distance", "Show Distance", true));
            private final NumberSetting opacity = setting(new NumberSetting(
                    "opacity", "Opacity", 0.6, 0.0, 1.0));
            private final ComboSetting fontMode = setting(new ComboSetting(
                    "font_mode", "Font Mode", "Interface", "Interface", "Mojangles", "Product Sans"));
            private final NumberSetting size = setting(new NumberSetting(
                    "size", "Size", 1.0, 0.5, 3.0));

            @Override
            public void onHudRender() {
                Object local = player();
                if (local == null) return;
                for (Object target : otherPlayers()) {
                    try {
                        if (!teammates.getValue()
                                && Boolean.TRUE.equals(ModernMinecraftAccess.invoke(target, "isAlliedTo", local))) {
                            continue;
                        }
                        Object displayName = ModernMinecraftAccess.invoke(target, "getDisplayName");
                        String name = String.valueOf(ModernMinecraftAccess.invoke(displayName, "getString"));
                        if (antiBot.getValue() && (name.trim().isEmpty() || name.length() > 48)) continue;
                        float[] position = project(ModernMinecraftAccess.number(target, "getX"),
                                ModernMinecraftAccess.number(target, "getEyeY") + 0.55,
                                ModernMinecraftAccess.number(target, "getZ"));
                        if (position == null) continue;
                        float health = ((Number) ModernMinecraftAccess.invoke(target, "getHealth")).floatValue();
                        String hpText = String.format(java.util.Locale.ROOT, " [%.1f]", health);
                        String distanceText = "";
                        if (distance.getValue()) {
                            double dx = ModernMinecraftAccess.number(target, "getX") - ModernMinecraftAccess.number(local, "getX");
                            double dy = ModernMinecraftAccess.number(target, "getY") - ModernMinecraftAccess.number(local, "getY");
                            double dz = ModernMinecraftAccess.number(target, "getZ") - ModernMinecraftAccess.number(local, "getZ");
                            distanceText = String.format(java.util.Locale.ROOT, " [%.1fm]",
                                    Math.sqrt(dx * dx + dy * dy + dz * dz));
                        }
                        ImFont font = nametagFont(fontMode.getValue());
                        int fontSize = Math.max(8, Math.round(16.0f * size.getValue().floatValue()));
                        float nameWidth = font.calcTextSizeAX(fontSize, Float.MAX_VALUE, 0.0f, name);
                        float hpWidth = font.calcTextSizeAX(fontSize, Float.MAX_VALUE, 0.0f, hpText);
                        float distanceWidth = font.calcTextSizeAX(fontSize, Float.MAX_VALUE, 0.0f, distanceText);
                        float totalWidth = nameWidth + hpWidth + distanceWidth;
                        float textHeight = fontSize;
                        float padX = 6.0f * size.getValue().floatValue();
                        float padY = 3.0f * size.getValue().floatValue();
                        float startX = position[0] - totalWidth / 2.0f;
                        float startY = position[1] - textHeight / 2.0f;
                        ImGui.getForegroundDrawList().addRectFilled(
                                startX - padX, startY - padY,
                                startX + totalWidth + padX, startY + textHeight + padY,
                                abgr(0, 0, 0, (int) (opacity.getValue() * 255.0)),
                                4.0f * size.getValue().floatValue());
                        int nameColor = displayNameColor(displayName);
                        ImGui.getForegroundDrawList().addText(font, fontSize, startX, startY,
                                nameColor, name);
                        ImGui.getForegroundDrawList().addText(font, fontSize,
                                startX + nameWidth, startY, abgr(0, 255, 0, 255), hpText);
                        if (!distanceText.isEmpty()) {
                            ImGui.getForegroundDrawList().addText(font, fontSize,
                                    startX + nameWidth + hpWidth, startY,
                                    abgr(128, 204, 255, 255), distanceText);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }

            private int displayNameColor(Object displayName) {
                try {
                    Object style = ModernMinecraftAccess.invoke(displayName, "getStyle");
                    Object color = ModernMinecraftAccess.invoke(style, "getColor");
                    if (color != null) {
                        int rgb = ((Number) ModernMinecraftAccess.invoke(color, "getValue")).intValue();
                        return abgr((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255);
                    }
                } catch (Throwable ignored) {
                }
                return abgr(255, 255, 255, 255);
            }
        });
    }

    private void registerHealthbar() {
        modules.add(new ModernModule("healthbar", "Healthbar",
                "Show a vertical health bar next to players.", ModernCategory.GHOST, false) {
            private final NumberSetting alpha = setting(new NumberSetting("alpha", "Alpha", 0.9, 0.15, 1.0));
            private final NumberSetting width = setting(new NumberSetting("width", "Width", 1.55, 0.7, 2.8));
            private final NumberSetting sideOffset = setting(new NumberSetting("side_offset", "Side Offset", 0.08, 0.0, 0.45));
            private final NumberSetting range = setting(new NumberSetting("range", "Range", 42.0, 10.0, 120.0));

            @Override
            public void onHudRender() {
                Object local = player();
                if (local == null) return;
                for (Object target : otherPlayers()) {
                    try {
                        double dx = ModernMinecraftAccess.number(target, "getX") - ModernMinecraftAccess.number(local, "getX");
                        double dy = ModernMinecraftAccess.number(target, "getY") - ModernMinecraftAccess.number(local, "getY");
                        double dz = ModernMinecraftAccess.number(target, "getZ") - ModernMinecraftAccess.number(local, "getZ");
                        if (dx * dx + dy * dy + dz * dz > range.getValue() * range.getValue()) continue;
                        float[] feet = project(ModernMinecraftAccess.number(target, "getX"),
                                ModernMinecraftAccess.number(target, "getY"), ModernMinecraftAccess.number(target, "getZ"));
                        float[] head = project(ModernMinecraftAccess.number(target, "getX"),
                                ModernMinecraftAccess.number(target, "getEyeY") + 0.35, ModernMinecraftAccess.number(target, "getZ"));
                        if (feet == null || head == null) continue;
                        float health = ((Number) ModernMinecraftAccess.invoke(target, "getHealth")).floatValue();
                        float maximum = Math.max(1.0f, ((Number) ModernMinecraftAccess.invoke(target, "getMaxHealth")).floatValue());
                        float ratio = clamp(health / maximum, 0.0f, 1.0f);
                        float barHeight = Math.abs(feet[1] - head[1]);
                        float barWidth = Math.max(2.0f, width.getValue().floatValue() * 2.0f);
                        float x = head[0] + barHeight * 0.31f + sideOffset.getValue().floatValue() * 10.0f;
                        int opacity = (int) (alpha.getValue() * 255);
                        ImGui.getForegroundDrawList().addRectFilled(x, head[1], x + barWidth,
                                feet[1], abgr(20, 22, 28, opacity));
                        int red = Math.round((1.0f - ratio) * 255.0f);
                        int green = Math.round(ratio * 255.0f);
                        ImGui.getForegroundDrawList().addRectFilled(x, feet[1] - barHeight * ratio,
                                x + barWidth, feet[1], abgr(red, green, 70, opacity));
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            }
        });
    }

    private void registerJumpReset() {
        modules.add(new ModernModule("jump_reset", "Jump Reset",
                "Automatically jumps when you get hit to attempt a jump reset.", ModernCategory.GHOST, false) {
            private final NumberSetting chance = setting(new NumberSetting("success_chance", "Success Chance", 100, 0, 100));
            private final NumberSetting delay = setting(new NumberSetting("delay", "Delay", 0, 0, 3));
            private int previousHurt;
            private int scheduled = -1;
            private int cooldown;

            @Override
            public void onTick(Object client) {
                try {
                    Object player = ModernMinecraftAccess.field(client, "player");
                    if (player == null || hasScreen(client)) return;
                    int hurt = ((Number) ModernMinecraftAccess.field(player, "hurtTime")).intValue();
                    if (cooldown > 0) cooldown--;
                    if (hurt > previousHurt && cooldown == 0
                            && ThreadLocalRandom.current().nextDouble(100.0) < chance.getValue()) {
                        scheduled = delay.getValue().intValue();
                    }
                    previousHurt = hurt;
                    if (scheduled > 0) scheduled--;
                    else if (scheduled == 0 && Boolean.TRUE.equals(ModernMinecraftAccess.invoke(player, "onGround"))) {
                        ModernMinecraftAccess.invoke(player, "jumpFromGround");
                        scheduled = -1;
                        cooldown = 6;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
        });
    }

    private void registerSafeWalk() {
        modules.add(new ModernModule("safe_walk", "SafeWalk",
                "Automatically sneak at block edges when the next step is air.", ModernCategory.GHOST, false) {
            private boolean forced;

            @Override
            public void onTick(Object client) {
                boolean edge = isWalkingTowardAir(client);
                if (edge) {
                    setShiftKey(client, true);
                    forced = true;
                } else if (forced) {
                    setShiftKey(client, false);
                    forced = false;
                }
            }

            @Override
            public void onDisable(Object client) {
                if (forced) setShiftKey(client, false);
                forced = false;
            }
        });
    }

    private void registerYouTubePip() {
        modules.add(new ModernModule("youtube_pip", "YouTube PiP",
                "Displays the current YouTube video in picture-in-picture.", ModernCategory.OTHER, false) {
            {
                setting(new ComboSetting("quality", "Quality", "480p", "360p", "480p", "720p"));
            }
        });
    }

    private boolean hasScreen(Object client) {
        try {
            return ModernMinecraftAccess.field(client, "screen") != null;
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    private Object player() {
        try {
            return ModernMinecraftAccess.field(minecraft, "player");
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private List<Object> otherPlayers() {
        if (hudFrameActive) return hudPlayers;
        List<Object> result = new ArrayList<Object>();
        Object local = player();
        if (local == null) return result;
        try {
            Object level = ModernMinecraftAccess.field(minecraft, "level");
            if (level == null) return result;
            Iterable<?> players = (Iterable<?>) ModernMinecraftAccess.invoke(level, "players");
            for (Object target : players) {
                if (target != local && Boolean.TRUE.equals(ModernMinecraftAccess.invoke(target, "isAlive"))) {
                    result.add(target);
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return result;
    }

    private void prepareHudFrame() {
        hudPlayers.clear();
        Object local = player();
        if (local != null) {
            try {
                Object level = ModernMinecraftAccess.field(minecraft, "level");
                if (level != null) {
                    Iterable<?> players = (Iterable<?>) ModernMinecraftAccess.invoke(level, "players");
                    for (Object target : players) {
                        if (target != local
                                && Boolean.TRUE.equals(ModernMinecraftAccess.invoke(target, "isAlive"))) {
                            hudPlayers.add(target);
                        }
                    }
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        hudFrameActive = true;
        try {
            prepareProjection(minecraft.getClass().getClassLoader());
        } catch (Throwable ignored) {
            hudProjectionMatrix = null;
            hudProjectionVector = null;
        }
    }

    private void prepareProjection(ClassLoader loader) throws ReflectiveOperationException,
            ClassNotFoundException, InstantiationException, IllegalAccessException {
        Object gameRenderer = ModernMinecraftAccess.field(minecraft, "gameRenderer");
        Object camera = ModernMinecraftAccess.invoke(gameRenderer, "getMainCamera");
        Object position = ModernMinecraftAccess.invoke(camera, "position");
        hudCameraX = ModernMinecraftAccess.number(position, "x");
        hudCameraY = ModernMinecraftAccess.number(position, "y");
        hudCameraZ = ModernMinecraftAccess.number(position, "z");
        Class<?> matrixType = Class.forName("org.joml.Matrix4f", true, loader);
        if (hudProjectionMatrix == null) hudProjectionMatrix = matrixType.newInstance();
        hudProjectionMatrix = ModernMinecraftAccess.invoke(
                camera, "getViewRotationProjectionMatrix", hudProjectionMatrix);
        if (hudProjectionVector == null) {
            hudProjectionVector = Class.forName("org.joml.Vector3f", true, loader).newInstance();
        }
    }

    private float[] project(double worldX, double worldY, double worldZ) {
        try {
            ClassLoader loader = minecraft.getClass().getClassLoader();
            if (!hudFrameActive || hudProjectionMatrix == null || hudProjectionVector == null) {
                prepareProjection(loader);
            }
            double relativeX = worldX - hudCameraX;
            double relativeY = worldY - hudCameraY;
            double relativeZ = worldZ - hudCameraZ;
            Object projected = hudProjectionVector;
            ModernMinecraftAccess.invoke(hudProjectionMatrix, "transformProject",
                    Float.valueOf((float) relativeX), Float.valueOf((float) relativeY),
                    Float.valueOf((float) relativeZ), projected);
            float x = ((Number) ModernMinecraftAccess.invoke(projected, "x")).floatValue();
            float y = ((Number) ModernMinecraftAccess.invoke(projected, "y")).floatValue();
            float z = ((Number) ModernMinecraftAccess.invoke(projected, "z")).floatValue();
            if (z < -1.0f || z > 1.0f) return null;
            return new float[] {
                    (x + 1.0f) * 0.5f * ImGui.getIO().getDisplaySizeX(),
                    (1.0f - y) * 0.5f * ImGui.getIO().getDisplaySizeY()
            };
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean mouseButtonDown(Object client, int button) {
        try {
            Object window = ModernMinecraftAccess.invoke(client, "getWindow");
            long handle = ModernMinecraftAccess.windowHandle(window);
            Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW", true, client.getClass().getClassLoader());
            return ((Number) glfw.getMethod("glfwGetMouseButton", long.class, int.class)
                    .invoke(null, handle, button)).intValue() == 1;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean keyboardKeyDown(Object client, int key) {
        try {
            Object window = ModernMinecraftAccess.invoke(client, "getWindow");
            long handle = ModernMinecraftAccess.windowHandle(window);
            Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW", true, client.getClass().getClassLoader());
            return ((Number) glfw.getMethod("glfwGetKey", long.class, int.class)
                    .invoke(null, handle, key)).intValue() == 1;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isWalkingTowardAir(Object client) {
        try {
            if (hasScreen(client)) return false;
            Object player = ModernMinecraftAccess.field(client, "player");
            Object level = ModernMinecraftAccess.field(client, "level");
            if (player == null || level == null
                    || !Boolean.TRUE.equals(ModernMinecraftAccess.invoke(player, "onGround"))) return false;

            Object options = ModernMinecraftAccess.field(client, "options");
            double forward = keyDown(options, "keyUp") ? 1.0 : 0.0;
            if (keyDown(options, "keyDown")) forward -= 1.0;
            double strafe = keyDown(options, "keyLeft") ? 1.0 : 0.0;
            if (keyDown(options, "keyRight")) strafe -= 1.0;
            if (forward == 0.0 && strafe == 0.0) return false;

            double length = Math.sqrt(forward * forward + strafe * strafe);
            forward /= length;
            strafe /= length;
            double yaw = Math.toRadians(((Number) ModernMinecraftAccess.invoke(player, "getYRot")).doubleValue());
            double directionX = -Math.sin(yaw) * forward + Math.cos(yaw) * strafe;
            double directionZ = Math.cos(yaw) * forward + Math.sin(yaw) * strafe;
            Object velocity = ModernMinecraftAccess.invoke(player, "getDeltaMovement");
            double motionX = ((Number) ModernMinecraftAccess.field(velocity, "x")).doubleValue();
            double motionZ = ((Number) ModernMinecraftAccess.field(velocity, "z")).doubleValue();
            double stepX = motionX + directionX * 0.16;
            double stepZ = motionZ + directionZ * 0.16;

            Object box = ModernMinecraftAccess.invoke(player, "getBoundingBox");
            double minX = ((Number) ModernMinecraftAccess.field(box, "minX")).doubleValue() + stepX;
            double maxX = ((Number) ModernMinecraftAccess.field(box, "maxX")).doubleValue() + stepX;
            double minZ = ((Number) ModernMinecraftAccess.field(box, "minZ")).doubleValue() + stepZ;
            double maxZ = ((Number) ModernMinecraftAccess.field(box, "maxZ")).doubleValue() + stepZ;
            double belowY = ((Number) ModernMinecraftAccess.field(box, "minY")).doubleValue() - 0.08;
            double middleX = (minX + maxX) * 0.5;
            double middleZ = (minZ + maxZ) * 0.5;
            double inset = 0.035;

            // Probe the complete leading edge of the player's future
            // footprint. A single center probe misses diagonal and high-speed
            // approaches; three points per moving axis remain stable on slabs
            // and narrow blocks while catching the edge before walking off.
            boolean unsupported = false;
            if (Math.abs(stepX) > 0.001) {
                double edgeX = stepX > 0.0 ? maxX + 0.055 : minX - 0.055;
                unsupported |= !hasSupport(client, level, edgeX, belowY, minZ + inset);
                unsupported |= !hasSupport(client, level, edgeX, belowY, middleZ);
                unsupported |= !hasSupport(client, level, edgeX, belowY, maxZ - inset);
            }
            if (Math.abs(stepZ) > 0.001) {
                double edgeZ = stepZ > 0.0 ? maxZ + 0.055 : minZ - 0.055;
                unsupported |= !hasSupport(client, level, minX + inset, belowY, edgeZ);
                unsupported |= !hasSupport(client, level, middleX, belowY, edgeZ);
                unsupported |= !hasSupport(client, level, maxX - inset, belowY, edgeZ);
            }
            return unsupported;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean hasSupport(Object client, Object level, double x, double y, double z) {
        try {
            Class<?> blockPos = Class.forName("net.minecraft.core.BlockPos", true,
                    client.getClass().getClassLoader());
            Object position = blockPos.getMethod("containing", double.class, double.class, double.class)
                    .invoke(null, x, y, z);
            Object state = ModernMinecraftAccess.invoke(level, "getBlockState", position);
            if (Boolean.TRUE.equals(ModernMinecraftAccess.invoke(state, "isAir"))) return false;
            try {
                Object shape = ModernMinecraftAccess.invoke(state, "getCollisionShape", level, position);
                return !Boolean.TRUE.equals(ModernMinecraftAccess.invoke(shape, "isEmpty"));
            } catch (ReflectiveOperationException unavailableShape) {
                return true;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean keyDown(Object options, String field) throws ReflectiveOperationException {
        Object mapping = ModernMinecraftAccess.field(options, field);
        return Boolean.TRUE.equals(ModernMinecraftAccess.invoke(mapping, "isDown"));
    }

    private void setShiftKey(Object client, boolean down) {
        try {
            Object options = ModernMinecraftAccess.field(client, "options");
            Object shift = ModernMinecraftAccess.field(options, "keyShift");
            ModernMinecraftAccess.invoke(shift, "setDown", down);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static float wrapDegrees(float value) {
        value %= 360.0f;
        if (value >= 180.0f) value -= 360.0f;
        if (value < -180.0f) value += 360.0f;
        return value;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clampDouble(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static int abgr(int red, int green, int blue, int alpha) {
        return ((alpha & 0xFF) << 24) | ((blue & 0xFF) << 16)
                | ((green & 0xFF) << 8) | (red & 0xFF);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public void onTick() {
        refreshFastState();
        for (ModernModule module : modules) {
            int keybind = module.getKeybind();
            boolean pressed = keybind >= 0 && keyboardKeyDown(minecraft, keybind);
            boolean wasPressed = Boolean.TRUE.equals(bindDown.get(module.getId()));
            if (pressed && !wasPressed && !"settings".equals(module.getId())) {
                toggle(module);
            }
            bindDown.put(module.getId(), pressed);
            if (module.isEnabled()) {
                try {
                    module.onTick(minecraft);
                } catch (Throwable error) {
                    GlideLogger.warn("Modern module tick failed: " + module.getName());
                }
            }
        }
    }

    public void onHudRender() {
        ModernHudRenderer.setBackground(
                hudBackground == null || hudBackground.getValue());
        ModernHudRenderer.begin();
        if (needsProjectedHud()) prepareHudFrame();
        else hudFrameActive = false;
        try {
            for (ModernModule module : modules) {
                if (module.isEnabled()) {
                    module.onHudRender();
                }
            }
        } finally {
            hudFrameActive = false;
        }
    }

    public void onFrame() {
        for (ModernModule module : modules) {
            if (module.isEnabled()) {
                try {
                    module.onFrame(minecraft);
                } catch (Throwable error) {
                    GlideLogger.warn("Modern module frame failed: " + module.getName());
                }
            }
        }
    }

    private boolean needsProjectedHud() {
        for (ModernModule module : modules) {
            if (!module.isEnabled()) continue;
            String id = module.getId();
            if ("ghost_nametags".equals(id) || "healthbar".equals(id)) return true;
            if ("esp".equals(id) && !realEspActive) return true;
        }
        return false;
    }

    public boolean onMouseTurn(Object mouseHandler) {
        ModernScaffoldModule scaffoldModule = scaffold;
        if (scaffoldModule != null && scaffoldModule.isEnabled()
                && scaffoldModule.onMouseTurn(mouseHandler)) return true;
        for (ModernModule module : modules) {
            if (module == scaffoldModule) continue;
            if (module.isEnabled() && module.onMouseTurn(mouseHandler)) return true;
        }
        return false;
    }

    public void onCameraUpdate(Object camera) {
        for (ModernModule module : modules) {
            if (module.isEnabled()) module.onCameraUpdate(camera);
        }
    }

    public void onLocalPlayerTick(Object player) {
        ModernScaffoldModule module = scaffold;
        if (module != null && module.isEnabled()) module.onTick(minecraft);
    }

    public void onLocalPlayerAiStepHead(Object player) {
        ModernScaffoldModule module = scaffold;
        if (module != null && module.isEnabled()) module.onAiStepHead(player);
    }

    public void onLocalPlayerAiStepTail(Object player) {
        ModernScaffoldModule module = scaffold;
        if (module != null && module.isEnabled()) module.onAiStepTail(player);
    }

    public void onLocalPlayerSendPositionHead(Object player) {
        ModernScaffoldModule module = scaffold;
        if (module != null && module.isEnabled()) module.onSendPositionHead(player);
    }

    public void onLocalPlayerSendPositionTail(Object player) {
        ModernScaffoldModule module = scaffold;
        if (module != null && module.isEnabled()) module.onSendPositionTail(player);
    }

    public Object onLivingEntityTravel(Object entity, Object movement) {
        ModernScaffoldModule module = scaffold;
        return module != null && module.isEnabled()
                ? module.onTravel(entity, movement) : movement;
    }

    public float[] getAccentColor() {
        float hue = accentHue == null ? 0.62f : accentHue.getValue().floatValue();
        float saturation = 0.62f;
        float brightness = 0.96f;
        float sector = (hue - (float) Math.floor(hue)) * 6.0f;
        int index = (int) sector;
        float fraction = sector - index;
        float low = brightness * (1.0f - saturation);
        float descending = brightness * (1.0f - saturation * fraction);
        float ascending = brightness * (1.0f - saturation * (1.0f - fraction));
        switch (index) {
            case 0: return new float[] {brightness, ascending, low};
            case 1: return new float[] {descending, brightness, low};
            case 2: return new float[] {low, brightness, ascending};
            case 3: return new float[] {low, descending, brightness};
            case 4: return new float[] {ascending, low, brightness};
            default: return new float[] {brightness, low, descending};
        }
    }

    public boolean isRealEspEnabled() {
        return realEspActive;
    }

    private void refreshFastState() {
        boolean active = false;
        for (ModernModule module : modules) {
            if (!"esp".equals(module.getId()) || !module.isEnabled()) continue;
            for (ModernSetting<?> setting : module.getSettings()) {
                if ("mode".equals(setting.getKey())) {
                    active = "Real".equals(String.valueOf(setting.getValue()));
                    break;
                }
            }
        }
        realEspActive = active;
    }

    public void setNametagFonts(ImFont interfaceFont, ImFont minecraftFont, ImFont productFont) {
        this.nametagInterfaceFont = interfaceFont;
        this.nametagMinecraftFont = minecraftFont;
        this.nametagProductFont = productFont;
    }

    private ImFont nametagFont(String mode) {
        if ("Mojangles".equals(mode) && nametagMinecraftFont != null) return nametagMinecraftFont;
        if ("Product Sans".equals(mode) && nametagProductFont != null) return nametagProductFont;
        return nametagInterfaceFont == null ? ImGui.getFont() : nametagInterfaceFont;
    }

    public void toggle(ModernModule module) {
        module.setEnabled(!module.isEnabled(), minecraft);
        refreshFastState();
        save();
    }

    public List<ModernModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<ModernModule> getModules(ModernCategory category) {
        List<ModernModule> result = new ArrayList<ModernModule>();
        for (ModernModule module : modules) {
            if (module.getCategory() == category) {
                result.add(module);
            }
        }
        return result;
    }

    public void save() {
        Properties properties = new Properties();
        for (ModernModule module : modules) {
            properties.setProperty("module." + module.getId(), Boolean.toString(module.isEnabled()));
            properties.setProperty("bind." + module.getId(), Integer.toString(module.getKeybind()));
            for (ModernSetting<?> setting : module.getSettings()) {
                properties.setProperty(
                        "setting." + module.getId() + "." + setting.getKey(),
                        setting.serialize());
            }
        }
        File parent = configFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            return;
        }
        try (FileOutputStream output = new FileOutputStream(configFile)) {
            properties.store(output, "FlaxClient Lunar 26.1.2");
        } catch (IOException error) {
            GlideLogger.warn("Could not save the Lunar 26.1.2 config");
        }
    }

    private void load() {
        if (!configFile.isFile()) {
            return;
        }
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(configFile)) {
            properties.load(input);
        } catch (IOException error) {
            GlideLogger.warn("Could not read the Lunar 26.1.2 config");
            return;
        }
        for (ModernModule module : modules) {
            String enabled = properties.getProperty("module." + module.getId());
            if (enabled != null) {
                module.setEnabled(Boolean.parseBoolean(enabled), minecraft);
            }
            String bind = properties.getProperty("bind." + module.getId());
            if (bind != null) {
                try {
                    module.setKeybind(Integer.parseInt(bind));
                } catch (NumberFormatException ignored) {
                }
            }
            for (ModernSetting<?> setting : module.getSettings()) {
                String value = properties.getProperty(
                        "setting." + module.getId() + "." + setting.getKey());
                if (value != null) {
                    setting.deserialize(value);
                }
            }
        }
    }
}
