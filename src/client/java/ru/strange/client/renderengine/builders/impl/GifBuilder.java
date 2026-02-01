package ru.strange.client.renderengine.builders.impl;

import com.mojang.blaze3d.textures.GpuTextureView;
import ru.strange.client.renderengine.builders.AbstractBuilder;
import ru.strange.client.renderengine.builders.states.QuadColorState;
import ru.strange.client.renderengine.builders.states.QuadRadiusState;
import ru.strange.client.renderengine.builders.states.SizeState;
import ru.strange.client.renderengine.renderers.GifRenderer;

import java.util.ArrayList;
import java.util.List;

public final class GifBuilder extends AbstractBuilder<GifRenderer> {

    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;
    private float smoothness;
    private List<GpuTextureView> frames;
    private List<Integer> delays;
    private long startTime;
    private String gifId;

    public GifBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public GifBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public GifBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    public GifBuilder smoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    public GifBuilder addFrame(GpuTextureView texture, int delayMs) {
        if (this.frames == null) {
            this.frames = new ArrayList<>();
            this.delays = new ArrayList<>();
        }
        this.frames.add(texture);
        this.delays.add(delayMs);
        return this;
    }

    public GifBuilder frames(List<GpuTextureView> frames, List<Integer> delays) {
        if (this.frames == null) {
            this.frames = new ArrayList<>();
            this.delays = new ArrayList<>();
        } else {
            this.frames.clear();
            this.delays.clear();
        }
        if (frames != null) {
            this.frames.addAll(frames);
        }
        if (delays != null) {
            this.delays.addAll(delays);
        }
        return this;
    }

    public GifBuilder gifId(String gifId) {
        this.gifId = gifId;
        return this;
    }

    public GifBuilder startTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    @Override
    protected GifRenderer _build() {
        // Инициализируем списки если они null
        if (frames == null) {
            frames = new ArrayList<>();
        }
        if (delays == null) {
            delays = new ArrayList<>();
        }

        // Убеждаемся, что количество задержек соответствует количеству кадров
        while (delays.size() < frames.size()) {
            delays.add(100); // Дефолтная задержка 100мс
        }

        // Генерируем уникальный ID для этого GIF, если не установлен
        String finalGifId = this.gifId;
        if (finalGifId == null || finalGifId.isEmpty()) {
            finalGifId = generateGifId(frames);
        }

        return new GifRenderer(
            this.size,
            this.radius,
            this.color,
            this.smoothness,
            new ArrayList<>(this.frames),
            new ArrayList<>(this.delays),
            finalGifId
        );
    }

    private String generateGifId(List<GpuTextureView> frames) {
        if (frames == null || frames.isEmpty()) {
            return "gif_empty_" + System.currentTimeMillis();
        }
        // Генерируем стабильный ID на основе количества кадров
        // Используем только количество кадров для простоты и стабильности
        // Если нужна уникальность, лучше использовать gifId из GifData
        return "gif_" + frames.size() + "_" + frames.hashCode();
    }

    @Override
    protected void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.WHITE;
        this.smoothness = 1.0f;
        if (this.frames != null) {
            this.frames.clear();
        } else {
            this.frames = new ArrayList<>();
        }
        if (this.delays != null) {
            this.delays.clear();
        } else {
            this.delays = new ArrayList<>();
        }
        this.startTime = 0;
        this.gifId = null;
    }

}
