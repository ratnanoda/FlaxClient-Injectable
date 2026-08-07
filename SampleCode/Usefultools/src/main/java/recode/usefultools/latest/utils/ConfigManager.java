/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.minecraft.client.Minecraft
 *  net.minecraft.network.chat.Component
 */
package recode.usefultools.latest.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Arrays;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import recode.usefultools.latest.Modules.BaseModule;
import recode.usefultools.latest.Modules.ModuleHeader;
import recode.usefultools.latest.Modules.ModuleManager;
import recode.usefultools.latest.setting.BoolSetting;
import recode.usefultools.latest.setting.ColorSetting;
import recode.usefultools.latest.setting.EnumSetting;
import recode.usefultools.latest.setting.ListSetting;
import recode.usefultools.latest.setting.NumberSetting;
import recode.usefultools.latest.setting.Setting;

public class ConfigManager {
    public final static ConfigManager INSTANCE = new ConfigManager();
    private final File configDir;
    private final Gson gson;
    public String lastLoadedConfig;
    public String pendingConfigName;

    public ConfigManager() {
        this.configDir = new File(Minecraft.getInstance().gameDirectory, "useful-tools-custom-config");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.lastLoadedConfig = "none";
        this.pendingConfigName = "";
    }

    public void init() {
        if (!this.configDir.exists()) {
            this.configDir.mkdirs();
        }
    }

