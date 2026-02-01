package ru.strange.client.event.impl;

import net.minecraft.entity.Entity;
import ru.strange.client.event.Event;

public class EventAttack extends Event {
    private Entity target;

    public EventAttack(Entity target) {
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }

    public void setTarget(Entity target) {
        this.target = target;
    }
}
