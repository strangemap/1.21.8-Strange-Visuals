package ru.strange.client.module.impl.player;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ru.strange.client.Strange;
import ru.strange.client.event.EventInit;
import ru.strange.client.event.impl.EventAttack;
import ru.strange.client.event.impl.EventChangeWorld;
import ru.strange.client.event.impl.EventRender3D;
import ru.strange.client.module.api.Category;
import ru.strange.client.module.api.IModule;
import ru.strange.client.module.api.Module;
import ru.strange.client.module.api.setting.impl.HueSetting;
import ru.strange.client.module.api.setting.impl.ModeSetting;
import ru.strange.client.utils.animation.util.Animation;
import ru.strange.client.utils.animation.util.Easings;
import ru.strange.client.utils.math.Mathf;
import ru.strange.client.utils.render.RenderUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@IModule(
        name = "Хит бабл",
        description = "Эффект кольца при ударе по цели",
        category = Category.Player,
        bind = -1
)
public class HitBubble extends Module {

    public static ModeSetting textureMode = new ModeSetting("Текстура", "Bubble1", "Bubble1", "Bubble2");
    public static HueSetting colorSetting = new HueSetting("Цвет", new Color(255, 255, 255));

    public HitBubble() {
        addSettings(textureMode, colorSetting);
    }

    private static final int QUAD_BUFFER_SIZE_BYTES = 1 << 10;
    private static final String PIPELINE_NAMESPACE = "strange";

