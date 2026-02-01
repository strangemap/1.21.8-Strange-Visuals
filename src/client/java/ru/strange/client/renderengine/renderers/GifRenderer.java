package ru.strange.client.renderengine.renderers;

import com.mojang.blaze3d.textures.GpuTextureView;
import me.x150.renderer.mixin.DrawContextAccessor;
import me.x150.renderer.render.SimpleGuiRenderState;
import me.x150.renderer.util.DirectVertexConsumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2fStack;
import ru.strange.client.renderengine.builders.states.QuadColorState;
import ru.strange.client.renderengine.builders.states.QuadRadiusState;
import ru.strange.client.renderengine.builders.states.SizeState;
import ru.strange.client.renderengine.renderers.pipeline.PipelineRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record GifRenderer(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float smoothness,
        List<GpuTextureView> frames,
        List<Integer> delays,
        String gifId
) implements Renderer {

    private static final Map<String, Long> startTimeCache = new HashMap<>();

    /**
     * Сбрасывает время начала анимации для указанного GIF
     */
    public static void resetAnimation(String gifId) {
        startTimeCache.remove(gifId);
    }

    /**
     * Очищает весь кэш времени начала анимаций
     */
    public static void clearAnimationCache() {
        startTimeCache.clear();
    }

    /**
     * Получает время начала анимации для указанного GIF (без создания нового)
     */
    public static Long getStartTimeForGif(String gifId) {
        return startTimeCache.get(gifId);
    }

    private static ScreenRect createBounds(DrawContext c, float x, float y, float w, float h) {
        Matrix3x2fStack mat = c.getMatrices();
        DrawContext.ScissorStack ss = ((DrawContextAccessor) c).getScissorStack();
        ScreenRect scissor = ss.peekLast();

        ScreenRect screenRect = new ScreenRect(
                (int) Math.floor(x),
                (int) Math.floor(y),
                (int) Math.ceil(w),
                (int) Math.ceil(h)
        ).transformEachVertex(mat);

        return scissor != null ? scissor.intersection(screenRect) : screenRect;
    }

    private long getStartTime() {
        // Получаем или создаем startTime для этого GIF
        // Используем synchronized для thread-safety
        synchronized (startTimeCache) {
            return startTimeCache.computeIfAbsent(gifId, k -> System.currentTimeMillis());
        }
    }

    private int getCurrentFrame() {
        if (frames.isEmpty()) return 0;
        if (frames.size() == 1) return 0;

        long currentTime = System.currentTimeMillis();
        long startTime = getStartTime();
        long elapsed = currentTime - startTime;

        // Вычисляем общую длительность одного цикла
        long totalDelay = 0;
        for (int delay : delays) {
            totalDelay += delay;
        }
        
        if (totalDelay <= 0) {
            // Если все задержки 0, используем дефолтную задержку для всех кадров
            int defaultDelay = 100;
            totalDelay = (long) frames.size() * defaultDelay;
        }

        // Находим текущий кадр на основе прошедшего времени
        // Используем модуль для плавного циклического воспроизведения
        long cycleTime = elapsed % totalDelay;
        
        long accumulatedDelay = 0;
        for (int i = 0; i < frames.size(); i++) {
            int frameDelay = delays.get(i);
            accumulatedDelay += frameDelay;
            if (cycleTime < accumulatedDelay) {
                return i;
            }
        }

        // Если дошли до конца (не должно происходить при правильном модуле), возвращаем последний кадр
        return frames.size() - 1;
    }

    @Override
    public void render(double x, double y, DrawContext ctx) {
        if (PipelineRegistry.TEXTURE_PIPELINE == null) return;
        if (frames.isEmpty()) return;

        int currentFrame = getCurrentFrame();
        GpuTextureView texture = frames.get(currentFrame);

        Matrix3x2fStack guiMatrices = ctx.getMatrices();
        TextureSetup textureSetup = TextureSetup.of(texture);

        SimpleGuiRenderState state = new SimpleGuiRenderState(
                PipelineRegistry.TEXTURE_PIPELINE,
                textureSetup,
                ctx,
                createBounds(ctx, (float) x, (float) y, size.width(), size.height()),
                buffer -> {
                    DirectVertexConsumer dvc = new DirectVertexConsumer((BufferBuilder) buffer, false);

                    // Вершина 1 (top-left)
                    dvc.vertex(guiMatrices, (float) x, (float) y, (float) 0)
                            .texture(0, 0)
                            .color(this.color.color1())
                            .texture(size.width(), size.height())
                            .texture(radius.radius1(), radius.radius2())
                            .texture(radius.radius3(), radius.radius4())
                            .texture(smoothness, 0);

                    // Вершина 2 (bottom-left)
                    dvc.vertex(guiMatrices, (float) x, (float) (y + size.height()), (float) 0)
                            .texture(0, 1)
                            .color(this.color.color2())
                            .texture(size.width(), size.height())
                            .texture(radius.radius1(), radius.radius2())
                            .texture(radius.radius3(), radius.radius4())
                            .texture(smoothness, 0);

                    // Вершина 3 (bottom-right)
                    dvc.vertex(guiMatrices, (float) (x + size.width()), (float) (y + size.height()), (float) 0)
                            .texture(1, 1)
                            .color(this.color.color3())
                            .texture(size.width(), size.height())
                            .texture(radius.radius1(), radius.radius2())
                            .texture(radius.radius3(), radius.radius4())
                            .texture(smoothness, 0);

                    // Вершина 4 (top-right)
                    dvc.vertex(guiMatrices, (float) (x + size.width()), (float) y, (float) 0)
                            .texture(1, 0)
                            .color(this.color.color4())
                            .texture(size.width(), size.height())
                            .texture(radius.radius1(), radius.radius2())
                            .texture(radius.radius3(), radius.radius4())
                            .texture(smoothness, 0);
                }
        );

        ((DrawContextAccessor) ctx).getState().addSimpleElement(state);
    }
}
