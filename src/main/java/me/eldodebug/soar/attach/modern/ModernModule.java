package me.eldodebug.soar.attach.modern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModernModule {

    private final String id;
    private final String name;
    private final String description;
    private final ModernCategory category;
    private final List<ModernSetting<?>> settings = new ArrayList<ModernSetting<?>>();
    private boolean enabled;
    private int keybind = -1;

    public ModernModule(
            String id,
            String name,
            String description,
            ModernCategory category,
            boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = enabled;
    }

    public void onTick(Object minecraft) {
    }

    public void onHudRender() {
    }

    public void onFrame(Object minecraft) {
    }

    public boolean onMouseTurn(Object mouseHandler) {
        return false;
    }

    public void onCameraUpdate(Object camera) {
    }

    public void onEnable(Object minecraft) {
    }

    public void onDisable(Object minecraft) {
    }

    protected <T extends ModernSetting<?>> T setting(T setting) {
        settings.add(setting);
        return setting;
    }

    public void setEnabled(boolean enabled, Object minecraft) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            onEnable(minecraft);
        } else {
            onDisable(minecraft);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ModernCategory getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<ModernSetting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public int getKeybind() {
        return keybind;
    }

    public void setKeybind(int keybind) {
        this.keybind = keybind;
    }
}
