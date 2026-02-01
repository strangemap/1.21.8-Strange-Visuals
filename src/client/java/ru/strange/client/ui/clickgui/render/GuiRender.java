package ru.strange.client.ui.clickgui.render;

import net.minecraft.client.gui.DrawContext;
import ru.strange.client.ui.clickgui.GuiClient;
import ru.strange.client.ui.clickgui.GuiScreen;
import ru.strange.client.utils.math.ScaleHelper;
import ru.strange.client.utils.math.ScaledResolution;

public class GuiRender extends GuiScreen {
    public static void renderGui(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int mouseX1 = (int) ScaleHelper.calc(mouseX, mouseY)[0];
        int mouseY1 = (int) ScaleHelper.calc(mouseX, mouseY)[1];
        ScaledResolution sr = new ScaledResolution(mc);

        x = ScaleHelper.calc(sr.getWidth()) / 2 - width / 2;
        y = ScaleHelper.calc(sr.getHeight()) / 2 - height / 2;

        GuiRenderBackGround.renderBackGround(context);
        GuiRenderCategory.renderCategory(context);
        GuiRenderModule.renderModule(context, mouseX, mouseY);
        GuiRenderTheme.renderTheme(context);
    }
}
