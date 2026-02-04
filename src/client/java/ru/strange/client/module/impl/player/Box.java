package ru.strange.client.module.impl.player;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import ru.strange.client.Strange;
import ru.strange.client.event.EventInit;
import ru.strange.client.event.impl.EventRender3D;
import ru.strange.client.event.impl.EventScreen;
import ru.strange.client.module.api.Category;
import ru.strange.client.module.api.IModule;
import ru.strange.client.module.api.Module;
import ru.strange.client.module.api.setting.impl.BooleanSetting;
import ru.strange.client.module.api.setting.impl.HueSetting;
import ru.strange.client.module.api.setting.impl.MultiBooleanSetting;
import ru.strange.client.utils.render.Render3D;
import ru.strange.client.utils.render.RenderUtil;

import java.awt.*;
import java.util.OptionalDouble;

@IModule(
        name = "Боксы",
        description = " ",
        category = Category.Player,
        bind = -1
)
public class Box extends Module {

    private static final int QUAD_BUFFER_SIZE_BYTES = 1 << 10;

    public static MultiBooleanSetting targets = new MultiBooleanSetting(
            "Кого отображать",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Мобы", true)
    );
    public static HueSetting colorSetting = new HueSetting("Цвет", new Color(131, 166, 232));

    public Box() {
        addSettings(targets, colorSetting);
    }

    @EventInit
    public void render2d(EventScreen e) {
//        drawFovZone(e.drawContext());
    }

    @EventInit
    public void render(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        BufferAllocator allocator = new BufferAllocator(1 << 18);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);

