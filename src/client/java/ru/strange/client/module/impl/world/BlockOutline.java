package ru.strange.client.module.impl.world;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import ru.strange.client.event.EventInit;
import ru.strange.client.event.impl.EventRender3D;
import ru.strange.client.module.api.Category;
import ru.strange.client.module.api.IModule;
import ru.strange.client.module.api.Module;
import ru.strange.client.module.api.setting.impl.BooleanSetting;
import ru.strange.client.module.api.setting.impl.HueSetting;
import ru.strange.client.module.api.setting.impl.ModeSetting;
import ru.strange.client.module.api.setting.impl.SliderSetting;
import ru.strange.client.utils.render.RenderUtil;

import java.awt.*;
import java.util.OptionalDouble;

@IModule(
        name = "Обводка блока",
        description = "Обводка блока под прицелом",
        category = Category.World,
        bind = -1
)
public class BlockOutline extends Module {
    public static HueSetting colorSetting = new HueSetting("Цвет", new Color(131, 166, 232));
    public static ModeSetting colorMode = new ModeSetting("Режим цвета", "Client", "Client", "RGB", "Astolfo");
    public static BooleanSetting animateColors = new BooleanSetting("Анимация", true)
            .hidden(() -> colorMode.is("Client"));
    public static SliderSetting colorSpeed = new SliderSetting("Скорость", 1.0f, 0.0f, 5.0f, 0.1f, false)
            .hidden(() -> colorMode.is("Client") || !animateColors.get());

    public BlockOutline() {
        addSettings(colorSetting, colorMode, animateColors, colorSpeed);
    }

    @EventInit
    public void onRender(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        HitResult hitResult = mc.crosshairTarget;
        if (!(hitResult instanceof BlockHitResult blockHitResult)) return;

        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockState state = mc.world.getBlockState(blockPos);
        if (state.isAir()) return;

        VoxelShape shape = state.getOutlineShape(mc.world, blockPos, ShapeContext.of(mc.player));
        if (shape.isEmpty()) return;

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Matrix4f matrix = event.getMatrixStack().peek().getPositionMatrix();

        int[] baseColors = getCornerColors();
        int[] outlineColors = applyAlpha(baseColors, 255);
        BufferAllocator allocator = new BufferAllocator(1 << 18);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);

        try {
            RenderLayer lineLayer = OUTLINE_NO_DEPTH_LAYER;
            VertexConsumer lineBuffer = immediate.getBuffer(lineLayer);

            shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                double x1 = blockPos.getX() + minX - cameraPos.x;
                double y1 = blockPos.getY() + minY - cameraPos.y;
                double z1 = blockPos.getZ() + minZ - cameraPos.z;
                double x2 = blockPos.getX() + maxX - cameraPos.x;
                double y2 = blockPos.getY() + maxY - cameraPos.y;
                double z2 = blockPos.getZ() + maxZ - cameraPos.z;

                double expand = 0.0;
                x1 -= expand;
                y1 -= expand;
                z1 -= expand;
                x2 += expand;
                y2 += expand;
                z2 += expand;

                drawBoxEdges(lineBuffer, matrix, x1, y1, z1, x2, y2, z2, outlineColors);
            });

            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    private int[] getCornerColors() {
        return switch (colorMode.get()) {
            case "RGB", "Random" -> getRgbCornerColors();
            case "Astolfo" -> getAstolfoCornerColors();
            default -> new int[]{
                    colorSetting.getRGB(),
                    colorSetting.getRGB(),
                    colorSetting.getRGB(),
                    colorSetting.getRGB(),
                    colorSetting.getRGB(),
                    colorSetting.getRGB(),
                    colorSetting.getRGB(),
                    colorSetting.getRGB()
            };
        };
    }

    private int[] getRgbCornerColors() {
        float baseHue = getAnimatedHue(4000L);
        int[] colors = new int[8];
        for (int i = 0; i < colors.length; i++) {
            float hue = (baseHue + i * 0.125f) % 1.0f;
            colors[i] = Color.getHSBColor(hue, 1.0f, 1.0f).getRGB();
        }
        return colors;
    }

    private int[] getAstolfoCornerColors() {
        float baseHue = getAnimatedHue(12000L);
        int[] colors = new int[8];
        for (int i = 0; i < colors.length; i++) {
            float hue = (baseHue + i * 0.08f) % 1.0f;
            colors[i] = Color.getHSBColor(hue, 0.6f, 1.0f).getRGB();
        }
        return colors;
    }

    private float getAnimatedHue(long basePeriodMs) {
        if (!animateColors.get() || colorSpeed.get() <= 0.0f) {
            return 0.0f;
        }
        float speed = Math.max(0.05f, colorSpeed.get());
        long period = (long) Math.max(250L, basePeriodMs / speed);
        return (float) (System.currentTimeMillis() % period) / (float) period;
    }

    private static int[] applyAlpha(int[] colors, int alpha) {
        int[] out = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            out[i] = RenderUtil.ColorUtil.replAlpha(colors[i], alpha);
        }
        return out;
    }

    private static void drawBoxEdges(VertexConsumer buffer, Matrix4f matrix,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ,
                                     int[] colors) {
        int[] c = colors.length >= 8 ? colors : new int[]{colors[0], colors[0], colors[0], colors[0], colors[0], colors[0], colors[0], colors[0]};


        drawLine(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, c[0], c[1]);
        drawLine(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, c[1], c[2]);
        drawLine(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, c[2], c[3]);
        drawLine(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, c[3], c[0]);

        drawLine(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, c[4], c[5]);
        drawLine(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, c[5], c[6]);
        drawLine(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, c[6], c[7]);
        drawLine(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, c[7], c[4]);

        drawLine(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, c[0], c[4]);
        drawLine(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, c[1], c[5]);
        drawLine(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, c[2], c[6]);
        drawLine(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, c[3], c[7]);
    }


    private static void drawLine(VertexConsumer buffer, Matrix4f matrix,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 int color1, int color2) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;

        buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).color(r1, g1, b1, a1);
        buffer.vertex(matrix, (float) x2, (float) y2, (float) z2).color(r2, g2, b2, a2);
    }


    private static final int LINE_BUFFER_SIZE_BYTES = 1 << 10;

    private static final RenderPipeline OUTLINE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(net.minecraft.util.Identifier.of("strange", "block_outline_lines"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    private static final RenderPipeline OUTLINE_NO_DEPTH_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(net.minecraft.util.Identifier.of("strange", "block_outline_lines_no_depth"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    private static final RenderLayer OUTLINE_LAYER = RenderLayer.of(
            "strange_block_outline",
            LINE_BUFFER_SIZE_BYTES,
            false,
            true,
            OUTLINE_PIPELINE,
            RenderLayer.MultiPhaseParameters.builder()
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(2.0)))
                    .build(false)
    );

    private static final RenderLayer OUTLINE_NO_DEPTH_LAYER = RenderLayer.of(
            "strange_block_outline_no_depth",
            LINE_BUFFER_SIZE_BYTES,
            false,
            true,
            OUTLINE_NO_DEPTH_PIPELINE,
            RenderLayer.MultiPhaseParameters.builder()
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(2.0)))
                    .build(false)
    );
}