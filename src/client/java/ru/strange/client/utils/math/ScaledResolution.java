package ru.strange.client.utils.math;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.util.math.MathHelper;

import java.lang.reflect.Method;


public class ScaledResolution
{
    private final double scaledWidthD;
    private final double scaledHeightD;
    private int scaledWidth;
    private int scaledHeight;
    private static int scaleFactor;

    public ScaledResolution(MinecraftClient mc) {
        // Проверяем что окно инициализировано
        if (mc == null || mc.getWindow() == null) {
            // Fallback значения если окно еще не создано
            this.scaledWidth = 1920;
            this.scaledHeight = 1080;
            scaleFactor = 1;
            this.scaledWidthD = this.scaledWidth;
            this.scaledHeightD = this.scaledHeight;
            return;
        }
        
        this.scaledWidth = mc.getWindow().getWidth();
        this.scaledHeight = mc.getWindow().getHeight();
        scaleFactor = 1;
        
        boolean forceUnicodeFont = false;
        try {
            GameOptions options = mc.options;
            // Пробуем использовать публичный метод getForceUnicodeFont()
            try {
                Method getMethod = GameOptions.class.getMethod("getForceUnicodeFont");
                @SuppressWarnings("unchecked")
                SimpleOption<Boolean> flag = (SimpleOption<Boolean>) getMethod.invoke(options);
                forceUnicodeFont = flag != null && flag.getValue();
            } catch (NoSuchMethodException e) {
                // Если публичного метода нет, используем рефлексию для приватного метода
                Method forceUnicodeFontMethod = GameOptions.class.getDeclaredMethod("forceUnicodeFont");
                forceUnicodeFontMethod.setAccessible(true);
                @SuppressWarnings("unchecked")
                SimpleOption<Boolean> flag = (SimpleOption<Boolean>) forceUnicodeFontMethod.invoke(options);
                forceUnicodeFont = flag != null && flag.getValue();
            }
        } catch (Exception e) {
            // Если не удалось получить доступ к forceUnicodeFont, просто игнорируем
        }
        
        int i = 2;

        while (scaleFactor < i && this.scaledWidth / (scaleFactor + 1) >= 320 && this.scaledHeight / (scaleFactor + 1) >= 240) ++scaleFactor;
        if (forceUnicodeFont && scaleFactor % 2 != 0 && scaleFactor != 1) --scaleFactor;

        this.scaledWidthD = (double)this.scaledWidth / scaleFactor;
        this.scaledHeightD = (double)this.scaledHeight / scaleFactor;
        this.scaledWidth = MathHelper.ceil(this.scaledWidthD);
        this.scaledHeight = MathHelper.ceil(this.scaledHeightD);
    }

    public int getWidth() {
        return this.scaledWidth;
    }

    public int getHeight() {
        return this.scaledHeight;
    }

    public double getScaledWidth_double() {
        return this.scaledWidthD;
    }

    public double getScaledHeight_double() {
        return this.scaledHeightD;
    }

    public static int getScaleFactor() {
        return scaleFactor;
    }
}
