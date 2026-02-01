package ru.strange.client;

import net.fabricmc.api.ClientModInitializer;
import ru.strange.client.manager.cfg.ConfigManager;
import ru.strange.client.event.EventManager;
import ru.strange.client.manager.friend.FriendManager;
import ru.strange.client.module.api.Manager;
import ru.strange.client.renderengine.font.FontManager;
import ru.strange.client.renderengine.renderers.pipeline.PipelineInitializer;
import ru.strange.client.rpc.RPC;

import java.awt.*;
import java.io.File;


public class Strange implements ClientModInitializer {
    public static Strange get;

    public static String name = "Strange Visuals";
    public static final File preRoot = new File("C:\\", "");
    public static final File root = new File(preRoot, name);
    public static String rootRes = "strange";

    public Manager manager;
    public ConfigManager configManager;
    public FriendManager friendManager;
    private final RPC rpc = new RPC();

    @Override
    public void onInitializeClient() {
        System.out.println("[Strange Visuals] onInitializeClient() START");
        get = this;
        manager = new Manager();
        configManager = new ConfigManager();
        friendManager = new FriendManager();

        PipelineInitializer.init();

        FontManager fontManager = FontManager.getInstance();

        try {
            fontManager.loadFontFromResources("medium", "strange:fonts/medium.ttf");
            System.out.println("Medium font loaded successfully");
        } catch (Exception e) {
            System.err.println("Failed to load medium font: " + e.getMessage());
            e.printStackTrace();
        }

        rpc.startRpc();

        if (configManager != null) {
            configManager.load();
            if (configManager.findConfig("default") != null) {
                configManager.loadConfig("default");
            }
        }

        EventManager.register(this);
    }
}
