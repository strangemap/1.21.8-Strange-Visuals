package ru.strange.client.module.impl.player;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ru.strange.client.Strange;
import ru.strange.client.event.EventInit;
import ru.strange.client.event.impl.EventJump;
import ru.strange.client.event.impl.EventRender3D;
import ru.strange.client.module.api.Category;
import ru.strange.client.module.api.IModule;
import ru.strange.client.module.api.Module;
import ru.strange.client.module.api.setting.impl.HueSetting;
import ru.strange.client.utils.animation.util.Animation;
import ru.strange.client.utils.animation.util.Easings;
import ru.strange.client.utils.render.RenderUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@IModule(
        name = "Джампики",
        description = " ",
        category = Category.Player,
        bind = -1
)
public class JumpCircle extends Module {
    private final List<Circle> circles = new ArrayList<>();
    public static HueSetting colorSetting = new HueSetting("Цвет", new Color(131, 166, 232));

    public JumpCircle() {
        addSettings(colorSetting);
    }

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

    @EventInit
    public void onJump(EventJump e) {
        circles.add(new Circle(mc.player.getPos().add(0,0.05, 0)));
    }

    @EventInit
    public void onRender(EventRender3D e) {
        if (circles.isEmpty()) {
            return;
        }


        circles.removeIf(c -> System.currentTimeMillis() - c.time > 4500);

        if (circles.isEmpty()) {
            return;
        }

        BufferAllocator allocator = new BufferAllocator(1 << 18);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        MatrixStack pose = e.getMatrixStack();

        try {
            Identifier texture = Identifier.of(Strange.rootRes, "textures/world/jump.png");
            int color = colorSetting.getRGB();
            int alpha = 255;
            for (Circle c : circles) {
                if (System.currentTimeMillis() - c.time > 2000 && !c.isBack) {
                    c.animation.run(0, 1, Easings.EXPO_IN);
                    c.animation2.run(0, 1, Easings.QUART_IN );
                    c.isBack = true;
                }

                c.animation.update();
                c.animation2.update();
                float rad = (float) c.animation.getValue();

                double posX = c.vector3d.x - mc.gameRenderer.getCamera().getPos().x;
                double posY = c.vector3d.y - mc.gameRenderer.getCamera().getPos().y;
                double posZ = c.vector3d.z - mc.gameRenderer.getCamera().getPos().z;

                float size = 0.6f  * rad;

                pose.push();
                pose.translate(posX, posY, posZ);
                pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) (90 + 360 * c.animation2.getValue())));

                RenderLayer renderLayer = RenderLayer.of(
                        texture.toString(),
                        QUAD_BUFFER_SIZE_BYTES,
                        false,
                        true,
                        TEXTURED_QUADS_PIPELINE,
                        RenderLayer.MultiPhaseParameters.builder()
                                .texture(new RenderPhase.Texture(texture, false))
                                .build(false)
                );

                MatrixStack.Entry entry = pose.peek();
                Matrix4f matrix4f = entry.getPositionMatrix();
                Matrix3f normalMatrix = entry.getNormalMatrix();
                VertexConsumer buffer = immediate.getBuffer(renderLayer);

                drawTexturedQuad(buffer, matrix4f, normalMatrix, -size / 2f, -size / 2f, size, size, color, alpha);

                pose.pop();
            }

            immediate.draw();
        } finally {
            allocator.close();
        }
    }


    private void drawTexturedQuad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix, float x, float y, float width, float height, int color, int alpha) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;


        Vector3f normal = new Vector3f(0, 0, 1);
        normalMatrix.transform(normal);
        normal.normalize();

        float x1 = x;
        float y1 = y;
        float x2 = x + width;
        float y2 = y + height;

        buffer.vertex(matrix, x1, y1, 0.0f).color(r, g, b, alpha).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(normal.x, normal.y, normal.z);
        buffer.vertex(matrix, x2, y1, 0.0f).color(r, g, b, alpha).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(normal.x, normal.y, normal.z);
        buffer.vertex(matrix, x2, y2, 0.0f).color(r, g, b, alpha).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(normal.x, normal.y, normal.z);
        buffer.vertex(matrix, x1, y2, 0.0f).color(r, g, b, alpha).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(normal.x, normal.y, normal.z);
    }

    private class Circle {

        private final Vec3d vector3d;

        private final long time;
        private final Animation animation = new Animation();
        private final Animation animation2 = new Animation();
        private boolean isBack;

        public Circle(Vec3d vector3d) {
            this.vector3d = vector3d;
            time = System.currentTimeMillis();
            animation.run(2, 1, Easings.EXPO_OUT);
            animation2.run(1    , 1, Easings.QUART_OUT);
        }

    }
}