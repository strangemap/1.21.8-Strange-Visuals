package ru.strange.client.mixin;

import me.x150.renderer.event.RenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.strange.client.event.EventManager;
import ru.strange.client.event.impl.EventScreen;
import ru.strange.client.utils.render.RenderUtil;

import java.awt.*;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("RETURN"))
    public void onRenderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Profiler prof = Profilers.get();
        prof.push("abHud");
        RenderEvents.HUD.invoker().rendered(context);
        prof.pop();

        MinecraftClient client = MinecraftClient.getInstance();
        EventManager.call(new EventScreen(client, context));
    }
}
