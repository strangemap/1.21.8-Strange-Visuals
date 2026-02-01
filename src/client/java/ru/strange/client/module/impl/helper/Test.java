package ru.strange.client.module.impl.helper;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import ru.strange.client.event.EventInit;
import ru.strange.client.event.impl.EventScreen;
import ru.strange.client.module.api.Category;
import ru.strange.client.module.api.IModule;
import ru.strange.client.module.api.Module;
import ru.strange.client.module.api.setting.impl.*;
import ru.strange.client.renderengine.builders.impl.RectangleBuilder;
import ru.strange.client.renderengine.builders.states.QuadColorState;
import ru.strange.client.renderengine.builders.states.QuadRadiusState;
import ru.strange.client.renderengine.builders.states.SizeState;
import ru.strange.client.utils.render.FontDraw;
import ru.strange.client.utils.render.RenderUtil;

import java.io.File;
import java.io.IOException;

@IModule(
        name = "Test",
        description = "Демонстрация Liquid Glass эффекта",
        category = Category.Other,
        bind = GLFW.GLFW_KEY_Y
)
public class Test extends Module {
    public static BooleanSetting a1 = new BooleanSetting("Test1",true);
    public static BooleanSetting a2 = new BooleanSetting("Test2",false);
    public static SliderSetting a4 = new SliderSetting("Test3", 4.5f, 1, 6, 0.5f, false);
    public static SliderSetting a6 = new SliderSetting("Test4", 4.5f, 1, 6, 0.5f, true);
    public static ModeSetting a5 = new ModeSetting("Test5", "WhiteWindwos", "WhiteWindwos", "Strange");
    public static MultiBooleanSetting a50 = new MultiBooleanSetting("Test7",new BooleanSetting("Тест", true), new BooleanSetting("Test", false), new BooleanSetting("Sosal?", true), new BooleanSetting("123", false));
    public static StringSetting a10 = new StringSetting("Test10", "");
    public static BindSettings a11 = new BindSettings("Test11", -1);
    public static HueSetting a12 = new HueSetting("Test12", 15, 1.0f, 1.0f);

    public Test() {
        addSettings(a12, a11, a10, a5, a4, a6, a1, a50, a2);
    }

    @EventInit
    public void onScreen(EventScreen e) throws IOException {
//        RenderUtil.Shadow.draw(e.drawContext(), 100,100, 100, 100, 5,25, -1);
//        RenderUtil.Round.draw(e.drawContext(), 100 + 2, 100 + 2, 100, 100, 5, -1);
        new RectangleBuilder()
                .size(new SizeState(199, 199))
                .radius(new QuadRadiusState(15, 0, 0, 15))
                .color(new QuadColorState(-1))
                .build()
                .render(199, 199, e.drawContext());
    }
}
