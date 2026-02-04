package ru.strange.client.manager.cfg;

import com.google.gson.JsonObject;
import ru.strange.client.Strange;
import ru.strange.client.module.Theme;
import ru.strange.client.module.ThemeManager;
import ru.strange.client.module.api.Module;
import ru.strange.client.ui.clickgui.GuiScreen;

import java.io.File;

public final class Config implements ConfigUpdater {

    private final String name;
    private final File file;

    public Config(String name) {
        this.name = name;
        this.file = new File(ConfigManager.configDirectory, name + ".json");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
            }
        }
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    @Override
    public JsonObject save() {
        JsonObject jsonObject = new JsonObject();
        JsonObject modulesObject = new JsonObject();

        for (Module module : Strange.get.manager.module) {
            modulesObject.add(module.name, module.save());
        }

        jsonObject.add("Features", modulesObject);
        
        jsonObject.addProperty("Theme", ThemeManager.getTheme().name());
        
        return jsonObject;
    }

    @Override
    public void load(JsonObject object) {
        System.out.println("[Config] Loading config: " + name);
        if (object.has("Features")) {
            JsonObject modulesObject = object.getAsJsonObject("Features");
            int enabledCount = 0;
            for (Module module : Strange.get.manager.module) {
                if (module.enable) {
                    module.toggle();
                }
                if (modulesObject.has(module.name)) {
                    module.load(modulesObject.getAsJsonObject(module.name));
                    if (module.enable) {
                        enabledCount++;
                        System.out.println("[Config] Module enabled: " + module.name);
                    }
                }
            }
            System.out.println("[Config] Total modules enabled: " + enabledCount);
        }
        
        if (object.has("Theme")) {
            try {
                String themeName = object.get("Theme").getAsString();
                Theme theme = Theme.valueOf(themeName);
                ThemeManager.setTheme(theme);
                ThemeManager.finishAnimation();
                GuiScreen.selectedTheme = theme;
                GuiScreen.preSelectedTheme = theme;
                System.out.println("[Config] Theme loaded: " + theme.getName());
            } catch (Exception e) {
                System.out.println("[Config] Failed to load theme: " + e.getMessage());
            }
        }
    }
}
