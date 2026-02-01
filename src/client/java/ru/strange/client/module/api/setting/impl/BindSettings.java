package ru.strange.client.module.api.setting.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import ru.strange.client.module.api.setting.Setting;

import java.util.function.Supplier;

public class BindSettings extends Setting {

    public int key;
    public String description;
//    public Animation animation = new EaseInOutQuad(300, 1);
//    public Animation animation2 = new EaseInOutQuad(300, 1);
    public boolean active;

    public BindSettings(String name, int key) {
        this.name = name;
        this.key = key;
        this.description = description;
    }

    public int get() {
        return key;
    }

    public void set(int key) {
        this.key = key;
        triggerAutoSave();
    }

    public BindSettings hidden(Supplier<Boolean> hidden) {
        this.hidden = hidden;
        return this;
    }

    public boolean isKeyDown(int keyCode) {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), keyCode);
    }

}
