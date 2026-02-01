package ru.strange.client.ui.clickgui.mouse;

import net.minecraft.util.Identifier;
import ru.strange.client.module.Theme;
import ru.strange.client.module.ThemeManager;
import ru.strange.client.module.api.Category;
import ru.strange.client.ui.clickgui.GuiScreen;
import ru.strange.client.utils.render.FontDraw;
import ru.strange.client.utils.render.RenderUtil;

import java.awt.*;

public class GuiMouseClickedTheme extends GuiScreen {
    public static boolean clickedTheme(double mouseX, double mouseY) {
        if (selectedCategories != Category.Theme) return false;

        float startXLeft = x + 7;
        float startXRight = x + 7 + 110;
        float startY = y + 64;

        float yDown = 0;

        for (int index = 0; index < themes.length; index++) {
            Theme theme = themes[index];
            boolean leftColumn = index % 2 == 0;
            float drawX = leftColumn ? startXLeft : startXRight;
            float drawY = startY + yDown;

            if (isHovered(mouseX, mouseY, drawX, drawY, 102, 26)) {
                ThemeManager.setTheme(theme);
                return true;
            }

            if (!leftColumn) {
                yDown += 26 + 4;
            }
        }
        return false;
    }
}
