package me.eldodebug.soar.attach.modern;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.ImFont;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.attach.ModernClientRuntime;

public final class ModernImGuiEngine {

    private final Object minecraft;
    private final ModernModuleManager modules;
    private final ModernClickGui clickGui;
    private final ImGuiImplGlfw glfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 gl3 = new ImGuiImplGl3();
    private boolean initialized;
    private boolean failed;
    private boolean menuVisible;
    private final long notificationStartedAt = System.currentTimeMillis();

    public ModernImGuiEngine(Object minecraft, ModernModuleManager modules) {
        this.minecraft = minecraft;
        this.modules = modules;
        this.clickGui = new ModernClickGui(modules);
    }

    public void render() {
        if (failed) {
            return;
        }
        try {
            if (!initialized) {
                initialize();
            }
            glfw.newFrame();
            gl3.newFrame();
            ImGui.newFrame();
            float[] accent = modules.getAccentColor();
            ImGui.getStyle().setColor(21, accent[0], accent[1], accent[2], 1.0f);
            ImGui.getStyle().setColor(18, accent[0], accent[1], accent[2], 1.0f);
            modules.onHudRender();
            drawAttachNotification();
            if (menuVisible) {
                clickGui.draw();
            }
            ImGui.render();
            gl3.renderDrawData(ImGui.getDrawData());
        } catch (Throwable error) {
            failed = true;
            ModernClientRuntime.diagnostic("ImGui renderer failed", error);
            GlideLogger.error(
                    "Lunar 26.1.2 ImGui renderer failed",
                    error instanceof Exception ? (Exception) error : new Exception(error));
        }
    }

    public void setMenuVisible(boolean visible) {
        menuVisible = visible;
        if (initialized) {
            // Minecraft releases the GLFW cursor while the menu is open. Let
            // Windows draw that native cursor instead of layering ImGui's
            // software cursor on top of it.
            ImGui.getIO().setMouseDrawCursor(false);
        }
        clickGui.onVisibilityChanged(visible);
    }

    public boolean isMenuVisible() {
        return menuVisible;
    }

    public boolean onKeyInput(int key, int action) {
        return menuVisible && clickGui.onKeyInput(key, action);
    }

    public boolean consumeEscapeCaptured() {
        return clickGui.consumeEscapeCaptured();
    }

    private void initialize() throws ReflectiveOperationException {
        Object window = ModernMinecraftAccess.invoke(minecraft, "getWindow");
        long handle = ModernMinecraftAccess.windowHandle(window);
        ModernClientRuntime.diagnostic("Resolved GLFW window handle", null);

        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        ImFont defaultFont = addFont(io, "/assets/minecraft/soar/fonts/inter/Inter-Regular.ttf", 18.0f);
        if (defaultFont == null) {
            defaultFont = io.getFonts().addFontDefault();
        }
        // The legacy workspace is rendered in framebuffer coordinates. A 48px
        // source atlas remains sharp when the UI is scaled on 1440p/4K screens;
        // the former 18px atlas had to be enlarged and visibly blurred.
        ImFont regular = addFont(io, "/assets/minecraft/soar/fonts/inter/Inter-Regular.ttf", 36.0f);
        ImFont medium = addFont(io, "/assets/minecraft/soar/fonts/inter/Inter-Medium.ttf", 36.0f);
        ImFont semibold = addFont(io, "/assets/minecraft/soar/fonts/inter/Inter-SemiBold.ttf", 36.0f);
        ImFont icons = addFont(io, "/assets/minecraft/soar/fonts/Icon.ttf", 36.0f);
        ImFont mojangles = addFont(io, "/assets/minecraft/soar/fonts/mojangles.ttf", 36.0f);
        if (regular == null) regular = defaultFont;
        if (medium == null) medium = regular;
        if (semibold == null) semibold = regular;
        if (icons == null) icons = regular;
        if (mojangles == null) mojangles = regular;
        io.setFontDefault(defaultFont);
        clickGui.setFonts(regular, medium, semibold, icons);
        modules.setNametagFonts(regular, mojangles, medium);
        glfw.init(handle, true);
        gl3.init("#version 150");

        ImGuiStyle style = ImGui.getStyle();
        style.setWindowRounding(10.0f);
        style.setFrameRounding(6.0f);
        style.setGrabRounding(6.0f);
        style.setScrollbarRounding(8.0f);
        style.setWindowBorderSize(0.0f);
        style.setColor(2, 0.045f, 0.052f, 0.08f, 0.98f);
        style.setColor(7, 0.12f, 0.14f, 0.20f, 1.0f);
        style.setColor(21, 0.28f, 0.42f, 0.95f, 1.0f);
        style.setColor(18, 0.28f, 0.42f, 0.95f, 1.0f);

        initialized = true;
        ModernClientRuntime.diagnostic("ImGui renderer initialized", null);
        GlideLogger.info("Lunar 26.1.2 ImGui renderer initialized");
    }

    private ImFont addFont(ImGuiIO io, String resource, float size) {
        try {
            byte[] data = readResource(resource);
            return data == null ? null : io.getFonts().addFontFromMemoryTTF(data, size);
        } catch (Throwable unavailable) {
            ModernClientRuntime.diagnostic("Could not load UI font " + resource, unavailable);
            return null;
        }
    }

    private byte[] readResource(String resource) throws IOException {
        InputStream input = ModernImGuiEngine.class.getResourceAsStream(resource);
        if (input == null) return null;
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }

    private void drawAttachNotification() {
        if (System.currentTimeMillis() - notificationStartedAt > 8000L) {
            return;
        }
        String title = "Inject Successed!";
        String detail = "FlaxClient 26.1.2 is ready - Right Shift opens the menu";
        float width = 390.0f;
        float left = (ImGui.getIO().getDisplaySizeX() - width) / 2.0f;
        ImGui.getForegroundDrawList().addRectFilled(
                left, 20.0f, left + width, 82.0f, 0xEE18211C, 9.0f);
        ImGui.getForegroundDrawList().addRectFilled(
                left, 20.0f, left + 5.0f, 82.0f, 0xFF70DA70, 9.0f);
        ImGui.getForegroundDrawList().addText(left + 18.0f, 32.0f, 0xFFFFFFFF, title);
        ImGui.getForegroundDrawList().addText(left + 18.0f, 55.0f, 0xFFBCC6C0, detail);
    }
}
