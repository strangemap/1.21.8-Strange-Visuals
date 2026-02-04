package ru.strange.client.module.impl.interfaces;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.strange.client.Strange;
import ru.strange.client.event.EventInit;
import ru.strange.client.event.impl.EventScreen;
import ru.strange.client.module.api.Category;
import ru.strange.client.module.api.IModule;
import ru.strange.client.module.api.Module;
import ru.strange.client.module.api.setting.impl.BooleanSetting;
import ru.strange.client.module.api.setting.impl.MultiBooleanSetting;
import ru.strange.client.utils.render.FontDraw;
import ru.strange.client.utils.render.RenderUtil;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@IModule(
        name = "Водяной знак",
        description = "Кастомизация анимации руки",
        category = Category.Interface,
        bind = -1
)
public class WaterMark extends Module {

    public MultiBooleanSetting settings = new MultiBooleanSetting(
            "Элементы",
            new BooleanSetting("Ник", true),
            new BooleanSetting("ФПС", true),
            new BooleanSetting("Пинг", false),
            new BooleanSetting("Время", false),
            new BooleanSetting("Сервер", false)
    );

    private float animatedWidth = 71f;

    private final Map<String, Float> segProgress = new LinkedHashMap<>();

    private static final float BASE_WIDTH = 71f;
    private static final float HEIGHT = 16f;

    private static final float WIDTH_SPEED = 0.18f;
    private static final float SEG_SPEED  = 0.22f;

    public WaterMark() {
        addSettings(settings);
    }

    @EventInit
    public void onScreen(EventScreen e) {
        if (mc.player == null) return;

        float x = 10;
        float y = 10;

        float dt = 1.0f;

        String nickText = mc.player.getName().getString();
        String fpsText = mc.getCurrentFps() + " FPS";

        int pingValue = -1;
        if (mc.getNetworkHandler() != null
                && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
            pingValue = mc.getNetworkHandler()
                    .getPlayerListEntry(mc.player.getUuid())
                    .getLatency();
        }
        String pingText = pingValue >= 0 ? pingValue + " ms" : "N/A";

        String timeText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String serverText = mc.getCurrentServerEntry() != null
                ? mc.getCurrentServerEntry().name
                : "Singleplayer";

        Map<String, String> segments = new LinkedHashMap<>();
        segments.put("Ник", nickText);
        segments.put("ФПС", fpsText);
        segments.put("Пинг", pingText);
        segments.put("Время", timeText);
        segments.put("Сервер", serverText);

        for (Map.Entry<String, String> seg : segments.entrySet()) {
            String key = seg.getKey();
            boolean enabled = settings.get(key);

            float p = segProgress.getOrDefault(key, 0f);
            float target = enabled ? 1f : 0f;

            p = approach(p, target, SEG_SPEED * dt);
            segProgress.put(key, p);
        }

        float targetWidth = BASE_WIDTH;

        for (Map.Entry<String, String> seg : segments.entrySet()) {
            String key = seg.getKey();
            String text = seg.getValue();

            float p = segProgress.getOrDefault(key, 0f);
            if (p <= 0.001f) continue;

            float w = FontDraw.getWidth(FontDraw.FontType.MEDIUM, text, 7);

            float pad;
            if (key.equals("Ник")) pad = 6;
            else if (key.equals("ФПС")) pad = 6;
            else pad = 6;

            targetWidth += (w + pad) * p;
        }

        animatedWidth = lerp(animatedWidth, targetWidth, WIDTH_SPEED * dt);

        RenderUtil.drawClientRect(e.drawContext(), x, y, animatedWidth - 2, HEIGHT);

        RenderUtil.Image.draw(
                e.drawContext(),
                Identifier.of(Strange.rootRes, "/icons/gui/logo.png"),
                x + 4.09f, y + 4,
                8.17f, 8,
                RenderUtil.ColorUtil.replAlpha(RenderUtil.ColorUtil.getTextColor(1, 1), 178)
        );

        FontDraw.drawText(
                FontDraw.FontType.MEDIUM,
                e.drawContext(),
                "Strange Visuals",
                x + 15,
                y + 10.5f,
                7,
                RenderUtil.ColorUtil.replAlpha(RenderUtil.ColorUtil.getTextColor(1, 1), 178)
        );

        float cursorX = x + BASE_WIDTH;

        for (Map.Entry<String, String> seg : segments.entrySet()) {
            String key = seg.getKey();
            String text = seg.getValue();

            float p = segProgress.getOrDefault(key, 0f);
            if (p <= 0.01f) continue;

            float textW = FontDraw.getWidth(FontDraw.FontType.MEDIUM, text, 7);

            float pad;
            if (key.equals("Ник")) pad = 6;
            else if (key.equals("ФПС")) pad = 6;
            else pad = 6;

            cursorX = drawSegmentAnimated(e, cursorX, y, text, p, textW, pad);
        }
    }


    private float drawSegmentAnimated(EventScreen e, float cursorX, float y, String text,
                                      float p, float textW, float pad) {
        int baseAlphaText = 178;
        int baseAlphaSep  = 150;

        int aText = (int) (baseAlphaText * p);
        int aSep  = (int) (baseAlphaSep * p);

        float slide = (1f - p) * 6f;
        float drawX = cursorX - slide;

        RenderUtil.Round.draw(
                e.drawContext(),
                drawX - 3,
                y + 4,
                1,
                8,
                0.5f,
                RenderUtil.ColorUtil.replAlpha(RenderUtil.ColorUtil.getTextColor(1, 1), aSep)
        );

        FontDraw.drawText(
                FontDraw.FontType.MEDIUM,
                e.drawContext(),
                text,
                drawX,
                y + 10.5f,
                7,
                RenderUtil.ColorUtil.replAlpha(RenderUtil.ColorUtil.getTextColor(1, 1), aText)
        );

        return cursorX + (textW + pad) * p;
    }

    private static float lerp(float from, float to, float speed) {
        speed = MathHelper.clamp(speed, 0f, 1f);
        return from + (to - from) * speed;
    }

    private static float approach(float value, float target, float speed) {
        if (value < target) return Math.min(value + speed, target);
        return Math.max(value - speed, target);
    }

}
