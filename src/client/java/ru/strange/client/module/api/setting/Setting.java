package ru.strange.client.module.api.setting;

import ru.strange.client.Strange;

import java.util.function.Supplier;

public class Setting extends Config {
    public String name;
    public Supplier<Boolean> hidden = () -> false;
    
    /**
     * Вызывает автосохранение конфига при изменении настройки
     */
    public void triggerAutoSave() {
        if (Strange.get != null && Strange.get.configManager != null) {
            Strange.get.configManager.autoSave();
        }
    }
}
