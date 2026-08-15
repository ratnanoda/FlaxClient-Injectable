package me.eldodebug.soar.forge.gui;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import me.eldodebug.soar.forge.render.FogSettings;
import me.eldodebug.soar.logger.GlideLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Small Forge GUI for changing spatial fog settings while playing. */
@SideOnly(Side.CLIENT)
public final class GuiFogSettings extends GuiScreen {

    private final GuiScreen parent;
    /*
     * Keep this as Object.  Forge's 1.8.9 universal jar uses obfuscated GUI
     * superclasses, while this client compiles its Minecraft sources with MCP
     * names.  Loading GuiSlider reflectively avoids that compile-time mapping
     * collision; at runtime this is still Forge's GuiSlider.
     */
    private Object fogDistanceSlider;
    private Object fogDensitySlider;

    private GuiFogSettings(GuiScreen parent) {
        this.parent = parent;
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.displayGuiScreen(new GuiFogSettings(minecraft.currentScreen));
    }

    @Override
    public void initGui() {
        buttonList.clear();
        addFogDistanceSlider();
        addFogDensitySlider();
        buttonList.add(new GuiButton(101, width / 2 - 100, height / 2 + 46, 200, 20, "Done"));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void addFogDistanceSlider() {
        try {
            fogDistanceSlider = createSlider(100, height / 2 - 10,
                    "Fog distance: ", " blocks", 40.0D, 120.0D,
                    FogSettings.getFogEndDistance(), new SliderChange() {
                        @Override
                        public void onChange(float value) {
                            FogSettings.setFogEndDistance(value);
                        }
                    });
            ((java.util.List) buttonList).add(fogDistanceSlider);
        } catch (Exception e) {
            GlideLogger.error("Failed to create Forge fog distance slider", e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void addFogDensitySlider() {
        try {
            fogDensitySlider = createSlider(102, height / 2 + 14,
                    "Fog density: ", "%", 0.0D, 100.0D,
                    FogSettings.getFogDensity() * 100.0F, new SliderChange() {
                        @Override
                        public void onChange(float value) {
                            FogSettings.setFogDensity(value / 100.0F);
                        }
                    });
            ((java.util.List) buttonList).add(fogDensitySlider);
        } catch (Exception e) {
            GlideLogger.error("Failed to create Forge fog density slider", e);
        }
    }

    private Object createSlider(int id, int y, String prefix, String suffix,
            double minimum, double maximum, double initialValue,
            final SliderChange onChange) throws Exception {
        final Class<?> sliderClass = Class.forName("net.minecraftforge.fml.client.config.GuiSlider");
        final Class<?> sliderListenerClass = Class.forName("net.minecraftforge.fml.client.config.GuiSlider$ISlider");
        Object listener = Proxy.newProxyInstance(sliderListenerClass.getClassLoader(),
                new Class<?>[] { sliderListenerClass }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("onChangeSliderValue".equals(method.getName()) && args != null && args.length == 1) {
                            Object value = args[0].getClass().getMethod("getValue").invoke(args[0]);
                            onChange.onChange(((Number) value).floatValue());
                        }
                        return null;
                    }
                });
        return sliderClass.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE,
                Integer.TYPE, String.class, String.class, Double.TYPE, Double.TYPE, Double.TYPE, Boolean.TYPE,
                Boolean.TYPE, sliderListenerClass).newInstance(id, width / 2 - 100, y, 200, 20,
                        prefix, suffix, minimum, maximum, initialValue, false, true, listener);
    }

    private interface SliderChange {
        void onChange(float value);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 101) {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "Spatial fog settings", width / 2, height / 2 - 42, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "Fog begins at 30 blocks and gradually thickens with distance.",
                width / 2, height / 2 - 28, 0xB8B8B8);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
