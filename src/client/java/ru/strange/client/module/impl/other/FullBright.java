package ru.strange.client.module.impl.other;

import net.minecraft.entity.effect.StatusEffects;
import ru.strange.client.module.api.Category;
import ru.strange.client.module.api.IModule;
import ru.strange.client.module.api.Module;

@IModule(
        name = "Гамма",
        description = " ",
        category = Category.Other,
        bind = -1
)
public class FullBright extends Module {
    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }
}
