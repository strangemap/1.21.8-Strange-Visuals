package ru.strange.client.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.strange.client.event.EventManager;
import ru.strange.client.event.impl.EventRotation;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Unique
    private EventRotation strange$rotationEvent;

    @Unique
    private float strange$originalYaw;

    @Unique
    private float strange$originalPitch;

    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdateHead(net.minecraft.world.BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        if (focusedEntity != null) {
            this.strange$originalYaw = focusedEntity.getYaw(tickProgress);
            this.strange$originalPitch = focusedEntity.getPitch(tickProgress);
            this.strange$rotationEvent = new EventRotation(this.strange$originalYaw, this.strange$originalPitch, tickProgress);
            EventManager.call(this.strange$rotationEvent);
        } else {
            this.strange$rotationEvent = null;
        }
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void redirectSetRotation(Camera instance, float yaw, float pitch) {
        if (this.strange$rotationEvent != null && (this.strange$rotationEvent.getYaw() != this.strange$originalYaw || this.strange$rotationEvent.getPitch() != this.strange$originalPitch)) {
            this.setRotation(this.strange$rotationEvent.getYaw(), this.strange$rotationEvent.getPitch());
        } else {
            this.setRotation(yaw, pitch);
        }
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void onUpdateReturn(CallbackInfo ci) {
        this.strange$rotationEvent = null;
    }
}
