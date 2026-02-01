package ru.strange.client.module.api.setting.impl;

import ru.strange.client.module.api.setting.Setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ListSetting extends Setting {
    public List<String> list;
    public boolean opened;
    public String description;
    public List<String> selected = new ArrayList<>();
//    public Animation animation = new EaseInOutQuad(300, 1);
//    public Animation animation2 = new EaseInOutQuad(300, 1);

    public ListSetting(String name, String... settings) {
        this.name = name;
        this.list = Arrays.asList(settings);
        this.description = description;
    }

    public ListSetting hidden(Supplier<Boolean> hidden) {
        this.hidden = hidden;
        return this;
    }

    public String getFormattedList() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));

            if (i == 2 && list.size() > 3) {
                sb.append("...");
                break;
            }

            if (i < list.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public boolean isSelected(String element) {
        return selected.contains(element);
    }
}

