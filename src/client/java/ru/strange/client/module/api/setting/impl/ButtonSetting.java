package ru.strange.client.module.api.setting.impl;

import ru.strange.client.module.api.setting.Setting;

import java.util.function.Supplier;

public class ButtonSetting extends Setting {

    public int mode;
    public String description;
//    public Animation animation = new EaseInOutQuad(300, 1);
//    public Animation animation2 = new EaseInOutQuad(300, 1);

    public ButtonSetting(String name, int mode) {
        this.name = name;
        this.mode = mode;
    }

    public int get() {
        return mode;
    }

    public void set(int mode) {
        this.mode = mode;
    }

    public ButtonSetting hidden(Supplier<Boolean> hidden) {
        this.hidden = hidden;
        return this;
    }
}
