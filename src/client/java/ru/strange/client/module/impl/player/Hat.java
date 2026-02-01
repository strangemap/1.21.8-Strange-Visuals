package ru.strange.client.module.impl.player;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.strange.client.event.EventInit;
import ru.strange.client.event.impl.EventRender3D;
import ru.strange.client.module.api.Category;
import ru.strange.client.module.api.IModule;
import ru.strange.client.module.api.Module;
import ru.strange.client.module.api.setting.impl.BooleanSetting;
import ru.strange.client.module.api.setting.impl.HueSetting;
import ru.strange.client.module.api.setting.impl.ModeSetting;
import ru.strange.client.utils.render.RenderUtil;

import java.awt.*;
import java.util.OptionalDouble;

import static ru.strange.client.utils.render.RenderUtil.*;

@IModule(
        name = "Китайская шляпа",
        description = " ",
        category = Category.Player,
        bind = -1
)
public class Hat extends Module {

    private static final int BUFFER_SIZE = 1 << 16;
    public static HueSetting colorSetting = new HueSetting("Цвет", new Color(131, 166, 232));

    public Hat() {
        addSettings(colorSetting);
    }

    @EventInit
    public void onRender(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON) return;

        BufferAllocator allocator = new BufferAllocator(BUFFER_SIZE);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        MatrixStack matrices = event.getMatrixStack();
        float partialTicks = event.getTickDelta();

        try {
            Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
            ItemStack helmet = mc.player.getEquippedStack(EquipmentSlot.HEAD);

            double x = mc.player.lastRenderX + (mc.player.getX() - mc.player.lastRenderX) * partialTicks;
            double y = mc.player.lastRenderY + (mc.player.getY() - mc.player.lastRenderY) * partialTicks;
            double z = mc.player.lastRenderZ + (mc.player.getZ() - mc.player.lastRenderZ) * partialTicks;

            double hatY = y + mc.player.getHeight() - (!helmet.isEmpty() ? -0.08f : mc.player.isSneaking() ? 0.1f : 0.03f);

            matrices.push();
            matrices.translate(x - cameraPos.x, hatY - cameraPos.y, z - cameraPos.z);

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            renderChinaHat(immediate, matrix);

            matrices.pop();
            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    private void renderChinaHat(VertexConsumerProvider.Immediate immediate, Matrix4f matrix) {
        int segments = 120;
        float radius = 0.55f;
        float height = 0.25f;
        float alpha = 180 / 255f;

        VertexConsumer fillBuffer = immediate.getBuffer(getHatFillLayer());

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) Math.toRadians(i * (360.0 / segments));
            float angle2 = (float) Math.toRadians((i + 1) * (360.0 / segments));

            int c1 = ColorUtil.multAlpha(colorSetting.getRGB(), alpha);
            int c2 = ColorUtil.multAlpha(colorSetting.getRGB(), alpha);

            fillBuffer.vertex(matrix, 0, height, 0).color(c1);
            fillBuffer.vertex(matrix, (float)Math.cos(angle1) * radius, 0, (float)Math.sin(angle1) * radius).color(c1);
            fillBuffer.vertex(matrix, (float)Math.cos(angle2) * radius, 0, (float)Math.sin(angle2) * radius).color(c2);
        }

        VertexConsumer lineBuffer = immediate.getBuffer(getHatLineLayer());
        int lineCol = ColorUtil.replAlpha(ColorUtil.fade(), 255);

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) Math.toRadians(i * (360.0 / segments));
            float angle2 = (float) Math.toRadians((i + 1) * (360.0 / segments));

            lineBuffer.vertex(matrix, (float)Math.cos(angle1) * radius, 0, (float)Math.sin(angle1) * radius).color(lineCol);
            lineBuffer.vertex(matrix, (float)Math.cos(angle2) * radius, 0, (float)Math.sin(angle2) * radius).color(lineCol);
        }
    }
    private static final RenderPipeline HAT_FILL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("strange", "hat_fill"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    private static RenderPipeline HAT_LINE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("strange", "hat_line"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    private RenderLayer getHatFillLayer() {
        return RenderLayer.of(
                "strange_hat_fill",
                BUFFER_SIZE,
                false,
                true,
                HAT_FILL_PIPELINE,
                RenderLayer.MultiPhaseParameters.builder().build(false)
        );
    }

    private RenderLayer getHatLineLayer() {
        return RenderLayer.of(
                "strange_hat_line",
                BUFFER_SIZE,
                false,
                true,
                HAT_LINE_PIPELINE,
                RenderLayer.MultiPhaseParameters.builder()
                        .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(3)))
                        .build(false)
        );
    }
}