package ru.strange.client.renderengine.builders.states;

import java.awt.Color;

public record QuadColorState(int color1, int color2, int color3, int color4) {

    public static final QuadColorState TRANSPARENT = new QuadColorState(0, 0, 0, 0);
    public static final QuadColorState WHITE = new QuadColorState(-1, -1, -1, -1);

    public QuadColorState(Color color1, Color color2, Color color3, Color color4) {
        this(toABGR(color1), toABGR(color2), toABGR(color3), toABGR(color4));
    }

    public QuadColorState(Color color) {
        this(color, color, color, color);
    }

    public QuadColorState(int color) {
        this(color, color, color, color);
    }

    // Конвертирует Color в ABGR формат для OpenGL
    private static int toABGR(Color color) {
        int a = color.getAlpha();
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

}