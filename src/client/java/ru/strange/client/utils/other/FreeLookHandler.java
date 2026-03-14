package ru.strange.client.utils.other;

import net.minecraft.util.math.MathHelper;
import ru.strange.client.event.EventInit;
import ru.strange.client.event.EventManager;
import ru.strange.client.event.impl.EventLook;
import ru.strange.client.event.impl.EventRotation;
import ru.strange.client.utils.Helper;

public class FreeLookHandler implements Helper {

    public FreeLookHandler() {
        EventManager.register(this);
    }

    private static boolean active;
    private static float freeYaw, freePitch;

    @EventInit
    public void onLook(EventLook event) {

        if (active) {
            rotateTowards(event.yaw, event.pitch);
            event.cancel();
        }
    }
    @EventInit
    public void onRotation(EventRotation event) {

        if (active) {
            event.yaw = freeYaw;
            event.pitch = freePitch;
        } else {
            freeYaw = event.yaw;
            freePitch = event.pitch;
        }
    }

    public static void setActive(boolean state) {
        if (active != state) {
            active = state;
            resetRotation();
        }
    }

    private void rotateTowards(double yaw, double pitch) {
        double d0 = pitch * 0.15D;
        double d1 = yaw * 0.15D;
        freePitch = (float) ((double) freePitch + d0);
        freeYaw = (float) ((double) freeYaw + d1);
        freePitch = MathHelper.clamp(freePitch, -90.0F, 90.0F);
    }

    private static void resetRotation() {
        assert mc.player != null;
        mc.player.setYaw(freeYaw);
        mc.player.setPitch(freePitch);
    }

    public static boolean isActive() {
        return active;
    }

    public static float getFreeYaw() {
        return freeYaw;
    }

    public static void setFreeYaw(float freeYaw) {
        FreeLookHandler.freeYaw = freeYaw;
    }

    public static float getFreePitch() {
        return freePitch;
    }

    public static void setFreePitch(float freePitch) {
        FreeLookHandler.freePitch = freePitch;
    }


}
