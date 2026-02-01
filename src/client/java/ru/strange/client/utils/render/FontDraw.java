package ru.strange.client.utils.render;

import net.minecraft.client.gui.DrawContext;
import ru.strange.client.renderengine.font.FontRenderer;
import ru.strange.client.utils.Helper;

/**
 *  Author https://github.com/WhiteWindows1 20.01.2026
 */

public class FontDraw implements Helper {

    public static void drawText(FontType f, DrawContext mt,String text ,float x,float y,int size,int color,boolean shadow) {
        if(size == 0) {size = 1;}

        String familyF = "medium";
        if(f == FontType.MEDIUM) {
            familyF = "medium";
        } else if(f == FontType.SEMIBOLD) {
            familyF = "semibold";
        }

        FontRenderer fontRenderer = FontRenderer.create(familyF, size);
        fontRenderer.setShadow(shadow);
        fontRenderer.drawDirect(mt, text, x, y, color);

    }

    public static void drawText(FontType f, DrawContext mt,String text ,float x,float y,int size,int color) {
        drawText(f,mt,text,x,y,size,color,false);
    }

    public static void drawCenter(FontType f, DrawContext mt,String text ,float x,float y,int size,int color,boolean shadow) {
        drawText(f,mt,text,x - getWidth(f,text,size) / 2f,y,size,color,shadow);
    }

    public static float getWidth(FontType f,String text,int size) {
        if(size == 0) {size = 1;}

        String familyF = "medium";
        if(f == FontType.MEDIUM) {
            familyF = "medium";
        } else if(f == FontType.SEMIBOLD) {
            familyF = "semibold";
        }

        FontRenderer fontRenderer = FontRenderer.create(familyF, size);

        return fontRenderer.getWidth(text,size);
    }

    public enum FontType {
        MEDIUM,SEMIBOLD
    }

}