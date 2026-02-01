package ru.strange.client.mixin;

import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.strange.client.event.EventManager;
import ru.strange.client.event.impl.EventMouseInput;
import ru.strange.client.event.impl.EventMouseScroll;

@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        EventMouseInput event = new EventMouseInput(window, button, action, modifiers);
        EventManager.call(event);

        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void onMouseScroll(long window, double horizontalAmount, double verticalAmount, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double mouseX = mc.mouse.getX();
        double mouseY = mc.mouse.getY();
        EventManager.call(new EventMouseScroll(mouseX, mouseY, horizontalAmount, verticalAmount));
    }
}
