package ru.strange.client.utils.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import ru.strange.client.utils.Helper;

public class RenderPlayer implements Helper {
    public static void onRenderPlayer(DrawContext context, Screen screen, int mouseX, int mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        int x2 = screen.width - 25;
        int x1 = x2 - 450;
        int y1 = 35;
        int y2 = y1 + 320;

        int size = 55;

        float scale = 0.0625F;

        InventoryScreen.drawEntity(
                context,
                x1, y1, x2, y2,
                size,
                scale,
                (float) mouseX, (float) mouseY,
                player
        );
    }
}
