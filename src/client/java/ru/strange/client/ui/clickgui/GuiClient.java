package ru.strange.client.ui.clickgui;

import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.strange.client.Strange;
import ru.strange.client.module.Theme;
import ru.strange.client.module.ThemeManager;
import ru.strange.client.module.api.Category;
import ru.strange.client.module.api.Module;
import ru.strange.client.module.api.setting.Setting;
import ru.strange.client.module.api.setting.impl.BindSettings;
import ru.strange.client.module.api.setting.impl.HueSetting;
import ru.strange.client.module.api.setting.impl.SliderSetting;
import ru.strange.client.module.api.setting.impl.StringSetting;
import ru.strange.client.ui.clickgui.mouse.GuiMouseClicked;
import ru.strange.client.ui.clickgui.render.GuiRender;
import ru.strange.client.utils.Helper;

import static ru.strange.client.ui.clickgui.GuiScreen.scroll;

public class GuiClient extends Screen implements Helper {
    public GuiClient() {
        super(Text.literal("Gui"));
        GuiScreen.width = 225;
        GuiScreen.height = 217;
        GuiScreen.categories = Category.values();
        GuiScreen.modules = Strange.get.manager.getType(GuiScreen.selectedCategories);
        GuiScreen.themes = Theme.values();
    }

    @Override
    protected void init() {
        super.init();
        GuiScreen.width = 225;
        GuiScreen.height = 217;
        GuiScreen.x = this.width / 2f - GuiScreen.width / 2f;
        GuiScreen.y = this.height / 2f - GuiScreen.height / 2f;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        ThemeManager.update();
        GuiRender.renderGui(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Category c : Category.values()) {
            for (Module m : Strange.get.manager.getType(c)) {
                if (m.binding) {
                    m.bind = button;
                    m.binding = false;
                    m.displayName = m.name;
                    if (ru.strange.client.Strange.get != null && ru.strange.client.Strange.get.configManager != null) {
                        ru.strange.client.Strange.get.configManager.autoSave();
                    }
                    return true;
                }
                for (Setting setting : m.getSettingsForGUI()) {
                    if (setting instanceof BindSettings) {
                        BindSettings s = (BindSettings) setting;
                        if (s.hidden.get()) continue;
                        if (s.active) {
                            s.set(button);
                            s.active = false;
                            s.triggerAutoSave();
                        }
                    }
                }
            }
        }
        return GuiMouseClicked.mouseClickedGui(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        float modulesX = GuiScreen.x + 7;
        float modulesY = GuiScreen.y + 64;
        float modulesWidth = 211;
        float modulesHeight = GuiScreen.height - 64 - 7;

        if (GuiScreen.isHovered(mouseX, mouseY, modulesX, modulesY, modulesWidth, modulesHeight)) {
            GuiScreen.scroll.handleScroll(verticalAmount);
            return true;
        }

        return false;
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Category c : Category.values()) {
            for (Module m : Strange.get.manager.getType(c)) {
                if (m.binding) {
                    if (keyCode == 261) {
                        m.bind = -1;
                    } else {
                        m.bind = keyCode;
                    }
                    m.binding = false;
                    m.displayName = m.name;
                    if (ru.strange.client.Strange.get != null && ru.strange.client.Strange.get.configManager != null) {
                        ru.strange.client.Strange.get.configManager.autoSave();
                    }
                    return true;
                }
                for (Setting setting : m.getSettingsForGUI()) {
                    if (setting instanceof BindSettings) {
                        BindSettings s = (BindSettings) setting;
                        if (s.hidden.get()) continue;
                        if (s.active) {
                            s.set(keyCode);
                            if (keyCode == 261) {
                                s.set(-1);
                            }
                            s.active = false;
                            s.triggerAutoSave();
                        }
                    }
                    if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                        if (setting instanceof StringSetting) {
                            StringSetting s = (StringSetting) setting;
                            if (s.hidden.get()) continue;

                            if (s.active && s.input.length() > 0) {
                                s.input = s.input.substring(0, s.input.length() - 1);
                                s.triggerAutoSave();
                            }
                        }
                    }
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (Category c : Category.values()) {
            for (Module m : Strange.get.manager.getType(c)) {
                for (Setting setting : m.getSettingsForGUI()) {
                    if (setting instanceof StringSetting) {
                        StringSetting s = (StringSetting) setting;
                        if (s.hidden.get()) continue;

                        if (s.active) {
                            StringBuilder result = new StringBuilder();
                            for (int i = 0; i < Character.toString(codePoint).length(); i++) {
                                char c2 = Character.toString(codePoint).charAt(i);
                                if (c2 >= 32 && c2 != 127) {
                                    result.append(c2);
                                }
                            }
                            s.input += result.toString();
                            s.triggerAutoSave();
                        }

                    }
                }
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void close() {
        for (Category c : Category.values()) {
            for (Module m : Strange.get.manager.getType(c)) {
                for (Setting setting : m.getSettingsForGUI()) {
                    if (setting instanceof SliderSetting) {
                        SliderSetting s = (SliderSetting) setting;
                        s.sliding = false;
                    }
                    if (setting instanceof HueSetting) {
                        HueSetting s = (HueSetting) setting;
                        s.sliding = false;
                        s.colorSliding = false;
                    }
                }
            }
        }
        super.close();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Category c : Category.values()) {
            for (Module m : Strange.get.manager.getType(c)) {
                for (Setting setting : m.getSettingsForGUI()) {
                    if (setting instanceof SliderSetting) {
                        SliderSetting s = (SliderSetting) setting;
                        s.sliding = false;
                    }
                    if (setting instanceof HueSetting) {
                        HueSetting s = (HueSetting) setting;
                        s.sliding = false;
                        s.colorSliding = false;
                    }
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
