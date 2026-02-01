package ru.strange.client.event.impl;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import ru.strange.client.event.Event;

public class EventRenderItem extends Event {
    private final MatrixStack matrix;
    private final Hand hand;

    public EventRenderItem(MatrixStack matrix, Hand hand) {
        this.matrix = matrix;
        this.hand = hand;
    }

    public MatrixStack getMatrix() {
        return matrix;
    }

    public Hand getHand() {
        return hand;
    }

    public boolean isRightHand() {
        return hand == Hand.MAIN_HAND;
    }
}