    private static final RenderPipeline BUBBLE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
                    .withLocation(Identifier.of(PIPELINE_NAMESPACE, "pipeline/world/hit_bubble"))
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.ADDITIVE)
                    .build()
    );

    private static RenderLayer createLayer(Identifier texture) {
        return RenderLayer.of(
                texture.toString(),
                QUAD_BUFFER_SIZE_BYTES,
                false,
                true,
                BUBBLE_PIPELINE,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(new RenderPhase.Texture(texture, false))
                        .build(false)
        );
    }

    private final List<Bubble> bubbles = new ArrayList<>();

    @Override
    public void toggle() {
        super.toggle();
        bubbles.clear();
    }

    @EventInit
    public void onWorldChange(EventChangeWorld e) {
        bubbles.clear();
    }

    @EventInit
    public void onAttack(EventAttack event) {
        if (mc.player == null || mc.world == null) return;
        Entity target = event.getTarget();
        if (!(target instanceof LivingEntity living)) return;
        PlayerEntity player = mc.player;
        if (player == null) return;

        Vec3d fromEye = player.getPos().add(0, player.getEyeHeight(player.getPose()), 0);
        Vec3d to = living.getPos().add(0, living.getHeight() * 0.8, 0);
        Vec3d diff = to.subtract(fromEye);

        double distance = Math.max(0.0, player.distanceTo(living) - 0.5 + player.getWidth() * 0.5);
        double yawRad = Math.atan2(diff.z, diff.x) - Math.PI / 2.0;
        double xOff = -Math.sin(yawRad) * distance;
        double zOff = Math.cos(yawRad) * distance;

        Vec3d bubblePos = new Vec3d(
                player.getX() + xOff,
                to.y,
                player.getZ() + zOff
        );

        bubbles.add(new Bubble(bubblePos));
    }

    @EventInit
    public void onRender(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;
        if (bubbles.isEmpty()) return;

        float partialTicks = event.getTickDelta();
        MatrixStack matrices = event.getMatrixStack();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        Identifier texture = getCurrentTexture();
        RenderLayer layer = createLayer(texture);

        BufferAllocator allocator = new BufferAllocator(1 << 18);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);

        try {
            VertexConsumer buffer = immediate.getBuffer(layer);

            bubbles.removeIf(Bubble::isDead);
            if (bubbles.isEmpty()) return;

            for (Bubble bubble : bubbles) {
                bubble.update();
                bubble.render(matrices, buffer, cameraPos, partialTicks);
            }

            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    private Identifier getCurrentTexture() {
        String name = textureMode.get();
        String path = "textures/world/" + (name.equalsIgnoreCase("Bubble2") ? "bubble2.png" : "bubble1.png");
        return Identifier.of(Strange.rootRes, path);
    }

    private class Bubble {
        private final Vec3d pos;
        private final long startTime = System.currentTimeMillis();
        private final Animation anim = new Animation();
        private final Animation rotAnim = new Animation();

        private static final long LIFE_TIME_MS = 900L;

        Bubble(Vec3d pos) {
            this.pos = pos;
            anim.set(0.0);
            anim.run(1.0, (LIFE_TIME_MS / 2_000.0), Easings.QUAD_OUT, false);
            rotAnim.set(0.0);
            rotAnim.run(1.0, LIFE_TIME_MS / 1000.0, Easings.QUAD_OUT, true);
        }

        boolean isDead() {
            return System.currentTimeMillis() - startTime >= LIFE_TIME_MS;
        }

        void update() {
            anim.update();
            rotAnim.update();
        }

        private float getAgePc() {
            long dt = System.currentTimeMillis() - startTime;
            return MathHelper.clamp((float) dt / (float) LIFE_TIME_MS, 0.0f, 1.0f);
        }

        private float getAlpha() {
            long now = System.currentTimeMillis();
            long age = now - startTime;

            if (age <= 0L || age >= LIFE_TIME_MS) return 0.0f;

            float t = MathHelper.clamp((float) age / (float) LIFE_TIME_MS, 0.0f, 1.0f);
            float in = MathHelper.clamp(t / 0.35f, 0.0f, 1.0f);
            float out = MathHelper.clamp((1.0f - t) / 0.35f, 0.0f, 1.0f);
            float base = Math.min(in, out);

            return (float) Easings.QUART_OUT.ease(base);
        }

        void render(MatrixStack matrices, VertexConsumer buffer, Vec3d cameraPos, float partialTicks) {
            float alpha = getAlpha();
            if (alpha <= 0.01f) return;

            float baseScale = 0.9f;
            float scale = baseScale * (0.7f + anim.get() * 0.6f);

            int baseColor = colorSetting.getRGB();
            int color = RenderUtil.ColorUtil.multAlpha(baseColor, alpha);

            double x = MathHelper.lerp(partialTicks, pos.x, pos.x);
            double y = MathHelper.lerp(partialTicks, pos.y, pos.y);
            double z = MathHelper.lerp(partialTicks, pos.z, pos.z);

            matrices.push();
            matrices.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);

            matrices.multiply(mc.gameRenderer.getCamera().getRotation());

            float rot = rotAnim.get() * 360.0f;
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(rot));

            matrices.scale(scale, scale, scale);

            MatrixStack.Entry entry = matrices.peek();
            Matrix4f matrix4f = entry.getPositionMatrix();
            Matrix3f normalMatrix = entry.getNormalMatrix();

            drawTexturedQuad(buffer, matrix4f, normalMatrix, -0.5f, -0.5f, 1.0f, 1.0f, color);

            matrices.pop();
        }

        private final Vector3f REUSABLE_NORMAL = new Vector3f(0, 0, 1);

        private void drawTexturedQuad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix,
                                      float x, float y, float width, float height, int color) {
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            int a = (color >> 24) & 0xFF;

            REUSABLE_NORMAL.set(0, 0, 1);
            normalMatrix.transform(REUSABLE_NORMAL);
            REUSABLE_NORMAL.normalize();

            float x1 = x;
            float y1 = y;
            float x2 = x + width;
            float y2 = y + height;

            buffer.vertex(matrix, x1, y1, 0.0f).color(r, g, b, a).texture(0, 1)
                    .overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0)
                    .normal(REUSABLE_NORMAL.x, REUSABLE_NORMAL.y, REUSABLE_NORMAL.z);
            buffer.vertex(matrix, x2, y1, 0.0f).color(r, g, b, a).texture(1, 1)
                    .overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0)
                    .normal(REUSABLE_NORMAL.x, REUSABLE_NORMAL.y, REUSABLE_NORMAL.z);
            buffer.vertex(matrix, x2, y2, 0.0f).color(r, g, b, a).texture(1, 0)
                    .overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0)
                    .normal(REUSABLE_NORMAL.x, REUSABLE_NORMAL.y, REUSABLE_NORMAL.z);
            buffer.vertex(matrix, x1, y2, 0.0f).color(r, g, b, a).texture(0, 0)
                    .overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0)
                    .normal(REUSABLE_NORMAL.x, REUSABLE_NORMAL.y, REUSABLE_NORMAL.z);
        }
    }
}
