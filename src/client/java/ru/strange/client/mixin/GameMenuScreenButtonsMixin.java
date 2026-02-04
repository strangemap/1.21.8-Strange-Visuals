package ru.strange.client.mixin;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.strange.client.utils.other.SkinUtil;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenButtonsMixin extends Screen {

    protected GameMenuScreenButtonsMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void strange$addButtons(CallbackInfo ci) {

        int skinX2 = this.width - 25;
        int skinX1 = skinX2 - 450;
        int skinY1 = 35;
        int skinY2 = skinY1 + 320;

        int buttonWidth = 100;
        int buttonHeight = 20;
        int spacing = 8;

        int centerX = (skinX1 + skinX2) / 2;
        int y = skinY2 - 100;

        int leftX  = centerX - buttonWidth - (spacing / 2);
        int rightX = centerX + (spacing / 2);

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Поставить скин"),
                        btn -> strange$onApplySkinClicked()
                ).dimensions(leftX, y, buttonWidth, buttonHeight).build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Сбросить скин"),
                        btn -> strange$onResetSkinClicked()
                ).dimensions(rightX, y, buttonWidth, buttonHeight).build()
        );
    }

    @Unique
    private void strange$onApplySkinClicked() {
        // ✔ открывает диалог и применяет скин
        SkinUtil.uiPickAndApplySkin();
    }

    @Unique
    private void strange$onResetSkinClicked() {
        // ✔ удаляет файл и возвращает ванильный скин
        SkinUtil.uiResetSkin();
    }
}
