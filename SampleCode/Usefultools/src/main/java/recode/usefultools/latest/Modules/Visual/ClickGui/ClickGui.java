/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  imgui.ImDrawList
 *  imgui.ImFont
 *  imgui.ImGui
 *  imgui.ImVec2
 *  net.minecraft.client.Minecraft
 *  org.lwjgl.glfw.GLFW
 */
package recode.usefultools.latest.Modules.Visual.ClickGui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.Category;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.Modules.Visual.ClickGui.ClickGui_h;
import recode.usefultools.latest.Modules.Visual.Interface.Interface;
import recode.usefultools.latest.Modules.Visual.Interface.Interface_h;
import recode.usefultools.latest.mixin.MouseHandlerAccessor;
import recode.usefultools.latest.mixin.WindowAccessor;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.NumberSetting;
import recode.usefultools.latest.setting.Setting;
import recode.usefultools.latest.utils.BlurRenderer;
import recode.usefultools.latest.utils.ImGuiEngine;
import recode.usefultools.latest.utils.MathUtils;

public class ClickGui
extends BaseModule<ClickGui_h> {
    public static ClickGui instance;
    private static float anim;
    private static boolean closing;
    private static Tab currentTab;
    private static float tabUnderlineX;
    private static float tabUnderlineWidth;
    private final static Map<Category, CatState> catStates;
    private final static Map<String, Boolean> showSettings;
    private final static Map<String, Float> cAnim;
    private final static Map<String, Float> expandAnims;
    private static String tooltipText;

    public ClickGui() {
        super(new ClickGui_h());
        instance = this;
    }

    @Override
    public void onEnable() {
        closing = false;
        if (mc.getWindow() != null) {
            MouseHandlerAccessor mha = (MouseHandlerAccessor)ClickGui.mc.mouseHandler;
            mha.setAccumulatedDX(0.0);
            mha.setAccumulatedDY(0.0);
            ClickGui.mc.mouseHandler.releaseMouse();
            WindowAccessor wa = (WindowAccessor)mc.getWindow();
            GLFW.glfwSetInputMode((long)wa.getWindowHandle(), 208897, 212993);
        }
        catStates.clear();
    }

    @Override
    public void onDisable() {
        closing = true;
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().mouseHandler.grabMouse());
    }

    @Override
    public void onUpdate() {
        WindowAccessor wa;
        float target = ((ClickGui_h)this.h).enabled && !closing ? 1.0f : 0.0f;
        float speed = (float)(closing ? ((ClickGui_h)this.h).outAnimSpeed.value : ((ClickGui_h)this.h).inAnimSpeed.value);
        anim += (target - anim) * speed;
        if (closing && anim < 0.01f) {
            anim = 0.0f;
            closing = false;
        }
        float deltaTime = ImGui.getIO().getDeltaTime();
        for (BaseModule<?> m : ModuleManager.INSTANCE.getModules()) {
            boolean show = showSettings.getOrDefault(((ModuleHeader)m.h).name, false);
            float currentC = cAnim.getOrDefault(((ModuleHeader)m.h).name, Float.valueOf(0.0f)).floatValue();
            cAnim.put(((ModuleHeader)m.h).name, Float.valueOf(currentC + ((show ? 1.0f : 0.0f) - currentC) * 15.0f * deltaTime));
            float currentE = expandAnims.getOrDefault(((ModuleHeader)m.h).name, Float.valueOf(0.0f)).floatValue();
            expandAnims.put(((ModuleHeader)m.h).name, Float.valueOf(currentE + ((show ? 1.0f : 0.0f) - currentE) * 15.0f * deltaTime));
        }
        if (((ClickGui_h)this.h).enabled && !closing && (wa = (WindowAccessor)mc.getWindow()) != null && GLFW.glfwGetKey((long)wa.getWindowHandle(), 256) == 1) {
            this.setEnabled(false);
        }
    }

    public void updateSmoothAnimations(float dt) {
        this.onUpdate();
    }

    public static void updateAnimation() {
        if (instance != null) {
            instance.onUpdate();
        }
    }

    public static boolean isVisible() {
        return instance != null && (((ClickGui_h)ClickGui.instance.h).enabled || anim > 0.01f);
    }

    public static void drawImGui() {
        if (instance == null || anim <= 0.01f) {
            return;
        }
        tooltipText = "";
        Interface iface = (Interface)ModuleManager.INSTANCE.getModuleByName("Interface");
        float eased = MathUtils.easeOutExpo(anim);
        if (iface == null || iface.h == null || ((Interface_h)iface.h).font == null || ((Interface_h)iface.h).font.value == null) {
            return;
        }
        boolean isMcFont = ((Interface_h)iface.h).font.value == Interface_h.FontType.Mojangles;
        ImGui.pushFont((ImFont)ImGuiEngine.INSTANCE.fonts.getOrDefault(isMcFont ? "minecraft" : "main", ImGuiEngine.INSTANCE.fonts.get("main")));
        float vw = ImGui.getIO().getDisplaySizeX();
        float vh = ImGui.getIO().getDisplaySizeY();
        float slideY = (1.0f - eased) * -vh;
        ImDrawList dl = ImGui.getBackgroundDrawList();
        if (((ClickGui_h)ClickGui.instance.h).bgType.value == ClickGui_h.Background.New) {
            BlurRenderer.renderBlur((float)((ClickGui_h)ClickGui.instance.h).blurStrength.value * eased);
        } else {
            dl.addRectFilled(0.0f, slideY, vw, vh + slideY, ImGui.getColorU32(0.0f, 0.0f, 0.0f, (float)(eased * 0.42f)));
        }
        ClickGui.drawTabs(vw, eased, iface, slideY);
        float panelWidth = 155.0f;
        float spacing = 20.0f;
        float totalWidth = (float)Category.values().length * panelWidth + (float)(Category.values().length - 1) * spacing;
        float baseStartX = (vw - totalWidth) / 2.0f + (float)((ClickGui_h)ClickGui.instance.h).xOffset.value;
        float baseStartY = 100.0f + (float)((ClickGui_h)ClickGui.instance.h).yOffset.value;
        ImGui.pushStyleColor(30, 0, 0, 0, 0);
        ImGui.pushStyleColor(32, 0, 0, 0, 0);
        int catIdx = 0;
        for (Category cat : Category.values()) {
            boolean hoverHeader;
            if (!catStates.containsKey((Object)cat)) {
                catStates.put(cat, new CatState(baseStartX + (float)catIdx * (panelWidth + spacing), baseStartY));
            }
            CatState cs = catStates.get((Object)cat);
            float curY = cs.y + slideY;
            float panelH = ClickGui.calculatePanelHeight(cat, cs, eased, curY, vh);
            float rounding = (float)((ClickGui_h)ClickGui.instance.h).rounding.value;
            dl.addRectFilled(cs.x, curY, cs.x + panelWidth, curY + panelH, ImGui.getColorU32(0.06f, 0.06f, 0.06f, (float)(eased * 0.95f)), rounding);
            float headerHeight = 35.0f;
            ImVec2 mPos = ImGui.getMousePos();
            boolean bl = hoverHeader = mPos.x >= cs.x && mPos.x <= cs.x + panelWidth && mPos.y >= curY && mPos.y <= curY + headerHeight;
            if (hoverHeader) {
                if (ImGui.isMouseClicked(0)) {
                    cs.dragging = true;
                }
                if (ImGui.isMouseClicked(1)) {
                    boolean bl2 = cs.isExtended = !cs.isExtended;
                }
            }
            if (cs.dragging) {
                if (ImGui.isMouseDown(0)) {
                    ImVec2 d = ImGui.getIO().getMouseDelta();
                    cs.x += d.x;
                    cs.y += d.y;
                } else {
                    cs.dragging = false;
                }
            }
            ImGui.setNextWindowPos((float)cs.x, (float)curY);
            ImGui.setNextWindowSize((float)panelWidth, (float)panelH);
            ImGui.setNextWindowBgAlpha(0.0f);
            ImGui.pushStyleVar(3, (float)rounding);
            ImGui.pushStyleVar(4, 0.0f);
            ImGui.pushStyleVar(2, 8.0f, 5.0f);
            int windowFlags = 159;
            if (ImGui.begin((String)("Solstice##" + cat.name()), (int)windowFlags)) {
                int color = iface.getCurrentColor(catIdx * 5);
                ImGui.pushFont((ImFont)ImGuiEngine.INSTANCE.fonts.get("icons"));
                String icon = switch (cat) {
                    case Category.COMBAT -> "c";
                    case Category.MOVEMENT -> "f";
                    case Category.VISUAL -> "d";
                    case Category.PLAYER -> "e";
                    default -> "a";
                };
                ImGui.textColored((int)color, (String)icon);
                ImGui.popFont();
                ImGui.sameLine();
                ImGui.pushFont((ImFont)ImGuiEngine.INSTANCE.fonts.get("main_bold"));
                ClickGui.drawText(cat.name(), -1, isMcFont);
                ImGui.popFont();
                ImGui.separator();
                if (cs.isExtended) {
                    ImGui.beginChild((String)("Scroll##" + cat.name()), (float)(panelWidth - 10.0f), (float)(panelH - 38.0f), (boolean)false, 136);
                    ClickGui.renderModulesWithBoxes(cat, iface, catIdx, panelWidth, isMcFont, rounding, eased);
                    ImGui.endChild();
                }
            }
            ImGui.end();
            ImGui.popStyleVar(3);
            ++catIdx;
        }
        ImGui.popStyleColor(2);
        if (!tooltipText.isEmpty()) {
            ClickGui.drawTooltip(eased);
        }
        ImGui.popFont();
    }

    private static float calculatePanelHeight(Category cat, CatState cs, float eased, float curY, float screenHeight) {
        float h = 35.0f;
        if (cs.isExtended) {
            float contentH = 0.0f;
            for (BaseModule<?> m : ModuleManager.INSTANCE.getModules()) {
                if (((ModuleHeader)m.h).category != cat) continue;
                contentH += 20.0f;
                float modAnim = cAnim.getOrDefault(((ModuleHeader)m.h).name, Float.valueOf(0.0f)).floatValue();
                if (!(modAnim > 0.01f)) continue;
                float sH = 0.0f;
                for (Setting s : ((ModuleHeader)m.h).settings) {
                    if (!s.isVisible()) continue;
                    sH += s instanceof BoolSetting ? 20.0f : 38.0f;
                }
                contentH += sH * modAnim;
            }
            float maxAllowedHeight = screenHeight - curY - 20.0f;
            maxAllowedHeight = Math.max(maxAllowedHeight, 150.0f);
            h += Math.min(contentH, maxAllowedHeight - 35.0f);
        }
        return h * eased;
    }

    private static void renderModulesWithBoxes(Category cat, Interface iface, int catIdx, float pWidth, boolean isMcFont, float rounding, float eased) {
        ImDrawList dl = ImGui.getWindowDrawList();
        int modIdx = 0;
        for (BaseModule<?> m : ModuleManager.INSTANCE.getModules()) {
            if (((ModuleHeader)m.h).category != cat) continue;
            boolean wasActive = ((ModuleHeader)m.h).enabled;
            boolean isExp = showSettings.getOrDefault(((ModuleHeader)m.h).name, false);
            float modAnim = cAnim.getOrDefault(((ModuleHeader)m.h).name, Float.valueOf(0.0f)).floatValue();
            int modColor = iface.getCurrentColor(catIdx * 5 + modIdx);
            ImVec2 pos = ImGui.getCursorScreenPos();
            if (modAnim > 0.01f) {
                float sH = 0.0f;
                for (Setting s : ((ModuleHeader)m.h).settings) {
                    if (!s.isVisible()) continue;
                    sH += s instanceof BoolSetting ? 20.0f : 38.0f;
                }
                float expandedH = sH * modAnim;
                dl.addRectFilled(pos.x - 2.0f, pos.y + 18.0f, pos.x + pWidth - 14.0f, pos.y + 18.0f + expandedH, ImGui.getColorU32(0.08f, 0.08f, 0.08f, (float)(modAnim * 0.9f)), rounding);
            }
            if (wasActive) {
                ImGui.pushStyleColor(0, (int)modColor);
            }
            if (ImGui.selectable((String)(((ModuleHeader)m.h).name + (((ModuleHeader)m.h).settings.isEmpty() ? "" : (isExp ? " -" : " +")) + "##" + ((ModuleHeader)m.h).name), (boolean)wasActive)) {
                m.toggle();
            }
            if (ImGui.isItemHovered()) {
                tooltipText = ((ModuleHeader)m.h).description;
            }
            if (ImGui.isItemClicked(1) && !((ModuleHeader)m.h).settings.isEmpty()) {
                showSettings.put(((ModuleHeader)m.h).name, !isExp);
            }
            if (wasActive) {
                ImGui.popStyleColor();
            }
            if (modAnim > 0.01f) {
                ImGui.pushStyleVar(0, (float)modAnim);
                ImGui.indent(5.0f);
                for (Setting s : ((ModuleHeader)m.h).settings) {
                    if (!s.isVisible()) continue;
                    ClickGui.renderSetting(s, ((ModuleHeader)m.h).name, iface, catIdx * 5 + modIdx, pWidth, isMcFont);
                }
                ImGui.popStyleVar();
                ImGui.unindent(5.0f);
            }
            ++modIdx;
        }
    }

    private static void renderSetting(Setting s, String modName, Interface iface, int idx, float pWidth, boolean isMcFont) {
        String id = "##" + s.name + modName;
        int accent = iface.getCurrentColor(idx);
        ImGui.pushStyleColor(18, (int)accent);
        ImGui.pushStyleColor(19, (int)accent);
        if (!(s instanceof BoolSetting)) {
            ClickGui.drawText(s.name, -5592406, isMcFont);
        }
        ImGui.setNextItemWidth((float)(pWidth - 30.0f));
        Setting setting = s;
        Objects.requireNonNull(setting);
        Setting setting2 = setting;
        int n = 0;
        switch (s) {
            case BoolSetting b: {
                if (!ImGui.checkbox((String)(s.name + id), (boolean)b.value)) break;
                b.value = !b.value;
                break;
            }
            case NumberSetting n: {
                float[] v = new float[]{(float)n.value};
                if (!ImGui.sliderFloat((String)id, (float[])v, (float)((float)n.min), (float)((float)n.max), (String)"%.2f")) break;
                n.value = v[0];
                break;
            }
            case EnumSetting e: {
                String displayName = ((Enum)e.value).name();
                int ordinal = ((Enum)e.value).ordinal();
                if (e.displayNames != null && ordinal < e.displayNames.length) {
                    displayName = e.displayNames[ordinal];
                }
                if (!ImGui.button((String)(e.name + ": " + displayName + id))) break;
                ClickGui.cycleEnum(e);
                break;
            }
        }
        ImGui.popStyleColor(2);
    }

    private static void drawTabs(float sw, float eased, Interface iface, float slideY) {
        ImDrawList dl = ImGui.getForegroundDrawList();
        float tabY = MathUtils.lerp(-40.0f, 20.0f, eased) + slideY;
        Tab[] ts = Tab.values();
        float startX = (sw - (float)(ts.length * 120)) / 2.0f;
        int accent = iface != null ? iface.getCurrentColor(0) : -1;
        for (int i = 0; i < ts.length; ++i) {
            float tx = startX + (float)(i * 120);
            boolean active = currentTab == ts[i];
            dl.addText(tx + 20.0f, tabY, active ? accent : -5592406, ts[i].name());
            if (ImGui.isMouseClicked(0)) {
                ImVec2 m = ImGui.getIO().getMousePos();
                if (m.x >= tx && m.x <= tx + 100.0f && m.y >= tabY && m.y <= tabY + 25.0f) {
                    currentTab = ts[i];
                }
            }
            if (!active) continue;
            tabUnderlineX = MathUtils.lerp(tabUnderlineX, tx + 15.0f, 0.15f);
            tabUnderlineWidth = MathUtils.lerp(tabUnderlineWidth, 90.0f, 0.15f);
        }
        dl.addLine(tabUnderlineX, tabY + 22.0f, tabUnderlineX + tabUnderlineWidth, tabY + 22.0f, accent, 2.0f);
    }

    private static void drawText(String text, int color, boolean round) {
        if (round) {
            ImVec2 pos = ImGui.getCursorScreenPos();
            ImGui.getWindowDrawList().addText((float)Math.round(pos.x), (float)Math.round(pos.y), color, text);
            ImGui.dummy(0.0f, 15.0f);
        } else {
            ImGui.textColored((int)color, (String)text);
        }
    }

    private static void drawTooltip(float eased) {
        ImVec2 m = ImGui.getMousePos();
        ImGui.setNextWindowPos((float)(m.x + 15.0f), (float)(m.y + 15.0f));
        ImGui.setNextWindowBgAlpha((float)(0.9f * eased));
        ImGui.pushStyleVar(3, 0.0f);
        if (ImGui.begin((String)"Tooltip", 65)) {
            ImGui.text((String)tooltipText);
            ImGui.end();
        }
        ImGui.popStyleVar();
    }

    private static <T extends Enum<T>> void cycleEnum(EnumSetting<T> e) {
        Enum[] v = (Enum[])e.value.getClass().getEnumConstants();
        e.value = v[(((Enum)e.value).ordinal() + 1) % v.length];
    }

    static {
        anim = 0.0f;
        closing = false;
        currentTab = Tab.ClickGui;
        tabUnderlineX = 0.0f;
        tabUnderlineWidth = 0.0f;
        catStates = new HashMap<Category, CatState>();
        showSettings = new HashMap<String, Boolean>();
        cAnim = new HashMap<String, Float>();
        expandAnims = new HashMap<String, Float>();
        tooltipText = "";
    }

    private static class CatState {
        float x;
        float y;
        boolean isExtended = true;
        boolean dragging = false;

        CatState(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static enum Tab {
        ClickGui,
        HudEditor,
        Scripting;

    }
}

