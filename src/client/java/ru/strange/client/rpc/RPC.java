package ru.strange.client.rpc;

import ru.strange.client.utils.Helper;

public class RPC implements Helper {

    public static DiscordRichPresence presence = new DiscordRichPresence();
    public static boolean started;
    private static Thread thread;

    public void startRpc() {
        // Проверяем, доступна ли библиотека Discord RPC
        if (!DiscordRPC.Loader.isAvailable()) {
            return;
        }

        DiscordRPC rpc = DiscordRPC.Loader.getInstance();
        if (!started) {
            started = true;
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            rpc.Discord_Initialize("1463261568648085715", handlers, true, "");
            presence.startTimestamp = (System.currentTimeMillis() / 1000L);
            presence.largeImageText = "Strange Visuals - 1.21.8";
            rpc.Discord_UpdatePresence(presence);
            thread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    rpc.Discord_RunCallbacks();
                    presence.details = "Version: v1";
                    presence.state = "Release wow.";

                    presence.button_label_1 = "Telegram";
                    presence.button_url_1 = "https://t.me/strangevisuals";

                    presence.button_label_2 = "Discord";
                    presence.button_url_2 = "https://discord.gg/TCMk6afSKc";

                    presence.largeImageKey = "https://i.ibb.co/LhhNp5Bv/photo-2026-01-20-00.jpg";

                    rpc.Discord_UpdatePresence(presence);
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException ignored) {
                    }
                }
            }, "TH-RPC-Handler");
            thread.start();

        }
    }
}
