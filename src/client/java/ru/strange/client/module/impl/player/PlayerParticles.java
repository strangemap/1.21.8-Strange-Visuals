package ru.strange.client.module.impl.player;

import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import ru.strange.client.event.EventInit;
import ru.strange.client.event.impl.EventAttack;
import ru.strange.client.event.impl.EventChangeWorld;
import ru.strange.client.event.impl.EventMotion;
import ru.strange.client.event.impl.EventRender3D;
import ru.strange.client.module.api.Category;
import ru.strange.client.module.api.IModule;
import ru.strange.client.module.api.Module;
import ru.strange.client.module.api.setting.impl.*;
import ru.strange.client.utils.math.Mathf;
import ru.strange.client.utils.particle.ParticleUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 *  Author https://github.com/WhiteWindows1 20.01.2026
 */

@IModule(
        name = "Частицы Игрока",
        description = "Частицы при атаке и броске",
        category = Category.Player,
        bind = -1
)
public class PlayerParticles extends Module {
    public static BooleanSetting attackEnabled = new BooleanSetting("Атака", true);
    public static BooleanSetting throwEnabled = new BooleanSetting("Бросок", true);
    public static HueSetting colorSetting = new HueSetting("Цвет", new Color(131, 166, 232));
    public static ModeSetting particleMode = new ModeSetting("Тип частиц", "Bloom", "Bloom", "Star", "Snow", "Heart","Dollar","Triangle","Sakura","Genshin","Rhombus");
    public static SliderSetting size = new SliderSetting("Размер", 0.5f, 0.0f, 1.0f, 0.1f, false);

    private long lastUpdateTime = System.nanoTime();

    public PlayerParticles() {
        addSettings(attackEnabled, throwEnabled, colorSetting, particleMode, size);
    }

    private final List<ParticleUtil.Particle> targetParticles = new ArrayList<>();
    private final List<ParticleUtil.Particle> flameParticles = new ArrayList<>();

    private void clear() {
        targetParticles.clear();
        flameParticles.clear();
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
    public void onEvent(EventAttack event) {
        if (!attackEnabled.get()) return;
        
        Entity target = event.getTarget();
        float motion = 6;
        for (int i = 0; i < 35; i++) {
            spawnParticle(targetParticles, new Vec3d(target.getX(), target.getY() + Mathf.randomValue(0, target.getHeight()), target.getZ()),
                    new Vec3d(Mathf.randomValue(-motion, motion), Mathf.randomValue(-motion, motion ), Mathf.randomValue(-motion, motion)));
        }
    }

    @EventInit
    public void onEvent(EventMotion event) {
        if (!throwEnabled.get()) return;
        if (mc.world == null) return;

        for (Entity entity : mc.world.getEntities()) {

            if (!(entity instanceof EnderPearlEntity
                    || entity instanceof ArrowEntity
                    || entity instanceof TridentEntity)) {
                continue;
            }

            if (entity instanceof TridentEntity trident) {
                if (trident.isOnGround()) {
                    continue;
                }
            }

            boolean isMoving =
                    entity.lastX != entity.getX() ||
                            entity.lastY != entity.getY() ||
                            entity.lastZ != entity.getZ();

            if (!isMoving) continue;

            Vec3d pos = entity.getPos();

            for (int i = 0; i < 4; i++) {
                spawnParticle(
                        flameParticles,
                        new Vec3d(
                                pos.x + MathHelper.nextDouble(Random.create(), -0.2, 0.2),
                                pos.y + MathHelper.nextDouble(Random.create(), -0.2, 0.2),
                                pos.z + MathHelper.nextDouble(Random.create(), -0.2, 0.2)
                        ),
                        new Vec3d(
                                MathHelper.nextDouble(Random.create(), -1, 1),
                                MathHelper.nextDouble(Random.create(), -0.3, 0.3 ),
                                MathHelper.nextDouble(Random.create(), -1, 1)
                        )
                );
            }
        }

        removeExpiredParticles(targetParticles, 2000);
        removeExpiredParticles(flameParticles, 2000);
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
            renderParticles(matrix, immediate, cameraPos, targetParticles, 400, 600, deltaTime);
            renderParticles(matrix, immediate, cameraPos, flameParticles, 700, 1200, deltaTime);
            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void renderParticles(MatrixStack matrix, VertexConsumerProvider.Immediate immediate, Vec3d cameraPos, List<ParticleUtil.Particle> particles, long fadeInTime, long fadeOutTime, double deltaTime) {
        ParticleUtil.renderParticles(matrix, immediate, cameraPos, particles, fadeInTime, fadeOutTime, deltaTime);
    }

    private void removeExpiredParticles(List<ParticleUtil.Particle> particles, long lifespan) {
        particles.removeIf(particle -> particle.time().finished(lifespan));
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