    public void save(String name, boolean force) {
        if (!this.configDir.exists()) {
            this.configDir.mkdirs();
        }
        File targetFile = this.findConfigFile(name);
        if (!force && targetFile != null && !targetFile.getName().equalsIgnoreCase(this.lastLoadedConfig + ".json")) {
            this.pendingConfigName = name;
            this.sendMessage("§6[Warning]§f '" + name + "' already exists. Type §c.c confirm§e or §c.c s " + name);
            return;
        }
        try {
            JsonObject json = new JsonObject();
            for (BaseModule<?> m : ModuleManager.INSTANCE.getModules()) {
                if (m == null || m.h == null) continue;
                JsonObject mJson = new JsonObject();
                mJson.addProperty("enabled", Boolean.valueOf(((ModuleHeader)m.h).enabled));
                mJson.addProperty("key", (Number)((ModuleHeader)m.h).key);
                JsonObject sJson = new JsonObject();
                for (Setting s : ((ModuleHeader)m.h).settings) {
                    ListSetting l;
                    JsonArray arr;
                    if (s == null) continue;
                    if (s instanceof BoolSetting b) {
                        sJson.addProperty(s.name, Boolean.valueOf(b.value));
                        continue;
                    }
                    if (s instanceof NumberSetting n) {
                        sJson.addProperty(s.name, (Number)n.value);
                        continue;
                    }
                    if (s instanceof EnumSetting) {
                        EnumSetting e = (EnumSetting)s;
                        if (e.value == null) continue;
                        sJson.addProperty(s.name, ((Enum)e.value).name());
                        continue;
                    }
                    if (s instanceof ColorSetting) {
                        ColorSetting c = (ColorSetting)s;
                        if (c.rgba == null || c.rgba.length != 4) continue;
                        arr = new JsonArray();
                        for (Object val : (Object)c.rgba) {
                            arr.add((Number)Float.valueOf((float)val));
                        }
                        sJson.add(s.name, (JsonElement)arr);
                        continue;
                    }
                    if (!(s instanceof ListSetting) || null == (l = (ListSetting)s)) continue;
                    arr = new JsonArray();
                    for (String str : l.value) {
                        arr.add(str);
                    }
                    sJson.add(s.name, (JsonElement)arr);
                }
                mJson.add("settings", (JsonElement)sJson);
                json.add(((ModuleHeader)m.h).name, (JsonElement)mJson);
            }
            File saveFile = new File(this.configDir, name + ".json");
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(saveFile)));){
                writer.print(this.gson.toJson((JsonElement)json));
            }
            this.lastLoadedConfig = name;
            this.pendingConfigName = "";
            this.sendMessage("§aSaved§f config: §7" + name);
        } catch (Exception e) {
            e.printStackTrace();
            this.sendMessage("§cSave failed: §e" + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    public void load(String name) {
        File file = this.findConfigFile(name);
        if (file == null) {
            this.sendMessage("§cConfig '§7" + name + "§c' not found.");
            return;
        }
        try (FileReader reader = new FileReader(file);){
            JsonObject json = JsonParser.parseReader((Reader)reader).getAsJsonObject();
            for (BaseModule<?> m : ModuleManager.INSTANCE.getModules()) {
                if (m == null || m.h == null || !json.has(((ModuleHeader)m.h).name)) continue;
                JsonObject mJson = json.getAsJsonObject(((ModuleHeader)m.h).name);
                m.setEnabled(mJson.get("enabled").getAsBoolean());
                ((ModuleHeader)m.h).key = mJson.get("key").getAsInt();
                if (!mJson.has("settings")) continue;
                JsonObject sJson = mJson.getAsJsonObject("settings");
                for (Setting s : ((ModuleHeader)m.h).settings) {
                    if (s == null || !sJson.has(s.name)) continue;
                    try {
                        ListSetting l;
                        JsonArray arr;
                        if (s instanceof BoolSetting b) {
                            b.value = sJson.get(s.name).getAsBoolean();
                            continue;
                        }
                        if (s instanceof NumberSetting n) {
                            n.value = sJson.get(s.name).getAsDouble();
                            continue;
                        }
                        if (s instanceof EnumSetting e) {
                            this.restoreEnum(e, sJson.get(s.name).getAsString());
                            continue;
                        }
                        if (s instanceof ColorSetting) {
                            ColorSetting c = (ColorSetting)s;
                            arr = sJson.getAsJsonArray(s.name);
                            if (arr == null || arr.size() != 4 || c.rgba == null) continue;
                            c.rgba[0] = arr.get(0).getAsFloat();
                            c.rgba[1] = arr.get(1).getAsFloat();
                            c.rgba[2] = arr.get(2).getAsFloat();
                            c.rgba[3] = arr.get(3).getAsFloat();
                            continue;
                        }
                        if (!(s instanceof ListSetting) || null == (l = (ListSetting)s) || (arr = sJson.getAsJsonArray(s.name)) == null) continue;
                        l.value.clear();
                        for (JsonElement el : arr) {
                            l.value.add(el.getAsString());
                        }
                    } catch (Exception e) {
                        System.err.println("[UsefulTools] Gracefully skipped mismatched config setting: " + s.name + " in module: " + ((ModuleHeader)m.h).name);
                    }
                }
            }
            this.lastLoadedConfig = file.getName().replace(".json", "");
            this.sendMessage("§bLoaded§f config: §7" + this.lastLoadedConfig);
        } catch (Exception e) {
            e.printStackTrace();
            this.sendMessage("§cLoad failed: §e" + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    public void list() {
        File[] files = this.configDir.listFiles((dir, fName) -> fName.endsWith(".json"));
        if (files == null || files.length == 0) {
            this.sendMessage("§7No configs found.");
            return;
        }
        String list = Arrays.stream(files).map(f -> f.getName().replace(".json", "")).collect(Collectors.joining("§7, §f"));
        this.sendMessage("§bConfigs: §f" + list);
    }

    private <T extends Enum<T>> void restoreEnum(EnumSetting<T> setting, String name) {
        if (name == null || setting == null) {
            return;
        }
        try {
            Enum[] constants;
            for (Enum c : constants = (Enum[])setting.value.getClass().getEnumConstants()) {
                if (!c.name().equalsIgnoreCase(name)) continue;
                setting.value = c;
                return;
            }
        } catch (Exception exception) {
            // empty catch block
        }
    }

    private File findConfigFile(String name) {
        File[] files = this.configDir.listFiles();
        if (files == null) {
            return null;
        }
        for (File f : files) {
            if (!f.getName().equalsIgnoreCase(name + ".json")) continue;
            return f;
        }
        return null;
    }

    public void sendMessage(String msg) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage((Component)Component.literal((String)("§7[§bUT§7] " + msg)));
        }
    }
}

