package ru.strange.client.module.impl.world;

import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import ru.strange.client.event.EventInit;
import ru.strange.client.event.impl.EventChangeWorld;
import ru.strange.client.event.impl.EventMotion;
import ru.strange.client.event.impl.EventRender3D;
import ru.strange.client.module.api.Category;
import ru.strange.client.module.api.IModule;
import ru.strange.client.module.api.Module;
import ru.strange.client.module.api.setting.impl.*;
import ru.strange.client.utils.animation.util.Animation;
import ru.strange.client.utils.animation.util.Easings;
import ru.strange.client.utils.math.Mathf;
import ru.strange.client.utils.particle.ParticleUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 *  Author https://github.com/WhiteWindows1 20.01.2026
 */

@IModule(
        name = "Частицы Мира",
        description = "Частицы в мире",
        category = Category.World,
        bind = -1
)
public class WorldParticles extends Module {
    public static HueSetting colorSetting = new HueSetting("Цвет", new Color(131, 166, 232));
    public static ModeSetting particleMode = new ModeSetting("Тип частиц", "Bloom", "Bloom", "Star", "Snow", "Heart","Dollar","Triangle","Sakura","Genshin","Rhombus");
    public static SliderSetting size = new SliderSetting("Размер", 0.5f, 0.0f, 1.0f, 0.1f, false);

    private long lastUpdateTime = System.nanoTime();

    public WorldParticles() {
        addSettings(colorSetting, particleMode, size);
    }

    private final List<ParticleUtil.Particle> worldParticles = new ArrayList<>();

    private void clear() {
        worldParticles.clear();
    }

    private void spawnParticle(List<ParticleUtil.Particle> particles, Vec3d position, Vec3d velocity) {
        float particleSize = 0.05F + (this.size.get() * 0.2F);
        int color = colorSetting.getRGB();

        ParticleUtil.ParticleType type = switch (this.particleMode.get()) {
            case "Heart" -> ParticleUtil.ParticleType.HEART;
            case "Star" -> ParticleUtil.ParticleType.STAR;
            case "Snow" -> ParticleUtil.ParticleType.SNOW;
            case "Bloom" -> ParticleUtil.ParticleType.BLOOM;
            case "Dollar" -> ParticleUtil.ParticleType.DOLLAR;
            case "Triangle" -> ParticleUtil.ParticleType.TRIANGLE;
            case "Sakura" -> ParticleUtil.ParticleType.SAKURA;
            case "Genshin" -> ParticleUtil.ParticleType.GEMINI;
            case "Rhombus" -> ParticleUtil.ParticleType.SIMS;
            default ->  ParticleUtil.ParticleType.BLOOM;
        };

        particles.add(new ParticleUtil.Particle(mc, type,
                position.add(0, particleSize, 0),
                velocity,
                particles.size(),
                (int) Mathf.step(Mathf.randomValue(0, 360), 15),
                color,
                particleSize,
                0.2F)
        );
    }

    @EventInit
    public void onEvent(EventMotion event) {
        if (mc.world == null || mc.player == null) return;

        int r = 12;

        for (int i = 0; i <7; i++) {

            Vec3d additional = mc.player.getPos().add(
                    Mathf.randomValue(-r, r),
                    0,
                    Mathf.randomValue(-r, r)
            );

            BlockPos topPos = mc.world.getTopPosition(
                    Heightmap.Type.MOTION_BLOCKING,
                    BlockPos.ofFloored(additional)
            );

            double x = topPos.getX() + Mathf.randomValue(0, 1);
            double z = topPos.getZ() + Mathf.randomValue(0, 1);
            double y = mc.player.getY() + Mathf.randomValue(mc.player.getHeight(), r);

            Vec3d spawnPos = new Vec3d(x, y, z);

            while (!mc.world.isAir(BlockPos.ofFloored(spawnPos)) && spawnPos.y < mc.world.getTopYInclusive()) {
                spawnPos = spawnPos.add(0, 1, 0);
            }

            spawnParticle(
                    worldParticles,
                    spawnPos,
                    new Vec3d(
                            mc.player.getVelocity().x + Mathf.randomValue(-2, 2),
                            Mathf.randomValue((float) -0.2, 0.2F),
                            mc.player.getVelocity().z + Mathf.randomValue(-2, 2)
                    )
            );
        }

        removeExpiredParticles(worldParticles, 4000);
    }

    @EventInit
    public void onEvent(EventRender3D event) {
        MatrixStack matrix = event.getMatrixStack();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        long now = System.nanoTime();
        double deltaTime = (now - lastUpdateTime) / 1_000_000_000.0;
        lastUpdateTime = now;

        BufferAllocator allocator = new BufferAllocator(1 << 18);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);

        try {
            renderParticles(matrix, immediate, cameraPos, worldParticles, 1500, 2200, deltaTime);
            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    private void removeExpiredParticles(List<ParticleUtil.Particle> particles, long lifespan) {
        particles.removeIf(particle -> particle.time().finished(lifespan));
    }

    @SuppressWarnings("SameParameterValue")
    private void renderParticles(MatrixStack matrix, VertexConsumerProvider.Immediate immediate, Vec3d cameraPos, List<ParticleUtil.Particle> particles, long fadeInTime, long fadeOutTime, double deltaTime) {
        ParticleUtil.renderParticles(matrix, immediate, cameraPos, particles, fadeInTime, fadeOutTime, deltaTime);
    }


    @Override
    public void toggle() {
        super.toggle();
        clear();
    }

    @EventInit
    public void onEvent(EventChangeWorld event) {
        clear();
    }

}
