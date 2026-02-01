package ru.strange.client.module.api.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Config {
    private final ArrayList<Setting> settingList = new ArrayList();

    public final void addSettings(Setting... var1) {
        this.settingList.addAll(Arrays.asList(var1));
    }

    public final List<Setting> getSettingsForGUI() {
        return (List)this.settingList.stream().filter((var0) -> !(Boolean)var0.hidden.get()).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public final List<Setting> getSettings() {
        return this.settingList;
    }
}
