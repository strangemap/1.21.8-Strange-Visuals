package ru.strange.client.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.strange.client.Strange;
import ru.strange.client.event.EventManager;
import ru.strange.client.event.impl.EventKeyInput;
import ru.strange.client.module.api.Module;
import ru.strange.client.ui.clickgui.GuiClient;

import java.util.ArrayList;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {

        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_RELEASE) return;

        MinecraftClient mc = MinecraftClient.getInstance();

        EventKeyInput event = new EventKeyInput(window, key, scancode, action, modifiers);
        EventManager.call(event);

        if (event.isCancelled()) {
            ci.cancel();
            return;
        }

        if (mc.currentScreen != null) return;

        if (action == GLFW.GLFW_PRESS && key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            mc.setScreen(new GuiClient());
            if (mc.mouse != null) mc.mouse.unlockCursor();
            ci.cancel();
            return;
        }

        if (mc.player != null && action == GLFW.GLFW_PRESS) {
            for (Module m : Strange.get.manager.getBind(key)) {
                m.toggle();
            }
        }
    }
}
