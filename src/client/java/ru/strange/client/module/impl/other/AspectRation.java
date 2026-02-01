package ru.strange.client.module.impl.other;

import ru.strange.client.Strange;
import ru.strange.client.module.api.Category;
import ru.strange.client.module.api.IModule;
import ru.strange.client.module.api.Module;
import ru.strange.client.module.api.setting.impl.ModeSetting;
import ru.strange.client.module.api.setting.impl.SliderSetting;
import ru.strange.client.utils.math.ScaledResolution;

@IModule(
        name = "Аспект ратион",
        description = " ",
        category = Category.Other,
        bind = -1
)
public class AspectRation extends Module {
    public static final ModeSetting aspect = new ModeSetting("Соотношение экрана", "16:9", "16:9", "4:3", "1:1", "16:10", "21:9", "32:9", "5:4", "2:1", "Кастомное");
    public static final SliderSetting customAspect = new SliderSetting("Кастомое значние", 2, 1, 3, 0.1F, false)
            .hidden(() -> !aspect.is("Кастомное"));

    public AspectRation() {
        addSettings(aspect, customAspect);
    }
    public static float getAspectRation() {
        ScaledResolution sr = new ScaledResolution(mc);
        if (!Strange.get.manager.getModule(AspectRation.class).enable) {
            return 0F;
        }

        float aspect1 = (float) sr.getWidth() / sr.getHeight();

        float newAspect = switch (aspect.get()) {
            case "16:9" -> 16F / 9f;
            case "4:3" -> 4F / 3F;
            case "1:1" -> 1F;
            case "16:10" -> 16F / 10F;
            case "21:9" -> 21F / 9F;
            case "32:9" -> 32F / 9F;
            case "5:4" -> 5F / 4F;
            case "2:1" -> 2F;
            default -> customAspect.get();
        };

        return newAspect - aspect1;
    }
}