package ru.strange.client.ui.clickgui.mouse;

import ru.strange.client.ui.clickgui.GuiScreen;
import ru.strange.client.utils.math.MathHelper;
import ru.strange.client.utils.math.ScaleHelper;
import ru.strange.client.utils.math.ScaledResolution;

public class GuiMouseClicked extends GuiScreen {
    public static boolean mouseClickedGui(double pMouseX, double pMouseY, int pButton) {
        int mouseX = (int) ScaleHelper.calc((float) pMouseX, (float) pMouseY)[0];
        int mouseY = (int) ScaleHelper.calc((float) pMouseX, (float) pMouseY)[1];
        ScaledResolution sr = new ScaledResolution(GuiScreen.mc);
        x = (int) MathHelper.clamp(GuiScreen.x, 0, ScaleHelper.calc(sr.getWidth()) - width);
        y = (int) MathHelper.clamp(GuiScreen.y, 0, ScaleHelper.calc(sr.getHeight()) - height);

        if (GuiMouseClickedCategory.clickedCategory(pMouseX, pMouseY)) {
            return true;
        }
        if (GuiMouseClickedModule.clickedModule(pMouseX, pMouseY, pButton)) {
            return true;
        }

        if (GuiMouseClickedTheme.clickedTheme(pMouseX, pMouseY)) {
            return true;
        }

        return false;
    }
}