        try {
            for (var ent : mc.world.getEntities()) {
                if (shouldRender(ent) && isInFieldOfView(ent, event.getTickDelta()) && isVisibleThroughBlocks(ent, event.getTickDelta())) {
                    renderBox(event.getMatrixStack(), immediate, ent, event.getTickDelta());
                }
            }
            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    private boolean shouldRender(Entity entity) {
        if (entity == mc.player) return false;
        
        if (entity.isInvisible()) return false;

        if (entity instanceof PlayerEntity) {
            return targets.get("Игроки");
        } else if (entity instanceof LivingEntity) {
            return targets.get("Мобы");
        }

        return false;
    }

    private void drawFovZone(DrawContext ctx) {
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        if (screenW <= 0 || screenH <= 0) return;

        float aspect = (float) screenW / (float) screenH;

        var camera = mc.gameRenderer.getCamera();

        float vFovDeg = (float) mc.gameRenderer.getFov(camera, mc.getRenderTickCounter().getDynamicDeltaTicks(), false);

        float vFov = vFovDeg * MathHelper.RADIANS_PER_DEGREE;
        float hFov = (float) (2.0 * Math.atan(Math.tan(vFov * 0.5) * aspect));

        float zoneX = 150 * MathHelper.RADIANS_PER_DEGREE;
        float zoneY = 100 * MathHelper.RADIANS_PER_DEGREE;

        float width  = (float) (screenW * (Math.tan(zoneX * 0.5) / Math.tan(hFov * 0.5)));
        float height = (float) (screenH * (Math.tan(zoneY * 0.5) / Math.tan(vFov * 0.5)));

        width  = MathHelper.clamp(width,  2f, screenW);
        height = MathHelper.clamp(height, 2f, screenH);

        float x = screenW * 0.5f - width * 0.5f;
        float y = screenH * 0.5f - height * 0.5f;

        int color = 0x6600FF00;
        RenderUtil.Rect.draw(ctx, x, y, width, height, color);
    }

    private boolean isInFieldOfView(Entity entity, float partialTicks) {
        var camera = mc.gameRenderer.getCamera();
        Vec3d cam = camera.getPos();

        double ex = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * partialTicks;
        double ey = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * partialTicks + entity.getHeight() * 0.5;
        double ez = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * partialTicks;

        double dx = ex - cam.x;
        double dy = ey - cam.y;
        double dz = ez - cam.z;

        double distXZ = Math.sqrt(dx * dx + dz * dz);
        if (distXZ < 1e-6) return true;

        float targetYaw = (float)(MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90f;
        float targetPitch = (float)(-(MathHelper.atan2(dy, distXZ) * 180.0 / Math.PI));

        float camYaw = camera.getYaw();
        float camPitch = camera.getPitch();

        float dyaw = MathHelper.wrapDegrees(targetYaw - camYaw);
        float dpitch = MathHelper.wrapDegrees(targetPitch - camPitch);

        return Math.abs(dyaw) <= 150 * 0.5f
                && Math.abs(dpitch) <= 150 * 0.5f;
    }

    private boolean isVisibleThroughBlocks(Entity entity, float partialTicks) {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        double entityX = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * partialTicks;
        double entityY = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * partialTicks + entity.getHeight() * 0.5;
        double entityZ = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * partialTicks;
        Vec3d entityCenter = new Vec3d(entityX, entityY, entityZ);

        HitResult hit = mc.world.raycast(new RaycastContext(
                cameraPos,
                entityCenter,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    private void renderBox(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Entity target, float partialTicks) {
        if (target == null) return;

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();


        double x = target.lastRenderX + (target.getX() - target.lastRenderX) * partialTicks;
        double y = target.lastRenderY + (target.getY() - target.lastRenderY) * partialTicks;
        double z = target.lastRenderZ + (target.getZ() - target.lastRenderZ) * partialTicks;


        net.minecraft.util.math.Box boundingBox = target.getBoundingBox();


        double minX = boundingBox.minX - target.getX() + x - cameraPos.x;
        double minY = boundingBox.minY - target.getY() + y - cameraPos.y;
        double minZ = boundingBox.minZ - target.getZ() + z - cameraPos.z;
        double maxX = boundingBox.maxX - target.getX() + x - cameraPos.x;
        double maxY = boundingBox.maxY - target.getY() + y - cameraPos.y;
        double maxZ = boundingBox.maxZ - target.getZ() + z - cameraPos.z;

        float alphaPC = 1.0f;
        int fadeColor;
        if (target instanceof AbstractClientPlayerEntity player) {
            String playerName = player.getNameForScoreboard();
            if (Strange.get.friendManager.isFriend(playerName)) {
                fadeColor = Color.GREEN.getRGB();
            } else {
                fadeColor = colorSetting.getRGB();
            }
        } else {
            fadeColor = colorSetting.getRGB();
        }
        
        int baseColor = RenderUtil.ColorUtil.multAlpha(fadeColor, alphaPC);

        int color1 = RenderUtil.ColorUtil.multDark(baseColor, 0.3F);
        int color2 = RenderUtil.ColorUtil.multDark(baseColor, 0.6F);
        int color3 = RenderUtil.ColorUtil.multDark(baseColor, 0.3F);
        int color4 = RenderUtil.ColorUtil.multDark(baseColor, 0.6F);

        int[] gradientColors = new int[]{
                color1,
                color2,
                color3,
                color4
        };

        Matrix4f matrix = matrices.peek().getPositionMatrix();


        VertexConsumer fillBuffer = immediate.getBuffer(BOX_FILL_LAYER);
        Render3D.drawBoxFill(fillBuffer, matrix, minX, minY + 0.01f, minZ, maxX, maxY, maxZ, gradientColors, 85);


        VertexConsumer lineBuffer = immediate.getBuffer(BOX_LINE_LAYER);
        Render3D.drawBoxOutline(lineBuffer, matrix, minX, minY + 0.01f, minZ, maxX, maxY, maxZ, gradientColors, 255, 0.15, 0.08);
    }

    private static final RenderPipeline BOX_FILL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("minecraft", "rendertype_lequal_depth_test"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    private static final RenderPipeline BOX_LINE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("minecraft", "rendertype_lines"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    private static final RenderLayer BOX_FILL_LAYER = RenderLayer.of(
            "strange_esp_box_fill",
            QUAD_BUFFER_SIZE_BYTES,
            false,
            true,
            BOX_FILL_PIPELINE,
            RenderLayer.MultiPhaseParameters.builder().build(false)
    );

    private static final RenderLayer BOX_LINE_LAYER = RenderLayer.of(
            "strange_esp_box_line",
            QUAD_BUFFER_SIZE_BYTES,
            false,
            true,
            BOX_LINE_PIPELINE,
            RenderLayer.MultiPhaseParameters.builder()
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(2)))
                    .build(false)
    );
}
