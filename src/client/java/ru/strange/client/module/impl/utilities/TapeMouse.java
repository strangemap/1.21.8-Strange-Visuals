package ru.strange.client.module.impl.utilities;

import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ru.strange.client.event.EventInit;
import ru.strange.client.event.impl.EventUpdate;
import ru.strange.client.mixin.MinecraftClientAccessor;
import ru.strange.client.module.api.Category;
import ru.strange.client.module.api.IModule;
import ru.strange.client.module.api.Module;
import ru.strange.client.module.api.setting.impl.ModeSetting;
import ru.strange.client.module.api.setting.impl.SliderSetting;
import ru.strange.client.utils.math.StopWatch;

@IModule(
        name = "Тейп маус",
        description = "кто ваще это юзает ?-?",
        category = Category.Utilities,
        bind = -1
)
public class TapeMouse extends Module {

    private final SliderSetting delay = new SliderSetting("Задержка", 1000, 100, 5000, 100, false);
    private final StopWatch timerUtil = new StopWatch();

    public TapeMouse() {
        addSettings(delay);
    }

    @Override
    public void toggle() {
        super.toggle();
        resetTimer();
    }

    @EventInit
    public void onEvent(EventUpdate event) {
        if (timerUtil.finished(delay.get())) {
            doAttack();
            timerUtil.reset();
        }
    }

    private void doAttack() {
        if (mc.player == null || mc.interactionManager == null) return;

        HitResult hitResult = mc.crosshairTarget;
        
        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity target = entityHitResult.getEntity();
            
            if (target != null) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        } else if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            BlockPos blockPos = blockHitResult.getBlockPos();
            Direction direction = blockHitResult.getSide();
            
            mc.interactionManager.attackBlock(blockPos, direction);
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            Vec3d direction = mc.player.getRotationVector();
            Vec3d eyePos = mc.player.getEyePos();
            Vec3d targetPos = eyePos.add(direction.multiply(3.0));
            BlockPos blockPos = BlockPos.ofFloored(targetPos);
            
            mc.interactionManager.attackBlock(blockPos, Direction.UP);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private void resetTimer() {
        timerUtil.reset();
    }
}
