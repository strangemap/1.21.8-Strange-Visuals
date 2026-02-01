package ru.strange.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import me.x150.renderer.event.RenderEvents;
import me.x150.renderer.util.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.strange.client.Strange;
import ru.strange.client.module.api.Module;
import ru.strange.client.module.impl.other.AspectRation;
import ru.strange.client.module.impl.other.NoRender;
import ru.strange.client.renderengine.renderers.util.FrameTracker;

import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    public abstract float getFarPlaneDistance();

    @WrapOperation(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V"))
    void renderer_postWorldRender(WorldRenderer instance, ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, Operation<Void> original) {
        original.call(instance, allocator, tickCounter, renderBlockOutline, camera, positionMatrix, projectionMatrix, fogBuffer, fogColor, renderSky);

        Profiler prof = Profilers.get();
        prof.swap("rendererLibWorld");

        MatrixStack matrix = new MatrixStack();
        matrix.multiplyPositionMatrix(positionMatrix);

        RenderUtils.lastProjMat.set(projectionMatrix);
        RenderUtils.lastModMat.set(RenderSystem.getModelViewMatrix());
        RenderUtils.lastWorldSpaceMatrix.set(matrix.peek().getPositionMatrix());
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, RenderUtils.lastViewport);

        RenderEvents.AFTER_WORLD.invoker().rendered(matrix);

        // restore state like the original world rendering code did
        GlStateManager._depthMask(true);
        GlStateManager._disableBlend();
    }

    @Inject(method = "getBasicProjectionMatrix", at = @At("HEAD"), cancellable = true)
    public void getBasicProjectionMatrix(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        Matrix4f matrix4f = new Matrix4f();
        cir.cancel();
        float aspect = (float) MinecraftClient.getInstance().getWindow().getFramebufferWidth() / (float) MinecraftClient.getInstance().getWindow().getFramebufferHeight() + AspectRation.getAspectRation();

        cir.setReturnValue(matrix4f.perspective(
                (fovDegrees * (float) (Math.PI / 180.0)),
                aspect,
                0.05F,
                this.getFarPlaneDistance())
        );
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    public void getBasicProjectionMatrix(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
        if (Strange.get.manager.getModule(NoRender.class).enable && NoRender.settings.get("Убрать тряску")) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        FrameTracker.onFrameStart();
    }
}
