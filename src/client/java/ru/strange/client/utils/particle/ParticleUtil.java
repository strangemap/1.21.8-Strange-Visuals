package ru.strange.client.utils.particle;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ru.strange.client.utils.animation.util.Animation;
import ru.strange.client.utils.animation.util.Easings;
import ru.strange.client.utils.math.Mathf;
import ru.strange.client.utils.math.StopWatch;
import ru.strange.client.utils.player.PlayerUtil;
import ru.strange.client.utils.render.RenderUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParticleUtil {
    private static final int QUAD_BUFFER_SIZE_BYTES = 1 << 10;
    private static final String PIPELINE_NAMESPACE = "strange";
    private static final RenderPipeline TEXTURED_QUADS_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
                    .withLocation(Identifier.of(PIPELINE_NAMESPACE, "pipeline/world/textured_quads"))
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    public static final Map<ParticleType, RenderLayer> RENDER_LAYER_CACHE = new ConcurrentHashMap<>();
    private static final Vector3f REUSABLE_NORMAL = new Vector3f(0, 0, 1);

    public static void renderParticle(MatrixStack matrix, VertexConsumerProvider.Immediate immediate,
                                     Particle particle, float x, float y, float z,
                                     float pos, int color, int alpha) {

        matrix.push();

        RenderUtil.setupOrientationMatrix(matrix, x, y, z);
        matrix.multiply(particle.mc.gameRenderer.getCamera().getRotation());

        RenderLayer renderLayer = RENDER_LAYER_CACHE.computeIfAbsent(particle.type(), type -> {
            Identifier texture = type.texture();
            return RenderLayer.of(
                    texture.toString(),
                    QUAD_BUFFER_SIZE_BYTES,
                    false,
                    true,
                    TEXTURED_QUADS_PIPELINE,
                    RenderLayer.MultiPhaseParameters.builder()
                            .texture(new RenderPhase.Texture(texture, false))
                            .build(false)
            );
        });

        MatrixStack.Entry entry = matrix.peek();
        Matrix4f matrix4f = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();

        VertexConsumer buffer = immediate.getBuffer(renderLayer);

        drawTexturedQuad(buffer, matrix4f, normalMatrix,
                -pos, -pos, pos * 2, pos * 2, color, alpha);

        if (particle.type == ParticleType.BLOOM) {
            drawTexturedQuad(buffer, matrix4f, normalMatrix,
                    -pos / 2, -pos / 2, pos, pos, color, alpha);
        }

        matrix.pop();
    }

    private static void drawTexturedQuad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix, float x, float y, float width, float height, int color, int alpha) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        REUSABLE_NORMAL.set(0, 0, 1);
        normalMatrix.transform(REUSABLE_NORMAL);
        REUSABLE_NORMAL.normalize();

        float x1 = x;
        float y1 = y;
        float x2 = x + width;
        float y2 = y + height;

        buffer.vertex(matrix, x1, y1, 0.0f).color(r, g, b, alpha).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(REUSABLE_NORMAL.x, REUSABLE_NORMAL.y, REUSABLE_NORMAL.z);
        buffer.vertex(matrix, x2, y1, 0.0f).color(r, g, b, alpha).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(REUSABLE_NORMAL.x, REUSABLE_NORMAL.y, REUSABLE_NORMAL.z);
        buffer.vertex(matrix, x2, y2, 0.0f).color(r, g, b, alpha).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(REUSABLE_NORMAL.x, REUSABLE_NORMAL.y, REUSABLE_NORMAL.z);
        buffer.vertex(matrix, x1, y2, 0.0f).color(r, g, b, alpha).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(REUSABLE_NORMAL.x, REUSABLE_NORMAL.y, REUSABLE_NORMAL.z);
    }

    public static void renderParticles(MatrixStack matrix, VertexConsumerProvider.Immediate immediate, Vec3d cameraPos, java.util.List<Particle> particles, long fadeInTime, long fadeOutTime, double deltaTime) {
        if (particles.isEmpty()) return;

        matrix.push();
        for (Particle particle : particles) {
            particle.update(true, deltaTime);

            boolean notFinishedFadeIn = !particle.time().finished(fadeInTime);
            boolean finishedFadeOut = particle.time().finished(fadeOutTime);

            if (notFinishedFadeIn) {
                particle.animation().run(1, 0.4, Easings.QUAD_OUT, true);
            } else if (finishedFadeOut) {
                particle.animation().run(0, 0.4, Easings.QUAD_OUT, true);
            }

            if (particle.animation.isAlive()) {
                particle.animation.update();
            }

            float animValue = particle.animation.get();
            int alpha = (int) (animValue * 255);
            if (alpha <= 0) continue;

            Vec3d v = particle.position();
//            if (!RenderUtil.isInFieldOfView(particle.mc, v.x, v.y, v.z)) continue;

            int color = RenderUtil.ColorUtil.replAlpha(particle.color(), alpha);
            renderParticle(matrix, immediate, particle, (float) v.x, (float) v.y, (float) v.z, particle.size, color, alpha);
        }
        matrix.pop();
    }

    public enum ParticleType {
        HEART("heart", false),
        STAR("star", false),
        SNOW("snowflake", false),
        BLOOM("firefly", false),
        DOLLAR("dollar", false),
        TRIANGLE("triangle", false),
        SAKURA("sakura", false),
        GEMINI("genshin", false),
        SIMS("rhombus", false);

        private final Identifier texture;
        private final boolean rotatable;

        ParticleType(String name, boolean rotatable) {
            this.texture = Identifier.of("strange", "textures/world/" + name + ".png");
            this.rotatable = rotatable;
        }

        public Identifier texture() {
            return texture;
        }

        public boolean rotatable() {
            return rotatable;
        }
    }

    public static class Particle {
        private net.minecraft.client.MinecraftClient mc;
        private Box box;
        private final ParticleType type;
        private Vec3d position;
        private Vec3d velocity;
        private final int index;
        private final int rotate;
        private final int color;
        private final float size;
        private static final double BASE_VELOCITY = 0.05;
        private final double speedMultiplier;

        private final StopWatch time = new StopWatch();
        private final Animation animation = new Animation();

        public Particle(
                net.minecraft.client.MinecraftClient mc,
                ParticleType type,
                Vec3d position,
                Vec3d velocity,
                int index,
                int rotate,
                int color,
                float size,
                double speedMultiplier
        ) {
            this.mc = mc;
            double halfSize = size / 2.0;

            this.box = new Box(
                    new Vec3d(position.x - halfSize, position.y - halfSize, position.z - halfSize),
                    new Vec3d(position.x + halfSize, position.y + halfSize, position.z + halfSize)
            );

            this.type = type;
            this.position = position;
            this.velocity = velocity.multiply(BASE_VELOCITY);
            this.index = index;
            this.rotate = rotate;
            this.color = color;
            this.size = size;
            this.speedMultiplier = speedMultiplier;

            this.time.reset();
        }

        // ===== GETTERS =====

        public Box box() {
            return box;
        }

        public ParticleType type() {
            return type;
        }

        public Vec3d position() {
            return position;
        }

        public Vec3d velocity() {
            return velocity;
        }

        public int index() {
            return index;
        }

        public int rotate() {
            return rotate;
        }

        public int color() {
            return color;
        }

        public float size() {
            return size;
        }

        public double speedMultiplier() {
            return speedMultiplier;
        }

        public StopWatch time() {
            return time;
        }

        public Animation animation() {
            return animation;
        }

        // ===== LOGIC =====

        public void update(boolean physic, double deltaTime) {
            if (physic && mc.world != null) {
                double velMagSq =
                        velocity.x * velocity.x +
                                velocity.y * velocity.y +
                                velocity.z * velocity.z;

                if (velMagSq > 0.0001) {
                    if (PlayerUtil.isBlockSolid(position.x, position.y, position.z + velocity.z)) {
                        velocity = new Vec3d(
                                velocity.x * 1.35F,
                                velocity.y * 1.35F,
                                velocity.z * -1.1
                        );
                    }

                    if (PlayerUtil.isBlockSolid(position.x, position.y + velocity.y, position.z)) {
                        velocity = new Vec3d(
                                velocity.x * 1.35F,
                                velocity.y * -1.1,
                                velocity.z * 1.35F
                        );
                    }

                    if (PlayerUtil.isBlockSolid(position.x + velocity.x, position.y, position.z)) {
                        velocity = new Vec3d(
                                velocity.x * -1.1,
                                velocity.y * 1.35F,
                                velocity.z * 1.35F
                        );
                    }
                }

                double friction = Math.pow(0.999, deltaTime * 60);
                velocity = velocity.multiply(friction).subtract(0, 0.00002, 0);
            }

            double deltaMultiplier = deltaTime * 60 * speedMultiplier;
            position = new Vec3d(
                    position.x + velocity.x * deltaMultiplier,
                    position.y + velocity.y * deltaMultiplier,
                    position.z + velocity.z * deltaMultiplier
            );

            double halfSize = size / 2.0;
            box = new Box(
                    new Vec3d(position.x - halfSize, position.y - halfSize, position.z - halfSize),
                    new Vec3d(position.x + halfSize, position.y + halfSize, position.z + halfSize)
            );
        }
    }
}
