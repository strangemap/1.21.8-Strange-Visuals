package ru.strange.client.module.api;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import ru.strange.client.event.EventManager;
import ru.strange.client.module.api.setting.Config;
import ru.strange.client.module.api.setting.Setting;
import ru.strange.client.module.api.setting.impl.*;

import java.util.ArrayList;

public class Module extends Config {
    public IModule module = this.getClass().getAnnotation(IModule.class);
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public String name;
    public int bind;
    public boolean enable;
    public boolean open = false;
    public Category category;
    public String displayName;
    public String description;
    public boolean binding;
    public boolean isRender = true;

    public Module() {
        name = module.name();
        category = module.category();
        if (module.bind() == 0) {
            bind = -1;
        } else {
            bind = module.bind();
        }
        enable = false;
        description = module.description();
        displayName = name;
    }


    public void onEnable() {
        System.out.println("[Module] Enabling: " + name);
        try {
            EventManager.register(this);
        } catch (Exception e) {
            System.err.println("[Module] Failed to enable " + name + ": " + e.getMessage());
            e.printStackTrace();
            enable = false;
            return;
        }
    }

    public void onDisable() {
//        DragManager.saveDragData();
        EventManager.unregister(this);
    }

    public String getDisplayName(){
        return displayName;
    }

    public void toggle() {
        enable = !enable;
        if (enable) {
            onEnable();
        } else {
            onDisable();
        }
        // Автосохранение конфига при изменении состояния модуля
        if (ru.strange.client.Strange.get != null && ru.strange.client.Strange.get.configManager != null) {
            ru.strange.client.Strange.get.configManager.autoSave();
        }
    }

    public JsonObject save() {
        JsonObject object = new JsonObject();
        if (enable)
            object.addProperty("enable", enable);
        if (bind > 0)
            object.addProperty("keyIndex", this.bind);
        JsonObject propertiesObject = new JsonObject();
        for (Setting set : getSettings()) {
            if (set instanceof BooleanSetting) {
                propertiesObject.addProperty(set.name, ((BooleanSetting) set).get());
            } else if (set instanceof ModeSetting) {
                propertiesObject.addProperty(set.name, ((ModeSetting) set).currentMode);
            } else if (set instanceof SliderSetting) {
                propertiesObject.addProperty(set.name, ((SliderSetting) set).current);
            } else if (set instanceof BindSettings) {
                propertiesObject.addProperty(set.name, ((BindSettings) set).key);
            } else if (set instanceof StringSetting) {
                propertiesObject.addProperty(set.name, ((StringSetting) set).input);
            } else if (set instanceof HueSetting) {
                HueSetting hueSetting = (HueSetting) set;
                com.google.gson.JsonObject hueObject = new com.google.gson.JsonObject();
                hueObject.addProperty("current", hueSetting.current);
                hueObject.addProperty("saturation", hueSetting.saturation);
                hueObject.addProperty("brightness", hueSetting.brightness);
                propertiesObject.add(set.name, hueObject);
            } else if (set instanceof MultiBooleanSetting) {
                com.google.gson.JsonObject multiBoolObject = new com.google.gson.JsonObject();
                for (BooleanSetting boolSetting : ((MultiBooleanSetting) set).settings) {
                    multiBoolObject.addProperty(boolSetting.name, boolSetting.get());
                }
                propertiesObject.add(set.name, multiBoolObject);
            }
        }
        object.add("Settings", propertiesObject);
        return object;
    }


    public void load(JsonObject object) {
        if (object != null) {
            if (object.has("enable")) {
                this.setState(object.get("enable").getAsBoolean());
            }

            if (object.has("keyIndex")) {
                bind = (object.get("keyIndex").getAsInt());
            }

            for (Setting set : getSettings()) {
                JsonObject propertiesObject = object.getAsJsonObject("Settings");
                if (set == null)
                    continue;
                if (propertiesObject == null)
                    continue;
                if (!propertiesObject.has(set.name))
                    continue;
                if (set instanceof BooleanSetting) {
                    ((BooleanSetting) set).set(propertiesObject.get(set.name).getAsBoolean());
                } else if (set instanceof ModeSetting) {
                    ((ModeSetting) set).currentMode = propertiesObject.get(set.name).getAsString();
                } else if (set instanceof SliderSetting) {
                    ((SliderSetting) set).current = (propertiesObject.get(set.name).getAsFloat());
                } else if (set instanceof BindSettings) {
                    ((BindSettings) set).key = (propertiesObject.get(set.name).getAsInt());
                } else if (set instanceof StringSetting) {
                    ((StringSetting) set).input = (propertiesObject.get(set.name).getAsString());
                } else if (set instanceof HueSetting) {
                    HueSetting hueSetting = (HueSetting) set;
                    if (propertiesObject.get(set.name).isJsonObject()) {
                        com.google.gson.JsonObject hueObject = propertiesObject.getAsJsonObject(set.name);
                        if (hueObject.has("current")) {
                            hueSetting.current = hueObject.get("current").getAsFloat();
                        }
                        if (hueObject.has("saturation")) {
                            hueSetting.saturation = hueObject.get("saturation").getAsFloat();
                        }
                        if (hueObject.has("brightness")) {
                            hueSetting.brightness = hueObject.get("brightness").getAsFloat();
                        }
                    } else {
                        // Обратная совместимость: если сохранено как просто число
                        hueSetting.current = propertiesObject.get(set.name).getAsFloat();
                    }
                } else if (set instanceof MultiBooleanSetting) {
                    if (propertiesObject.get(set.name).isJsonObject()) {
                        com.google.gson.JsonObject multiBoolObject = propertiesObject.getAsJsonObject(set.name);
                        for (BooleanSetting boolSetting : ((MultiBooleanSetting) set).settings) {
                            if (multiBoolObject.has(boolSetting.name)) {
                                boolSetting.set(multiBoolObject.get(boolSetting.name).getAsBoolean());
                            }
                        }
                    }
                } else if (set instanceof ListSetting) {
                    String[] split = propertiesObject.get(set.name).getAsString().split(",");
                    ((ListSetting) set).selected = new ArrayList<>();

                    for (String s : split) {
                        if (((ListSetting) set).list.contains(s)) {
                            ((ListSetting) set).selected.add(s);
                        }
                    }
                }
            }
        }

    }

    public int getBind() {
        return bind;
    }

    public void setState(boolean enable) {
        this.enable = enable;
        if (enable) {

            onEnable();
        } else {

            onDisable();
        }
        // Автосохранение конфига при изменении состояния модуля
        if (ru.strange.client.Strange.get != null && ru.strange.client.Strange.get.configManager != null) {
            ru.strange.client.Strange.get.configManager.autoSave();
        }
    }
    public void setEnable(boolean enable) {
        this.enable = !enable;
        if(enable){
            onEnable();
        } else {
            onDisable();
        }
    }
}