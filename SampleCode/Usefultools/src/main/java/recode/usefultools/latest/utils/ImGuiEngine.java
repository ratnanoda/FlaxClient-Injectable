/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  imgui.ImFont
 *  imgui.ImGui
 *  imgui.ImGuiIO
 *  imgui.ImGuiStyle
 *  imgui.gl3.ImGuiImplGl3
 *  imgui.glfw.ImGuiImplGlfw
 *  net.minecraft.client.Minecraft
 *  net.minecraft.resources.Identifier
 *  net.minecraft.server.packs.resources.Resource
 *  org.lwjgl.opengl.GL11
 *  org.lwjgl.opengl.GL13
 *  org.lwjgl.opengl.GL15
 *  org.lwjgl.opengl.GL20
 *  org.lwjgl.opengl.GL30
 */
package recode.usefultools.latest.utils;

import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Visual.ClickGui.ClickGui;
import recode.usefultools.latest.mixin.WindowAccessor;
import recode.usefultools.latest.utils.AccountManager;

public class ImGuiEngine {
    public final static ImGuiEngine INSTANCE = new ImGuiEngine();
    public final Map<String, ImFont> fonts = new HashMap<String, ImFont>();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private boolean initialized = false;

    public void onFrame() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) {
            return;
        }
        if (!this.initialized) {
            this.initImGui(mc);
            this.initialized = true;
        }
        this.imGuiGlfw.newFrame();
        this.imGuiGl3.newFrame();
        ImGui.newFrame();
        float deltaTime = ImGui.getIO().getDeltaTime();
        if (ClickGui.instance != null) {
            ClickGui.instance.updateSmoothAnimations(deltaTime);
        }
        ModuleManager.INSTANCE.onRenderHUD();
        if (ClickGui.isVisible()) {
            ClickGui.drawImGui();
        }
        AccountManager.INSTANCE.draw();
        ImGui.render();
        if (ClickGui.isVisible() || ModuleManager.INSTANCE.getModules().stream().anyMatch(m -> ((ModuleHeader)m.h).enabled && (((ModuleHeader)m.h).name.equals("Watermark") || ((ModuleHeader)m.h).name.equals("ArrayList") || ((ModuleHeader)m.h).name.equals("Nametags")))) {
            int last_program = GL11.glGetInteger(35725);
            int last_active_texture = GL11.glGetInteger(34016);
            GL13.glActiveTexture(33984);
            int last_texture = GL11.glGetInteger(32873);
            int last_array_buffer = GL11.glGetInteger(34964);
            int last_vertex_array = GL11.glGetInteger(34229);
            int[] last_viewport = new int[4];
            GL11.glGetIntegerv(2978, (int[])last_viewport);
            int[] last_scissor = new int[4];
            GL11.glGetIntegerv(3088, (int[])last_scissor);
            int last_fbo = GL11.glGetInteger(36006);
            boolean last_enable_blend = GL11.glIsEnabled(3042);
            boolean last_enable_cull = GL11.glIsEnabled(2884);
            boolean last_enable_depth = GL11.glIsEnabled(2929);
            boolean last_enable_scissor = GL11.glIsEnabled(3089);
            GL30.glBindFramebuffer(36160, 0);
            GL11.glViewport(0, 0, (int)mc.getWindow().getWidth(), (int)mc.getWindow().getHeight());
            GL11.glDisable(2929);
            GL11.glDisable(2884);
            GL11.glDisable(3089);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            this.imGuiGl3.renderDrawData(ImGui.getDrawData());
            GL20.glUseProgram((int)last_program);
            GL30.glBindVertexArray((int)last_vertex_array);
            GL15.glBindBuffer(34962, (int)last_array_buffer);
            GL11.glBindTexture(3553, (int)last_texture);
            GL13.glActiveTexture((int)last_active_texture);
            GL30.glBindFramebuffer(36160, (int)last_fbo);
            GL11.glViewport((int)last_viewport[0], (int)last_viewport[1], (int)last_viewport[2], (int)last_viewport[3]);
            GL11.glScissor((int)last_scissor[0], (int)last_scissor[1], (int)last_scissor[2], (int)last_scissor[3]);
            if (last_enable_blend) {
                GL11.glEnable(3042);
            } else {
                GL11.glDisable(3042);
            }
            if (last_enable_cull) {
                GL11.glEnable(2884);
            } else {
                GL11.glDisable(2884);
            }
            if (last_enable_depth) {
                GL11.glEnable(2929);
            } else {
                GL11.glDisable(2929);
            }
            if (last_enable_scissor) {
                GL11.glEnable(3089);
            } else {
                GL11.glDisable(3089);
            }
        } else {
            ImGui.render();
        }
    }

    private void initImGui(Minecraft mc) {
        WindowAccessor window = (WindowAccessor)mc.getWindow();
        ImGui.createContext();
        ImGui.getIO().setIniFilename(null);
        this.fonts.put("main", this.loadFont(ImGui.getIO(), "product_sans.ttf", 19.0f));
        this.fonts.put("main_bold", this.loadFont(ImGui.getIO(), "product_sans_bold.ttf", 19.0f));
        this.fonts.put("watermark", this.loadFont(ImGui.getIO(), "product_sans_bold.ttf", 45.0f));
        this.fonts.put("icons", this.loadFont(ImGui.getIO(), "tenacity_icons.ttf", 24.0f));
        this.fonts.put("minecraft", this.loadFont(ImGui.getIO(), "mojangles.ttf", 18.0f));
        this.fonts.put("minecraft_bold", this.loadFont(ImGui.getIO(), "mojangles_bold.ttf", 18.0f));
        this.fonts.put("minecraft_large", this.loadFont(ImGui.getIO(), "mojangles.ttf", 32.0f));
        this.fonts.put("minecraft_bold_large", this.loadFont(ImGui.getIO(), "mojangles_bold.ttf", 32.0f));
        this.io_setup(window.getWindowHandle());
    }

    private void io_setup(long handle) {
        this.imGuiGlfw.init(handle, true);
        this.imGuiGl3.init("#version 150");
        ImGuiStyle style = ImGui.getStyle();
        style.setWindowRounding(12.0f);
        style.setWindowBorderSize(0.0f);
        style.setColor(2, 0.06f, 0.06f, 0.06f, 0.94f);
        style.setColor(18, 0.0f, 0.6f, 1.0f, 1.0f);
    }

    private ImFont loadFont(ImGuiIO io, String name, float size) {
        Identifier id = Identifier.parse((String)("recode-useful-tools:fonts/" + name));
        Optional res = Minecraft.getInstance().getResourceManager().getResource(id);
        if (res.isPresent()) {
            ImFont imFont;
            block9: {
                InputStream is = ((Resource)res.get()).open();
                try {
                    imFont = io.getFonts().addFontFromMemoryTTF(is.readAllBytes(), size);
                    if (is == null) break block9;
                } catch (Throwable throwable) {
                    try {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }
                        throw throwable;
                    } catch (Exception e) {
                        System.err.println("Error loading font: " + name);
                    }
                }
                is.close();
            }
            return imFont;
        }
        return io.getFonts().addFontDefault();
    }
}

